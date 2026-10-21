<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Download, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createConfig,
  deleteConfigs,
  exportConfigs,
  getConfigDetail,
  getConfigList,
  refreshConfigCache,
  updateConfig,
} from '@gal/shared'
import type { SysConfig } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { saveBlob } from '@/utils/download'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/* 系统内置参数不允许删除 */
const isBuiltIn = (row: SysConfig) => row.configType === 'Y'

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<SysConfig[]>([])
const total = ref(0)
const selection = ref<SysConfig[]>([])
const query = reactive({ configName: '', configKey: '', configType: '', pageNum: 1 })
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getConfigList({
      pageNum: query.pageNum,
      pageSize,
      configName: query.configName || undefined,
      configKey: query.configKey || undefined,
      configType: query.configType || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '参数列表加载失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.configName = ''
  query.configKey = ''
  query.configType = ''
  query.pageNum = 1
  load()
}

/* ==================== 新增 / 修改 ==================== */
const formRef = ref<FormInstance>()
const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<SysConfig>>({
  configId: undefined,
  configName: '',
  configKey: '',
  configValue: '',
  configType: 'N',
  remark: '',
})
const rules: FormRules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }],
}

function openAdd() {
  dialog.isEdit = false
  dialog.visible = true
  Object.assign(form, {
    configId: undefined,
    configName: '',
    configKey: '',
    configValue: '',
    configType: 'N',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function openEdit(row: SysConfig) {
  dialog.isEdit = true
  dialog.visible = true
  formRef.value?.clearValidate()
  try {
    const res = await getConfigDetail(row.configId)
    const detail = res.data ?? row
    Object.assign(form, {
      configId: detail.configId,
      configName: detail.configName,
      configKey: detail.configKey,
      configValue: detail.configValue,
      configType: detail.configType ?? 'N',
      remark: detail.remark ?? '',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '参数详情加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialog.loading = true
  try {
    if (dialog.isEdit) {
      await updateConfig({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createConfig({ ...form })
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    dialog.loading = false
  }
}

/* ==================== 删除 / 缓存 / 导出 ==================== */
async function handleDelete(rows?: SysConfig[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的参数')
    return
  }
  if (targets.some((t) => isBuiltIn(t))) {
    ElMessage.warning('系统内置参数不允许删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 个参数吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteConfigs(targets.map((t) => t.configId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function handleRefreshCache() {
  try {
    await refreshConfigCache()
    ElMessage.success('参数缓存已刷新')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '刷新缓存失败')
  }
}

async function handleExport() {
  try {
    const blob = await exportConfigs({
      configName: query.configName || undefined,
      configKey: query.configKey || undefined,
      configType: query.configType || undefined,
    })
    saveBlob(blob, `参数数据_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.configName"
        placeholder="参数名称"
        clearable
        style="width: 170px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="query.configKey"
        placeholder="参数键名"
        clearable
        style="width: 190px"
        @keyup.enter="load"
        @clear="load"
      />
      <el-select v-model="query.configType" placeholder="系统内置" clearable style="width: 130px" @change="load">
        <el-option label="是" value="Y" />
        <el-option label="否" value="N" />
      </el-select>
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('system:config:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button
        v-if="can('system:config:remove')"
        type="danger"
        plain
        :disabled="!selection.length"
        @click="handleDelete()"
      >
        删除
      </el-button>
      <el-button v-if="can('system:config:export')" :icon="Download" @click="handleExport">导出</el-button>
      <el-button v-if="can('system:config:edit')" @click="handleRefreshCache">刷新缓存</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: SysConfig[]) => (selection = r)">
        <el-table-column type="selection" width="46" :selectable="(row: SysConfig) => !isBuiltIn(row)" />
        <el-table-column prop="configId" label="ID" width="70" />
        <el-table-column prop="configName" label="参数名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="configKey" label="参数键名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="参数键值" min-width="180" show-overflow-tooltip />
        <el-table-column label="系统内置" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="isBuiltIn(row) ? 'warning' : 'info'" size="small" effect="plain">
              {{ isBuiltIn(row) ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('system:config:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button
              v-if="can('system:config:remove')"
              link
              type="danger"
              :icon="Delete"
              :disabled="isBuiltIn(row)"
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

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '修改参数' : '新增参数'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="如：主框架页-默认皮肤样式名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如：sys.index.skinName" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="3" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="系统内置">
          <el-radio-group v-model="form.configType">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
