# 02 · 系统管理接口

> 共 60 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> ⚠️ 本模块**前端全部未封装**（`web-ui/shared/src/api.ts` 中仅有 `getSystemUserList`），是管理端补齐的重点。

**通用说明**

- 所有接口需登录，并按表格中的权限标识鉴权（超管 `*:*:*` 不受限）。
- 列表接口统一支持分页参数（`pageNum`/`pageSize`/`orderByColumn`/`isAsc`）。
- 支持日期区间查询的列表，通过 `params[beginTime]`、`params[endTime]` 传参（`yyyy-MM-dd HH:mm:ss` 或 `yyyy-MM-dd`）。
- 导出接口（`POST /export`）返回 Excel 二进制流，前端需以 `responseType: 'blob'` 请求；查询条件与列表一致（通常放在 Query 参数中）。

---

## 1. 用户管理 `/system/user`（12）

### 1.1 用户列表

`GET /system/user/list` · 权限 `system:user:list` · 前端 ✅（仅列表页）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userName` | string | 否 | 用户名，模糊匹配 |
| `phonenumber` | string | 否 | 手机号，模糊匹配 |
| `status` | string | 否 | 状态：`0`正常 / `1`停用，精确匹配 |
| `params[beginTime]` | string | 否 | 创建时间起 |
| `params[endTime]` | string | 否 | 创建时间止 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**

```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 2,
  "rows": [
    {
      "userId": 1, "userName": "admin", "nickName": "AweNovel管理员",
      "email": "", "phonenumber": "", "sex": "1", "avatar": "",
      "status": "0", "delFlag": "0", "loginIp": "127.0.0.1",
      "loginDate": "2024-01-01 00:00:00", "createBy": "admin",
      "createTime": "2024-01-01 00:00:00", "remark": "管理员",
      "roles": [ { "roleId": 1, "roleName": "超级管理员", "roleKey": "admin" } ]
    }
  ]
}
```

`SysUser` 字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | long | 用户ID |
| `userName` | string | 登录账号 |
| `nickName` | string | 昵称 |
| `email` | string | 邮箱 |
| `phonenumber` | string | 手机号 |
| `sex` | string | `0`男 `1`女 `2`未知 |
| `avatar` | string | 头像相对路径 |
| `status` | string | `0`正常 `1`停用 |
| `delFlag` | string | `0`存在 `2`删除 |
| `loginIp` / `loginDate` | string | 最后登录 IP / 时间 |
| `createBy` / `createTime` / `updateBy` / `updateTime` | string | 审计字段 |
| `remark` | string | 备注 |
| `roles` | array | 该用户角色列表（`SysRole`） |

### 1.2 用户详情

`GET /system/user/{userId}` · 权限 `system:user:query`
（也支持 `GET /system/user/` 不带 id，此时只返回角色下拉数据）

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { "userId": 2, "userName": "ry", "nickName": "若依", "sex": "0", "status": "0" },
  "roles": [ { "roleId": 1, "roleName": "超级管理员" }, { "roleId": 2, "roleName": "普通角色" } ],
  "roleIds": [2]
}
```

| 字段 | 说明 |
|---|---|
| `data` | 用户详情（不带 id 请求时**不返回**） |
| `roles` | 可选角色列表（非超管用户不返回超管角色） |
| `roleIds` | 该用户已分配的角色 ID 数组（不带 id 请求时不返回） |

### 1.3 新增用户

