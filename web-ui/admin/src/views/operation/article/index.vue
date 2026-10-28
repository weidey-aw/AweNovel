<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import {
  changeArticleStatus,
  createArticle,
  deleteArticles,
  getAdminArticleDetail,
  getAdminArticleList,
  renderMarkdown,
  resolveAssetUrl,
  updateArticle,
  uploadFile,
} from '@gal/shared'
import type { Article } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

const CATEGORIES = [
  { value: 'news', label: '资讯' },
  { value: 'review', label: '评测' },
  { value: 'guide', label: '攻略' },
]

const STATUS_MAP: Record<string, { label: string; type: 'success' | 'warning' | 'danger' }> = {
  '0': { label: '待审核', type: 'warning' },
  '1': { label: '已发布', type: 'success' },
  '2': { label: '已拒绝', type: 'danger' },
}

const categoryLabel = (value?: string) => CATEGORIES.find((c) => c.value === value)?.label ?? '—'

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<Article[]>([])
const total = ref(0)
const selection = ref<Article[]>([])
const query = reactive({ keyword: '', category: '', status: '', pageNum: 1 })
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getAdminArticleList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      status: query.status || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '文章列表加载失败')
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
  query.category = ''
  query.status = ''
  query.pageNum = 1
  load()
}

/* ==================== 新增 / 修改 ==================== */
const dialog = reactive({ visible: false, isEdit: false, loading: false })
const formRef = ref<FormInstance>()
const form = reactive<Partial<Article>>({
  articleId: undefined,
  title: '',
  category: 'news',
  summary: '',
  cover: '',
  content: '',
  status: '1',
})
const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

const coverInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

function pickCover() {
  coverInputRef.value?.click()
}

