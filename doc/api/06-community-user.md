# 06 · 用户中心接口（签到 / 积分 / 消息 / 社交）

> 共 10 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> ⚠️ 本模块**全部需要登录**。表结构见 [`../../sql/awe_novel.sql`](../../sql/awe_novel.sql)。

| # | 方法 | 路径 | 前端状态 |
|---|---|---|---|
| 1 | POST | `/community/sign` | ✅ |
| 2 | GET | `/community/sign` | ❌（后端空实现） |
| 3 | GET | `/community/user/profile` | ✅ |
| 4 | GET | `/community/user/points` | ✅ |
| 5 | GET | `/community/user/messages` | ✅ |
| 6 | GET | `/community/user/messages/unread` | ✅ |
| 7 | POST | `/community/user/message/read/{messageId}` | ✅ |
| 8 | GET | `/community/user/favorites` | ✅ |
| 9 | GET | `/community/user/following` | ✅ |
| 10 | POST | `/community/user/follow/{targetUserId}` | 🔸 已封装未接页面 |

---

## 1. 积分与等级体系（重要背景）

### 1.1 积分来源与消耗（按源码实现）

| 行为 | 积分 | 经验 | 触发点 |
|---|---|---|---|
| **每日签到** | **+5** | **+5** | `POST /community/sign` |
| 下载资源 | −`resource.points`（可为 0） | — | `POST /community/resource/download/{id}` |
| AI 看板娘聊天 | **−1/次** | — | `GET /ai/chat` |
| 注册 | 0 | 0 | 注册时**不发放**，画像懒创建且积分为 0 |
| 发布文章 / 发布资源 / 评论 / 评分 | 0 | 0 | （源码中**没有**奖励逻辑） |

