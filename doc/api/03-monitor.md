# 03 · 系统监控接口

> 共 19 个接口。响应结构、鉴权、分页约定见 [README.md](README.md#1-通用约定)。
> ⚠️ 本模块**前端全部未封装**。

---

## 1. 登录日志 `/monitor/logininfor`（5）

`SysLogininfor` 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `infoId` | long | 日志ID |
| `userName` | string | 用户账号 |
| `ipaddr` | string | 登录IP |
| `loginLocation` | string | 登录地点（内网显示「内网IP」） |
| `browser` | string | 浏览器 |
| `os` | string | 操作系统 |
| `status` | string | `0`成功 `1`失败 |
| `msg` | string | 提示消息（如「登录成功」「用户不存在/密码错误」） |
| `loginTime` | string | 访问时间 |

### 1.1 登录日志列表

`GET /monitor/logininfor/list` · 权限 `monitor:logininfor:list`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userName` | string | 否 | 账号，模糊匹配 |
| `ipaddr` | string | 否 | IP，模糊匹配 |
| `status` | string | 否 | `0`成功 / `1`失败 |
| `params[beginTime]` / `params[endTime]` | string | 否 | 按 `loginTime` 区间 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**

```json
{
  "code": 200, "msg": "查询成功", "total": 186,
  "rows": [
    {
      "infoId": 186, "userName": "admin", "ipaddr": "127.0.0.1", "loginLocation": "内网IP",
      "browser": "Chrome 12", "os": "Windows 10", "status": "0",
      "msg": "登录成功", "loginTime": "2024-01-01 12:00:00"
    }
  ]
}
```

> 排序：按 `infoId` 倒序。

### 1.2 ~ 1.5 其他接口

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 1.2 | `POST /monitor/logininfor/export` | `monitor:logininfor:export` | 导出 Excel（文件名「登录日志.xlsx」），查询条件同列表 |
| 1.3 | `DELETE /monitor/logininfor/{infoIds}` | `monitor:logininfor:remove` | 批量删除（逗号分隔） |
| 1.4 | `DELETE /monitor/logininfor/clean` | `monitor:logininfor:remove` | 清空全部登录日志 |
| 1.5 | `GET /monitor/logininfor/unlock/{userName}` | `monitor:logininfor:unlock` | **账户解锁**：清除该账号的密码错误次数缓存（被锁定 10 分钟的账号用此接口解封） |

---

## 2. 操作日志 `/monitor/operlog`（4）

`SysOperLog` 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `operId` | long | 日志ID |
| `title` | string | 操作模块（如「用户管理」） |
| `businessType` | int | `0`其它 `1`新增 `2`修改 `3`删除 `4`授权 `5`导出 `6`导入 `7`强退 `8`生成代码 `9`清空数据 |
| `method` | string | 请求方法（类名.方法名） |
| `requestMethod` | string | 请求方式（GET/POST/PUT/DELETE） |
| `operatorType` | int | `0`其它 `1`后台用户 `2`手机端用户 |
| `operName` | string | 操作人员 |
| `deptName` | string | 部门名称（本项目已移除部门，恒为空） |
| `operUrl` | string | 请求地址 |
| `operIp` | string | 操作IP |
| `operLocation` | string | 操作地点 |
| `operParam` | string | 请求参数（JSON 字符串，最长 2000） |
| `jsonResult` | string | 返回参数（最长 2000） |
| `status` | int | `0`正常 `1`异常 |
| `errorMsg` | string | 错误消息 |
| `operTime` | string | 操作时间（`yyyy-MM-dd HH:mm:ss`） |
| `costTime` | long | 耗时（毫秒） |

### 2.1 操作日志列表

`GET /monitor/operlog/list` · 权限 `monitor:operlog:list`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | string | 否 | 操作模块，模糊匹配 |
| `operName` | string | 否 | 操作人员，模糊匹配 |
| `businessTypes` | int[] | 否 | 业务类型数组，如 `businessTypes=1&businessTypes=2` |
| `status` | int | 否 | `0`正常 / `1`异常 |
| `params[beginTime]` / `params[endTime]` | string | 否 | 按 `operTime` 区间 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**：`TableDataInfo<SysOperLog>`，按 `operId` 倒序。

```json
{
  "code": 200, "msg": "查询成功", "total": 117,
  "rows": [
    {
      "operId": 117, "title": "用户管理", "businessType": 1, "method": "com.weidey.web.controller.system.SysUserController.add()",
      "requestMethod": "POST", "operatorType": 1, "operName": "admin", "operUrl": "/system/user",
      "operIp": "127.0.0.1", "operLocation": "内网IP", "operParam": "{\"userName\":\"test\"}",
      "jsonResult": "{\"msg\":\"操作成功\",\"code\":200}", "status": 0, "errorMsg": "",
      "operTime": "2024-01-01 12:00:00", "costTime": 45
    }
  ]
}
```

### 2.2 ~ 2.4 其他接口

| # | 接口 | 权限 | 说明 |
|---|---|---|---|
| 2.2 | `POST /monitor/operlog/export` | `monitor:operlog:export` | 导出 Excel（「操作日志.xlsx」） |
| 2.3 | `DELETE /monitor/operlog/{operIds}` | `monitor:operlog:remove` | 批量删除（逗号分隔） |
| 2.4 | `DELETE /monitor/operlog/clean` | `monitor:operlog:remove` | 清空全部操作日志 |

---

## 3. 在线用户 `/monitor/online`（2）

`SysUserOnline` 字段：`sessionId`(即 tokenId) / `loginName` / `deptName`(恒空) / `ipaddr` / `loginLocation` / `browser` / `os` / `status`(`on_line`/`off_line`) / `startTimestamp` / `lastAccessTime` / `expireTime`(分钟)

### 3.1 在线用户列表

`GET /monitor/online/list` · 权限 `monitor:online:list`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `ipaddr` | string | 否 | 按 IP 精确筛选 |
| `userName` | string | 否 | 按账号精确筛选 |
| `pageNum` / `pageSize` | int | 否 | 分页 |

**响应**

```json
{
  "code": 200, "msg": "查询成功", "total": 1,
  "rows": [
    {
      "sessionId": "a1b2c3d4e5f6...", "loginName": "admin", "deptName": "",
      "ipaddr": "127.0.0.1", "loginLocation": "内网IP", "browser": "Chrome 12", "os": "Windows 10",
      "status": "on_line", "startTimestamp": "2024-01-01 12:00:00",
      "lastAccessTime": "2024-01-01 12:10:00", "expireTime": 30
    }
  ]
}
```

> 数据来源为 Redis 中的登录态（key `login_tokens:*`），按登录时间倒序。

### 3.2 强退用户

`DELETE /monitor/online/{tokenId}` · 权限 `monitor:online:forceLogout`

| 参数 | 说明 |
|---|---|
| `tokenId` | 即列表中的 `sessionId` |

**响应**：`{"code":200,"msg":"操作成功"}`（删除 Redis 登录态，用户下一次请求将 401）

---

## 4. 缓存监控 `/monitor/cache`（7）

> 权限均为 `monitor:cache:list`。注意：**无子路径的 `GET /monitor/cache`** 是缓存概览。

| # | 接口 | 说明 |
|---|---|---|
| 4.1 | `GET /monitor/cache` | Redis 概览，返回 `{code,msg,data:{info:{...},dbSize:123,commandStats:[{name,value}]}}` |
| 4.2 | `GET /monitor/cache/getNames` | 缓存分类列表，返回 `data:[{cacheName,remark}]`（如 `login_tokens:`→用户信息、`sys_config:`→配置信息、`sys_dict:`→数据字典、`captcha_codes:`→验证码、`repeat_submit:`→防重提交、`rate_limit:`→限流处理、`pwd_err_cnt:`→密码错误次数） |
| 4.3 | `GET /monitor/cache/getKeys/{cacheName}` | 按缓存名前缀取 key 列表，返回 `data:["sys_config:sys.account.captchaEnabled", ...]` |
| 4.4 | `GET /monitor/cache/getValue/{cacheName}/{cacheKey}` | 取单个缓存值，返回 `data:{cacheName,cacheKey,cacheValue,remark}` |
| 4.5 | `DELETE /monitor/cache/clearCacheName/{cacheName}` | 按缓存名前缀清除（如清空全部验证码） |
| 4.6 | `DELETE /monitor/cache/clearCacheKey/{cacheKey}` | 清除指定 key |
| 4.7 | `DELETE /monitor/cache/clearCacheAll` | 清空全部缓存（**包含所有登录态，慎用**） |

**`GET /monitor/cache` 响应示例**

```json
{
  "code": 200, "msg": "操作成功",
  "data": {
    "info": { "redis_version": "7.0.0", "used_memory_human": "1.2M", "connected_clients": "3" },
    "dbSize": 42,
    "commandStats": [ { "name": "get", "value": "120" }, { "name": "set", "value": "35" } ]
  }
}
```

---

## 5. 服务器监控 `/monitor/server`（1）

`GET /monitor/server` · 权限 `monitor:server:list`

**响应**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "cpu": { "cpuNum": 8, "total": 100.0, "sys": 12.3, "used": 25.6, "wait": 0.0, "free": 62.1 },
    "mem": { "total": 16.0, "used": 7.5, "free": 8.5, "usage": 46.9 },
    "jvm": {
      "total": 512.0, "max": 1024.0, "free": 300.0, "version": "17.0.9", "home": "/usr/lib/jvm/java-17",
      "name": "OpenJDK 64-Bit Server VM", "startTime": "2024-01-01 12:00:00", "runTime": "1天2小时3分钟",
      "usage": 41.4, "used": 212.0
    },
    "sys": {
      "computerName": "server", "computerIp": "127.0.0.1", "userDir": "/opt/awenovel",
      "osName": "Linux", "osArch": "amd64"
    },
    "sysFiles": [
      { "dirName": "/", "sysTypeName": "ext4", "typeName": "本地固定磁盘 (/)",
        "total": "100.0 GB", "free": "40.0 GB", "used": "60.0 GB", "usage": 60.0 }
    ]
  }
}
```

| 字段 | 说明 |
|---|---|
| `cpu` | CPU 核数、使用率（`total`/`sys`/`used`/`wait`/`free`，单位 %） |
| `mem` | 物理内存，单位 GB + `usage` 百分比 |
| `jvm` | JVM 内存（MB）、版本、启动时间、运行时长 |
| `sys` | 主机名、IP、项目路径、操作系统 |
| `sysFiles` | 各磁盘挂载点容量（字符串格式，含单位） |

> ⚠️ 该接口采集时 CPU 采样会**阻塞约 1 秒**（OSHI `OSHI_WAIT_SECOND`），前端避免高频轮询。
