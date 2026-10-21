<script setup lang="ts">
/**
 * 游戏编辑页（整页编辑，替代原弹窗）
 * 布局：左「简介 Markdown 编辑器」· 中「前台效果预览」· 右「基本信息」
 * 封面 3:4 / 轮播图 16:9，裁剪后统一以 WebP 上传
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Delete, Plus, Upload } from '@element-plus/icons-vue'
import {
  createGame,
  getAllTags,
  getAdminGameDetail,
  getBrandList,
  publishGame,
  resolveAssetUrl,
  updateGame,
  uploadFile,
} from '@gal/shared'
import type { Brand, Game, Tag } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

const STATUS_MAP: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  '0': { label: '已下架', type: 'info' },
  '1': { label: '已发布', type: 'success' },
  '2': { label: '待审核', type: 'warning' },
  '3': { label: '审核驳回', type: 'danger' },
  '4': { label: '草稿', type: 'info' },
}

const MAX_CAROUSEL = 7

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const brands = ref<Brand[]>([])
const tags = ref<Tag[]>([])
const carousel = ref<string[]>([])

const form = reactive<Partial<Game>>({
  gameId: undefined,
  title: '',
  titleCn: '',
  cover: '',
  releaseDate: '',
  brandId: undefined,
  summary: '',
  staffPaint: '',
  staffScenario: '',
  staffVoice: '',
  tagIds: [],
  status: '4',
})

const isNew = computed(() => !form.gameId)
const statusInfo = computed(() => STATUS_MAP[form.status ?? '4'] ?? STATUS_MAP['4'])
const brandName = computed(() => brands.value.find((b) => b.brandId === form.brandId)?.name || '未选择会社')
const selectedTags = computed(() => tags.value.filter((t) => (form.tagIds ?? []).includes(t.tagId)))
const previewHtml = computed(() => renderMarkdown(form.summary))

function parseCarousel(raw?: string | null): string[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((item): item is string => typeof item === 'string') : []
  } catch {
    return raw
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
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

async function loadDetail() {
  const id = Number(route.query.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getAdminGameDetail(id)
    const game = res.data
    if (!game) {
      ElMessage.error('游戏不存在')
      return
    }
    Object.assign(form, {
      gameId: game.gameId,
      title: game.title,
      titleCn: game.titleCn ?? '',
      cover: game.cover ?? '',
      releaseDate: game.releaseDate ?? '',
      brandId: game.brandId,
      summary: game.summary ?? '',
      staffPaint: game.staffPaint ?? '',
      staffScenario: game.staffScenario ?? '',
      staffVoice: game.staffVoice ?? '',
      tagIds: (game.tags ?? []).map((t) => t.tagId),
      status: game.status ?? '4',
    })
    carousel.value = parseCarousel(game.carousel)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '游戏详情加载失败')
  } finally {
    loading.value = false
  }
}

/* ==================== 图片裁剪上传（封面 / 轮播图） ==================== */
const crop = reactive({
  visible: false,
  mode: 'cover' as 'cover' | 'carousel',
  cropSrc: '',
  uploading: false,
})
const cropperRef = ref<{ toFile: (name: string) => Promise<File | null> } | null>(null)
const fileInputRef = ref<HTMLInputElement>()

const cropAspect = computed(() => (crop.mode === 'cover' ? 3 / 4 : 16 / 9))
const cropWidth = computed(() => (crop.mode === 'cover' ? 900 : 1600))

function openCrop(mode: 'cover' | 'carousel') {
  if (mode === 'carousel' && carousel.value.length >= MAX_CAROUSEL) {
    ElMessage.warning(`轮播图最多 ${MAX_CAROUSEL} 张`)
    return
  }
  crop.mode = mode
  fileInputRef.value?.click()
}

function onFilePicked(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('原图大小不能超过 20MB')
    return
  }
  if (crop.cropSrc) URL.revokeObjectURL(crop.cropSrc)
  crop.cropSrc = URL.createObjectURL(file)
  crop.visible = true
}

async function submitCrop() {
  const cropper = cropperRef.value
  if (!cropper) {
    ElMessage.warning('请先选择图片')
    return
  }
  crop.uploading = true
  try {
    const file = await cropper.toFile(crop.mode === 'cover' ? 'game-cover' : 'game-carousel')
    if (!file) {
      ElMessage.error('裁剪失败，请重新选择图片')
      return
    }
    const uploaded = await uploadFile(file)
    const path = uploaded.fileName || uploaded.url
    if (crop.mode === 'cover') {
      form.cover = path
      ElMessage.success('封面已更新')
    } else {
      carousel.value.push(path)
      ElMessage.success('轮播图已添加')
    }
    crop.visible = false
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    crop.uploading = false
  }
}

