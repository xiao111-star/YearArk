---
inclusion: auto
---

# YearArk 鉴权架构详解

## 三套独立的身份体系

本项目存在三套完全独立的 Sa-Token StpLogic 实例，各自管理不同的用户群体，互不干扰：

| 身份体系 | StpLogic 工具类 | loginType | Token Header | 用户表 | 适用场景 |
|---|---|---|---|---|---|
| 管理端 | `StpUtil`（Sa-Token 默认） | `login` | `Authorization` | `sys_user` | 管理后台操作 |
| 用户端已登录用户 | `StpUserUtil` | `ya-user` | `Ya-Auth` | `ya_user` | 纪念册创建/管理 |
| 匿名上传者 | `StpAnonUtil` | `ya-anon` | `Ya-Anon-Auth` | `ya_invite_token` | 通过邀请链接上传素材 |

## 后端拦截器执行顺序

一个请求进来后，Spring MVC 拦截器链按注册顺序执行。本项目有两个关键拦截器：

### 拦截器 1：SecurityConfig（管理端，框架自带）

- 来源：`ruoyi-common-security` 模块，`@AutoConfiguration` 自动装配
- 拦截范围：`/**`（所有路径）
- 排除路径：`application.yml` 中 `security.excludes` 配置，包含 `/api/user/**`
- 认证方式：`StpUtil.checkLogin()`（读 `Authorization` header）
- 权限控制：通过 `@SaCheckPermission` 注解（如 `yearark:template:add`）

```
请求 → SecurityConfig 拦截器
  ├── 路径匹配 /api/user/** → 跳过（excludePathPatterns 排除）
  ├── 路径匹配 /yearark/** → StpUtil.checkLogin() → 读 Authorization header
  └── 路径匹配 /system/** → StpUtil.checkLogin() → 读 Authorization header
```

### 拦截器 2：YaUserSecurityConfig（用户端，自定义）

- 来源：`ruoyi-system` 模块，`@Configuration`
- 拦截范围：`/api/user/**`
- 关键配置：`interceptor.isAnnotation = false`（禁用默认注解鉴权，避免触发 StpUtil）
- 认证方式：按路径分别使用 StpUserUtil 或 StpAnonUtil

```
请求 /api/user/** → YaUserSecurityConfig 拦截器
  ├── /api/user/album/**        → StpUserUtil.checkLogin() → 读 Ya-Auth header
  ├── /api/user/invite/**       → StpUserUtil.checkLogin() → 读 Ya-Auth header
  ├── /api/user/media/**        → StpUserUtil.checkLogin() → 读 Ya-Auth header
  ├── /api/user/template/**     → StpUserUtil.checkLogin() → 读 Ya-Auth header
  ├── /api/user/share/upload/** → StpAnonUtil.checkLogin() → 读 Ya-Anon-Auth header
  ├── /api/user/share/my-uploads→ StpAnonUtil.checkLogin() → 读 Ya-Anon-Auth header
  └── 其他 /api/user/** 路径    → 不拦截（公开接口）
      ├── /api/user/auth/**              → 注册登录
      ├── /api/user/share/{code}         → 验证邀请码
      └── /api/user/share/{code}/verify  → 验证访问码
```

### isAnnotation = false 的重要性

`SaInterceptor` 默认会在执行完自定义 auth handler 后，还会扫描 Controller 方法上的注解（`@SaCheckLogin` 等）并用默认的 `StpUtil` 执行鉴权。对于用户端路径，这会导致去读 `Authorization` header（管理端 token），前端不会带这个 header，直接 401。设置 `isAnnotation = false` 后只执行我们自定义的路由规则。

## 登录与 Token 下发流程

### 管理端登录

```
管理端前端 → POST /auth/login（用户名+密码）
  → SysLoginService.login()
  → StpUtil.login("sys_user:" + userId)  // 默认 StpLogic
  → Sa-Token 生成 token，存入 Redis
  → 触发 UserActionListener.doLogin()，记录在线用户信息到 Redis
  → 返回 token 给前端
  → 前端存储 token，后续请求放入 Authorization header
```

### 用户端登录（ya_user）

```
用户端前端 → POST /api/user/auth/login（用户名+密码）
  → YaUserAuthServiceImpl.login()
  → BCrypt 验证密码
  → 构建 YaLoginUser（userId, username）
  → YaLoginHelper.login(loginUser)
    → StpUserUtil.login(userId)  // 独立 StpLogic，loginType="ya-user"
    → Sa-Token 生成 token，存入 Redis（key 前缀带 ya-user）
    → 将 YaLoginUser 存入 token session
  → 触发 UserActionListener.doLogin()
    → 通过 loginType=="ya-user" 判断，仅记录日志，不走管理端逻辑
  → 返回 YaLoginVo（token, userId, username）
  → 前端存入 localStorage（key: ya-auth-token）
  → 后续请求通过 axios 拦截器自动放入 Ya-Auth header
```

