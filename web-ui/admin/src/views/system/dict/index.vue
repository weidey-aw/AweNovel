<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Download, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createDictData,
  createDictType,
  deleteDictData,
  deleteDictTypes,
  exportDictData,
  exportDictTypes,
  getDictDataDetail,
  getDictDataList,
  getDictTypeDetail,
  getDictTypeList,
  getDictTypeOptions,
  refreshDictTypeCache,
  updateDictData,
  updateDictType,
} from '@gal/shared'
import type { SysDictData, SysDictType } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { saveBlob } from '@/utils/download'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

const activeTab = ref<'type' | 'data'>('type')

/* ==================== 字典类型 ==================== */
const typeLoading = ref(false)
const typeList = ref<SysDictType[]>([])
const typeTotal = ref(0)
const typeSelection = ref<SysDictType[]>([])
const typeQuery = reactive({ dictName: '', dictType: '', pageNum: 1 })
const typePageSize = 10

async function loadTypes() {
  typeLoading.value = true
  try {
    const res = await getDictTypeList({
      pageNum: typeQuery.pageNum,
      pageSize: typePageSize,
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

function resetTypeQuery() {
  typeQuery.dictName = ''
  typeQuery.dictType = ''
  typeQuery.pageNum = 1
  loadTypes()
}

const typeFormRef = ref<FormInstance>()
const typeDialog = reactive({ visible: false, isEdit: false, loading: false })
const typeForm = reactive<Partial<SysDictType>>({
  dictId: undefined,
  dictName: '',
  dictType: '',
  status: '0',
  remark: '',
})
const typeRules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
}

function openAddType() {
  typeDialog.isEdit = false
  typeDialog.visible = true
  Object.assign(typeForm, { dictId: undefined, dictName: '', dictType: '', status: '0', remark: '' })
  typeFormRef.value?.clearValidate()
}

async function openEditType(row: SysDictType) {
  typeDialog.isEdit = true
  typeDialog.visible = true
  typeFormRef.value?.clearValidate()
  try {
    const res = await getDictTypeDetail(row.dictId)
    const detail = res.data ?? row
    Object.assign(typeForm, {
      dictId: detail.dictId,
      dictName: detail.dictName,
      dictType: detail.dictType,
      status: detail.status ?? '0',
      remark: detail.remark ?? '',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '字典类型详情加载失败')
  }
}

async function submitType() {
  if (!typeFormRef.value) return
  const valid = await typeFormRef.value.validate().catch(() => false)
  if (!valid) return
  typeDialog.loading = true
  try {
    if (typeDialog.isEdit) {
      await updateDictType({ ...typeForm })
      ElMessage.success('修改成功')
    } else {
      await createDictType({ ...typeForm })
      ElMessage.success('新增成功')
    }
    typeDialog.visible = false
    loadTypes()
    loadDictTypeOptions()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    typeDialog.loading = false
  }
}

async function handleDeleteTypes(rows?: SysDictType[]) {
  const targets = rows ?? typeSelection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的字典类型')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 个字典类型吗？`, '删除确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteDictTypes(targets.map((t) => t.dictId).join(','))
    ElMessage.success('删除成功')
    loadTypes()
    loadDictTypeOptions()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function handleRefreshCache() {
  try {
    await refreshDictTypeCache()
    ElMessage.success('字典缓存已刷新')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '刷新缓存失败')
  }
}

async function handleExportTypes() {
  try {
    const blob = await exportDictTypes({
      dictName: typeQuery.dictName || undefined,
      dictType: typeQuery.dictType || undefined,
    })
    saveBlob(blob, `字典类型_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
}

/* ==================== 字典数据 ==================== */
const dataLoading = ref(false)
const dataList = ref<SysDictData[]>([])
const dataTotal = ref(0)
const dataSelection = ref<SysDictData[]>([])
const dataQuery = reactive({ dictType: '', dictLabel: '', pageNum: 1 })
const dataPageSize = 10
const dictTypeOptions = ref<SysDictType[]>([])

const LIST_CLASS_OPTIONS = [
  { label: '默认', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'danger' },
  { label: '信息', value: 'info' },
]

async function loadDictTypeOptions() {
  try {
    const res = await getDictTypeOptions()
    dictTypeOptions.value = res.data ?? []
  } catch {
    dictTypeOptions.value = []
  }
}

async function loadData() {
  dataLoading.value = true
  try {
    const res = await getDictDataList({
      pageNum: dataQuery.pageNum,
      pageSize: dataPageSize,
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

function resetDataQuery() {
  dataQuery.dictType = ''
  dataQuery.dictLabel = ''
  dataQuery.pageNum = 1
  loadData()
}

const dataFormRef = ref<FormInstance>()
const dataDialog = reactive({ visible: false, isEdit: false, loading: false })
const dataForm = reactive<Partial<SysDictData>>({
  dictCode: undefined,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  dictSort: 0,
  listClass: 'primary',
  isDefault: 'N',
  status: '0',
  remark: '',
})
const dataRules: FormRules = {
  dictType: [{ required: true, message: '请选择字典类型', trigger: 'change' }],
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
}

function openAddData() {
  dataDialog.isEdit = false
  dataDialog.visible = true
  Object.assign(dataForm, {
    dictCode: undefined,
    dictType: dataQuery.dictType || dictTypeOptions.value[0]?.dictType || '',
    dictLabel: '',
    dictValue: '',
    dictSort: 0,
    listClass: 'primary',
    isDefault: 'N',
    status: '0',
    remark: '',
  })
  dataFormRef.value?.clearValidate()
}

async function openEditData(row: SysDictData) {
  dataDialog.isEdit = true
  dataDialog.visible = true
  dataFormRef.value?.clearValidate()
  try {
    const res = await getDictDataDetail(row.dictCode)
    const detail = res.data ?? row
    Object.assign(dataForm, {
      dictCode: detail.dictCode,
      dictType: detail.dictType,
      dictLabel: detail.dictLabel,
      dictValue: detail.dictValue,
      dictSort: detail.dictSort ?? 0,
      listClass: detail.listClass || 'primary',
      isDefault: detail.isDefault ?? 'N',
      status: detail.status ?? '0',
      remark: detail.remark ?? '',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '字典数据详情加载失败')
  }
}

async function submitData() {
  if (!dataFormRef.value) return
  const valid = await dataFormRef.value.validate().catch(() => false)
  if (!valid) return
  dataDialog.loading = true
  try {
    if (dataDialog.isEdit) {
      await updateDictData({ ...dataForm })
      ElMessage.success('修改成功')
    } else {
      await createDictData({ ...dataForm })
      ElMessage.success('新增成功')
    }
    dataDialog.visible = false
    loadData()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    dataDialog.loading = false
  }
}

async function handleDeleteData(rows?: SysDictData[]) {
  const targets = rows ?? dataSelection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的字典数据')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条字典数据吗？`, '删除确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteDictData(targets.map((d) => d.dictCode).join(','))
    ElMessage.success('删除成功')
    loadData()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function handleExportData() {
  try {
    const blob = await exportDictData({
      dictType: dataQuery.dictType || undefined,
      dictLabel: dataQuery.dictLabel || undefined,
    })
    saveBlob(blob, `字典数据_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
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
  loadDictTypeOptions()
  loadData()
})
</script>

<template>
  <div class="page">
    <el-tabs v-model="activeTab" class="dict-tabs">
      <!-- ================= 字典类型 ================= -->
      <el-tab-pane label="字典类型" name="type">
        <div class="toolbar table-card">
          <el-input
            v-model="typeQuery.dictName"
            placeholder="字典名称"
            clearable
            style="width: 170px"
            @keyup.enter="loadTypes"
            @clear="loadTypes"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="typeQuery.dictType"
            placeholder="字典类型"
            clearable
            style="width: 170px"
            @keyup.enter="loadTypes"
            @clear="loadTypes"
          />
          <el-button class="grad-btn" @click="loadTypes">查询</el-button>
          <el-button :icon="Refresh" @click="resetTypeQuery">重置</el-button>
          <span class="spacer" />
          <el-button v-if="can('system:dict:add')" type="primary" :icon="Plus" @click="openAddType">新增</el-button>
          <el-button
            v-if="can('system:dict:remove')"
            type="danger"
            plain
            :disabled="!typeSelection.length"
            @click="handleDeleteTypes()"
          >
            删除
          </el-button>
          <el-button v-if="can('system:dict:export')" :icon="Download" @click="handleExportTypes">导出</el-button>
          <el-button v-if="can('system:dict:edit')" @click="handleRefreshCache">刷新缓存</el-button>
        </div>

        <div class="table-card">
          <el-table
            v-loading="typeLoading"
            :data="typeList"
            border
            stripe
            @selection-change="(r: SysDictType[]) => (typeSelection = r)"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="dictId" label="ID" width="70" />
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
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewData(row)">数据</el-button>
                <el-button v-if="can('system:dict:edit')" link type="primary" :icon="Edit" @click="openEditType(row)">
                  修改
                </el-button>
                <el-button
                  v-if="can('system:dict:remove')"
                  link
                  type="danger"
                  :icon="Delete"
                  @click="handleDeleteTypes([row])"
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
              :total="typeTotal"
              :page-size="typePageSize"
              :current-page="typeQuery.pageNum"
              @current-change="(p: number) => { typeQuery.pageNum = p; loadTypes() }"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ================= 字典数据 ================= -->
      <el-tab-pane label="字典数据" name="data">
        <div class="toolbar table-card">
          <el-input
            v-model="dataQuery.dictType"
            placeholder="字典类型"
            clearable
            style="width: 190px"
            @keyup.enter="loadData"
            @clear="loadData"
          />
          <el-input
            v-model="dataQuery.dictLabel"
            placeholder="字典标签"
            clearable
            style="width: 160px"
            @keyup.enter="loadData"
            @clear="loadData"
          />
          <el-button class="grad-btn" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetDataQuery">重置</el-button>
          <span class="spacer" />
          <el-button v-if="can('system:dict:add')" type="primary" :icon="Plus" @click="openAddData">新增</el-button>
          <el-button
            v-if="can('system:dict:remove')"
            type="danger"
            plain
            :disabled="!dataSelection.length"
            @click="handleDeleteData()"
          >
            删除
          </el-button>
          <el-button v-if="can('system:dict:export')" :icon="Download" @click="handleExportData">导出</el-button>
        </div>

        <div class="table-card">
          <el-table
            v-loading="dataLoading"
            :data="dataList"
            border
            stripe
            @selection-change="(r: SysDictData[]) => (dataSelection = r)"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="dictCode" label="编码" width="80" />
            <el-table-column prop="dictType" label="字典类型" min-width="170" />
            <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
            <el-table-column prop="dictValue" label="字典键值" min-width="110" />
            <el-table-column prop="dictSort" label="排序" width="70" align="center" />
            <el-table-column label="回显样式" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.listClass" size="small" :type="row.listClass">{{ row.listClass }}</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="默认" width="70" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 'Y'" type="success" size="small">是</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button v-if="can('system:dict:edit')" link type="primary" :icon="Edit" @click="openEditData(row)">
                  修改
                </el-button>
                <el-button
                  v-if="can('system:dict:remove')"
                  link
                  type="danger"
                  :icon="Delete"
                  @click="handleDeleteData([row])"
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
              :total="dataTotal"
              :page-size="dataPageSize"
              :current-page="dataQuery.pageNum"
              @current-change="(p: number) => { dataQuery.pageNum = p; loadData() }"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialog.visible" :title="typeDialog.isEdit ? '修改字典类型' : '新增字典类型'" width="520px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：用户性别" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如：sys_user_sex" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="typeDialog.loading" @click="submitType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialog.visible" :title="dataDialog.isEdit ? '修改字典数据' : '新增字典数据'" width="560px">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典类型" prop="dictType">
          <el-select
            v-model="dataForm.dictType"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入字典类型"
            style="width: 100%"
          >
            <el-option
              v-for="t in dictTypeOptions"
              :key="t.dictType"
              :label="`${t.dictName}（${t.dictType}）`"
              :value="t.dictType"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="如：男" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="如：0" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回显样式">
          <el-select v-model="dataForm.listClass" style="width: 100%">
            <el-option v-for="o in LIST_CLASS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否默认">
          <el-radio-group v-model="dataForm.isDefault">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dataDialog.loading" @click="submitData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dict-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
