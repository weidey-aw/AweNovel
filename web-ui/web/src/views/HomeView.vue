<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArticleList, getGameList, resolveAssetUrl } from '@gal/shared'
import type { Article, Game } from '@gal/shared'
import GameCard from '@/components/GameCard.vue'

const router = useRouter()

const latest = ref<Game[]>([])
const topRated = ref<Game[]>([])
const articles = ref<Article[]>([])
const stats = ref({ games: 0, articles: 0, loading: true })

onMounted(async () => {
  try {
    const [latestRes, topRes, articleRes] = await Promise.all([
      getGameList({ pageNum: 1, pageSize: 8 }),
      getGameList({ pageNum: 1, pageSize: 50 }),
      getArticleList({ pageNum: 1, pageSize: 4 }),
    ])
    latest.value = latestRes.rows ?? []
    topRated.value = (topRes.rows ?? [])
      .filter((g) => Number(g.ratingAvg) > 0)
      .sort((a, b) => Number(b.ratingAvg) - Number(a.ratingAvg))
      .slice(0, 8)
    articles.value = articleRes.rows ?? []
    stats.value = {
      games: latestRes.total ?? latest.value.length,
      articles: articleRes.total ?? articles.value.length,
      loading: false,
    }
  } catch (err: unknown) {
    stats.value.loading = false
    ElMessage.error(err instanceof Error ? err.message : '首页数据加载失败')
  }
})
</script>

<template>
  <div>
    <!-- Hero 横幅 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-badge">✦ AWE NOVEL</div>
        <h1 class="hero-title">
          欢迎来到 <span class="grad-text">AweNovel</span>
        </h1>
        <p class="hero-sub">
          收录游戏图鉴、攻略评测、资源分享，还有 AI 看板娘「伊卡洛斯」陪你聊天。
          右下角的悬浮球随时可以呼唤她哦～
        </p>
        <div class="hero-actions">
          <el-button class="grad-btn hero-btn" size="large" @click="router.push('/games')">
            浏览游戏库
          </el-button>
          <el-button class="hero-btn ghost" size="large" @click="router.push('/articles')">
            阅读文章
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="stat">
            <b>{{ stats.loading ? '—' : stats.games }}</b>
            <span>收录游戏</span>
          </div>
          <div class="stat">
            <b>{{ stats.loading ? '—' : stats.articles }}</b>
            <span>社区文章</span>
          </div>
          <div class="stat">
            <b>✦</b>
            <span>伊卡洛斯在线</span>
          </div>
        </div>
      </div>
    </section>

    <div class="page-container">
      <!-- 最新游戏 -->
      <div class="section-title">
        最新游戏
        <span class="more" @click="router.push('/games')">查看全部 →</span>
      </div>
      <div v-if="latest.length" class="game-grid">
        <GameCard v-for="g in latest" :key="g.gameId" :game="g" />
      </div>
      <el-empty v-else description="暂无游戏数据，请确认后端已启动" />

      <!-- 高分游戏 -->
      <div class="section-title">
        高分推荐
        <span class="more" @click="router.push('/games')">查看全部 →</span>
      </div>
      <div v-if="topRated.length" class="game-grid">
        <GameCard v-for="g in topRated" :key="g.gameId" :game="g" />
      </div>
      <el-empty v-else description="还没有评分数据" />

      <!-- 最新文章 -->
      <div class="section-title">
        最新文章
        <span class="more" @click="router.push('/articles')">查看全部 →</span>
      </div>
      <div v-if="articles.length" class="article-list">
        <div
          v-for="a in articles"
          :key="a.articleId"
          class="article-row"
          @click="router.push(`/article/${a.articleId}`)"
        >
          <div class="art-main">
            <div class="art-title">{{ a.title }}</div>
            <div class="art-summary">{{ a.summary || a.content?.slice(0, 80) }}</div>
            <div class="art-meta">
              <span class="art-cat">{{ a.category || '杂谈' }}</span>
              <span>{{ a.nickname || a.userName || '匿名' }}</span>
              <span>{{ a.createTime || '' }}</span>
              <span>👁 {{ a.viewCount || 0 }} · 👍 {{ a.likeCount || 0 }}</span>
            </div>
          </div>
          <div v-if="a.cover" class="art-cover">
            <img :src="resolveAssetUrl(a.cover)" :alt="a.title" loading="lazy" />
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无文章" />
    </div>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  padding: 72px 20px 56px;
  background:
    radial-gradient(600px 300px at 20% 0%, rgba(139, 92, 246, 0.22), transparent 60%),
    radial-gradient(500px 260px at 85% 20%, rgba(236, 72, 153, 0.18), transparent 60%);
  border-bottom: 1px solid var(--border-glow);
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  display: inline-block;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--accent);
  border: 1px solid rgba(167, 139, 250, 0.4);
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(139, 92, 246, 0.08);
}

.hero-title {
  font-size: 42px;
  margin: 22px 0 14px;
  letter-spacing: 2px;
}

.hero-sub {
  color: var(--text-sub);
  font-size: 15px;
  line-height: 1.8;
  max-width: 620px;
  margin: 0 auto;
}

.hero-actions {
  margin-top: 30px;
  display: flex;
  gap: 14px;
  justify-content: center;
}

.hero-btn {
  padding: 0 34px;
  height: 44px;
}

.hero-btn.ghost {
  background: transparent;
  border: 1px solid rgba(167, 139, 250, 0.45);
  color: var(--text-main);
}

.hero-stats {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  gap: 60px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat b {
  font-size: 28px;
  background: var(--grad-main);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.stat span {
  font-size: 12px;
  color: var(--text-dim);
}

.game-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 20px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.article-row {
  display: flex;
  gap: 18px;
  padding: 16px 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.22s;
}

.article-row:hover {
  border-color: rgba(167, 139, 250, 0.55);
  transform: translateX(4px);
}

.art-main {
  flex: 1;
  min-width: 0;
}

.art-title {
  font-size: 16px;
  font-weight: 700;
}

.art-summary {
  margin-top: 8px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.art-meta {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--text-dim);
  align-items: center;
  flex-wrap: wrap;
}

.art-cat {
  color: var(--accent);
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.25);
  padding: 2px 10px;
  border-radius: 999px;
}

.art-cover {
  width: 140px;
  height: 88px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.art-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 768px) {
  .hero {
    padding: 44px 16px 36px;
  }
  .hero-title {
    font-size: 28px;
  }
  .hero-stats {
    gap: 30px;
  }
  .art-cover {
    display: none;
  }
}
</style>
