<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { approveReview, getArticleList, rejectReview, resolveAssetUrl } from '@gal/shared'
import type { Article, ReviewTask } from '@gal/shared'
import { findReviewTask } from '@/utils/review'

const loading = ref(false)
const list = ref<Article[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<Article | null>(null)

const query = reactive({
  category: undefined as string | undefined,
  keyword: '',
  pageNum: 1,
})
const pageSize = 10

const categories = ['攻略', '评测', '杂谈', '资讯']

async function load() {
  loading.value = true
  try {
    const res = await getArticleList({
      pageNum: query.pageNum,
      pageSize,
      category: query.category,
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

function reset() {
  query.category = undefined
  query.keyword = ''
  query.pageNum = 1
  load()
}

function showDetail(row: Article) {
  detail.value = row
  detailVisible.value = true
}

/** 通过/拒绝走审核任务流程（按 articleId 匹配 processInstanceId） */
async function audit(row: Article, pass: boolean) {
  const task: ReviewTask | undefined = await findReviewTask('article', row.articleId)
  if (!task) {
    ElMessage.warning('未找到对应的审核任务，请到「内容审核」页处理')
    return
  }
  try {
    await ElMessageBox.confirm(`确认${pass ? '通过' : '拒绝'}文章「${row.title}」？`, '审核确认', { type: 'warning' })
    if (pass) await approveReview(task.processInstanceId)
    else await rejectReview(task.processInstanceId)
    ElMessage.success(pass ? '已通过' : '已拒绝')
    load()
  } catch {
    /* 取消 */
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 140px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索文章标题"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="articleId" label="ID" width="70" />
        <el-table-column label="封面" width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.cover && resolveAssetUrl(row.cover)"
              :src="resolveAssetUrl(row.cover)"
              fit="cover"
              style="width: 56px; height: 36px; border-radius: 4px"
              :preview-src-list="[resolveAssetUrl(row.cover)]"
              preview-teleported
            />
            <span v-else class="text-dim">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="90">
          <template #default="{ row }">{{ row.category || '杂谈' }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="作者" width="120">
          <template #default="{ row }">{{ row.nickname || row.userName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="commentCount" label="评论" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="Number(row.status ?? 0) === 1" type="success" size="small">已发布</el-tag>
            <el-tag v-else type="warning" size="small">待审核</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">查看</el-button>
            <template v-if="Number(row.status ?? 0) !== 1">
              <el-button size="small" type="success" plain @click="audit(row, true)">通过</el-button>
              <el-button size="small" type="danger" plain @click="audit(row, false)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
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

    <!-- 文章详情 -->
    <el-dialog v-model="detailVisible" title="文章详情" width="640px">
      <div v-if="detail" class="article-detail">
        <h3 class="ad-title">{{ detail.title }}</h3>
        <div class="ad-meta">
          <el-tag size="small">{{ detail.category || '杂谈' }}</el-tag>
          <span>{{ detail.nickname || detail.userName || '匿名' }}</span>
          <span>{{ detail.createTime || '' }}</span>
        </div>
        <div v-if="detail.cover" class="ad-cover">
          <img :src="resolveAssetUrl(detail.cover)" :alt="detail.title" />
        </div>
        <div class="ad-body" v-html="detail.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.article-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.ad-title {
  margin: 0 0 10px;
}

.ad-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 12.5px;
  color: #8a8aa3;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f8;
}

.ad-cover {
  margin: 14px 0;
  border-radius: 8px;
  overflow: hidden;
}

.ad-cover img {
  width: 100%;
  max-height: 260px;
  object-fit: cover;
}

.ad-body {
  line-height: 1.9;
  font-size: 14px;
  word-break: break-word;
}
</style>
