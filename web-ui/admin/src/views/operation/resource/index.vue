<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { approveReview, getResourceList, rejectReview } from '@gal/shared'
import type { Resource, ReviewTask } from '@gal/shared'
import { findReviewTask } from '@/utils/review'

type TagType = 'primary' | 'success' | 'danger' | 'info' | 'warning'

const statusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
}

const loading = ref(false)
const list = ref<Resource[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<Resource | null>(null)

const query = reactive({
  gameId: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getResourceList({
      pageNum: query.pageNum,
      pageSize,
      gameId: query.gameId,
      status: query.status,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '资源列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.gameId = undefined
  query.status = undefined
  query.pageNum = 1
  load()
}

function showDetail(row: Resource) {
  detail.value = row
  detailVisible.value = true
}

function statusInfo(s?: number): { label: string; type: TagType } {
  const hit = statusMap[Number(s ?? 0)]
  return hit ?? { label: '未知', type: 'info' }
}

/** 通过/拒绝走审核任务流程（按 resourceId 匹配 processInstanceId） */
async function audit(row: Resource, pass: boolean) {
  const task: ReviewTask | undefined = await findReviewTask('resource', row.resourceId)
  if (!task) {
    ElMessage.warning('未找到对应的审核任务，请到「内容审核」页处理')
    return
  }
  try {
    await ElMessageBox.confirm(`确认${pass ? '通过' : '拒绝'}资源「${row.title}」？`, '审核确认', { type: 'warning' })
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
      <el-input
        v-model.number="query.gameId"
        placeholder="游戏 ID"
        clearable
        style="width: 140px"
        type="number"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="resourceId" label="ID" width="70" />
        <el-table-column prop="gameId" label="游戏ID" width="80" />
        <el-table-column prop="title" label="资源名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column prop="version" label="版本" width="90">
          <template #default="{ row }">{{ row.version || '—' }}</template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="90">
          <template #default="{ row }">{{ row.size || '—' }}</template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="80">
          <template #default="{ row }">{{ row.points ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type" size="small">{{ statusInfo(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">查看</el-button>
            <template v-if="Number(row.status ?? 0) === 0">
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

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="资源详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="ID">{{ detail.resourceId }}</el-descriptions-item>
        <el-descriptions-item label="游戏ID">{{ detail.gameId }}</el-descriptions-item>
        <el-descriptions-item label="名称" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.type }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.version || '—' }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ detail.size || '—' }}</el-descriptions-item>
        <el-descriptions-item label="所需积分">{{ detail.points ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="下载次数">{{ detail.downloadCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="提取码">{{ detail.extractPwd || '—' }}</el-descriptions-item>
        <el-descriptions-item label="校验值" :span="2">{{ detail.checksum || '—' }}</el-descriptions-item>
        <el-descriptions-item label="资源地址" :span="2">
          <el-link v-if="detail.url" type="primary" :href="detail.url" target="_blank">{{ detail.url }}</el-link>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="发布人">{{ detail.userName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detail.createTime || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
