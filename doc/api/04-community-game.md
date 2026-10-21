# 04 · 游戏库接口（游戏 / 会社 / 标签）

> 共 18 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> 表结构见 [`../../sql/awe_novel.sql`](../../sql/awe_novel.sql)（`gal_game` / `gal_brand` / `gal_tag` / `gal_game_tag`）。

| # | 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|---|
| 1 | GET | `/community/game/list` | 匿名 | ✅ 前台列表（仅已发布） |
| 2 | GET | `/community/game/{gameId}` | 匿名 | ✅ 前台详情 |
| 3 | GET | `/community/game/tag/all` | 匿名 | ✅ |
| 4 | GET | `/community/game/admin/list` | `community:game:list` | ✅ 管理端列表（全状态 + 多维筛选） |
| 5 | GET | `/community/game/admin/{gameId}` | `community:game:list` | ✅ 编辑页回显（不增浏览量） |
| 6 | POST | `/community/game/publish/{gameId}` | `community:game:publish` | ✅ 提交发布（进入审核） |
| 7 | POST | `/community/game` | `community:game:add` | ✅ 新增（默认草稿） |
| 8 | PUT | `/community/game` | `community:game:edit` | ✅ 修改 |
| 9 | DELETE | `/community/game/{gameIds}` | `community:game:remove` | ✅ 删除 |
| 10 | GET | `/community/brand/list` | 匿名 | ✅ |
| 11 | POST | `/community/brand` | 登录 | ✅ |
| 12 | PUT | `/community/brand` | 登录 | ✅ |
| 13 | DELETE | `/community/brand/{ids}` | 登录 | ✅ |
| 14 | GET | `/community/tag/list` | 匿名 | ✅ |
| 15 | POST | `/community/tag` | 登录 | ✅ |
| 16 | PUT | `/community/tag` | 登录 | ✅ |
| 17 | DELETE | `/community/tag/{ids}` | 登录 | ✅ |
| 18 | POST | `/community/game/withdraw/{gameId}` | `community:game:publish` | ✅ 撤回审核（回到草稿） |

