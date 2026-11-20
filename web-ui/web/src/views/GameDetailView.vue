<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CopyDocument, Download, Link, Star, StarFilled, Warning } from '@element-plus/icons-vue'
import {
  downloadResource,
  FEEDBACK_TYPES,
  getFavoriteStatus,
  getGameDetail,
  getMyRating,
  getOfficialResources,
  getResourceList,
  getResourceTypes,
  rateGame,
  renderMarkdown,
  resolveAssetUrl,
  submitResourceFeedback,
  toggleFavorite,
} from '@gal/shared'
import type { DictItem, Game, OfficialResource, Resource, ResourceSource } from '@gal/shared'
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
const officialResources = ref<OfficialResource[]>([])
const resourceTypeDict = ref<DictItem[]>([])
const officialTypeDict = ref<DictItem[]>([])
const resLoading = ref(false)
const myScore = ref(0)
const ratingLoading = ref(false)
const downloadTarget = ref<Resource | null>(null)
const downloadSources = ref<ResourceSource[]>([])
const downloadVisible = ref(false)

/** 收藏 */
const favorited = ref(false)
const favLoading = ref(false)

/** 资源反馈 */
const feedback = ref({
  visible: false,
  resource: null as Resource | null,
  type: 1,
  content: '',
  submitting: false,
})

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

/**
 * 截图轮播：始终一屏 3 张。
 * 采用「滑动窗口 + 循环」的方式（窗口起点每次移动 1 张），
 * 避免 4 张图时第二屏只显示 1 张的问题。
 */
const shotStart = ref(0)
const visibleShots = computed<string[]>(() => {
  const list = screenshotList.value
  if (!list.length) return []
  const count = Math.min(3, list.length)
  return Array.from({ length: count }, (_, i) => list[(shotStart.value + i) % list.length])
})
const shotHasNav = computed(() => screenshotList.value.length > 3)
const shotIndexAt = (i: number) => (shotStart.value + i) % screenshotList.value.length

function shiftShots(delta: number) {
  const total = screenshotList.value.length
  if (total <= 3) return
  shotStart.value = (shotStart.value + delta + total) % total
}

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
    const [res, official, types] = await Promise.all([
      getResourceList({ pageNum: 1, pageSize: 100, gameId: gameId.value }),
      getOfficialResources(gameId.value).catch(() => null),
      getResourceTypes().catch(() => null),
    ])
    resources.value = (res.rows ?? []).filter((r) => r.status === undefined || r.status === '1')
    officialResources.value = official?.data ?? []
    resourceTypeDict.value = types?.data?.resource ?? []
    officialTypeDict.value = types?.data?.official ?? []
  } catch {
    resources.value = []
    officialResources.value = []
  } finally {
    resLoading.value = false
  }
}

/** 字典回显 */
function typeLabel(value?: string, list = resourceTypeDict.value) {
  return list.find((d) => d.dictValue === value)?.dictLabel ?? value ?? '其他'
}

/** 复制文本 */
async function copyText(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

/* ==================== 收藏 ==================== */

async function loadFavorite() {
  if (!userStore.isLoggedIn) {
    favorited.value = false
    return
  }
  try {
    const res = await getFavoriteStatus('G', gameId.value)
    favorited.value = Boolean(res.data?.favorited)
  } catch {
    favorited.value = false
  }
}

async function toggleFav() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后才能收藏游戏哦')
    return
  }
  favLoading.value = true
  try {
    const res = await toggleFavorite('G', gameId.value)
    favorited.value = Boolean(res.data?.favorited)
    ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  } finally {
    favLoading.value = false
  }
}

/* ==================== 资源反馈 ==================== */

function openFeedback(resource: Resource) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后才能反馈资源问题哦')
    return
  }
  feedback.value = {
    visible: true,
    resource,
    type: 1,
    content: '',
    submitting: false,
  }
}

