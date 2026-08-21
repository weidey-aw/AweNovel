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
  /** 0=风格 1=题材 2=其它 */
  type: number
}

export interface Brand {
  brandId: number
  name: string
  nameCn?: string
  country?: string
  description?: string
  logo?: string
  website?: string
  createTime?: string
}

export interface Game {
  gameId: number
  title: string
  titleCn?: string
  cover?: string
  releaseDate?: string
  summary?: string
  staffPaint?: string
  staffScenario?: string
  staffVoice?: string
  ratingAvg?: number
  ratingCount?: number
  viewCount?: number
  brandId?: number
  brandName?: string
  tags?: Tag[]
  status?: number
  createTime?: string
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
  createTime?: string
  remark?: string
}

/* ---------------- 查询参数 ---------------- */

export interface GameQuery extends PageParams {
  keyword?: string
  brandId?: number
  tagId?: number
}

export interface ArticleQuery extends PageParams {
  category?: string
  keyword?: string
}

export interface CommentQuery extends PageParams {
  targetType?: string
  targetId?: number
}