function onCropClosed() {
  if (crop.cropSrc) {
    URL.revokeObjectURL(crop.cropSrc)
    crop.cropSrc = ''
  }
}

function removeCarousel(index: number) {
  carousel.value.splice(index, 1)
}

function moveCarousel(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= carousel.value.length) return
  const [item] = carousel.value.splice(index, 1)
  carousel.value.splice(target, 0, item)
}

/* ==================== 保存 / 发布 ==================== */
async function persist(status: string): Promise<boolean> {
  if (!form.title?.trim()) {
    ElMessage.warning('请填写游戏原名')
    return false
  }
  const payload: Partial<Game> = {
    gameId: form.gameId,
    title: form.title.trim(),
    titleCn: form.titleCn,
    cover: form.cover,
    releaseDate: form.releaseDate,
    brandId: form.brandId,
    summary: form.summary,
    staffPaint: form.staffPaint,
    staffScenario: form.staffScenario,
    staffVoice: form.staffVoice,
    tagIds: form.tagIds ?? [],
    carousel: carousel.value.length ? JSON.stringify(carousel.value) : '',
    status,
  }
  if (!form.gameId) {
    const res = await createGame(payload)
    const created = res.data
    if (created?.gameId) form.gameId = created.gameId
  } else {
    await updateGame(payload)
  }
  form.status = status
  return true
}

