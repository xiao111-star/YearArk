# Requirements Document

## Introduction

纪念册可视化编辑器（Album Visual Editor）是一个独立的前端编辑页面，允许纪念册创建者对已生成的纪念册内容进行可视化编辑。核心改造包括：将渲染逻辑从后端迁移到前端（前端统一维护模板占位符替换逻辑），新增独立编辑路由页面，支持文案内联编辑和图片可视化调整，并通过自动保存机制将修改持久化。

## Glossary

- **Editor**: 纪念册可视化编辑器前端页面（路由 `/album/:id/edit`）
- **Renderer**: 前端渲染模块，负责将 templateHtml（含 `{{slot_id}}` 占位符）与 Data JSON 合并为可展示的 HTML
- **TemplateHtml**: 模板原始 HTML 字符串，包含 `{{slot_id}}` 占位符，由后端 `GET /api/user/album/{id}/edit-data` 返回
- **DataJson**: 每页的数据 JSON，格式为 `{"image_1": {"url":"...", "focus_x":0.5, "focus_y":0.5, "scale":1.0}, "text_1": "文字"}`
- **TextSlot**: 模板中的文本占位符区域，对应 DataJson 中 String 类型的 slot 值
- **ImageSlot**: 模板中的图片占位符区域，对应 DataJson 中包含 url/focus_x/focus_y/scale 的 Map 类型 slot 值
- **MediaLibrary**: 纪念册素材库，存储于 `ya_album_media` 表，type=2 为图片素材
- **AutoSave**: 编辑完成后自动调用 `PUT /api/user/album/page/{pageId}` 保存，无需手动触发
- **ImageAdjustMode**: 图片选择后进入的调整模式，支持 scale（缩放）和 focus_x/focus_y（焦点位置）调整

## Requirements

### Requirement 1: 前端统一渲染逻辑

**User Story:** As a 前端开发者, I want 前端统一维护模板渲染逻辑, so that 编辑页和预览页共用同一套渲染代码，不依赖后端渲染的 HTML 字符串。

#### Acceptance Criteria

1. THE Renderer SHALL 将 templateHtml 中所有 `{{slot_id}}` 占位符替换为对应的 DataJson 值
2. WHEN DataJson 中某 slot 值为 String 类型时，THE Renderer SHALL 将占位符替换为该字符串的 HTML 转义文本
3. WHEN DataJson 中某 slot 值为 ImageSlotValue 类型时，THE Renderer SHALL 将占位符替换为包含 url、focus_x、focus_y、scale 样式的 `<img>` 标签
4. WHEN templateHtml 中存在未在 DataJson 中定义的占位符时，THE Renderer SHALL 将该占位符替换为空字符串
5. THE Renderer SHALL 被 Editor 页面和 AlbumPreviewPage 页面共同复用，不允许各自独立实现渲染逻辑

### Requirement 2: 后端扩展 edit-data 接口返回 templateHtml

**User Story:** As a 前端开发者, I want `GET /api/user/album/{id}/edit-data` 返回每页的原始模板 HTML, so that 前端 Renderer 可以在客户端完成占位符替换。

#### Acceptance Criteria

1. THE EditablePageVo SHALL 新增 `templateHtml` 字段，存储模板页面的原始 HTML（含 `{{slot_id}}` 占位符）
2. WHEN 调用 `GET /api/user/album/{id}/edit-data` 时，THE AlbumPageEditService SHALL 在每个 EditablePageVo 中填充对应 YaTemplatePage 的 content 字段作为 templateHtml
3. THE EditablePageVo 中原有的 `html`（渲染后 HTML）字段 SHALL 继续保留，以保持向后兼容

### Requirement 3: 编辑页面路由与布局

**User Story:** As a 纪念册创建者, I want 通过独立路由访问编辑页面, so that 我可以在专注的编辑环境中修改纪念册内容。

#### Acceptance Criteria

1. THE Editor SHALL 在路由 `/album/:id/edit` 下作为独立页面存在，受登录鉴权保护
2. THE Editor SHALL 在页面顶部展示导航栏，包含返回按钮（返回 `/album/:id`）和纪念册名称
3. THE Editor SHALL 以竖向滚动长页面方式排列所有纪念册页面，不使用翻页模式
4. WHEN 页面数据加载中时，THE Editor SHALL 展示加载状态提示
5. WHEN 纪念册尚未生成（无页面数据）时，THE Editor SHALL 展示空状态提示并引导用户返回详情页

### Requirement 4: 文案内联编辑

**User Story:** As a 纪念册创建者, I want 点击文字区域直接编辑文案, so that 我可以快速修改纪念册中的文字内容。

#### Acceptance Criteria

