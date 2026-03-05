# 需求文档 - 管理端模板管理功能

## 介绍

本功能为 YearArk 纪念册系统的管理端提供完整的模板管理能力，包括模板套件（YaTemplate）、模板页面（YaTemplatePage）和 JSON Schema（YaTemplateSchema）的增删改查操作。管理员可以通过此功能创建和维护纪念册模板，为用户端提供多样化的模板选择。

## 术语表

- **Template_Management_System**: 模板管理系统，负责处理模板套件、模板页面和 JSON Schema 的管理
- **Admin_User**: 管理端用户，具有模板管理权限的系统管理员
- **Template_Suite**: 模板套件（YaTemplate），包含多个模板页面的完整模板集合
- **Template_Page**: 模板页面（YaTemplatePage），模板套件中的单个页面，包含 H5 模板字符串
- **JSON_Schema**: JSON Schema（YaTemplateSchema），定义模板页面中占位区域的结构配置
- **H5_Template_String**: H5 模板字符串，存储在 Template_Page.content 字段中的 HTML 模板代码
- **Placeholder_Area**: 占位区域，在 JSON Schema 中定义的可替换内容区域（图片或文字）
- **Template_Type**: 模板类型，使用数据字典管理的模板分类
- **Album**: 纪念册，使用模板套件创建的用户纪念册实例

## 需求

### 需求 1: 模板套件列表查询

**用户故事:** 作为管理员，我想要查看所有模板套件的列表，以便了解系统中已有的模板资源。

#### 验收标准

1. THE Template_Management_System SHALL 提供分页查询接口返回模板套件列表
2. THE Template_Management_System SHALL 支持按模板名称进行模糊搜索
3. THE Template_Management_System SHALL 支持按模板类型进行筛选
4. THE Template_Management_System SHALL 支持按状态进行筛选
5. THE Template_Management_System SHALL 在列表中显示模板 ID、名称、类型、预览图、描述、状态和创建时间、更新时间、创建人、更新者
6. THE Template_Management_System SHALL 按创建时间倒序排列模板套件

### 需求 2: 模板套件创建

**用户故事:** 作为管理员，我想要创建新的模板套件，以便为用户提供新的纪念册模板选择。

#### 验收标准

1. WHEN Admin_User 提交模板套件创建请求，THE Template_Management_System SHALL 验证必填字段（名称）
2. WHEN 模板名称为空，THE Template_Management_System SHALL 返回错误提示"请输入模板名称"
4. WHEN 验证通过，THE Template_Management_System SHALL 创建模板套件记录并自动填充创建时间和创建人
5. WHEN 创建成功，THE Template_Management_System SHALL 返回成功提示并刷新列表
6. THE Template_Management_System SHALL 支持上传预览图片到 OSS 并保存 URL
7. THE Template_Management_System SHALL 将模板状态默认设置为启用

### 需求 3: 模板套件编辑

**用户故事:** 作为管理员，我想要编辑已有的模板套件信息，以便更新模板的描述或预览图。

#### 验收标准

1. WHEN Admin_User 选择编辑模板套件，THE Template_Management_System SHALL 加载该模板的完整信息
2. WHEN Admin_User 提交更新请求，THE Template_Management_System SHALL 验证必填字段
3. WHEN 验证通过，THE Template_Management_System SHALL 更新模板套件记录并自动更新修改时间和修改人
4. THE Template_Management_System SHALL 支持修改模板名称、类型、预览图、描述和状态
5. WHEN 更新成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 4: 模板套件删除

**用户故事:** 作为管理员，我想要删除不再使用的模板套件，以便保持模板库的整洁。

#### 验收标准

1. WHEN Admin_User 请求删除模板套件，THE Template_Management_System SHALL 检查该模板是否被纪念册使用
2. IF 模板已被纪念册使用，THEN THE Template_Management_System SHALL 返回错误提示"该模板已被使用，请先删除该模板下的所有相册"
3. IF 模板未被使用，THEN THE Template_Management_System SHALL 执行逻辑删除
4. THE Template_Management_System SHALL 支持批量删除多个模板套件
5. WHEN 删除成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 5: 模板页面列表查询

**用户故事:** 作为管理员，我想要在模板套件详情页中查看该模板下的所有模板页面，以便管理模板的页面结构。

#### 验收标准

1. WHEN Admin_User 进入模板套件详情页，THE Template_Management_System SHALL 自动加载该模板下的所有模板页面
2. THE Template_Management_System SHALL 提供分页查询接口返回模板页面列表
4. THE Template_Management_System SHALL 支持按页面类型筛选
5. THE Template_Management_System SHALL 在列表中显示页面 ID、关联 Schema、页面类型、预览图和状态
6. THE Template_Management_System SHALL 按创建时间倒序排列模板页面

### 需求 6: 模板页面创建