`POST /system/user` · 权限 `system:user:add`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userName` | string | 是 | 登录账号（唯一） |
| `nickName` | string | 是 | 昵称 |
| `password` | string | 是 | 明文密码，后端 BCrypt 加密 |
| `email` | string | 否 | 邮箱（唯一） |
| `phonenumber` | string | 否 | 手机号（唯一） |
| `sex` | string | 否 | `0`/`1`/`2` |
| `status` | string | 否 | 默认 `0` |
| `roleIds` | long[] | 否 | 角色 ID 数组 |
| `remark` | string | 否 | 备注 |

**响应**：`{ "code": 200, "msg": "操作成功" }`；失败如 `{"code":500,"msg":"新增用户'xx'失败，登录账号已存在"}`

### 1.4 修改用户

`PUT /system/user` · 权限 `system:user:edit`

请求体同新增（含 `userId`；`password` 不通过此接口修改，请用重置密码）。返回同上。

### 1.5 删除用户

`DELETE /system/user/{userIds}` · 权限 `system:user:remove`

| 参数 | 说明 |
|---|---|
| `userIds` | 用户 ID，**多个用英文逗号分隔**，如 `/system/user/2,3` |

**响应**：`{"code":200,"msg":"操作成功"}`；不允许删除当前登录用户（`{"code":500,"msg":"当前用户不能删除"}`）

### 1.6 重置密码

`PUT /system/user/resetPwd` · 权限 `system:user:resetPwd`

```json
{ "userId": 2, "password": "newPwd123" }
```

**响应**：`{ "code": 200, "msg": "密码重置成功" }`

**备注**：重置成功后后端会**向该用户邮箱发送通知邮件**（标题「AweNovel 提示,您的密码已重置」，正文含新密码）。需配置邮箱，否则发送失败但不影响重置结果。

### 1.7 修改用户状态

`PUT /system/user/changeStatus` · 权限 `system:user:edit`

```json
{ "userId": 2, "status": "1" }
```

**响应**：`{"code":200,"msg":"操作成功"}`

### 1.8 查询用户已授权角色

`GET /system/user/authRole/{userId}` · 权限 `system:user:query`

```json
{
  "code": 200, "msg": "操作成功",
  "user": { "userId": 2, "userName": "ry", "nickName": "若依" },
  "roles": [ { "roleId": 1, "roleName": "超级管理员", "flag": false }, { "roleId": 2, "roleName": "普通角色", "flag": true } ]
}
```

> `roles[].flag = true` 表示该用户已拥有此角色。

### 1.9 用户授权角色

`PUT /system/user/authRole?userId=2&roleIds=1,2` · 权限 `system:user:edit`

> 参数为 **Query 参数**（`Long userId, Long[] roleIds`）

**响应**：`{"code":200,"msg":"操作成功"}`（该接口为**全量覆盖**：先清空再写入）

### 1.10 导出用户

`POST /system/user/export` · 权限 `system:user:export` · 返回 Excel 流（文件名「用户数据.xlsx」）

### 1.11 导入用户

`POST /system/user/importData` · 权限 `system:user:import` · `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file | 是 | Excel 文件 |
| `updateSupport` | boolean | 否 | 是否允许更新已存在用户 |

**响应**：`{"code":200,"msg":"恭喜您，数据已全部导入成功！共 X 条..."}`

### 1.12 下载导入模板

`POST /system/user/importTemplate` · 登录 · 返回 Excel 模板流

---

## 2. 角色管理 `/system/role`（13）

`SysRole` 字段：`roleId` / `roleName` / `roleKey` / `roleSort` / `menuCheckStrictly`(boolean) / `status`(0正常 1停用) / `delFlag` / `createBy` / `createTime` / `updateBy` / `updateTime` / `remark`；非表字段：`menuIds`(long[])、`permissions`(Set)、`flag`(boolean)

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 2.1 | `GET /system/role/list` | `system:role:list` | 分页查询。查询参数：`roleName`(模糊)、`roleKey`(模糊)、`status`、`params[beginTime]`、`params[endTime]` |
| 2.2 | `POST /system/role/export` | `system:role:export` | 导出 Excel |
| 2.3 | `GET /system/role/{roleId}` | `system:role:query` | 角色详情，返回 `{code,msg,data:SysRole}` |
| 2.4 | `POST /system/role` | `system:role:add` | 新增，body 含 `roleName`/`roleKey`/`roleSort`/`status`/`remark`/`menuIds`(菜单权限数组)。校验角色名与权限字符唯一 |
| 2.5 | `PUT /system/role` | `system:role:edit` | 修改（同上，含 `roleId`）。修改后会刷新当前登录用户权限缓存 |
| 2.6 | `PUT /system/role/changeStatus` | `system:role:edit` | body `{roleId, status}` |
| 2.7 | `DELETE /system/role/{roleIds}` | `system:role:remove` | 支持逗号分隔批量删除 |
| 2.8 | `GET /system/role/optionselect` | `system:role:query` | 全部角色下拉，返回 `{code,msg,data:[SysRole]}` |
| 2.9 | `GET /system/role/authUser/allocatedList` | `system:role:list` | 已分配该角色的用户分页列表。参数：`roleId`、`userName`、`phonenumber` |
| 2.10 | `GET /system/role/authUser/unallocatedList` | `system:role:list` | 未分配该角色的用户分页列表，参数同上 |
| 2.11 | `PUT /system/role/authUser/cancel` | `system:role:edit` | 取消单个用户授权，body `{roleId, userId}` |
| 2.12 | `PUT /system/role/authUser/cancelAll?roleId=1&userIds=2,3` | `system:role:edit` | 批量取消授权（Query 参数） |
| 2.13 | `PUT /system/role/authUser/selectAll?roleId=1&userIds=2,3` | `system:role:edit` | 批量添加授权（Query 参数） |

