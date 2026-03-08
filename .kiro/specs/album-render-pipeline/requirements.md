# 需求文档：纪念册渲染管线重构

## 简介

本需求文档描述纪念册从"拿到素材数据"到"渲染出最终页面"这一核心流程的重构。现有实现采用简单的顺序填充 + 字符串替换方式，无法满足后续 AI 智能分组、JSON Schema 严格校验、前端可视化编辑等需求。

重构后的渲染管线将分为以下阶段：
1. Java 后端通过 RabbitMQ 向 Python AI 服务发送素材数据，Python 服务完成智能分组并生成每页的 Data JSON
2. Java 后端接收 Data JSON 后，使用 JSON Schema 进行结构与业务规则校验
3. 后端将校验通过的 Data JSON 与 HTML 模板合并渲染
4. 前端支持拖动编辑（调整图片位置、修改文案、拖动替换图片）

> **⚠️ 当前阶段实现说明（Mock AI 服务）**
>
> 根据项目优先级，Python AI 服务（需求 1 RabbitMQ 通信、需求 2 AI 分组策略）在当前阶段**不实际开发**，采用以下 Mock 策略：
> - **不集成 RabbitMQ**：`Generation_Service` 内部直接调用 Mock 方法生成 Data JSON
> - **Mock 分组算法**：按素材 sort 顺序依次填充到模板页的 slot 中，素材不足时智能选择能被完全填满的模板页子集（与现有逻辑一致但增加页面选择）
> - **focus_point 默认值**：所有图片的 `focus_point` 统一使用默认值 `{x: 0.5, y: 0.5}`
> - **需求 1、2 保留**：作为未来 AI 服务接入的设计目标和接口契约参考，当前阶段跳过实现

### 项目现状

- **现有生成逻辑**：`AlbumGenerationServiceImpl` 按 sort 顺序将素材依次填入模板页，无智能分组
- **现有渲染逻辑**：`TemplateRenderServiceImpl` 使用简单字符串替换 `{{slot_id}}`，无 Schema 校验
- **现有前端预览**：`BookViewer.vue` 使用 iframe 渲染后端返回的完整 HTML，只读不可编辑
- **Schema 设计**：已有 `ya_template_schema` 表，content 字段存储 slots JSON（`image_N`/`text_N` 命名规则）
- **Python AI 服务**：计划独立部署为 FastAPI 服务，通过 RabbitMQ 与 Java 后端异步通信（该服务本身不在本需求范围内，但需定义通信协议和数据契约）

### 核心流程（重构后）

**当前阶段（Mock AI）：**
```
用户点击"生成纪念册"
    ↓
Java 后端收集素材 + 模板信息
    ↓
Mock 分组：按 sort 顺序将素材填充到模板页 slot，focus_point 默认 {0.5, 0.5}
    ↓
Java 后端：JSON Schema 校验（结构校验 + 业务规则校验）
    ↓
校验通过 → 存储 ya_album_page.data → 渲染 HTML
    ↓
前端：展示渲染结果 + 支持拖动编辑（改文案、拖图片、调位置）
    ↓
用户编辑后 → 前端提交修改后的 Data JSON → 后端重新校验 + 渲染
```

**未来阶段（AI 服务接入后）：**
```
用户点击"生成纪念册"
    ↓
Java 后端收集素材 + 模板信息，发送 MQ 消息给 Python AI 服务
    ↓
Python AI 服务：人脸聚类 → 场景分类 → 智能分组 → 生成每页 Data JSON
    ↓
Java 后端通过 MQ 接收 Data JSON 结果
    ↓
Java 后端：JSON Schema 校验（结构校验 + 业务规则校验）
    ↓
校验通过 → 存储 ya_album_page.data → 渲染 HTML
    ↓
前端：展示渲染结果 + 支持拖动编辑（改文案、拖图片、调位置）
    ↓
用户编辑后 → 前端提交修改后的 Data JSON → 后端重新校验 + 渲染
```

## 术语表

