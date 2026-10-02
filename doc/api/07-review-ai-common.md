# 07 · 内容审核 / AI 看板娘 / 通用接口

> 共 9 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。

| # | 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|---|
| 1 | GET | `/community/review/tasks` | `community:review:list` | ✅ |
| 2 | POST | `/community/review/approve/{processInstanceId}` | `community:review:edit` | ✅ |
| 3 | POST | `/community/review/reject/{processInstanceId}` | `community:review:edit` | ✅ |
| 4 | GET | `/ai/chat?message=` | 登录（SSE） | ✅ |
| 5 | POST | `/common/upload` | 登录 | ❌ |
| 6 | POST | `/common/uploads` | 登录 | ❌ |
| 7 | GET | `/common/download` | 登录 | ❌ |
| 8 | GET | `/common/download/resource` | 登录 | ❌ |
| 9 | GET | `/` | 匿名 | ❌（无需对接） |

---

## 1. 内容审核（Flowable 工作流）

### 1.1 流程说明

- 流程定义：`content-review.bpmn20.xml`，`processKey = contentReview`，名称「内容审核流程」。
- 流转：`提交内容` → `内容审核`（**用户任务，候选组 `admin`**）→ 排他网关（`${approved}`）→ `通过` / `拒绝`。
- 触发方：发布文章（`POST /community/article`）与发布资源（`POST /community/resource`）时各自启动一个流程实例。
  - 业务标识：`businessKey = "article:<articleId>"` / `"resource:<resourceId>"`
  - 流程变量：`bizType`（`article` / `resource`）、`bizId`（业务ID）
  - 流程实例ID写回业务表的 `process_instance_id` 字段
- 审核结果会同步更新业务表 `status`：通过 → `'1'`，拒绝 → `'2'`。

> ⚠️ 待办任务查询使用 `taskCandidateGroup("admin")`，因此**只有候选组为 `admin` 的任务**能查到；同时调用者还需具备 `community:review:list` 权限。

### 1.2 待审核任务列表

`GET /community/review/tasks` · 权限 `community:review:list` · 前端 ✅

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "taskId": "25004",
      "processInstanceId": "25003",
      "taskName": "内容审核",
      "bizType": "resource",
      "bizId": 9
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `taskId` | string | Flowable 任务ID |
| `processInstanceId` | string | 流程实例ID（**审核通过/拒绝时用它，不是 taskId**） |
| `taskName` | string | 任务名称（固定「内容审核」） |
| `bizType` | string | `article` 文章 / `resource` 资源 |
| `bizId` | long | 业务ID（文章ID / 资源ID） |

**备注**

- **不分页**，一次性返回全部待办。
- ⚠️ 审核列表本身**不返回业务详情**（标题、封面、提交人）。前端需要用 `bizType` + `bizId` 再去调 `GET /community/article/{id}` 或 `GET /community/resource/list` 拼详情 —— 当前管理端 `ReviewManageView.vue` 就是这么做的（资源靠拉全量列表匹配，效率较低，建议后端补联表字段或新增 `GET /community/review/task/{processInstanceId}/detail`）。

### 1.3 审核通过

`POST /community/review/approve/{processInstanceId}` · 权限 `community:review:edit` · 前端 ✅

| 参数 | 说明 |
|---|---|
| `processInstanceId` | 流程实例ID（来自待办列表） |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**失败示例**

```json
{ "code": 500, "msg": "审核任务不存在或已处理" }
```

**服务端动作**

1. 取流程变量 `bizType` / `bizId`；
2. 查询该流程实例的当前任务（`singleResult()`），不存在则报错；
3. 以 `approved = true` 完成任务，流程走「通过」分支结束；
4. 同步更新业务表状态：文章/资源 `status = '1'`。

### 1.4 审核拒绝

`POST /community/review/reject/{processInstanceId}` · 权限 `community:review:edit` · 前端 ✅

参数、响应同 1.3；`approved = false`，流程走「拒绝」分支，业务表 `status = '2'`。

**备注（当前实现的局限）**

- **不支持填写审核意见/驳回理由**（流程没有该字段，业务表也没有 `reject_reason`）。
- 审核结果**不会给作者发送站内消息**（`MessageService.send` 未被调用）。
- 审核历史/已审列表没有接口（Flowable 历史表已随数据库迁移保留，但未暴露 REST）。
- 若文章/资源被**重复提交**、或审核中被删除，可能出现任务与业务数据不一致的情况。

---

## 2. AI 看板娘（SSE 流式聊天）

`GET /ai/chat?message=xxx` · 登录 · 前端 ✅（`ChatMaid.vue`）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `message` | string | 是 | 用户消息内容（URL 编码） |

**请求头**：`Authorization: Bearer <token>`（`Content-Type` 无需设置）

**响应**：`text/event-stream`（SSE），逐个 `data:` 事件推送增量文本：