### 匿名用户登录（邀请链接访客）

```
访客前端 → GET /api/user/share/{inviteCode}（公开，获取纪念册信息）
  → 返回 ShareInfoVo（纪念册名称、是否需要访问码）

访客前端 → POST /api/user/share/{inviteCode}/verify（公开，验证访问码）
  → InviteTokenAuthServiceImpl.generateToken()
  → 验证 invite 有效性（存在、状态可用、未过期）
  → 创建 ya_invite_token 记录
  → 构建 YaAnonUser（tokenId, albumId, inviteId）
  → YaLoginHelper.anonLogin(anonUser)
    → StpAnonUtil.login(tokenId)  // 独立 StpLogic，loginType="ya-anon"
    → 将 YaAnonUser 存入 token session
  → 回写 token 到 ya_invite_token 表
  → 返回 TokenVo（token, albumId, albumName）
  → 前端存入 localStorage（key: ya-anon-token）
  → 后续上传请求通过 axios 拦截器自动放入 Ya-Anon-Auth header
```

## 前端 Token 管理

### 用户端前端（yearark-web）

`request.ts` 的 axios 拦截器同时发送两个 header：
- `Ya-Auth`: localStorage 中的 `ya-auth-token`（已登录用户 token）
- `Ya-Anon-Auth`: localStorage 中的 `ya-anon-token`（匿名用户 token）

路由守卫（`router/index.ts`）：
- `meta.requiresAuth` 路由：检查 `useUserStore().isLoggedIn`，未登录跳转 `/login`
- `meta.guest` 路由（登录/注册页）：已登录则跳转 `/dashboard`
- `/share/:inviteCode` 路由：无需认证

### 管理端前端（ruoyi-admin）

使用框架自带的 token 管理，token 放在 `Authorization` header。

## 后端 Controller 包结构与鉴权对应

```
controller/
├── user/                          # 用户端接口（/api/user/**）
│   ├── UserAuthController         # /api/user/auth/**     → 公开
│   ├── UserTemplateController     # /api/user/template/** → StpUserUtil
│   ├── AnonUserController         # /api/user/share/**    → 公开 + StpAnonUtil
│   ├── YaAlbumController          # /api/user/album/**    → StpUserUtil
│   ├── YaInviteController         # /api/user/invite/**   → StpUserUtil
│   └── YaAlbumMediaController     # /api/user/media/**    → StpUserUtil
│
└── yearark/                       # 管理端接口（/yearark/**）
    ├── YaTemplateController       # /yearark/template     → StpUtil + @SaCheckPermission
    ├── YaTemplatePageController   # /yearark/templatePage → StpUtil
    ├── YaTemplateSchemaController # /yearark/templateSchema → StpUtil
    ├── YaUserController           # /yearark/user         → StpUtil
    ├── YaInviteTokenController    # /yearark/inviteToken  → StpUtil
    └── YaAlbumPageController      # /yearark/albumPage    → StpUtil
```

## Sa-Token 全局事件监听

`UserActionListener` 实现了 `SaTokenListener`，所有 StpLogic 实例的登录/注销事件都会触发。

`doLogin` 方法通过 `loginType` 参数区分身份体系：
- `"ya-user"` / `"ya-anon"` → 仅记录日志，直接 return
- 其他（管理端）→ 解析 UserType 枚举，记录在线用户信息到 Redis

## Token 存储（Redis）

三套 StpLogic 的 token 都存在同一个 Redis 实例中，通过 key 前缀区分：
- 管理端：`satoken:login:token:xxx`（默认前缀）
- 用户端：`satoken:ya-user:login:token:xxx`
- 匿名端：`satoken:ya-anon:login:token:xxx`

管理端使用 JWT 简单模式（`StpLogicJwtForSimple`），用户端和匿名端使用普通 token 模式。

## 踩过的坑

1. `SaInterceptor` 默认会执行注解鉴权（用默认 StpUtil），用户端拦截器必须设置 `isAnnotation = false`
2. `UserActionListener.doLogin()` 是全局的，用户端登录也会触发，必须通过 `loginType` 提前判断并 return，否则会尝试解析管理端的 UserType 枚举导致异常
3. 用户端 loginId 直接用 userId（Integer），不需要加 `"ya-user:"` 前缀，因为 StpUserUtil 已经是独立的 StpLogic 实例
4. Sa-Token 全局配置 `token-prefix: "Bearer"` 会被所有 StpLogic 实例继承（包括 StpUserUtil 和 StpAnonUtil），前端发送 token 时必须加 `Bearer ` 前缀，否则 Sa-Token 解析 token 值会出错，导致 `未能读取到有效Token`
