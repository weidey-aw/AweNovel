# 05 · 内容接口（文章 / 资源 / 评论 / 评分）

> 共 12 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> 表结构见 [`../../sql/awe_novel.sql`](../../sql/awe_novel.sql)（`gal_article` / `gal_resource` / `gal_comment` / `gal_rating`）。

| # | 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|---|
| 1 | GET | `/community/article/list` | 匿名 | ✅ |
| 2 | GET | `/community/article/{articleId}` | 匿名 | ✅ |
| 3 | POST | `/community/article` | 登录 | 🔸 已封装未接页面 |
| 4 | GET | `/community/resource/list` | 匿名 | ✅ |
| 5 | POST | `/community/resource` | 登录 | 🔸 已封装未接页面 |
| 6 | POST | `/community/resource/download/{resourceId}` | 登录 | ✅ |
| 7 | POST | `/community/resource/report/{resourceId}` | 登录 | ✅ |
| 8 | GET | `/community/comment/list` | 匿名 | ✅ |
| 9 | POST | `/community/comment` | 登录 | ✅ |
| 10 | DELETE | `/community/comment/{ids}` | 登录 | ✅ |
| 11 | GET | `/community/rating/{gameId}` | **登录** | ✅ |
| 12 | POST | `/community/rating` | 登录 | ✅ |

**审核状态约定（文章与资源一致）**：`0` = 待审核、`1` = 审核通过（对外可见）、`2` = 已拒绝

---

## 1. 文章 `/community/article`

### 1.1 数据模型 `Article`（表 `gal_article`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `articleId` | long | 文章ID（新增不传） |
| `userId` | long | 作者ID（发布时后端取当前登录用户，前端**不要传**） |
| `title` | string | 标题（必填） |
| `summary` | string | 简介 |
| `content` | string | 正文（前端按 **HTML** 渲染） |
| `cover` | string | 封面路径 |
| `category` | string | 分类：`news`资讯 / `review`评测 / `guide`攻略 |
| `status` | **string** | `0`待审核 / `1`通过 / `2`拒绝（发布时后端强制 `0`） |
| `processInstanceId` | string | Flowable 流程实例ID（后端写入，前端只读） |
| `viewCount` | int | 浏览量（详情接口 +1） |
| `likeCount` | int | 点赞数（**后端无点赞接口，恒为 0**） |
| `commentCount` | int | 评论数（评论接口自动 +1） |
| `delFlag` / `createTime` / `updateTime` | — | 基类字段 |

> ⚠️ 后端**不返回**作者昵称/头像（`userName`、`nickname`）。列表/详情要展示作者，需后端补联表字段。

### 1.2 文章列表

