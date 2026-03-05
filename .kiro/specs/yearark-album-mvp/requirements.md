# 需求文档：YearArk 纪念册 MVP

## 简介

YearArk 纪念册 MVP 旨在打通纪念册创建的核心流程：用户创建纪念册 → 生成分享链接 → 匿名用户上传素材 → 生成纪念册。本 MVP 不包含 AI 审查、AI 挑选、在线编辑等高级功能，聚焦于最小可用流程的贯通。

项目基于 RuoYi-AI 框架（Java 后端 + Vue 前端），后端已有基础 CRUD 实体和控制器（ya_album、ya_invite、ya_invite_token、ya_album_media、ya_album_page、ya_template 等），但缺少面向用户端的业务逻辑接口和用户端前端（yearark-web）。

## 术语表

- **Album_Service**：纪念册后端服务，负责纪念册的创建、查询、状态管理等业务逻辑
- **Invite_Service**：邀请链接后端服务，负责邀请链接的生成、验证、过期管理
- **Token_Service**：匿名 Token 后端服务，负责匿名上传者身份的生成与验证
- **Media_Service**：素材后端服务，负责素材的上传、存储、查询
- **Generation_Service**：纪念册生成服务，负责将素材与模板合并生成纪念册页面
- **User_Web**：用户端前端应用（yearark-web），面向终端用户的 Vue3 SPA
- **YaUser**：用户端用户，独立于管理端 sys_user，通过 ya_user 表管理
- **YaAlbum**：纪念册实体，包含名称、描述、模板、状态等信息
- **YaInvite**：邀请链接实体，包含 invite_code（6位随机串）和 access_code
- **YaInviteToken**：匿名上传者虚拟身份，通过 JWT token 标识匿名用户
- **YaAlbumMedia**：纪念册素材，支持文本（type=1）和图片（type=2）
- **YaAlbumPage**：纪念册页面，关联模板页面并存储填充数据 JSON
- **YaTemplate**：模板套件，包含多个页面模板
- **YaTemplatePage**：套件内的页面模板，包含 H5 模板字符串

## 需求

### 需求 1：用户注册与登录

**用户故事：** 作为一个用户，我希望能注册和登录 YearArk 用户端，以便创建和管理我的纪念册。

#### 验收标准

1. WHEN YaUser 提交包含用户名、密码和邮箱的注册请求, THE User_Web SHALL 将请求发送至后端，后端对密码进行加密后存储到 ya_user 表，并返回注册成功结果
2. WHEN YaUser 提交包含用户名和密码的登录请求, THE Album_Service SHALL 验证凭据，生成 JWT token 并返回给 User_Web
3. IF 注册时用户名已存在, THEN THE Album_Service SHALL 返回"用户名已存在"的错误提示
4. IF 登录时凭据不正确, THEN THE Album_Service SHALL 返回"用户名或密码错误"的错误提示
5. WHILE YaUser 处于已登录状态, THE User_Web SHALL 在每次请求中携带 JWT token 进行身份验证

### 需求 2：创建纪念册

**用户故事：** 作为一个已登录用户，我希望能创建一本纪念册并选择模板，以便开始制作我的毕业纪念册。

#### 验收标准

1. WHEN YaUser 提交创建纪念册请求（包含名称、描述）, THE Album_Service SHALL 创建一条 ya_album 记录，status 设为 0（草稿），is_public 设为 0（不公开），并关联当前用户的 user_id
2. WHEN YaUser 请求模板列表, THE Album_Service SHALL 返回所有 status 为可用的 YaTemplate 列表，包含名称、预览图和描述
3. WHEN YaUser 为纪念册选择一个模板, THE Album_Service SHALL 将 ya_album.template_id 更新为所选模板的 ID
4. THE User_Web SHALL 展示纪念册创建表单，包含名称输入框、描述输入框和模板选择区域
5. IF 创建纪念册时名称为空, THEN THE User_Web SHALL 阻止提交并显示"纪念册名称不能为空"的提示