**用户故事:** 作为管理员，我想要为模板套件添加新的页面，以便丰富模板的内容结构。

#### 验收标准

1. WHEN Admin_User 提交模板页面创建请求，THE Template_Management_System SHALL 验证必填字段（模板 ID、页面类型、H5 模板字符串）
2. WHEN 页面类型未选择，THE Template_Management_System SHALL 返回错误提示"请选择页面类型"
3. WHEN H5 模板字符串为空，THE Template_Management_System SHALL 返回错误提示"请输入模板内容"
4. WHEN 验证通过，THE Template_Management_System SHALL 创建模板页面记录并自动填充创建时间和创建人
5. THE Template_Management_System SHALL 支持关联已有的 JSON Schema
6. THE Template_Management_System SHALL 支持上传单页预览图到 OSS 并保存 URL
7. WHEN 创建成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 7: 模板页面编辑

**用户故事:** 作为管理员，我想要编辑模板页面的内容和配置，以便优化模板效果。

#### 验收标准

1. WHEN Admin_User 选择编辑模板页面，THE Template_Management_System SHALL 加载该页面的完整信息包括 H5 模板字符串
2. WHEN Admin_User 提交更新请求，THE Template_Management_System SHALL 验证必填字段
3. WHEN 验证通过，THE Template_Management_System SHALL 更新模板页面记录并自动更新修改时间和修改人
4. THE Template_Management_System SHALL 支持修改 H5 模板字符串、关联 Schema、页面类型、预览图和状态
5. THE Template_Management_System SHALL 提供代码编辑器用于编辑 H5 模板字符串
6. WHEN 更新成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 8: 模板页面删除

**用户故事:** 作为管理员，我想要删除不需要的模板页面，以便调整模板结构。

#### 验收标准

1. WHEN Admin_User 请求删除模板页面，THE Template_Management_System SHALL 执行逻辑删除
2. THE Template_Management_System SHALL 支持批量删除多个模板页面
3. WHEN 删除成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 9: JSON Schema 列表查询

**用户故事:** 作为管理员，我想要查看所有 JSON Schema 配置，以便了解可用的占位区域定义。

#### 验收标准

1. THE Template_Management_System SHALL 提供分页查询接口返回 JSON Schema 列表
2. THE Template_Management_System SHALL 支持按状态筛选
3. THE Template_Management_System SHALL 在列表中显示 Schema ID、内容预览、状态和创建时间
4. THE Template_Management_System SHALL 按创建时间倒序排列 JSON Schema

### 需求 10: JSON Schema 创建

**用户故事:** 作为管理员，我想要创建新的 JSON Schema 配置，以便定义模板页面的占位区域结构。

#### 验收标准

1. WHEN Admin_User 提交 JSON Schema 创建请求，THE Template_Management_System SHALL 验证 JSON 格式的有效性
2. WHEN JSON 格式无效，THE Template_Management_System SHALL 返回错误提示"JSON 格式不正确"
3. WHEN 验证通过，THE Template_Management_System SHALL 创建 JSON Schema 记录并自动填充创建时间和创建人
4. THE Template_Management_System SHALL 提供 JSON 编辑器用于编辑 Schema 内容
5. THE Template_Management_System SHALL 支持定义图片类型占位区域（包含位置、尺寸、数量等属性）
6. THE Template_Management_System SHALL 支持定义文字类型占位区域（包含位置、字数限制、样式等属性）
7. WHEN 创建成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 11: JSON Schema 编辑

**用户故事:** 作为管理员，我想要编辑 JSON Schema 配置，以便调整占位区域的定义。

#### 验收标准

1. WHEN Admin_User 选择编辑 JSON Schema，THE Template_Management_System SHALL 加载该 Schema 的完整内容
2. WHEN Admin_User 提交更新请求，THE Template_Management_System SHALL 验证 JSON 格式的有效性
3. WHEN 验证通过，THE Template_Management_System SHALL 更新 JSON Schema 记录并自动更新修改时间和修改人
4. THE Template_Management_System SHALL 提供 JSON 编辑器用于编辑 Schema 内容
5. WHEN 更新成功，THE Template_Management_System SHALL 返回成功提示并刷新列表

### 需求 12: JSON Schema 删除

**用户故事:** 作为管理员，我想要删除不再使用的 JSON Schema，以便保持配置库的整洁。

#### 验收标准

1. WHEN Admin_User 请求删除 JSON Schema，THE Template_Management_System SHALL 检查该 Schema 是否被模板页面引用
2. IF Schema 被模板页面引用，THEN THE Template_Management_System SHALL 返回错误提示"该 Schema 已被模板页面使用，无法删除"
3. IF Schema 未被引用，THEN THE Template_Management_System SHALL 执行逻辑删除
4. THE Template_Management_System SHALL 支持批量删除多个 JSON Schema
5. WHEN 删除成功，THE Template_Management_System SHALL 返回成功提示并刷新列表
### 需求 13: 模板套件详情查看与模板页管理