- **Render_Pipeline**：纪念册渲染管线，从素材数据到最终渲染页面的完整处理流程
- **AI_Service**：Python FastAPI 服务，负责素材智能分组和 Data JSON 生成，通过 RabbitMQ 与 Java 后端通信
- **Generation_Service**：Java 后端纪念册生成服务（`AlbumGenerationService`），负责编排整个生成流程
- **Render_Service**：Java 后端模板渲染服务（`TemplateRenderService`），负责将 Data JSON 与 HTML 模板合并
- **Schema_Validator**：JSON Schema 校验器，负责校验 Data JSON 是否符合模板 Schema 定义
- **Data_JSON**：每页的实际内容数据，扁平 key-value 结构（如 `{"image_1": "url", "text_1": "文字"}`），存储在 `ya_album_page.data`
- **JSON_Schema**：存储在 `ya_template_schema.content` 中的 Schema 定义，描述模板页需要的 slot 类型、数量和校验规则
- **MQ_Message**：通过 RabbitMQ 传递的消息，包含生成请求和生成结果
- **Page_Editor**：前端页面编辑器组件，支持拖动图片、修改文案等可视化编辑操作
- **Image_Fit_Strategy**：图片适配策略，描述图片如何填充到模板预留位置（裁剪、缩放、主体识别等）
- **Grouping_Strategy**：AI 素材分组策略，描述如何将素材智能分配到各模板页
- **User_Web**：用户端前端应用（yearark-web）
- **YaAlbumPage**：纪念册页面实体，关联模板页并存储 Data JSON
- **YaTemplatePage**：模板页实体，包含 HTML 模板字符串
- **YaTemplateSchema**：模板 Schema 实体，定义 slot 结构和校验规则

## 需求

### 需求 1：RabbitMQ 消息通信协议

**用户故事：** 作为系统架构师，我希望定义 Java 后端与 Python AI 服务之间的 RabbitMQ 通信协议，以便两个服务能可靠地异步交换素材分组和 Data JSON 生成的数据。

#### 验收标准

1. THE Generation_Service SHALL 定义生成请求消息结构，包含纪念册 ID、素材列表（每条素材包含 ID、类型、内容 URL/文本、人脸数量、标签）、模板页列表（每页包含模板页 ID、关联 Schema ID、imageCount、textCount）和纪念册元信息（名称、描述）
2. THE AI_Service SHALL 定义生成结果消息结构，包含纪念册 ID、状态码（成功/失败）、每页的 Data JSON 列表（每项包含模板页 ID 和对应的 Data JSON 对象）以及失败时的错误信息
3. THE Generation_Service SHALL 使用 RabbitMQ Direct Exchange 模式，定义请求队列（`album.generation.request`）和结果队列（`album.generation.result`），消息格式为 JSON，消息体包含 correlationId 用于请求-响应关联
4. THE Generation_Service SHALL 在发送 MQ 消息时设置消息过期时间（TTL）为 5 分钟，防止消息在队列中无限堆积
5. IF AI_Service 在 5 分钟内未返回结果, THEN THE Generation_Service SHALL 将该纪念册的生成状态标记为"超时失败"并记录日志

### 需求 2：AI 素材分组策略设计

**用户故事：** 作为产品经理，我希望 AI 服务能智能地将素材分组到各模板页，以便生成的纪念册在视觉和内容上更有意义。

#### 验收标准

1. THE AI_Service SHALL 实现三级分组策略：第一级按人脸聚类将包含相同人物的照片归为一组，第二级按场景分类（如室内、室外、合影、活动）对同一人物组内的照片进行细分，第三级按时间顺序对同一场景内的照片排序
2. THE AI_Service SHALL 在分组完成后，根据每个模板页的 imageCount 和 textCount 需求，将素材组匹配到对应的模板页，优先将同组素材分配到同一页面
3. WHEN 素材总量超过所有模板页的总容量, THE AI_Service SHALL 按照素材质量评分（清晰度、构图）选择最优素材，丢弃低质量素材
4. WHEN 素材总量不足以填满所有模板页, THE AI_Service SHALL 根据可用素材数量从模板页列表中智能选择能被完全填满的页面子集（优先保留 imageCount 较小的页面），跳过无法填满的页面，确保每个被选中的模板页的所有 required slot 都有真实素材填充，绝不使用占位图或空字符串
5. THE AI_Service SHALL 为每张图片素材生成一个 `focus_point` 字段（归一化坐标 `{x: 0.0~1.0, y: 0.0~1.0}`），标识图片主体的中心位置，用于前端裁剪时的焦点定位
6. THE AI_Service SHALL 为每页生成的 Data JSON 中包含文字素材的智能匹配结果，将语义相关的文字与图片分配到同一页面

### 需求 3：JSON Schema 扩展与校验

**用户故事：** 作为开发者，我希望 JSON Schema 能定义更丰富的校验规则，以便在 Data JSON 存储前确保数据的结构正确性和业务合规性。