```
data:你
data:好
data:，主人
```

> 前端需用 `fetch + ReadableStream` 手动解析（`EventSource` 无法携带 Authorization 头），仓库中 `web-ui/shared/src/api.ts` 的 `streamChat()` 已实现该解析，可直接复用。

**错误与边界**

| 场景 | 服务端行为 |
|---|---|
| 积分不足 | 推送一条 `data:积分不足，无法与看板娘聊天，请先签到获取积分～` 后结束 |
| 未配置 API Key | 推送 `data:AI 服务未配置，请联系管理员设置 DEEPSEEK_API_KEY` 后结束 |
| DeepSeek 返回非 200 | 推送 `data:AI 服务返回错误：<状态码>` 后结束 |
| 正常结束 | 关闭连接（前端 `reader.read()` 返回 `done`） |

**规则**

- 每次调用**消耗 1 积分**（`change_type = 'ai_chat'`），且**扣费在流式返回之前**：即使后续 AI 调用失败，积分也已扣除。
- 服务端超时 **120 秒**（`SseEmitter(120000L)`），DeepSeek 请求超时同为 120 秒。
- **无多轮上下文**：每次请求只发送 `system`（人设）+ 当前 `user` 消息，历史对话不入库、不带上下文。
- 人设/模型/地址由配置决定：`ai.deepseek.persona` / `model`（默认 `deepseek-chat`）/ `base-url`。
- 并发线程池固定 8 个线程。

---

## 3. 通用文件接口

### 3.1 上传单个文件

`POST /common/upload` · 登录 · 前端 ❌

**请求**：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file | 是 | 文件（字段名固定为 **`file`**） |

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "url": "http://127.0.0.1:9090/profile/upload/2024/01/01/abc_20240101120000A001.jpg",
  "fileName": "/profile/upload/2024/01/01/abc_20240101120000A001.jpg",
  "newFileName": "abc_20240101120000A001.jpg",
  "originalFilename": "封面图.jpg"
}
```

| 字段 | 说明 |
|---|---|
| `url` | **完整地址**（含服务端 host），可直接用于 `img src` |
| `fileName` | 相对路径（**入库请存这个值**，例如 `Game.cover` / `Article.cover`） |
| `newFileName` | 重命名后的文件名 |
| `originalFilename` | 原始文件名 |

**限制**

| 项 | 值 |
|---|---|
| 单文件大小 | **10MB**（`spring.servlet.multipart.max-file-size`） |
| 单次请求总大小 | 20MB |
| 允许扩展名 | `bmp gif jpg jpeg png doc docx xls xlsx ppt pptx html htm txt rar zip gz bz2 mp4 avi rmvb pdf` |
| 上传目录 | `awenovel.profile` + `/upload/yyyy/MM/dd/` |

**失败示例**：`{"code":500,"msg":"上传文件大小超出限制"}`、`{"code":500,"msg":"文件类型不正确"}`

### 3.2 上传多个文件

`POST /common/uploads` · 登录 · 前端 ❌

**请求**：`multipart/form-data`，字段名 **`files`**（可重复）

**响应**（多值以英文逗号拼接）

```json
{
  "code": 200, "msg": "操作成功",
  "urls": "http://.../a.jpg,http://.../b.jpg",
  "fileNames": "/profile/upload/2024/01/01/a.jpg,/profile/upload/2024/01/01/b.jpg",
  "newFileNames": "a.jpg,b.jpg",
  "originalFilenames": "1.jpg,2.jpg"
}
```

### 3.3 下载文件（通用下载）

`GET /common/download?fileName=xxx&delete=false` · 登录 · 前端 ❌

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `fileName` | string | 是 | 文件名（**必须是形如 `xxx_原名.ext` 的格式**，服务端会截取第一个 `_` 之后的部分作为下载名） |
| `delete` | boolean | 否 | 下载后是否删除源文件 |

**响应**：二进制流（`Content-Disposition: attachment`）

> 该接口主要用于「导出后立即下载」（配合 Excel 导出：导出文件落在 `profile/download/`，再带文件名调此接口）。

### 3.4 下载本地资源

`GET /common/download/resource?resource=/profile/upload/2024/01/01/a.jpg` · 登录 · 前端 ❌

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `resource` | string | 是 | 以 `/profile` 开头的资源相对路径 |

**响应**：二进制流。

> 普通图片/文件展示**不需要**该接口 —— 静态资源已通过 `/profile/**` 直接暴露（见 [README §1.5](README.md#15-时间与静态资源)）。

---

## 4. 服务首页

`GET /` · 匿名

**响应**：`text/plain`

```
欢迎使用AweNovel后台管理框架，当前版本：v3.8.7，请通过前端地址访问。
```

> 仅用于探活/部署自检（部署文档中的健康检查用的就是它），前端无需对接。
