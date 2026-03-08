# YearArk 模板制作规范手册

> 面向模板制作者。本手册涵盖 Schema JSON 格式、H5 模板 HTML 编写规范、数据格式说明及数据库录入步骤。

---

## 一、核心概念

一个"模板页"由三部分组成：

| 组成 | 存储位置 | 作用 |
|------|----------|------|
| Schema JSON | `ya_template_schema.content` | 声明这个模板页需要哪些素材（几张图、几段文字） |
| HTML 模板 | `ya_template_page.content` | 带占位符的 H5 页面，定义视觉布局 |

**渲染流程：**
```
Data JSON  →  Schema 校验  →  替换 HTML 占位符  →  输出完整 HTML
```

---

## 二、Slot 命名规则（最重要）

所有占位符（slot）必须使用**类型 + 编号**的格式，不允许使用语义化命名。

| 类型 | 格式 | 示例 |
|------|------|------|
| 图片 | `image_N`（N 从 1 开始） | `image_1`、`image_2`、`image_3` |
| 文字 | `text_N`（N 从 1 开始） | `text_1`、`text_2` |

**正确示例：** `image_1`、`text_1`、`image_2`
**错误示例：** `cover_photo`、`title`、`main_image`（语义化命名，禁止使用）

> 原因：同一个 Schema 下的多个模板页可以互相替换，编号式命名保证 Data JSON 无需修改即可切换模板。

---

## 三、Schema JSON 格式规范

### 3.1 完整格式

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

### 3.2 字段说明

#### 通用字段（所有 slot 类型）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 占位符 ID，命名规则见第二节，对应 HTML 中的 `{{id}}` |
| `type` | string | 是 | 类型：`image` 或 `text` |
| `label` | string | 是 | 中文标签，用于管理端展示和 AI 提示（如"封面照片"、"标题"） |
| `required` | boolean | 是 | 是否必填。`true` = 必须有值，`false` = 可选 |
| `default` | string | 否 | 非必填 slot 的默认值，缺失时自动填充 |

#### image slot 专属字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `width` | number | 否 | 图片框设计宽度（像素），供 AI 参考图片尺寸匹配 |
| `height` | number | 否 | 图片框设计高度（像素） |

#### text slot 专属字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `maxLength` | number | 否 | 文字最大字符数，超出时校验失败 |

### 3.3 校验规则

系统会对 Data JSON 进行以下校验：

1. `required: true` 的 slot 必须存在且值非空
2. `type: text` 的 slot 若定义了 `maxLength`，文字长度不能超过该值
3. `type: image` 的 slot 的 URL 必须以 `http://` 或 `https://` 开头
4. 非必填 slot 缺失时，若定义了 `default`，系统自动填充默认值

---

## 四、H5 模板 HTML 编写规范

### 4.1 占位符语法

使用双花括号 `{{slot_id}}` 作为占位符：

```html
<!-- 图片占位符 -->
<img src="{{image_1}}" />

<!-- 文字占位符 -->
<p>{{text_1}}</p>
<h1>{{text_2}}</h1>
```

### 4.2 图片标签要求

**必须使用 `<img>` 标签**，不能用 CSS background-image 方式引用图片 slot。

系统渲染时会自动在 `<img>` 标签上注入焦点和缩放样式：

```css
object-fit: cover;
object-position: {focus_x}% {focus_y}%;
transform: scale({scale});
transform-origin: {focus_x}% {focus_y}%;
```

因此图片容器需要设置 `overflow: hidden` 以确保裁剪效果正常显示：

```html
<div class="photo-frame" style="overflow:hidden; width:400px; height:300px;">
  <img src="{{image_1}}" style="width:100%; height:100%;" />
</div>
```

### 4.3 页面尺寸规范

- 纪念册页面固定宽高比为 A4（竖版）
- 推荐设计尺寸：`750px × 1000px`
- 使用相对单位（`%`、`vw/vh`）或固定像素均可，但需确保在容器内正确缩放

### 4.4 完整 HTML 模板示例

```html
<div class="page" style="width:750px; height:1000px; position:relative; background:#fff; font-family:sans-serif;">
  <div class="photo-frame" style="overflow:hidden; position:absolute; top:50px; left:50px; width:650px; height:500px;">
    <img src="{{image_1}}" style="width:100%; height:100%; object-fit:cover;" />
  </div>
  <h1 style="position:absolute; top:580px; left:50px; right:50px; font-size:36px; text-align:center;">{{text_1}}</h1>
  <p style="position:absolute; top:640px; left:50px; right:50px; font-size:20px; color:#666; text-align:center;">{{text_2}}</p>
</div>
```

---

## 六、完整示例

### 示例 1：封面页（1图2文）

**Schema JSON（imageCount=1, textCount=2）：**
```json
{
  "slots": [
    { "id": "image_1", "type": "image", "label": "封面照片", "required": true, "width": 750, "height": 600 },
    { "id": "text_1", "type": "text", "label": "标题", "required": true, "maxLength": 20 },
    { "id": "text_2", "type": "text", "label": "副标题", "required": false, "maxLength": 50, "default": "" }
  ]
}
```

