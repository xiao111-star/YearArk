# YearArk — 基于 AI 的毕业纪念册自动生成系统 设计文档

## 一、项目概述

YearArk 是一个毕业设计项目，核心目标是通过 AI 技术实现毕业纪念册的自动化生成。用户创建纪念册后，通过邀请链接让同学上传照片和留言，系统利用 AI 视觉模型对素材进行智能分组、文案生成，最终自动排版成一本完整的电子纪念册，支持在线预览、可视化编辑和导出。

### 核心流程

```
用户注册登录 → 创建纪念册 → 选择模板 → 生成邀请链接 → 分享给同学
                                                          ↓
                                              同学通过链接匿名上传照片/文字
                                                          ↓
用户查看素材 ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
    ↓
点击"生成纪念册"
    ↓
Java 后端收集素材 → RabbitMQ → Python AI 服务
    ↓                              ↓
    ↓                    视觉分组 → 模板匹配 → 文案生成
    ↓                              ↓
    ← ← ← ← ← ← ← ← ← ← ← ← ←
    ↓
Schema 校验 → 渲染 HTML → 存储页面数据
    ↓
用户在线预览 → 可视化编辑（调整图片焦点/缩放、修改文案、替换素材）→ 导出
```

---

## 二、系统架构

### 2.1 整体架构

系统采用前后端分离 + 微服务架构，由四个子项目组成：

```
YearArk/
├── ruoyi-admin/     # 管理端前端（Vben Admin + Ant Design Vue）
├── ruoyi-ai/        # Java 后端（Spring Boot 3.4）
├── yearark-web/     # 用户端前端（Vue3 + Tailwind CSS + shadcn-vue）
└── yearark-ai/      # Python AI 服务（FastAPI）
```

### 2.2 架构图

```
┌──────────────────────┐    ┌──────────────────────┐
│   管理端前端            │    │   用户端前端            │
│   ruoyi-admin          │    │   yearark-web          │
│   Vue3 + Ant Design    │    │   Vue3 + shadcn-vue    │
│   模板/Schema/用户管理  │    │   纪念册创建/预览/编辑  │
└──────────┬───────────┘    └──────────┬───────────┘
           │ /yearark/**                │ /api/user/**
           ▼                            ▼
┌─────────────────────────────────────────────────────┐
│              Java 后端 (ruoyi-ai)                     │
│         Spring Boot 3.4 + MyBatis-Plus               │
│                                                       │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │ Sa-Token 三套 │  │ 业务 Service  │  │ MQ 通信     │  │
│  │ 独立鉴权体系  │  │ 层（核心逻辑）│  │ RabbitMQ   │  │
│  └─────────────┘  └──────────────┘  └─────┬──────┘  │
│                                            │         │
└────────────────────────────────────────────┼─────────┘
           │                                 │
     ┌─────▼─────┐                    ┌──────▼──────┐
     │  MySQL 8.0 │                    │ Python AI   │
     │  + Redis   │                    │ FastAPI     │
     │  + OSS     │                    │ yearark-ai  │
     └───────────┘                    └─────────────┘
```

### 2.3 技术栈

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| 用户端前端 | Vue3 + Vite + TypeScript + Tailwind CSS + shadcn-vue + Pinia | 面向终端用户 |
| 管理端前端 | Vue3 + TypeScript + Ant Design Vue（Vben Admin 框架） | 面向管理员 |
| Java 后端 | Spring Boot 3.4 + MyBatis-Plus + Sa-Token + RabbitMQ | 核心业务逻辑 |
| Python AI 服务 | FastAPI + aio-pika + OpenAI SDK（阿里云百炼） | AI 智能处理 |
| 数据库 | MySQL 8.0（utf8mb4） | 结构化数据存储 |
| 缓存 | Redis | 登录状态、Token 管理 |
| 消息队列 | RabbitMQ | Java ↔ Python 异步通信 |
| 文件存储 | 阿里云 OSS | 图片、PDF 等文件 |
| 部署 | Docker + K8s | 容器化部署 |

### 2.4 中间件职责

| 组件 | 用途 |
|------|------|
| MySQL 8.0 | 所有业务数据的持久化存储 |
| Redis | Sa-Token 三套身份体系的 Token 存储、登录状态缓存 |
| RabbitMQ | Java 后端与 Python AI 服务之间的异步任务通信（生成请求/结果） |
| 阿里云 OSS | 用户上传的图片、模板预览图、生成的 PDF 等文件存储 |

