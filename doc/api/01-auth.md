# 01 · 认证与账号接口

> 共 14 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。

| # | 方法 | 路径 | 鉴权 | 前端状态 |
|---|---|---|---|---|
| 1 | GET | `/captchaImage` | 匿名 | ✅ |
| 2 | POST | `/login` | 匿名 | ✅ |
| 3 | POST | `/logout` | 登录 | ✅ |
| 4 | GET | `/getInfo` | 登录 | ✅ |
| 5 | GET | `/getRouters` | 登录 | 🔸 |
| 6 | POST | `/register` | 匿名 | ✅ |
| 7 | GET | `/register/code` | 匿名 | ✅ |
| 8 | GET | `/forget/code` | 匿名 | ✅ |
| 9 | POST | `/code` | 匿名 | ❌ |
| 10 | PUT | `/forgetPwd` | 匿名 | ✅ |
| 11 | GET | `/system/user/profile` | 登录 | ❌ |
| 12 | PUT | `/system/user/profile` | 登录 | ✅ |
| 13 | PUT | `/system/user/profile/updatePwd` | 登录 | ✅ |
| 14 | POST | `/system/user/profile/avatar` | 登录 | ✅ |

---

## 1. 图形验证码

`GET /captchaImage` · 匿名

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "captchaEnabled": true,
  "uuid": "8e36a52c7dba425abbd3613a44331b43",
  "img": "/9j/4AAQSkZJRgABAQAAAQ..."
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `captchaEnabled` | boolean | 验证码开关（读取 `sys_config.sys.account.captchaEnabled`）。为 `false` 时**不返回** `uuid` 与 `img` |
| `uuid` | string | 验证码唯一标识，登录时需回传 |
| `img` | string | 验证码图片 Base64（JPEG）。前端拼接 `data:image/gif;base64,` 前缀即可显示 |

**备注**

- 验证码内容存 Redis，key = `captcha_codes:<uuid>`，有效期 **2 分钟**（`Constants.CAPTCHA_EXPIRATION`）。
- 验证码类型由配置 `awenovel.captchaType` 决定：`math`（算术题）/ `char`（字符）。

---

## 2. 登录

`POST /login` · 匿名

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码（明文，HTTPS 传输） |
| `code` | string | 条件必填 | 图形验证码；`captchaEnabled=true` 时必填 |
| `uuid` | string | 条件必填 | 图形验证码 uuid，与 `code` 同时必填 |

```json
{ "username": "admin", "password": "admin123", "code": "12", "uuid": "8e36a52c..." }
```

**响应**（`token` 在**顶层**，不在 `data` 内）

```json
{ "code": 200, "msg": "操作成功", "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

**失败示例**

```json
{ "code": 500, "msg": "验证码错误" }
{ "code": 500, "msg": "用户不存在/密码错误" }
{ "code": 500, "msg": "密码输入错误5次，帐户锁定10分钟" }
{ "code": 500, "msg": "很遗憾，访问IP已被列入系统黑名单" }
```

**校验顺序**

1. 验证码（若开启）→ 2. 用户名长度 2-20、密码长度 5-20 → 3. IP 黑名单（`sys_config.sys.login.blackIPList`）→ 4. 密码比对（BCrypt）→ 5. 账号状态（停用/删除会拒绝）。

**备注**

- 密码错误累计 **5 次锁定 10 分钟**（`user.password.maxRetryCount` / `lockTime`）。
- 登录成功会写入 `sys_logininfor` 登录日志（异步）。
- 新用户注册后**没有任何角色**，登录成功但无管理权限。

---

## 3. 登出

`POST /logout` · 登录

**请求**：无参数（携带 `Authorization` 头即可）

**响应**

```json
{ "code": 200, "msg": "退出成功" }
```

**备注**：服务端删除 Redis 中的登录态并记录登出日志。前端需同时清空 `localStorage.token`。

---

## 4. 获取当前用户信息

`GET /getInfo` · 登录

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "user": {
    "userId": 1,
    "userName": "admin",
    "nickName": "AweNovel管理员",
    "email": "",
    "phonenumber": "",
    "sex": "1",
    "avatar": "",
    "status": "0",
    "loginIp": "127.0.0.1",
    "loginDate": "2024-01-01 00:00:00",
    "createTime": "2024-01-01 00:00:00"
  },
  "roles": ["admin"],
  "permissions": ["*:*:*"]
}
```

