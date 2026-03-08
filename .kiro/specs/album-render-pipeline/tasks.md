# 实现计划：纪念册渲染管线重构

## 概述

按照设计文档，分阶段实现渲染管线重构：先完成后端核心模型和校验器，再重构生成服务和渲染服务，最后实现前端编辑器和状态管理。每个任务构建在前一个任务之上，确保增量可验证。

## 任务

- [ ] 1. 数据库变更与实体扩展
  - [ ] 1.1 执行 ya_album 表 DDL 变更，新增 `generation_status`、`generation_fail_reason`、`is_degraded` 三个字段
    - 编写 SQL：`ALTER TABLE ya_album ADD COLUMN generation_status VARCHAR(20) NOT NULL DEFAULT 'pending', ADD COLUMN generation_fail_reason VARCHAR(500) DEFAULT NULL, ADD COLUMN is_degraded TINYINT(1) NOT NULL DEFAULT 0;`
    - _需求: 4.7_
  - [ ] 1.2 在 `YaAlbum` 实体类中新增 `generationStatus`、`generationFailReason`、`isDegraded` 字段
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/YaAlbum.java`
    - _需求: 4.7_

- [ ] 2. 核心值对象与 DTO 创建
  - [ ] 2.1 创建 `ImageSlotValue` 值对象
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/ImageSlotValue.java`
    - 实现 `fromDataValue(Object)` 静态方法，兼容纯字符串 URL 和 `{url, focus_x, focus_y}` 对象格式
    - 纯字符串时 focusX/focusY 默认 0.5
    - _需求: 5.2, 5.5_
  - [ ] 2.2 创建 `GenerationContext`、`TemplatePageInfo`、`PageDataResult` 数据类
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/` 目录下
    - `GenerationContext` 包含 albumId、images、texts、pages 列表
    - `TemplatePageInfo` 包含 templatePageId、schemaId、imageCount、textCount、schemaContent
    - `PageDataResult` 包含 templatePageId、dataMap
    - _需求: 4.2_
  - [ ] 2.3 创建 `ValidationResult`、`SlotError` 校验结果类
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/` 目录下
    - `ValidationResult` 包含 valid 布尔值和 errors 列表
    - `SlotError` 包含 slotId 和 message
    - _需求: 3.6_
  - [ ] 2.4 创建 `PageUpdateDto` 和 `EditablePageVo`
    - `PageUpdateDto`: pageId + dataMap，参数校验注解在实体类中
    - `EditablePageVo`: pageId + sort + html + data + schemaContent
    - _需求: 8.1, 6.2_

- [ ] 3. 检查点 - 确保编译通过
  - 确保所有新增实体类、DTO、VO 编译无误，ask the user if questions arise.

- [ ] 4. SchemaValidator 校验器实现
  - [ ] 4.1 创建 `SchemaValidator` 接口和 `SchemaValidatorImpl` 实现类
    - 接口路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/SchemaValidator.java`
    - 实现路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/SchemaValidatorImpl.java`
    - 解析 Schema JSON 中的 slots 数组，逐个 slot 校验 Data JSON
    - 校验规则：required 非空检查、text maxLength 检查、image URL 格式检查
    - 非必填 slot 缺失时使用 default 值自动填充到 dataMap 中
    - _需求: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - [ ]* 4.2 编写 SchemaValidator 属性测试
    - **Property 3: Schema 校验器正确识别违规 slot**
    - **Property 4: Schema 校验器 default 值自动填充**
    - **验证: 需求 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
  - [ ]* 4.3 编写 SchemaValidator 单元测试
    - 测试具体的校验成功/失败示例、空 Schema、空 Data JSON
    - _需求: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [ ] 5. DataGenerationStrategy 策略模式与 Mock 实现
  - [ ] 5.1 创建 `DataGenerationStrategy` 接口
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/DataGenerationStrategy.java`
    - 定义 `generatePageDataList(GenerationContext context)` 方法
    - _需求: 4.2_
  - [ ] 5.2 创建 `MockDataGenerationStrategy` 实现类
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/MockDataGenerationStrategy.java`
    - 按 sort 顺序将图片素材填充到 image slot，生成 ImageSlotValue 对象（focus_x=0.5, focus_y=0.5）
    - 按 sort 顺序将文字素材填充到 text slot
    - 素材不足时：计算可用素材总量，按 imageCount 升序选择能被完全填满的模板页子集，跳过无法填满的页面
    - 绝不使用占位图或空字符串填充 required slot
    - _需求: 2.4, 2.5, 4.2_
  - [ ]* 5.3 编写 MockDataGenerationStrategy 属性测试
    - **Property 1: Mock 生成的 Data JSON 所有 slot 均有真实素材填充**
    - **Property 2: Mock 生成的 image slot 焦点默认值**
    - **验证: 需求 2.4, 2.5**
  - [ ]* 5.4 编写 MockDataGenerationStrategy 单元测试
    - 测试素材不足时的智能页面选择（跳过无法填满的页面）、素材超出时的截断
    - _需求: 2.4_