---

## 三、三端职责划分

### 3.1 管理端前端（ruoyi-admin）

基于 Vben Admin 框架，面向系统管理员：
- 模板套件管理（ya_template）：创建、编辑、删除模板，上传预览图
- 模板页面管理（ya_template_page）：H5 模板字符串编辑，关联 Schema
- JSON Schema 管理（ya_template_schema）：定义模板页的插槽结构
- 用户管理（ya_user）：查看和管理用户端用户
- 系统配置与监控

### 3.2 用户端前端（yearark-web）

面向终端用户的 SPA 应用：
- 用户注册/登录
- 创建纪念册、选择模板
- 生成邀请链接，同学通过链接匿名上传图片/文字
- 一键生成纪念册（触发 AI 处理）
- 在线翻页预览
- 可视化编辑器（修改文案、替换图片、调整焦点和缩放）
- 导出 PDF

### 3.3 Java 后端（ruoyi-ai）

基于 RuoYi-AI 开源框架，承担所有核心业务逻辑：
- 三套独立的 Sa-Token 鉴权体系（管理端 / 用户端 / 匿名用户）
- 纪念册 CRUD、邀请链接管理、素材管理
- 纪念册生成编排（收集素材 → 发送 MQ → 接收结果 → Schema 校验 → 渲染）
- 模板渲染服务（HTML 占位符替换 + 图片焦点/缩放样式注入）
- 页面编辑保存与重新渲染
- 文件上传到 OSS

### 3.4 Python AI 服务（yearark-ai）

通过 RabbitMQ 与 Java 后端异步通信，负责 AI 智能处理：
- 视觉分组：一次视觉 LLM 调用，分析所有图片并按场景/事件分组，同时判断每张图的视觉焦点
- 模板匹配：根据分组结果和模板页的 imageCount/textCount 进行智能匹配，加权随机 + 桶内轮转保证多样性
- 文案生成：两轮并发策略——先生成内容页文案，再携带内容页文案生成章节页和封面页文案
- 结果组装：输出每页的 Data JSON，通过 MQ 返回给 Java 后端

---

## 四、数据库设计

### 4.1 核心业务表（ya_ 前缀）

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| ya_user | 用户端用户（独立于管理端 sys_user） | username, password_hash, email, avatar_url, status |
| ya_album | 纪念册主表 | name, des, user_id, template_id, status(0草稿/1发布), generation_status(0待生成/1生成中/2完成/3失败), is_public |
| ya_album_media | 纪念册素材 | album_id, token_id, type(1文本/2图片), content, sort, faces_count, tags, status(-1不通过/0待审核/1审核中/2通过) |
| ya_album_page | 纪念册页面（生成结果） | album_id, template_page_id, sort, data(JSON) |
| ya_invite | 邀请链接 | album_id, invite_code(6位随机串), access_code, status, expire_at |
| ya_invite_token | 匿名上传者虚拟身份 | album_id, invite_id, token, ip_address, expired_at |
| ya_template | 模板套件 | name, type(字典), preview_url, des(给AI看), status |
| ya_template_page | 套件内的页面模板 | template_id, template_schema_id, content(H5字符串), preview_url, type |
| ya_template_schema | 页面模板的 JSON Schema | name, content(JSON), image_count, text_count, status |

### 4.2 实体关系

```
ya_user 1──N ya_album（一个用户创建多个纪念册）
ya_album N──1 ya_template（纪念册选择一个模板套件）
ya_album 1──N ya_album_media（一个纪念册有多个素材）
ya_album 1──N ya_album_page（一个纪念册有多个页面）
ya_album 1──N ya_invite（一个纪念册可生成多个邀请链接）
ya_invite 1──N ya_invite_token（一个邀请可产生多个匿名 token）
ya_template 1──N ya_template_page（一个套件包含多个页面模板）
ya_template_page N──1 ya_template_schema（页面模板对应一个 Schema）
ya_album_page N──1 ya_template_page（纪念册页面使用某个页面模板）
```

### 4.3 开发约定

- Java 版本：17
- 逻辑删除字段：`is_delete`（0 存在，2 删除）
- 时间字段：`create_at` / `update_at`（DATETIME）
- 后端使用 MyBatis-Plus Service 层方法，不使用 Mapper 层代码
- 参数校验优先在实体类中进行，若无法进行则在 Service 层完成
- 数据库字符集：utf8mb4（utf8mb4_0900_ai_ci）

