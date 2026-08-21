# AweNovel 前端（pnpm workspace monorepo）

AweNovel 用户端 + 管理端 + 共享包，统一 **Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router**。

## 目录结构

```
web-ui/
├── package.json          # 根：pnpm -r / --filter 脚本
├── pnpm-workspace.yaml   # packages: shared / web / admin
├── .npmrc                # shamefully-hoist=true
├── shared/               # @gal/shared：业务类型 + axios 封装 + 全部后端接口函数
│   └── src/
│       ├── index.ts      # 统一出口
│       ├── types.ts      # AjaxResult / TableDataInfo / Game / Resource / Article / Comment / ...
│       ├── http.ts       # axios 实例（baseURL ''，Bearer token 拦截器，401 处理）
│       └── api.ts        # 认证/社区/审核/系统 全部接口 + SSE 流式聊天 streamChat
├── web/                  # @gal/web 用户端（深色 ACG 风格，端口 5173）
│   └── src/
│       ├── components/   # NavBar / Footer / ChatMaid(伊卡洛斯) / GameCard / ScoreBar / CommentSection
│       └── views/        # Home / GameList / GameDetail / ArticleList / ArticleDetail / Login / Register / UserCenter
└── admin/                # @gal/admin 管理端（端口 5174）
    └── src/
        ├── layout/       # AdminLayout（侧边栏菜单 + 顶栏）
        ├── utils/review.ts
        └── views/        # Login / Dashboard / Game / Brand / Tag / Resource / Article / Comment / Review / User
```

## 常用命令

```bash
pnpm install            # 安装全部 workspace 依赖
pnpm dev:web            # 启动用户端 http://localhost:5173
pnpm dev:admin          # 启动管理端 http://localhost:5174
pnpm build              # 依次构建 shared → web → admin（含 vue-tsc 类型检查）
pnpm typecheck          # 全量类型检查
```

## 后端联调

- 后端：Spring Boot @ `http://localhost:9090`，context-path 为 `/`。
- 两个应用的 `vite.config.ts` 均已将 `/login /register /captchaImage /getInfo /getRouters /logout /community /ai /system /monitor /profile` 代理到 9090；`axios` 的 `baseURL` 为 `''`。
- 登录 token 存 `localStorage['token']`，请求拦截器自动附加 `Authorization: Bearer <token>`。
- AI 看板娘走 `GET /ai/chat?message=...`（SSE），用 `fetch + ReadableStream` 手动解析 `data:` 行（EventSource 无法带 Authorization 头），每次对话消耗 1 积分。

## 验收

```bash
cd web-ui && pnpm install && pnpm build   # 应全部成功、无 TS 错误
```

## 需要与后端核对/确认的点

1. **会社/标签 CRUD 路径为约定值**：`GET/POST/PUT /community/brand(/list|/{ids})`、`GET/POST/PUT /community/tag(/list|/{ids})`，契约中未给出，若后端路径不同需调整 `shared/src/api.ts`。
2. **评论删除**：`DELETE /community/comment/{ids}` 为约定值。
3. **注册邮箱验证码**：`GET /register/code?email=` 为约定值（若依原版无此接口，可去掉发送按钮，手动填码）。
4. **游戏新增/编辑提交的标签**以 `tagIds: number[]` 形式提交；若后端期待 `tags: Tag[]` 需转换。
5. **资源/文章状态字段约定**：0=待审核、1=已通过/已发布、2=已拒绝；审核通过/拒绝统一走 `/community/review/approve|reject/{processInstanceId}`，资源/文章管理页通过 `findReviewTask(bizType, bizId)` 匹配任务。
6. **文章正文 `content`** 按 HTML 渲染（`v-html`），若后端返回 Markdown 需换渲染器。
7. **收藏/关注列表**返回结构以 `rows` 数组为准，前端已做兼容读取。
8. 未登录时 `/ai/chat`、评分、下载等需要登录的接口由后端 401 兜底；前端主要入口已做登录引导。
