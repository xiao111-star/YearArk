# Implementation Plan: Album Visual Editor

## Overview

将纪念册渲染逻辑迁移到前端，新增独立编辑页面支持文案内联编辑和图片可视化调整，通过 AutoSave 自动保存。后端扩展 edit-data 接口返回 templateHtml，新增登录用户图片上传接口，调整素材列表查询条件。

## Tasks

- [x] 1. 后端接口扩展
  - [x] 1.1 EditablePageVo 新增 templateHtml 字段并在 AlbumPageEditService 中填充
    - 在 `EditablePageVo` 中新增 `private String templateHtml` 字段
    - 在 `AlbumPageEditServiceImpl.getEditData()` 中设置 `vo.setTemplateHtml(tp.getContent())`
    - 保留原有 `html` 字段不变
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 1.2 新增登录用户图片上传接口
    - 在 `UserAlbumController` 新增 `POST /api/user/album/{id}/media/upload` 端点，接收 `MultipartFile`
    - 在 Service 层验证用户对纪念册的所有权（`albumService.checkOwnership`）
    - 在 Service 层校验文件 MIME 类型为 `image/*`，非图片返回错误
    - 上传文件到 OSS 获取 URL，插入 `ya_album_media` 记录：`type=2, status=2, tokenId=null`
    - 使用 MyBatis Plus Service 层方法（`albumMediaService.save()`），不使用 Mapper
    - 返回 `YaAlbumMediaVo`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 1.3 调整 unused-media 接口查询条件
    - 修改 `IYaAlbumMediaService.listUnusedImages` 方法，移除"未使用"过滤逻辑
    - 改为查询该纪念册所有 `type=2, status=2` 的图片，按 `sort` 升序返回
    - 接口路径和返回结构保持不变
    - _Requirements: 9.1, 9.2, 9.3_

- [x] 2. Checkpoint - 后端接口验证
  - Ensure all tests pass, ask the user if questions arise.


- [x] 3. 前端渲染模块与类型定义
  - [x] 3.1 创建前端 TypeScript 类型定义
    - 在 `yearark-web/src/types/editor.ts` 中定义 `ImageSlotValue`、`SlotValue`、`SlotDef`、`PageData` 等接口
    - _Requirements: 1.2, 1.3_

  - [x] 3.2 实现 albumRenderer.ts 前端渲染纯函数
    - 创建 `yearark-web/src/utils/albumRenderer.ts`，导出 `renderPage(templateHtml, data)` 函数
    - text slot（string 类型）：HTML 转义后替换 `{{slot_id}}`
    - image slot（ImageSlotValue 类型）：生成带 `object-position`、`transform:scale` 样式的 `<img>` 标签
    - 未定义的占位符替换为空字符串
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ]* 3.3 Property 1 属性测试：Renderer 消除所有占位符
    - **Property 1: Renderer eliminates all placeholders**
    - 使用 fast-check 生成随机 templateHtml 和 DataJson，验证输出无 `{{...}}` 模式
    - **Validates: Requirements 1.1, 1.4**

  - [ ]* 3.4 Property 2 属性测试：文本 slot HTML 转义
    - **Property 2: Text slot values are HTML-escaped**
    - 使用 fast-check 生成含特殊字符的字符串，验证转义正确
    - **Validates: Requirements 1.2**

  - [ ]* 3.5 Property 3 属性测试：图片 slot 渲染样式
    - **Property 3: Image slot renders with correct styles**
    - 使用 fast-check 生成随机 ImageSlotValue，验证 img 标签和样式属性
    - **Validates: Requirements 1.3**

- [x] 4. 前端 API 层与 Composables
  - [x] 4.1 扩展前端 API 层
    - 在 `yearark-web/src/api/album.ts` 中新增 `uploadAlbumMedia(albumId, file)` 方法，调用 `POST /api/user/album/{id}/media/upload`
    - 确认 `getUnusedMedia` 和 `updatePageData` 已存在且签名匹配
    - _Requirements: 5.3, 8.1_

  - [x] 4.2 实现 useAutoSave composable
    - 创建 `yearark-web/src/composables/useAutoSave.ts`
    - 导出 `saving` 状态和 `save(pageId, data)` 方法
    - 调用 `updatePageData` API，保存中展示 loading 状态
    - 成功后展示 toast 通知，失败展示错误提示并保留用户输入
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 4.5_

  - [x] 4.3 实现 useAlbumEditor composable
    - 创建 `yearark-web/src/composables/useAlbumEditor.ts`
    - 导出 `pages`、`loading`、`albumName`、`loadEditData`、`updateSlotValue`、`getPageData`
    - `loadEditData` 调用 `getAlbumEditData` 和 `getAlbumDetail` API
    - `updateSlotValue` 更新本地 pages 数据并触发 `useAutoSave.save`
    - _Requirements: 3.4, 3.5, 4.2, 4.3, 6.4, 7.1, 7.2_

