<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getNoticeDetail, getNoticeList } from '@gal/shared'
import type { SysNotice } from '@gal/shared'

const loading = ref(false)
const list = ref<SysNotice[]>([])
const total = ref(0)

const query = reactive({
  noticeTitle: '',
  noticeType: '',
  pageNum: 1,
})
const pageSize = 10

const detailVisible = ref(false)
const detail = ref<SysNotice | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await getNoticeList({
      pageNum: query.pageNum,
      pageSize,
      noticeTitle: query.noticeTitle || undefined,
      noticeType: query.noticeType || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '公告列表加载失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  query.noticeTitle = ''
  query.noticeType = ''
  query.pageNum = 1
  load()
}

async function openDetail(row: SysNotice) {
  try {
    const res = await getNoticeDetail(row.noticeId)
    detail.value = res.data ?? row
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <el-input
        v-model="query.noticeTitle"
        placeholder="公告标题"
        clearable
        style="width: 220px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.noticeType" placeholder="类型" clearable style="width: 140px" @change="load">
        <el-option label="通知" value="1" />
        <el-option label="公告" value="2" />
      </el-select>
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="noticeId" label="ID" width="80" />
        <el-table-column prop="noticeTitle" label="公告标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.noticeType === '2' ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.noticeType === '2' ? '公告' : '通知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建者" width="120">
          <template #default="{ row }">{{ row.createBy || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" title="公告详情" width="640px">
      <template v-if="detail">
        <h3 class="notice-title">{{ detail.noticeTitle }}</h3>
        <div class="notice-meta">
          <span>{{ detail.noticeType === '2' ? '公告' : '通知' }}</span>
          <span>{{ detail.createBy || '系统' }}</span>
          <span>{{ detail.createTime || '' }}</span>
        </div>
        <div class="notice-body" v-html="detail.noticeContent"></div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notice-title {
  margin: 0 0 8px;
  font-size: 17px;
}

.notice-meta {
  display: flex;
  gap: 16px;
  color: #8a8aa3;
  font-size: 12.5px;
  margin-bottom: 14px;
}

.notice-body {
  line-height: 1.75;
  color: #3d3d52;
  word-break: break-word;
}
</style>
