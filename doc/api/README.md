# AweNovel 后端接口文档 · 总览

> 本文档由后端源码（`weidey-admin` / `weidey-AweNovel` / `weidey-framework` / `weidey-system`）逐接口核对生成，
> 用于前端对接与工作量评估。**共覆盖 138 个接口**。
>
> 模块详情见同目录分册：
>
> | 分册 | 模块 | 接口数 |
> |---|---|---|
> | [01-auth.md](01-auth.md) | 认证与账号（登录/注册/验证码/个人资料） | 14 |
> | [02-system.md](02-system.md) | 系统管理（用户/角色/菜单/字典/参数/公告） | 60 |
> | [03-monitor.md](03-monitor.md) | 系统监控（日志/在线用户/缓存/服务器） | 19 |
> | [04-community-game.md](04-community-game.md) | 游戏库（游戏/会社/标签） | 14 |
> | [05-community-content.md](05-community-content.md) | 内容（文章/资源/评论/评分） | 12 |
> | [06-community-user.md](06-community-user.md) | 用户中心（签到/积分/消息/社交） | 10 |
> | [07-review-ai-common.md](07-review-ai-common.md) | 内容审核 / AI 看板娘 / 通用文件 | 9 |

---

## 1. 通用约定

### 1.1 服务地址

| 环境 | 地址 | 说明 |
|---|---|---|
| 开发 | `http://localhost:9090` | context-path 为 `/`，接口路径即下文所列路径 |
| 前端开发代理 | `http://localhost:5173` / `5174` | Vite 已代理 `/login /register /captchaImage /getInfo /getRouters /logout /community /ai /system /monitor /profile` 到 9090 |
| 生产 | `https://<域名>` | Nginx 反向代理，静态资源与接口同源 |

### 1.2 鉴权

- 除标注「匿名」的接口外，所有接口都需要登录。
- 登录成功后返回 `token`，前端需在请求头携带：

```
Authorization: Bearer <token>
```

- Token 有效期默认 **30 分钟**（`token.expireTime`，采用滑动续期：每次请求若剩余时间不足 20 分钟会自动刷新）。
- 未登录/Token 失效返回 **HTTP 401**，响应体形如 `{"code":401,"msg":"认证失败，无法访问系统资源"}`。
- 无权限返回 **HTTP 403**，响应体形如 `{"code":403,"msg":"没有访问权限，请联系管理员"}`。

### 1.3 响应结构

**普通接口**（`AjaxResult`，本质是 `HashMap`）：

```json
{ "code": 200, "msg": "操作成功", "data": { } }
```

- `code`：200 成功、500 业务异常、401 未认证、403 无权限；部分接口还会返回 `warn`(601)。
- `data`：**可选字段**，`AjaxResult.success()` 无参时不含 `data`，前端取值需判空。
- 部分接口把业务字段**平铺在顶层**（如 `/login` 的 `token`、`/getInfo` 的 `user/roles/permissions`、`/captchaImage` 的 `uuid/img`），此时没有 `data` 包裹。

**分页接口**（`TableDataInfo`）：

```json
{ "code": 200, "msg": "查询成功", "total": 128, "rows": [ ] }
```

**文件下载接口**直接返回二进制流（`application/octet-stream`），不走上述结构。

### 1.4 分页与排序参数

