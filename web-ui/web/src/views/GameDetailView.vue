<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Warning, ArrowLeft } from '@element-plus/icons-vue'
import {
  downloadResource,
  getGameDetail,
  getMyRating,
  getResourceList,
  rateGame,
  renderMarkdown,
  reportResource,
  resolveAssetUrl,
} from '@gal/shared'
import type { Game, Resource } from '@gal/shared'
import { useUserStore } from '@/stores/user'
import ScoreBar from '@/components/ScoreBar.vue'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const gameId = computed(() => Number(route.params.id))

const game = ref<Game | null>(null)
const loading = ref(true)
const resources = ref<Resource[]>([])
const resLoading = ref(false)
const myScore = ref(0)
const ratingLoading = ref(false)
const downloadTarget = ref<Resource | null>(null)
const downloadVisible = ref(false)
const downloadUrl = ref('')
const downloadPwd = ref('')

/** 游戏截图（carousel 字段为 JSON 数组字符串；0 或 3-7 张） */
const screenshotList = computed<string[]>(() => {
  const raw = game.value?.carousel
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    const list = Array.isArray(arr) ? arr.filter((item): item is string => typeof item === 'string') : []
    return list.map((item) => resolveAssetUrl(item))
  } catch {
    return []
  }
})

/** 每 3 张一屏（左右箭头翻页，不自动滚动） */
const screenshotSlides = computed<string[][]>(() => {
  const slides: string[][] = []
  for (let i = 0; i < screenshotList.value.length; i += 3) {
    slides.push(screenshotList.value.slice(i, i + 3))
  }
  return slides
})

/** 封面图 */
const coverSrc = computed(() => resolveAssetUrl(game.value?.cover))

/** 简介按 Markdown 渲染（支持图片 / 视频） */
const summaryHtml = computed(() => renderMarkdown(game.value?.summary))

async function load() {
  loading.value = true
  try {
    const res = await getGameDetail(gameId.value)
    game.value = res.data ?? null
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '游戏详情加载失败')
  } finally {
    loading.value = false
  }
}

async function loadResources() {
  resLoading.value = true
  try {
    const res = await getResourceList({ pageNum: 1, pageSize: 100, gameId: gameId.value })
    resources.value = (res.rows ?? []).filter((r) => r.status === undefined || r.status === 1)
  } catch {
    resources.value = []
  } finally {
    resLoading.value = false
  }
}

async function loadMyRating() {
  if (!userStore.isLoggedIn) return
  try {
    const res = (await getMyRating(gameId.value)) as unknown as { data?: { score: number } } | { score: number }
    const r = ('data' in res && res.data ? res.data : res) as { score: number }
    myScore.value = Number(r.score) || 0
  } catch {
    myScore.value = 0
  }
}

async function submitRating(score: number) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再评分')
    return
  }
  ratingLoading.value = true
  try {
    await rateGame({ gameId: gameId.value, score })
    myScore.value = score
    ElMessage.success(`已评分：${score} 分`)
    await load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '评分失败')
  } finally {
    ratingLoading.value = false
  }
}

async function doDownload(r: Resource) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再下载资源')
    return
  }
  try {
    const res = await downloadResource(r.resourceId)
    const data = (res as { data?: Resource }).data ?? r
    downloadTarget.value = r
    downloadVisible.value = true
    downloadUrl.value = resolveAssetUrl(data.url)
    downloadPwd.value = data.extractPwd || ''
    await loadResources()
    ElMessage.success(`下载成功，已扣除 ${data.points ?? 0} 积分`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '下载失败')
  }
}

async function doReport(r: Resource) {
  try {
    await ElMessageBox.confirm('确认举报该资源已失效？', '失效举报', { type: 'warning' })
    await reportResource(r.resourceId)
    ElMessage.success('举报成功，感谢反馈')
  } catch {
    /* 取消 */
  }
}

function openUrl(url: string) {
  window.open(url, '_blank')
}

onMounted(() => {
  load()
  loadResources()
  loadMyRating()
})
</script>

