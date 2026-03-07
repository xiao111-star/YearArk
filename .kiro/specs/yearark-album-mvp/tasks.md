# 任务文档 - YearArk 纪念册 MVP

## 任务总览

基于 design.md，将 MVP 实现拆分为 7 个任务组。后端实体的基础 CRUD 已存在，本次聚焦于：
- 改造 3 个现有 Controller（Album、Invite、Media）为用户端接口
- 新建 3 个 Controller（Auth、Share、Template）
- 新增 4 个 Service（认证、匿名token、生成、渲染）
- 搭建用户端前端 yearark-web

---

## 任务组 1：后端 - 用户端认证体系

### Task 1.1: Sa-Token 多账号体系工具类（StpUserUtil + StpAnonUtil）
- [x] 创建 `StpUserUtil`，封装 ya_user 体系的 Sa-Token 操作（TYPE="ya-user"，内部持有独立 StpLogic 实例）
- [x] 创建 `StpAnonUtil`，封装匿名上传者体系的 Sa-Token 操作（TYPE="ya-anon"，内部持有独立 StpLogic 实例）
- [x] 每个工具类封装 login / checkLogin / getLoginIdAsLong / getTokenValue / getTokenSession / logout 等静态方法
- [x] 创建 `YaLoginUser`（userId, username）和 `YaAnonUser`（tokenId, albumId, inviteId）Session 数据模型
- [x] 创建 `YaLoginHelper`，参照管理端 LoginHelper 模式，封装 login / getLoginUser / getUserId / anonLogin / getAnonUser 方法
- **设计参考**: design.md - StpUserUtil、StpAnonUtil、YaLoginHelper
- **参照**: `ruoyi-ai/ruoyi-common/ruoyi-common-satoken/src/main/java/org/ruoyi/common/satoken/utils/LoginHelper.java`
- **文件**:
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/util/StpUserUtil.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/util/StpAnonUtil.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/model/YaLoginUser.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/model/YaAnonUser.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/util/YaLoginHelper.java`

### Task 1.2: YaUserAuthService 用户认证服务
- [x] 创建 `YaUserAuthService` 接口和实现类
- [x] `register(username, password, email)`：校验用户名唯一（Service 层校验） → BCrypt 加密 → 复用 IYaUserService 保存
- [x] `login(username, password)`：查询用户 → BCrypt 验证密码 → 构建 YaLoginUser → YaLoginHelper.login() → 返回 LoginVo（含 StpUserUtil.getTokenValue()）
- **需求**: 1.1, 1.2, 1.3, 1.4, 1.6
- **文件**:
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/YaUserAuthService.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/YaUserAuthServiceImpl.java`

