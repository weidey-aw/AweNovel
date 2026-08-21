<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getSystemUserList } from '@gal/shared'
import type { SystemUser } from '@gal/shared'

const loading = ref(false)
const list = ref<SystemUser[]>([])
const total = ref(0)

const query = reactive({
  userName: '',
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getSystemUserList({
      pageNum: query.pageNum,
      pageSize,
      userName: query.userName || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.userName = ''
  query.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <el-input
        v-model="query.userName"
        placeholder="搜索用户名"
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
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="userName" label="用户名" min-width="130" />
        <el-table-column prop="nickName" label="昵称" min-width="130">
          <template #default="{ row }">{{ row.nickName || '—' }}</template>
        </el-table-column>
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :size="34" :src="row.avatar || undefined" class="u-avatar">
              {{ (row.nickName || row.userName || 'U').slice(0, 1) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phonenumber" label="手机号" width="130">
          <template #default="{ row }">{{ row.phonenumber || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
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
.u-avatar {
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}
</style>