> ⚠️ 会社/标签的写接口（11~13、15~17）后端**只校验登录、没有 `@PreAuthorize`**，任何注册用户均可调用
> （详见 [README §6.2](README.md#62-权限风险后端问题前端需知晓)）。游戏的写接口已补权限校验。

---

## 0. 状态与审核流程（重要）

`gal_game.status` 语义：

| 值 | 含义 | 前台是否可见 |
|---|---|---|
| `0` | 已下架 | 否 |
| `1` | 已发布（审核通过） | **是** |
| `2` | 待审核 | 否 |
| `3` | 审核驳回 | 否 |
| `4` | 草稿 | 否 |

流程：

```
编辑页「存草稿」 → status=4
编辑页「发布」   → 保存内容 + POST /community/game/publish/{id} → status=2 且启动 Flowable 审核
管理端「内容审核」通过 → status=1（前台可见）
管理端「内容审核」驳回 → status=3（需修改后重新发布）
编辑页「撤回」   → POST /community/game/withdraw/{id} → status=4 且取消审核流程
```

**状态机约束（避免重复发布 / 卡死）**

| 当前状态 | 存草稿 | 发布 | 撤回 | 修改(PUT) |
|---|---|---|---|---|
| `4` 草稿 / `3` 驳回 | ✅ | ✅ | — | ✅ |
| `2` 待审核 | ❌（前端禁用，后端拒绝修改） | ❌ | ✅ | ❌ `{"code":500,"msg":"该游戏正在审核中，请先撤回后再修改"}` |
| `1` 已发布 | ✅（转为草稿） | ❌ | — | ✅ |

- 发布前置校验：`title` 与 `cover` 不能为空，否则返回 `{"code":500,"msg":"请先上传封面图"}`。
- 已经在审核队列（存在运行中的 Flowable 流程）时再次发布：`{"code":500,"msg":"该游戏已在审核队列中，请先撤回后再发布"}`。
- 撤回后流程实例被删除（审核列表不再出现），状态回到草稿，可重新编辑并发布；重复撤回已发布/草稿状态返回
  `{"code":500,"msg":"只有待审核的游戏可以撤回"}`。
- 审核接口见 [07-review-ai-common.md](07-review-ai-common.md)，`bizType = "game"`。


---

## 1. 数据模型

### 1.1 游戏 `Game`（表 `gal_game`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `gameId` | long | 游戏ID（自增，新增时**不要传**） |
| `brandId` | long | 制作会社ID |
| `title` | string | 游戏原名 |
| `titleCn` | string | 游戏译名 |
| `cover` | string | 封面图相对路径（`/profile/upload/...`，前端裁剪后统一 **WebP**，900×1200） |
| `carousel` | string | **轮播图**，JSON 数组字符串（最多 7 张，16:9 · WebP），如 `["/profile/upload/a.webp","/profile/upload/b.webp"]` |
| `releaseDate` | string | 发售日期（`yyyy-MM-dd`） |
| `summary` | string | 简介（**Markdown**，支持内嵌图片/视频：`![x](url)`，`.mp4/.webm` 会渲染为 `<video>`） |
| `staffPaint` | string | 原画 |
| `staffScenario` | string | 剧本 |
| `staffVoice` | string | 主要声优 |
| `ratingAvg` | number | 平均评分（冗余，由评分接口自动重算，1 位小数） |
| `ratingCount` | int | 评分人数（冗余） |
| `viewCount` | int | 浏览量（详情接口自动 +1） |
| `status` | **string** | `0`下架 / `1`已发布 / `2`待审核 / `3`审核驳回 / `4`草稿（新增不传时默认 `4`） |
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

`GET /community/game/list` · **匿名** · 前端 ✅（用户端 GameListView / HomeView）

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
      "cover": "/profile/upload/2024/01/01/clannad.webp",
      "carousel": "[\"/profile/upload/2024/01/01/c1.webp\"]",
      "releaseDate": "2004-04-28",
      "summary": "## 简介\n\n……", "staffPaint": "樋上いたる", "staffScenario": "麻枝准", "staffVoice": "中原麻衣",
      "ratingAvg": 9.2, "ratingCount": 15, "viewCount": 320, "status": "1",
      "brandName": "Key", "createTime": "2024-01-01 00:00:00"
    }
  ]
}
```

**规则**

- 固定只返回 `status = '1'`（已发布）的游戏，**草稿/待审核/驳回/下架都看不到**。
- 排序：`rating_avg DESC, game_id DESC`。
- 每行会**额外填充 `brandName`**（会社名），但**不返回 `tags`**。
- 逻辑删除的记录自动过滤。

### 2.1.1 管理端游戏列表（多维筛选）

`GET /community/game/admin/list` · 权限 `community:game:list` · 前端 ✅（游戏管理页）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 原名 / 译名模糊匹配 |
| `brandId` | long | 否 | 制作会社 |
| `tagIds` | string | 否 | **标签多选**，逗号分隔，如 `1,3,5`（与 `tagId` 二选一优先） |
| `tagId` | long | 否 | 单个标签（兼容旧调用） |
| `beginDate` / `endDate` | string | 否 | **发售日期**区间（`yyyy-MM-dd`） |
| `status` | string | 否 | `0`下架 / `1`已发布 / `2`待审核 / `3`驳回 / `4`草稿 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

- 返回全部状态的游戏，**同时填充 `brandName` 与 `tags`**，排序 `update_time DESC, game_id DESC`。
- 日期按 `release_date` 字段比较（字符串形式的 `yyyy-MM-dd` 直接参与 SQL 比较）。

### 2.1.2 管理端游戏详情

`GET /community/game/admin/{gameId}` · 权限 `community:game:list` · 前端 ✅（编辑页）

- 与前台详情返回一致（含 `tags`、`carousel`），但**不增加浏览量**，且可读取草稿/待审核数据。

### 2.1.3 提交发布（进入审核）

`POST /community/game/publish/{gameId}` · 权限 `community:game:publish` · 前端 ✅

- 校验：`title`、`cover` 非空；状态不能已是 `2`（待审核）。
- 成功：`status` 置为 `2` 并启动 Flowable 审核流程（`bizType = "game"`），返回 `{"code":200,"msg":"已提交审核"}`。
- 审核通过 → `status = 1`（前台可见）；驳回 → `status = 3`。
- 失败示例：`{"code":500,"msg":"请先上传封面图"}`、`{"code":500,"msg":"该游戏已提交审核，请等待审核结果"}`。

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