- [ ] 6. 检查点 - 确保校验器和策略模式测试通过
  - 确保所有测试通过，ask the user if questions arise.

- [ ] 7. AlbumGenerationService 重构
  - [ ] 7.1 重构 `AlbumGenerationServiceImpl`
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/AlbumGenerationServiceImpl.java`
    - 注入 `DataGenerationStrategy`（当前为 MockDataGenerationStrategy）和 `SchemaValidator`
    - 生成前校验：纪念册已关联模板且至少有 1 条 status=2 的素材
    - 生成开始时更新 `generation_status = processing`
    - 调用策略生成 PageDataResult 列表
    - 对每页 Data JSON 调用 SchemaValidator 校验
    - 校验失败的页面使用回退策略（按顺序填充素材生成 Data JSON）
    - 清除旧 album_page，批量插入新记录
    - 成功后更新 `generation_status = completed`，失败更新为 `failed` 并记录原因
    - 移除 `DEFAULT_PLACEHOLDER_IMAGE` 占位图逻辑
    - _需求: 4.1, 4.3, 4.4, 4.5, 4.6, 4.7_
  - [ ]* 7.2 编写 AlbumGenerationService 属性测试
    - **Property 5: 生成前置条件校验**
    - **Property 6: 成功生成后状态与数据一致性**
    - **验证: 需求 4.1, 4.4, 4.7**

- [ ] 8. TemplateRenderService 重构
  - [ ] 8.1 重构 `TemplateRenderServiceImpl`
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/TemplateRenderServiceImpl.java`
    - `renderPage` 方法支持 Data JSON 中 image slot 值为对象格式 `{url, focus_x, focus_y}`
    - 对象格式时：替换 `{{image_N}}` 为 url，并在对应 `<img>` 标签注入 `style="object-fit:cover; object-position:{focus_x*100}% {focus_y*100}%"`
    - 纯字符串 URL 时：使用默认焦点 {0.5, 0.5}（向后兼容）
    - text slot 值始终为 String，直接替换
    - _需求: 5.2, 5.3, 5.5_
  - [ ]* 8.2 编写 TemplateRenderService 属性测试
    - **Property 8: 渲染服务注入 focus_point 样式**
    - **验证: 需求 5.3**
  - [ ]* 8.3 编写 ImageSlotValue 属性测试
    - **Property 7: ImageSlotValue 解析兼容性**
    - **验证: 需求 5.2, 5.5**

- [ ] 9. AlbumPageEditService 编辑保存服务
  - [ ] 9.1 创建 `AlbumPageEditService` 接口和 `AlbumPageEditServiceImpl` 实现类
    - 接口路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/AlbumPageEditService.java`
    - 实现路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/AlbumPageEditServiceImpl.java`
    - `updatePageData(pageId, dataMap)`: 校验页面归属 → Schema 校验 → 更新 data → 重新渲染 → 返回 RenderedPageVo
    - `batchUpdatePageData(updates)`: 事务内批量校验和更新，任一页校验失败则整个操作回滚
    - 校验失败时返回结构化错误列表，不更新数据库
    - _需求: 8.1, 8.2, 8.3, 8.4, 8.5_
  - [ ]* 9.2 编写 AlbumPageEditService 属性测试
    - **Property 10: 单页更新 round-trip**
    - **Property 11: 校验失败时数据不变**
    - **Property 12: 页面归属校验**
    - **Property 13: 批量更新事务性**
    - **验证: 需求 8.1, 8.2, 8.3, 8.4, 8.5**

- [ ] 10. 检查点 - 确保后端所有服务测试通过
  - 确保所有测试通过，ask the user if questions arise.

- [ ] 11. 后端 Controller 接口扩展
  - [ ] 11.1 在 `UserAlbumController` 中新增接口
    - 路径: `ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/client/UserAlbumController.java`
    - `GET /api/user/album/{id}/status` — 获取生成状态（generationStatus + failReason）
    - `GET /api/user/album/{id}/edit-data` — 获取编辑数据（每页的 Data JSON + Schema + 渲染 HTML）
    - `PUT /api/user/album/page/{pageId}` — 更新单页 Data JSON（调用 AlbumPageEditService）
    - `PUT /api/user/album/{id}/pages` — 批量更新多页 Data JSON
    - `GET /api/user/album/{id}/unused-media` — 获取未使用的素材列表
    - 所有接口均校验纪念册归属
    - _需求: 7.1, 8.1, 8.4, 8.5, 6.5_

