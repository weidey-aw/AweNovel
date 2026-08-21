<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createTag, deleteTags, getTagList, updateTag } from '@gal/shared'
import type { Tag } from '@gal/shared'

const typeLabels: Record<number, string> = { 0: '风格', 1: '题材', 2: '其它' }

function tagTypeLabel(type: number): string {
  return typeLabels[type] ?? '其它'
}

function tagTypeColor(type: number): 'primary' | 'success' | 'info' {
  if (type === 0) return 'primary'
  if (type === 1) return 'success'
  return 'info'
}

const loading = ref(false)
const list = ref<Tag[]>([])
const selection = ref<Tag[]>([])

async function load() {
  loading.value = true
  try {
    const res = (await getTagList({ pageNum: 1, pageSize: 500 })) as unknown as
      | { rows?: Tag[] }
      | Tag[]
    list.value = Array.isArray(res) ? res : (res.rows ?? [])
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '标签列表加载失败')
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
  tagId: undefined as number | undefined,
  name: '',
  type: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择标签类型', trigger: 'change' }],
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { tagId: undefined, name: '', type: 0 })
  dialogVisible.value = true
}

function openEdit(row: Tag) {
  isEdit.value = true
  Object.assign(form, { tagId: row.tagId, name: row.name ?? '', type: Number(row.type) || 0 })
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTag({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createTag({ ...form })
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

async function removeRows(rows: Tag[]) {
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 个标签？`, '删除确认', { type: 'warning' })
    await deleteTags(rows.map((r) => r.tagId).join(','))
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
      <span class="tip">标签用于游戏筛选，类型分为「风格 / 题材 / 其它」。</span>
      <span class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button>
      <el-button type="danger" plain :disabled="!selection.length" @click="removeRows(selection)">批量删除</el-button>
    </div>

    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @selection-change="(rows: Tag[]) => (selection = rows)"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="tagId" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" min-width="200" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="tagTypeColor(Number(row.type))" size="small">{{ tagTypeLabel(Number(row.type)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="removeRows([row])">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑标签' : '新增标签'" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：废萌、剧情、科幻" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="风格" :value="0" />
            <el-option label="题材" :value="1" />
            <el-option label="其它" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="grad-btn" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tip {
  font-size: 13px;
  color: #8a8aa3;
}
</style>
