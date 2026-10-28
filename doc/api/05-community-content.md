# 05 · 内容接口（文章 / 资源 / 评论 / 评分）

> 共 32 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> 表结构见 [`../../sql/awe_novel.sql`](../../sql/awe_novel.sql)（`gal_article` / `gal_resource` / `gal_resource_source` / `gal_official_resource` / `gal_comment` / `gal_rating`）。

| # | 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|---|
| 1 | GET | `/community/article/list` | 匿名 | ✅ |
| 2 | GET | `/community/article/{articleId}` | 匿名 | ✅ |
| 3 | POST | `/community/article` | 登录 | ✅ 发布页 `/article/new` |
| 4 | GET | `/community/article/admin/list` | `community:article:list` | ✅ 管理端筛选 |
| 5 | GET | `/community/article/admin/{articleId}` | `community:article:list` | ✅ |
| 6 | POST | `/community/article/admin` | `community:article:add` | ✅ |
| 7 | PUT | `/community/article/admin` | `community:article:edit` | ✅ |
| 8 | PUT | `/community/article/admin/status` | `community:article:edit` | ✅ 通过/拒绝 |
| 9 | DELETE | `/community/article/admin/{articleIds}` | `community:article:remove` | ✅ 级联删评论 |
| 10 | GET | `/community/resource/list` | 匿名 | ✅ |
| 11 | GET | `/community/resource/types` | 匿名 | ✅ 资源类型字典 |
| 12 | GET | `/community/resource/official/list` | 匿名 | ✅ 正版资源 |
| 13 | POST | `/community/resource` | 登录 | 🔸 已封装未接页面 |
| 14 | POST | `/community/resource/download/{resourceId}` | 登录 | ✅ 返回多来源 |
| 15 | POST | `/community/resource/report/{resourceId}` | 登录 | ✅ |
| 16 | GET | `/community/resource/admin/list` | `community:resource:list` | ✅ 管理端筛选 |
| 17 | GET | `/community/resource/admin/{resourceId}` | `community:resource:list` | ✅ 含来源详情 |
| 18 | POST | `/community/resource/admin` | `community:resource:add` | ✅ 新增（含来源） |
| 19 | PUT | `/community/resource/admin` | `community:resource:edit` | ✅ 修改（来源覆盖） |
| 20 | DELETE | `/community/resource/admin/{resourceIds}` | `community:resource:remove` | ✅ 删除（级联来源） |
| 21 | POST | `/community/resource/official` | `community:resource:edit` | ✅ 新增正版 |
| 22 | PUT | `/community/resource/official` | `community:resource:edit` | ✅ 修改正版 |
| 23 | DELETE | `/community/resource/official/{ids}` | `community:resource:edit` | ✅ 删除正版 |
| 24 | GET | `/community/comment/list` | 匿名 | ✅ |
| 25 | POST | `/community/comment` | 登录 | ✅ |
| 26 | DELETE | `/community/comment/own/{commentId}` | 登录 | ✅ 删除自己的评论 |
| 27 | GET | `/community/comment/admin/list` | `community:comment:list` | ✅ 管理端筛选 |
| 28 | GET | `/community/comment/admin/{commentId}` | `community:comment:list` | ✅ |
| 29 | PUT | `/community/comment/admin/status` | `community:comment:edit` | ✅ 显示/隐藏 |
| 30 | DELETE | `/community/comment/admin/{commentIds}` | `community:comment:remove` | ✅ 含子回复 |
| 31 | GET | `/community/rating/{gameId}` | **登录** | ✅ |
| 32 | POST | `/community/rating` | 登录 | ✅ |

> 资源相关权限需执行 [`sql/upgrade_resource_v2.sql`](../../sql/upgrade_resource_v2.sql)（建表 + 字典 + 按钮权限）；
> 文章/评论权限与评论表 `update_time` 修复见 [`sql/upgrade_article_comment.sql`](../../sql/upgrade_article_comment.sql)。

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

> 作者昵称与头像由后端联表 `sys_user` 返回：`userName`（优先昵称）、`userAvatar`。

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
- 列表行包含 `userName` / `userAvatar`，可直接展示作者。
- 管理端要查看「待审核/已拒绝」文章，请用下文 §1.5 管理端接口。

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