| 字段 | 说明 |
|---|---|
| `user` | 当前登录用户（`SysUser`，**不含密码**） |
| `roles` | 角色标识集合（`role_key`），超管为 `["admin"]` |
| `permissions` | 权限标识集合，超管为 `["*:*:*"]` |

`SysUser` 字段：`userId` / `userName` / `nickName` / `email` / `phonenumber` / `sex`(0男 1女 2未知) / `avatar` / `status`(0正常 1停用) / `delFlag` / `loginIp` / `loginDate` / `createBy` / `createTime` / `updateBy` / `updateTime` / `remark`

---

## 5. 获取路由菜单

`GET /getRouters` · 登录 · 前端状态：🔸 已封装未使用

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "name": "System",
      "path": "/system",
      "hidden": false,
      "component": "Layout",
      "meta": { "title": "系统管理", "icon": "system", "noCache": false },
      "children": []
    }
  ]
}
```

**备注**：当前管理端使用**静态路由**（`web-ui/admin/src/router/index.ts`），未使用该接口。若后续改为「菜单由数据库驱动」，需要对接此接口。

---

## 6. 注册

`POST /register` · 匿名

**请求体**（`RegisterBody`，继承 `LoginBody`）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | 是 | 用户名，2-20 个字符 |
| `password` | string | 是 | 密码，5-20 个字符 |
| `nickname` | string | 是 | 昵称（后端字段为 `nickname`，注意前端 `RegisterParams` 用的也是 `nickname`） |
| `email` | string | 是 | 邮箱（用于验证码校验与唯一性校验） |
| `code` | string | 条件必填 | **邮箱验证码**（不是图形验证码）；`sys.account.captchaEnabled=true` 时必填 |
| `emailType` | string | 否 | 传 `REGISTER`（后端未强制校验该字段） |
| `uuid` | string | 否 | 图形验证码 uuid（当前注册流程**不使用**图形验证码） |

```json
{
  "username": "player01",
  "password": "123456",
  "nickname": "萌新玩家",
  "email": "123456@qq.com",
  "code": "654321"
}
```

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**失败示例**

```json
{ "code": 500, "msg": "邮箱验证码错误" }
{ "code": 500, "msg": "保存用户'player01'失败，注册账号已存在" }
{ "code": 500, "msg": "当前系统没有开启注册功能！" }
```

**备注**

- 开关：`sys_config.sys.account.registerUser`，为 `true` 才允许注册。
- 注册用户**不会分配角色**，也不会创建社区画像（`gal_user_profile` 在首次签到/访问个人中心时懒创建）。
- 由 `AsyncManager` 异步写入注册日志。

---

## 7. 发送注册邮箱验证码

`GET /register/code?email=xxx` · 匿名

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `email` | string | 是 | 接收验证码的邮箱 |

**响应**

```json
{ "code": 200, "msg": "验证码发送成功" }
```

**失败示例**

```json
{ "code": 500, "msg": "当前系统没有开启邮箱功能！" }
{ "code": 500, "msg": "该邮箱已被注册" }
{ "code": 500, "msg": "验证码已发送，请勿频繁操作" }
```

**备注**

- 开关：`sys_config.sys.account.enable.Email`。
- 验证码存 Redis，key = `REGISTER:<email>`，有效期 **10 分钟**，邮件正文含验证码。
- 发送频率限制：缓存未过期时再次请求会提示「请勿频繁操作」。
- **该接口的 `email` 无 QQ 邮箱格式限制**（与 `POST /code` 不同）。

---

## 8. 发送找回密码验证码

`GET /forget/code?email=xxx` · 匿名

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `email` | string | 是 | 已注册邮箱 |

**响应 / 失败示例**

```json
{ "code": 200, "msg": "验证码发送成功" }
{ "code": 500, "msg": "该用户不存在" }
```

**备注**：验证码存 Redis，key = `FORGET_PASS:<email>`，有效期 10 分钟。

---

## 9. 发送邮箱验证码（POST 版）

`POST /code` · 匿名 · 前端状态：❌ 未封装

**请求体**（`EmailCodeParam`）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `email` | string | 是 | **仅允许 QQ 邮箱**（正则 `[1-9][0-9]+@qq.com`，不满足返回参数校验错误） |
| `type` | string | 是 | `REGISTER`（注册）或 `FORGET_PASS`（找回密码） |

```json
{ "email": "123456@qq.com", "type": "REGISTER" }
```

**响应**：同第 7、8 节。

**备注**：与 `GET /register/code`、`GET /forget/code` 功能重合，建议前端统一使用 GET 版本，此接口可忽略。

---

## 10. 重置密码（忘记密码）

`PUT /forgetPwd` · 匿名

**请求体**（`ForgetBody`）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `email` | string | 是 | 注册邮箱 |
| `emailType` | string | 是 | 必须传 `FORGET_PASS`（用于读取 Redis 中对应的验证码） |
| `code` | string | 是 | 邮箱验证码 |
| `password` | string | 是 | 新密码（明文，后端 BCrypt 加密后存储） |

```json
{
  "email": "123456@qq.com",
  "emailType": "FORGET_PASS",
  "code": "654321",
  "password": "newPwd123"
}
```

**响应**

```json
{ "code": 200, "msg": "重置密码成功" }
{ "code": 500, "msg": "验证码错误" }
{ "code": 500, "msg": "重置密码失败" }
```

**备注（风险）**

- 若 `email` 在库中不存在，后端 `user` 为 `null` 会抛空指针 → 返回 **500 系统异常**。前端应先提示「邮箱未注册」。
- 验证码校验成功后**不会删除 Redis 缓存**，同一验证码 10 分钟内可重复使用。

---

## 11. 个人信息

`GET /system/user/profile` · 登录 · 前端状态：❌ 未封装

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { "userId": 1, "userName": "admin", "nickName": "AweNovel管理员", "sex": "1" },
  "roleGroup": "超级管理员"
}
```

