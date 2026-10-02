<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { createBrand, deleteBrands, getBrandList, updateBrand } from '@gal/shared'
import type { Brand } from '@gal/shared'

const loading = ref(false)
const list = ref<Brand[]>([])
const total = ref(0)
const selection = ref<Brand[]>([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = (await getBrandList({ pageNum: pageNum.value, pageSize })) as unknown as
      | { rows?: Brand[]; total?: number }
      | Brand[]
    if (Array.isArray(res)) {
      list.value = res
      total.value = res.length
    } else {
      list.value = res.rows ?? []
      total.value = res.total ?? list.value.length
    }
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '会社列表加载失败')
  } finally {
    loading.value = false
  }
}

/* ---------- 弹窗 ---------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  brandId: undefined as number | undefined,
  name: '',
  nameCn: '',
  country: '',
  website: '',
  logo: '',
  description: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入会社名称', trigger: 'blur' }],
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { brandId: undefined, name: '', nameCn: '', country: '', website: '', logo: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: Brand) {
  isEdit.value = true
  Object.assign(form, {
    brandId: row.brandId,
    name: row.name ?? '',
    nameCn: row.nameCn ?? '',
    country: row.country ?? '',
    website: row.website ?? '',
    logo: row.logo ?? '',
    description: row.description ?? '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBrand({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createBrand({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function removeRows(rows: Brand[]) {
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 个会社？`, '删除确认', { type: 'warning' })
    await deleteBrands(rows.map((r) => r.brandId).join(','))
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
      <el-input
        v-model="keyword"
        placeholder="搜索会社名称"
        clearable
        style="width: 220px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="grad-btn" @click="load">查询</el-button>
      <span class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增会社</el-button>
      <el-button type="danger" plain :disabled="!selection.length" @click="removeRows(selection)">批量删除</el-button>
    </div>

    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @selection-change="(rows: Brand[]) => (selection = rows)"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="brandId" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nameCn" label="中文名" width="140">
          <template #default="{ row }">{{ row.nameCn || '—' }}</template>
        </el-table-column>
        <el-table-column prop="country" label="国家/地区" width="110">
          <template #default="{ row }">{{ row.country || '—' }}</template>
        </el-table-column>
        <el-table-column prop="website" label="官网" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <a v-if="row.website" :href="row.website" target="_blank" rel="noopener">{{ row.website }}</a>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
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
          :current-page="pageNum"
          @current-change="(p: number) => { pageNum = p; load() }"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑会社' : '新增会社'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="会社日文/英文名" />
        </el-form-item>
        <el-form-item label="中文名">
          <el-input v-model="form.nameCn" placeholder="中文译名（可选）" />
        </el-form-item>
        <el-form-item label="国家/地区">
          <el-input v-model="form.country" placeholder="如：日本" />
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="form.website" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="form.logo" placeholder="Logo 图片 URL（可选）" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="会社简介（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="grad-btn" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