`GET /community/article/list` · **匿名** · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `category` | string | 否 | `news` / `review` / `guide`，精确匹配 |
| `keyword` | string | 否 | 标题模糊匹配 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<Article>`

```json
{
  "code": 200, "msg": "查询成功", "total": 12,
  "rows": [
    {
      "articleId": 12, "userId": 1, "title": "《CLANNAD》评测：家族与泪水",
      "summary": "……", "content": "<p>正文 HTML</p>", "cover": "/profile/upload/.../cover.jpg",
      "category": "review", "status": "1", "processInstanceId": "25001",
      "viewCount": 156, "likeCount": 0, "commentCount": 8,
      "createTime": "2024-01-01 00:00:00"
    }
  ]
}
```

**规则**

- 固定只返回 `status = '1'`（审核通过）的文章。
- 排序：`article_id DESC`（最新在前）。
- ⚠️ 管理端要展示「待审核/已拒绝」文章时，本接口**无法按状态筛选**（后端把 `status` 写死为 `"1"`），需后端新增管理端列表接口（支持 `status` 参数）。

### 1.3 文章详情

`GET /community/article/{articleId}` · **匿名** · 前端 ✅

**响应**：`{ "code":200, "msg":"操作成功", "data": Article }`

**规则**

- 每次调用**浏览量 +1**。
- ⚠️ 该接口**不校验 `status`**：待审核/已拒绝的文章，只要知道 ID 也能读到（前端应自行控制入口）。
- 不存在时返回 `{"code":500,"msg":"文章不存在"}`。

### 1.4 发布文章

`POST /community/article` · 登录 · 前端 🔸（`publishArticle` 已封装，无页面）

**请求体**

| 字段 | 必填 | 说明 |
|---|---|---|
| `title` | 是 | 标题 |
| `content` | 是 | 正文（HTML） |
| `summary` | 否 | 简介 |
| `cover` | 否 | 封面路径（先调 `/common/upload`） |
| `category` | 否 | 默认 `news` |

```json
{ "title": "《Summer Pockets》攻略", "summary": "全线攻略", "content": "<p>...</p>", "cover": "/profile/upload/.../x.jpg", "category": "guide" }
```

**响应**

```json
{
  "code": 200, "msg": "操作成功",
  "data": {
    "articleId": 13, "userId": 2, "title": "《Summer Pockets》攻略",
    "status": "0", "processInstanceId": "25002",
    "viewCount": 0, "likeCount": 0, "commentCount": 0, "createTime": "2024-01-01 12:00:00"
  }
}
```

**规则**

- 后端强制：`status = "0"`（待审核）、计数清零、`userId` 取当前登录用户。
- 同时启动 **Flowable 审核流程**（`processKey = contentReview`，`businessKey = article:<articleId>`），流程实例ID写入 `processInstanceId`。
- 审核通过后 `status` 才会变成 `"1"`，此时才会出现在公开列表。（审核接口见 [07-review-ai-common.md](07-review-ai-common.md)）

---

## 2. 资源 `/community/resource`

### 2.1 数据模型 `Resource`（表 `gal_resource`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `resourceId` | long | 资源ID |
| `gameId` | long | 所属游戏ID（必填） |
| `userId` | long | 发布者ID（后端取登录用户） |
| `title` | string | 资源标题（必填） |
| `type` | string | `netdisk`网盘 / `magnet`磁力 / `torrent`种子 |
| `url` | string | 下载链接 |
| `version` | string | 版本 |
| `size` | string | 大小（如 `3.2GB`） |
| `extractPwd` | string | 解压密码 |
| `checksum` | string | 校验码（MD5 等） |
| `points` | int | **下载所需积分**（0 表示免费） |
| `downloadCount` | int | 下载次数 |
| `reportCount` | int | 失效举报数 |
| `status` | **string** | `0`待审核 / `1`通过 / `2`拒绝 |
| `processInstanceId` | string | Flowable 流程实例ID |
| `delFlag` / `createTime` / `updateTime` | — | 基类字段 |

### 2.2 资源列表

`GET /community/resource/list` · **匿名** · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `gameId` | long | 否 | 按游戏筛选 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<Resource>`

```json
{
  "code": 200, "msg": "查询成功", "total": 5,
  "rows": [
    {
      "resourceId": 8, "gameId": 1, "userId": 2, "title": "CLANNAD 汉化版 全CG存档",
      "type": "netdisk", "url": "https://pan.baidu.com/s/xxxx",
      "version": "v1.2", "size": "3.2GB", "extractPwd": "abcd",
      "checksum": "d41d8cd98f00b204e9800998ecf8427e", "points": 10,
      "downloadCount": 23, "reportCount": 0, "status": "1",
      "createTime": "2024-01-01 00:00:00"
    }
  ]
}
```

**规则**

- 固定只返回 `status = '1'`；排序 `resource_id DESC`。
- ⚠️ **公开列表就会返回 `url` 与 `extractPwd`**（即未扣积分也能看到真实下载链接）。若要做「扣积分后才可见」，需后端在列表接口中屏蔽这两个字段（建议后续优化）。
- ⚠️ 不支持按发布者/状态查询，管理端审核页目前拉全量（`pageSize=200`）再前端过滤。
- 不返回 `userName` / `gameTitle`（前端 `types.ts` 中的对应字段拿到的是 `undefined`）。

### 2.3 发布资源

`POST /community/resource` · 登录 · 前端 🔸（`publishResource` 已封装，无页面）

**请求体**