所有 `getDataTable(startPage(), ...)` 的列表接口统一支持：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` | int | 否 | 页码，**从 1 开始**；不传则不分页（返回全部） |
| `pageSize` | int | 否 | 每页条数 |
| `orderByColumn` | string | 否 | 排序列（驼峰会自动转下划线） |
| `isAsc` | string | 否 | `asc` / `desc`（兼容 `ascending` / `descending`） |
| `reasonable` | boolean | 否 | 页码合理化，默认 `true` |

> 说明：当 `pageNum` 为空时 `startPage()` 不启动分页，返回的 `total` 为 0，`rows` 为全量数据，前端需自行判断。

### 1.5 时间与静态资源

- 时间字段统一格式 `yyyy-MM-dd HH:mm:ss`；`SysOperLog.operTime` 显式使用该格式，其余为 Jackson 默认（`yyyy-MM-dd'T'HH:mm:ss.SSSZ` 或时间戳，建议前端做兼容解析）。
- 上传的文件通过 `/profile/**` 访问（后端将 `${awenovel.profile}` 目录映射到该路径），数据库中存储的是形如 `/profile/upload/2024/01/01/xxx.png` 的相对路径，前端拼域名即可（前端已有 `resolveAssetUrl` 工具）。

### 1.6 前端对接状态标记

| 标记 | 含义 |
|---|---|
| ✅ 已对接 | `web-ui/shared/src/api.ts` 已封装，且已在页面/组件中调用 |
| 🔸 仅封装 | `api.ts` 已封装，但没有任何页面调用 |
| ❌ 未封装 | 前端尚无对应 API 函数（需前端新增封装，或后端补齐/裁剪接口） |

---

## 2. 全量接口清单与前端对接状态

### 2.1 认证与账号（14）

| 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|
| GET | `/captchaImage` | 匿名 | ✅ |
| POST | `/login` | 匿名 | ✅ |
| POST | `/logout` | 登录 | ✅ |
| GET | `/getInfo` | 登录 | ✅ |
| GET | `/getRouters` | 登录 | 🔸 |
| POST | `/register` | 匿名 | ✅ |
| GET | `/register/code` | 匿名 | ✅ |
| GET | `/forget/code` | 匿名 | ✅ |
| POST | `/code` | 匿名 | ❌ |
| PUT | `/forgetPwd` | 匿名 | ✅ |
| GET | `/system/user/profile` | 登录 | ❌ |
| PUT | `/system/user/profile` | 登录 | ✅ |
| PUT | `/system/user/profile/updatePwd` | 登录 | ✅ |
| POST | `/system/user/profile/avatar` | 登录 | ✅ |

### 2.2 系统管理（60）

**用户管理（12）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/user/list` | `system:user:list` | ✅（仅列表） |
| POST | `/system/user/export` | `system:user:export` | ❌ |
| POST | `/system/user/importData` | `system:user:import` | ❌ |
| POST | `/system/user/importTemplate` | 登录 | ❌ |
| GET | `/system/user/{userId}` | `system:user:query` | ❌ |
| POST | `/system/user` | `system:user:add` | ❌ |
| PUT | `/system/user` | `system:user:edit` | ❌ |
| DELETE | `/system/user/{userIds}` | `system:user:remove` | ❌ |
| PUT | `/system/user/resetPwd` | `system:user:resetPwd` | ❌ |
| PUT | `/system/user/changeStatus` | `system:user:edit` | ❌ |
| GET | `/system/user/authRole/{userId}` | `system:user:query` | ❌ |
| PUT | `/system/user/authRole` | `system:user:edit` | ❌ |

**角色管理（13）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/role/list` | `system:role:list` | ✅ 查询页 |
| POST | `/system/role/export` | `system:role:export` | ❌ |
| GET | `/system/role/{roleId}` | `system:role:query` | ❌ |
| POST | `/system/role` | `system:role:add` | ❌ |
| PUT | `/system/role` | `system:role:edit` | ❌ |
| PUT | `/system/role/changeStatus` | `system:role:edit` | ❌ |
| DELETE | `/system/role/{roleIds}` | `system:role:remove` | ❌ |
| GET | `/system/role/optionselect` | `system:role:query` | ❌ |
| GET | `/system/role/authUser/allocatedList` | `system:role:list` | ❌ |
| GET | `/system/role/authUser/unallocatedList` | `system:role:list` | ❌ |
| PUT | `/system/role/authUser/cancel` | `system:role:edit` | ❌ |
| PUT | `/system/role/authUser/cancelAll` | `system:role:edit` | ❌ |
| PUT | `/system/role/authUser/selectAll` | `system:role:edit` | ❌ |

**菜单管理（7）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/menu/list` | `system:menu:list` | ✅ 查询页 |
| GET | `/system/menu/{menuId}` | `system:menu:query` | ❌ |
| GET | `/system/menu/treeselect` | 登录 | ❌ |
| GET | `/system/menu/roleMenuTreeselect/{roleId}` | 登录 | ❌ |
| POST | `/system/menu` | `system:menu:add` | ❌ |
| PUT | `/system/menu` | `system:menu:edit` | ❌ |
| DELETE | `/system/menu/{menuId}` | `system:menu:remove` | ❌ |

**字典类型（8）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/dict/type/list` | `system:dict:list` | ✅ 查询页 |
| POST | `/system/dict/type/export` | `system:dict:export` | ❌ |
| GET | `/system/dict/type/{dictId}` | `system:dict:query` | ❌ |
| POST | `/system/dict/type` | `system:dict:add` | ❌ |
| PUT | `/system/dict/type` | `system:dict:edit` | ❌ |
| DELETE | `/system/dict/type/{dictIds}` | `system:dict:remove` | ❌ |
| DELETE | `/system/dict/type/refreshCache` | `system:dict:remove` | ❌ |
| GET | `/system/dict/type/optionselect` | 登录 | ❌ |

**字典数据（7）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/dict/data/list` | `system:dict:list` | ✅ 查询页 |
| POST | `/system/dict/data/export` | `system:dict:export` | ❌ |
| GET | `/system/dict/data/{dictCode}` | `system:dict:query` | ❌ |
| GET | `/system/dict/data/type/{dictType}` | 登录 | ❌ |
| POST | `/system/dict/data` | `system:dict:add` | ❌ |
| PUT | `/system/dict/data` | `system:dict:edit` | ❌ |
| DELETE | `/system/dict/data/{dictCodes}` | `system:dict:remove` | ❌ |

**参数配置（8）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/config/list` | `system:config:list` | ✅ 查询页 |
| POST | `/system/config/export` | `system:config:export` | ❌ |
| GET | `/system/config/{configId}` | `system:config:query` | ❌ |
| GET | `/system/config/configKey/{configKey}` | 登录 | ❌ |
| POST | `/system/config` | `system:config:add` | ❌ |
| PUT | `/system/config` | `system:config:edit` | ❌ |
| DELETE | `/system/config/{configIds}` | `system:config:remove` | ❌ |
| DELETE | `/system/config/refreshCache` | `system:config:remove` | ❌ |

**通知公告（5）**

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/system/notice/list` | `system:notice:list` | ✅ 查询页 |
| GET | `/system/notice/{noticeId}` | `system:notice:query` | ✅ 详情弹窗 |
| POST | `/system/notice` | `system:notice:add` | ❌ |
| PUT | `/system/notice` | `system:notice:edit` | ❌ |
| DELETE | `/system/notice/{noticeIds}` | `system:notice:remove` | ❌ |

### 2.3 系统监控（19）

| 方法 | 路径 | 权限 | 前端状态 |
|---|---|---|---|
| GET | `/monitor/logininfor/list` | `monitor:logininfor:list` | ✅ 查询页 |
| POST | `/monitor/logininfor/export` | `monitor:logininfor:export` | ❌ |
| DELETE | `/monitor/logininfor/{infoIds}` | `monitor:logininfor:remove` | ❌ |
| DELETE | `/monitor/logininfor/clean` | `monitor:logininfor:remove` | ❌ |
| GET | `/monitor/logininfor/unlock/{userName}` | `monitor:logininfor:unlock` | ❌ |
| GET | `/monitor/operlog/list` | `monitor:operlog:list` | ✅ 查询页 |
| POST | `/monitor/operlog/export` | `monitor:operlog:export` | ❌ |
| DELETE | `/monitor/operlog/{operIds}` | `monitor:operlog:remove` | ❌ |
| DELETE | `/monitor/operlog/clean` | `monitor:operlog:remove` | ❌ |
| GET | `/monitor/online/list` | `monitor:online:list` | ✅ 查询页 |
| DELETE | `/monitor/online/{tokenId}` | `monitor:online:forceLogout` | ✅ 强退按钮 |
| GET | `/monitor/cache` | `monitor:cache:list` | ✅ 概览 |
| GET | `/monitor/cache/getNames` | `monitor:cache:list` | ✅ 分类列表 |
| GET | `/monitor/cache/getKeys/{cacheName}` | `monitor:cache:list` | ❌ |
| GET | `/monitor/cache/getValue/{cacheName}/{cacheKey}` | `monitor:cache:list` | ❌ |
| DELETE | `/monitor/cache/clearCacheName/{cacheName}` | `monitor:cache:list` | ❌ |
| DELETE | `/monitor/cache/clearCacheKey/{cacheKey}` | `monitor:cache:list` | ❌ |
| DELETE | `/monitor/cache/clearCacheAll` | `monitor:cache:list` | ❌ |
| GET | `/monitor/server` | `monitor:server:list` | ✅ 监控页 |

### 2.4 游戏库（14）

| 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|
| GET | `/community/game/list` | **匿名** | ✅ |
| GET | `/community/game/{gameId}` | **匿名** | ✅ |
| GET | `/community/game/tag/all` | **匿名** | ✅ |
| POST | `/community/game` | 登录（**无权限校验，见 §6.2**） | ✅ |
| PUT | `/community/game` | 登录（同上有风险） | ✅ |
| DELETE | `/community/game/{gameIds}` | 登录（同上有风险） | ✅ |
| GET | `/community/brand/list` | **匿名** | ✅ |
| POST | `/community/brand` | 登录（同上有风险） | ✅ |
| PUT | `/community/brand` | 登录（同上有风险） | ✅ |
| DELETE | `/community/brand/{ids}` | 登录（同上有风险） | ✅ |
| GET | `/community/tag/list` | **匿名** | ✅ |
| POST | `/community/tag` | 登录（同上有风险） | ✅ |
| PUT | `/community/tag` | 登录（同上有风险） | ✅ |
| DELETE | `/community/tag/{ids}` | 登录（同上有风险） | ✅ |

### 2.5 内容：文章 / 资源 / 评论 / 评分（12）

| 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|
| GET | `/community/article/list` | **匿名** | ✅ |
| GET | `/community/article/{articleId}` | **匿名** | ✅ |
| POST | `/community/article` | 登录 | 🔸 |
| GET | `/community/resource/list` | **匿名** | ✅ |
| POST | `/community/resource` | 登录 | 🔸 |
| POST | `/community/resource/download/{resourceId}` | 登录 | ✅ |
| POST | `/community/resource/report/{resourceId}` | 登录 | ✅ |
| GET | `/community/comment/list` | **匿名** | ✅ |
| POST | `/community/comment` | 登录 | ✅ |
| DELETE | `/community/comment/{ids}` | 登录（同上有风险） | ✅ |
| GET | `/community/rating/{gameId}` | 登录 | ✅ |
| POST | `/community/rating` | 登录 | ✅ |

### 2.6 用户中心（10）

| 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|
| POST | `/community/sign` | 登录 | ✅ |
| GET | `/community/sign` | 登录 | ❌（**后端为空实现**，见 §5） |
| GET | `/community/user/profile` | 登录 | ✅ |
| GET | `/community/user/points` | 登录 | ✅ |
| GET | `/community/user/messages` | 登录 | ✅ |
| GET | `/community/user/messages/unread` | 登录 | ✅ |
| POST | `/community/user/message/read/{messageId}` | 登录 | ✅ |
| GET | `/community/user/favorites` | 登录 | ✅ |
| GET | `/community/user/following` | 登录 | ✅ |
| POST | `/community/user/follow/{targetUserId}` | 登录 | 🔸 |

### 2.7 内容审核 / AI / 通用（9）

| 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|
| GET | `/community/review/tasks` | `community:review:list` | ✅ |
| POST | `/community/review/approve/{processInstanceId}` | `community:review:edit` | ✅ |
| POST | `/community/review/reject/{processInstanceId}` | `community:review:edit` | ✅ |
| GET | `/ai/chat?message=` | 登录（SSE） | ✅ |
| POST | `/common/upload` | 登录 | ❌ |
| POST | `/common/uploads` | 登录 | ❌ |
| GET | `/common/download` | 登录 | ❌ |
| GET | `/common/download/resource` | 登录 | ❌ |
| GET | `/` | 匿名 | ❌（仅返回欢迎文本，无需对接） |

---

## 3. 前端对接进度分析

按「接口维度」统计：

| 状态 | 数量 | 占比 |
|---|---|---|
| ✅ 已对接（有页面调用） | 61 | 44% |
| 🔸 仅封装未接页面 | 4 | 3% |
| ❌ 未封装 | 73 | 53% |
| **合计** | **138** | 100% |

> 最近一轮管理端改造（动态路由 + 模块化导航）新增对接了 14 个查询类接口：
> 角色/菜单/字典（类型+数据）/参数/公告列表与公告详情、登录日志/操作日志列表、在线用户列表与强退、缓存概览与分类、服务监控。

按「业务视角」看，你提到的「约 60%」更接近**用户端前台**的完成度：

| 视角 | 完成度 | 说明 |
|---|---|---|
| **用户端（web）核心流程** | ≈ 70% | 登录注册/改密、游戏库+详情、文章列表+详情、评论、评分、资源下载、签到、积分流水、消息、AI 看板娘 均已打通 |
| **用户端（web）缺失能力** | — | ①发布文章 UI ②发布资源 UI ③关注/粉丝 UI ④收藏与点赞（**后端也没有 REST 接口**，见 §5）⑤`/community/sign` 签到状态 |
| **管理端（admin）** | ≈ 55% | 已有：**动态路由导航（3 大模块 19 个页面）**、游戏/会社/标签 CRUD、文章与资源审核、评论删除、用户/角色/菜单/字典/参数/公告**查询**、日志查询、在线用户（含强退）、缓存/服务监控、Druid 数据监控 |
| **管理端（admin）缺失能力** | — | 系统管理的**增删改**（用户增删改/重置密码/授权、角色与菜单维护、字典与参数与公告的写操作）、日志的删除/导出/账户解锁、缓存清理、定时任务（后端无接口）、文件上传封装 |

**结论**：前端缺的主要是「**内容生产**（发文章/发资源）」与「**后台系统管理/监控**」两大类，合计约 87 个接口未封装。

---

## 4. 优先级建议（供排期参考）

### P0 — 用户核心闭环（直接影响可用性）

| 项 | 涉及接口 | 说明 |
|---|---|---|
| 发布文章 | `POST /community/article`（已封装，需接页面） | 发布后进入 Flowable 审核，状态 `0=待审核` |
| 发布资源 | `POST /community/resource`（已封装，需接页面） | 需选择所属游戏、积分、网盘/磁力链接 |
| 关注/粉丝 | `POST /community/user/follow/{id}`（已封装） + `GET /community/user/following`（已对接） | 缺粉丝列表接口（见 §5） |
| 收藏 / 点赞 | **需后端新增接口**（见 §5） | 前端无法实现，属后端补齐项 |
| 文件上传封装 | `POST /common/upload` | 发文章/资源需要封面上传；当前前端只有头像上传 |

### P1 — 管理端基础能力（运营必需）

| 项 | 涉及接口 | 说明 |
|---|---|---|
| 用户管理完整化 | `/system/user` 增删改、重置密码、状态切换、授权角色、导出/导入（10 个） | 当前只有列表 |
| 角色与菜单 | `/system/role/**`、`/system/menu/**`（20 个） | 权限体系维护入口 |
| 字典 / 参数 / 公告 | `/system/dict/**`、`/system/config/**`、`/system/notice/**`（28 个） | 站点可配置化 |
| 操作日志 / 登录日志 | `/monitor/operlog/**`、`/monitor/logininfor/**`（9 个） | 审计与排查 |
| 在线用户 | `/monitor/online/**`（2 个） | 强退与在线监控 |

> **进度更新**：上表各模块的**查询页面已全部完成**（列表 + 分页 + 条件筛选，菜单与页面见 [sql/README.md](../../sql/README.md#菜单与动态路由约定)）；
> 剩余工作集中在**写操作**：新增/修改/删除、重置密码、角色授权、菜单树维护、缓存清理、日志导出与账户解锁、文件上传封装。

### P2 — 运维与体验增强

| 项 | 涉及接口 | 说明 |
|---|---|---|
| 缓存监控 / 服务器监控 | `/monitor/cache/**`、`/monitor/server`（8 个） | 运维面板 |
| 系统个人信息 | `GET /system/user/profile` | 可直接用 `/getInfo` 替代 |
| 邮箱验证码（POST） | `POST /code` | 与 `GET /register/code` 功能重合，建议弃用 |
| 首页 / 字典下拉 | `GET /`、`/system/dict/data/type/{dictType}` | 字典前端可本地硬编码 |

---

## 5. 后端接口缺口（前端无法实现，需后端补齐）

以下能力**后端 Service 已实现，但没有暴露 REST 接口**，前端无法对接：

| 缺失能力 | 已有 Service 方法 | 建议新增接口 |
|---|---|---|
| 收藏 / 取消收藏 / 是否已收藏 | `FavoriteService.favorite / unfavorite / isFavorited` | `POST /community/favorite`、`DELETE /community/favorite`、`GET /community/favorite/check` |
| 点赞 / 取消点赞 / 是否已点赞 / 点赞数 | `LikeService.like / unlike / isLiked / countLikes` | `POST /community/like`、`DELETE /community/like`、`GET /community/like/check` |
| 粉丝列表 | `FollowService.pageFollowers` | `GET /community/user/followers` |
| 消息发送（系统通知/互动通知） | `MessageService.send` | 可先由后端在评论/审核等流程内触发，无需对外开放 |
| 等级配置维护 | `LevelConfigMapper`（表 `gal_level_config`） | `GET/PUT /community/level/config`（管理端） |
| 用户积分调整 | `UserProfileService.addPoints / spendPoints` | `POST /community/user/points/adjust`（管理员） |

此外：

- `GET /community/sign` 目前是**空实现**（`return success()`，无数据）。若前端要做「今日是否已签到」，需后端补充：返回 `{signed: boolean, continuousDays: number}`。
- 社区管理类接口（游戏/会社/标签/评论的写操作）**没有权限校验**（详见 §6.2），建议后端补 `@PreAuthorize`。
- 资源列表 `GET /community/resource/list` 只支持 `gameId` 过滤，**不支持按发布者/审核状态查询**（管理端审核页只能拉全量再过滤）。

---

## 6. 契约差异与风险提示

### 6.1 前端类型定义与后端实际不一致（建议修正 `web-ui/shared/src/types.ts`）

| 字段 | 前端定义 | 后端实际 | 影响 |
|---|---|---|---|
| `Tag.type` | `number`（0=风格 1=题材） | `string`：`theme`/`play`/`type` | 标签类型判断失效 |
| `Comment.targetType` | 注释写 `'game' \| 'article'` | `'G'`(游戏) / `'A'`(文章) / `'R'`(资源) | 评论将无法定位目标 |
| `Message.isRead` | `boolean` | `string`：`'0'`/`'1'` | 未读判断恒为真/假 |
| `Game.status` / `Article.status` / `Resource.status` | `number` | `string`：`'0'`/`'1'`/`'2'` | 状态判断需按字符串 |
| `Brand` | 含 `nameCn/country/website` | 后端仅 `brandId/name/logo/description` | 表单字段提交无效 |
| `Resource.userName/gameTitle` | 期望联表返回 | 后端**不返回** | 列表展示为空 |
| `Article.userName/nickname` | 期望联表返回 | 后端**不返回** | 作者名展示为空 |
| `Comment.nickname/avatar` | 期望联表返回 | 实体有该字段但 Service **未填充** | 评论昵称/头像为空 |
| `Favorite` / `FollowItem` | 期望含游戏对象/昵称头像 | 后端返回的是 `gal_user_favorite` / `gal_user_follow` **原始行**（`targetType/targetId`、`userId/followUserId`），无联表字段 | 收藏列表无法直接展示游戏，关注列表字段名不匹配（`followUserId` ≠ `targetUserId`） |

### 6.2 权限风险（后端问题，前端需知晓）

社区管理类写接口**只校验「已登录」，没有 `@PreAuthorize`**，即**任何注册用户都能调用**：

```
POST/PUT/DELETE /community/game
POST/PUT/DELETE /community/brand
POST/PUT/DELETE /community/tag
DELETE           /community/comment/{ids}
```

建议后端补权限（如 `community:game:edit` 等），否则管理端功能等于对全体用户开放。

### 6.3 其他注意点

- `/register` 中的 `code` 是**邮箱验证码**（不是图形验证码）；仅当 `sys_config.sys.account.captchaEnabled = true` 时才校验。
- 注册成功后用户**没有分配任何角色**（`sys_user_role` 无记录），只能访问「登录即可」的接口，管理端功能不可用。
- `POST /code` 的 `email` 有参数校验：**仅允许 QQ 邮箱**（正则 `[1-9][0-9]+@qq.com`），而 `GET /register/code`、`GET /forget/code` 无此限制。
- 邮箱验证码存 Redis（key = `REGISTER:<email>` / `FORGET_PASS:<email>`），**10 分钟有效**，且验证成功后**不删除**（可重复使用），存在一定安全风险。
- 游戏列表默认只返回 `status='1'`（上架）；文章/资源列表默认只返回 `status='1'`（审核通过），审核中的内容前端看不到（管理端审核页目前靠拉全量数据再匹配，见 §5）。

---

## 7. 权限标识清单

后端 `@PreAuthorize("@ss.hasPermi('xxx')")` 用到的权限标识（用于角色/菜单配置）：

```
system:user:list / query / add / edit / remove / export / import / resetPwd
system:role:list / query / add / edit / remove / export
system:menu:list / query / add / edit / remove
system:dict:list / query / add / edit / remove / export
system:config:list / query / add / edit / remove / export
system:notice:list / query / add / edit / remove
monitor:logininfor:list / remove / export / unlock
monitor:operlog:list / remove / export
monitor:online:list / forceLogout
monitor:cache:list
monitor:server:list
community:review:list / edit
```

> 超级管理员角色（`role_key = admin`，`user_id = 1`）拥有 `*:*:*` 全部权限，不受上述限制。

---

## 8. 文档维护

- 本文档与源码同步维护；后端接口发生变更时请同步更新对应分册。
- 数据库结构见 [`../sql/awe_novel.sql`](../sql/awe_novel.sql)，部署见 [`../DEPLOYMENT.md`](../DEPLOYMENT.md)。