async function submitFeedback() {
  const target = feedback.value.resource
  if (!target) return
  if (!feedback.value.content.trim()) {
    ElMessage.warning('请填写反馈内容，方便上传者定位问题')
    return
  }
  feedback.value.submitting = true
  try {
    await submitResourceFeedback(target.resourceId, {
      type: feedback.value.type,
      content: feedback.value.content.trim(),
    })
    ElMessage.success('反馈已发送给上传者，可在「用户中心-消息」查看回复')
    feedback.value.visible = false
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '反馈失败')
  } finally {
    feedback.value.submitting = false
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
    downloadTarget.value = data
    downloadSources.value = data.sources ?? []
    downloadVisible.value = true
    await loadResources()
    if ((data.points ?? 0) > 0) {
      ElMessage.success(`下载成功，已扣除 ${data.points} 积分`)
    }
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '下载失败')
  }
}

function openUrl(url: string) {
  window.open(url, '_blank')
}

/** 点击标签 → 带参数跳转到游戏搜索页（多标签用 tagIds） */
function goTag(tagId: number) {
  router.push({ path: '/games', query: { tagIds: String(tagId) } })
}

onMounted(() => {
  load()
  loadResources()
  loadMyRating()
  loadFavorite()
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
          <div class="detail-title-row">
            <h1 class="detail-title">{{ game.titleCn || game.title }}</h1>
            <el-button
              class="fav-btn"
              :class="{ active: favorited }"
              :loading="favLoading"
              :icon="favorited ? StarFilled : Star"
              @click="toggleFav"
            >
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
          </div>
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
            <span
              v-for="t in game.tags || []"
              :key="t.tagId"
              class="dtag clickable"
              title="查看该标签下的游戏"
              @click="goTag(t.tagId)"
            >
              {{ t.name }}
            </span>
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

        <!-- 游戏截图：无图则整块隐藏；始终一屏 3 张，左右箭头滑动窗口（4 张也不会只剩 1 张） -->
        <template v-if="screenshotList.length">
          <div class="section-title">游戏截图</div>
          <div class="panel-card shot-panel">
            <div class="shot-carousel">
              <button
                v-if="shotHasNav"
                class="shot-nav prev"
                aria-label="上一组截图"
                @click="shiftShots(-1)"
              >
                ‹
              </button>
              <div class="shot-row">
                <el-image
                  v-for="(img, imgIndex) in visibleShots"
                  :key="`${img}-${imgIndex}`"
                  :src="img"
                  fit="cover"
                  class="shot-img"
                  :preview-src-list="screenshotList"
                  :initial-index="shotIndexAt(imgIndex)"
                  preview-teleported
                />
              </div>
              <button
                v-if="shotHasNav"
                class="shot-nav next"
                aria-label="下一组截图"
                @click="shiftShots(1)"
              >
                ›
              </button>
              <div v-if="shotHasNav" class="shot-dots">
                <i
                  v-for="(_, idx) in screenshotList"
                  :key="idx"
                  :class="{ active: idx === shotStart }"
                  @click="shotStart = idx"
                />
              </div>
            </div>
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

      <!-- 正版资源（优先展示） -->
      <template v-if="officialResources.length">
        <div class="section-title">
          正版资源
          <span class="section-sub">支持正版，从这里购买</span>
        </div>
        <div class="official-grid">
          <div
            v-for="item in officialResources"
            :key="item.officialId"
            class="official-card panel-card"
            @click="openUrl(item.url)"
          >
            <el-avatar v-if="item.icon" :size="40" shape="square" :src="resolveAssetUrl(item.icon)" />
            <div v-else class="official-icon">🏷️</div>
            <div class="official-main">
              <div class="official-title">{{ item.title }}</div>
              <div class="official-type">{{ typeLabel(item.sourceType, officialTypeDict) }}</div>
            </div>
            <el-icon class="official-link"><Link /></el-icon>
          </div>
        </div>
      </template>

      <!-- 社区资源 -->
      <div class="section-title">
        社区资源
        <span class="section-sub">由社区用户分享</span>
      </div>
      <div class="panel-card">
        <el-skeleton v-if="resLoading" :rows="3" animated />
        <el-empty v-else-if="!resources.length" description="暂无社区资源" :image-size="80" />
        <div v-else class="resource-list">
          <div v-for="r in resources" :key="r.resourceId" class="resource-item">
            <el-avatar :size="42" :src="r.userAvatar ? resolveAssetUrl(r.userAvatar) : undefined" class="res-avatar">
              {{ (r.userName || 'U').slice(0, 1) }}
            </el-avatar>
            <div class="res-main">
              <div class="res-title">{{ r.title }}</div>
              <div class="res-meta">
                <span class="res-user">{{ r.userName || '社区用户' }}</span>
                <span class="dot">·</span>
                <el-tag size="small" effect="plain">{{ typeLabel(r.type) }}</el-tag>
                <span v-if="r.version" class="dot">·</span>
                <span v-if="r.version">版本 {{ r.version }}</span>
                <span v-if="r.size" class="dot">·</span>
                <span v-if="r.size">{{ r.size }}</span>
                <span class="dot">·</span>
                <span>{{ r.sourceCount ?? r.sources?.length ?? 0 }} 个下载来源</span>
                <span class="dot">·</span>
                <span>已下载 {{ r.downloadCount ?? 0 }} 次</span>
                <span v-if="(r.points ?? 0) > 0" class="dot">·</span>
                <span v-if="(r.points ?? 0) > 0" class="points">{{ r.points }} 积分</span>
              </div>
            </div>
            <div class="res-actions">
              <el-button size="small" type="primary" :icon="Download" @click="doDownload(r)">下载</el-button>
              <el-button size="small" text :icon="Warning" @click="openFeedback(r)">反馈</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <CommentSection :target-type="'G'" :target-id="gameId" />
    </div>

    <el-empty v-else description="游戏不存在或已被删除" />
  </div>

  <!-- 下载结果弹窗：多来源（链接 / 提取码 / 解压码） -->
  <el-dialog v-model="downloadVisible" title="资源下载" width="620px" @closed="downloadTarget = null">
    <div v-if="downloadTarget" class="dl-dialog">
      <div class="dl-title">{{ downloadTarget.title }}</div>
      <div class="dl-sub">
        <el-tag size="small" effect="plain">{{ typeLabel(downloadTarget.type) }}</el-tag>
        <span v-if="downloadTarget.version">版本 {{ downloadTarget.version }}</span>
        <span v-if="downloadTarget.size">{{ downloadTarget.size }}</span>
        <span>分享者：{{ downloadTarget.userName || '社区用户' }}</span>
      </div>

      <el-empty v-if="!downloadSources.length" description="该资源暂未提供下载来源" :image-size="64" />

      <div v-for="(s, index) in downloadSources" :key="s.sourceId ?? index" class="dl-source">
        <div class="dl-source-head">
          <el-tag size="small" effect="plain">{{ typeLabel(s.sourceType) }}</el-tag>
          <span v-if="s.remark" class="dl-remark">{{ s.remark }}</span>
        </div>
        <div class="dl-row">
          <span class="dl-label">下载地址</span>
          <el-input :model-value="s.url" readonly class="dl-input">
            <template #append>
              <el-button @click="openUrl(s.url || '')">打开</el-button>
              <el-button :icon="CopyDocument" @click="copyText(s.url)" />
            </template>
          </el-input>
        </div>
        <div class="dl-row">
          <span class="dl-label">提取码</span>
          <el-tag v-if="s.extractPwd" type="warning" class="dl-code">{{ s.extractPwd }}</el-tag>
          <span v-else class="text-dim">无</span>
          <el-button v-if="s.extractPwd" text :icon="CopyDocument" @click="copyText(s.extractPwd)" />
        </div>
        <div class="dl-row">
          <span class="dl-label">解压码</span>
          <el-tag v-if="s.unzipPwd" type="success" class="dl-code">{{ s.unzipPwd }}</el-tag>
          <span v-else class="text-dim">无</span>
          <el-button v-if="s.unzipPwd" text :icon="CopyDocument" @click="copyText(s.unzipPwd)" />
        </div>
      </div>

      <p class="dl-tip text-dim">积分已扣除，请尽快保存资源。若链接失效或无法运行，可点击「反馈」告知上传者。</p>
    </div>
  </el-dialog>

  <!-- 资源反馈弹窗 -->
  <el-dialog v-model="feedback.visible" title="资源反馈" width="480px">
    <div v-if="feedback.resource" class="fb-dialog">
      <div class="fb-target">
        <span class="fb-target-label">反馈资源</span>
        <span class="fb-target-title">{{ feedback.resource.title }}</span>
      </div>
      <div class="fb-field">
        <span class="fb-label">反馈类型</span>
        <el-radio-group v-model="feedback.type" class="fb-types">
          <el-radio v-for="t in FEEDBACK_TYPES" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
        </el-radio-group>
      </div>
      <div class="fb-field">
        <span class="fb-label">反馈内容</span>
        <el-input
          v-model="feedback.content"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请描述具体问题，例如下载链接打不开、解压报错、缺少汉化补丁等"
        />
      </div>
      <p class="fb-tip text-dim">反馈会以站内消息发送给资源上传者，你可以在「用户中心 - 消息」查看处理进度。</p>
    </div>
    <template #footer>
      <el-button @click="feedback.visible = false">取消</el-button>
      <el-button type="primary" :loading="feedback.submitting" @click="submitFeedback">提交反馈</el-button>
    </template>
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
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  padding: 4px 12px;
  border-radius: 999px;
}

.dtag.clickable {
  cursor: pointer;
  transition: all 0.18s;
}

.dtag.clickable:hover {
  background: var(--accent-soft);
  border-color: rgba(167, 139, 250, 0.75);
  transform: translateY(-1px);
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

.shot-carousel {
  position: relative;
  height: 210px;
}

.shot-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--border-glow);
  background: var(--bg-card);
  color: var(--text-main);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-card);
  transition: all 0.2s;
}

