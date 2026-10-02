<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  createGame,
  deleteGames,
  getAllTags,
  getBrandList,
  getGameList,
  resolveAssetUrl,
  updateGame,
} from '@gal/shared'
import type { Brand, Game, Tag } from '@gal/shared'

const loading = ref(false)
const list = ref<Game[]>([])
const total = ref(0)
const brands = ref<Brand[]>([])
const tags = ref<Tag[]>([])
const selection = ref<Game[]>([])

const query = reactive({
  keyword: '',
  brandId: undefined as number | undefined,
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getGameList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
      brandId: query.brandId,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '游戏列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const res = (await getAllTags()) as unknown as { data?: Tag[] } | Tag[]
    tags.value = (Array.isArray(res) ? res : res?.data) ?? []
  } catch {
    tags.value = []
  }
  try {
    const res = (await getBrandList()) as unknown as { rows?: Brand[]; data?: Brand[] } | Brand[]
    brands.value = Array.isArray(res) ? res : (res?.rows ?? res?.data ?? [])
  } catch {
    brands.value = []
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.brandId = undefined
  query.pageNum = 1
  load()
}

/* ---------- 编辑弹窗 ---------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<{
  gameId?: number
  title: string
  titleCn: string
  cover: string
  releaseDate: string
  brandId?: number
  tagIds: number[]
  summary: string
  staffPaint: string
  staffScenario: string
  staffVoice: string
}>({
  title: '',
  titleCn: '',
  cover: '',
  releaseDate: '',
  brandId: undefined,
  tagIds: [],
  summary: '',
  staffPaint: '',
  staffScenario: '',
  staffVoice: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入游戏标题', trigger: 'blur' }],
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    gameId: undefined,
    title: '',
    titleCn: '',
    cover: '',
    releaseDate: '',
    brandId: undefined,
    tagIds: [],
    summary: '',
    staffPaint: '',
    staffScenario: '',
    staffVoice: '',
  })
  dialogVisible.value = true
}

function openEdit(row: Game) {
  isEdit.value = true
  Object.assign(form, {
    gameId: row.gameId,
    title: row.title ?? '',
    titleCn: row.titleCn ?? '',
    cover: row.cover ?? '',
    releaseDate: row.releaseDate ? row.releaseDate.slice(0, 10) : '',
    brandId: row.brandId ?? undefined,
    tagIds: (row.tags ?? []).map((t) => t.tagId),
    summary: row.summary ?? '',
    staffPaint: row.staffPaint ?? '',
    staffScenario: row.staffScenario ?? '',
    staffVoice: row.staffVoice ?? '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  const payload = {
    gameId: form.gameId,
    title: form.title,
    titleCn: form.titleCn || undefined,
    cover: form.cover || undefined,
    releaseDate: form.releaseDate || undefined,
    brandId: form.brandId,
    tagIds: form.tagIds,
    summary: form.summary || undefined,
    staffPaint: form.staffPaint || undefined,
    staffScenario: form.staffScenario || undefined,
    staffVoice: form.staffVoice || undefined,
  }
  try {
    if (isEdit.value) {
      await updateGame(payload)
      ElMessage.success('修改成功')
    } else {
      await createGame(payload)
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

/* ---------- 删除 ---------- */
async function removeRows(rows: Game[]) {
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 个游戏？`, '删除确认', { type: 'warning' })
    await deleteGames(rows.map((r) => r.gameId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch {
    /* 取消 */
  }
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<template>
  <div class="page">
    <!-- 筛选栏 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.keyword"
        placeholder="搜索标题 / 中文名"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="query.brandId" placeholder="全部会社" clearable style="width: 160px">
        <el-option v-for="b in brands" :key="b.brandId" :label="b.nameCn || b.name" :value="b.brandId" />
      </el-select>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增游戏</el-button>
      <el-button type="danger" plain :disabled="!selection.length" @click="removeRows(selection)">
        批量删除
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @selection-change="(rows: Game[]) => (selection = rows)"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="gameId" label="ID" width="70" />
        <el-table-column label="封面" width="66">
          <template #default="{ row }">
            <el-image
              v-if="resolveAssetUrl(row.cover)"
              :src="resolveAssetUrl(row.cover)"
              fit="cover"
              style="width: 44px; height: 58px; border-radius: 4px"
              :preview-src-list="[resolveAssetUrl(row.cover)]"
              preview-teleported
            />
            <div v-else style="width: 44px; height: 58px; border-radius: 4px; background: #f0f0f8; display: flex; align-items: center; justify-content: center">
              🎮
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="titleCn" label="中文名" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.titleCn || '—' }}</template>
        </el-table-column>
        <el-table-column prop="brandName" label="会社" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.brandName || '—' }}</template>
        </el-table-column>
        <el-table-column label="评分" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.ratingAvg) > 0">★ {{ Number(row.ratingAvg).toFixed(1) }}（{{ row.ratingCount || 0 }}）</span>
            <span v-else class="text-dim">暂无</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="releaseDate" label="发售日" width="110">
          <template #default="{ row }">{{ row.releaseDate ? row.releaseDate.slice(0, 10) : '—' }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="130">
          <template #default="{ row }">
            <el-tag v-for="t in (row.tags || []).slice(0, 3)" :key="t.tagId" size="small" class="cell-tag">
              {{ t.name }}
            </el-tag>
            <span v-if="!(row.tags || []).length" class="text-dim">—</span>
          </template>
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
          :current-page="query.pageNum"
          @current-change="(p: number) => { query.pageNum = p; load() }"
        />
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑游戏' : '新增游戏'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="日文原名 / 英文名" />
        </el-form-item>
        <el-form-item label="中文名">
          <el-input v-model="form.titleCn" placeholder="中文译名（可选）" />
        </el-form-item>
        <el-form-item label="封面">
          <el-input v-model="form.cover" placeholder="封面图片 URL（/profile/... 或 http(s)）" />
          <div v-if="form.cover" class="cover-preview">
            <el-image :src="resolveAssetUrl(form.cover)" fit="cover" style="width: 90px; height: 120px; border-radius: 6px" />
          </div>
        </el-form-item>
        <el-form-item label="会社">
          <el-select v-model="form.brandId" placeholder="选择会社" clearable style="width: 100%">
            <el-option v-for="b in brands" :key="b.brandId" :label="b.nameCn || b.name" :value="b.brandId" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple placeholder="选择标签（可多选）" style="width: 100%">
            <el-option v-for="t in tags" :key="t.tagId" :label="t.name" :value="t.tagId" />
          </el-select>
        </el-form-item>
        <el-form-item label="发售日">
          <el-date-picker
            v-model="form.releaseDate"
            type="date"
            placeholder="选择发售日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" type="textarea" :rows="4" placeholder="游戏简介" />
        </el-form-item>
        <el-form-item label="原画">
          <el-input v-model="form.staffPaint" placeholder="原画师" />
        </el-form-item>
        <el-form-item label="剧本">
          <el-input v-model="form.staffScenario" placeholder="剧本作者" />
        </el-form-item>
        <el-form-item label="声优">
          <el-input v-model="form.staffVoice" placeholder="主要声优" />
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
.cell-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}

.cover-preview {
  margin-top: 8px;
}
</style>
