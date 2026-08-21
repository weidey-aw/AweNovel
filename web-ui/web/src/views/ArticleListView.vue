<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getArticleList, resolveAssetUrl } from '@gal/shared'
import type { Article } from '@gal/shared'

const router = useRouter()

const categories = ['全部', '攻略', '评测', '杂谈', '资讯']

const list = ref<Article[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  category: '全部',
  keyword: '',
  pageNum: 1,
})

const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getArticleList({
      pageNum: query.pageNum,
      pageSize,
      category: query.category === '全部' ? undefined : query.category,
      keyword: query.keyword || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '文章列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function switchCategory(cat: string) {
  query.category = cat
  search()
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="section-title">社区文章</div>

    <div class="toolbar">
      <div class="cats">
        <button
          v-for="c in categories"
          :key="c"
          class="cat"
          :class="{ active: query.category === c }"
          @click="switchCategory(c)"
        >
          {{ c }}
        </button>
      </div>
      <el-input
        v-model="query.keyword"
        placeholder="搜索文章标题"
        clearable
        class="search"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-skeleton v-if="loading && !list.length" :rows="6" animated />

    <div v-else-if="list.length" class="article-list">
      <div
        v-for="a in list"
        :key="a.articleId"
        class="article-card"
        @click="router.push(`/article/${a.articleId}`)"
      >
        <div v-if="a.cover" class="ac-cover">
          <img :src="resolveAssetUrl(a.cover)" :alt="a.title" loading="lazy" />
        </div>
        <div class="ac-main">
          <div class="ac-title">{{ a.title }}</div>
          <div class="ac-summary">{{ a.summary || a.content?.slice(0, 100) }}</div>
          <div class="ac-meta">
            <span class="ac-cat">{{ a.category || '杂谈' }}</span>
            <span>{{ a.nickname || a.userName || '匿名' }}</span>
            <span>{{ a.createTime || '' }}</span>
            <span class="ac-stats">👁 {{ a.viewCount || 0 }} · 👍 {{ a.likeCount || 0 }} · 💬 {{ a.commentCount || 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无文章" />

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="query.pageNum"
        @current-change="(p: number) => { query.pageNum = p; load() }"
      />
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.cats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cat {
  border: 1px solid rgba(46, 46, 72, 0.8);
  background: transparent;
  color: var(--text-sub);
  padding: 6px 18px;
  border-radius: 999px;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.2s;
}

.cat:hover {
  color: var(--accent);
  border-color: var(--accent);
}

.cat.active {
  background: var(--grad-main);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}

.search {
  width: 220px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  display: flex;
  gap: 18px;
  padding: 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.22s;
}

.article-card:hover {
  transform: translateY(-3px);
  border-color: rgba(167, 139, 250, 0.55);
  box-shadow: 0 10px 28px rgba(139, 92, 246, 0.18);
}

.ac-cover {
  width: 150px;
  height: 96px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.ac-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ac-main {
  flex: 1;
  min-width: 0;
}

.ac-title {
  font-size: 16.5px;
  font-weight: 700;
}

.ac-summary {
  margin-top: 8px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ac-meta {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--text-dim);
  align-items: center;
  flex-wrap: wrap;
}

.ac-cat {
  color: var(--accent);
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.25);
  padding: 2px 10px;
  border-radius: 999px;
}

.ac-stats {
  margin-left: auto;
}

.pager {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .search {
    width: 100%;
  }
  .ac-cover {
    display: none;
  }
}
</style>
