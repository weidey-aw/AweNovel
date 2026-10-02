# 04 · 游戏库接口（游戏 / 会社 / 标签）

> 共 14 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> 表结构见 [`../../sql/awe_novel.sql`](../../sql/awe_novel.sql)（`gal_game` / `gal_brand` / `gal_tag` / `gal_game_tag`）。

| # | 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|---|
| 1 | GET | `/community/game/list` | 匿名 | ✅ |
| 2 | GET | `/community/game/{gameId}` | 匿名 | ✅ |
| 3 | GET | `/community/game/tag/all` | 匿名 | ✅ |
| 4 | POST | `/community/game` | 登录 | ✅ |
| 5 | PUT | `/community/game` | 登录 | ✅ |
| 6 | DELETE | `/community/game/{gameIds}` | 登录 | ✅ |
| 7 | GET | `/community/brand/list` | 匿名 | ✅ |
| 8 | POST | `/community/brand` | 登录 | ✅ |
| 9 | PUT | `/community/brand` | 登录 | ✅ |
| 10 | DELETE | `/community/brand/{ids}` | 登录 | ✅ |
| 11 | GET | `/community/tag/list` | 匿名 | ✅ |
| 12 | POST | `/community/tag` | 登录 | ✅ |
| 13 | PUT | `/community/tag` | 登录 | ✅ |
| 14 | DELETE | `/community/tag/{ids}` | 登录 | ✅ |

