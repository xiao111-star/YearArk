# 实现计划：纪念册渲染管线重构

## 概述

按照设计文档，分阶段实现渲染管线重构：
1. 数据库变更与核心实体扩展
2. Java 后端核心值对象、DTO、校验器
3. Python FastAPI AI 服务（Mock 实现）
4. Java RabbitMQ 通信层
5. AlbumGenerationService 重构（接入 MQ）
6. TemplateRenderService 重构（支持 focus_point + scale）
7. AlbumPageEditService 编辑保存服务
8. 后端 Controller 接口扩展
9. 前端 API 层、状态反馈、编辑器组件

## 任务

- [x] 1. 数据库变更与实体扩展
  - [x] 1.1 执行 ya_album 表 DDL 变更，新增 `generation_status`、`generation_fail_reason` 两个字段
    - SQL 文件路径: `ruoyi-ai/script/sql/update/2025-01-01-ya-album-add-generation-fields.sql`
    - 注：`is_degraded` 字段不需要，已从设计中移除
    - _需求: 4.7_
  - [x] 1.2 在 `YaAlbum` 实体类中新增 `generationStatus`、`generationFailReason` 字段
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/YaAlbum.java`
    - 移除已添加的 `isDegraded` 字段
    - _需求: 4.7_

- [x] 2. 核心值对象与 DTO 创建
  - [x] 2.1 创建 `ImageSlotValue` 值对象
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/ImageSlotValue.java`
    - 实现 `fromDataValue(Object)` 静态方法，兼容纯字符串 URL 和 `{url, focus_x, focus_y, scale}` 对象格式
    - 纯字符串时 focusX/focusY 默认 0.5，scale 默认 1.0
    - _需求: 5.2, 5.5_
  - [x] 2.2 创建 `GenerationRequestMessage`、`GenerationResultMessage` MQ 消息模型
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/mq/` 目录下
    - `GenerationRequestMessage` 包含 correlationId、albumId、mediaList、templatePages
    - `GenerationResultMessage` 包含 correlationId、albumId、status、pages（含 templatePageId + dataMap）、errorMessage
    - _需求: 1.1, 1.2_
  - [ ] 2.3 创建 `ValidationResult`、`SlotError` 校验结果类
    - 路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/` 目录下
    - `ValidationResult` 包含 valid 布尔值和 errors 列表
    - `SlotError` 包含 slotId 和 message
    - _需求: 3.6_
  - [ ] 2.4 创建 `PageUpdateDto` 和 `EditablePageVo`
    - `PageUpdateDto`: pageId + dataMap，参数校验注解在实体类中
    - `EditablePageVo`: pageId + sort + html + data + schemaContent
    - _需求: 8.1, 6.2_

- [x] 3. 检查点 - 确保编译通过
  - 确保所有新增实体类、DTO、VO、MQ 消息模型编译无误

- [x] 4. SchemaValidator 校验器实现
  - [x] 4.1 创建 `SchemaValidator` 接口和 `SchemaValidatorImpl` 实现类
    - 接口路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/SchemaValidator.java`
    - 实现路径: `ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/impl/SchemaValidatorImpl.java`
    - 解析 Schema JSON 中的 slots 数组，逐个 slot 校验 Data JSON
    - 校验规则：required 非空检查、text maxLength 检查、image URL 格式检查
    - 非必填 slot 缺失时使用 default 值自动填充到 dataMap 中
    - _需求: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 5. Python FastAPI AI 服务（Mock 实现）
  - [x] 5.1 创建 Python 项目骨架
  - [x] 5.2 创建 Pydantic 消息模型
  - [x] 5.3 实现 MockGroupingStrategy
  - [x] 5.4 实现 RabbitMQ Consumer 和 Publisher
  - [x] 5.5 在 `main.py` 中启动 FastAPI 应用和 RabbitMQ Consumer

- [ ] 6. 检查点 - 确保 Python 服务可启动并处理消息
  - 手动测试：启动 Python 服务，发送一条测试消息到请求队列，验证结果队列收到正确响应

- [x] 7. Java RabbitMQ 配置与通信层
  - [x] 7.1 添加 RabbitMQ 依赖和配置
  - [x] 7.2 创建 RabbitMQ Exchange/Queue 配置类
  - [x] 7.3 创建 `AlbumGenerationMQPublisher` 消息发布组件
  - [x] 7.4 创建 `AlbumGenerationMQConsumer` 消息消费组件

- [x] 8. AlbumGenerationService 重构
  - [x] 8.1 重构 `AlbumGenerationServiceImpl`
  - [x] 8.2 在 `AlbumGenerationMQConsumer` 中实现结果处理逻辑

- [x] 9. TemplateRenderService 重构
  - [x] 9.1 重构 `TemplateRenderServiceImpl`

- [x] 10. AlbumPageEditService 编辑保存服务
  - [x] 10.1 创建 `AlbumPageEditService` 接口和 `AlbumPageEditServiceImpl` 实现类

- [ ] 11. 检查点 - 确保后端编译通过，MQ 通信联调
  - 确保 Java 后端编译无误
  - 联调测试：Java 发送生成请求 → Python 处理 → Java 接收结果 → Schema 校验 → 存储

- [x] 12. 后端 Controller 接口扩展
  - [x] 12.1 在 `UserAlbumController` 中新增接口

- [x] 13. 前端 API 层扩展
  - [x] 13.1 在 `yearark-web/src/api/album.ts` 中新增 API 方法

- [x] 14. 前端生成状态与进度反馈
  - [x] 14.1 修改 `AlbumDetailPage.vue`，展示生成状态可视化标识

- [x] 15. 前端页面编辑器核心组件
  - [x] 15.1 创建 `PageEditor.vue` 组件
  - [x] 15.2 实现文案编辑功能
  - [x] 15.3 创建 `FocusPointPicker.vue` 焦点与缩放调整组件
  - [x] 15.4 创建 `MediaPanel.vue` 素材库面板组件

- [x] 16. 前端预览页面集成编辑模式
  - [x] 16.1 修改 `AlbumPreviewPage.vue`，集成编辑模式
  - [x] 16.2 实现编辑保存流程

- [ ] 17. 最终检查点 - 确保端到端流程通过

## 备注

- 任务 1.1/1.2 已完成（但需修正：移除 is_degraded 字段）
- Python 服务位于 `yearark-ai/` 目录，与 Java 后端并列
- 后端使用 MyBatis Plus Service 层方法，不使用 Mapper 层代码
- 参数校验优先在实体类中进行（如 PageUpdateDto 的 @NotNull 注解）
- image slot 的 Data JSON 格式：`{url, focus_x, focus_y, scale}`，scale 默认 1.0
- Schema 中 image slot 新增 `width`、`height` 字段，供 Python AI 参考图片尺寸匹配
