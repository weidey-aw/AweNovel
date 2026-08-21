<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  approveReview,
  getArticleDetail,
  getGameDetail,
  getResourceList,
  getReviewTasks,
  rejectReview,
} from '@gal/shared'
import type { ReviewTask } from '@gal/shared'

const loading = ref(false)
const tasks = ref<ReviewTask[]>([])
const detailVisible = ref(false)
const detailTitle = ref('')
const detailBody = ref('')

const bizLabels: Record<string, string> = {
  game: '游戏',
  resource: '资源',
  article: '文章',
  comment: '评论',
}

const pendingCount = computed(() => tasks.value.length)

async function load() {
  loading.value = true
  try {
    const res = (await getReviewTasks()) as unknown as { data?: ReviewTask[] } | ReviewTask[]
    tasks.value = (Array.isArray(res) ? res : res?.data) ?? []
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '审核任务加载失败')
  } finally {
    loading.value = false
  }
}

function bizLabel(bizType: string): string {
  return bizLabels[bizType] ?? bizType
}

async function showDetail(task: ReviewTask) {
  const bizId = Number(task.bizId)
  try {
    if (task.bizType === 'game') {
      const res = await getGameDetail(bizId)
      const g = res.data
      detailTitle.value = g?.titleCn || g?.title || `游戏 #${bizId}`
      detailBody.value = g?.summary || '（无简介）'
    } else if (task.bizType === 'article') {
      const res = await getArticleDetail(bizId)
      const a = res.data
      detailTitle.value = a?.title || `文章 #${bizId}`
      detailBody.value = a?.content || '（无正文）'
    } else if (task.bizType === 'resource') {
      const res = await getResourceList({ pageNum: 1, pageSize: 200 })
      const r = (res.rows ?? []).find((x) => Number(x.resourceId) === bizId)
      detailTitle.value = r?.title || `资源 #${bizId}`
      detailBody.value = [
        `类型：${r?.type ?? '—'}`,
        `版本：${r?.version ?? '—'}`,
        `大小：${r?.size ?? '—'}`,
        `所需积分：${r?.points ?? 0}`,
        `地址：${r?.url ?? '—'}`,
      ].join('\n')
    } else {
      detailTitle.value = `${bizLabel(task.bizType)} #${bizId}`
      detailBody.value = '暂不支持预览该类型详情'
    }
    detailVisible.value = true
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '详情加载失败')
  }
}

async function audit(task: ReviewTask, pass: boolean) {
  try {
    await ElMessageBox.confirm(
      `确认${pass ? '通过' : '拒绝'}该${bizLabel(task.bizType)}（ID: ${task.bizId}）？`,
      '审核确认',
      { type: 'warning' },
    )
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
      <el-alert
        :title="`当前共有 ${pendingCount} 个待审核任务`"
        type="info"
        :closable="false"
        show-icon
        class="alert"
      />
      <span class="spacer" />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tasks" border stripe empty-text="暂无待审核任务 🎉">
        <el-table-column prop="taskId" label="任务ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="taskName" label="任务名称" min-width="140" />
        <el-table-column label="业务类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ bizLabel(row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizId" label="业务ID" width="90" />
        <el-table-column prop="processInstanceId" label="流程实例ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">预览</el-button>
            <el-button size="small" type="success" plain @click="audit(row, true)">通过</el-button>
            <el-button size="small" type="danger" plain @click="audit(row, false)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情预览 -->
    <el-dialog v-model="detailVisible" :title="`详情 · ${detailTitle}`" width="620px">
      <div class="detail-preview">
        <h3 class="dp-title">{{ detailTitle }}</h3>
        <pre class="dp-body">{{ detailBody }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.alert {
  flex: 1;
  min-width: 260px;
}

.detail-preview {
  max-height: 60vh;
  overflow-y: auto;
}

.dp-title {
  margin: 0 0 12px;
}

.dp-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  font-family: inherit;
  font-size: 14px;
  margin: 0;
}
</style>
