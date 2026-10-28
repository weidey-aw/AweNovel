/**
 * 业务类型定义（与后端接口契约严格对齐）
 */

/* ---------------- 统一响应 ---------------- */

/** 普通接口统一响应：AjaxResult */
export interface AjaxResult<T = unknown> {
  code: number
  msg: string
  data?: T
}

/** 分页接口统一响应：TableDataInfo */
export interface TableDataInfo<T = unknown> {
  code: number
  msg: string
  total: number
  rows: T[]
}

/** 分页查询参数（pageNum / pageSize 从 1 开始） */
export interface PageParams {
  pageNum?: number
  pageSize?: number
}

/* ---------------- 认证 ---------------- */

export interface CaptchaResponse {
  code: number
  msg: string
  uuid: string
  /** base64 图片，使用时需加 data:image/gif;base64, 前缀 */
  img: string
}

export interface LoginParams {
  username: string
  password: string
  code: string
  uuid: string
}

export interface RegisterParams {
  username: string
  password: string
  nickname: string
  email: string
  code: string
}

export interface UserInfo {
  userId?: number
  userName?: string
  nickName?: string
  nickname?: string
  email?: string
  avatar?: string
  phonenumber?: string
  sex?: string
  status?: string
  createTime?: string
  remark?: string
}

/* ---------------- 社区 ---------------- */

export interface Tag {
  tagId: number
  name: string
  /** 标签类型：theme 题材 / play 玩法 / type 类型 */
  type?: string
  createTime?: string
  /** 关联游戏数（管理端列表统计） */
  gameCount?: number
}

export interface Brand {
  brandId: number
  name: string
  /** 官方网站（外链，可空） */
  website?: string
  description?: string
  logo?: string
  createTime?: string
  /** 关联游戏数（管理端列表统计） */
  gameCount?: number
}

export interface Game {
  gameId: number
  title: string
  titleCn?: string
  cover?: string
  /** 轮播图（JSON 数组字符串，最多 7 张） */
  carousel?: string
  releaseDate?: string
  /** 简介（Markdown） */
  summary?: string
  staffPaint?: string
  staffScenario?: string
  staffVoice?: string
  ratingAvg?: number
  ratingCount?: number
  viewCount?: number
  /** 资源总下载次数（后端按资源汇总） */
  downloadCount?: number
  brandId?: number
  brandName?: string
  tags?: Tag[]
  tagIds?: number[]
  /** 状态：0下架 1已发布 2待审核 3审核驳回 4草稿 */
  status?: string
  createTime?: string
  updateTime?: string
}

export interface Resource {
  resourceId: number
  gameId: number
  userId?: number
  title: string
  type: string
  url?: string
  version?: string
  size?: string
  extractPwd?: string
  checksum?: string
  points?: number
  downloadCount?: number
  /** 0=待审核 1=已通过 2=已拒绝 */
  status?: number
  createTime?: string
  /** 联表冗余字段 */
  userName?: string
  gameTitle?: string
}

export interface Article {
  articleId: number
  userId?: number
  title: string
  summary?: string
  content: string
  cover?: string
  category?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  /** 0=待审核 1=已发布 */
  status?: number
  createTime?: string
  userName?: string
  nickname?: string
}

export interface Comment {
  commentId: number
  /** 'game' | 'article' */
  targetType: string
  targetId: number
  userId?: number
  /** 父评论 id，0 或空为顶层评论 */
  pid?: number
  content: string
  likeCount?: number
  createTime?: string
  /** 联表冗余字段 */
  userName?: string
  nickname?: string
  avatar?: string
  replyTo?: string
  children?: Comment[]
}

export interface Rating {
  gameId: number
  /** 1-10 */
  score: number
  createTime?: string
}

export interface SignResult {
  continuous: number
  points: number
  exp: number
  level: number
  totalPoints: number
}

export interface UserProfile {
  userId: number
  nickname?: string
  avatar?: string
  points: number
  exp: number
  level: number
  signStreak: number
}

export interface PointLog {
  logId?: number
  changeType: string
  changeAmount: number
  balanceAfter: number
  remark?: string
  createTime?: string
}

export interface Message {
  messageId: number
  type?: string
  content: string
  isRead?: boolean
  createTime?: string
}

export interface Favorite {
  favoriteId?: number
  targetType?: string
  targetId?: number
  gameId?: number
  game?: Game
  createTime?: string
}

export interface FollowItem {
  followId?: number
  targetUserId: number
  nickname?: string
  avatar?: string
  createTime?: string
}

export interface ReviewTask {
  taskId: string
  processInstanceId: string
  taskName: string
  bizType: string
  bizId: number
  createTime?: string
}

/* ---------------- 系统管理（若依） ---------------- */

export interface SystemUser {
  userId: number
  userName?: string
  nickName?: string
  avatar?: string
  email?: string
  phonenumber?: string
  sex?: string
  status?: string
  delFlag?: string
  loginIp?: string
  loginDate?: string
  createTime?: string
  updateTime?: string
  remark?: string
  /** 角色ID数组（新增/修改时提交） */
  roleIds?: number[]
  /** 角色列表（详情返回，非表字段） */
  roles?: SysRole[]
}

/** 用户授权角色数据（GET /system/user/authRole/{userId}） */
export interface UserAuthRoleData {
  user: SystemUser
  roles: SysRole[]
}