async function onSaveDraft() {
  saving.value = true
  try {
    if (await persist('4')) {
      ElMessage.success('草稿已保存')
      if (isNew.value && form.gameId) {
        router.replace({ path: '/operation/game/edit', query: { id: String(form.gameId) } })
      }
    }
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onPublish() {
  if (!form.cover) {
    ElMessage.warning('发布前请先上传封面')
    return
  }
  publishing.value = true
  try {
    if (!(await persist('4'))) return
    await publishGame(form.gameId as number)
    form.status = '2'
    ElMessage.success('已提交审核，审核通过后前台可见')
    if (isNew.value && form.gameId) {
      router.replace({ path: '/operation/game/edit', query: { id: String(form.gameId) } })
    }
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '发布失败')
  } finally {
    publishing.value = false
  }
}

function goBack() {
  router.push('/operation/game')
}

onMounted(() => {
  loadOptions()
  loadDetail()
})
</script>

<template>
  <div v-loading="loading" class="game-editor">
    <!-- 顶部操作条 -->
    <div class="editor-head">
      <div class="head-left">
        <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
        <span class="head-title">{{ isNew ? '新增游戏' : '编辑游戏' }}</span>
        <el-tag :type="statusInfo.type" size="small">{{ statusInfo.label }}</el-tag>
        <span v-if="form.gameId" class="head-id">#{{ form.gameId }}</span>
      </div>
      <div class="head-actions">
        <span class="head-tip">发布后进入「待审核」，审核通过才在前台展示</span>
        <el-button v-if="can('community:game:edit')" :loading="saving" @click="onSaveDraft">存草稿</el-button>
        <el-button v-if="can('community:game:publish')" type="primary" :loading="publishing" @click="onPublish">
          发布
        </el-button>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左：简介 -->
      <section class="col col-md">
        <div class="col-head">
          <span>简介</span>
          <span class="col-sub">Markdown · 支持图片/视频</span>
        </div>
        <MarkdownEditor v-model="form.summary" :height="520" class="md-editor-box" />
      </section>

      <!-- 中：预览 -->
      <section class="col col-preview">
        <div class="col-head">
          <span>前台预览</span>
          <span class="col-sub">实时</span>
        </div>
        <div class="preview-card">
          <div class="pv-cover">
            <img v-if="form.cover" :src="resolveAssetUrl(form.cover)" alt="" />
            <div v-else class="pv-cover-empty">暂无封面</div>
          </div>
          <h1 class="pv-title">{{ form.title || '游戏原名' }}</h1>
          <p class="pv-sub">{{ form.titleCn || '游戏译名' }}</p>
          <div class="pv-meta">
            <span>{{ brandName }}</span>
            <span class="dot">·</span>
            <span>{{ form.releaseDate || '发售日期' }}</span>
            <span class="dot">·</span>
            <span>{{ form.ratingAvg ?? '—' }} 分</span>
          </div>
          <div class="pv-tags">
            <el-tag v-for="t in selectedTags" :key="t.tagId" size="small" effect="plain">{{ t.name }}</el-tag>
            <span v-if="!selectedTags.length" class="pv-empty">未选择标签</span>
          </div>
          <div v-if="carousel.length" class="pv-carousel">
            <img v-for="(item, index) in carousel" :key="index" :src="resolveAssetUrl(item)" alt="" />
          </div>
          <div class="pv-staff">
            <p v-if="form.staffPaint"><b>原画</b>{{ form.staffPaint }}</p>
            <p v-if="form.staffScenario"><b>剧本</b>{{ form.staffScenario }}</p>
            <p v-if="form.staffVoice"><b>声优</b>{{ form.staffVoice }}</p>
          </div>
          <div class="pv-summary md-body" v-html="previewHtml || '<p class=\'pv-empty\'>暂无简介</p>'" />
        </div>
      </section>

      <!-- 右：基本信息 -->
      <section class="col col-form">
        <div class="col-head">
          <span>基本信息</span>
          <span class="col-sub">带 * 为必填</span>
        </div>
        <el-form label-position="top" class="info-form">
          <el-form-item label="游戏原名 *">
            <el-input v-model="form.title" placeholder="如：Summer Pockets" maxlength="255" />
          </el-form-item>
          <el-form-item label="游戏译名">
            <el-input v-model="form.titleCn" placeholder="中文译名" maxlength="255" />
          </el-form-item>
          <el-form-item label="制作会社">
            <el-select v-model="form.brandId" placeholder="选择会社" clearable filterable style="width: 100%">
              <el-option v-for="b in brands" :key="b.brandId" :label="b.name" :value="b.brandId" />
            </el-select>
          </el-form-item>
          <el-form-item label="发售日期">
            <el-date-picker
              v-model="form.releaseDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择发售日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="标签">
            <el-select
              v-model="form.tagIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择标签"
              style="width: 100%"
            >
              <el-option v-for="t in tags" :key="t.tagId" :label="t.name" :value="t.tagId" />
            </el-select>
          </el-form-item>
          <el-form-item label="原画">
            <el-input v-model="form.staffPaint" placeholder="原画师，多个用顿号分隔" />
          </el-form-item>
          <el-form-item label="剧本">
            <el-input v-model="form.staffScenario" placeholder="剧本作者" />
          </el-form-item>
          <el-form-item label="主要声优">
            <el-input v-model="form.staffVoice" placeholder="声优列表" />
          </el-form-item>

          <!-- 封面 -->
          <div class="block-head">封面（3:4 · WebP）</div>
          <div class="cover-box" @click="openCrop('cover')">
            <img v-if="form.cover" :src="resolveAssetUrl(form.cover)" alt="" />
            <div v-else class="cover-empty">
              <el-icon><Upload /></el-icon>
              <span>上传封面</span>
            </div>
            <span class="cover-mask">{{ form.cover ? '更换封面' : '上传封面' }}</span>
          </div>

          <!-- 轮播图 -->
          <div class="block-head">
            轮播图（16:9 · WebP）
            <span class="block-sub">{{ carousel.length }}/{{ MAX_CAROUSEL }}</span>
          </div>
          <div class="carousel-grid">
            <div v-for="(item, index) in carousel" :key="`${item}-${index}`" class="carousel-item">
              <img :src="resolveAssetUrl(item)" alt="" />
              <div class="carousel-actions">
                <el-button text size="small" @click.stop="moveCarousel(index, -1)">←</el-button>
                <el-button text size="small" @click.stop="moveCarousel(index, 1)">→</el-button>
                <el-button text size="small" type="danger" :icon="Delete" @click.stop="removeCarousel(index)" />
              </div>
            </div>
            <div v-if="carousel.length < MAX_CAROUSEL" class="carousel-add" @click="openCrop('carousel')">
              <el-icon><Plus /></el-icon>
              <span>添加</span>
            </div>
          </div>
        </el-form>
      </section>
    </div>

    <!-- 裁剪弹窗 -->
    <el-dialog
      v-model="crop.visible"
      :title="crop.mode === 'cover' ? '裁剪封面' : '裁剪轮播图'"
      width="440px"
      append-to-body
      @closed="onCropClosed"
    >
      <div class="crop-box" v-loading="crop.uploading">
        <ImageCropper
          v-if="crop.cropSrc"
          ref="cropperRef"
          :src="crop.cropSrc"
          :aspect="cropAspect"
          :out-width="cropWidth"
        />
      </div>
      <template #footer>
        <el-button @click="openCrop(crop.mode)">重新选择</el-button>
        <el-button @click="crop.visible = false">取消</el-button>
        <el-button type="primary" :loading="crop.uploading" @click="submitCrop">上传</el-button>
      </template>
    </el-dialog>

    <input ref="fileInputRef" type="file" accept="image/*" hidden @change="onFilePicked" />
  </div>
</template>

<style scoped>
.game-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eeeef7;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.head-title {
  font-weight: 700;
  color: #2b2b3c;
}

.head-id {
  color: #a0a0b8;
  font-size: 12px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.head-tip {
  color: #9a9ab0;
  font-size: 12px;
  margin-right: 4px;
}

.editor-body {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.col {
  background: #fff;
  border: 1px solid #eeeef7;
  border-radius: 12px;
  padding: 12px 14px 16px;
  min-height: 640px;
}

.col-md {
  flex: 1.15;
  min-width: 0;
}

.col-preview {
  flex: 1;
  min-width: 0;
}

.col-form {
  width: 360px;
  flex-shrink: 0;
}

.col-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-weight: 600;
  color: #2b2b3c;
  padding-bottom: 10px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f2f2f8;
}

.col-sub {
  color: #9a9ab0;
  font-size: 12px;
  font-weight: 400;
}

.md-editor-box {
  height: 100%;
}

.preview-card {
  padding: 4px 2px;
}

.pv-cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  max-height: 320px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5fb;
  margin-bottom: 12px;
}

.pv-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pv-cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a8a8bf;
  font-size: 13px;
}