#### 验收标准

1. THE JSON_Schema SHALL 在现有 slot 定义基础上扩展以下校验规则字段：`maxLength`（文字最大长度）、`minLength`（文字最小长度）、`pattern`（文字正则匹配，如日期格式）、`imageMinWidth`（图片最小宽度像素）、`imageMinHeight`（图片最小高度像素）、`imageAspectRatio`（图片推荐宽高比，如 `"16:9"`）
2. THE Schema_Validator SHALL 在 Java 后端实现，接收 Data JSON 和对应的 JSON Schema，逐个 slot 进行校验，返回校验结果列表（每个 slot 的通过/失败状态及失败原因）
3. WHEN Data JSON 中某个 required slot 的值为空或缺失, THE Schema_Validator SHALL 将该 slot 标记为校验失败，失败原因为"必填字段不能为空"
4. WHEN Data JSON 中某个 text 类型 slot 的值超过 maxLength 限制, THE Schema_Validator SHALL 将该 slot 标记为校验失败，失败原因包含实际长度和最大长度
5. WHEN Data JSON 中某个 image 类型 slot 的值不是合法的 URL 格式, THE Schema_Validator SHALL 将该 slot 标记为校验失败，失败原因为"图片地址格式不合法"
6. THE Schema_Validator SHALL 返回结构化的校验结果对象，包含整体是否通过、失败的 slot 列表及每个失败 slot 的详细原因，Generation_Service 根据校验结果决定是否继续渲染
7. THE JSON_Schema SHALL 支持 `default` 字段，WHEN 某个非必填 slot 在 Data JSON 中缺失时, THE Schema_Validator SHALL 使用 default 值自动填充

### 需求 4：纪念册生成流程重构

**用户故事：** 作为纪念册创建者，我希望点击"生成纪念册"后，系统能通过 AI 智能分组素材并生成高质量的纪念册页面。

#### 验收标准

1. WHEN 用户请求生成纪念册, THE Generation_Service SHALL 验证纪念册已关联模板且至少有 1 条 status 为 2 的素材，验证通过后将纪念册生成状态更新为"生成中"
2. THE Generation_Service SHALL 收集纪念册的所有审核通过素材和模板页信息，组装为 MQ 请求消息，发送到 `album.generation.request` 队列
3. WHEN Generation_Service 从 `album.generation.result` 队列接收到 AI_Service 返回的 Data JSON 结果, THE Generation_Service SHALL 对每页的 Data JSON 调用 Schema_Validator 进行校验
4. WHEN 所有页面的 Data JSON 校验通过, THE Generation_Service SHALL 清除该纪念册已有的 ya_album_page 记录，为每页创建新的 ya_album_page 记录（data 字段存储 Data JSON），并将纪念册状态更新为"已生成"
5. IF 某页的 Data JSON 校验失败, THEN THE Generation_Service SHALL 记录校验失败详情日志，对该页使用回退策略（按顺序填充素材生成 Data JSON），确保生成流程不会因单页校验失败而中断
6. IF AI_Service 返回失败状态, THEN THE Generation_Service SHALL 使用现有的顺序填充算法作为降级方案完成生成，并在生成结果中标记"使用了降级方案"
7. THE Generation_Service SHALL 在 ya_album 表中新增 `generation_status` 字段，支持以下状态流转：`pending`（待生成）→ `processing`（生成中）→ `completed`（已完成）→ `failed`（失败），前端根据此状态展示生成进度

### 需求 5：图片适配与主体识别

**用户故事：** 作为纪念册创建者，我希望图片能智能适配模板中的预留位置，即使图片尺寸与模板不匹配也能展示出最佳效果。

#### 验收标准

1. THE AI_Service SHALL 为每张图片生成 `focus_point` 坐标（归一化值 `{x: 0.0~1.0, y: 0.0~1.0}`），该坐标标识图片中最重要的主体区域中心点
2. THE Data_JSON SHALL 为每个 image 类型 slot 扩展存储结构，从单一 URL 字符串改为对象格式：`{"url": "图片URL", "focus_x": 0.5, "focus_y": 0.3}`，同时保持向后兼容（纯字符串 URL 视为 focus_point 默认居中 `{0.5, 0.5}`）
3. THE Render_Service SHALL 在渲染 HTML 时，将 image slot 的 `focus_x` 和 `focus_y` 值注入到 `<img>` 标签的 `style` 属性中，使用 CSS `object-fit: cover` 配合 `object-position` 实现基于焦点的裁剪
4. THE JSON_Schema SHALL 支持 `imageAspectRatio` 字段（如 `"16:9"`、`"1:1"`、`"3:4"`），Schema_Validator 在校验时记录宽高比不匹配的警告（非阻断性），供前端展示提示
5. IF 图片的 focus_point 数据缺失, THEN THE Render_Service SHALL 使用默认焦点 `{x: 0.5, y: 0.5}`（居中裁剪）进行渲染

