<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, SortDown, SortUp } from '@element-plus/icons-vue'
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

/** 支持的排序维度（与后端 sortBy 对应） */
const SORT_OPTIONS = [
  { value: 'hot', label: '热门（点击量）' },
  { value: 'download', label: '下载（下载次数）' },
  { value: 'rating', label: '高分（评分）' },
  { value: 'publish', label: '发布时间' },
  { value: 'release', label: '发售时间' },
]

const parseTagIds = (raw: unknown): number[] => {
  if (typeof raw !== 'string' || !raw) return []
  return raw
    .split(',')
    .map((v) => Number(v))
    .filter((v) => Number.isFinite(v) && v > 0)
}

const query = reactive({
  keyword: (route.query.keyword as string) || '',
  tagIds: parseTagIds(route.query.tagIds),
  brandId: route.query.brandId ? Number(route.query.brandId) : undefined,
  /** 排序维度，默认热门 */
  sortBy: (route.query.sortBy as string) || 'hot',
  /** 默认降序 */
  isAsc: route.query.isAsc === 'true',
  pageNum: route.query.page ? Number(route.query.page) : 1,
})

const pageSize = 12

const hasFilter = computed(
  () => !!(query.keyword || query.tagIds.length || query.brandId || query.sortBy !== 'hot'),
)

async function load() {
  loading.value = true
  try {
    const res = await getGameList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
      tagIds: query.tagIds.length ? query.tagIds.join(',') : undefined,
      brandId: query.brandId,
      sortBy: query.sortBy as 'hot' | 'download' | 'rating' | 'publish' | 'release',
      isAsc: query.isAsc,
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
    const res = (await getBrandList()) as unknown as { data?: Brand[] } | Brand[]
    brands.value = (Array.isArray(res) ? res : res?.data) ?? []
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
  query.tagIds = []
  query.brandId = undefined
  query.sortBy = 'hot'
  query.isAsc = false
  query.pageNum = 1
  syncRoute()
  load()
}

function toggleOrder() {
  query.isAsc = !query.isAsc
  search()
}

/** 路由参数同步（便于分享/刷新/从详情页带参跳转） */
function syncRoute() {
  const q: Record<string, string> = {}
  if (query.keyword) q.keyword = query.keyword
  if (query.tagIds.length) q.tagIds = query.tagIds.join(',')
  if (query.brandId) q.brandId = String(query.brandId)
  if (query.sortBy && query.sortBy !== 'hot') q.sortBy = query.sortBy
  if (query.isAsc) q.isAsc = 'true'
  if (query.pageNum > 1) q.page = String(query.pageNum)
  router.replace({ path: '/games', query: q })
}

function onPageChange(p: number) {
  query.pageNum = p
  syncRoute()
  load()
}

/** 详情页点击标签会以 tagIds 形式跳转过来，这里同步筛选条件 */
watch(
  () => route.query.tagIds,
  (v) => {
    const next = parseTagIds(v)
    if (next.join(',') !== query.tagIds.join(',')) {
      query.tagIds = next
      query.pageNum = 1
      load()
    }
  },
)

watch(
  () => route.query.keyword,
  (v) => {
    const next = typeof v === 'string' ? v : ''
    if (next !== query.keyword) {
      query.keyword = next
      query.pageNum = 1
      load()
    }
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

      <el-select
        v-model="query.tagIds"
        multiple
        collapse-tags
        collapse-tags-tooltip
        filterable
        clearable
        placeholder="标签（可多选）"
        class="f-tags"
        @change="search"
      >
        <el-option v-for="t in tags" :key="t.tagId" :label="t.name" :value="t.tagId" />
      </el-select>

      <el-select v-model="query.brandId" placeholder="全部会社" clearable filterable class="f-select" @change="search">
        <el-option v-for="b in brands" :key="b.brandId" :label="b.name" :value="b.brandId" />
      </el-select>

      <div class="sort-group">
        <el-select v-model="query.sortBy" class="f-sort" @change="search">
          <el-option v-for="s in SORT_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-tooltip :content="query.isAsc ? '升序（点击切换为降序）' : '降序（点击切换为升序）'" placement="top">
          <el-button class="sort-toggle" :icon="query.isAsc ? SortUp : SortDown" @click="toggleOrder">
            {{ query.isAsc ? '升序' : '降序' }}
          </el-button>
        </el-tooltip>
      </div>

      <el-button class="grad-btn" @click="search">筛选</el-button>
      <el-button :icon="Refresh" @click="reset" :disabled="!hasFilter">重置</el-button>
    </div>

    <!-- 已选标签 -->
    <div v-if="query.tagIds.length" class="active-tags">
      <span class="active-label">已选标签：</span>
      <el-tag
        v-for="id in query.tagIds"
        :key="id"
        closable
        size="small"
        effect="plain"
        @close="
          () => {
            query.tagIds = query.tagIds.filter((x) => x !== id)
            search()
          }
        "
      >
        {{ tags.find((t) => t.tagId === id)?.name ?? `#${id}` }}
      </el-tag>
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
  margin-bottom: 12px;
}

.f-keyword {
  width: 220px;
}

.f-tags {
  width: 240px;
}

.f-select {
  width: 160px;
}

.sort-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.f-sort {
  width: 170px;
}

.sort-toggle {
  white-space: nowrap;
}

.active-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.active-label {
  color: var(--text-dim);
  font-size: 12.5px;
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
  .f-keyword,
  .f-tags {
    width: 100%;
  }
  .f-select,
  .f-sort {
    flex: 1;
    min-width: 120px;
  }
}
</style>