### Task 1.3: InviteTokenAuthService 匿名 token 认证服务
- [x] 创建 `InviteTokenAuthService` 接口和实现类
- [x] `verifyAccessCode(inviteCode, accessCode)`：通过 IYaInviteService 查询 invite 记录，校验 accessCode
- [x] `generateToken(inviteCode, ipAddress)`：验证 invite_code 有效（存在、status=1、未过期） → 创建 ya_invite_token 记录（复用 IYaInviteTokenService） → 构建 YaAnonUser → YaLoginHelper.anonLogin() → 返回 TokenVo（含 StpAnonUtil.getTokenValue()）
- **需求**: 4.1, 4.2, 4.3, 4.7
- **文件**:
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/InviteTokenAuthService.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/InviteTokenAuthServiceImpl.java`

### Task 1.4: YaUserSecurityConfig（Sa-Token 路由拦截配置）
- [x] 创建 `YaUserSecurityConfig` 实现 `WebMvcConfigurer`
  - 注册 SaInterceptor 拦截 `/api/user/**`
  - 已登录用户接口（`/api/user/album/**`、`/api/user/invite/**`、`/api/user/media/**`、`/api/user/template/**`）→ StpUserUtil.checkLogin()
  - 匿名上传接口（`/api/user/share/upload/**`、`/api/user/share/my-uploads`）→ StpAnonUtil.checkLogin()
  - 公开接口（`/api/user/auth/**`、`/api/user/share/{code}`、`/api/user/share/{code}/verify`）→ 不拦截
- [x] 在 `application.yml` 的 `security.excludes` 中添加 `/api/user/**`，排除管理端 SecurityConfig 对用户端路径的拦截
- **需求**: 1.5, 1.6
- **设计参考**: design.md - YaUserSecurityConfig
- **参照**: `ruoyi-ai/ruoyi-common/ruoyi-common-security/src/main/java/org/ruoyi/common/security/config/SecurityConfig.java`
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/config/YaUserSecurityConfig.java`
  - `ruoyi-ai/ruoyi-admin/src/main/resources/application.yml`（修改 security.excludes）

---

## 任务组 2：后端 - 辅助 DTO/VO

### Task 2.1: 创建用户端专用 DTO 和 VO
- [x] `RegisterDto`：username, password, email
- [x] `LoginDto`：username, password
- [x] `LoginVo`：token, userId, username
- [x] `InviteCreateDto`：albumId, accessCode(可选), expireHours
- [x] `ShareInfoVo`：albumName, albumDes, needAccessCode
- [x] `TokenVo`：token, albumId, albumName
- [x] `UploadTextDto`：content
- [x] `MediaStatsVo`：imageCount, textCount
- [x] `RenderedPageVo`：pageId, sort, html
- **说明**: YaLoginUser、YaAnonUser 已在 Task 1.1 中创建，替代原 InviteTokenInfo
- **设计参考**: design.md - 接口数据结构
- **文件**:
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/RegisterDto.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/LoginDto.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/LoginVo.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/InviteCreateDto.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/ShareInfoVo.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/TokenVo.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/UploadTextDto.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/MediaStatsVo.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/RenderedPageVo.java`

---

## 任务组 3：后端 - Controller（新建 + 改造）

### Task 3.1: UserAuthController（新建）
- [x] 创建 `UserAuthController`，路径前缀 `/api/user/auth`
- [x] `POST /register`：接收 RegisterDto，调用 YaUserAuthService.register()
- [x] `POST /login`：接收 LoginDto，调用 YaUserAuthService.login()，返回 LoginVo
- **需求**: 1.1, 1.2, 1.3, 1.4
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/user/UserAuthController.java`

### Task 3.2: 改造 YaAlbumController
- [x] 路径从 `/yearark/album` 改为 `/api/user/album`
- [x] 移除 Sa-Token 管理端认证依赖，走 YaUserSecurityConfig（StpUserUtil 校验）
- [x] `POST /`：创建纪念册，从 YaLoginHelper.getUserId() 获取 userId 自动填充
- [x] `GET /list`：查询时自动注入 userId 条件（只返回当前用户的纪念册）
- [x] `GET /{id}`：纪念册详情，增加归属校验（album.userId == currentUserId）
- [x] `POST /update`：更新纪念册，增加归属校验
- [x] `DELETE /{id}`：删除纪念册，增加归属校验（改为单个删除）
- [x] 新增 `POST /{id}/generate`：校验归属 → 调用 AlbumGenerationService.generate()
- [x] 新增 `GET /{id}/preview`：校验归属 → 调用 TemplateRenderService.renderAlbum()
- **需求**: 2.1, 2.3, 6.1, 7.1, 8.1, 8.3
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/yearark/YaAlbumController.java`（改造）

### Task 3.3: 改造 YaInviteController
- [x] 路径从 `/yearark/invite` 改为 `/api/user/invite`
- [x] 移除 Sa-Token 管理端认证依赖，走 YaUserSecurityConfig（StpUserUtil 校验）
- [x] `POST /`：生成邀请链接，自动生成 6 位唯一 invite_code，根据 expireHours 计算 expire_at，校验纪念册归属
- [x] `GET /list`：查询某纪念册的邀请链接列表（query: albumId），校验纪念册归属
- [x] 新增 `POST /{id}/disable`：禁用邀请链接，校验归属
- [x] 移除不需要的通用 update 和批量删除接口
- **需求**: 3.1, 3.2, 3.3, 3.4, 3.6
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/yearark/YaInviteController.java`（改造）

### Task 3.4: UserTemplateController（新建）
- [x] 创建 `UserTemplateController`，路径前缀 `/api/user/template`
- [x] `GET /list`：查询所有 status 可用的模板列表（名称、预览图、描述），复用 IYaTemplateService
- [x] `GET /{id}`：模板详情（含模板页列表），复用 IYaTemplateService + IYaTemplatePageService
- **需求**: 2.2
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/user/UserTemplateController.java`

### Task 3.5: 改造 YaAlbumMediaController
- [x] 路径从 `/yearark/album-media` 改为 `/api/user/media`
- [x] 移除 Sa-Token 管理端认证依赖，走 YaUserSecurityConfig（StpUserUtil 校验）
- [x] `GET /list`：查询某纪念册的素材列表（query: albumId, type），校验纪念册归属
- [x] 新增 `GET /stats`：素材统计（图片数、文字数），校验纪念册归属
- [x] 移除新增/修改/删除接口（用户端素材通过 ShareController 匿名上传）
- **需求**: 5.1, 5.2, 5.3, 5.4
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/yearark/YaAlbumMediaController.java`（改造）

### Task 3.6: ShareController（新建）
- [x] 创建 `ShareController`，路径前缀 `/api/user/share`
- [x] `GET /{inviteCode}`：验证邀请码，返回 ShareInfoVo（纪念册名称、描述、是否需要访问码）
- [x] `POST /{inviteCode}/verify`：验证访问码 → 调用 InviteTokenAuthService.generateToken() → 返回 TokenVo（含 StpAnonUtil token）
- [x] `POST /upload/image`：从 YaLoginHelper.getAnonUser() 获取匿名身份 → 上传图片到 OSS → 创建 media 记录（type=2, status=2）
- [x] `POST /upload/text`：从 YaLoginHelper.getAnonUser() 获取匿名身份 → 创建 media 记录（type=1, status=2）
- [x] `GET /my-uploads`：从 YaLoginHelper.getAnonUser() 获取 tokenId → 查询该 token 上传的素材列表
- **需求**: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7, 4.8
- **文件**:
  - `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/user/ShareController.java`

---

## 任务组 4：后端 - 纪念册生成与渲染

### Task 4.1: TemplateRenderService 模板渲染服务
- [x] 创建 `TemplateRenderService` 接口和实现类
- [x] `renderPage(htmlTemplate, dataJson)`：解析 dataJson 为 Map → 遍历替换 `{{key}}` 为 value → 未匹配的占位符替换为空字符串
- [x] `renderAlbum(albumId)`：查询所有 album_page（按 sort 排序） → 关联查询 template_page 获取 HTML 模板 → 逐页调用 renderPage → 返回 RenderedPageVo 列表
- **需求**: 7.1
- **设计参考**: design.md - TemplateRenderService 渲染逻辑
- **文件**:
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/TemplateRenderService.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/TemplateRenderServiceImpl.java`

### Task 4.2: AlbumGenerationService 纪念册生成服务
- [x] 创建 `AlbumGenerationService` 接口和实现类
- [x] `generate(albumId)` 完整流程：
  - a. 验证纪念册已关联模板（templateId != null），否则抛出"请先选择模板"
  - b. 验证至少有 1 条 status=2 的素材，否则抛出"请先上传素材"
  - c. 读取模板的所有 TemplatePage（按 sort 排序）及关联的 Schema（获取 imageCount/textCount）
  - d. 收集图片素材（type=2, status=2）和文字素材（type=1, status=2），按 sort 排序
  - e. 生成前先清除已有的 album_page 记录（支持重新生成）
  - f. 按 design.md 中的素材分配算法，逐页分配素材生成 Data JSON
  - g. 为每页创建 ya_album_page 记录（albumId, templatePageId, data, sort）
  - h. 更新 ya_album.status = 1（发布）
- [x] 素材不足时使用默认占位图/空字符串
- **需求**: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7
- **设计参考**: design.md - AlbumGenerationService 素材分配算法
- **文件**:
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/AlbumGenerationService.java`
  - `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/AlbumGenerationServiceImpl.java`

---

## 任务组 5：前端 - yearark-web 项目搭建

### Task 5.1: 初始化 yearark-web 项目
- [x] 使用 Vite 创建 Vue3 + TypeScript 项目，目录 `yearark-web/`
- [x] 安装依赖：tailwindcss、shadcn-vue、vue-router、pinia、axios
- [x] 配置 Tailwind CSS
- [x] 配置 shadcn-vue 组件库
- [x] 配置 Vite 代理（开发环境代理 `/api` 到后端）
- **文件**:
  - `yearark-web/package.json`
  - `yearark-web/vite.config.ts`
  - `yearark-web/tailwind.config.js`
  - `yearark-web/tsconfig.json`
  - `yearark-web/src/main.ts`

### Task 5.2: 路由、布局和状态管理
- [x] 配置 Vue Router，定义所有路由（design.md 前端路由设计）
- [x] 创建三种布局：AppLayout（已登录，顶部导航+内容区）、AuthLayout（登录注册，居中卡片）、ShareLayout（分享页，无导航）
- [x] 实现路由守卫：未登录跳转 /login，已登录访问 /login 跳转 /dashboard，/share 不需要登录
- [x] 创建 `useUserStore`（Pinia）：管理用户登录状态、token、用户信息
- **需求**: 1.5
- **文件**:
  - `yearark-web/src/router/index.ts`
  - `yearark-web/src/layouts/AppLayout.vue`
  - `yearark-web/src/layouts/AuthLayout.vue`
  - `yearark-web/src/layouts/ShareLayout.vue`
  - `yearark-web/src/stores/user.ts`

### Task 5.3: Axios 封装和 API 层
- [x] 封装 Axios 实例，统一请求/响应拦截
  - 请求拦截：已登录用户自动添加 `Ya-Auth` header，匿名用户自动添加 `Ya-Anon-Auth` header
  - 响应拦截：401 跳转登录、400 提示错误信息
- [x] 创建 API 模块，对应 design.md 中的所有接口：
  - `auth.ts`：register、login
  - `album.ts`：create、list、detail、update、delete、generate、preview
  - `template.ts`：list、detail
  - `invite.ts`：create、list、disable
  - `media.ts`：list、stats
  - `share.ts`：verify、verifyAccessCode、uploadImage、uploadText、myUploads
- **文件**:
  - `yearark-web/src/utils/request.ts`
  - `yearark-web/src/api/auth.ts`
  - `yearark-web/src/api/album.ts`
  - `yearark-web/src/api/template.ts`
  - `yearark-web/src/api/invite.ts`
  - `yearark-web/src/api/media.ts`
  - `yearark-web/src/api/share.ts`

---

## 任务组 6：前端 - 页面实现

### Task 6.1: 登录和注册页
- [x] `LoginPage.vue`：用户名+密码表单，调用 login API，存储 token 到 localStorage 和 Pinia，跳转 /dashboard
- [x] `RegisterPage.vue`：用户名+密码+邮箱表单，调用 register API，注册成功跳转 /login
- [x] 使用 shadcn-vue 的 Card、Input、Button 组件
- **需求**: 1.1, 1.2, 1.3, 1.4
- **文件**:
  - `yearark-web/src/views/LoginPage.vue`
  - `yearark-web/src/views/RegisterPage.vue`

### Task 6.2: 纪念册列表页（Dashboard）
- [x] `DashboardPage.vue`：卡片式展示纪念册列表
- [x] `AlbumCard.vue` 组件：名称、描述、状态标签（草稿/已发布）、创建时间、操作按钮（详情/删除）
- [x] 删除前弹出确认对话框
- [x] 右上角"创建纪念册"按钮，跳转 /album/create
- **需求**: 8.1, 8.2, 8.3, 8.4
- **文件**:
  - `yearark-web/src/views/DashboardPage.vue`
  - `yearark-web/src/components/AlbumCard.vue`

### Task 6.3: 创建纪念册页
- [x] `AlbumCreatePage.vue`：名称输入+描述输入+模板选择
- [x] `TemplateCard.vue` 组件：卡片式展示模板预览图，点击选中高亮
- [x] 名称为空时阻止提交
- [x] 提交创建 → 跳转到纪念册详情页 /album/:id
- **需求**: 2.1, 2.2, 2.3, 2.4, 2.5
- **文件**:
  - `yearark-web/src/views/AlbumCreatePage.vue`
  - `yearark-web/src/components/TemplateCard.vue`

### Task 6.4: 纪念册详情页
- [x] `AlbumDetailPage.vue`：展示纪念册信息、素材列表、邀请链接管理、操作按钮
- [x] 素材区域：`MediaGrid.vue`（图片网格）+ `MediaTextList.vue`（文字列表）+ 统计信息
- [x] 邀请链接区域：`InviteLinkItem.vue`（链接、状态、复制按钮、禁用按钮）+ 生成新链接按钮
- [x] 操作区域："生成纪念册"按钮、"预览纪念册"按钮
- **需求**: 3.3, 3.4, 3.5, 5.1, 5.2, 5.3, 5.4, 6.1
- **文件**:
  - `yearark-web/src/views/AlbumDetailPage.vue`
  - `yearark-web/src/components/MediaGrid.vue`
  - `yearark-web/src/components/MediaTextList.vue`
  - `yearark-web/src/components/InviteLinkItem.vue`

### Task 6.5: 纪念册预览页
- [x] `AlbumPreviewPage.vue`：翻页浏览渲染后的 HTML
- [x] `BookViewer.vue` 组件：使用 iframe 渲染后端返回的 HTML 字符串 + 上一页/下一页导航 + 页码指示
- [x] 未生成时显示"纪念册尚未生成，请先点击生成"提示
- **需求**: 7.1, 7.2, 7.3, 7.4
- **文件**:
  - `yearark-web/src/views/AlbumPreviewPage.vue`
  - `yearark-web/src/components/BookViewer.vue`

### Task 6.6: 分享上传页
- [x] `ShareUploadPage.vue`：匿名用户上传素材
- [x] 访问流程：验证 inviteCode → 需要访问码则弹出输入框 → 获取匿名 token 存入 localStorage → 进入上传页
- [x] `ImageUploader.vue` 组件：图片拖拽上传
- [x] 文字输入框提交留言
- [x] 展示当前匿名用户已上传的素材列表
- [x] 使用 Tailwind 响应式断点适配移动端（此页面需移动端适配）
- **需求**: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8
- **文件**:
  - `yearark-web/src/views/ShareUploadPage.vue`
  - `yearark-web/src/components/ImageUploader.vue`

---

## 任务组 7：数据准备

### Task 7.1: 录入模板数据到数据库
- [x] 创建 SQL 脚本，将 5 套 HTML 模板样例录入数据库
- [x] 创建 ya_template 记录（紫色毕业主题套件）
- [x] 创建 ya_template_schema 记录（5 种 Schema：1图2文、2图1文、3图1文、1图1文、1图1文）
- [x] 创建 ya_template_page 记录（5 个模板页，关联 template 和 schema）
- [x] 将 `templates/` 目录下的 HTML 内容填入 ya_template_page.content
- **文件**:
  - `sql/yearark-template-data.sql`

---

## 执行顺序

```
Phase 1 - 后端基础设施
  Task 2.1 (DTO/VO) → Task 1.1 (Sa-Token多账号工具类+YaLoginHelper) → Task 1.2 (用户认证) → Task 1.3 (匿名token认证) → Task 1.4 (Sa-Token路由拦截配置)

Phase 2 - 后端 Controller
  Task 3.1 (Auth新建) → Task 3.2 (Album改造) → Task 3.3 (Invite改造) → Task 3.4 (Template新建) → Task 3.5 (Media改造) → Task 3.6 (Share新建)

Phase 3 - 后端生成与渲染
  Task 4.1 (渲染服务) → Task 4.2 (生成服务)

Phase 4 - 前端搭建
  Task 5.1 (项目初始化) → Task 5.2 (路由布局Store) → Task 5.3 (API层)

Phase 5 - 前端页面
  Task 6.1 (登录注册) → Task 6.2 (纪念册列表) → Task 6.3 (创建纪念册) → Task 6.4 (纪念册详情) → Task 6.5 (预览) → Task 6.6 (分享上传)

Phase 6 - 数据准备（可与 Phase 1-3 并行）
  Task 7.1 (模板数据SQL)
```
