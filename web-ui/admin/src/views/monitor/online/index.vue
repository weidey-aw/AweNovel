<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { forceLogout, getOnlineUsers } from '@gal/shared'
import type { SysOnlineUser } from '@gal/shared'

const loading = ref(false)
const list = ref<SysOnlineUser[]>([])
const total = ref(0)

const query = reactive({
  userName: '',
  ipaddr: '',
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getOnlineUsers({
      pageNum: query.pageNum,
      pageSize,
      userName: query.userName || undefined,
      ipaddr: query.ipaddr || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '在线用户加载失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  query.userName = ''
  query.ipaddr = ''
  query.pageNum = 1
  load()
}

async function handleForceLogout(row: SysOnlineUser) {
  try {
    await ElMessageBox.confirm(`确认强退用户「${row.loginName}」的登录会话吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await forceLogout(row.sessionId)
    ElMessage.success('强退成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '强退失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <el-input
        v-model="query.userName"
        placeholder="用户账号"
        clearable
        style="width: 180px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="query.ipaddr"
        placeholder="登录IP"
        clearable
        style="width: 180px"
        @keyup.enter="load"
        @clear="load"
      />
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="sessionId" label="会话编号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="loginName" label="登录名称" min-width="130" />
        <el-table-column prop="ipaddr" label="主机" min-width="140" />
        <el-table-column prop="loginLocation" label="登录地点" min-width="140">
          <template #default="{ row }">{{ row.loginLocation || '—' }}</template>
        </el-table-column>
        <el-table-column prop="browser" label="浏览器" min-width="130">
          <template #default="{ row }">{{ row.browser || '—' }}</template>
        </el-table-column>
        <el-table-column prop="os" label="操作系统" min-width="130">
          <template #default="{ row }">{{ row.os || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'on_line' ? 'success' : 'info'" size="small">
              {{ row.status === 'on_line' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTimestamp" label="登录时间" width="170">
          <template #default="{ row }">{{ row.startTimestamp || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleForceLogout(row)">强退</el-button>
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
