<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture, View } from '@element-plus/icons-vue'
import {
  CODE_LANGUAGES,
  mdCodeBlock,
  publishArticle,
  renderMarkdown,
  resolveAssetUrl,
  uploadFile,
} from '@gal/shared'
import type { Article } from '@gal/shared'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const CATEGORIES = [
  { value: 'news', label: '资讯' },
  { value: 'review', label: '评测' },
  { value: 'guide', label: '攻略' },
]

const form = ref<Partial<Article>>({
  title: '',
  category: 'news',
  summary: '',
  cover: '',
  content: '',
})
const submitting = ref(false)
const uploading = ref(false)
const showPreview = ref(false)
const coverInputRef = ref<HTMLInputElement>()
const contentRef = ref<{ textarea?: HTMLTextAreaElement }>()
const codeLang = ref('javascript')

/** 在正文光标处插入文本 */
function insertText(text: string) {
  const ta = contentRef.value?.textarea
  const current = form.value.content ?? ''
  if (!ta) {
    form.value.content = current + text
    return
  }
  const start = ta.selectionStart ?? current.length
  const end = ta.selectionEnd ?? current.length
  form.value.content = current.slice(0, start) + text + current.slice(end)
  requestAnimationFrame(() => {
    ta.focus()
    const pos = start + text.length
    ta.setSelectionRange(pos, pos)
  })
}

/** 包裹选中文本（加粗/斜体/行内代码） */
function wrapText(prefix: string, suffix = prefix, fallback = '文字') {
  const ta = contentRef.value?.textarea
  const current = form.value.content ?? ''
  if (!ta) {
    form.value.content = current + prefix + fallback + suffix
    return
  }
  const start = ta.selectionStart ?? 0
  const end = ta.selectionEnd ?? 0
  const selected = current.slice(start, end) || fallback
  form.value.content = current.slice(0, start) + prefix + selected + suffix + current.slice(end)
  requestAnimationFrame(() => {
    ta.focus()
    ta.setSelectionRange(start + prefix.length, start + prefix.length + selected.length)
  })
}

/** 插入代码块（带语言标记，预览自动高亮） */
function insertCodeBlock() {
  insertText(mdCodeBlock(codeLang.value))
}

const previewHtml = computed(() => renderMarkdown(form.value.content))

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
    form.value.cover = res.fileName || res.url
    ElMessage.success('封面已上传')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

async function submit() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再发布文章')
    router.push({ path: '/login', query: { redirect: '/article/new' } })
    return
  }
  if (!form.value.title?.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.value.content?.trim()) {
    ElMessage.warning('请填写正文')
    return
  }
  submitting.value = true
  try {
    await publishArticle({
      title: form.value.title.trim(),
      category: form.value.category,
      summary: form.value.summary,
      cover: form.value.cover,
      content: form.value.content,
    })
    ElMessage.success('已提交审核，通过后会展示在文章列表')
    router.push('/articles')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '发布失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-button text :icon="ArrowLeft" class="back-btn" @click="router.back()">返回</el-button>

    <div class="section-title">写文章</div>

    <div class="editor-card panel-card">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="起个吸引人的标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="摘要（可选）">
              <el-input v-model="form.summary" placeholder="一句话介绍这篇文章" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="封面（可选）">
          <div class="cover-row">
            <el-image v-if="form.cover" :src="resolveAssetUrl(form.cover)" fit="cover" class="cover-preview" />
            <el-button :icon="Picture" :loading="uploading" @click="pickCover">
              {{ form.cover ? '更换封面' : '上传封面' }}
            </el-button>
            <el-button v-if="form.cover" text type="danger" @click="form.cover = ''">移除</el-button>
            <input ref="coverInputRef" type="file" accept="image/*" hidden @change="onCoverChange" />
          </div>
        </el-form-item>

        <el-form-item label="正文（支持 Markdown）">
          <div class="editor-toolbar">
            <el-button size="small" text @click="insertText('## ')">H2</el-button>
            <el-button size="small" text @click="wrapText('**', '**', '加粗文字')">B</el-button>
            <el-button size="small" text @click="wrapText('*', '*', '斜体文字')"><i>I</i></el-button>
            <el-button size="small" text @click="wrapText('`', '`', 'code')">&lt;/&gt;</el-button>
            <el-button size="small" text @click="insertText('\n- ')">• 列表</el-button>
            <el-button size="small" text @click="insertText('\n> ')">❝ 引用</el-button>
            <el-button size="small" text @click="insertText('\n---\n')">—</el-button>
            <el-select v-model="codeLang" size="small" class="code-lang">
              <el-option v-for="l in CODE_LANGUAGES" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
            <el-button size="small" text @click="insertCodeBlock">{ } 代码块</el-button>
          </div>
          <el-input
            ref="contentRef"
            v-model="form.content"
            type="textarea"
            :rows="16"
            placeholder="支持 Markdown：# 标题、**加粗**、- 列表、![图片](url)、```代码块```（自动高亮）"
          />
        </el-form-item>
      </el-form>

      <div class="editor-actions">
        <el-button :icon="View" @click="showPreview = !showPreview">
          {{ showPreview ? '关闭预览' : '预览' }}
        </el-button>
        <span class="hint">提交后进入「待审核」，通过后才会展示在文章列表</span>
        <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
      </div>

      <div v-if="showPreview" class="preview-box">
        <h2 class="pv-title">{{ form.title || '未命名文章' }}</h2>
        <div v-if="form.cover" class="pv-cover">
          <img :src="resolveAssetUrl(form.cover)" alt="" />
        </div>
        <div class="md-body" v-html="previewHtml || '<p class=\'text-dim\'>正文为空</p>'" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.back-btn {
  margin-bottom: 10px;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  width: 100%;
  margin-bottom: 8px;
  padding: 6px 8px;
  border: 1px solid var(--border-glow);
  border-radius: 10px;
  background: var(--bg-soft);
}

.code-lang {
  width: 120px;
  margin: 0 4px;
}

.editor-card {
  padding: 22px 24px;
}

.cover-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cover-preview {
  width: 160px;
  height: 90px;
  border-radius: 8px;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.hint {
  flex: 1;
  color: var(--text-dim);
  font-size: 12.5px;
  text-align: right;
}

.preview-box {
  margin-top: 22px;
  border-top: 1px dashed var(--border-glow);
  padding-top: 18px;
}

.pv-title {
  margin: 0 0 12px;
  font-size: 22px;
}

.pv-cover {
  margin-bottom: 14px;
  border-radius: 10px;
  overflow: hidden;
  max-height: 320px;
}

.pv-cover img {
  width: 100%;
  object-fit: cover;
}

.md-body {
  line-height: 1.95;
  color: var(--text-sub);
  font-size: 15px;
  word-break: break-word;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  margin: 20px 0 10px;
  color: var(--text-main);
}

.md-body :deep(.md-img) {
  max-width: 100%;
  border-radius: 10px;
  display: block;
  margin: 12px 0;
}

.md-body :deep(.md-video) {
  width: 100%;
  border-radius: 10px;
  margin: 12px 0;
  background: #000;
}

.md-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 14px;
  border-left: 3px solid var(--accent);
  background: var(--accent-soft);
  border-radius: 6px;
}

.md-body :deep(code) {
  background: var(--accent-soft);
  color: var(--accent);
  padding: 1px 6px;
  border-radius: 4px;
}

.md-body :deep(.md-code) {
  background: var(--overlay-strong);
  padding: 14px 16px;
  border-radius: 8px;
  overflow-x: auto;
}
</style>
