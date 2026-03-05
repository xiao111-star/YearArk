# YearArk 模板 Schema 与数据格式设计文档

## 概述

本文档定义了 YearArk 纪念册系统中三个核心数据结构的格式规范：
1. **JSON Schema**（`ya_template_schema.content`）—— 描述模板页需要什么
2. **Data JSON**（`ya_album_page.data`）—— 填入模板页的实际内容
3. **HTML 模板**（`ya_template_page.content`）—— 带占位符的 HTML 页面

以及它们之间的关系和渲染流程。

## 核心设计思想

### Schema 是模板页与数据之间的"契约"

```
Data JSON  ←→  Schema  ←→  模板页A（网格风格）
                       ←→  模板页B（瀑布流风格）
                       ←→  模板页C（轮播风格）
```

- 多个模板页可以关联同一个 Schema
- 同 Schema 下的模板页可以**无缝替换**，Data JSON 不需要任何修改
- 换模板 = 换 `template_page_id`，数据完全不动

### 统一命名规则保证可替换性

slot id 采用**类型+编号**的命名方式，而非语义化命名：

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 图片 | `image_N` | `image_1`, `image_2`, `image_3` |
| 文字 | `text_N` | `text_1`, `text_2`, `text_3` |

**为什么不用语义化命名（如 `cover_photo`、`title`）：**
- 不同模板设计师会取不同的名字，换模板时 key 对不上
- 有序编号天然支持匹配，`image_1` 在任何模板里都是第一张图
- AI 生成 Data JSON 时更容易遵循规则

## 数据结构定义

### 1. JSON Schema（ya_template_schema）

#### 数据库字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键 |
| content | TEXT | JSON Schema 内容 |
| image_count | INT | 图片数量（从 JSON 提升为独立字段，方便 SQL 查询匹配） |
| text_count | INT | 文字数量（同上） |
| status | INT | 状态 |

#### content JSON 格式

```json
{
  "slots": [
    { "id": "image_1", "type": "image", "label": "照片1", "required": true },
    { "id": "text_1", "type": "text", "label": "标题", "required": true, "maxLength": 20 }
  ]
}
```

#### slot 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 占位符标识，对应 HTML 中的 `{{id}}`，命名规则：`image_N` 或 `text_N` |
| type | string | 是 | 类型：`image` 或 `text` |
| label | string | 是 | 中文标签，用于管理端展示和 AI 提示 |
| required | boolean | 是 | 是否必填 |
| maxLength | number | 否 | 文字最大长度（MVP 阶段不校验，仅作参考） |

### 2. Data JSON（ya_album_page.data）

扁平的 key-value 结构，key 是 slot 的 id，value 是具体内容。

```json
{
  "image_1": "https://oss.example.com/photos/abc123.jpg",
  "text_1": "我们的毕业纪念册"
}
```

- 图片类型的 value：OSS 图片 URL
- 文字类型的 value：字符串

### 3. HTML 模板（ya_template_page.content）

使用 `{{slot_id}}` 作为占位符，后端渲染时替换为 Data JSON 中的实际值。

```html
<div class="page">
  <img src="{{image_1}}" />
  <p>{{text_1}}</p>
</div>
```

## 完整示例

### 示例1：封面页（1图2文）

**Schema（imageCount=1, textCount=2）：**
```json
{
  "slots": [
    { "id": "image_1", "type": "image", "label": "封面照片", "required": true },
    { "id": "text_1", "type": "text", "label": "标题", "required": true, "maxLength": 20 },
    { "id": "text_2", "type": "text", "label": "副标题", "required": false, "maxLength": 50 }
  ]
}
```

**HTML 模板：**
```html
<div class="cover-page">
  <img src="{{image_1}}" class="cover-photo" />
  <h1>{{text_1}}</h1>
  <p class="subtitle">{{text_2}}</p>
</div>
```

**Data JSON：**
```json
{
  "image_1": "https://oss.example.com/cover.jpg",
  "text_1": "2024届毕业纪念册",
  "text_2": "计算机科学与技术学院"
}
```

### 示例2：拼图页（4图1文）