<template>
  <div class="page-container">
    <el-button text :icon="ArrowLeft" class="back-btn" @click="router.back()">返回</el-button>

    <el-skeleton v-if="loading" :rows="8" animated />

    <div v-else-if="game" class="detail">
      <div class="detail-main panel-card">
        <div class="detail-left">
          <!-- 封面图（游戏截图在简介下方单独展示） -->
          <div class="detail-cover">
            <el-image
              v-if="coverSrc"
              :src="coverSrc"
              fit="cover"
              :preview-src-list="[coverSrc]"
              preview-teleported
              class="cover-img"
            />
            <div v-else class="cover-fallback">🎮</div>
          </div>
        </div>

        <div class="detail-right">
          <h1 class="detail-title">{{ game.titleCn || game.title }}</h1>
          <div v-if="game.titleCn && game.titleCn !== game.title" class="detail-sub">{{ game.title }}</div>

          <div class="detail-meta">
            <div class="meta-item">
              <span class="meta-label">会社</span>
              <span class="meta-value">{{ game.brandName || '未知' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">发售日</span>
              <span class="meta-value">{{ game.releaseDate ? game.releaseDate.slice(0, 10) : '未知' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">浏览</span>
              <span class="meta-value">{{ game.viewCount || 0 }}</span>
            </div>
          </div>

          <div class="detail-tags">
            <span v-for="t in game.tags || []" :key="t.tagId" class="dtag">{{ t.name }}</span>
          </div>

          <!-- 评分 -->
          <div class="detail-rating panel-card">
            <div class="rating-head">
              <span class="rating-label">我的评分</span>
              <span class="rating-tip">1-10 分，点击星星即可评分</span>
            </div>
            <el-rate
              :model-value="myScore"
              :max="10"
              :disabled="ratingLoading"
              show-score
              :score-template="'{value} 分'"
              class="rating-rate"
              @change="submitRating"
            />
            <div class="rating-stats">
              <ScoreBar :value="Number(game.ratingAvg)" :count="game.ratingCount" />
            </div>
          </div>
        </div>
      </div>

      <!-- 简介 / 制作阵容 -->
      <div class="detail-sections">
        <div class="section-title">游戏简介</div>
        <div class="panel-card summary md-body">
          <div v-if="summaryHtml" v-html="summaryHtml" />
          <p v-else class="text-dim">暂无简介</p>
        </div>

        <!-- 游戏截图：无图则整块隐藏；一屏 3 张，左右箭头切换，不自动滚动 -->
        <template v-if="screenshotSlides.length">
          <div class="section-title">游戏截图</div>
          <div class="panel-card shot-panel">
            <el-carousel
              height="210px"
              :autoplay="false"
              :loop="screenshotSlides.length > 1"
              arrow="always"
              indicator-position="none"
            >
              <el-carousel-item v-for="(slide, slideIndex) in screenshotSlides" :key="slideIndex">
                <div class="shot-row">
                  <el-image
                    v-for="(img, imgIndex) in slide"
                    :key="`${img}-${imgIndex}`"
                    :src="img"
                    fit="cover"
                    class="shot-img"
                    :preview-src-list="screenshotList"
                    :initial-index="slideIndex * 3 + imgIndex"
                    preview-teleported
                  />
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>
        </template>

        <div v-if="game.staffPaint || game.staffScenario || game.staffVoice" class="section-title">
          制作阵容
        </div>
        <div v-if="game.staffPaint || game.staffScenario || game.staffVoice" class="staff panel-card">
          <div v-if="game.staffPaint" class="staff-item">
            <span class="staff-role">原画</span>
            <span>{{ game.staffPaint }}</span>
          </div>
          <div v-if="game.staffScenario" class="staff-item">
            <span class="staff-role">剧本</span>
            <span>{{ game.staffScenario }}</span>
          </div>
          <div v-if="game.staffVoice" class="staff-item">
            <span class="staff-role">声优</span>
            <span>{{ game.staffVoice }}</span>
          </div>
        </div>
      </div>

      <!-- 资源列表 -->
      <div class="section-title">资源分享</div>
      <div class="panel-card">
        <el-skeleton v-if="resLoading" :rows="3" animated />
        <el-empty v-else-if="!resources.length" description="暂无资源" :image-size="80" />
        <el-table v-else :data="resources" class="res-table">
          <el-table-column prop="title" label="资源名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="90" />
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column prop="size" label="大小" width="100" />
          <el-table-column prop="points" label="所需积分" width="90">
            <template #default="{ row }">
              <span class="points">{{ row.points ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="downloadCount" label="下载" width="80" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Download" @click="doDownload(row)">下载</el-button>
              <el-button size="small" text :icon="Warning" @click="doReport(row)">举报</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 评论区 -->
      <CommentSection :target-type="'game'" :target-id="gameId" />
    </div>

    <el-empty v-else description="游戏不存在或已被删除" />
  </div>

  <!-- 下载结果弹窗 -->
  <el-dialog v-model="downloadVisible" title="下载资源" width="460px" @closed="downloadTarget = null">
    <div v-if="downloadTarget" class="dl-dialog">
      <div class="dl-title">{{ downloadTarget.title }}</div>
      <el-alert
        v-if="downloadPwd"
        title="该资源需要提取码"
        type="warning"
        :closable="false"
        show-icon
        class="dl-alert"
      />
      <div class="dl-row">
        <span class="dl-label">提取码</span>
        <el-tag v-if="downloadPwd" type="warning">{{ downloadPwd }}</el-tag>
        <span v-else class="text-dim">无</span>
      </div>
      <div class="dl-row">
        <span class="dl-label">下载地址</span>
        <el-input :model-value="downloadUrl" readonly>
          <template #append>
            <el-button @click="openUrl(downloadUrl)">打开</el-button>
          </template>
        </el-input>
      </div>
      <p class="dl-tip text-dim">积分已扣除，请尽快保存资源。若链接失效可点击「举报」告知我们。</p>
    </div>
  </el-dialog>
</template>

<style scoped>
.back-btn {
  margin-bottom: 10px;
}

.detail-main {
  display: flex;
  gap: 28px;
  padding: 24px;
}

.detail-left {
  flex-shrink: 0;
}

.detail-cover {
  width: 260px;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 3 / 4;
  background: linear-gradient(160deg, #23233a, #14141f);
  box-shadow: var(--shadow-card);
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  opacity: 0.5;
}

.detail-right {
  flex: 1;
  min-width: 0;
}

.detail-title {
  font-size: 26px;
  margin: 0 0 6px;
}

.detail-sub {
  color: var(--text-dim);
  margin-bottom: 16px;
}

.detail-meta {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: var(--text-dim);
}

.meta-value {
  font-size: 14px;
}

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.dtag {
  font-size: 12.5px;
  color: var(--accent);
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.3);
  padding: 4px 12px;
  border-radius: 999px;
}

.detail-rating {
  padding: 16px 18px;
}

.rating-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
}

.rating-label {
  font-weight: 700;
}

.rating-tip {
  font-size: 12px;
  color: var(--text-dim);
}

.rating-rate {
  --el-rate-fill-color: #f59e0b;
}

.rating-stats {
  margin-top: 10px;
}

.detail-sections {
  margin-top: 8px;
}

.summary {
  white-space: pre-wrap;
  line-height: 1.9;
  color: var(--text-sub);
}

/* ---------- 封面 / 游戏截图 ---------- */
.cover-img {
  width: 100%;
  height: 100%;
  display: block;
  cursor: zoom-in;
}

.shot-panel {
  padding: 12px;
}

.shot-row {
  display: flex;
  gap: 10px;
  height: 100%;
  align-items: stretch;
  justify-content: center;
}

.shot-img {
  flex: 1;
  min-width: 0;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  cursor: zoom-in;
  background: rgba(0, 0, 0, 0.25);
}

.shot-img :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ---------- Markdown 简介 ---------- */
.md-body {
  white-space: normal;
  line-height: 1.9;
  color: var(--text-sub);
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  margin: 18px 0 10px;
  color: var(--text-main, #f5f5ff);
  font-size: 17px;
}

.md-body :deep(p) {
  margin: 8px 0;
}

.md-body :deep(ul),
.md-body :deep(ol) {
  padding-left: 22px;
  margin: 8px 0;
}

.md-body :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-left: 3px solid var(--accent);
  background: rgba(139, 92, 246, 0.08);
  border-radius: 6px;
}

.md-body :deep(.md-img) {
  max-width: 100%;
  border-radius: 10px;
  display: block;
  margin: 12px 0;
}

.md-body :deep(.md-video) {
  width: 100%;
  max-height: 520px;
  border-radius: 10px;
  margin: 12px 0;
  background: #000;
}

.md-body :deep(code) {
  background: rgba(139, 92, 246, 0.14);
  color: var(--accent);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12.5px;
}

.md-body :deep(.md-code) {
  background: rgba(0, 0, 0, 0.35);
  padding: 12px 14px;
  border-radius: 8px;
  overflow-x: auto;
}

.md-body :deep(a) {
  color: var(--accent);
}

.md-body :deep(hr) {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin: 16px 0;
}

.staff {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.staff-item {
  display: flex;
  gap: 14px;
  align-items: center;
}

.staff-role {
  width: 46px;
  text-align: center;
  color: var(--accent);
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 6px;
  padding: 2px 0;
  font-size: 12.5px;
}

.points {
  color: #f59e0b;
  font-weight: 700;
}

.res-table {
  --el-table-border-color: rgba(46, 46, 72, 0.5);
}

.dl-dialog .dl-title {
  font-weight: 700;
  margin-bottom: 14px;
}

.dl-alert {
  margin-bottom: 12px;
}

.dl-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.dl-label {
  width: 64px;
  flex-shrink: 0;
  color: var(--text-sub);
  font-size: 13px;
}

.dl-tip {
  font-size: 12px;
  margin: 6px 0 0;
}

@media (max-width: 768px) {
  .detail-main {
    flex-direction: column;
  }
  .detail-cover {
    width: 100%;
    max-width: 260px;
    margin: 0 auto;
  }
}
</style>
