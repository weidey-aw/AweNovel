<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getDictDataList, getDictTypeList } from '@gal/shared'
import type { SysDictData, SysDictType } from '@gal/shared'

const activeTab = ref('type')

/* ---------------- 字典类型 ---------------- */
const typeLoading = ref(false)
const typeList = ref<SysDictType[]>([])
const typeTotal = ref(0)
const typeQuery = reactive({ dictName: '', dictType: '', pageNum: 1 })

async function loadTypes() {
  typeLoading.value = true
  try {
    const res = await getDictTypeList({
      pageNum: typeQuery.pageNum,
      pageSize: 10,
      dictName: typeQuery.dictName || undefined,
      dictType: typeQuery.dictType || undefined,
    })
    typeList.value = res.rows ?? []
    typeTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '字典类型加载失败')
  } finally {
    typeLoading.value = false
  }
}

function resetTypes() {
  typeQuery.dictName = ''
  typeQuery.dictType = ''
  typeQuery.pageNum = 1
  loadTypes()
}

/* ---------------- 字典数据 ---------------- */
const dataLoading = ref(false)
const dataList = ref<SysDictData[]>([])
const dataTotal = ref(0)
const dataQuery = reactive({ dictType: '', dictLabel: '', pageNum: 1 })

async function loadData() {
  dataLoading.value = true
  try {
    const res = await getDictDataList({
      pageNum: dataQuery.pageNum,
      pageSize: 10,
      dictType: dataQuery.dictType || undefined,
      dictLabel: dataQuery.dictLabel || undefined,
    })
    dataList.value = res.rows ?? []
    dataTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '字典数据加载失败')
  } finally {
    dataLoading.value = false
  }
}

function resetData() {
  dataQuery.dictType = ''
  dataQuery.dictLabel = ''
  dataQuery.pageNum = 1
  loadData()
}

/** 从类型行跳到数据页签 */
function viewData(row: SysDictType) {
  dataQuery.dictType = row.dictType
  dataQuery.pageNum = 1
  activeTab.value = 'data'
  loadData()
}

onMounted(() => {
  loadTypes()
  loadData()
})
</script>

<template>
  <div class="page">
    <el-tabs v-model="activeTab" class="dict-tabs">
      <!-- 字典类型 -->
      <el-tab-pane label="字典类型" name="type">
        <div class="toolbar table-card">
          <el-input
            v-model="typeQuery.dictName"
            placeholder="字典名称"
            clearable
            style="width: 180px"
            @keyup.enter="loadTypes"
            @clear="loadTypes"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="typeQuery.dictType"
            placeholder="字典类型"
            clearable
            style="width: 180px"
            @keyup.enter="loadTypes"
            @clear="loadTypes"
          />
          <el-button class="grad-btn" @click="loadTypes">查询</el-button>
          <el-button :icon="Refresh" @click="resetTypes">重置</el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="typeLoading" :data="typeList" border stripe>
            <el-table-column prop="dictId" label="ID" width="80" />
            <el-table-column prop="dictName" label="字典名称" min-width="150" />
            <el-table-column prop="dictType" label="字典类型" min-width="200" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewData(row)">查看数据</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="typeTotal"
              :page-size="10"
              :current-page="typeQuery.pageNum"
              @current-change="(p: number) => { typeQuery.pageNum = p; loadTypes() }"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 字典数据 -->
      <el-tab-pane label="字典数据" name="data">
        <div class="toolbar table-card">
          <el-input
            v-model="dataQuery.dictType"
            placeholder="字典类型"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
            @clear="loadData"
          />
          <el-input
            v-model="dataQuery.dictLabel"
            placeholder="字典标签"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
            @clear="loadData"
          />
          <el-button class="grad-btn" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetData">重置</el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="dataLoading" :data="dataList" border stripe>
            <el-table-column prop="dictCode" label="编码" width="90" />
            <el-table-column prop="dictType" label="字典类型" min-width="180" />
            <el-table-column prop="dictLabel" label="字典标签" min-width="130" />
            <el-table-column prop="dictValue" label="字典键值" min-width="120" />
            <el-table-column prop="dictSort" label="排序" width="80" />
            <el-table-column label="默认" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 'Y'" type="success" size="small">是</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="dataTotal"
              :page-size="10"
              :current-page="dataQuery.pageNum"
              @current-change="(p: number) => { dataQuery.pageNum = p; loadData() }"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.dict-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}
</style>