**Schema（imageCount=4, textCount=1）：**
```json
{
  "slots": [
    { "id": "image_1", "type": "image", "label": "照片1", "required": true },
    { "id": "image_2", "type": "image", "label": "照片2", "required": true },
    { "id": "image_3", "type": "image", "label": "照片3", "required": true },
    { "id": "image_4", "type": "image", "label": "照片4", "required": true },
    { "id": "text_1", "type": "text", "label": "说明文字", "required": false, "maxLength": 100 }
  ]
}
```

**模板页A（网格风格）：**
```html
<div class="grid-layout">
  <div class="grid-2x2">
    <img src="{{image_1}}" /><img src="{{image_2}}" />
    <img src="{{image_3}}" /><img src="{{image_4}}" />
  </div>
  <p class="caption">{{text_1}}</p>
</div>
```

**模板页B（瀑布流风格，同一个 Schema）：**
```html
<div class="waterfall-layout">
  <div class="col-left">
    <img src="{{image_1}}" /><img src="{{image_3}}" />
  </div>
  <div class="col-right">
    <img src="{{image_2}}" /><img src="{{image_4}}" />
  </div>
  <p class="footer-text">{{text_1}}</p>
</div>
```

**同一份 Data JSON 两个模板都能用：**
```json
{
  "image_1": "https://oss.example.com/p1.jpg",
  "image_2": "https://oss.example.com/p2.jpg",
  "image_3": "https://oss.example.com/p3.jpg",
  "image_4": "https://oss.example.com/p4.jpg",
  "text_1": "难忘的校园时光"
}
```

### 示例3：个人页（1图1文）

**Schema（imageCount=1, textCount=1）：**
```json
{
  "slots": [
    { "id": "image_1", "type": "image", "label": "个人照片", "required": true },
    { "id": "text_1", "type": "text", "label": "寄语", "required": true, "maxLength": 200 }
  ]
}
```

**Data JSON：**
```json
{
  "image_1": "https://oss.example.com/person.jpg",
  "text_1": "愿前程似锦，未来可期"
}
```

## 渲染流程

```
1. 素材（图片/文字）
       ↓ 分组
2. 素材组
       ↓ 匹配（通过 imageCount/textCount 快速筛选可用 Schema）
3. 选定 Schema + 模板页
       ↓ 生成
4. Data JSON（每页一个，key 为 slot id，value 为实际内容）
       ↓ 校验
5. Schema 校验 Data JSON（required 字段是否齐全）
       ↓ 渲染
6. 后端用 Data JSON 替换 HTML 模板中的 {{slot_id}} 占位符
       ↓ 返回
7. 前端拿到完整 HTML 直接渲染展示
```

### 后端渲染方式

Java 后端使用简单的字符串替换（或 Mustache.java 模板引擎）：

```java
public String renderPage(String htmlTemplate, Map<String, String> data) {
    String result = htmlTemplate;
    for (Map.Entry<String, String> entry : data.entrySet()) {
        result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
}
```

### 换模板流程

```
用户选择"换模板"
    ↓
查询同 Schema 下的其他模板页
    ↓
用户选择新模板页
    ↓
后端用同一份 Data JSON + 新模板页的 HTML 重新渲染
    ↓
返回新的 HTML 给前端
```

Data JSON 完全不变，只是换了 HTML 模板。

## 校验规则

### MVP 阶段

1. 遍历 Schema 的 slots
2. 检查 Data JSON 中是否有对应 key
3. `required: true` 的 slot 必须有值且不为空
4. 不校验 maxLength、图片格式等细节

### 后续可扩展

- maxLength 文字长度校验
- 图片宽高比约束
- 文字样式约束（字体、颜色等）
- AI 内容审核

## 设计决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| imageCount/textCount 存储位置 | 数据库独立字段 | 方便 SQL 查询匹配，不需要解析 JSON |
| slot id 命名方式 | `image_N`/`text_N` 编号式 | 保证同 Schema 下模板可替换 |
| Data JSON 结构 | 扁平 key-value | 简单直接，AI 生成友好 |
| 渲染位置 | 后端渲染 | 统一控制，前端只负责展示 |
| MVP 校验粒度 | 仅校验 required | AI 生成内容格式不确定，严格校验反而成为障碍 |
| 模板引擎 | 简单字符串替换 `{{}}` | MVP 够用，后续可升级为 Mustache/Handlebars |