- [x] 5. 编辑器页面路由与主框架
  - [x] 5.1 注册编辑器路由
    - 在 `yearark-web/src/router/index.ts` 的 AppLayout children 中新增 `/album/:id/edit` 路由，指向 `AlbumEditorPage.vue`
    - 路由需登录鉴权（在 `requiresAuth` 的 AppLayout 下）
    - _Requirements: 3.1_

  - [x] 5.2 实现 AlbumEditorPage.vue 主页面
    - 创建 `yearark-web/src/views/AlbumEditorPage.vue`
    - 顶部导航栏：返回按钮（→ `/album/:id`）+ 纪念册名称
    - 调用 `useAlbumEditor` 加载数据
    - 竖向滚动排列所有 `PageCanvas` 组件
    - 展示加载中 / 空状态提示
    - 管理 MediaPicker / ImageAdjustPanel 弹出状态
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_


- [x] 6. 页面渲染容器与 Overlay 组件
  - [x] 6.1 实现 PageCanvas.vue 单页渲染容器
    - 创建 `yearark-web/src/components/editor/PageCanvas.vue`
    - 调用 `renderPage(templateHtml, data)` 生成 HTML，通过 `v-html` 渲染
    - 解析 `schemaContent` 中的 slots 定义，叠加 Overlay 层
    - text slot → `TextSlotOverlay`，image slot → `ImageSlotOverlay`
    - _Requirements: 1.1, 1.5_

  - [x] 6.2 实现 TextSlotOverlay.vue 文本内联编辑
    - 创建 `yearark-web/src/components/editor/TextSlotOverlay.vue`
    - 点击进入 `contenteditable` 编辑模式，展示高亮边框
    - blur 或 Enter 退出编辑，emit `update:value` 事件
    - 仅支持纯文本，不支持富文本
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 6.3 实现 ImageSlotOverlay.vue 图片点击触发
    - 创建 `yearark-web/src/components/editor/ImageSlotOverlay.vue`
    - 点击时 emit `select` 事件，展示半透明遮罩 + 相机图标提示
    - _Requirements: 5.1_

- [x] 7. Checkpoint - 编辑器基础渲染验证
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. 图片选择与调整面板
  - [x] 8.1 实现 MediaPicker.vue 图片选择面板
    - 创建 `yearark-web/src/components/editor/MediaPicker.vue`
    - 调用 `getUnusedMedia` 加载素材列表（type=2, status=2）
    - 网格展示缩略图，支持面板内上传（调用 `uploadAlbumMedia`）
    - 上传成功后将新素材追加到列表头部
    - 选择图片后 emit `pick` 事件
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 8.2 实现 ImageAdjustPanel.vue 焦点拖拽 + 缩放调整
    - 创建 `yearark-web/src/components/editor/ImageAdjustPanel.vue`
    - 图片预览区域：拖拽调整 focus_x / focus_y（范围 0.0 ~ 1.0）
    - 缩放滑块：调整 scale（范围 0.5 ~ 3.0）
    - 不提供旋转功能
    - 确认 → emit `confirm`（触发 AutoSave），取消 → emit `cancel`（恢复原始值）
    - 值超出范围时 clamp 到边界
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 8.3 Property 8 属性测试：ImageSlotValue 约束 clamp
    - **Property 8: ImageSlotValue constraints are clamped**
    - 使用 fast-check 生成任意 number，验证 clamp 后 focus_x/focus_y 在 [0.0, 1.0]，scale 在 [0.5, 3.0]
    - **Validates: Requirements 6.1, 6.2**

- [x] 9. 编辑器交互串联
  - [x] 9.1 在 AlbumEditorPage 中串联所有交互流程
    - 将 PageCanvas、TextSlotOverlay、ImageSlotOverlay、MediaPicker、ImageAdjustPanel 串联
    - TextSlot 编辑完成 → `updateSlotValue` → AutoSave
    - ImageSlot 点击 → MediaPicker 选择 → ImageAdjustPanel 调整 → 确认 → `updateSlotValue` → AutoSave
    - ImageAdjustPanel 取消 → 恢复原始值，不触发 AutoSave
    - 展示保存中状态指示和保存成功 toast
    - _Requirements: 4.2, 4.3, 5.5, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 10. 改造 AlbumPreviewPage 复用前端渲染
  - [x] 10.1 改造 AlbumPreviewPage 使用 albumRenderer
    - 修改 `AlbumPreviewPage.vue`，调用 `getAlbumEditData` 获取 templateHtml + data
    - 使用 `renderPage(templateHtml, data)` 在前端完成渲染，替代直接使用后端返回的 html
    - 确保与 Editor 共用同一套 `albumRenderer.ts` 渲染逻辑
    - _Requirements: 1.5_

- [x] 11. Final checkpoint - 全部功能验证
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 后端使用 MyBatis Plus Service 层方法，不使用 Mapper 层
- 参数校验优先在实体类注解中完成，MIME 类型等在 Service 层校验
- 图片不支持旋转，仅支持缩放和焦点调整
- 仅自动保存，无手动保存按钮，无撤销功能
- 前端渲染模块 albumRenderer.ts 被 Editor 和 PreviewPage 共同复用