| 字段 | 必填 | 说明 |
|---|---|---|
| `gameId` | 是 | 所属游戏 |
| `title` | 是 | 资源标题 |
| `type` | 否 | 默认 `netdisk` |
| `url` | 是 | 下载链接 |
| `points` | 否 | 下载所需积分，默认 0 |
| `version` / `size` / `extractPwd` / `checksum` | 否 | 附件信息 |

```json
{
  "gameId": 1, "title": "CLANNAD 汉化版", "type": "netdisk",
  "url": "https://pan.baidu.com/s/xxxx", "version": "v1.2", "size": "3.2GB",
  "extractPwd": "abcd", "points": 10
}
```

**响应**

```json
{
  "code": 200, "msg": "操作成功",
  "data": { "resourceId": 9, "gameId": 1, "userId": 2, "status": "0", "processInstanceId": "25003", "downloadCount": 0, "reportCount": 0 }
}
```

**规则**：后端强制 `status="0"`、`downloadCount=0`、`reportCount=0`，并启动 Flowable 审核（`businessKey = resource:<resourceId>`）。

### 2.4 下载资源（扣积分）

`POST /community/resource/download/{resourceId}` · 登录 · 前端 ✅

| 参数 | 说明 |
|---|---|
| `resourceId` | 资源ID（路径参数） |

**响应**

```json
{
  "code": 200, "msg": "操作成功",
  "data": {
    "resourceId": 8, "gameId": 1, "title": "CLANNAD 汉化版",
    "type": "netdisk", "url": "https://pan.baidu.com/s/xxxx",
    "extractPwd": "abcd", "points": 10, "downloadCount": 24, "status": "1"
  }
}
```

**失败示例**

```json
{ "code": 500, "msg": "资源不存在" }
{ "code": 500, "msg": "资源暂不可下载" }
{ "code": 500, "msg": "积分不足，无法下载" }
```

**规则**

1. 资源必须存在且 `status = '1'`（审核通过），否则报「资源暂不可下载」。
2. `points > 0` 时从 `gal_user_profile.points` 扣减并写积分流水（`change_type = 'download'`）；余额不足报「积分不足」。
3. 扣分成功后 `download_count + 1`，返回完整资源对象（含 `url`、`extractPwd`）。
4. 整个操作为事务（扣积分与计数一致）。

### 2.5 失效举报

`POST /community/resource/report/{resourceId}` · 登录 · 前端 ✅

**响应**：`{"code":200,"msg":"操作成功"}`

**规则**：仅将 `report_count + 1`，**不校验重复举报**、不自动下架。

---

## 3. 评论 `/community/comment`

### 3.1 数据模型 `Comment`（表 `gal_comment`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `commentId` | long | 评论ID |
| `targetType` | **string** | 目标类型：**`G`**游戏 / **`A`**文章 / **`R`**资源（⚠️ 不是 `game`/`article`） |
| `targetId` | long | 目标ID |
| `userId` | long | 评论人（后端取登录用户） |
| `pid` | long | 父评论ID，`0` = 顶层（不传后端补 0，实现楼中楼） |
| `content` | string | 评论内容（最长 1000） |
| `likeCount` | int | 点赞数（**后端无点赞接口，恒为 0**） |
| `status` | string | `1`正常 / `0`隐藏（新增时后端置 `1`） |
| `delFlag` / `createTime` / `updateTime` | — | 基类字段 |
| `nickName` / `avatar` | string | **非表字段**，后端**当前未填充**（返回为空） |

### 3.2 评论列表

`GET /community/comment/list` · **匿名** · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `targetType` | string | **是** | `G` / `A` / `R`（不传会报 400） |
| `targetId` | long | **是** | 目标ID（不传会报 400） |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<Comment>`

```json
{
  "code": 200, "msg": "查询成功", "total": 3,
  "rows": [
    { "commentId": 1, "targetType": "A", "targetId": 12, "userId": 2, "pid": 0,
      "content": "写得太好了！", "likeCount": 0, "status": "1", "createTime": "2024-01-01 12:00:00" },
    { "commentId": 2, "targetType": "A", "targetId": 12, "userId": 1, "pid": 1,
      "content": "同感～", "likeCount": 0, "status": "1", "createTime": "2024-01-01 12:05:00" }
  ]
}
```

**规则**

- 仅返回 `status = '1'` 且未逻辑删除的评论；排序 `comment_id ASC`（**楼层正序**，楼中楼需前端按 `pid` 组装）。
- 返回的是**平铺列表**，没有 `children`；顶层评论 `pid = 0`。

### 3.3 发表评论

`POST /community/comment` · 登录 · 前端 ✅

```json
{ "targetType": "A", "targetId": 12, "content": "写得太好了！", "pid": 0 }
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `targetType` | 是 | `G` / `A` / `R` |
| `targetId` | 是 | 目标ID |
| `content` | 是 | 内容 |
| `pid` | 否 | 回复某条评论时传其 `commentId` |