/** 用户列表查询参数（分页参数之外的条件） */
export interface UserQuery extends PageParams {
  userName?: string
  phonenumber?: string
  status?: string
  /** 创建时间起止，由前端转换为 params[beginTime] / params[endTime] */
  beginTime?: string
  endTime?: string
}

/* ---------------- 查询参数 ---------------- */

export interface GameQuery extends PageParams {
  keyword?: string
  brandId?: number
  tagId?: number
  /** 标签多选（逗号分隔ID） */
  tagIds?: string
  /** 0下架 1已发布 2待审核 3审核驳回 4草稿 */
  status?: string
  /** 排序维度：hot 点击量 / download 下载量 / rating 评分 / publish 发布时间 / release 发售时间 */
  sortBy?: 'hot' | 'download' | 'rating' | 'publish' | 'release'
  /** 是否升序（默认降序） */
  isAsc?: boolean
  /** 发售日期区间 */
  beginDate?: string
  endDate?: string
}

export interface ArticleQuery extends PageParams {
  category?: string
  keyword?: string
}

export interface CommentQuery extends PageParams {
  targetType?: string
  targetId?: number
}

/* ---------------- 系统管理（后端返回结构对齐） ---------------- */

/** 角色 sys_role */
export interface SysRole {
  roleId: number
  roleName: string
  roleKey: string
  roleSort: number
  menuCheckStrictly?: boolean
  status?: string
  delFlag?: string
  createTime?: string
  remark?: string
  /** 是否已拥有该角色（授权场景） */
  flag?: boolean
  /** 菜单权限（提交用） */
  menuIds?: number[]
}

/** 菜单树节点（角色分配菜单用，由 /system/menu/treeselect 返回） */
export interface MenuTreeNode {
  id: number
  label: string
  children?: MenuTreeNode[]
}

/** 角色菜单树数据（/system/menu/roleMenuTreeselect/{roleId}） */
export interface RoleMenuTreeData {
  /** 已勾选的菜单ID */
  checkedKeys: number[]
  /** 完整菜单树 */
  menus: MenuTreeNode[]
}

/** 菜单 sys_menu */
export interface SysMenu {
  menuId: number
  menuName: string
  parentId: number
  orderNum: number
  path?: string
  component?: string
  query?: string
  isFrame?: string
  isCache?: string
  menuType?: string
  visible?: string
  status?: string
  perms?: string
  icon?: string
  createTime?: string
  remark?: string
  children?: SysMenu[]
}

/** 字典类型 sys_dict_type */
export interface SysDictType {
  dictId: number
  dictName: string
  dictType: string
  status?: string
  createTime?: string
  remark?: string
}

/** 字典数据 sys_dict_data */
export interface SysDictData {
  dictCode: number
  dictSort: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass?: string
  listClass?: string
  isDefault?: string
  status?: string
  createTime?: string
  remark?: string
}

/** 参数配置 sys_config */
export interface SysConfig {
  configId: number
  configName: string
  configKey: string
  configValue: string
  configType?: string
  createTime?: string
  remark?: string
}

/** 通知公告 sys_notice */
export interface SysNotice {
  noticeId: number
  noticeTitle: string
  noticeType: string
  noticeContent: string
  status?: string
  createBy?: string
  createTime?: string
  remark?: string
}

/** 操作日志 sys_oper_log */
export interface SysOperLog {
  operId: number
  title?: string
  businessType?: number
  method?: string
  requestMethod?: string
  operatorType?: number
  operName?: string
  operUrl?: string
  operIp?: string
  operLocation?: string
  operParam?: string
  jsonResult?: string
  status?: number
  errorMsg?: string
  operTime?: string
  costTime?: number
}

/** 登录日志 sys_logininfor */
export interface SysLogininfor {
  infoId: number
  userName?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  status?: string
  msg?: string
  loginTime?: string
}

/** 在线用户 sys_user_online */
export interface SysOnlineUser {
  sessionId: string
  loginName?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  status?: string
  startTimestamp?: string
  lastAccessTime?: string
  expireTime?: number
}

/** 缓存名称项 */
export interface CacheName {
  cacheName: string
  remark: string
}

/** 缓存内容项 */
export interface CacheValue {
  cacheName: string
  cacheKey: string
  cacheValue: string
  remark?: string
}

/** 缓存概览 */
export interface CacheInfo {
  info: Record<string, string>
  dbSize: number
  commandStats: Array<{ name: string; value: string }>
}

/** 服务器监控信息 */
export interface ServerInfo {
  cpu: { cpuNum: number; total: number; sys: number; used: number; wait: number; free: number }
  mem: { total: number; used: number; free: number; usage: number }
  jvm: {
    total: number
    max: number
    free: number
    version: string
    home: string
    name: string
    startTime: string
    runTime: string
    usage: number
    used: number
  }
  sys: { computerName: string; computerIp: string; userDir: string; osName: string; osArch: string }
  sysFiles: Array<{
    dirName: string
    sysTypeName: string
    typeName: string
    total: string
    free: string
    used: string
    usage: number
  }>
}

/* ---------------- 动态路由（/getRouters 返回结构） ---------------- */

export interface RouterMeta {
  title?: string
  icon?: string
  noCache?: boolean
  link?: string
}

export interface RouterVo {
  name?: string
  path: string
  hidden?: boolean
  redirect?: string
  component?: string
  query?: string
  alwaysShow?: boolean
  meta?: RouterMeta
  children?: RouterVo[]
}