1. WHEN 用户点击 TextSlot 区域时，THE Editor SHALL 将该区域切换为 `contenteditable` 模式，允许直接输入文字
2. WHEN 用户在 TextSlot 编辑区域触发 blur 事件时，THE Editor SHALL 退出编辑模式并触发 AutoSave
3. WHEN 用户在 TextSlot 编辑区域按下 Enter 键时，THE Editor SHALL 退出编辑模式并触发 AutoSave
4. WHILE TextSlot 处于编辑模式时，THE Editor SHALL 展示视觉高亮边框以区分可编辑状态
5. IF AutoSave 请求失败，THEN THE Editor SHALL 展示错误提示，保留用户输入内容不丢失

### Requirement 5: 图片选择面板

**User Story:** As a 纪念册创建者, I want 点击图片区域后从素材库选择图片, so that 我可以替换纪念册中的图片内容。

#### Acceptance Criteria

1. WHEN 用户点击 ImageSlot 区域时，THE Editor SHALL 弹出图片选择面板（MediaPicker）
2. THE MediaPicker SHALL 展示该纪念册 MediaLibrary 中所有 type=2、status=2 的图片素材
3. THE MediaPicker SHALL 支持在面板内直接上传新图片，调用 `POST /api/user/album/{id}/media/upload` 接口
4. WHEN 图片上传成功后，THE MediaPicker SHALL 将新图片立即显示在素材列表中，无需刷新页面
5. WHEN 用户在 MediaPicker 中选择一张图片时，THE Editor SHALL 关闭面板并进入 ImageAdjustMode

### Requirement 6: 图片调整模式

**User Story:** As a 纪念册创建者, I want 在选择图片后调整图片的缩放和焦点位置, so that 图片在纪念册中的展示效果符合我的预期。

#### Acceptance Criteria

1. WHEN 进入 ImageAdjustMode 时，THE Editor SHALL 展示图片预览区域，支持通过拖拽调整 focus_x 和 focus_y（范围 0.0 ~ 1.0）
2. THE Editor SHALL 提供缩放控件，支持调整 scale 值（范围 0.5 ~ 3.0）
3. THE Editor SHALL 不提供旋转功能
4. WHEN 用户完成图片调整并确认时，THE Editor SHALL 触发 AutoSave，将新的 ImageSlotValue（url、focus_x、focus_y、scale）保存
5. WHEN 用户取消图片调整时，THE Editor SHALL 恢复该 ImageSlot 的原始数据，不触发 AutoSave

### Requirement 7: 自动保存

**User Story:** As a 纪念册创建者, I want 编辑完成后内容自动保存, so that 我不需要手动点击保存按钮，降低操作负担。

#### Acceptance Criteria

1. WHEN TextSlot 编辑完成（blur 或 Enter）时，THE Editor SHALL 自动调用 `PUT /api/user/album/page/{pageId}` 保存该页完整 DataJson
2. WHEN ImageSlot 图片调整完成并确认时，THE Editor SHALL 自动调用 `PUT /api/user/album/page/{pageId}` 保存该页完整 DataJson
3. THE Editor SHALL 不提供手动保存按钮
4. THE Editor SHALL 不维护历史记录或撤销功能
5. WHILE AutoSave 请求进行中时，THE Editor SHALL 展示保存中状态指示（如 loading 图标）
6. IF AutoSave 请求成功，THEN THE Editor SHALL 展示短暂的保存成功提示（如 toast 通知）

### Requirement 8: 登录用户上传图片到素材库

**User Story:** As a 纪念册创建者, I want 直接上传图片到纪念册素材库, so that 我可以在编辑时使用自己上传的图片，而不仅限于通过分享链接上传的图片。

#### Acceptance Criteria

1. THE UserAlbumController SHALL 新增 `POST /api/user/album/{id}/media/upload` 接口，供登录用户上传图片到指定纪念册的素材库
2. WHEN 登录用户调用上传接口时，THE Service SHALL 验证该用户对该纪念册的所有权
3. WHEN 上传成功后，THE Service SHALL 将图片记录插入 `ya_album_media`，type=2，status=2（审核通过），tokenId=null
4. IF 上传的文件不是图片类型（非 image/* MIME），THEN THE Service SHALL 返回错误提示
5. THE 上传接口 SHALL 返回新创建的 YaAlbumMediaVo，包含图片 URL，供前端立即展示

### Requirement 9: 素材库图片列表接口扩展

**User Story:** As a 前端开发者, I want 获取纪念册所有已审核通过的图片素材, so that 图片选择面板可以展示完整的可用图片列表。

#### Acceptance Criteria

1. THE UserAlbumController 中 `GET /api/user/album/{id}/unused-media` 接口 SHALL 改为返回该纪念册所有 type=2、status=2 的图片，不再过滤"未使用"状态
2. WHEN 调用该接口时，THE IYaAlbumMediaService SHALL 按 sort 升序返回所有符合条件的图片素材
3. THE 接口路径和返回结构 SHALL 保持不变，仅调整查询条件，以减少前端改动