.shot-nav:hover {
  color: #fff;
  background: var(--grad-main);
  border-color: transparent;
}

.shot-nav.prev {
  left: -6px;
}

.shot-nav.next {
  right: -6px;
}

.shot-dots {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -4px;
  display: flex;
  justify-content: center;
  gap: 6px;
}

.shot-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--border-glow);
  cursor: pointer;
  transition: all 0.2s;
}

.shot-dots i.active {
  width: 16px;
  border-radius: 3px;
  background: var(--accent);
}

.detail-title-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.detail-title-row .detail-title {
  flex: 1;
  min-width: 0;
}

.fav-btn {
  flex-shrink: 0;
  border-color: var(--border-glow);
  color: var(--text-sub);
}

.fav-btn.active {
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.45);
  background: rgba(245, 158, 11, 0.1);
}

.fb-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fb-target {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--bg-soft);
  border: 1px solid var(--border-glow);
  font-size: 13px;
}

.fb-target-label {
  color: var(--text-dim);
  flex-shrink: 0;
}

.fb-target-title {
  color: var(--text-main);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fb-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fb-label {
  font-size: 13px;
  color: var(--text-sub);
}

.fb-types {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
}

.fb-tip {
  font-size: 12px;
  margin: 0;
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
  background: var(--overlay-strong);
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
  background: var(--accent-soft);
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
  background: var(--accent-soft);
  color: var(--accent);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12.5px;
}

.md-body :deep(.md-code) {
  background: var(--overlay-strong);
  padding: 12px 14px;
  border-radius: 8px;
  overflow-x: auto;
}

.md-body :deep(a) {
  color: var(--accent);
}

.md-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-glow);
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
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
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