**响应**：`{"code":200,"msg":"操作成功","data":Comment}`

**规则**

- 后端补 `pid=0`、`likeCount=0`、`status='1'`，`userId` 取登录用户。
- 当 `targetType = 'A'` 时，对应文章 `comment_count + 1`（**仅文章**，游戏/资源不计数）。
- 评论**不消耗也不奖励积分**；不会给被回复者发消息通知。

### 3.4 删除评论（管理端）

`DELETE /community/comment/{ids}` · 登录 · 前端 ✅

| 参数 | 说明 |
|---|---|
| `ids` | 评论ID，多个逗号分隔 |

**响应**：`{"code":200,"msg":"操作成功"}`（逻辑删除）

> ⚠️ 该接口**没有 `@PreAuthorize`**，任何登录用户都能删除任意评论。

---

## 4. 评分 `/community/rating`

### 4.1 数据模型 `Rating`（表 `gal_rating`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `ratingId` | long | 评分ID |
| `userId` | long | 用户ID（后端取登录用户） |
| `gameId` | long | 游戏ID |
| `score` | int | `1` ~ `10` |
| `createTime` | string | 时间 |

> 表上有唯一索引 `uk_gal_rating_user_game (user_id, game_id)`，一人一游戏仅一条评分。

### 4.2 我的评分

`GET /community/rating/{gameId}` · **登录**（⚠️ 匿名会返回 401） · 前端 ✅

**响应（已评分）**

```json
{ "code": 200, "msg": "操作成功", "data": { "ratingId": 5, "userId": 2, "gameId": 1, "score": 9, "createTime": "2024-01-01 12:00:00" } }
```

**响应（未评分）**

```json
{ "code": 200, "msg": "操作成功", "data": null }
```

> 前端需兼容 `data` 为 `null` 与「无 `data` 字段」两种情况。游戏详情页应在未登录时跳过该请求。

### 4.3 评分 / 修改评分

`POST /community/rating` · 登录 · 前端 ✅

```json
{ "gameId": 1, "score": 9 }
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `gameId` | 是 | 游戏ID |
| `score` | 是 | `1`~`10`，越界报 `{"code":500,"msg":"评分需在 1-10 之间"}` |

**响应**：`{"code":200,"msg":"操作成功","data":Rating}`

**规则**

- **幂等 Upsert**：同一用户对同一游戏重复评分会**覆盖**原分数（不新增记录）。
- 每次评分后**重算该游戏的平均分与评分人数**，写回 `gal_game.rating_avg`（1 位小数，四舍五入）与 `rating_count`。
- 评分**不奖励积分**。

---

## 5. 前端对接提示（本模块）

1. **`targetType` 必须用 `G`/`A`/`R`**：前端 `CommentQuery.targetType` 注释写的是 `'game' | 'article'`，若按此传值，后端查不到任何评论。
2. `Article.status` / `Resource.status` 是**字符串**，不是数字。
3. 评论列表返回**平铺结构**，需前端按 `pid` 自行组装楼中楼（当前 `CommentSection.vue` 已处理）。
4. 评分接口**需要登录**，游戏详情页在未登录时应跳过 `/community/rating/{gameId}`，否则会触发 401 跳转。
5. 「发布文章」「发布资源」两个接口已封装但**没有页面入口**，是 P0 待补齐项；两者都依赖 `/common/upload` 上传封面。
6. 点赞功能（文章/评论/资源）**后端没有接口**，`likeCount` 恒为 0；如需该功能需后端先补齐（见 [README §5](README.md#5-后端接口缺口前端无法实现需后端补齐)）。
