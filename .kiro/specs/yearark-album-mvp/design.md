# 技术设计文档 - YearArk 纪念册 MVP

## Overview

本设计文档描述 YearArk 纪念册 MVP 的技术实现方案，覆盖后端业务接口（用户认证、邀请链接、匿名上传、纪念册生成与渲染）和用户端前端（yearark-web）的完整实现。

### 已有基础

- 所有实体的基础 CRUD 已存在（管理端 Controller + Service + Mapper + Entity + DTO/VO）
- 管理端模板管理前端已完成
- Schema 格式已确定：`image_N`/`text_N` 命名规则，详见 #[[file:.kiro/specs/yearark-schema-design/schema-design.md]]
- 5 套 HTML 模板样例已准备

### 本次需要新增/改造

1. 改造现有管理端 Controller（YaAlbumController、YaInviteController、YaAlbumMediaController）路径从 `/yearark/**` 改为 `/api/user/**`，增加用户认证和归属校验
2. 新建用户端 Controller（UserAuthController、ShareController、UserTemplateController）
3. 用户端认证体系（Sa-Token 多账号体系 StpUserUtil + StpAnonUtil，复用 Redis token 管理）
4. 纪念册生成服务（AlbumGenerationService）和渲染服务（TemplateRenderService）
5. 用户端前端项目 yearark-web（Vue3 + Tailwind CSS + shadcn-vue）

### 技术栈

- **后端**: Spring Boot 3.4 + MyBatis-Plus + Sa-Token（管理端 StpUtil + 用户端 StpUserUtil 多账号体系）
- **前端**: Vue3 + Vite + TypeScript + Tailwind CSS + shadcn-vue + Vue Router + Pinia + Axios
- **数据库**: MySQL 8.0
- **文件存储**: 阿里云 OSS
- **密码加密**: BCrypt

## Architecture