---

## 五、鉴权架构

### 5.1 三套独立身份体系

系统使用 Sa-Token 框架，配置了三套完全独立的 StpLogic 实例：

| 身份体系 | 工具类 | loginType | Token Header | 用户表 | 适用场景 |
|---------|--------|-----------|-------------|--------|---------|
| 管理端 | StpUtil（默认） | login | Authorization | sys_user | 管理后台操作 |
| 用户端已登录用户 | StpUserUtil | ya-user | Ya-Auth | ya_user | 纪念册创建/管理 |
| 匿名上传者 | StpAnonUtil | ya-anon | Ya-Anon-Auth | ya_invite_token | 通过邀请链接上传素材 |

三套体系的 Token 都存在同一个 Redis 实例中，通过 key 前缀区分：
- 管理端：`satoken:login:token:xxx`
- 用户端：`satoken:ya-user:login:token:xxx`
- 匿名端：`satoken:ya-anon:login:token:xxx`

### 5.2 拦截器设计

```
请求进入
  │
  ├── /yearark/**（管理端接口）
  │     → SecurityConfig 拦截器 → StpUtil.checkLogin() → 读 Authorization header
  │     → @SaCheckPermission 注解控制细粒度权限
  │
  ├── /api/user/auth/**（公开）→ 不拦截
  ├── /api/user/share/{code}（公开）→ 不拦截
  │
  ├── /api/user/album/**、/api/user/invite/**、/api/user/media/**、/api/user/template/**
  │     → YaUserSecurityConfig 拦截器 → StpUserUtil.checkLogin() → 读 Ya-Auth header
  │
  └── /api/user/share/upload/**、/api/user/share/my-uploads
        → YaUserSecurityConfig 拦截器 → StpAnonUtil.checkLogin() → 读 Ya-Anon-Auth header
```

关键设计点：
- YaUserSecurityConfig 设置 `isAnnotation = false`，禁用默认注解鉴权，避免触发管理端的 StpUtil
- SecurityConfig 的 excludePathPatterns 排除 `/api/user/**`，避免管理端拦截器干扰用户端请求
- Sa-Token 全局配置 `token-prefix: "Bearer"`，所有体系的前端发送 token 时必须加 `Bearer ` 前缀

### 5.3 登录流程

**用户端登录：**
```
前端 POST /api/user/auth/login → BCrypt 验证密码 → StpUserUtil.login(userId)
→ Sa-Token 生成 token 存入 Redis → 返回 token + userId + username
→ 前端存入 localStorage，后续请求通过 axios 拦截器放入 Ya-Auth header
```

**匿名用户登录：**
```
访客 GET /api/user/share/{inviteCode} → 获取纪念册信息
访客 POST /api/user/share/{inviteCode}/verify → 验证访问码
→ 创建 ya_invite_token 记录 → StpAnonUtil.login(tokenId)
→ 返回 token → 前端存入 localStorage，后续请求放入 Ya-Anon-Auth header
```

---

## 六、模板系统设计

### 6.1 核心概念

一个"模板页"由两部分组成：

| 组成 | 存储位置 | 作用 |
|------|---------|------|
| Schema JSON | ya_template_schema.content | 声明模板页需要哪些素材（几张图、几段文字） |
| HTML 模板 | ya_template_page.content | 带占位符的 H5 页面，定义视觉布局 |

**渲染流程：** Data JSON → Schema 校验 → 替换 HTML 占位符 → 输出完整 HTML

### 6.2 Slot 命名规则

所有占位符采用**类型 + 编号**格式，不允许语义化命名：

| 类型 | 格式 | 示例 |
|------|------|------|
| 图片 | image_N（N 从 1 开始） | image_1、image_2 |
| 文字 | text_N（N 从 1 开始） | text_1、text_2 |

这样设计的原因：同一个 Schema 下的多个模板页可以互相替换，编号式命名保证 Data JSON 无需修改即可切换模板。

### 6.3 Schema JSON 格式

```json
{
  "slots": [
    {
      "id": "image_1",
      "type": "image",
      "label": "封面照片",
      "required": true,
      "width": 800,
      "height": 600
    },
    {
      "id": "text_1",
      "type": "text",
      "label": "标题",
      "required": true,
      "maxLength": 20,
      "default": "默认标题"
    }
  ]
}
```