- [ ] 12. 前端 API 层扩展
  - [ ] 12.1 在 `yearark-web/src/api/album.ts` 中新增 API 方法
    - `getAlbumStatus(id)` — 获取生成状态
    - `getAlbumEditData(id)` — 获取编辑数据
    - `updatePageData(pageId, data)` — 更新单页
    - `batchUpdatePages(albumId, pages)` — 批量更新
    - `getUnusedMedia(albumId)` — 获取未使用素材
    - _需求: 7.1, 8.1, 6.5_

- [ ] 13. 前端生成状态与进度反馈
  - [ ] 13.1 修改 `AlbumDetailPage.vue`，展示生成状态可视化标识
    - 路径: `yearark-web/src/views/AlbumDetailPage.vue`
    - 展示 pending/processing/completed/failed 四种状态的可视化标识
    - processing 状态下每 5 秒轮询 `getAlbumStatus` 接口
    - completed 时自动停止轮询，启用"预览"和"编辑"按钮
    - failed 时停止轮询，显示失败原因和"重新生成"按钮
    - 降级方案生成时显示提示信息
    - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 14. 前端页面编辑器核心组件
  - [ ] 14.1 创建 `PageEditor.vue` 组件
    - 路径: `yearark-web/src/components/PageEditor.vue`
    - 接收 props: albumId、page（含 pageId/sort/html/data/schema）、unusedMedia
    - 将 Data JSON 解析为可编辑元素：text slot 渲染为可点击编辑的文本区域，image slot 渲染为可操作的图片区域
    - 支持图片拖拽交换（拖动一张图片到另一个 image slot 位置，交换两个 slot 的数据）
    - _需求: 6.2, 6.4_
  - [ ] 14.2 实现文案编辑功能
    - 在 PageEditor 中，点击 text slot 区域显示内联文本编辑器
    - 修改后实时更新该 slot 在 Data JSON 中的值
    - _需求: 6.3_
  - [ ] 14.3 创建 `FocusPointPicker.vue` 焦点调整组件
    - 路径: `yearark-web/src/components/FocusPointPicker.vue`
    - 点击 image slot 时显示焦点调整工具
    - 用户可通过拖动十字准星调整 focus_point 坐标
    - 实时预览裁剪效果（使用 object-position CSS）
    - _需求: 6.6_
  - [ ] 14.4 创建 `MediaPanel.vue` 素材库面板组件
    - 路径: `yearark-web/src/components/MediaPanel.vue`
    - 展示该纪念册所有未使用的图片素材
    - 支持从素材库面板拖入新图片替换当前 image slot
    - _需求: 6.5_

- [ ] 15. 前端预览页面集成编辑模式
  - [ ] 15.1 修改 `AlbumPreviewPage.vue`，集成编辑模式
    - 路径: `yearark-web/src/views/AlbumPreviewPage.vue`
    - 新增"编辑模式"开关按钮
    - 只读模式：使用现有 BookViewer iframe 渲染
    - 编辑模式：切换为 PageEditor 组件可交互渲染
    - 编辑模式下加载 edit-data 接口获取 Data JSON + Schema
    - _需求: 6.1_
  - [ ] 15.2 实现编辑保存流程
    - 点击"保存"时将修改后的 Data JSON 提交到后端
    - 校验通过后更新页面并切回只读预览
    - 校验失败时在对应 slot 位置显示红色边框和错误提示
    - _需求: 6.7, 6.8_
  - [ ]* 15.3 编写 PageEditor 图片拖拽交换属性测试
    - **Property 9: 图片拖拽交换数据对称性**
    - **验证: 需求 6.4**

- [ ] 16. 最终检查点 - 确保所有测试通过
  - 确保所有后端和前端测试通过，ask the user if questions arise.

## 备注

- 标记 `*` 的任务为可选测试任务，可跳过以加速 MVP 开发
- 需求 1（RabbitMQ 通信）和需求 2（AI 分组策略）在当前阶段不实现，通过 MockDataGenerationStrategy 替代
- 后端使用 MyBatis Plus Service 层方法，不使用 Mapper 层代码
- 参数校验优先在实体类中进行（如 PageUpdateDto 的 @NotNull 注解）
- 每个任务引用了具体的需求编号，确保需求全覆盖
- 属性测试标注了对应的 Property 编号和验证的需求条款