async function onCoverChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('封面不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(file)
    form.cover = res.fileName || res.url
    ElMessage.success('封面已上传')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

function openAdd() {
  dialog.isEdit = false
  dialog.visible = true
  Object.assign(form, {
    articleId: undefined,
    title: '',
    category: 'news',
    summary: '',
    cover: '',
    content: '',
    status: '1',
  })
  formRef.value?.clearValidate()
}

async function openEdit(row: Article) {
  dialog.isEdit = true
  dialog.visible = true
  formRef.value?.clearValidate()
  try {
    const res = await getAdminArticleDetail(row.articleId)
    const detail = res.data ?? row
    Object.assign(form, {
      articleId: detail.articleId,
      title: detail.title,
      category: detail.category ?? 'news',
      summary: detail.summary ?? '',
      cover: detail.cover ?? '',
      content: detail.content ?? '',
      status: detail.status ?? '1',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '文章详情加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialog.loading = true
  try {
    if (dialog.isEdit) {
      await updateArticle({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createArticle({ ...form })
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

/* ==================== 状态 / 删除 / 预览 ==================== */
async function quickStatus(row: Article, status: string) {
  const label = STATUS_MAP[status]?.label ?? status
  try {
    await ElMessageBox.confirm(`确认将「${row.title}」设置为${label}吗？`, '状态修改', { type: 'warning' })
  } catch {
    return
  }
  try {
    await changeArticleStatus(row.articleId, status)
    ElMessage.success(`已设置为${label}`)
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

async function handleDelete(rows?: Article[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的文章')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 篇文章吗？文章下的评论会一并删除。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteArticles(targets.map((a) => a.articleId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

const preview = reactive({ visible: false, article: null as Article | null })

async function openPreview(row: Article) {
  preview.visible = true
  preview.article = row
  try {
    const res = await getAdminArticleDetail(row.articleId)
    if (res.data) preview.article = res.data
  } catch {
    /* 使用列表数据兜底 */
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
        placeholder="文章标题"
        clearable
        style="width: 200px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.category" placeholder="分类" clearable style="width: 130px" @change="search">
        <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
        <el-option label="待审核" value="0" />
        <el-option label="已发布" value="1" />
        <el-option label="已拒绝" value="2" />
      </el-select>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('community:article:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button
        v-if="can('community:article:remove')"
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
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: Article[]) => (selection = r)">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="articleId" label="ID" width="70" />
        <el-table-column label="封面" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.cover"
              :src="resolveAssetUrl(row.cover)"
              fit="cover"
              class="cover"
              :preview-src-list="[resolveAssetUrl(row.cover)]"
              preview-teleported
            />
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="120">
          <template #default="{ row }">{{ row.userName || `#${row.userId}` }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="STATUS_MAP[row.status ?? '0']?.type ?? 'info'">
              {{ STATUS_MAP[row.status ?? '0']?.label ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" align="center" />
        <el-table-column prop="commentCount" label="评论" width="80" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openPreview(row)">预览</el-button>
            <el-button v-if="can('community:article:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button
              v-if="can('community:article:edit') && row.status !== '1'"
              link
              type="success"
              @click="quickStatus(row, '1')"
            >
              通过
            </el-button>
            <el-button
              v-if="can('community:article:edit') && row.status !== '2'"
              link
              type="warning"
              @click="quickStatus(row, '2')"
            >
              拒绝
            </el-button>
            <el-button
              v-if="can('community:article:remove')"
              link
              type="danger"
              :icon="Delete"
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
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '修改文章' : '新增文章'" width="900px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="12">
          <el-col :span="16">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="文章标题" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="摘要">
              <el-input v-model="form.summary" placeholder="列表页展示的简介（可空）" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="待审核" value="0" />
                <el-option label="已发布" value="1" />
                <el-option label="已拒绝" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面">
              <div class="cover-row">
                <el-image v-if="form.cover" :src="resolveAssetUrl(form.cover)" fit="cover" class="cover-preview" />
                <el-button :loading="uploading" @click="pickCover">
                  {{ form.cover ? '更换封面' : '上传封面' }}
                </el-button>
                <el-button v-if="form.cover" text type="danger" @click="form.cover = ''">移除</el-button>
                <input ref="coverInputRef" type="file" accept="image/*" hidden @change="onCoverChange" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="block-head">
          正文
          <span class="block-sub">支持 Markdown 与图片/视频上传</span>
        </div>
        <MarkdownEditor v-model="form.content" :height="380" />
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="preview.visible" title="文章预览" width="780px" top="6vh">
      <div v-if="preview.article" class="preview">
        <h2 class="pv-title">{{ preview.article.title }}</h2>
        <div class="pv-meta">
          <el-tag size="small" effect="plain">{{ categoryLabel(preview.article.category) }}</el-tag>
          <span>{{ preview.article.userName || '—' }}</span>
          <span>{{ preview.article.createTime || '' }}</span>
          <span>浏览 {{ preview.article.viewCount ?? 0 }}</span>
          <span>评论 {{ preview.article.commentCount ?? 0 }}</span>
        </div>
        <p v-if="preview.article.summary" class="pv-summary">{{ preview.article.summary }}</p>
        <img v-if="preview.article.cover" :src="resolveAssetUrl(preview.article.cover)" class="pv-cover" alt="" />
        <div class="md-body" v-html="renderMarkdown(preview.article.content)" />
      </div>
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

.cover {
  width: 60px;
  height: 40px;
  border-radius: 6px;
}

.cover-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cover-preview {
  width: 120px;
  height: 70px;
  border-radius: 8px;
}

.block-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-weight: 600;
  color: #4a4a5e;
  margin: 4px 0 10px;
  padding-top: 12px;
  border-top: 1px solid #f2f2f8;
}

.block-sub {
  color: #9a9ab0;
  font-weight: 400;
  font-size: 12px;
}

.preview {
  max-height: 68vh;
  overflow-y: auto;
}

.pv-title {
  margin: 0 0 8px;
  font-size: 20px;
}

.pv-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: #8a8aa3;
  font-size: 12.5px;
  margin-bottom: 10px;
}

.pv-summary {
  color: #6b6b85;
  font-size: 13.5px;
  background: #faf9ff;
  border-left: 3px solid #d8d0f8;
  padding: 8px 12px;
  border-radius: 6px;
}

.pv-cover {
  max-width: 100%;
  border-radius: 10px;
  margin: 10px 0;
}

.md-body {
  line-height: 1.9;
  color: #3a3a4e;
  font-size: 14px;
  word-break: break-word;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  margin: 16px 0 8px;
}

.md-body :deep(.md-img) {
  max-width: 100%;
  border-radius: 8px;
  display: block;
  margin: 10px 0;
}

.md-body :deep(.md-video) {
  width: 100%;
  border-radius: 8px;
  margin: 10px 0;
  background: #000;
}

.md-body :deep(blockquote) {
  margin: 10px 0;
  padding: 6px 12px;
  border-left: 3px solid #d8d0f8;
  background: #faf9ff;
  color: #6b6b85;
}

.md-body :deep(code) {
  background: #f5f3fe;
  color: #6b4fe0;
  padding: 1px 5px;
  border-radius: 4px;
}

.md-body :deep(.md-code) {
  background: #1e1e2a;
  color: #e8e8f0;
  padding: 12px 14px;
  border-radius: 8px;
  overflow-x: auto;
}
</style>