/* ---------- 正版资源 ---------- */
.section-sub {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-dim);
  font-weight: 400;
}

.official-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 8px;
}

.official-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border-glow);
}

.official-card:hover {
  border-color: rgba(167, 139, 250, 0.55);
  transform: translateY(-2px);
}

.official-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  font-size: 20px;
}

.official-main {
  flex: 1;
  min-width: 0;
}

.official-title {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.official-type {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
}

.official-link {
  color: var(--accent);
}

/* ---------- 社区资源 ---------- */
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--overlay-soft);
  border: 1px solid var(--border-glow);
}

.res-avatar {
  flex-shrink: 0;
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}

.res-main {
  flex: 1;
  min-width: 0;
}

.res-title {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.res-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--text-dim);
}

.res-user {
  color: var(--text-sub);
}

.res-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* ---------- 下载弹窗 ---------- */
.dl-dialog .dl-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.dl-sub {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-dim);
  margin-bottom: 12px;
}

.dl-source {
  border: 1px solid var(--border-glow);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: var(--overlay-soft);
}

.dl-source-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.dl-remark {
  font-size: 12.5px;
  color: var(--text-dim);
}

.dl-input {
  flex: 1;
}

.dl-code {
  font-family: ui-monospace, Consolas, monospace;
  letter-spacing: 1px;
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
