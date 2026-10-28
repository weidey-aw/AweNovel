<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Link, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import {
  createBrand,
  deleteBrands,
  getAdminBrandList,
  getBrandDetail,
  updateBrand,
  uploadFile,
} from '@gal/shared'
import type { Brand } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<Brand[]>([])
const total = ref(0)
const selection = ref<Brand[]>([])
const query = reactive({ keyword: '', pageNum: 1 })
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getAdminBrandList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '会社列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.pageNum = 1
  load()
}

/* ==================== 新增 / 修改 ==================== */
const dialog = reactive({ visible: false, isEdit: false, loading: false })
const formRef = ref<FormInstance>()
const form = reactive<Partial<Brand>>({
  brandId: undefined,
  name: '',
  website: '',
  logo: '',
  description: '',
})
const logoInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

const rules: FormRules = {
  name: [
    { required: true, message: '请输入会社名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过 100 个字符', trigger: 'blur' },
  ],
  website: [
    {
      validator: (_rule, value: string, callback) => {
        if (value && !/^https?:\/\/.+/i.test(value)) {
          callback(new Error('官网地址需以 http:// 或 https:// 开头'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function openAdd() {
  dialog.isEdit = false
  dialog.visible = true
  Object.assign(form, { brandId: undefined, name: '', website: '', logo: '', description: '' })
  formRef.value?.clearValidate()
}

async function openEdit(row: Brand) {
  dialog.isEdit = true
  dialog.visible = true
  formRef.value?.clearValidate()
  try {
    const res = await getBrandDetail(row.brandId)
    const detail = res.data ?? row
    Object.assign(form, {
      brandId: detail.brandId,
      name: detail.name,
      website: detail.website ?? '',
      logo: detail.logo ?? '',
      description: detail.description ?? '',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '会社详情加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialog.loading = true
  try {
    if (dialog.isEdit) {
      await updateBrand({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createBrand({ ...form })
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

/* ==================== Logo 上传 ==================== */
function pickLogo() {
  logoInputRef.value?.click()
}

async function onLogoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('Logo 不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(file)
    form.logo = res.fileName || res.url
    ElMessage.success('Logo 已上传')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

/* ==================== 删除 ==================== */
async function handleDelete(rows?: Brand[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的会社')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 个会社吗？会社下存在游戏时无法删除。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteBrands(targets.map((b) => b.brandId).join(','))
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
        v-model="query.keyword"
        placeholder="会社名称"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('community:brand:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button
        v-if="can('community:brand:remove')"
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
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: Brand[]) => (selection = r)">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="brandId" label="ID" width="70" />
        <el-table-column label="Logo" width="80" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row.logo" :size="40" shape="square" :src="row.logo" />
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="会社名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="官方网站" min-width="220">
          <template #default="{ row }">
            <a v-if="row.website" :href="row.website" target="_blank" rel="noopener noreferrer" class="site-link">
              <el-icon><Link /></el-icon>{{ row.website }}
            </a>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="游戏数" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.gameCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('community:brand:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button
              v-if="can('community:brand:remove')"
              link
              type="danger"
              :icon="Delete"
              :disabled="(row.gameCount ?? 0) > 0"
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
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '修改会社' : '新增会社'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="会社名称" prop="name">
          <el-input v-model="form.name" placeholder="如：Key" maxlength="100" />
        </el-form-item>
        <el-form-item label="官方网站" prop="website">
          <el-input v-model="form.website" placeholder="https://example.com（可空）" />
        </el-form-item>
        <el-form-item label="Logo">
          <div class="logo-row">
            <el-avatar v-if="form.logo" :size="56" shape="square" :src="form.logo" />
            <el-button :icon="Upload" :loading="uploading" @click="pickLogo">
              {{ form.logo ? '更换 Logo' : '上传 Logo' }}
            </el-button>
            <el-button v-if="form.logo" text type="danger" @click="form.logo = ''">移除</el-button>
            <input ref="logoInputRef" type="file" accept="image/*" hidden @change="onLogoChange" />
          </div>
          <div class="hint">建议正方形图片，不超过 5MB</div>
        </el-form-item>
        <el-form-item label="会社简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="会社简介（可空）" />
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

.muted {
  color: #a0a0b8;
}

.site-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #6b4fe0;
  text-decoration: none;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.site-link:hover {
  text-decoration: underline;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hint {
  color: #9a9ab0;
  font-size: 12px;
  margin-top: 4px;
}
</style>