> ⚠️ 第 4~6、8~10、12~14 为管理类写接口，但后端**只校验登录、没有 `@PreAuthorize`**，任何注册用户均可调用（详见 [README §6.2](README.md#62-权限风险后端问题前端需知晓)）。

---

## 1. 数据模型

### 1.1 游戏 `Game`（表 `gal_game`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `gameId` | long | 游戏ID（自增，新增时**不要传**） |
| `brandId` | long | 制作会社ID |
| `title` | string | 游戏原名 |
| `titleCn` | string | 游戏译名 |
| `cover` | string | 封面图相对路径（`/profile/upload/...`） |
| `releaseDate` | string | 发售日期（`yyyy-MM-dd`） |
| `summary` | string | 简介 |
| `staffPaint` | string | 原画 |
| `staffScenario` | string | 剧本 |
| `staffVoice` | string | 主要声优 |
| `ratingAvg` | number | 平均评分（冗余，由评分接口自动重算，1 位小数） |
| `ratingCount` | int | 评分人数（冗余） |
| `viewCount` | int | 浏览量（详情接口自动 +1） |
| `status` | **string** | `1`上架 / `0`下架（新增时后端强制置 `1`） |
| `delFlag` | string | `0`存在 / `2`删除（逻辑删除） |
| `createTime` / `updateTime` | string | 时间 |
| `brandName` | string | **非表字段**，连表返回的会社名 |
| `tags` | array | **非表字段**，标签列表（仅详情返回） |
| `tagIds` | long[] | **非表字段**，提交时用于设置标签关联 |

### 1.2 会社 `Brand`（表 `gal_brand`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `brandId` | long | 会社ID |
| `name` | string | 会社名称 |
| `logo` | string | Logo 路径 |
| `description` | string | 会社简介 |
| `delFlag` / `createTime` / `updateTime` | — | 同基类 |

> ⚠️ 后端**没有** `nameCn`（中文名）、`country`（国家）、`website`（官网）字段。前端 `types.ts` 中的这些字段需删除，否则提交无效。

### 1.3 标签 `Tag`（表 `gal_tag`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `tagId` | long | 标签ID |
| `name` | string | 标签名称 |
| `type` | **string** | `theme`题材 / `play`玩法 / `type`类型（**不是数字**） |
| `createTime` | string | 创建时间 |

---

## 2. 游戏接口

### 2.1 游戏列表

`GET /community/game/list` · **匿名** · 前端 ✅（用户端 GameListView / HomeView，管理端 GameManageView）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 关键词，同时模糊匹配 `title` 与 `titleCn` |
| `brandId` | long | 否 | 按会社筛选 |
| `tagId` | long | 否 | 按标签筛选（子查询 `gal_game_tag`） |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<Game>`

```json
{
  "code": 200, "msg": "查询成功", "total": 30,
  "rows": [
    {
      "gameId": 1, "brandId": 3, "title": "CLANNAD", "titleCn": "团子大家族",
      "cover": "/profile/upload/2024/01/01/clannad.jpg", "releaseDate": "2004-04-28",
      "summary": "……", "staffPaint": "樋上いたる", "staffScenario": "麻枝准", "staffVoice": "中原麻衣",
      "ratingAvg": 9.2, "ratingCount": 15, "viewCount": 320, "status": "1",
      "brandName": "Key", "createTime": "2024-01-01 00:00:00"
    }
  ]
}
```

**规则**

- 固定只返回 `status = '1'`（已上架）的游戏，**看不到下架的**。
- 排序：`rating_avg DESC, game_id DESC`。
- 每行会**额外填充 `brandName`**（会社名），但**不返回 `tags`**。
- 逻辑删除的记录自动过滤。

### 2.2 游戏详情

`GET /community/game/{gameId}` · **匿名** · 前端 ✅

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "gameId": 1, "title": "CLANNAD", "titleCn": "团子大家族",
    "brandId": 3, "brandName": "Key",
    "cover": "/profile/upload/2024/01/01/clannad.jpg",
    "releaseDate": "2004-04-28", "summary": "……",
    "staffPaint": "樋上いたる", "staffScenario": "麻枝准", "staffVoice": "中原麻衣",
    "ratingAvg": 9.2, "ratingCount": 15, "viewCount": 321, "status": "1",
    "tags": [ { "tagId": 2, "name": "恋爱", "type": "theme" }, { "tagId": 7, "name": "治愈", "type": "theme" } ]
  }
}
```

**规则**

- 返回 `brandName` 与 `tags`（完整标签对象数组）。
- 每次调用**浏览量 +1**（`view_count`），因此返回的 `viewCount` 是自增后的值。
- 游戏不存在时返回 `{"code":500,"msg":"游戏不存在"}`。

### 2.3 全部标签（游戏维度）

`GET /community/game/tag/all` · **匿名** · 前端 ✅

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": [ { "tagId": 1, "name": "校园", "type": "theme", "createTime": "2024-01-01 00:00:00" } ] }
```

> 与 `GET /community/tag/list` 数据完全相同（都返回全量标签并按 `tagId` 升序），前端**二选一**即可。

### 2.4 新增游戏

`POST /community/game` · 登录 · 前端 ✅（管理端）

**请求体**

```json
{
  "title": "Summer Pockets",
  "titleCn": "夏日口袋",
  "brandId": 3,
  "cover": "/profile/upload/2024/01/01/sp.jpg",
  "releaseDate": "2018-06-29",
  "summary": "简介……",
  "staffPaint": "Na-Ga",
  "staffScenario": "新岛夕",
  "staffVoice": "小原好美",
  "tagIds": [2, 7]
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `title` | 是 | 游戏原名 |
| `titleCn` | 否 | 译名 |
| `brandId` | 否 | 会社ID |
| `cover` | 否 | 封面路径（先调 `/common/upload` 上传） |
| `releaseDate` | 否 | `yyyy-MM-dd` |
| `summary` / `staffPaint` / `staffScenario` / `staffVoice` | 否 | 文本字段 |
| `tagIds` | 否 | 标签 ID 数组，后端写入 `gal_game_tag` |

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": { "gameId": 31, "title": "Summer Pockets", "status": "1", "tagIds": [2, 7] } }
```

**规则**：后端强制 `status = "1"`；`ratingAvg`/`ratingCount`/`viewCount` 由数据库默认值或后续接口维护。

### 2.5 修改游戏

`PUT /community/game` · 登录 · 前端 ✅

请求体同新增，**必须带 `gameId`**。`tagIds` 为**全量覆盖**（先删除该游戏全部标签关联，再按传入数组重建）。

### 2.6 删除游戏

`DELETE /community/game/{gameIds}` · 登录 · 前端 ✅

| 参数 | 说明 |
|---|---|
| `gameIds` | 游戏ID，多个用逗号分隔，如 `/community/game/1,2,3` |

**响应**：`{"code":200,"msg":"操作成功"}`（逻辑删除，`del_flag` 置 `2`）

---

## 3. 会社接口

### 3.1 会社列表

`GET /community/brand/list` · **匿名** · 前端 ✅

**响应**（`data` 为**数组**，不是分页结构 —— 后端未使用 `getDataTable`，忽略分页参数）

```json
{
  "code": 200, "msg": "操作成功",
  "data": [ { "brandId": 1, "name": "Key", "logo": "", "description": "日本视觉小说品牌", "createTime": "2024-01-01 00:00:00" } ]
}
```

> ⚠️ 前端 `getBrandList` 类型声明为 `TableDataInfo<Brand> | Brand[]`，实际返回的是 `AjaxResult{data: Brand[]}`，取数时需读 `res.data`。

### 3.2 新增会社

`POST /community/brand` · 登录 · 前端 ✅

```json
{ "name": "Key", "logo": "/profile/upload/.../key.png", "description": "日本视觉小说品牌" }
```

**响应**：`{"code":200,"msg":"操作成功","data":{ "brandId": 19, "name": "Key" }}`

### 3.3 修改会社

`PUT /community/brand` · 登录 · 前端 ✅ · 请求体同新增，需带 `brandId`

### 3.4 删除会社

`DELETE /community/brand/{ids}` · 登录 · 前端 ✅ · 多 ID 逗号分隔

> 注意：删除会社**不会**级联处理 `gal_game.brand_id`，前端应在删除前提示「该会社下仍有游戏」。

---

## 4. 标签接口

### 4.1 标签列表

`GET /community/tag/list` · **匿名** · 前端 ✅

**响应**（同 `data` 数组结构，按 `tagId` 升序）

```json
{ "code": 200, "msg": "操作成功", "data": [ { "tagId": 1, "name": "校园", "type": "theme", "createTime": "2024-01-01 00:00:00" } ] }
```

### 4.2 新增标签

`POST /community/tag` · 登录 · 前端 ✅

| 字段 | 必填 | 说明 |
|---|---|---|
| `name` | 是 | 标签名称（表上有唯一索引 `uk_gal_tag_name`） |
| `type` | 否 | `theme` / `play` / `type`，默认 `theme` |

```json
{ "name": "废萌", "type": "play" }
```

**响应**：`{"code":200,"msg":"操作成功","data":{"tagId":21,"name":"废萌","type":"play"}}`

### 4.3 修改标签

`PUT /community/tag` · 登录 · 前端 ✅ · 需带 `tagId`

### 4.4 删除标签

`DELETE /community/tag/{ids}` · 登录 · 前端 ✅ · 多 ID 逗号分隔

> 删除标签**不会**清理 `gal_game_tag` 中的关联记录，可能残留无效关联（游戏详情查标签时会按 `tag_id` 批量查询，查不到则不展示）。

---

## 5. 前端对接提示（本模块）

1. `Tag.type` 前端定义为 `number`，后端是**字符串** `theme/play/type`，需同步修改类型并调整筛选逻辑。
2. `Game.status` 前端定义为 `number`，后端是**字符串** `'0'/'1'`。
3. 会社列表/标签列表返回的是 `AjaxResult.data`（数组），不是 `TableDataInfo.rows`，前端 `GameListView.vue` / `GameManageView.vue` 已做兼容读取，新增页面请沿用 `unwrap()` 或读 `data`。
4. `GameDetailView` 中「评分」与「资源」是两个独立接口，需分别请求（`/community/rating/{gameId}` 需登录，`/community/resource/list` 匿名）。