`image_count` 和 `text_count` 作为数据库独立字段存储，方便 SQL 快速匹配，无需解析 JSON。

### 6.4 Data JSON 格式

image slot 值为对象格式（含焦点和缩放），text slot 值为字符串：

```json
{
  "image_1": {
    "url": "https://oss.example.com/photo.jpg",
    "focus_x": 0.5,
    "focus_y": 0.3,
    "scale": 1.2
  },
  "text_1": "标题文字"
}
```

向后兼容：image slot 值为纯字符串时，视为 URL，默认 focus_x=0.5, focus_y=0.5, scale=1.0。

### 6.5 HTML 模板规范

- 使用 `{{slot_id}}` 作为占位符
- 图片必须使用 `<img>` 标签，不能用 CSS background-image
- 图片容器需设置 `overflow: hidden`
- 渲染时系统自动注入焦点和缩放样式：`object-position` + `transform: scale()`
- 推荐设计尺寸：750px × 1000px

---

## 七、纪念册生成管线（核心功能）

### 7.1 整体流程

```
用户点击"生成" → Java 后端
    │
    ├── 1. 前置校验（已关联模板、有审核通过的素材）
    ├── 2. 更新 generation_status = processing
    ├── 3. 收集素材 + 模板页 + Schema 信息
    ├── 4. 组装 MQ 消息，发布到 album.generation.request 队列
    │
    │         ┌─── RabbitMQ ───┐
    │         │                │
    │         ▼                │
    │   Python AI 服务          │
    │     ├── Step1: 视觉分组    │
    │     ├── Step2: 模板匹配    │
    │     ├── Step3: 文案生成    │
    │     ├── Step4: 组装结果    │
    │     └── 发布到 album.generation.result 队列
    │                          │
    │         ◄────────────────┘
    │
    ├── 5. 接收结果，通过 correlationId 匹配
    ├── 6. 对每页 Data JSON 进行 Schema 校验
    ├── 7. 校验通过：清除旧页面，批量插入新 album_page，更新状态为 completed
    └── 8. 校验失败：更新状态为 failed，记录失败原因
```

### 7.2 RabbitMQ 通信协议

| 名称 | 类型 | 说明 |
|------|------|------|
| yearark.album | Direct Exchange | 纪念册生成消息的 Exchange |
| album.generation.request | Queue | Java → Python 的生成请求队列 |
| album.generation.result | Queue | Python → Java 的生成结果队列 |

消息 TTL：5 分钟，超时后 Java 将状态更新为 failed。

**请求消息（Java → Python）：**
```json
{
  "correlationId": "uuid-v4",
  "albumId": 123,
  "mediaList": [
    { "id": 1, "type": 2, "content": "https://oss/photo.jpg", "sort": 1 },
    { "id": 2, "type": 1, "content": "一段文字", "sort": 1 }
  ],
  "templatePages": [
    {
      "templatePageId": 10,
      "schemaId": 5,
      "imageCount": 2,
      "textCount": 1,
      "pageTypeName": "内容页",
      "schemaContent": "{\"slots\":[...]}"
    }
  ]
}
```

**结果消息（Python → Java）：**
```json
{
  "correlationId": "uuid-v4",
  "albumId": 123,
  "status": "success",
  "pages": [
    {
      "templatePageId": 10,
      "dataMap": {
        "image_1": { "url": "https://oss/photo.jpg", "focus_x": 0.5, "focus_y": 0.35, "scale": 1.0 },
        "text_1": "AI 生成的文案"
      }
    }
  ],
  "errorMessage": null
}
```

### 7.3 Python AI 服务处理流程

AI 服务采用 Pipeline 模式，四步编排：

**Step 1 — 视觉分组（OutlineService）：**
- 将所有图片一次性传给视觉 LLM（阿里云百炼 qwen3.5-plus）
- LLM 同时完成：理解每张图片内容 → 按场景/事件分成 2~5 个章节 → 判断每张图的视觉焦点（focus_x, focus_y）
- 输出：AlbumOutline（包含 album_title、chapters、每章的图片列表和焦点坐标）