### 1.5 管理端文章接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/community/article/admin/list` | `community:article:list` | 分页，支持 `keyword` / `category` / `status` |
| GET | `/community/article/admin/{articleId}` | `community:article:list` | 详情（含作者） |
| POST | `/community/article/admin` | `community:article:add` | 新增，可指定 `status`（默认 `1`） |
| PUT | `/community/article/admin` | `community:article:edit` | 修改（字段同 `Article`） |
| PUT | `/community/article/admin/status` | `community:article:edit` | `{ articleId, status }`，快速通过/拒绝 |
| DELETE | `/community/article/admin/{articleIds}` | `community:article:remove` | 批量删除，**级联删除该文章评论** |

> 管理端新增的文章直接以指定状态落库，不启动 Flowable 流程；用户端 `POST /community/article` 才走审核。

---

## 2. 资源 `/community/resource`

> 资源拆分为两部分：**元信息**（`gal_resource`）+ **多条来源**（`gal_resource_source`）；
> 另外每个游戏可配置 **正版资源**（`gal_official_resource`，与用户无关）。

### 2.1 数据模型

**`Resource`（表 `gal_resource`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `resourceId` | long | 资源ID |
| `gameId` | long | 所属游戏ID（必填） |
| `userId` | long | 发布者ID（`sys_user.user_id`） |
| `title` | string | 资源标题（必填） |
| `type` | string | 资源类型，字典 **`game_resource_type`**：`netdisk`网盘 / `magnet`磁力 / `direct`直链 / `torrent`种子 / `other`其他 |
| `version` | string | 版本 |
| `size` | string | 大小（如 `3.2GB`） |
| `checksum` | string | 校验码（MD5 等） |
| `points` | int | **下载所需积分**，0 表示免费 |
| `downloadCount` | int | 下载次数 |
| `reportCount` | int | 失效举报数 |
| `status` | **string** | `0`待审核 / `1`通过 / `2`拒绝 |
| `processInstanceId` | string | Flowable 流程实例ID |
| `url` / `extractPwd` | string | **已废弃**，仅保留兼容，新数据写在来源表 |
| `sources` | array | **非表字段**，资源来源列表（详情/下载返回） |
| `sourceCount` | long | **非表字段**，来源数量（列表返回） |
| `userName` / `userAvatar` | string | **非表字段**，发布者昵称与头像 |
| `gameTitle` | string | **非表字段**，所属游戏名 |
| `delFlag` / `createTime` / `updateTime` | — | 基类字段 |

**`ResourceSource`（表 `gal_resource_source`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `sourceId` | long | 来源ID |
| `resourceId` | long | 所属资源 |
| `sourceType` | string | 来源类型（同 `game_resource_type` 字典） |
| `url` | string | 资源链接 |
| `extractPwd` | string | 提取码 |
| `unzipPwd` | string | 解压码 |
| `remark` | string | 备注（如「百度网盘」「磁力」） |
| `sort` | int | 排序 |
| `createTime` | string | 创建时间 |

**`OfficialResource`（表 `gal_official_resource`）**：`officialId` / `gameId` / `title` / `sourceType`（字典 **`game_official_type`**：`official`/`store`/`steam`/`dlsite`/`other`）/ `url` / `icon` / `sort` / `createTime`

> 关系：`gal_game 1—N gal_resource 1—N gal_resource_source`；`gal_resource N—1 sys_user`；
> `gal_game 1—N gal_official_resource`。

### 2.2 资源列表（前台）

`GET /community/resource/list` · **匿名**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `gameId` | long | 否 | 按游戏筛选 |
| `type` | string | 否 | 按资源类型筛选（`game_resource_type` 字典值） |
| `pageNum` / `pageSize` | int | 否 | 分页 |

- 仅返回 `status = '1'`，排序 `resource_id DESC`。
- 每行返回 `userName`、`userAvatar`、`gameTitle`、`sourceCount`；**不返回具体链接**（需登录下载才可见）。

```json
{
  "code": 200, "msg": "查询成功", "total": 1,
  "rows": [
    { "resourceId": 8, "gameId": 1, "title": "CLANNAD 汉化版", "type": "netdisk",
      "version": "v1.2", "size": "3.2GB", "points": 10, "downloadCount": 23,
      "status": "1", "userName": "小明", "userAvatar": "/profile/avatar/xx.webp",
      "gameTitle": "团子大家族", "sourceCount": 2, "createTime": "2024-01-01 00:00:00" }
  ]
}
```

### 2.3 资源类型字典

`GET /community/resource/types` · **匿名**

```json
{ "code": 200, "msg": "操作成功",
  "data": {
    "resource": [ { "dictLabel": "网盘", "dictValue": "netdisk" }, { "dictLabel": "磁力链接", "dictValue": "magnet" } ],
    "official": [ { "dictLabel": "官方网站", "dictValue": "official" } ]
  } }
```

