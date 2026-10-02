<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getConfigList } from '@gal/shared'
import type { SysConfig } from '@gal/shared'

const loading = ref(false)
const list = ref<SysConfig[]>([])
const total = ref(0)

const query = reactive({
  configName: '',
  configKey: '',
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getConfigList({
      pageNum: query.pageNum,
      pageSize,
      configName: query.configName || undefined,
      configKey: query.configKey || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '参数列表加载失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  query.configName = ''
  query.configKey = ''
  query.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <el-input
        v-model="query.configName"
        placeholder="参数名称"
        clearable
        style="width: 200px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="query.configKey"
        placeholder="参数键名"
        clearable
        style="width: 240px"
        @keyup.enter="load"
        @clear="load"
      />
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="configId" label="ID" width="80" />
        <el-table-column prop="configName" label="参数名称" min-width="200" />
        <el-table-column prop="configKey" label="参数键名" min-width="230" show-overflow-tooltip />
        <el-table-column prop="configValue" label="参数键值" min-width="180" show-overflow-tooltip />
        <el-table-column label="系统内置" width="100">
          <template #default="{ row }">
            <el-tag :type="row.configType === 'Y' ? 'warning' : 'info'" size="small" effect="plain">
              {{ row.configType === 'Y' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
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