**角色列表响应示例**

```json
{
  "code": 200, "msg": "查询成功", "total": 2,
  "rows": [ { "roleId": 1, "roleName": "超级管理员", "roleKey": "admin", "roleSort": 1, "status": "0", "createTime": "2024-01-01 00:00:00" } ]
}
```

---

## 3. 菜单管理 `/system/menu`（7）

`SysMenu` 字段：`menuId` / `menuName` / `parentId` / `orderNum` / `path` / `component` / `query` / `isFrame`(0是外链 1否) / `isCache`(0缓存 1不缓存) / `menuType`(M目录 C菜单 F按钮) / `visible`(0显示 1隐藏) / `status`(0正常 1停用) / `perms` / `icon` / 审计字段 / `remark`；非表字段：`children`(array)、`parentName`

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 3.1 | `GET /system/menu/list` | `system:menu:list` | 菜单列表（**不分页**）。参数：`menuName`、`status`、`visible`；返回 `{code,msg,data:[SysMenu]}` |
| 3.2 | `GET /system/menu/{menuId}` | `system:menu:query` | 菜单详情 |
| 3.3 | `GET /system/menu/treeselect` | 登录 | 菜单下拉树，返回 `data:[{id,label,children}]` |
| 3.4 | `GET /system/menu/roleMenuTreeselect/{roleId}` | 登录 | 角色菜单树，返回 `{code,msg,checkedKeys:[menuId],menus:[树]}` |
| 3.5 | `POST /system/menu` | `system:menu:add` | 新增菜单 |
| 3.6 | `PUT /system/menu` | `system:menu:edit` | 修改菜单（上级菜单不能选自己） |
| 3.7 | `DELETE /system/menu/{menuId}` | `system:menu:remove` | 存在子菜单或已分配角色时拒绝，返回 `{"code":601,"msg":"存在子菜单,不允许删除"}` |

---

## 4. 字典管理

### 4.1 字典类型 `/system/dict/type`（8）

`SysDictType` 字段：`dictId` / `dictName` / `dictType` / `status` / 审计字段 / `remark`

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 4.1.1 | `GET /system/dict/type/list` | `system:dict:list` | 分页。参数：`dictName`(模糊)、`dictType`(模糊)、`status`、`params[beginTime]`、`params[endTime]` |
| 4.1.2 | `POST /system/dict/type/export` | `system:dict:export` | 导出 Excel |
| 4.1.3 | `GET /system/dict/type/{dictId}` | `system:dict:query` | 详情 |
| 4.1.4 | `POST /system/dict/type` | `system:dict:add` | 新增，`dictType` 唯一 |
| 4.1.5 | `PUT /system/dict/type` | `system:dict:edit` | 修改 |
| 4.1.6 | `DELETE /system/dict/type/{dictIds}` | `system:dict:remove` | 批量删除（逗号分隔） |
| 4.1.7 | `DELETE /system/dict/type/refreshCache` | `system:dict:remove` | 刷新字典缓存 |
| 4.1.8 | `GET /system/dict/type/optionselect` | 登录 | 全部字典类型下拉 |

### 4.2 字典数据 `/system/dict/data`（7）