### 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    用户端前端 (yearark-web)                       │
│         Vue3 + Tailwind CSS + shadcn-vue + Pinia                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ 注册登录  │ │ 纪念册管理│ │ 分享上传  │ │ 纪念册预览│          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────────┘
                            │ HTTP/JSON
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│              用户端 API 层 (/api/user/**)                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ UserAuthController(新建) │ ShareController(新建)        │   │
│  │ UserTemplateController(新建)                            │   │
│  │ YaAlbumController(改造)  │ YaInviteController(改造)     │   │
│  │ YaAlbumMediaController(改造)                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            │                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              认证拦截器 (Sa-Token 多账号体系)               │   │
│  │  已登录用户: StpUserUtil（ya_user 体系，token-name: Ya-Auth）│  │
│  │  匿名用户:   StpAnonUtil（invite_token 体系，token-name:   │   │
│  │              Ya-Anon-Auth）                               │   │
│  │  公开接口:   /api/user/auth/**, /api/user/share/{code}/** │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            │                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     Service 层                            │   │
│  │  复用已有: IYaAlbumService, IYaInviteService,            │   │
│  │           IYaInviteTokenService, IYaAlbumMediaService,   │   │
│  │           IYaAlbumPageService, IYaTemplateService,       │   │
│  │           IYaTemplatePageService, IYaTemplateSchemaService│  │
│  │  新增:    YaUserAuthService, InviteTokenAuthService,       │   │
│  │           AlbumGenerationService, TemplateRenderService    │   │
│  │  新增工具: StpUserUtil, StpAnonUtil (Sa-Token 多账号体系)  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      MySQL 8.0                                   │
│  ya_user │ ya_album │ ya_invite │ ya_invite_token │             │
│  ya_album_media │ ya_album_page │ ya_template │                 │
│  ya_template_page │ ya_template_schema                          │
└─────────────────────────────────────────────────────────────────┘
```

### API 路由设计

用户端接口统一使用 `/api/user` 前缀，与管理端 `/yearark` 前缀隔离。

**认证相关（公开）**
```
POST   /api/user/auth/register          # 用户注册
POST   /api/user/auth/login             # 用户登录
```

**纪念册管理（需登录）**
```
POST   /api/user/album                  # 创建纪念册
GET    /api/user/album/list             # 我的纪念册列表
GET    /api/user/album/{id}             # 纪念册详情
POST   /api/user/album/update           # 更新纪念册（选模板等）
DELETE /api/user/album/{id}             # 删除纪念册
POST   /api/user/album/{id}/generate    # 生成纪念册
GET    /api/user/album/{id}/preview     # 预览纪念册（返回渲染后的 HTML 列表）
```

**模板查询（需登录）**
```
GET    /api/user/template/list          # 可用模板列表
GET    /api/user/template/{id}          # 模板详情（含模板页列表）
```

**邀请链接管理（需登录）**
```
POST   /api/user/invite                 # 生成邀请链接
GET    /api/user/invite/list            # 某纪念册的邀请链接列表（query: albumId）
POST   /api/user/invite/{id}/disable    # 禁用邀请链接
```

**素材查看（需登录）**
```
GET    /api/user/media/list             # 某纪念册的素材列表（query: albumId, type）
GET    /api/user/media/stats            # 素材统计（query: albumId）
```

**分享页（公开/匿名 token）**
```
GET    /api/user/share/{inviteCode}     # 验证邀请码，返回纪念册信息
POST   /api/user/share/{inviteCode}/verify  # 验证访问码（如果有）
POST   /api/user/share/upload/image     # 匿名上传图片（需 Ya-Anon-Auth token）
POST   /api/user/share/upload/text      # 匿名上传文字（需 Ya-Anon-Auth token）
GET    /api/user/share/my-uploads       # 匿名用户已上传的素材（需 Ya-Anon-Auth token）
```

### 前端路由设计

```
/login                          # 登录页
/register                       # 注册页
/dashboard                      # 纪念册列表（首页）
/album/create                   # 创建纪念册
/album/:id                      # 纪念册详情（素材、邀请链接、生成按钮）
/album/:id/preview              # 纪念册预览（翻页浏览）
/share/:inviteCode              # 分享上传页（匿名，需移动端适配）
```

## Components and Interfaces

### 后端新增组件

#### 1. StpUserUtil 和 StpAnonUtil（Sa-Token 多账号体系）

参照管理端 `StpUtil`（默认账号体系）的模式，用户端创建两个独立的 StpLogic 实例：

```java
/**
 * 用户端已登录用户的 Sa-Token 操作工具类
 * 对应 ya_user 表，loginId 格式: "ya_user:{userId}"
 */
public class StpUserUtil {
    public static final String TYPE = "ya-user";
    public static final StpLogic stpLogic = new StpLogic(TYPE);
    // 封装 login / checkLogin / getLoginIdAsLong / getTokenValue / getTokenSession 等方法
}

/**
 * 匿名上传者的 Sa-Token 操作工具类
 * 对应 ya_invite_token 表，loginId 格式: "ya_anon:{tokenId}"
 */
public class StpAnonUtil {
    public static final String TYPE = "ya-anon";
    public static final StpLogic stpLogic = new StpLogic(TYPE);
    // 封装 login / checkLogin / getLoginIdAsLong / getTokenValue / getTokenSession 等方法
}
```

Sa-Token 多账号体系说明：
- 管理端使用默认的 `StpUtil`（token-name: `Authorization`）
- 用户端已登录用户使用 `StpUserUtil`（token-name: `Ya-Auth`）
- 匿名上传者使用 `StpAnonUtil`（token-name: `Ya-Anon-Auth`）
- 三套体系的 token 互不干扰，各自独立管理 session
- token 存储在 Redis 中，由 Sa-Token 框架自动管理

#### 2. YaLoginHelper（用户端登录助手）

参照管理端 `LoginHelper` 的模式，封装用户端的登录/获取用户信息逻辑：

```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class YaLoginHelper {
    public static final String LOGIN_USER_KEY = "yaLoginUser";
    public static final String USER_KEY = "yaUserId";

    /** 用户端登录（ya_user） */
    public static void login(YaLoginUser loginUser) {
        SaStorage storage = SaHolder.getStorage();
        storage.set(LOGIN_USER_KEY, loginUser);
        storage.set(USER_KEY, loginUser.getUserId());
        StpUserUtil.login(loginUser.getUserId());
        StpUserUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /** 获取当前登录的 ya_user 信息 */
    public static YaLoginUser getLoginUser() { ... }

    /** 获取当前 ya_user 的 userId */
    public static Integer getUserId() { ... }

    /** 匿名用户登录（ya_invite_token） */
    public static void anonLogin(YaAnonUser anonUser) {
        StpAnonUtil.login(anonUser.getTokenId());
        StpAnonUtil.getTokenSession().set("anonUser", anonUser);
    }

    /** 获取当前匿名用户信息 */
    public static YaAnonUser getAnonUser() { ... }
}
```

Session 存储结构：
- `StpUserUtil` session: `{ "yaLoginUser": { userId, username } }`
- `StpAnonUtil` session: `{ "anonUser": { tokenId, albumId, inviteId } }`

#### 3. YaLoginUser 和 YaAnonUser（Session 数据模型）

```java
/** 用户端已登录用户信息（存储在 StpUserUtil session 中） */
@Data
public class YaLoginUser implements Serializable {
    private Integer userId;
    private String username;
}

/** 匿名上传者信息（存储在 StpAnonUtil session 中） */
@Data
public class YaAnonUser implements Serializable {
    private Integer tokenId;
    private Integer albumId;
    private Integer inviteId;
}
```

#### 4. YaUserAuthService（用户认证服务）

```java
public interface YaUserAuthService {
    // 注册：校验用户名唯一 → BCrypt 加密密码 → 保存 ya_user
    R<Void> register(String username, String password, String email);
    
    // 登录：校验凭据 → StpUserUtil.login() → 返回 token
    R<LoginVo> login(String username, String password);
}
```

登录流程：
```
1. 查询 ya_user（通过 IYaUserService）
2. BCrypt.checkpw() 验证密码
3. 构建 YaLoginUser 对象
4. YaLoginHelper.login(yaLoginUser) → 内部调用 StpUserUtil.login()
5. 返回 StpUserUtil.getTokenValue() 作为 token
```

#### 5. InviteTokenAuthService（匿名 token 认证服务）

```java
public interface InviteTokenAuthService {
    // 验证 invite_code → 生成 ya_invite_token 记录 → StpAnonUtil.login() → 返回 token
    R<TokenVo> generateToken(String inviteCode, String ipAddress);
    
    // 验证访问码
    R<Void> verifyAccessCode(String inviteCode, String accessCode);
}
```

匿名登录流程：
```
1. 验证 invite_code 有效（存在、status=1、未过期）
2. 创建 ya_invite_token 记录（复用 IYaInviteTokenService）
3. 构建 YaAnonUser 对象（tokenId, albumId, inviteId）
4. YaLoginHelper.anonLogin(anonUser) → 内部调用 StpAnonUtil.login()
5. 返回 StpAnonUtil.getTokenValue() 作为 token
```

#### 3. AlbumGenerationService（纪念册生成服务）

```java
public interface AlbumGenerationService {
    /**
     * 生成纪念册
     * 1. 验证纪念册已关联模板且有素材
     * 2. 读取模板页和 Schema
     * 3. 收集素材（图片+文字），按 sort 排序
     * 4. 按 Schema 的 imageCount/textCount 分配素材到模板页
     * 5. 生成 Data JSON
     * 6. 创建 ya_album_page 记录
     * 7. 更新 ya_album.status = 1
     */
    R<Void> generate(Integer albumId);
}
```

素材分配算法：
```
输入：
  - templatePages: 模板页列表（按 sort 排序），每个关联一个 Schema
  - images: 图片素材列表（按 sort 排序）
  - texts: 文字素材列表（按 sort 排序）

处理：
  imageIndex = 0, textIndex = 0
  for each templatePage in templatePages:
    schema = templatePage.schema
    dataJson = {}
    
    // 填充图片
    for i in 1..schema.imageCount:
      if imageIndex < images.size():
        dataJson["image_" + i] = images[imageIndex].content
        imageIndex++
      else:
        dataJson["image_" + i] = DEFAULT_PLACEHOLDER_IMAGE
    
    // 填充文字
    for i in 1..schema.textCount:
      if textIndex < texts.size():
        dataJson["text_" + i] = texts[textIndex].content
        textIndex++
      else:
        dataJson["text_" + i] = ""
    
    // 创建 album_page 记录
    save(albumId, templatePage.id, dataJson, sort++)

输出：
  ya_album_page 记录列表
```

#### 4. TemplateRenderService（模板渲染服务）

```java
public interface TemplateRenderService {
    /**
     * 渲染单页：将 HTML 模板中的 {{slot_id}} 替换为 Data JSON 中的值
     */
    String renderPage(String htmlTemplate, String dataJson);
    
    /**
     * 渲染整本纪念册：返回所有页面的渲染后 HTML 列表
     */
    List<RenderedPageVo> renderAlbum(Integer albumId);
}
```

渲染逻辑：
```java
public String renderPage(String htmlTemplate, String dataJson) {
    Map<String, String> data = objectMapper.readValue(dataJson, Map.class);
    String result = htmlTemplate;
    for (Map.Entry<String, String> entry : data.entrySet()) {
        result = result.replace("{{" + entry.getKey() + "}}", 
                                entry.getValue() != null ? entry.getValue() : "");
    }
    return result;
}
```

#### 6. YaUserSecurityConfig（Sa-Token 用户端路由拦截配置）

不使用自定义 HandlerInterceptor，而是参照管理端 `SecurityConfig` 的模式，创建独立的 Sa-Token 路由拦截配置：

```java
@Configuration
public class YaUserSecurityConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 用户端已登录接口：使用 StpUserUtil 校验
            SaRouter.match("/api/user/album/**", "/api/user/invite/**", 
                           "/api/user/media/**", "/api/user/template/**")
                    .check(() -> StpUserUtil.checkLogin());

            // 匿名上传接口：使用 StpAnonUtil 校验
            SaRouter.match("/api/user/share/upload/**", "/api/user/share/my-uploads")
                    .check(() -> StpAnonUtil.checkLogin());

            // 公开接口（不拦截）：
            // /api/user/auth/**          → 注册登录
            // /api/user/share/{code}     → 验证邀请码
            // /api/user/share/{code}/verify → 验证访问码
        })).addPathPatterns("/api/user/**");
    }
}
```

同时需要在管理端 `SecurityConfig` 的 excludePathPatterns 中排除 `/api/user/**`，避免管理端 StpUtil 拦截用户端请求。方式：在 `application.yml` 的 `security.excludes` 中添加 `/api/user/**`。

### 后端新增 Controller

#### UserAuthController（新建）
```
POST /api/user/auth/register  → YaUserAuthService.register()
POST /api/user/auth/login     → YaUserAuthService.login()
```

#### ShareController（新建）
```
GET    /api/user/share/{inviteCode}           → 验证邀请码
POST   /api/user/share/{inviteCode}/verify    → 验证访问码 → 返回 StpAnonUtil token
POST   /api/user/share/upload/image           → 匿名上传图片（StpAnonUtil 校验）
POST   /api/user/share/upload/text            → 匿名上传文字（StpAnonUtil 校验）
GET    /api/user/share/my-uploads             → 匿名用户已上传素材（StpAnonUtil 校验）
```

#### UserTemplateController（新建）
```
GET    /api/user/template/list          → 可用模板列表
GET    /api/user/template/{id}          → 模板详情（含模板页列表）
```

### 改造现有管理端 Controller

以下三个 Controller 原为管理端 CRUD 接口（`/yearark/**`），管理端前端未使用，直接改造为用户端接口（`/api/user/**`），增加用户认证和归属校验逻辑。

#### YaAlbumController → 改造为用户端纪念册接口
- 路径从 `/yearark/album` 改为 `/api/user/album`
- 移除 Sa-Token 管理端认证依赖，走 YaUserSecurityConfig（StpUserUtil 校验）
- 所有接口增加从 YaLoginHelper.getUserId() 获取当前用户 ID 的逻辑
- 查询接口自动注入 userId 条件（只能看自己的纪念册）
- 详情/更新/删除接口增加归属校验（album.userId == currentUserId）
- 新增生成和预览接口
```
POST   /api/user/album                → 创建纪念册（自动填充 userId）
GET    /api/user/album/list           → 查询当前用户的纪念册列表
GET    /api/user/album/{id}           → 纪念册详情（校验归属）
POST   /api/user/album/update         → 更新纪念册（校验归属）
DELETE /api/user/album/{id}           → 删除纪念册（校验归属）
POST   /api/user/album/{id}/generate  → AlbumGenerationService.generate()
GET    /api/user/album/{id}/preview   → TemplateRenderService.renderAlbum()
```

#### YaInviteController → 改造为用户端邀请链接接口
- 路径从 `/yearark/invite` 改为 `/api/user/invite`
- 移除 Sa-Token 管理端认证依赖，走 YaUserSecurityConfig（StpUserUtil 校验）
- 新增接口增加自动生成 6 位唯一 invite_code 和过期时间逻辑
- 查询接口校验纪念册归属（通过 albumId 查到 album，校验 userId）
- 新增禁用接口（替代通用 update）
```
POST   /api/user/invite               → 生成邀请链接（自动生成 inviteCode）
GET    /api/user/invite/list           → 查询某纪念册的邀请链接（query: albumId，校验归属）
POST   /api/user/invite/{id}/disable  → 禁用邀请链接（校验归属）
```

#### YaAlbumMediaController → 改造为用户端素材查看接口
- 路径从 `/yearark/album-media` 改为 `/api/user/media`
- 移除 Sa-Token 管理端认证依赖，走 YaUserSecurityConfig（StpUserUtil 校验）
- 查询接口校验纪念册归属
- 新增素材统计接口
- 移除新增/修改/删除接口（用户端素材通过 ShareController 匿名上传，创建者只能查看）
```
GET    /api/user/media/list            → 查询某纪念册的素材列表（query: albumId, type，校验归属）
GET    /api/user/media/stats           → 素材统计（图片数、文字数）
```

### 前端组件

#### 页面组件

| 页面 | 路由 | 说明 |
|------|------|------|
| LoginPage | /login | 登录表单 |
| RegisterPage | /register | 注册表单 |
| DashboardPage | /dashboard | 纪念册卡片列表 |
| AlbumCreatePage | /album/create | 创建纪念册 + 选模板 |
| AlbumDetailPage | /album/:id | 纪念册详情（素材列表、邀请链接管理、生成按钮） |
| AlbumPreviewPage | /album/:id/preview | 翻页预览渲染后的 HTML |
| ShareUploadPage | /share/:inviteCode | 匿名上传页（移动端适配） |

#### 布局组件

| 组件 | 说明 |
|------|------|
| AppLayout | 已登录用户的主布局（顶部导航 + 内容区） |
| AuthLayout | 登录/注册页布局（居中卡片） |
| ShareLayout | 分享页布局（无导航，简洁） |

#### 通用组件

| 组件 | 说明 |
|------|------|
| AlbumCard | 纪念册卡片（名称、描述、状态、操作） |
| TemplateCard | 模板选择卡片（预览图、名称） |
| MediaGrid | 素材网格展示（图片缩略图） |
| MediaTextList | 文字素材列表 |
| InviteLinkItem | 邀请链接条目（链接、状态、复制按钮） |
| ImageUploader | 图片拖拽上传组件 |
| BookViewer | 纪念册翻页预览器（iframe + 翻页控制） |

### 接口数据结构

#### 认证相关

```typescript
// 注册请求
interface RegisterRequest {
  username: string;
  password: string;
  email: string;
}

// 登录请求
interface LoginRequest {
  username: string;
  password: string;
}

// 登录响应
interface LoginVo {
  token: string;        // StpUserUtil token（header: Ya-Auth）
  userId: number;
  username: string;
}
```

#### 纪念册相关

```typescript
// 创建纪念册请求
interface AlbumCreateRequest {
  name: string;
  des?: string;
  templateId?: number;
}

// 纪念册详情响应
interface AlbumDetailVo {
  id: number;
  name: string;
  des: string;
  templateId: number;
  templateName: string;
  status: number;       // 0草稿 1已发布
  isPublic: number;
  imageCount: number;   // 图片素材数
  textCount: number;    // 文字素材数
  pageCount: number;    // 已生成页数
  createAt: string;
}
```

#### 邀请链接相关

```typescript
// 生成邀请链接请求
interface InviteCreateRequest {
  albumId: number;
  accessCode?: string;  // 可选访问码
  expireHours: number;  // 过期时间（小时）
}

// 邀请链接响应
interface InviteVo {
  id: number;
  albumId: number;
  inviteCode: string;
  accessCode: string;
  status: number;
  expireAt: string;
  shareUrl: string;     // 完整分享 URL
}
```

#### 分享页相关

```typescript
// 验证邀请码响应
interface ShareInfoVo {
  albumName: string;
  albumDes: string;
  needAccessCode: boolean;  // 是否需要访问码
}

// 验证访问码响应
interface TokenVo {
  token: string;        // StpAnonUtil token（header: Ya-Anon-Auth）
  albumId: number;
  albumName: string;
}

// 匿名上传文字请求
interface UploadTextRequest {
  content: string;
}
```

#### 预览相关

```typescript
// 渲染后的页面
interface RenderedPageVo {
  pageId: number;
  sort: number;
  html: string;         // 渲染后的完整 HTML
}
```

## Data Models

### 已有数据表（无需修改）

所有表结构已存在，详见各 Entity 类。关键表：

- `ya_user` — 用户端用户
- `ya_album` — 纪念册
- `ya_invite` — 邀请链接
- `ya_invite_token` — 匿名上传者 token
- `ya_album_media` — 纪念册素材
- `ya_album_page` — 纪念册页面（data 字段存 Data JSON）
- `ya_template` — 模板套件
- `ya_template_page` — 模板页面（content 字段存 HTML 模板）
- `ya_template_schema` — Schema（content 字段存 slots JSON，imageCount/textCount 为独立字段）

### 关键数据流

```
ya_album_media (素材)
    ↓ 按 imageCount/textCount 分配
ya_template_page.content (HTML 模板) + Data JSON
    ↓ 渲染（替换 {{slot_id}}）
ya_album_page.data (存储 Data JSON) → 渲染后 HTML 返回前端
```

## Correctness Properties

### 属性 1: 用户名唯一性
注册时，如果 ya_user 表中已存在相同 username 的记录，注册请求应被拒绝。
**验证需求: 1.1, 1.3**

### 属性 2: Sa-Token 登录有效性
登录成功后 StpUserUtil 应正确创建 session 并返回有效 token，在有效期内可通过 StpUserUtil.checkLogin() 验证。
**验证需求: 1.2, 1.5**

### 属性 3: 纪念册归属校验
用户只能操作自己创建的纪念册，查询、更新、删除操作应校验 ya_album.user_id 等于当前用户 ID。
**验证需求: 2.1, 8.1, 8.3**

### 属性 4: invite_code 唯一性
生成的 invite_code 在 ya_invite 表中应唯一，不与任何已有记录重复。
**验证需求: 3.1, 3.2**

### 属性 5: 邀请链接有效性校验
通过 invite_code 访问时，应验证对应记录存在、status=1 且未过期（当前时间 < expire_at）。
**验证需求: 3.6, 4.1**

### 属性 6: 匿名 token 关联正确性
生成的 ya_invite_token 记录应正确关联 album_id 和 invite_id。
**验证需求: 4.3**

### 属性 7: 素材关联正确性
匿名上传的素材应正确关联 album_id 和 token_id，type 字段应与实际内容类型一致。
**验证需求: 4.4, 4.5**

### 属性 8: 生成前置条件校验
生成纪念册前应验证已关联模板且至少有 1 条 status=2 的素材。
**验证需求: 6.1, 6.5, 6.6**

### 属性 9: Data JSON 格式正确性
生成的 Data JSON 应为扁平 key-value 结构，key 遵循 `image_N`/`text_N` 命名规则，数量与 Schema 的 imageCount/textCount 一致。
**验证需求: 6.2d**

### 属性 10: 渲染完整性
渲染后的 HTML 不应包含未替换的 `{{...}}` 占位符（所有占位符都应被替换为实际值或空字符串）。
**验证需求: 7.1**

### 属性 11: 纪念册状态流转
生成完成后 ya_album.status 应更新为 1（发布），未生成时应保持 0（草稿）。
**验证需求: 6.4**

### 属性 12: 逻辑删除级联
删除纪念册时应同时逻辑删除关联的 ya_album_page 记录。
**验证需求: 8.3**

## Error Handling

### 认证错误

| 场景 | HTTP 状态码 | 错误信息 |
|------|------------|---------|
| 用户名已存在 | 400 | 用户名已存在 |
| 用户名或密码错误 | 400 | 用户名或密码错误 |
| 未携带 token | 401 | 请先登录 |
| token 过期 | 401 | 登录已过期，请重新登录 |
| 无权操作该纪念册 | 403 | 无权操作该纪念册 |

### 邀请链接错误

| 场景 | HTTP 状态码 | 错误信息 |
|------|------------|---------|
| invite_code 不存在 | 404 | 邀请链接不存在 |
| 邀请链接已禁用 | 400 | 该邀请链接已禁用 |
| 邀请链接已过期 | 400 | 链接已过期 |
| 访问码错误 | 400 | 访问码错误 |
| 匿名 token 过期 | 401 | 身份已过期，请重新通过邀请链接访问 |

### 生成纪念册错误

| 场景 | HTTP 状态码 | 错误信息 |
|------|------------|---------|
| 未关联模板 | 400 | 请先选择模板 |
| 无审核通过的素材 | 400 | 请先上传素材 |
| 模板页无关联 Schema | 500 | 模板配置异常，请联系管理员 |

## Testing Strategy

### 后端测试重点

1. **认证流程**: 注册 → 登录 → Sa-Token session 验证 → 接口鉴权
2. **邀请链接**: 生成唯一码 → 验证有效性 → 过期处理
3. **匿名上传**: token 生成 → 上传图片/文字 → 关联校验
4. **纪念册生成**: 素材分配算法 → Data JSON 生成 → HTML 渲染
5. **归属校验**: 用户只能操作自己的纪念册

### 前端测试重点

1. **路由守卫**: 未登录跳转登录页，已登录跳转首页
2. **分享页**: invite_code 验证 → 访问码输入 → 上传功能
3. **预览页**: iframe 渲染 → 翻页交互
