<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getAllTags, getBrandList, getGameList } from '@gal/shared'
import type { Brand, Game, Tag } from '@gal/shared'
import GameCard from '@/components/GameCard.vue'

const route = useRoute()
const router = useRouter()

const games = ref<Game[]>([])
const total = ref(0)
const loading = ref(false)
const tags = ref<Tag[]>([])
const brands = ref<Brand[]>([])

const query = reactive({
  keyword: (route.query.keyword as string) || '',
  tagId: route.query.tagId ? Number(route.query.tagId) : undefined,
  brandId: route.query.brandId ? Number(route.query.brandId) : undefined,
  pageNum: route.query.page ? Number(route.query.page) : 1,
})

const pageSize = 12

const hasFilter = computed(() => !!(query.keyword || query.tagId || query.brandId))

async function load() {
  loading.value = true
  try {
    const res = await getGameList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
      tagId: query.tagId,
      brandId: query.brandId,
    })
    games.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '游戏列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const res = (await getAllTags()) as unknown as { data?: Tag[] } | Tag[]
    tags.value = (Array.isArray(res) ? res : res?.data) ?? []
  } catch {
    tags.value = []
  }
  try {
    const res = (await getBrandList()) as unknown as { rows?: Brand[]; data?: Brand[] } | Brand[]
    if (Array.isArray(res)) brands.value = res
    else brands.value = res?.rows ?? res?.data ?? []
  } catch {
    brands.value = []
  }
}

function search() {
  query.pageNum = 1
  syncRoute()
  load()
}

function reset() {
  query.keyword = ''
  query.tagId = undefined
  query.brandId = undefined
  query.pageNum = 1
  syncRoute()
  load()
}

function syncRoute() {
  const q: Record<string, string> = {}
  if (query.keyword) q.keyword = query.keyword
  if (query.tagId) q.tagId = String(query.tagId)
  if (query.brandId) q.brandId = String(query.brandId)
  if (query.pageNum > 1) q.page = String(query.pageNum)
  router.replace({ path: '/games', query: q })
}

function onPageChange(p: number) {
  query.pageNum = p
  syncRoute()
  load()
}

watch(
  () => route.query.keyword,
  (v) => {
    if (v !== undefined) query.keyword = v as string
  },
)

onMounted(() => {
  loadOptions()
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="section-title">游戏库</div>

    <!-- 筛选栏 -->
    <div class="filter-bar panel-card">
      <el-input
        v-model="query.keyword"
        placeholder="搜索游戏标题 / 中文名"
        clearable
        class="f-keyword"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="query.tagId" placeholder="全部标签" clearable class="f-select">
        <el-option v-for="t in tags" :key="t.tagId" :label="t.name" :value="t.tagId" />
      </el-select>
      <el-select v-model="query.brandId" placeholder="全部会社" clearable class="f-select">
        <el-option v-for="b in brands" :key="b.brandId" :label="b.nameCn || b.name" :value="b.brandId" />
      </el-select>
      <el-button class="grad-btn" @click="search">筛选</el-button>
      <el-button :icon="Refresh" @click="reset" :disabled="!hasFilter">重置</el-button>
    </div>

    <!-- 列表 -->
    <el-skeleton v-if="loading && !games.length" :rows="8" animated class="list-skeleton" />

    <div v-else-if="games.length" class="game-grid">
      <GameCard v-for="g in games" :key="g.gameId" :game="g" />
    </div>

    <el-empty v-else description="没有找到符合条件的游戏" />

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="query.pageNum"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.f-keyword {
  width: 240px;
}

.f-select {
  width: 160px;
}

.list-skeleton {
  padding: 12px 0;
}

.game-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 20px;
}

.pager {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .f-keyword {
    width: 100%;
  }
  .f-select {
    flex: 1;
    min-width: 120px;
  }
}
</style>