### 2.4 正版资源列表（前台）

`GET /community/resource/official/list?gameId=1` · **匿名** · 返回 `data:[OfficialResource]`（按 `sort` 升序）

### 2.5 下载资源

`POST /community/resource/download/{resourceId}` · 登录

- 校验资源为 `status='1'`；`points > 0` 时扣积分（不足报错）。
- `download_count + 1`，返回**含 `sources` 的完整资源**（链接 / 提取码 / 解压码都在 `sources` 里）。

```json
{ "code": 200, "msg": "操作成功",
  "data": { "resourceId": 8, "title": "CLANNAD 汉化版", "points": 10, "downloadCount": 24,
    "sources": [
      { "sourceId": 1, "sourceType": "netdisk", "url": "https://pan.baidu.com/s/xxxx",
        "extractPwd": "abcd", "unzipPwd": "123456", "remark": "百度网盘", "sort": 0 }
    ] } }
```

### 2.6 失效举报

`POST /community/resource/report/{resourceId}` · 登录 · `report_count + 1`

### 2.7 用户发布资源

`POST /community/resource` · 登录 · 状态置为 `0` 并启动 Flowable 审核（`bizType = resource`）

请求体同 `Resource`，可通过 `sources` 数组同时提交来源。

### 2.8 管理端列表

`GET /community/resource/admin/list` · 权限 `community:resource:list`

| 参数 | 说明 |
|---|---|
| `keyword` | 标题模糊 |
| `gameId` | 所属游戏 |
| `type` | 资源类型 |
| `status` | `0`/`1`/`2` |
| `pageNum` / `pageSize` | 分页 |

每行返回 `userName`、`gameTitle`、`sourceCount`，可按游戏/类型/状态筛选（满足「编辑资源跳转后只看该游戏」的场景）。

### 2.9 管理端详情 / 新增 / 修改 / 删除

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/community/resource/admin/{resourceId}` | `community:resource:list` | 详情（含 `sources`） |
| POST | `/community/resource/admin` | `community:resource:add` | 新增（可带 `sources`；`userId` 缺省为当前管理员） |
| PUT | `/community/resource/admin` | `community:resource:edit` | 修改；**传 `sources` 即全量覆盖来源**，不传则不动 |
| DELETE | `/community/resource/admin/{resourceIds}` | `community:resource:remove` | 批量删除，**级联删除来源** |

### 2.10 正版资源管理

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/community/resource/official` | `community:resource:edit` | 新增（`gameId`/`title`/`url` 必填） |
| PUT | `/community/resource/official` | `community:resource:edit` | 修改（需 `officialId`） |
| DELETE | `/community/resource/official/{ids}` | `community:resource:edit` | 批量删除 |

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

### 3.4 删除自己的评论

`DELETE /community/comment/own/{commentId}` · 登录 · 前端 ✅

- 仅能删除**本人**评论，否则返回 `{"code":500,"msg":"只能删除自己的评论"}`。
- 其下的回复会一并删除，并同步扣减文章 `comment_count`。

### 3.5 管理端评论接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/community/comment/admin/list` | `community:comment:list` | 分页，支持 `keyword` / `targetType` / `targetId` / `status` |
| GET | `/community/comment/admin/{commentId}` | `community:comment:list` | 详情（含用户昵称头像） |
| PUT | `/community/comment/admin/status` | `community:comment:edit` | `{ commentId, status }`，`1` 正常 / `0` 隐藏 |
| DELETE | `/community/comment/admin/{commentIds}` | `community:comment:remove` | 批量删除（含子回复），同步扣减文章评论数 |

> 管理端列表行返回 `nickName` / `avatar`；公开列表只返回 `status='1'` 的评论。

> ⚠️ 历史问题已修复：`gal_comment` 缺少 `update_time` 列导致发表评论报 `Unknown column 'update_time'`，
> 升级脚本 [`../../sql/upgrade_article_comment.sql`](../../sql/upgrade_article_comment.sql) 会自动补列。

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
5. 「发布文章」已有页面入口 `/article/new`（`ArticleEditorView.vue`，支持 Markdown + 实时预览 + 封面上传，提交后进入待审核）；「发布资源」仍为接口封装、无页面入口。
6. 点赞功能（文章/评论/资源）**后端没有接口**，`likeCount` 恒为 0；如需该功能需后端先补齐（见 [README §5](README.md#5-后端接口缺口前端无法实现需后端补齐)）。
7. 文章详情正文为 **Markdown**，前端用 `renderMarkdown()`（`@gal/shared`）渲染，不要直接 `v-html` 原文。