**HTML 模板：**
```html
<div class="page" style="width:750px; height:1000px; position:relative; background:#1a1a2e;">
  <div style="overflow:hidden; position:absolute; top:0; left:0; width:750px; height:600px;">
    <img src="{{image_1}}" style="width:100%; height:100%; object-fit:cover;" />
  </div>
  <div style="position:absolute; bottom:0; left:0; right:0; height:400px; background:linear-gradient(transparent, #1a1a2e); display:flex; flex-direction:column; justify-content:flex-end; padding:40px;">
    <h1 style="color:#fff; font-size:40px; margin:0 0 12px;">{{text_1}}</h1>
    <p style="color:rgba(255,255,255,0.7); font-size:22px; margin:0;">{{text_2}}</p>
  </div>
</div>
```

---

### 示例 2：四格拼图页（4图1文）

**Schema JSON（imageCount=4, textCount=1）：**
```json
{
  "slots": [
    { "id": "image_1", "type": "image", "label": "照片1", "required": true, "width": 370, "height": 370 },
    { "id": "image_2", "type": "image", "label": "照片2", "required": true, "width": 370, "height": 370 },
    { "id": "image_3", "type": "image", "label": "照片3", "required": true, "width": 370, "height": 370 },
    { "id": "image_4", "type": "image", "label": "照片4", "required": true, "width": 370, "height": 370 },
    { "id": "text_1", "type": "text", "label": "说明文字", "required": false, "maxLength": 60, "default": "" }
  ]
}
```

**HTML 模板（网格风格）：**
```html
<div class="page" style="width:750px; height:1000px; position:relative; background:#fff;">
  <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px; padding:10px; height:760px;">
    <div style="overflow:hidden;"><img src="{{image_1}}" style="width:100%; height:100%; object-fit:cover;" /></div>
    <div style="overflow:hidden;"><img src="{{image_2}}" style="width:100%; height:100%; object-fit:cover;" /></div>
    <div style="overflow:hidden;"><img src="{{image_3}}" style="width:100%; height:100%; object-fit:cover;" /></div>
    <div style="overflow:hidden;"><img src="{{image_4}}" style="width:100%; height:100%; object-fit:cover;" /></div>
  </div>
  <p style="position:absolute; bottom:20px; left:0; right:0; text-align:center; font-size:20px; color:#666;">{{text_1}}</p>
</div>
```

> 同一个 Schema 可以对应多个不同布局的 HTML 模板（如瀑布流、轮播等），Data JSON 完全不变。


## 七、数据库录入说明

### 7.1 录入 Schema（ya_template_schema）

| 字段 | 说明 | 示例 |
|------|------|------|
| `content` | Schema JSON 字符串（见第三节） | `{"slots":[...]}` |
| `image_count` | Schema 中 `type=image` 的 slot 数量 | `1` |
| `text_count` | Schema 中 `type=text` 的 slot 数量 | `2` |
| `status` | 状态，`1` = 启用 | `1` |

> `image_count` 和 `text_count` 必须与 `content` 中实际的 slot 数量一致，系统通过这两个字段快速匹配可用 Schema，不一致会导致匹配错误。

**SQL 示例：**
```sql
INSERT INTO ya_template_schema (content, image_count, text_count, status)
VALUES (
  '{"slots":[{"id":"image_1","type":"image","label":"封面照片","required":true,"width":750,"height":600},{"id":"text_1","type":"text","label":"标题","required":true,"maxLength":20},{"id":"text_2","type":"text","label":"副标题","required":false,"maxLength":50,"default":""}]}',
  1, 2, 1
);
```

### 7.2 录入模板页（ya_template_page）

| 字段 | 说明 |
|------|------|
| `template_id` | 所属模板 ID |
| `schema_id` | 关联的 Schema ID（必须与 HTML 中的占位符对应） |
| `content` | HTML 模板字符串（见第四节） |
| `sort` | 页面排序，从 1 开始 |
| `status` | 状态，`1` = 启用 |

**SQL 示例：**
```sql
INSERT INTO ya_template_page (template_id, schema_id, content, sort, status)
VALUES (
  1,
  1,
  '<div class="page">...</div>',
  1,
  1
);
```

### 7.3 录入检查清单

录入前请逐项确认：

- [ ] Schema 中每个 slot 的 `id` 符合 `image_N` / `text_N` 命名规则
- [ ] `image_count` = Schema 中 `type=image` 的 slot 数量
- [ ] `text_count` = Schema 中 `type=text` 的 slot 数量
- [ ] HTML 中每个 `{{slot_id}}` 都在 Schema 的 slots 中有对应定义
- [ ] 图片 slot 使用 `<img src="{{image_N}}">` 标签（不用 background-image）
- [ ] 图片容器设置了 `overflow:hidden`

---

## 八、常见错误

| 错误 | 原因 | 修正方式 |
|------|------|----------|
| 图片不显示 | HTML 中用了 `background-image: url({{image_1}})` | 改为 `<img src="{{image_1}}">` |
| 图片不裁剪 | 图片容器没有 `overflow:hidden` | 给容器加 `overflow:hidden` |
| 生成失败 | `image_count` / `text_count` 与 Schema 实际数量不符 | 重新统计并更新数据库字段 |
| 换模板后内容错位 | slot id 使用了语义化命名（如 `cover_photo`） | 改为 `image_1`、`text_1` 等编号式命名 |
| 文字被截断 | `maxLength` 设置过小 | 调大 `maxLength` 或改为 `null` |
| 必填项报错 | `required: true` 但 AI 没有填充该 slot | 检查素材数量是否满足模板需求 |
