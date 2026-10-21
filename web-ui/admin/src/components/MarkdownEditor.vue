<script setup lang="ts">
/**
 * 极简 Markdown 编辑器（游戏简介用）
 * - 工具栏：标题 / 加粗 / 斜体 / 引用 / 列表 / 代码 / 图片上传 / 视频上传
 * - 图片与视频通过 /common/upload 上传，返回相对路径后插入 Markdown
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, VideoCamera } from '@element-plus/icons-vue'
import { uploadFile } from '@gal/shared'
import { mdImage, mdVideo } from '@/utils/markdown'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    height?: number
  }>(),
  {
    placeholder: '支持 Markdown：## 标题、**加粗**、- 列表、![图片](url)；图片/视频可用工具栏上传',
    height: 420,
  },
)

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const taRef = ref<HTMLTextAreaElement>()
const imgInputRef = ref<HTMLInputElement>()
const videoInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

function update(value: string) {
  emit('update:modelValue', value)
}

/** 在光标处插入文本 */
function insert(text: string) {
  const ta = taRef.value
  const current = props.modelValue ?? ''
  if (!ta) {
    update(current + text)
    return
  }
  const start = ta.selectionStart ?? current.length
  const end = ta.selectionEnd ?? current.length
  update(current.slice(0, start) + text + current.slice(end))
  requestAnimationFrame(() => {
    ta.focus()
    const pos = start + text.length
    ta.setSelectionRange(pos, pos)
  })
}

/** 用标记包裹选中文本 */
function wrap(prefix: string, suffix = prefix, fallback = '') {
  const ta = taRef.value
  const current = props.modelValue ?? ''
  if (!ta) {
    update(current + prefix + fallback + suffix)
    return
  }
  const start = ta.selectionStart ?? 0
  const end = ta.selectionEnd ?? 0
  const selected = current.slice(start, end) || fallback
  update(current.slice(0, start) + prefix + selected + suffix + current.slice(end))
  requestAnimationFrame(() => {
    ta.focus()
    ta.setSelectionRange(start + prefix.length, start + prefix.length + selected.length)
  })
}

/** 当前行行首插入前缀（标题/列表/引用） */
function linePrefix(prefix: string) {
  const ta = taRef.value
  const current = props.modelValue ?? ''
  const start = ta?.selectionStart ?? current.length
  const lineStart = current.lastIndexOf('\n', Math.max(0, start - 1)) + 1
  update(current.slice(0, lineStart) + prefix + current.slice(lineStart))
  requestAnimationFrame(() => {
    ta?.focus()
    const pos = start + prefix.length
    ta?.setSelectionRange(pos, pos)
  })
}

/** 上传图片/视频并插入 */
async function onFileChange(kind: 'image' | 'video', event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const isVideo = kind === 'video'
  const maxMB = isVideo ? 200 : 20
  if (file.size > maxMB * 1024 * 1024) {
    ElMessage.warning(`文件大小不能超过 ${maxMB}MB`)
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(file)
    const url = res.fileName || res.url
    insert(isVideo ? `\n${mdVideo(url, 'video')}\n` : `\n${mdImage(url, 'image')}\n`)
    ElMessage.success(isVideo ? '视频已插入' : '图片已插入')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

function onInput(event: Event) {
  update((event.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <div v-loading="uploading" class="md-editor">
    <div class="md-toolbar">
      <div class="md-group">
        <el-button size="small" text @click="linePrefix('# ')">H1</el-button>
        <el-button size="small" text @click="linePrefix('## ')">H2</el-button>
        <el-button size="small" text @click="linePrefix('### ')">H3</el-button>
      </div>
      <div class="md-group">
        <el-button size="small" text title="加粗" @click="wrap('**', '**', '加粗文字')">B</el-button>
        <el-button size="small" text title="斜体" @click="wrap('*', '*', '斜体文字')"><i>I</i></el-button>
        <el-button size="small" text title="删除线" @click="wrap('~~', '~~', '删除线')">S</el-button>
        <el-button size="small" text title="行内代码" @click="wrap('`', '`', 'code')">&lt;/&gt;</el-button>
      </div>
      <div class="md-group">
        <el-button size="small" text title="引用" @click="linePrefix('> ')">❝</el-button>
        <el-button size="small" text title="无序列表" @click="linePrefix('- ')">•</el-button>
        <el-button size="small" text title="有序列表" @click="linePrefix('1. ')">1.</el-button>
        <el-button size="small" text title="分割线" @click="insert('\n---\n')">—</el-button>
      </div>
      <span class="md-spacer" />
      <div class="md-group">
        <el-button size="small" :icon="Picture" @click="imgInputRef?.click()">图片</el-button>
        <el-button size="small" :icon="VideoCamera" @click="videoInputRef?.click()">视频</el-button>
      </div>
      <input ref="imgInputRef" type="file" accept="image/*" hidden @change="onFileChange('image', $event)" />
      <input ref="videoInputRef" type="file" accept="video/*" hidden @change="onFileChange('video', $event)" />
    </div>
    <textarea
      ref="taRef"
      class="md-textarea"
      :value="modelValue"
      :placeholder="placeholder"
      :style="{ height: `${height}px` }"
      @input="onInput"
    />
    <div class="md-footer">
      <span>{{ (modelValue || '').length }} 字符</span>
      <span>支持 Markdown · 图片/视频自动上传</span>
    </div>
  </div>
</template>

<style scoped>
.md-editor {
  border: 1px solid #e4e4f0;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.md-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid #eeeef7;
  background: #fafaff;
}

.md-group {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-right: 6px;
  border-right: 1px solid #ececf6;
}

.md-group:last-child {
  border-right: none;
}

.md-spacer {
  flex: 1;
}

.md-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: vertical;
  padding: 12px 14px;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.75;
  color: #2b2b3c;
  background: #fff;
}

.md-footer {
  display: flex;
  justify-content: space-between;
  padding: 6px 12px;
  border-top: 1px solid #f2f2f8;
  color: #9a9ab0;
  font-size: 12px;
}
</style>