### 需求 3：生成与管理邀请链接

**用户故事：** 作为纪念册创建者，我希望能生成分享链接，以便邀请同学上传照片和留言。

#### 验收标准

1. WHEN YaUser 请求为指定纪念册生成邀请链接, THE Invite_Service SHALL 生成一条 ya_invite 记录，包含 6 位随机 invite_code、可选的 access_code，status 设为 1（可用），并设置过期时间 expire_at
2. THE Invite_Service SHALL 确保生成的 invite_code 在 ya_invite 表中唯一
3. WHEN YaUser 请求查看某纪念册的邀请链接列表, THE Invite_Service SHALL 返回该纪念册下所有未删除的邀请链接，包含 invite_code、状态和过期时间
4. WHEN YaUser 请求禁用某个邀请链接, THE Invite_Service SHALL 将该 ya_invite 记录的 status 更新为 0（禁用）
5. THE User_Web SHALL 将 invite_code 拼接为完整的分享 URL，并提供复制链接功能
6. IF 邀请链接已过期（当前时间超过 expire_at）, THEN THE Invite_Service SHALL 拒绝通过该链接的访问请求并返回"链接已过期"的提示

### 需求 4：匿名用户通过邀请链接上传素材

**用户故事：** 作为收到分享链接的同学，我希望能匿名上传照片和留言，以便为纪念册贡献内容。

#### 验收标准

1. WHEN 匿名用户通过 invite_code 访问分享页面, THE Token_Service SHALL 验证 invite_code 对应的 ya_invite 记录存在、status 为 1（可用）且未过期
2. IF ya_invite 设置了 access_code, THEN THE User_Web SHALL 要求匿名用户输入访问码，Token_Service 验证通过后方可进入上传页面
3. WHEN 匿名用户通过验证后首次访问, THE Token_Service SHALL 为该用户生成一条 ya_invite_token 记录，包含 JWT token、IP 地址和过期时间，并将 token 返回给 User_Web 存储在本地
4. WHEN 匿名用户上传图片文件, THE Media_Service SHALL 将图片上传至 OSS，创建一条 ya_album_media 记录（type=2，content 为 OSS URL，status 设为 2 即直接通过），并关联 album_id 和 token_id
5. WHEN 匿名用户提交文本留言, THE Media_Service SHALL 创建一条 ya_album_media 记录（type=1，content 为文本内容，status 设为 2 即直接通过），并关联 album_id 和 token_id
6. THE User_Web SHALL 在分享页面展示上传区域，支持图片拖拽上传和文本输入框
7. IF 匿名用户的 token 已过期, THEN THE Token_Service SHALL 拒绝上传请求并返回"身份已过期，请重新通过邀请链接访问"的提示
8. WHILE 匿名用户处于上传页面, THE User_Web SHALL 展示该用户已上传的素材列表

### 需求 5：纪念册创建者查看素材

**用户故事：** 作为纪念册创建者，我希望能查看所有同学上传的素材，以便了解收集进度。

#### 验收标准

1. WHEN YaUser 请求查看某纪念册的素材列表, THE Media_Service SHALL 返回该纪念册下所有未删除的 ya_album_media 记录，按 sort 字段升序排列
2. THE User_Web SHALL 以网格视图展示图片素材，以列表视图展示文本素材
3. WHEN YaUser 请求按类型筛选素材, THE Media_Service SHALL 根据 type 参数过滤返回结果
4. THE User_Web SHALL 展示素材统计信息，包含图片总数和文本总数

### 需求 6：生成纪念册

**用户故事：** 作为纪念册创建者，我希望能一键生成纪念册，将收集到的素材按模板排版成完整的纪念册。

#### 验收标准