**用户故事:** 作为管理员，我想要在模板套件详情页中查看模板信息并管理其下的所有模板页面，以便集中管理模板的完整结构。

#### 验收标准

1. WHEN Admin_User 在模板列表中点击"详情"按钮，THE Template_Management_System SHALL 进入该模板的详情页面
2. THE Template_Management_System SHALL 在详情页顶部显示模板的基本信息（名称、类型、预览图、描述、状态、创建时间和修改时间）
3. THE Template_Management_System SHALL 在详情页中部显示该模板被使用的纪念册数量统计
4. THE Template_Management_System SHALL 在详情页下部显示该模板下所有关联的模板页面列表
5. THE Template_Management_System SHALL 在模板页列表区域提供"新增模板页"按钮
6. THE Template_Management_System SHALL 在模板页列表的每一行提供"编辑"和"删除"操作按钮
7. WHEN Admin_User 点击"新增模板页"，THE Template_Management_System SHALL 自动关联当前模板套件 ID
8. THE Template_Management_System SHALL 支持在详情页内完成模板页的增删改操作而无需跳转到其他页面
9. WHEN 模板页操作完成，THE Template_Management_System SHALL 自动刷新详情页中的模板页列表面列表
4. THE Template_Management_System SHALL 显示该模板被使用的纪念册数量

### 需求 14: 模板预览功能

**用户故事:** 作为管理员，我想要预览模板的实际效果，以便验证模板配置是否正确。

#### 验收标准

1. WHEN Admin_User 点击预览模板套件，THE Template_Management_System SHALL 渲染模板的所有页面
2. THE Template_Management_System SHALL 使用示例数据填充占位区域
3. THE Template_Management_System SHALL 支持单页预览功能
4. WHEN 预览模板页面，THE Template_Management_System SHALL 根据 H5_Template_String 和 JSON_Schema 渲染页面

### 需求 15: 模板类型字典管理

**用户故事:** 作为管理员，我想要管理模板类型字典，以便灵活定义模板分类。

#### 验收标准

1. THE Template_Management_System SHALL 使用系统字典（sys_dict_data）管理模板类型
2. THE Template_Management_System SHALL 在创建和编辑模板时从字典加载类型选项
3. THE Template_Management_System SHALL 支持通过系统字典管理功能添加新的模板类型
4. THE Template_Management_System SHALL 在模板列表中显示类型的中文名称而非代码值

### 需求 16: 权限控制

**用户故事:** 作为系统管理员，我想要控制模板管理功能的访问权限，以便保护模板数据安全。

#### 验收标准

1. THE Template_Management_System SHALL 验证 Admin_User 是否具有模板管理权限
2. WHEN Admin_User 无权限访问，THE Template_Management_System SHALL 返回 403 错误
3. THE Template_Management_System SHALL 记录所有模板管理操作到操作日志（sys_oper_log）
4. THE Template_Management_System SHALL 在创建和修改记录时自动记录操作人信息

### 需求 17: 文件上传管理

**用户故事:** 作为管理员，我想要上传模板预览图和页面预览图，以便用户直观了解模板效果。

#### 验收标准

1. THE Template_Management_System SHALL 支持上传图片文件到阿里云 OSS
2. THE Template_Management_System SHALL 验证上传文件的格式（仅支持 jpg、png、webp）
3. THE Template_Management_System SHALL 验证上传文件的大小（不超过 5MB）
4. WHEN 文件格式不正确，THE Template_Management_System SHALL 返回错误提示"仅支持 jpg、png、webp 格式"
5. WHEN 文件大小超限，THE Template_Management_System SHALL 返回错误提示"文件大小不能超过 5MB"
6. WHEN 上传成功，THE Template_Management_System SHALL 返回文件的 OSS URL
7. THE Template_Management_System SHALL 记录上传文件信息到 sys_file_info 表

### 需求 18: 数据验证和错误处理

**用户故事:** 作为管理员，我想要系统提供清晰的错误提示，以便快速定位和解决问题。

#### 验收标准

1. WHEN 请求参数缺失或格式错误，THE Template_Management_System SHALL 返回 400 错误和具体错误信息
2. WHEN 数据库操作失败，THE Template_Management_System SHALL 返回 500 错误和友好的错误提示
3. WHEN JSON 解析失败，THE Template_Management_System SHALL 返回具体的解析错误位置
4. THE Template_Management_System SHALL 验证所有外键关联的有效性
5. WHEN 关联的模板套件不存在，THE Template_Management_System SHALL 返回错误提示"关联的模板套件不存在"
6. WHEN 关联的 JSON Schema 不存在，THE Template_Management_System SHALL 返回错误提示"关联的 Schema 不存在"
