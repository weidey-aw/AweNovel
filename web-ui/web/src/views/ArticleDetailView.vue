<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, Star, StarFilled, View } from '@element-plus/icons-vue'
import { getArticleDetail, getFavoriteStatus, renderMarkdown, resolveAssetUrl, toggleFavorite } from '@gal/shared'
import type { Article } from '@gal/shared'
import { useUserStore } from '@/stores/user'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const articleId = computed(() => Number(route.params.id))
const article = ref<Article | null>(null)
const loading = ref(true)
const comments = ref(0)

/** 收藏 */
const favorited = ref(false)
const favLoading = ref(false)

/** 分类中文名 */
const CATEGORY_LABEL: Record<string, string> = {
  news: '资讯',
  review: '评测',
  guide: '攻略',
}
const categoryLabel = computed(() => {
  const c = article.value?.category
  return c ? CATEGORY_LABEL[c] ?? c : '杂谈'
})

/** 正文按 Markdown 渲染（图片 / 视频 / 代码块均支持） */
const contentHtml = computed(() => renderMarkdown(article.value?.content))

async function load() {
  loading.value = true
  try {
    const res = await getArticleDetail(articleId.value)
    article.value = res.data ?? null
    comments.value = article.value?.commentCount ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '文章加载失败')
  } finally {
    loading.value = false
  }
}

async function loadFavorite() {
  if (!userStore.isLoggedIn) {
    favorited.value = false
    return
  }
  try {
    const res = await getFavoriteStatus('A', articleId.value)
    favorited.value = Boolean(res.data?.favorited)
  } catch {
    favorited.value = false
  }
}

async function toggleFav() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后才能收藏文章哦')
    return
  }
  favLoading.value = true
  try {
    const res = await toggleFavorite('A', articleId.value)
    favorited.value = Boolean(res.data?.favorited)
    ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  } finally {
    favLoading.value = false
  }
}

onMounted(() => {
  load()
  loadFavorite()
})
</script>

<template>
  <div class="page-container article-page">
    <el-button text :icon="ArrowLeft" class="back-btn" @click="router.back()">返回</el-button>

    <el-skeleton v-if="loading" :rows="10" animated />

    <template v-else-if="article">
      <article class="panel-card article-card">
        <h1 class="a-title">{{ article.title }}</h1>
        <div class="a-meta">
          <span class="a-cat">{{ categoryLabel }}</span>
          <div class="a-author">
            <el-avatar :size="26" :src="article.userAvatar ? resolveAssetUrl(article.userAvatar) : undefined">
              {{ (article.userName || 'U').slice(0, 1) }}
            </el-avatar>
            <span>{{ article.userName || '匿名' }}</span>
          </div>
          <span>{{ article.createTime || '' }}</span>
          <span class="a-stats">
            <el-icon><View /></el-icon>{{ article.viewCount || 0 }}
            <el-icon class="ml"><ChatDotRound /></el-icon>{{ comments }}
            <el-button
              class="fav-btn"
              :class="{ active: favorited }"
              :loading="favLoading"
              :icon="favorited ? StarFilled : Star"
              size="small"
              text
              @click="toggleFav"
            >
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
          </span>
        </div>
        <div v-if="article.cover" class="a-cover">
          <el-image
            :src="resolveAssetUrl(article.cover)"
            fit="cover"
            :preview-src-list="[resolveAssetUrl(article.cover)]"
            preview-teleported
          />
        </div>
        <p v-if="article.summary" class="a-summary">{{ article.summary }}</p>
        <div class="article-body md-body" v-html="contentHtml || '<p class=\'text-dim\'>暂无内容</p>'" />
      </article>

      <CommentSection :target-type="'A'" :target-id="articleId" @changed="(n: number) => (comments = n)" />
    </template>

    <el-empty v-else description="文章不存在或已被删除" />
  </div>
</template>

<style scoped>
.back-btn {
  margin-bottom: 10px;
}

.article-card {
  padding: 32px 36px;
}

.a-title {
  font-size: 26px;
  margin: 0 0 14px;
  line-height: 1.4;
}

.a-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: var(--text-dim);
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-glow);
  flex-wrap: wrap;
}

.a-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.a-cat {
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-soft);
  padding: 2px 10px;
  border-radius: 999px;
}

.a-stats {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.fav-btn {
  margin-left: 10px;
  color: var(--text-sub);
}

.fav-btn.active {
  color: #f59e0b;
}

.a-stats .ml {
  margin-left: 12px;
}

.a-cover {
  margin: 20px 0;
  border-radius: 10px;
  overflow: hidden;
  max-height: 420px;
}

.a-cover :deep(img) {
  width: 100%;
  object-fit: cover;
}

.a-summary {
  color: var(--text-sub);
  background: var(--accent-soft);
  border-left: 3px solid var(--accent);
  padding: 10px 14px;
  border-radius: 6px;
  margin: 18px 0;
  line-height: 1.8;
}

.md-body {
  line-height: 1.95;
  color: var(--text-sub);
  font-size: 15px;
  word-break: break-word;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  margin: 24px 0 12px;
  color: var(--text-main);
}

.md-body :deep(.md-img) {
  max-width: 100%;
  border-radius: 10px;
  display: block;
  margin: 14px 0;
}

.md-body :deep(.md-video) {
  width: 100%;
  border-radius: 10px;
  margin: 14px 0;
  background: #000;
}

.md-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 14px;
  border-left: 3px solid var(--accent);
  background: var(--accent-soft);
  border-radius: 6px;
}

.md-body :deep(code) {
  background: var(--accent-soft);
  color: var(--accent);
  padding: 1px 6px;
  border-radius: 4px;
}

.md-body :deep(.md-code) {
  background: var(--overlay-strong);
  padding: 14px 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.md-body :deep(a) {
  color: var(--accent);
}

@media (max-width: 768px) {
  .article-card {
    padding: 20px 16px;
  }
}
</style>