.pv-title {
  margin: 0 0 4px;
  font-size: 20px;
  color: #2b2b3c;
}

.pv-sub {
  margin: 0 0 8px;
  color: #8a8aa3;
  font-size: 13px;
}

.pv-meta {
  color: #6b6b85;
  font-size: 12.5px;
  margin-bottom: 8px;
}

.dot {
  margin: 0 6px;
  color: #c8c8d8;
}

.pv-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.pv-empty {
  color: #b0b0c4;
  font-size: 12.5px;
}

.pv-carousel {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 8px;
}

.pv-carousel img {
  width: 150px;
  height: 84px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.pv-staff {
  color: #6b6b85;
  font-size: 12.5px;
  margin-bottom: 10px;
}

.pv-staff p {
  margin: 2px 0;
}

.pv-staff b {
  color: #8a8aa3;
  font-weight: 500;
  margin-right: 6px;
}

.md-body {
  color: #3a3a4e;
  font-size: 13.5px;
  line-height: 1.85;
  border-top: 1px dashed #ececf6;
  padding-top: 10px;
  word-break: break-word;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  margin: 12px 0 6px;
  color: #2b2b3c;
  font-size: 15px;
}

.md-body :deep(ul),
.md-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.md-body :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 10px;
  border-left: 3px solid #d8d0f8;
  background: #faf9ff;
  color: #6b6b85;
}

.md-body :deep(.md-img) {
  max-width: 100%;
  border-radius: 8px;
  display: block;
  margin: 8px 0;
}

.md-body :deep(.md-video) {
  width: 100%;
  border-radius: 8px;
  margin: 8px 0;
  background: #000;
}

.md-body :deep(code) {
  background: #f5f3fe;
  color: #6b4fe0;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12.5px;
}

.md-body :deep(.md-code) {
  background: #1e1e2a;
  color: #e8e8f0;
  padding: 10px 12px;
  border-radius: 8px;
  overflow-x: auto;
}

.info-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #4a4a5e;
  padding-bottom: 4px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #4a4a5e;
  margin: 6px 0 8px;
}

.block-sub {
  color: #9a9ab0;
  font-weight: 400;
  font-size: 12px;
}

.cover-box {
  position: relative;
  width: 132px;
  height: 176px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5fb;
  border: 1px dashed #d8d8ea;
  cursor: pointer;
  margin-bottom: 14px;
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #a0a0b8;
  font-size: 12.5px;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12.5px;
  opacity: 0;
  transition: opacity 0.15s;
}

.cover-box:hover .cover-mask {
  opacity: 1;
}

.carousel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.carousel-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5fb;
  aspect-ratio: 16 / 9;
}

.carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.carousel-actions {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.15s;
}

.carousel-item:hover .carousel-actions {
  opacity: 1;
}

.carousel-actions :deep(.el-button) {
  color: #fff;
  margin: 0;
  padding: 2px 6px;
}

.carousel-add {
  aspect-ratio: 16 / 9;
  border: 1px dashed #d8d8ea;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #a0a0b8;
  font-size: 12.5px;
  cursor: pointer;
  background: #fbfbfe;
}

.carousel-add:hover {
  border-color: #b9a8f5;
  color: #7c5cf0;
}

.crop-box {
  display: flex;
  justify-content: center;
  min-height: 420px;
  align-items: center;
}
</style>