| 字段 | 说明 |
|---|---|
| `data` | 当前登录用户（`SysUser`） |
| `roleGroup` | 角色名称，多个用 `,` 连接 |

**备注**：用户基础信息可从 `/getInfo` 获取，本接口额外返回 `roleGroup`。

---

## 12. 修改个人资料

`PUT /system/user/profile` · 登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `nickName` | string | 否 | 昵称 |
| `email` | string | 否 | 邮箱（唯一性校验） |
| `phonenumber` | string | 否 | 手机号（唯一性校验） |
| `sex` | string | 否 | 性别：`0`男 `1`女 `2`未知 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
{ "code": 500, "msg": "修改用户'admin'失败，手机号码已存在" }
```

**备注**：仅允许修改上述 4 个字段，其他字段（如 `userName`）会被忽略。

---

## 13. 修改密码

`PUT /system/user/profile/updatePwd` · 登录

> ⚠️ 参数为 **Query 参数**（不是 JSON body）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `oldPassword` | string | 是 | 旧密码 |
| `newPassword` | string | 是 | 新密码 |

```
PUT /system/user/profile/updatePwd?oldPassword=admin123&newPassword=newPwd456
```

**响应**

```json
{ "code": 200, "msg": "操作成功" }
{ "code": 500, "msg": "修改密码失败，旧密码错误" }
{ "code": 500, "msg": "新密码不能与旧密码相同" }
```

---

## 14. 上传头像

`POST /system/user/profile/avatar` · 登录

**请求**：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `avatarfile` | file | 是 | 头像图片（限 `MimeTypeUtils.IMAGE_EXTENSION`：bmp/gif/jpg/jpeg/png） |

**响应**

```json
{ "code": 200, "msg": "操作成功", "imgUrl": "/profile/avatar/2024/01/01/xxx_20240101120000A001.png" }
```

| 字段 | 说明 |
|---|---|
| `imgUrl` | 头像相对路径，前端拼接域名访问（`/profile/**` 为静态资源映射） |

**备注**：上传目录由 `awenovel.profile` 配置（开发 `D:/AweNovel/uploadPath`，生产 `/home/awenovel/uploadPath`），头像存于其 `avatar/` 子目录。