**Step 2 — 模板匹配（MatchingService）：**
- 将模板页按类型分类：封面页、章节页、内容页、纯文字页
- 封面页：取第一个封面模板，分配前 N 张图片
- 章节页：每章分配一个章节模板
- 内容页：按 imageCount 分桶，加权随机选桶 + 桶内轮转，保证模板多样性且连续两页不重复
- 纯文字页：直接分配纯文字模板

**Step 3 — 文案生成（TextService）：**
- 两轮并发策略：
  - 第一轮：所有内容页并发调用 LLM 生成文案（携带章节上下文和图片）
  - 第二轮：封面页和章节页并发生成（携带第一轮生成的内容页文案作为参考，确保概括性文案贴合实际内容）
- 章节页和封面页使用视觉模型（传入本章/全书代表性图片），内容页使用纯文本模型
- 纯文字页直接用用户上传的文字素材填充

**Step 4 — 组装结果：**
- 按 封面 → [章节页 + 内容页×N] × 章节数 → 纯文字页 的顺序组装
- 每页输出 templatePageId + dataMap

### 7.4 生成状态管理

```
pending（待生成）→ processing（生成中）→ completed（生成完成）
                                      → failed（生成失败/超时/校验失败）
failed → processing（重新生成，用最新素材重发）
completed → processing（重新生成）
```

---

## 八、模板渲染服务

### 8.1 渲染逻辑

TemplateRenderService 负责将 HTML 模板中的 `{{slot_id}}` 替换为 Data JSON 中的实际值：

- text slot：直接替换为字符串
- image slot：替换 `{{image_N}}` 为图片 URL，并在对应 `<img>` 标签注入样式：
  ```css
  object-fit: cover;
  object-position: {focus_x*100}% {focus_y*100}%;
  transform: scale({scale});
  transform-origin: {focus_x*100}% {focus_y*100}%;
  ```

### 8.2 Schema 校验

SchemaValidator 在渲染前对 Data JSON 进行校验：
1. required=true 的 slot 必须存在且值非空
2. type=text 的 slot 校验 maxLength
3. type=image 的 slot 校验 URL 格式合法性
4. 缺失的非必填 slot 使用 default 值填充

---

## 九、可视化编辑器

### 9.1 功能概述

用户在纪念册生成后可进入编辑模式，对每一页进行微调：
- 文案编辑：点击 text slot 直接修改文字
- 图片替换：从素材库拖入新图片替换现有图片
- 图片拖拽交换：两个 image slot 之间拖拽交换
- 焦点调整：拖动十字准星调整 focus_x/focus_y
- 缩放调整：滚轮或滑块调整 scale
- 自动保存：编辑后自动保存到后端

### 9.2 编辑保存流程

```
用户修改 → 前端更新 Data JSON → PUT /api/user/album/page/{pageId}
→ 后端 Schema 校验 → 校验通过：更新 data 字段 + 重新渲染 → 返回新 HTML
                    → 校验失败：返回错误列表，数据库不变
```

支持批量更新：`PUT /api/user/album/{id}/pages`，事务内批量校验和更新，任一页失败则整体回滚。

---

## 十、API 接口设计

### 10.1 用户端接口（/api/user/**）

**认证相关（公开）：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/auth/register | 用户注册 |
| POST | /api/user/auth/login | 用户登录 |

**纪念册管理（需登录 Ya-Auth）：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/album | 创建纪念册 |
| GET | /api/user/album/list | 我的纪念册列表 |
| GET | /api/user/album/{id} | 纪念册详情 |
| POST | /api/user/album/update | 更新纪念册 |
| DELETE | /api/user/album/{id} | 删除纪念册 |
| POST | /api/user/album/{id}/generate | 触发生成 |
| GET | /api/user/album/{id}/status | 获取生成状态 |
| GET | /api/user/album/{id}/preview | 预览（返回渲染后 HTML 列表） |
| GET | /api/user/album/{id}/edit-data | 获取编辑数据（Data JSON + Schema + HTML） |
| PUT | /api/user/album/page/{pageId} | 更新单页 Data JSON |
| PUT | /api/user/album/{id}/pages | 批量更新多页 |
| GET | /api/user/album/{id}/unused-media | 未使用的素材列表 |
| POST | /api/user/album/{id}/media/upload | 登录用户上传图片到素材库 |

**模板查询（需登录 Ya-Auth）：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/template/list | 可用模板列表 |
| GET | /api/user/template/{id} | 模板详情 |

