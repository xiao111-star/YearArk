# Implementation Plan: 管理端模板管理功能

## Overview

基于现有的后端 CRUD 基础代码（Controller、Service、Mapper、Entity、DTO/VO 已存在），本计划聚焦于：
1. 后端增强：JSON 格式验证、纪念册使用数量统计、权限注解、Sa-Token 操作日志、错误信息优化
2. 前端完整实现：API 服务层、模板套件列表页、模板套件详情页（含模板页管理）、JSON Schema 管理页
3. 前端路由配置和菜单注册

## Tasks

- [x] 1. 后端增强 - 模板套件 Service 和 Controller 完善
  - [x] 1.1 在 `YaTemplateVo` 中添加 `albumCount`（纪念册使用数量）和 `typeName`（类型中文名称）字段，在 `YaTemplateServiceImpl.queryPage()` 和 `queryById()` 中查询 `ya_album` 表统计使用数量并填充 `albumCount`，使用字典服务翻译 `type` 为 `typeName`
    - 修改文件: `YaTemplateVo.java`, `YaTemplateServiceImpl.java`
    - _Requirements: 13.3, 15.4, 1.5_

  - [x] 1.2 在 `YaTemplateController` 的增删改方法上添加 Sa-Token 权限注解 `@SaCheckPermission`（如 `yearark:template:add`, `yearark:template:edit`, `yearark:template:remove`），并添加 `@Log` 操作日志注解
    - 修改文件: `YaTemplateController.java`
    - _Requirements: 16.1, 16.3, 16.4_

  - [x] 1.3 优化 `YaTemplateController.delete()` 方法，将 `RuntimeException` 替换为框架的 `ServiceException`，确保返回正确的错误提示"该模板已被使用，请先删除该模板下的所有相册"
    - 修改文件: `YaTemplateController.java`
    - _Requirements: 4.2, 18.1_

- [x] 2. 后端增强 - 模板页面 Service 和 Controller 完善
  - [x] 2.1 在 `YaTemplatePageController` 的增删改方法上添加 Sa-Token 权限注解 `@SaCheckPermission` 和 `@Log` 操作日志注解；在 `insert()` 方法中增加 content 为空的校验，返回"请输入模板内容"
    - 修改文件: `YaTemplatePageController.java`
    - _Requirements: 6.2, 6.3, 16.1, 16.3_

  - [x] 2.2 在 `YaTemplatePageServiceImpl.insertByDto()` 和 `updateByDto()` 中，将 `RuntimeException` 替换为 `ServiceException`，优化外键校验错误信息为"关联的模板套件不存在"和"关联的 Schema 不存在"
    - 修改文件: `YaTemplatePageServiceImpl.java`
    - _Requirements: 18.5, 18.6_

  - [x] 2.3 在 `YaTemplatePageQueryDto` 中添加 `status` 筛选字段，在 `YaTemplatePageServiceImpl.buildWrapper()` 中增加 status 条件
    - 修改文件: `YaTemplatePageQueryDto.java`, `YaTemplatePageServiceImpl.java`
    - _Requirements: 5.4_

- [x] 3. 后端增强 - JSON Schema Service 和 Controller 完善
  - [x] 3.1 在 `YaTemplateSchemaServiceImpl.insertByDto()` 和 `updateByDto()` 中添加 JSON 格式验证逻辑：使用 `com.fasterxml.jackson.databind.ObjectMapper` 解析 `content` 字段，解析失败时抛出 `ServiceException("JSON 格式不正确")`，并在错误信息中包含解析错误位置
    - 修改文件: `YaTemplateSchemaServiceImpl.java`
    - _Requirements: 10.1, 10.2, 11.2, 18.3_

  - [x] 3.2 在 `YaTemplateSchemaController` 的增删改方法上添加 Sa-Token 权限注解 `@SaCheckPermission` 和 `@Log` 操作日志注解；将 `deleteByIds()` 中的 `RuntimeException` 替换为 `ServiceException`
    - 修改文件: `YaTemplateSchemaController.java`, `YaTemplateSchemaServiceImpl.java`
    - _Requirements: 12.2, 16.1, 16.3_

  - [x] 3.3 在 `YaTemplateSchemaVo` 中添加 `contentPreview`（内容预览，截取前 100 字符）和 `usageCount`（被引用的模板页面数量）字段，在 `queryPage()` 中填充这两个字段
    - 修改文件: `YaTemplateSchemaVo.java`, `YaTemplateSchemaServiceImpl.java`
    - _Requirements: 9.3, 12.1_

- [x] 4. Checkpoint - 后端增强完成
  - Ensure all tests pass, ask the user if questions arise.


- [x] 5. 前端 API 服务层 - 创建模板管理相关 API 文件
  - [x] 5.1 创建 `ruoyi-admin/apps/web-antd/src/api/yearark/template/index.ts` 和 `model.d.ts`，定义 `YaTemplate`、`YaTemplateQuery` 接口和 `templatePage`、`templateList`、`templateInfo`、`templateAdd`、`templateUpdate`、`templateRemove` API 函数
    - 参考现有 `api/system/config/index.ts` 的模式
    - API 路径: `/yearark/template`
    - _Requirements: 1.1, 2.4, 3.3, 4.3_

  - [x] 5.2 创建 `ruoyi-admin/apps/web-antd/src/api/yearark/template-page/index.ts` 和 `model.d.ts`，定义 `YaTemplatePage`、`YaTemplatePageQuery` 接口和 `templatePagePage`、`templatePageList`、`templatePageInfo`、`templatePageAdd`、`templatePageUpdate`、`templatePageRemove` API 函数
    - API 路径: `/yearark/template-page`
    - _Requirements: 5.2, 6.4, 7.3, 8.1_

  - [x] 5.3 创建 `ruoyi-admin/apps/web-antd/src/api/yearark/template-schema/index.ts` 和 `model.d.ts`，定义 `YaTemplateSchema`、`YaTemplateSchemaQuery` 接口和 `schemaPage`、`schemaList`、`schemaInfo`、`schemaAdd`、`schemaUpdate`、`schemaRemove` API 函数
    - API 路径: `/yearark/template-schema`
    - _Requirements: 9.1, 10.3, 11.3, 12.3_

