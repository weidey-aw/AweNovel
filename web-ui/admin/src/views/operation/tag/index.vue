<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createTag,
  deleteTags,
  getAdminTagList,
  getTagDetail,
  updateTag,
} from '@gal/shared'
import type { Tag } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/** 标签类型（与 gal_tag.type 对应） */
const TAG_TYPES = [
  { value: 'theme', label: '题材' },
  { value: 'play', label: '玩法' },
  { value: 'type', label: '类型' },
]

function typeLabel(type?: string) {
  return TAG_TYPES.find((t) => t.value === type)?.label ?? '其它'
}

function typeTag(type?: string): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'theme') return 'primary'
  if (type === 'play') return 'success'
  if (type === 'type') return 'warning'
  return 'info'
}

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<Tag[]>([])
const total = ref(0)
const selection = ref<Tag[]>([])
const query = reactive({ name: '', type: '', pageNum: 1 })
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getAdminTagList({
      pageNum: query.pageNum,
      pageSize,
      name: query.name || undefined,
      type: query.type || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '标签列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.name = ''
  query.type = ''
  query.pageNum = 1
  load()
}

/* ==================== 新增 / 修改 ==================== */
const dialog = reactive({ visible: false, isEdit: false, loading: false })
const formRef = ref<FormInstance>()
const form = reactive<Partial<Tag>>({ tagId: undefined, name: '', type: 'theme' })
const rules: FormRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { max: 50, message: '名称不能超过 50 个字符', trigger: 'blur' },
  ],
}

function openAdd() {
  dialog.isEdit = false
  dialog.visible = true
  Object.assign(form, { tagId: undefined, name: '', type: 'theme' })
  formRef.value?.clearValidate()
}

async function openEdit(row: Tag) {
  dialog.isEdit = true
  dialog.visible = true
  formRef.value?.clearValidate()
  try {
    const res = await getTagDetail(row.tagId)
    const detail = res.data ?? row
    Object.assign(form, {
      tagId: detail.tagId,
      name: detail.name,
      type: detail.type ?? 'theme',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '标签详情加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialog.loading = true
  try {
    if (dialog.isEdit) {
      await updateTag({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createTag({ ...form })
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

/* ==================== 删除 ==================== */
async function handleDelete(rows?: Tag[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的标签')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 个标签吗？删除后会同时解除其与游戏的关联。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteTags(targets.map((t) => t.tagId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.name"
        placeholder="标签名称"
        clearable
        style="width: 200px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.type" placeholder="标签类型" clearable style="width: 140px" @change="search">
        <el-option v-for="t in TAG_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('community:tag:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button
        v-if="can('community:tag:remove')"
        type="danger"
        plain
        :disabled="!selection.length"
        @click="handleDelete()"
      >
        删除
      </el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: Tag[]) => (selection = r)">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="tagId" label="ID" width="70" />
        <el-table-column prop="name" label="标签名称" min-width="160" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联游戏" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="(row.gameCount ?? 0) > 0 ? 'success' : 'info'">
              {{ row.gameCount ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('community:tag:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="can('community:tag:remove')" link type="danger" :icon="Delete" @click="handleDelete([row])">
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
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '修改标签' : '新增标签'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="如：恋爱" maxlength="50" />
        </el-form-item>
        <el-form-item label="标签类型">
          <el-radio-group v-model="form.type">
            <el-radio v-for="t in TAG_TYPES" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
          </el-radio-group>
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