> ⚠️ **当前唯一的积分获取途径是每日签到（+5/天）**。新用户初始积分为 `0`，而下载资源通常需要 10+ 积分、AI 聊天 1 积分/次，
> 因此「发帖/评论/评分奖励积分」和「注册赠送积分」属于**建议补强的运营缺口**（见 [README §5](README.md#5-后端接口缺口前端无法实现需后端补齐)）。

### 1.2 原始积分流水类型 `changeType`

| 值 | 含义 |
|---|---|
| `signin` | 每日签到（+5） |
| `download` | 下载资源（负数） |
| `ai_chat` | AI 聊天（−1） |
| `register` / `publish` / `comment` / `rating` / `admin` | 代码中预留，**当前未产生** |

### 1.3 等级阈值（表 `gal_level_config`）

| 等级 | 名称 | 所需累计经验 |
|---|---|---|
| 0 | 萌新 | 0 |
| 1 | 初级玩家 | 100 |
| 2 | 中级玩家 | 500 |
| 3 | 高级玩家 | 1500 |
| 4 | 资深玩家 | 4000 |
| 5 | 达人玩家 | 10000 |
| 6 | 传奇玩家 | 20000 |

> 等级由 `exp` 实时计算（取满足 `min_exp <= exp` 的最高等级）。表数据可通过 SQL 直接调整，**没有管理端接口**。

---

## 2. 签到

### 2.1 每日签到

`POST /community/sign` · 登录 · 前端 ✅

**请求**：无参数

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": { "continuous": 3, "points": 5, "exp": 5, "level": 1, "totalPoints": 25 } }
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `continuous` | int | 本次签到后的**连续签到天数**（断签重置为 1） |
| `points` | int | 本次获得积分（固定 5） |
| `exp` | int | 本次获得经验（固定 5） |
| `level` | int | 签到后的等级 |
| `totalPoints` | long | 签到后的积分余额 |

**失败示例**

```json
{ "code": 500, "msg": "今日已签到" }
```

**规则**

- 以「日期」为粒度去重（`gal_sign_record` 上有 `uk_gal_sign_user_date` 唯一索引）。
- 连续天数：查「昨天」是否有签到记录，有则 `昨天连续天数 + 1`，否则为 `1`。
- 签到记录写入 `points_award = 5`、`exp_award = 5`；同时写积分流水（`change_type = 'signin'`）并累加经验、重算等级。
- **连签奖励没有阶梯加成**（无论连续多少天都是 +5/+5）。

### 2.2 签到状态

`GET /community/sign` · 登录 · 前端 ❌

**响应（当前实现）**

```json
{ "code": 200, "msg": "操作成功" }
```

> ⚠️ **该接口是空实现**（仅返回成功，不返回任何状态数据）。
> 前端若要显示「今天是否已签到 / 连续天数」，需**后端补齐**：建议返回
> `{"signed": true, "continuousDays": 3, "points": 5}`；在此之前，前端只能通过是否收到「今日已签到」错误来推断状态。

---

## 3. 我的画像

`GET /community/user/profile` · 登录 · 前端 ✅

**响应**

```json
{
  "code": 200, "msg": "操作成功",
  "data": {
    "userId": 2, "points": 25, "exp": 15, "level": 1, "signStreak": 3,
    "createTime": "2024-01-01 00:00:00", "updateTime": "2024-01-03 12:00:00"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | long | 用户ID（与 `sys_user.user_id` 一致） |
| `points` | long | 积分余额 |
| `exp` | long | 累计经验 |
| `level` | int | 等级（0-6） |
| `signStreak` | int | 连续签到天数（**注意：签到接口不会更新该字段**，只在创建时为 0，因此该值长期为 0，实际连续天数看签到接口返回值） |
| `createTime` / `updateTime` | string | 时间 |

**规则**：若该用户在 `gal_user_profile` 没有记录，接口会**自动创建**一条（`points=0, exp=0, level=0, signStreak=0`）后返回，因此不会 404。

> ⚠️ 前端 `types.ts` 里的 `nickname` / `avatar` 字段后端**不返回**（画像表没有这些字段），昵称头像需从 `/getInfo` 获取。

---

## 4. 积分流水

`GET /community/user/points` · 登录 · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<PointLog>`（按 `log_id` 倒序）

```json
{
  "code": 200, "msg": "查询成功", "total": 2,
  "rows": [
    { "logId": 2, "userId": 2, "changeType": "download", "changeAmount": -10, "balanceAfter": 15,
      "bizType": "resource", "bizId": 8, "remark": "下载资源", "createTime": "2024-01-03 12:10:00" },
    { "logId": 1, "userId": 2, "changeType": "signin", "changeAmount": 5, "balanceAfter": 25,
      "bizType": "sign", "bizId": 1, "remark": "每日签到", "createTime": "2024-01-03 12:00:00" }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `changeAmount` | 变化量，**正数为收入、负数为支出** |
| `balanceAfter` | 该笔流水后的余额 |
| `bizType` / `bizId` | 业务类型与业务ID（如 `resource` / 资源ID、`sign` / 签到记录ID、`chat` / 空） |

> ⚠️ `balanceAfter` 存在精度缺陷：`addPoints` 用的是**更新前查出的余额 + amount**，`spendPoints` 用的是**更新后余额**，并发场景可能与实际余额不一致；前端展示请以 `/community/user/profile` 的 `points` 为准。

---

## 5. 站内消息

### 5.1 消息列表

`GET /community/user/messages` · 登录 · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<Message>`（按 `message_id` 倒序）

```json
{
  "code": 200, "msg": "查询成功", "total": 1,
  "rows": [
    { "messageId": 1, "userId": 2, "senderId": null, "type": "system",
      "content": "欢迎加入 AweNovel", "isRead": "0", "createTime": "2024-01-01 00:00:00" }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `senderId` | 发送者ID，系统消息为 `null` |
| `type` | 消息类型：`comment` 评论 / `like` 点赞 / `audit` 审核 / `system` 系统 |
| `isRead` | **字符串** `'0'` 未读 / `'1'` 已读（⚠️ 前端 `types.ts` 定义为 boolean，需改） |

> ⚠️ 消息表已建、Service 已实现，但**没有任何业务代码会触发发送消息**（评论/审核/点赞都不发通知）。
> 因此除初始化数据外，用户的收件箱一般为空。建议后端在「评论回复、审核结果、关注」等场景补 `MessageService.send`。

### 5.2 未读消息数

`GET /community/user/messages/unread` · 登录 · 前端 ✅

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": 3 }
```

> `data` 为**数字**（不是对象）；无未读时为 `0`。前端顶栏红点可直接用该值。

### 5.3 标记已读

`POST /community/user/message/read/{messageId}` · 登录 · 前端 ✅

**响应**：`{"code":200,"msg":"操作成功"}`

**规则**：仅能标记**自己的**消息（SQL 条件含 `user_id = 当前用户`）；不存在或非本人消息静默成功。

> 没有「全部标记已读」接口，如需请后端补充或前端循环调用。

---

## 6. 收藏

### 6.1 我的收藏

`GET /community/user/favorites` · 登录 · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<UserFavorite>`（按 `id` 倒序）

```json
{
  "code": 200, "msg": "查询成功", "total": 2,
  "rows": [
    { "id": 2, "userId": 2, "targetType": "G", "targetId": 1, "createTime": "2024-01-03 12:00:00" }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `targetType` | `G`游戏 / `A`文章 / `R`资源 |
| `targetId` | 目标ID |

> ⚠️ **返回的是收藏关联原始行，不含游戏/文章对象**。前端要展示收藏列表，需要按 `targetType + targetId` **逐个再请求详情接口**（N+1 问题），建议后端补联表返回（见 [README §5](README.md#5-后端接口缺口前端无法实现需后端补齐)）。
> ⚠️ 前端 `types.ts` 中 `Favorite` 的 `favoriteId` / `gameId` / `game` 字段后端**都不返回**，实际主键字段名为 **`id`**。

### 6.2 收藏 / 取消收藏

❌ **后端未提供接口**（`FavoriteService.favorite / unfavorite / isFavorited` 已实现，但没有 Controller）。
前端目前**无法实现收藏/取消收藏功能**，需后端新增接口后才能对接。

---

## 7. 关注

### 7.1 我的关注列表

`GET /community/user/following` · 登录 · 前端 ✅

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<UserFollow>`（按 `id` 倒序）

```json
{
  "code": 200, "msg": "查询成功", "total": 1,
  "rows": [
    { "id": 1, "userId": 2, "followUserId": 1, "createTime": "2024-01-03 12:00:00" }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `userId` | 关注者（当前登录用户） |
| `followUserId` | 被关注用户ID |

> ⚠️ 前端 `FollowItem` 定义的是 `followId` / `targetUserId` / `nickname` / `avatar`，与后端实际字段（`id` / `followUserId`）**全部不一致**，且后端不返回昵称头像 → 关注列表当前无法直接展示用户名，需前端改造或后端补联表。

### 7.2 关注 / 取关

`POST /community/user/follow/{targetUserId}` · 登录 · 前端 🔸 已封装未接页面

| 参数 | 说明 |
|---|---|
| `targetUserId` | 目标用户ID（路径参数） |

**响应**：`{"code":200,"msg":"操作成功"}`

**规则（幂等切换语义）**

- 服务端先判断是否已关注：**已关注则取关，未关注则关注**（同一个接口完成两个动作）。
- 不能关注自己（`userId == targetUserId` 时静默返回成功但不写入）。
- 重复调用会来回切换，前端需自行维护按钮状态（建议同时用关注列表接口校验）。

> ⚠️ 没有「粉丝列表」接口（`FollowService.pageFollowers` 已实现但未暴露），也**没有「是否已关注」查询接口** → 前端无法在进入他人主页时正确渲染「已关注/关注」按钮初始状态，建议后端补齐。

---

## 8. 前端对接提示（本模块）

1. `Message.isRead` 是**字符串** `'0'/'1'`，不是 boolean。
2. `Favorite` / `FollowItem` 的字段名与后端不一致（详见上文），需同步 `types.ts`。
3. `UserProfile.signStreak` 恒为 0（签到不更新该字段），连续签到天数请用 `POST /community/sign` 的返回值。
4. 「今日是否已签到」无查询接口（`GET /community/sign` 是空实现），建议优先让后端补齐。
5. 收藏/取消收藏、点赞、粉丝列表、是否已关注 **4 项能力后端缺接口**，前端排期时需先确认后端补齐时间。
