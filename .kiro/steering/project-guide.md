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
│   ├── ruoyi-modules/
│   │   └── ruoyi-system/  # 系统模块（包含 ya_ 业务）
│   │       ├── controller/  # 控制器层
│   │       ├── service/     # 服务层
│   │       └── mapper/      # 数据访问层
│   └── ruoyi-modules-api/
│       └── ruoyi-system-api/
│           └── domain/      # 实体类（ya_ domain 类在此）
├── yearark-web/          # 用户端前端（待建，Vue3 + TypeScript）
├── yearark-ai/           # Python AI 服务（待建，FastAPI）
├── sql/
│   ├── YearArk.sql       # 数据库 SQL 文件（旧版）
│   └── YearArkV2.0.sql   # 数据库 SQL 文件（当前版本，2026-03-02）
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

| 表名 | 说明 | 主键 | 关键字段 |
|---|---|---|---|
| `ya_user` | 用户端用户（独立于管理端 sys_user） | id (INT) | username, password_hash, email, avatar_url, status, create_at, update_at, is_delete |
| `ya_album` | 纪念册主表 | id (INT) | name, des, user_id, template_id, status(0草稿/1发布), is_public, pdf_url, create_at, update_at, is_delete |
| `ya_album_media` | 纪念册素材 | id (INT) | album_id, token_id, type(1文本/2图片), content, sort, size(MB), faces_count, tags, status(-1不通过/0待审核/1审核中/2通过), create_at, update_at, is_delete |
| `ya_album_page` | 纪念册页面 | id (INT) | album_id, template_page_id, des, sort, data(JSON), create_at, update_at, is_delete |
| `ya_invite` | 邀请链接 | id (INT) | album_id, invite_code(6位随机串), access_code, status(0禁用/1可用), create_at, expire_at, is_delete |
| `ya_invite_token` | 匿名上传者虚拟身份 | id (INT) | album_id, invite_id, token(JWT), ip_address, create_at, update_at, expired_at, status(0已过期/1可用) |
| `ya_template` | 模板套件 | id (INT) | name, type(存字典), preview_url, des(给AI看), status, create_at, update_at, create_by, update_by, is_delete |
| `ya_template_page` | 套件内的页面模板 | template_page_id (INT) | template_id, template_schema_id, content(模板H5字符串), preview_url(单页预览), type, create_at, update_at, create_by, update_by, is_delete |
| `ya_template_schema` | 页面模板的 JSON Schema | id (INT) | content(JSON Schema), status(默认1), create_at, update_at, create_by, update_by, is_delete |

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
- 渲染流程：前端取 template_page.content（模板H5字符串）+ album_page.data（JSON）→ 合并渲染
- ya_template.des 帮助 AI 理解模板适用场景，辅助 AI 选择合适的模板
- ya_template.type 使用字典管理模板分类
- 模板页面支持单页预览（preview_url）

## 核心业务流程

```
1. 用户创建纪念册 → 选择模板套件 → 填写基本信息（status=0草稿）
2. 生成邀请链接（6位随机invite_code + access_code）
3. 同学通过链接上传图片/文字 → 生成匿名 token（ya_invite_token，JWT格式）
4. 素材进入审核队列（MQ → Python AI 服务）
   - 模糊检测 / 违规过滤 / 去重
   - 人脸计数 / 场景分类 / 标签生成 → 写回 ya_album_media
   - status: -1审核不通过 / 0待审核 / 1审核中 / 2审核通过
   - 记录文件大小（size，单位MB）和排序（sort）
5. AI 根据素材统计 + 模板 schema 生成纪念册大纲（章节结构）
6. AI 为每个章节生成填充 JSON（文案 + 图片选择）→ 写入 ya_album_page.data
7. 前端渲染：template_page.content（模板H5字符串）+ album_page.data → 纪念册页面
8. 用户在线编辑 → 确认 → 发布（status=1）→ 导出 PDF（存储pdf_url）
9. 可选：设置 is_public=1 公开展示在首页
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
- `t_workflow*`（工作流引擎，包含 t_workflow, t_workflow_component, t_workflow_edge, t_workflow_node, t_workflow_runtime, t_workflow_runtime_node）
- `aihuman_*`（数字人）
- `mcp_info`（MCP 服务）、`prompt_template`（提示词模板）、`dev_schema*`（代码生成）

## 系统基础表（sys_ 前缀）

框架提供的系统管理表，用于管理端功能：

| 表名 | 说明 | 用途 |
|---|---|---|
| `sys_user` | 系统用户表 | 管理端用户（与ya_user独立） |
| `sys_role` | 角色表 | 权限管理 |
| `sys_menu` | 菜单表 | 管理端菜单权限 |
| `sys_dept` | 部门表 | 组织架构 |
| `sys_config` | 参数配置表 | 系统配置 |
| `sys_dict_type` / `sys_dict_data` | 字典表 | 数据字典（如ya_template.type） |
| `sys_oss` / `sys_oss_config` | 对象存储表 | OSS文件管理（阿里云OSS配置） |
| `sys_file_info` | 文件记录表 | 文件上传记录 |
| `sys_oper_log` | 操作日志表 | 管理端操作审计 |
| `sys_logininfor` | 登录日志表 | 登录记录 |
| `sys_notice` / `sys_notice_state` | 通知公告表 | 系统通知 |
| `sys_tenant` / `sys_tenant_package` | 租户表 | 多租户支持（tenant_id='000000'为默认租户） |
| `chat_config` | 配置信息表 | AI配置 |

## 开发约定

- 后端 Java 版本：17
- 包管理：前端 pnpm，后端 Maven
- 数据库：MySQL 8.0.44
- 数据库字符集：utf8mb4（ya_表使用utf8mb4_0900_ai_ci排序规则）
- 逻辑删除字段：`is_delete`（0 存在，1 删除，TINYINT类型）
- 时间字段：`create_at` / `update_at`（DATETIME类型，NOT NULL）
- 创建/更新人字段：`create_by` / `update_by`（INT类型，用于模板相关表）
- 邀请码：6位随机字符串（invite_code）
- Token格式：JWT（存储在ya_invite_token.token字段）
- 文件大小单位：MB（ya_album_media.size字段）
- 部署方案：Docker + K8s