- [x] 6. 前端页面 - 模板套件列表页
  - [x] 6.1 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template/data.ts`，定义表格列配置（ID、名称、类型、预览图、描述、状态、创建时间、更新时间、创建人、更新者）和搜索表单 Schema（名称模糊搜索、类型下拉选择从字典加载、状态筛选）
    - 类型字段使用 `DictTag` 组件显示中文名称
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 15.2_

  - [x] 6.2 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template/index.vue`，实现模板套件列表页：分页表格（使用 `useVbenVxeGrid`）、搜索筛选、新增/编辑/删除/批量删除按钮、跳转详情页按钮，按创建时间倒序排列
    - 参考 `views/system/config/index.vue` 的模式
    - 权限控制: `v-access:code="['yearark:template:add']"` 等
    - _Requirements: 1.1, 1.6, 2.5, 3.5, 4.4, 4.5_

  - [x] 6.3 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template/template-modal.vue`，实现模板新增/编辑弹窗：表单包含名称（必填）、类型（字典下拉）、预览图（图片上传组件）、描述（文本域）、状态（开关），编辑时加载已有数据
    - 预览图上传使用现有的 upload 组件
    - _Requirements: 2.1, 2.2, 2.6, 2.7, 3.1, 3.2, 3.4, 17.2, 17.3_

- [x] 7. 前端页面 - 模板套件详情页（含模板页管理）
  - [x] 7.1 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template/detail/index.vue`，实现模板详情页布局：顶部显示模板基本信息（名称、类型、预览图、描述、状态、创建时间、修改时间），中部显示纪念册使用数量统计，下部嵌入模板页面列表
    - 通过路由参数 `:id` 获取模板 ID，调用 `templateInfo` 加载详情
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

  - [x] 7.2 在详情页中实现模板页面列表区域：分页表格显示页面 ID、关联 Schema、页面类型、预览图、状态，提供"新增模板页"、"编辑"、"删除"按钮，新增时自动关联当前模板 ID
    - 调用 `templatePagePage` API 并传入 `templateId` 筛选
    - _Requirements: 5.1, 5.5, 5.6, 13.5, 13.6, 13.7, 13.8, 13.9_

  - [x] 7.3 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template/detail/template-page-modal.vue`，实现模板页面新增/编辑弹窗：表单包含页面类型（必填下拉）、关联 Schema（下拉选择，调用 `schemaList` 加载）、H5 模板字符串（代码编辑器组件）、预览图（图片上传）、状态
    - H5 模板字符串编辑使用代码编辑器（如 `codemirror` 或 `monaco-editor`），支持 HTML 语法高亮
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 6.6, 7.1, 7.2, 7.4, 7.5_

- [x] 8. Checkpoint - 模板套件前端页面完成
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. 前端页面 - JSON Schema 管理页
  - [x] 9.1 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template-schema/data.ts`，定义表格列配置（Schema ID、内容预览、状态、创建时间）和搜索表单 Schema（状态筛选）
    - _Requirements: 9.2, 9.3, 9.4_

  - [x] 9.2 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template-schema/index.vue`，实现 JSON Schema 列表页：分页表格、状态筛选、新增/编辑/删除/批量删除按钮，按创建时间倒序排列
    - _Requirements: 9.1, 12.4, 12.5_

  - [x] 9.3 创建 `ruoyi-admin/apps/web-antd/src/views/yearark/template-schema/schema-modal.vue`，实现 Schema 新增/编辑弹窗：表单包含 JSON 内容（JSON 编辑器组件，支持语法高亮和格式验证）、状态，提交前在前端进行 JSON 格式校验
    - JSON 编辑器使用 `codemirror` 或 `monaco-editor`，支持 JSON 语法高亮
    - _Requirements: 10.1, 10.2, 10.4, 10.5, 10.6, 11.1, 11.2, 11.4_

- [x] 10. 前端路由和菜单配置
  - [x] 10.1 配置前端路由：`/yearark/template` 指向模板列表页，`/yearark/template/detail/:id` 指向模板详情页，`/yearark/template-schema` 指向 Schema 管理页；在系统菜单中注册对应的菜单项和权限标识
    - _Requirements: 13.1, 16.1_

- [x] 11. 前端模板预览功能
  - [x] 11.1 在模板详情页中添加"预览"按钮，点击后弹出预览弹窗，根据模板页面的 `content`（H5 模板字符串）和关联的 `JSON Schema` 使用示例数据渲染预览效果；支持单页预览
    - 使用 iframe 或 v-html 渲染 H5 模板
    - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [x] 12. Final checkpoint - 全部功能完成
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 后端基础 CRUD 代码（Controller、Service、Mapper、Entity、DTO/VO）已存在，任务聚焦于增强和前端实现
- 前端参考现有 `views/system/config/` 和 `api/system/config/` 的代码模式
- 文件上传功能复用系统已有的 OSS 上传组件，无需重新实现
- 权限标识遵循 `yearark:template:*`、`yearark:templatePage:*`、`yearark:templateSchema:*` 命名规范
- 字典类型使用系统字典管理功能维护，前端通过 `DictTag` 组件渲染