1. WHEN YaUser 请求生成纪念册, THE Generation_Service SHALL 验证该纪念册已关联模板且至少有 1 条 status 为 2（审核通过）的素材
2. WHEN 验证通过后, THE Generation_Service SHALL 读取关联模板的所有 YaTemplatePage，按照每个页面模板的 YaTemplateSchema 将素材自动填充到模板中，为每个页面创建一条 ya_album_page 记录（data 字段存储填充后的 JSON）
3. THE Generation_Service SHALL 按照素材的 sort 顺序依次填充到模板页面中，图片素材填充到图片占位区域，文本素材填充到文本占位区域
4. WHEN 所有页面生成完成, THE Generation_Service SHALL 将 ya_album.status 更新为 1（发布）
5. IF 纪念册未关联模板, THEN THE Generation_Service SHALL 返回"请先选择模板"的错误提示
6. IF 纪念册没有任何审核通过的素材, THEN THE Generation_Service SHALL 返回"请先上传素材"的错误提示

### 需求 7：预览纪念册

**用户故事：** 作为纪念册创建者，我希望能在线预览生成后的纪念册，以便查看最终效果。

#### 验收标准

1. WHEN YaUser 请求预览纪念册, THE Album_Service SHALL 返回该纪念册的所有 ya_album_page 记录，包含每页的 template_page.content（H5 模板字符串）和 album_page.data（填充 JSON）
2. THE User_Web SHALL 将每页的 H5 模板字符串与填充 JSON 合并渲染，以翻页形式展示纪念册
3. WHILE YaUser 处于预览页面, THE User_Web SHALL 提供上一页、下一页的翻页导航
4. IF 纪念册尚未生成（没有 ya_album_page 记录）, THEN THE User_Web SHALL 显示"纪念册尚未生成，请先点击生成"的提示

### 需求 8：用户纪念册列表管理

**用户故事：** 作为已登录用户，我希望能查看和管理我创建的所有纪念册。

#### 验收标准

1. WHEN YaUser 请求查看纪念册列表, THE Album_Service SHALL 返回当前用户创建的所有未删除的 ya_album 记录，按创建时间降序排列
2. THE User_Web SHALL 以卡片形式展示纪念册列表，每张卡片包含纪念册名称、描述、状态（草稿/已发布）和创建时间
3. WHEN YaUser 请求删除某纪念册, THE Album_Service SHALL 逻辑删除该纪念册及其关联的所有 ya_album_page 记录
4. THE User_Web SHALL 在删除前弹出确认对话框，确认后执行删除操作

## 困难点分析

### 困难点 1：匿名上传的身份管理与安全性
- 匿名用户通过 invite_code 访问后需要生成 JWT token，该 token 需要在无用户账号的情况下标识唯一身份
- 需要处理 token 过期、刷新、以及同一用户多次访问的去重逻辑
- 需要防止恶意用户通过暴力猜测 invite_code 或 access_code 来非法访问

### 困难点 2：素材自动填充到模板的逻辑
- 模板页面通过 YaTemplateSchema（JSON Schema）定义了占位区域，Generation_Service 需要解析 schema 并将素材正确填充
- 素材数量与模板页面数量可能不匹配：素材多于模板页面时需要自动扩展页面，素材少于模板页面时需要处理空页面
- 图片和文本需要分别匹配到对应类型的占位区域

### 困难点 3：用户端前端（yearark-web）从零搭建
- 需要独立于管理端（ruoyi-admin）搭建一个面向用户的 Vue3 前端应用
- 需要实现独立的用户认证体系（ya_user），与管理端的 sys_user 认证体系隔离
- 分享页面需要支持未登录（匿名 token）访问，与已登录用户的路由和权限管理需要区分

### 困难点 4：文件上传与 OSS 集成
- 匿名用户上传图片需要经过后端转存到阿里云 OSS，需要处理大文件上传、并发上传等场景
- 需要复用 RuoYi 框架已有的 sys_oss 模块，但匿名用户的上传权限需要单独处理（不走 sys_user 认证）

### 困难点 5：纪念册渲染
- 前端需要将 H5 模板字符串（template_page.content）与 JSON 数据（album_page.data）合并渲染
- 需要确定模板变量的替换机制（如 Mustache、Handlebars 或自定义占位符）
- 翻页预览的交互体验需要流畅