### 需求 6：前端页面编辑器

**用户故事：** 作为纪念册创建者，我希望能在预览页面上直接拖动编辑纪念册内容，以便对 AI 生成的结果进行微调。

#### 验收标准

1. THE User_Web SHALL 在纪念册预览页面提供"编辑模式"开关，点击后进入可编辑状态，页面从 iframe 只读渲染切换为 Vue 组件可交互渲染
2. WHILE 处于编辑模式, THE Page_Editor SHALL 将每页的 Data JSON 解析为可编辑元素，每个 text 类型 slot 渲染为可点击编辑的文本区域，每个 image 类型 slot 渲染为可操作的图片区域
3. WHEN 用户点击某个 text slot 区域, THE Page_Editor SHALL 显示内联文本编辑器，用户可直接修改文案内容，修改后实时更新该 slot 在 Data JSON 中的值
4. WHEN 用户拖动一张图片到另一个 image slot 位置, THE Page_Editor SHALL 交换两个 slot 的图片数据（URL 和 focus_point），实现图片位置互换
5. THE Page_Editor SHALL 支持从素材库面板拖入新图片替换当前 image slot，素材库面板展示该纪念册所有未使用的图片素材
6. WHEN 用户点击某个 image slot, THE Page_Editor SHALL 显示焦点调整工具，用户可通过拖动十字准星调整 `focus_point` 坐标，实时预览裁剪效果
7. WHEN 用户完成编辑并点击"保存", THE Page_Editor SHALL 将修改后的 Data JSON 提交到后端，Generation_Service 对提交的 Data JSON 进行 Schema 校验，校验通过后更新 ya_album_page.data 并返回重新渲染的 HTML
8. IF 用户提交的 Data JSON 校验失败, THEN THE Page_Editor SHALL 在对应的 slot 位置显示红色边框和错误提示信息，阻止保存操作直到所有校验错误被修复

### 需求 7：生成状态与进度反馈

**用户故事：** 作为纪念册创建者，我希望能看到纪念册生成的实时进度，以便了解当前处理状态。

#### 验收标准

1. THE User_Web SHALL 在纪念册详情页展示当前生成状态，包括"待生成"、"生成中"、"已完成"、"失败"四种状态的可视化标识
2. WHILE 纪念册处于"生成中"状态, THE User_Web SHALL 每 5 秒轮询一次后端接口获取最新生成状态，并展示加载动画
3. WHEN 生成状态变为"已完成", THE User_Web SHALL 自动停止轮询并显示"生成完成"提示，同时启用"预览"和"编辑"按钮
4. WHEN 生成状态变为"失败", THE User_Web SHALL 停止轮询并显示失败原因，提供"重新生成"按钮
5. IF 纪念册使用了降级方案生成, THEN THE User_Web SHALL 在生成结果中显示提示信息"本次使用了基础排版方案，建议重新生成以获得更好效果"

### 需求 8：编辑后保存与重新渲染接口

**用户故事：** 作为开发者，我希望后端提供编辑保存接口，以便前端编辑器修改后的 Data JSON 能被校验、存储并重新渲染。

#### 验收标准

1. THE Generation_Service SHALL 提供单页 Data JSON 更新接口，接收纪念册页面 ID 和修改后的 Data JSON，执行 Schema 校验后更新 ya_album_page.data 字段
2. WHEN 单页更新请求的 Data JSON 校验通过, THE Render_Service SHALL 使用更新后的 Data JSON 重新渲染该页 HTML 并返回给前端
3. IF 单页更新请求的 Data JSON 校验失败, THEN THE Generation_Service SHALL 返回结构化的校验错误列表，不更新数据库中的 Data JSON
4. THE Generation_Service SHALL 在更新 ya_album_page.data 时校验该页面归属于当前用户的纪念册，防止越权修改
5. THE Generation_Service SHALL 支持批量更新接口，接收多页的 Data JSON 修改，在同一事务中完成校验和更新，任一页校验失败则整个批量操作回滚