**邀请链接管理（需登录 Ya-Auth）：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/invite | 生成邀请链接 |
| GET | /api/user/invite/list | 邀请链接列表 |
| POST | /api/user/invite/{id}/disable | 禁用邀请链接 |

**素材查看（需登录 Ya-Auth）：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/media/list | 素材列表 |
| GET | /api/user/media/stats | 素材统计 |

**分享页（公开 + 匿名 Ya-Anon-Auth）：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/share/{inviteCode} | 验证邀请码（公开） |
| POST | /api/user/share/{inviteCode}/verify | 验证访问码（公开） |
| POST | /api/user/share/upload/image | 匿名上传图片 |
| POST | /api/user/share/upload/text | 匿名上传文字 |
| GET | /api/user/share/my-uploads | 匿名用户已上传素材 |

### 10.2 管理端接口（/yearark/**）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/DELETE | /yearark/template/** | 模板套件 CRUD |
| GET/POST/DELETE | /yearark/template-page/** | 模板页面 CRUD |
| GET/POST/DELETE | /yearark/template-schema/** | JSON Schema CRUD |
| GET/POST/DELETE | /yearark/user/** | 用户管理 |

管理端接口通过 `@SaCheckPermission` 注解控制权限（如 `yearark:template:add`）。

---

## 十一、前端路由设计

### 11.1 用户端路由（yearark-web）

| 路由 | 页面 | 认证要求 |
|------|------|---------|
| / | 首页 | 无 |
| /login | 登录页 | guest only |
| /register | 注册页 | guest only |
| /dashboard | 纪念册列表 | 需登录 |
| /album/create | 创建纪念册 | 需登录 |
| /album/:id | 纪念册详情 | 需登录 |
| /album/:id/preview | 纪念册预览 | 需登录 |
| /album/:id/edit | 可视化编辑器 | 需登录 |
| /templates | 模板浏览 | 需登录 |
| /shared | 分享管理 | 需登录 |
| /profile | 个人中心 | 需登录 |
| /share/:inviteCode | 匿名上传页 | 无 |
| /view/:id | 公开纪念册浏览 | 无 |

### 11.2 管理端路由（ruoyi-admin）

| 路由 | 页面 |
|------|------|
| /yearark/template | 模板套件列表 |
| /yearark/template/detail/:id | 模板详情（含模板页管理） |
| /yearark/template-schema | JSON Schema 管理 |

---

## 十二、后端核心 Service 设计

### 12.1 Service 层架构

所有业务逻辑在 Service 层实现，使用 MyBatis-Plus 的 IService 方法操作数据库，不直接使用 Mapper 层。

**已有基础 Service（CRUD）：**
- IYaUserService、IYaAlbumService、IYaInviteService、IYaInviteTokenService
- IYaAlbumMediaService、IYaAlbumPageService
- IYaTemplateService、IYaTemplatePageService、IYaTemplateSchemaService

**新增业务 Service：**

| Service | 职责 |
|---------|------|
| AlbumGenerationService | 纪念册生成编排：前置校验 → 收集素材 → 发送 MQ → 接收结果 → Schema 校验 → 存储页面 |
| TemplateRenderService | 模板渲染：HTML 占位符替换 + 图片焦点/缩放样式注入 |
| AlbumPageEditService | 页面编辑：单页/批量更新 Data JSON，Schema 校验 + 重新渲染 |
| SchemaValidatorUtil | Schema 校验工具：验证 Data JSON 是否符合 Schema 定义 |
| IAnonUserService | 匿名用户认证：邀请码验证 → 生成匿名 token → StpAnonUtil 登录 |
| YaUserAuthService（隐含） | 用户认证：注册（BCrypt 加密）→ 登录（StpUserUtil 登录） |

### 12.2 Controller 层结构

```
controller/
├── user/                          # 用户端接口（/api/user/**）
│   ├── UserAuthController         # 注册登录（公开）
│   ├── UserAlbumController        # 纪念册管理 + 生成 + 编辑（StpUserUtil）
│   ├── UserTemplateController     # 模板查询（StpUserUtil）
│   ├── YaInviteController         # 邀请链接管理（StpUserUtil）
│   ├── YaAlbumMediaController     # 素材查看（StpUserUtil）
│   └── AnonUserController         # 匿名上传（公开 + StpAnonUtil）
│
└── yearark/                       # 管理端接口（/yearark/**）
    ├── YaTemplateController       # 模板套件 CRUD
    ├── YaTemplatePageController   # 模板页面 CRUD
    ├── YaTemplateSchemaController # Schema CRUD
    ├── YaUserController           # 用户管理
    └── PublicAlbumController      # 公开纪念册
```

---

## 十三、Python AI 服务架构

### 13.1 项目结构

```
yearark-ai/
├── main.py                  # FastAPI 入口，启动 MQ 消费者
├── config.py                # 全局配置（MQ 连接、LLM 模型等）
├── pipeline/
│   └── album_pipeline.py    # 生成管线编排（4步流程）
├── service/
│   ├── outline_service.py   # 视觉分组服务（1次视觉LLM调用）
│   ├── matching_service.py  # 模板匹配服务（纯算法）
│   └── text_service.py      # 文案生成服务（两轮并发LLM调用）
├── core/
│   ├── mq/
│   │   ├── consumer.py      # MQ 消费者（带自动重连）
│   │   └── publisher.py     # MQ 发布者（带重试机制）
│   └── llm/
│       └── client.py        # LLM 客户端封装
├── domain/
│   ├── request.py           # 请求模型（Pydantic）
│   ├── result.py            # 结果模型
│   └── outline.py           # 大纲数据模型
└── requirements.txt
```

### 13.2 关键配置

- LLM 模型：阿里云百炼 qwen3.5-plus（视觉 + 文本）
- MQ 消费者：prefetch_count=1，带自动重连（5秒间隔）
- MQ 发布者：最多重试 3 次（5秒间隔）
- 文案生成并发：ThreadPoolExecutor max_workers=8
- 健康检查：GET /health

---

## 十四、技术亮点

### 14.1 AI 视觉一步到位

传统方案需要多次调用 AI（分类 → 分组 → 焦点检测），本项目通过精心设计的 Prompt，一次视觉 LLM 调用同时完成图片理解、场景分组、章节命名和焦点坐标判断，大幅减少 API 调用次数和延迟。

### 14.2 两轮文案生成策略

第一轮并发生成所有内容页文案，第二轮携带第一轮结果生成章节页和封面页文案。这确保了概括性文案（章节标题、封面标题）能准确反映实际内容，而非凭空想象。

### 14.3 模板匹配多样性算法

按 imageCount 分桶 + 加权随机选桶 + 桶内 shuffle 轮转，保证：
- 同一本纪念册中模板页不会单调重复
- 连续两页尽量使用不同布局（soft anti-repeat）
- 模板种类多的桶被选中概率更高

### 14.4 Schema 驱动的模板可替换性

统一的 `image_N`/`text_N` 命名规则 + Schema 作为模板与数据之间的契约，使得：
- 同 Schema 下的模板页可以无缝替换，Data JSON 完全不变
- AI 生成的数据格式统一，校验规则明确
- 前端编辑器可以根据 Schema 动态渲染编辑控件

### 14.5 三套独立鉴权体系

Sa-Token 多账号体系实现管理端、用户端、匿名用户三套完全隔离的认证，Token 互不干扰，安全性高。匿名用户通过邀请链接获取临时身份，无需注册即可上传素材。

### 14.6 异步生成 + 状态轮询

纪念册生成通过 RabbitMQ 异步处理，前端通过轮询 generation_status 获取进度。Java 和 Python 服务完全解耦，可独立扩缩容。MQ 消息设置 5 分钟 TTL，超时自动标记失败。

---

## 十五、错误处理

| 场景 | 处理方式 |
|------|---------|
| 纪念册未关联模板 | 抛出 ServiceException("请先选择模板") |
| 无审核通过素材 | 抛出 ServiceException("请先上传素材") |
| MQ 消息超时（5分钟） | 更新 generation_status = failed |
| Python 返回 status=failed | 更新 generation_status = failed，记录 errorMessage |
| Schema 校验失败（生成时） | 更新 generation_status = failed，用户可重新生成 |
| Schema 校验失败（编辑保存时） | 返回校验错误列表，数据库不变 |
| 页面归属校验失败 | 抛出 ServiceException("无权操作") |
| 批量更新部分校验失败 | 整个批量操作回滚 |
| 邀请链接已过期 | 返回"链接已过期" |
| 匿名 token 过期 | 返回"身份已过期，请重新通过邀请链接访问" |
