<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getArticleDetail, resolveAssetUrl } from '@gal/shared'
import type { Article } from '@gal/shared'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => Number(route.params.id))
const article = ref<Article | null>(null)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const res = await getArticleDetail(articleId.value)
    article.value = res.data ?? null
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '文章加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container article-page">
    <el-button text :icon="ArrowLeft" class="back-btn" @click="router.back()">返回</el-button>

    <el-skeleton v-if="loading" :rows="10" animated />

    <template v-else-if="article">
      <article class="panel-card article-card">
        <h1 class="a-title">{{ article.title }}</h1>
        <div class="a-meta">
          <span v-if="article.category" class="a-cat">{{ article.category }}</span>
          <span>{{ article.nickname || article.userName || '匿名' }}</span>
          <span>{{ article.createTime || '' }}</span>
          <span class="a-stats">👁 {{ article.viewCount || 0 }} · 👍 {{ article.likeCount || 0 }}</span>
        </div>
        <div v-if="article.cover" class="a-cover">
          <img :src="resolveAssetUrl(article.cover)" :alt="article.title" />
        </div>
        <div class="article-body" v-html="article.content"></div>
      </article>

      <CommentSection :target-type="'article'" :target-id="articleId" />
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

.a-cat {
  color: var(--accent);
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.25);
  padding: 2px 10px;
  border-radius: 999px;
}

.a-stats {
  margin-left: auto;
}

.a-cover {
  margin: 20px 0;
  border-radius: 10px;
  overflow: hidden;
  max-height: 380px;
}

.a-cover img {
  width: 100%;
  object-fit: cover;
}

@media (max-width: 768px) {
  .article-card {
    padding: 20px 16px;
  }
}
</style>
