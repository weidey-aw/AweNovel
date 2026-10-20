import http, { getToken } from './http'
import type {
  AjaxResult,
  Article,
  ArticleQuery,
  Brand,
  CacheInfo,
  CacheName,
  CaptchaResponse,
  Comment,
  CommentQuery,
  Favorite,
  FollowItem,
  Game,
  GameQuery,
  LoginParams,
  MenuTreeNode,
  Message,
  PageParams,
  PointLog,
  Rating,
  RegisterParams,
  Resource,
  ReviewTask,
  RoleMenuTreeData,
  RouterVo,
  ServerInfo,
  SignResult,
  SysConfig,
  SysDictData,
  SysDictType,
  SysLogininfor,
  SysMenu,
  SysNotice,
  SysOnlineUser,
  SysOperLog,
  SysRole,
  SystemUser,
  TableDataInfo,
  Tag,
  UserInfo,
  UserProfile,
} from './types'

/** 兼容「后端直接返回业务对象」与「后端包一层 AjaxResult.data」两种情况 */
export function unwrap<T>(res: AjaxResult<T> | T): T {
  const r = res as AjaxResult<T>
  if (r && typeof r === 'object' && 'code' in r && 'data' in r && r.code === 200) {
    return r.data as T
  }
  return res as T
}

/* ================= 认证（沿用若依） ================= */

/** GET /captchaImage → { code, msg, uuid, img }（img 需加 data:image/gif;base64, 前缀） */
export const getCaptcha = () => http.get<CaptchaResponse>('/captchaImage')

/** POST /login → { code:200, msg, token }（token 在顶层，非 data 内） */
export const login = (data: LoginParams) =>
  http.post<{ code: number; msg: string; token: string }>('/login', data)

/** POST /register（邮箱验证码） */
export const register = (data: RegisterParams) => http.post<AjaxResult>('/register', data)

/** 发送邮箱注册验证码（若依无此接口时为管理端约定接口，失败可忽略提示） */
export const sendRegisterCode = (email: string) => http.get<AjaxResult>(`/register/code?email=${encodeURIComponent(email)}`)

/** POST /logout */
export const logout = () => http.post<AjaxResult>('/logout')

/** GET /getInfo → { code, user, roles, permissions } */
export const getInfo = () => http.get<AjaxResult<{ user: UserInfo; roles: string[]; permissions: string[] }>>('/getInfo')

/** GET /getRouters（管理端动态路由菜单树） */
export const getRouters = () => http.get<AjaxResult<RouterVo[]>>('/getRouters')

/** PUT /system/user/profile — 修改个人资料（昵称/邮箱/手机/性别） */
export const updateProfile = (data: Partial<UserInfo>) =>
  http.put<AjaxResult>('/system/user/profile', data)

/** PUT /system/user/profile/updatePwd — 修改密码 */
export const updatePassword = (oldPassword: string, newPassword: string) =>
  http.put<AjaxResult>('/system/user/profile/updatePwd', null, {
    params: { oldPassword, newPassword },
  })

