<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { deleteComments, getCommentList } from '@gal/shared'
import type { Comment } from '@gal/shared'

const loading = ref(false)
const list = ref<Comment[]>([])
const total = ref(0)
const selection = ref<Comment[]>([])

const query = reactive({
  targetType: undefined as string | undefined,
  targetId: undefined as number | undefined,
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getCommentList({
      pageNum: query.pageNum,
      pageSize,
      targetType: query.targetType,
      targetId: query.targetId,
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
  query.targetType = undefined
  query.targetId = undefined
  query.pageNum = 1
  load()
}

async function removeRows(rows: Comment[]) {
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 条评论？删除后不可恢复。`, '删除确认', { type: 'warning' })
    await deleteComments(rows.map((r) => r.commentId).join(','))
    ElMessage.success('删除成功')
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
      <el-select v-model="query.targetType" placeholder="全部类型" clearable style="width: 140px">
        <el-option label="游戏" value="game" />
        <el-option label="文章" value="article" />
      </el-select>
      <el-input
        v-model.number="query.targetId"
        placeholder="目标 ID（游戏/文章）"
        clearable
        type="number"
        style="width: 180px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button type="danger" plain :disabled="!selection.length" @click="removeRows(selection)">
        批量删除
      </el-button>
    </div>

    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @selection-change="(rows: Comment[]) => (selection = rows)"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="commentId" label="ID" width="70" />
        <el-table-column prop="targetType" label="类型" width="80" />
        <el-table-column prop="targetId" label="目标ID" width="90" />
        <el-table-column prop="userName" label="用户" width="120">
          <template #default="{ row }">{{ row.nickname || row.userName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" plain @click="removeRows([row])">删除</el-button>
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