`SysDictData` 字段：`dictCode` / `dictSort` / `dictLabel` / `dictValue` / `dictType` / `cssClass` / `listClass` / `isDefault`(Y/N) / `status` / 审计字段 / `remark`

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 4.2.1 | `GET /system/dict/data/list` | `system:dict:list` | 分页。参数：`dictType`(精确)、`dictLabel`(模糊)、`status` |
| 4.2.2 | `POST /system/dict/data/export` | `system:dict:export` | 导出 Excel |
| 4.2.3 | `GET /system/dict/data/{dictCode}` | `system:dict:query` | 详情 |
| 4.2.4 | `GET /system/dict/data/type/{dictType}` | 登录 | **按字典类型取字典项**，返回 `{code,msg,data:[SysDictData]}`。常用于下拉选项，无数据时返回空数组 |
| 4.2.5 | `POST /system/dict/data` | `system:dict:add` | 新增 |
| 4.2.6 | `PUT /system/dict/data` | `system:dict:edit` | 修改 |
| 4.2.7 | `DELETE /system/dict/data/{dictCodes}` | `system:dict:remove` | 批量删除 |

**内置字典类型**（初始化脚本已写入）：`sys_user_sex`、`sys_show_hide`、`sys_normal_disable`、`sys_yes_no`、`sys_notice_type`、`sys_notice_status`、`sys_oper_type`、`sys_common_status`

---

## 5. 参数配置 `/system/config`（8）

`SysConfig` 字段：`configId` / `configName` / `configKey` / `configValue` / `configType`(Y内置 N否) / 审计字段 / `remark`

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 5.1 | `GET /system/config/list` | `system:config:list` | 分页。参数：`configName`(模糊)、`configKey`(模糊)、`configType`、`params[beginTime]`、`params[endTime]` |
| 5.2 | `POST /system/config/export` | `system:config:export` | 导出 Excel |
| 5.3 | `GET /system/config/{configId}` | `system:config:query` | 详情 |
| 5.4 | `GET /system/config/configKey/{configKey}` | 登录 | 按键名取值，返回 `{code,msg,data:"值"}`（字符串） |
| 5.5 | `POST /system/config` | `system:config:add` | 新增，`configKey` 唯一 |
| 5.6 | `PUT /system/config` | `system:config:edit` | 修改（会刷新缓存） |
| 5.7 | `DELETE /system/config/{configIds}` | `system:config:remove` | 批量删除；**内置参数（`configType=Y`）不可删除** |
| 5.8 | `DELETE /system/config/refreshCache` | `system:config:remove` | 刷新参数缓存 |

**关键内置参数**（前端可能用到）

| configKey | 默认值 | 说明 |
|---|---|---|
| `sys.account.captchaEnabled` | `true` | 登录/注册验证码开关 |
| `sys.account.registerUser` | `true` | 注册开关 |
| `sys.account.enable.Email` | `true` | 邮箱验证码开关 |
| `sys.user.initPassword` | `123456` | 新增用户初始密码 |
| `sys.login.blackIPList` | 空 | 登录 IP 黑名单（逗号分隔） |
| `sys.index.skinName` / `sys.index.sideTheme` | `skin-blue` / `theme-dark` | 后台主题 |

---

## 6. 通知公告 `/system/notice`（5）

`SysNotice` 字段：`noticeId` / `noticeTitle` / `noticeType`(`1`通知 `2`公告) / `noticeContent` / `status`(`0`正常 `1`关闭) / 审计字段 / `remark`

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 6.1 | `GET /system/notice/list` | `system:notice:list` | 分页。参数：`noticeTitle`(模糊)、`noticeType`、`createBy`、`status` |
| 6.2 | `GET /system/notice/{noticeId}` | `system:notice:query` | 详情 |
| 6.3 | `POST /system/notice` | `system:notice:add` | 新增 |
| 6.4 | `PUT /system/notice` | `system:notice:edit` | 修改 |
| 6.5 | `DELETE /system/notice/{noticeIds}` | `system:notice:remove` | 批量删除 |

**备注**：`GET /system/notice/list` 是唯一在 XSS 过滤中排除的接口（`xss.excludes = /system/notice`），公告内容支持 HTML。

**响应示例**

```json
{
  "code": 200, "msg": "查询成功", "total": 1,
  "rows": [ { "noticeId": 1, "noticeTitle": "欢迎来到 AweNovel", "noticeType": "2", "noticeContent": "欢迎使用 AweNovel...", "status": "0", "createBy": "admin", "createTime": "2024-01-01 00:00:00" } ]
}
```

> 💡 公告目前**没有匿名可访问的接口**，用户端要展示站内公告需后端新增 `GET /community/notice/list`（或放开该接口的匿名访问）。
