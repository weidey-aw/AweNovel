<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import { changeCommentStatus, deleteComments, getAdminCommentList } from '@gal/shared'
import type { Comment } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/** 评论目标类型：G 游戏 / A 文章 / R 资源 */
const TARGET_TYPES = [
  { value: 'G', label: '游戏' },
  { value: 'A', label: '文章' },
  { value: 'R', label: '资源' },
]

const targetLabel = (value?: string) => TARGET_TYPES.find((t) => t.value === value)?.label ?? value ?? '—'

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<Comment[]>([])
const total = ref(0)
const selection = ref<Comment[]>([])
const query = reactive({ keyword: '', targetType: '', status: '', pageNum: 1 })
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getAdminCommentList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
      targetType: query.targetType || undefined,
      status: query.status || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '评论列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.targetType = ''
  query.status = ''
  query.pageNum = 1
  load()
}

/* ==================== 状态 / 删除 ==================== */
async function onStatusChange(row: Comment, value: string | number | boolean) {
  const nextStatus = value ? '1' : '0'
  try {
    await changeCommentStatus(row.commentId, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === '1' ? '已显示' : '已隐藏')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '状态修改失败')
    load()
  }
}

async function handleDelete(rows?: Comment[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的评论')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 条评论吗？其下的回复会一并删除，且无法恢复。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteComments(targets.map((c) => c.commentId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.keyword"
        placeholder="评论内容"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.targetType" placeholder="目标类型" clearable style="width: 140px" @change="search">
        <el-option v-for="t in TARGET_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
        <el-option label="正常" value="1" />
        <el-option label="已隐藏" value="0" />
      </el-select>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button
        v-if="can('community:comment:remove')"
        type="danger"
        plain
        :disabled="!selection.length"
        @click="handleDelete()"
      >
        删除
      </el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: Comment[]) => (selection = r)">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="commentId" label="ID" width="70" />
        <el-table-column label="目标" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ targetLabel(row.targetType) }}</el-tag>
            <span class="target-id">#{{ row.targetId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" :src="row.avatar || undefined">
                {{ (row.nickName || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ row.nickName || `#${row.userId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="回复" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.pid" class="muted">回复 #{{ row.pid }}</span>
            <span v-else class="muted">顶层</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '1'"
              :disabled="!can('community:comment:edit')"
              @change="(v: string | number | boolean) => onStatusChange(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="can('community:comment:remove')"
              link
              type="danger"
              :icon="Delete"
              @click="handleDelete([row])"
            >
              删除
            </el-button>
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
  </div>
</template>

<style scoped>
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.muted {
  color: #a0a0b8;
}

.target-id {
  margin-left: 4px;
  color: #8a8aa3;
  font-size: 12px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