/** POST /system/user/profile/avatar — 上传头像 */
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('avatarfile', file)
  return http.post<AjaxResult<{ imgUrl: string }>>('/system/user/profile/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** GET /forget/code?email= — 发送忘记密码邮箱验证码 */
export const sendForgetCode = (email: string) =>
  http.get<AjaxResult>(`/forget/code?email=${encodeURIComponent(email)}`)

/** PUT /forgetPwd — 通过邮箱验证码重置密码 */
export const forgetPassword = (email: string, code: string, password: string) =>
  http.put<AjaxResult>('/forgetPwd', {
    email,
    emailType: 'FORGET_PASS',
    code,
    password,
  })

/* ================= 游戏 ================= */

export const getGameList = (params: GameQuery) => http.get<TableDataInfo<Game>>('/community/game/list', { params })

export const getGameDetail = (gameId: number) => http.get<AjaxResult<Game>>(`/community/game/${gameId}`)

/** GET /community/game/tag/all → Tag[] */
export const getAllTags = () => http.get<AjaxResult<Tag[]> | Tag[]>('/community/game/tag/all')

export const createGame = (data: Partial<Game>) => http.post<AjaxResult>('/community/game', data)

export const updateGame = (data: Partial<Game>) => http.put<AjaxResult>('/community/game', data)

/** gameIds 为逗号分隔的字符串 */
export const deleteGames = (gameIds: string) => http.delete<AjaxResult>(`/community/game/${gameIds}`)

/* ================= 会社（管理端，接口路径为约定，需与后端核对） ================= */

export const getBrandList = (params?: PageParams) => http.get<TableDataInfo<Brand> | Brand[]>('/community/brand/list', { params })

export const createBrand = (data: Partial<Brand>) => http.post<AjaxResult>('/community/brand', data)

export const updateBrand = (data: Partial<Brand>) => http.put<AjaxResult>('/community/brand', data)

export const deleteBrands = (brandIds: string) => http.delete<AjaxResult>(`/community/brand/${brandIds}`)

/* ================= 标签（管理端） ================= */

export const getTagList = (params?: PageParams) => http.get<TableDataInfo<Tag> | Tag[]>('/community/tag/list', { params })

export const createTag = (data: Partial<Tag>) => http.post<AjaxResult>('/community/tag', data)

export const updateTag = (data: Partial<Tag>) => http.put<AjaxResult>('/community/tag', data)

export const deleteTags = (tagIds: string) => http.delete<AjaxResult>(`/community/tag/${tagIds}`)

/* ================= 资源 ================= */

export const getResourceList = (params: PageParams & { gameId?: number; status?: number }) =>
  http.get<TableDataInfo<Resource>>('/community/resource/list', { params })

/** 发布资源（登录） */
export const publishResource = (data: Partial<Resource>) => http.post<AjaxResult<Resource>>('/community/resource', data)

/** 下载资源（扣积分，返回含 url 的资源） */
export const downloadResource = (resourceId: number) => http.post<AjaxResult<Resource>>(`/community/resource/download/${resourceId}`)

/** 失效举报 */
export const reportResource = (resourceId: number) => http.post<AjaxResult>(`/community/resource/report/${resourceId}`)

/* ================= 文章 ================= */

export const getArticleList = (params: ArticleQuery) => http.get<TableDataInfo<Article>>('/community/article/list', { params })

export const getArticleDetail = (articleId: number) => http.get<AjaxResult<Article>>(`/community/article/${articleId}`)

/** 发布文章（登录） */
export const publishArticle = (data: Partial<Article>) => http.post<AjaxResult<Article>>('/community/article', data)

/* ================= 评论 ================= */

export const getCommentList = (params: CommentQuery) => http.get<TableDataInfo<Comment>>('/community/comment/list', { params })

/** 发表评论（登录） */
export const publishComment = (data: Partial<Comment>) => http.post<AjaxResult<Comment>>('/community/comment', data)

/** 删除评论（管理端，路径为约定，需与后端核对） */
export const deleteComments = (commentIds: string) => http.delete<AjaxResult>(`/community/comment/${commentIds}`)

/* ================= 评分 ================= */

/** GET /community/rating/{gameId} → 我的评分（登录） */
export const getMyRating = (gameId: number) => http.get<AjaxResult<Rating> | Rating>(`/community/rating/${gameId}`)

/** 评分 1-10（登录） */
export const rateGame = (data: { gameId: number; score: number }) => http.post<AjaxResult<Rating>>('/community/rating', data)

/* ================= 用户中心 ================= */

/** 每日签到（登录），返回 { continuous, points, exp, level, totalPoints } */
export const signDaily = () => http.post<AjaxResult<SignResult> | SignResult>('/community/sign')

/** 我的画像（登录） */
export const getUserProfile = () => http.get<AjaxResult<UserProfile> | UserProfile>('/community/user/profile')

/** 积分流水（登录） */
export const getPointLogs = (params: PageParams) => http.get<TableDataInfo<PointLog>>('/community/user/points', { params })

/** 我的消息（登录） */
export const getMessages = (params: PageParams) => http.get<TableDataInfo<Message>>('/community/user/messages', { params })

/** 未读消息数（登录） */
export const getUnreadCount = () => http.get<AjaxResult<number> | number>('/community/user/messages/unread')

/** 标记消息已读（登录） */
export const markMessageRead = (messageId: number) => http.post<AjaxResult>(`/community/user/message/read/${messageId}`)

/** 我的收藏（登录） */
export const getFavorites = (params: PageParams) => http.get<TableDataInfo<Favorite>>('/community/user/favorites', { params })

/** 我的关注（登录） */
export const getFollowing = (params: PageParams) => http.get<TableDataInfo<FollowItem>>('/community/user/following', { params })

/** 关注 / 取关（登录） */
export const toggleFollow = (targetUserId: number) => http.post<AjaxResult>(`/community/user/follow/${targetUserId}`)

/* ================= 内容审核（管理） ================= */

/** 待审核任务数组 [{ taskId, processInstanceId, taskName, bizType, bizId }] */
export const getReviewTasks = () => http.get<AjaxResult<ReviewTask[]> | ReviewTask[]>('/community/review/tasks')

export const approveReview = (processInstanceId: string) => http.post<AjaxResult>(`/community/review/approve/${processInstanceId}`)

export const rejectReview = (processInstanceId: string) => http.post<AjaxResult>(`/community/review/reject/${processInstanceId}`)

/* ================= 系统管理（若依） ================= */

export const getSystemUserList = (params: PageParams & { userName?: string }) =>
  http.get<TableDataInfo<SystemUser>>('/system/user/list', { params })

/* ================= AI 看板娘（SSE 流式） ================= */

export interface StreamChatOptions {
  message: string
  /** 每收到一段文本就回调一次 */
  onChunk: (text: string) => void
  signal?: AbortSignal
}

/** 解析单个 data: 负载（兼容纯文本与 { content } 等 JSON 形态） */
function parsePayload(payload: string): string {
  const trimmed = payload.trim()
  if (!trimmed.startsWith('{')) return trimmed
  try {
    const parsed: unknown = JSON.parse(trimmed)
    if (typeof parsed === 'string') return parsed
    if (parsed && typeof parsed === 'object') {
      const obj = parsed as Record<string, unknown>
      const val = obj.content ?? obj.text ?? obj.message ?? obj.delta ?? obj.data
      if (typeof val === 'string') return val
    }
  } catch {
    /* 不是 JSON，按纯文本处理 */
  }
  return trimmed
}

/**
 * AI 看板娘流式聊天：GET /ai/chat?message=xxx（text/event-stream，需登录，每次消耗 1 积分）。
 * EventSource 无法携带自定义 header，故用 fetch + ReadableStream 手动解析 SSE 的 data: 行。
 */
export async function streamChat(options: StreamChatOptions): Promise<string> {
  const { message, onChunk, signal } = options
  const token = getToken()
  const url = `/api/ai/chat?message=${encodeURIComponent(message)}`
  const resp = await fetch(url, {
    method: 'GET',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    signal,
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`AI 服务响应异常（${resp.status}）`)
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let full = ''

  const flush = (block: string) => {
    for (const line of block.split('\n')) {
      if (line.startsWith('data:')) {
        const payload = line.slice(5).trim()
        if (!payload || payload === '[DONE]') continue
        const text = parsePayload(payload)
        full += text
        onChunk(text)
      }
    }
  }

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''
    for (const block of blocks) flush(block)
  }
  if (buffer.trim()) flush(buffer)
  return full
}

/* ================= 系统管理：角色 /system/role ================= */

export const getRoleList = (params: Record<string, unknown>) =>
  http.get<TableDataInfo<SysRole>>('/system/role/list', { params })

export const getRoleDetail = (roleId: number) => http.get<AjaxResult<SysRole>>(`/system/role/${roleId}`)

export const getRoleOptions = () => http.get<AjaxResult<SysRole[]>>('/system/role/optionselect')

export const createRole = (data: Partial<SysRole>) => http.post<AjaxResult>('/system/role', data)

export const updateRole = (data: Partial<SysRole>) => http.put<AjaxResult>('/system/role', data)

export const deleteRoles = (roleIds: string) => http.delete<AjaxResult>(`/system/role/${roleIds}`)

export const changeRoleStatus = (roleId: number, status: string) =>
  http.put<AjaxResult>('/system/role/changeStatus', { roleId, status })

/** 导出角色（Excel 二进制） */
export const exportRoles = (params: Record<string, unknown>) =>
  http.post<Blob>('/system/role/export', null, { params, responseType: 'blob', timeout: 60000 })

/** 角色已分配用户列表 */
export const getRoleAllocatedUsers = (params: PageParams & { roleId: number; userName?: string; phonenumber?: string }) =>
  http.get<TableDataInfo<SystemUser>>('/system/role/authUser/allocatedList', { params })

/** 角色未分配用户列表 */
export const getRoleUnallocatedUsers = (
  params: PageParams & { roleId: number; userName?: string; phonenumber?: string },
) => http.get<TableDataInfo<SystemUser>>('/system/role/authUser/unallocatedList', { params })

/** 取消单个用户的角色授权 */
export const cancelRoleAuthUser = (roleId: number, userId: number) =>
  http.put<AjaxResult>('/system/role/authUser/cancel', { roleId, userId })

/** 批量取消用户角色授权 */
export const cancelRoleAuthUsers = (roleId: number, userIds: number[]) =>
  http.put<AjaxResult>('/system/role/authUser/cancelAll', null, {
    params: { roleId, userIds: userIds.join(',') },
  })

/** 批量选择用户授权角色 */
export const selectRoleAuthUsers = (roleId: number, userIds: number[]) =>
  http.put<AjaxResult>('/system/role/authUser/selectAll', null, {
    params: { roleId, userIds: userIds.join(',') },
  })

/* ================= 系统管理：菜单 /system/menu ================= */

export const getMenuList = (params?: { menuName?: string; status?: string }) =>
  http.get<AjaxResult<SysMenu[]>>('/system/menu/list', { params })

export const getMenuDetail = (menuId: number) => http.get<AjaxResult<SysMenu>>(`/system/menu/${menuId}`)

export const getMenuTreeselect = () => http.get<AjaxResult<MenuTreeNode[]>>('/system/menu/treeselect')

/** 角色菜单树：checkedKeys / menus 为顶层字段（非 data 包裹） */
export const getRoleMenuTreeselect = (roleId: number) =>
  http.get<{ code: number; msg: string; checkedKeys: number[]; menus: MenuTreeNode[] }>(
    `/system/menu/roleMenuTreeselect/${roleId}`,
  )

/* ================= 系统管理：字典 /system/dict ================= */

export const getDictTypeList = (params: PageParams & { dictName?: string; dictType?: string; status?: string }) =>
  http.get<TableDataInfo<SysDictType>>('/system/dict/type/list', { params })

export const getDictTypeOptions = () => http.get<AjaxResult<SysDictType[]>>('/system/dict/type/optionselect')

export const getDictDataList = (params: PageParams & { dictType?: string; dictLabel?: string; status?: string }) =>
  http.get<TableDataInfo<SysDictData>>('/system/dict/data/list', { params })

/** 按字典类型取字典项（下拉选项用） */
export const getDictDataByType = (dictType: string) =>
  http.get<AjaxResult<SysDictData[]>>(`/system/dict/data/type/${dictType}`)

/* ================= 系统管理：参数 /system/config ================= */

export const getConfigList = (params: PageParams & { configName?: string; configKey?: string; configType?: string }) =>
  http.get<TableDataInfo<SysConfig>>('/system/config/list', { params })

export const refreshConfigCache = () => http.delete<AjaxResult>('/system/config/refreshCache')

/* ================= 系统管理：公告 /system/notice ================= */

export const getNoticeList = (
  params: PageParams & { noticeTitle?: string; noticeType?: string; createBy?: string; status?: string },
) => http.get<TableDataInfo<SysNotice>>('/system/notice/list', { params })

export const getNoticeDetail = (noticeId: number) => http.get<AjaxResult<SysNotice>>(`/system/notice/${noticeId}`)

/* ================= 系统监控 /monitor ================= */

export const getOperlogList = (
  params: PageParams & { title?: string; operName?: string; status?: number; businessType?: number },
) => http.get<TableDataInfo<SysOperLog>>('/monitor/operlog/list', { params })

export const getLogininforList = (params: PageParams & { userName?: string; ipaddr?: string; status?: string }) =>
  http.get<TableDataInfo<SysLogininfor>>('/monitor/logininfor/list', { params })

export const unlockUser = (userName: string) =>
  http.get<AjaxResult>(`/monitor/logininfor/unlock/${encodeURIComponent(userName)}`)

export const getOnlineUsers = (params: PageParams & { ipaddr?: string; userName?: string }) =>
  http.get<TableDataInfo<SysOnlineUser>>('/monitor/online/list', { params })

/** 强退在线用户（tokenId 即列表中的 sessionId） */
export const forceLogout = (tokenId: string) => http.delete<AjaxResult>(`/monitor/online/${tokenId}`)

export const getCacheInfo = () => http.get<AjaxResult<CacheInfo>>('/monitor/cache')

export const getCacheNames = () => http.get<AjaxResult<CacheName[]>>('/monitor/cache/getNames')

export const getServerInfo = () => http.get<AjaxResult<ServerInfo>>('/monitor/server')

/* ================= 通用：文件上传 ================= */

/** 单文件上传，返回 { url 完整地址, fileName 相对路径 } */
export const uploadFile = (file: File, url = '/common/upload', field = 'file') => {
  const formData = new FormData()
  formData.append(field, file)
  return http.post<{
    code: number
    msg: string
    url: string
    fileName: string
    newFileName: string
    originalFilename: string
  }>(url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

/* ================= 用户管理 /system/user（管理端完整能力） ================= */

/** 用户分页列表（支持 params[beginTime] / params[endTime] 时间区间） */
export const getUserList = (params: Record<string, unknown>) =>
  http.get<TableDataInfo<SystemUser>>('/system/user/list', { params })

/** 用户详情 + 可选角色 + 已分配角色ID */
export const getUserDetail = (userId: number) =>
  http.get<{ code: number; msg: string; data?: SystemUser; roles?: SysRole[]; roleIds?: number[] }>(
    `/system/user/${userId}`,
  )

/** 新增用户（roleIds 用于同时分配角色） */
export const createUser = (data: Partial<SystemUser> & { password?: string }) =>
  http.post<AjaxResult>('/system/user', data)

/** 修改用户 */
export const updateUser = (data: Partial<SystemUser>) => http.put<AjaxResult>('/system/user', data)

/** 删除用户（多个 ID 逗号分隔） */
export const deleteUsers = (userIds: string) => http.delete<AjaxResult>(`/system/user/${userIds}`)

/**
 * 系统自动重置密码：后端生成 8 位数字+字母随机密码，并通过邮件通知用户。
 * 权限：system:user:resetPwd
 */
export const resetUserPwdAuto = (userId: number) =>
  http.put<AjaxResult>('/system/user/resetPwd', { userId })

/**
 * 管理员手动重置密码：指定新密码，并邮件通知用户。
 * 权限：system:user:resetPwdManual（独立权限）
 */
export const resetUserPwdManual = (userId: number, password: string) =>
  http.put<AjaxResult>('/system/user/resetPwdManual', { userId, password })

/** 修改用户状态（0 正常 / 1 停用） */
export const changeUserStatus = (userId: number, status: string) =>
  http.put<AjaxResult>('/system/user/changeStatus', { userId, status })

/** 查询用户已授权角色（roles[].flag = true 表示已拥有） */
export const getUserAuthRole = (userId: number) =>
  http.get<{ code: number; msg: string; user?: SystemUser; roles?: SysRole[] }>(
    `/system/user/authRole/${userId}`,
  )

/** 保存用户授权角色（全量覆盖） */
export const setUserAuthRole = (userId: number, roleIds: number[]) =>
  http.put<AjaxResult>('/system/user/authRole', null, {
    params: { userId, roleIds: roleIds.join(',') },
  })

/** 修改指定用户头像（管理端），field 固定 avatarfile */
export const uploadUserAvatar = (userId: number, file: File) => {
  const formData = new FormData()
  formData.append('avatarfile', file)
  return http.post<{ code: number; msg: string; imgUrl?: string }>(`/system/user/avatar?userId=${userId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 导出用户（Excel 二进制），params 与列表查询条件一致 */
export const exportUsers = (params: Record<string, unknown>) =>
  http.post<Blob>('/system/user/export', null, { params, responseType: 'blob', timeout: 60000 })

/** 导入用户（Excel） */
export const importUsers = (file: File, updateSupport: boolean) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('updateSupport', String(updateSupport))
  return http.post<AjaxResult>('/system/user/importData', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

/** 下载用户导入模板（Excel 二进制） */
export const downloadUserTemplate = () =>
  http.post<Blob>('/system/user/importTemplate', null, { responseType: 'blob', timeout: 60000 })
