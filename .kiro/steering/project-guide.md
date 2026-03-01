---
inclusion: auto
---

# YearArk — 基于 AI 的毕业纪念册自动生成系统

## 项目概述

YearArk 是一个毕业设计项目，核心目标是通过 AI 技术实现毕业纪念册的自动化生成。
用户创建纪念册 → 邀请同学上传素材 → AI 审核/分类/生成 → 用户在线编辑 → 导出成品。

## 项目结构

```
YearArk/
├── ruoyi-admin/          # 管理端前端（已有，基于 Vben Admin + Ant Design Vue）
├── ruoyi-ai/             # Java 后端（已有，基于 RuoYi-AI 框架 / SpringBoot 3.4）
├── yearark-web/          # 用户端前端（待建，Vue3 + TypeScript）
├── yearark-ai/           # Python AI 服务（待建，FastAPI）
├── YearArk.sql           # 数据库 SQL 文件
```

## 三端职责划分

### 管理端前端 (`ruoyi-admin/apps/web-antd`)
- 技术栈：Vue3 + TypeScript + Ant Design Vue（Vben Admin 框架）
- 职责：
  - 模板套件管理（ya_template）
  - 模板页面管理（ya_template_page + ya_template_schema）
  - 用户管理（ya_user）
  - 素材审核管理
  - 系统配置与监控

### 用户端前端 (`yearark-web`，待建)
- 技术栈：Vue3 + TypeScript（UI 框架待定）
- 职责：面向终端用户的所有纪念册功能
  - 用户注册/登录
  - 创建纪念册、填写基础信息、选择模板
  - 生成邀请链接，同学通过链接上传图片/文字
  - 查看 AI 处理进度
  - 查看/调整 AI 生成的大纲
  - 纪念册在线预览与编辑（修改文案、替换图片、调整样式）
  - 导出 PDF / 在线浏览

### Java 后端 (`ruoyi-ai/`)
- 技术栈：SpringBoot 3.4 + MyBatis-Plus + Sa-Token + Redis + MySQL 8
- 基于 RuoYi-AI 开源框架，仅使用其基础设施能力（权限、OSS、日志等）
- 框架自带的 AI 模块（chat、knowledge、graph、workflow、aihuman 等）不使用，后续清理

### Python AI 服务 (`yearark-ai/`，待建)
- 技术栈：FastAPI + OpenCV / CLIP / Stable Diffusion
- 通过 RabbitMQ 与 Java 后端异步通信
- 职责：
  - 图片质量筛选（模糊检测）与去重（pHash）
  - 内容审核
  - 人脸识别与计数
  - 图片分类与标签（个人/团队/风景/活动）
  - AI 文案生成（章节标题、段落描述）
  - AI 插画生成
  - 纪念册大纲结构生成

## 业务数据库设计（ya_ 前缀表）

### 核心表

| 表名 | 说明 |
|---|---|
| `ya_user` | 用户端用户（独立于管理端 sys_user），含昵称/密码/邮箱/头像 |
| `ya_album` | 纪念册主表，关联 template_id（模板套件）和 user_id（创建者） |
| `ya_album_media` | 纪念册素材，type 区分文本(1)和图片(2)，content 统一存内容，含 AI 标签/人脸数/审核状态 |
| `ya_album_page` | 纪念册页面，关联 template_page_id，data 字段存 AI 生成的填充 JSON |
| `ya_invite` | 邀请链接，含 invite_code（邀请码）+ access_code（访问码）+ 过期时间 |
| `ya_invite_token` | 匿名上传者的虚拟身份，通过邀请链接进入后生成 JWT token |
| `ya_template` | 模板套件，相同版式不同配色视为不同模板。des 字段存给 AI 看的描述 |
| `ya_template_page` | 套件内的页面模板，content 存 HTML（颜色写死），type 标识页面类型，关联 schema |
| `ya_template_schema` | 页面模板的 JSON Schema，定义该页需要填充的数据结构（供 AI 生成 JSON 用） |

### 关键关系

```
ya_user 1──N ya_album（一个用户创建多个纪念册）
ya_album N──1 ya_template（纪念册选择一个模板套件）
ya_album 1──N ya_album_media（一个纪念册有多个素材，type 区分文本/图片）
ya_album 1──N ya_album_page（一个纪念册有多个页面）
ya_album 1──N ya_invite（一个纪念册可生成多个邀请）
ya_invite 1──N ya_invite_token（一个邀请可产生多个匿名 token）
ya_album_page N──1 ya_template_page（页面使用套件内的某个页面模板）
ya_template 1──N ya_template_page（一个套件包含多个页面模板）
ya_template_page N──1 ya_template_schema（页面模板对应一个 schema）
```

### 模板系统设计原则

- 相同版式不同配色 = 不同模板套件，HTML 中颜色直接写死
- AI 不负责视觉样式，只根据 schema 生成填充数据 JSON（文案、图片选择等）
- 渲染流程：前端取 template_page.content（HTML）+ album_page.data（JSON）→ 合并渲染
- ya_template.des 帮助 AI 理解模板适用场景，辅助 AI 选择合适的模板

## 核心业务流程

```
1. 用户创建纪念册 → 选择模板套件 → 填写基本信息
2. 生成邀请链接（invite_code + access_code）
3. 同学通过链接上传图片/文字 → 生成匿名 token（ya_invite_token）
4. 素材进入审核队列（MQ → Python AI 服务）
   - 模糊检测 / 违规过滤 / 去重
   - 人脸计数 / 场景分类 / 标签生成 → 写回 ya_album_media
5. AI 根据素材统计 + 模板 schema 生成纪念册大纲（章节结构）
6. AI 为每个章节生成填充 JSON（文案 + 图片选择）→ 写入 ya_album_page.data
7. 前端渲染：template_page.content + album_page.data → 纪念册页面
8. 用户在线编辑 → 确认 → 导出 PDF
```

## 中间件

| 组件 | 用途 |
|---|---|
| MySQL 8 | 结构化数据存储 |
| Redis | 登录状态缓存、任务进度、邀请 token 过期管理 |
| RabbitMQ | Java 后端与 Python AI 服务的异步任务通信 |
| 阿里云 OSS | 图片、插画、PDF 等文件存储 |

## 框架冗余模块（待清理）

RuoYi-AI 框架自带但本项目不需要的模块：
- `chat_*`（AI 聊天）、`knowledge_*`（知识库）、`graph_*`（知识图谱）
- `t_workflow*`（工作流）、`aihuman_*`（数字人）
- `mcp_info`（MCP 服务）、`prompt_template`（提示词模板）、`dev_schema*`（代码生成）

## 开发约定

- 后端 Java 版本：17
- 包管理：前端 pnpm，后端 Maven
- 数据库字符集：utf8mb4
- 逻辑删除字段：`is_delete`（0 存在，1 删除）
- 时间字段：`create_at` / `update_at`
- 部署方案：Docker + K8s
