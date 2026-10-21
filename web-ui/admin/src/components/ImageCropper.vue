<script setup lang="ts">
/**
 * 轻量图片裁剪组件（无第三方依赖）
 * - 支持自定义裁剪比例（封面 3:4、轮播图 16:9、头像 1:1）
 * - 支持拖拽平移、滚轮/滑块缩放
 * - 默认输出 WebP，可通过 toBlob() / toFile() 取回
 */
import { computed, onBeforeUnmount, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 图片地址（blob: / data: / http） */
    src: string
    /** 裁剪比例 = 宽 / 高，默认 1:1 */
    aspect?: number
    /** 输出宽度（px），高度按比例计算 */
    outWidth?: number
    /** 输出格式 */
    format?: 'image/webp' | 'image/jpeg' | 'image/png'
    /** 输出质量（jpeg/webp） */
    quality?: number
  }>(),
  { aspect: 1, outWidth: 600, format: 'image/webp', quality: 0.9 },
)

/** 画布可视区边长 */
const BOX = 360

const imgRef = ref<HTMLImageElement>()
const naturalW = ref(0)
const naturalH = ref(0)
const scale = ref(1)
const coverScale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const zoomPercent = ref(100)

/** 裁剪框尺寸（按比例适配可视区） */
const cropW = computed(() => {
  const maxW = BOX - 36
  const maxH = BOX - 36
  const h = maxW / props.aspect
  return h <= maxH ? maxW : maxH * props.aspect
})
const cropH = computed(() => cropW.value / props.aspect)
const cropX = computed(() => (BOX - cropW.value) / 2)
const cropY = computed(() => (BOX - cropH.value) / 2)

const imgStyle = computed(() => ({
  width: `${naturalW.value}px`,
  height: `${naturalH.value}px`,
  transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${scale.value})`,
  transformOrigin: '0 0',
}))

const holeStyle = computed(() => ({
  left: `${cropX.value}px`,
  top: `${cropY.value}px`,
  width: `${cropW.value}px`,
  height: `${cropH.value}px`,
}))

/** 限制平移范围，保证裁剪框始终被图片覆盖 */
function clampOffsets() {
  const w = naturalW.value * scale.value
  const h = naturalH.value * scale.value
  offsetX.value = Math.min(cropX.value, Math.max(cropX.value + cropW.value - w, offsetX.value))
  offsetY.value = Math.min(cropY.value, Math.max(cropY.value + cropH.value - h, offsetY.value))
}

function onLoad() {
  const img = imgRef.value
  if (!img) return
  naturalW.value = img.naturalWidth
  naturalH.value = img.naturalHeight
  // 让图片刚好覆盖裁剪框
  coverScale.value = Math.max(cropW.value / naturalW.value, cropH.value / naturalH.value)
  zoomPercent.value = 100
  scale.value = coverScale.value
  offsetX.value = (BOX - naturalW.value * scale.value) / 2
  offsetY.value = (BOX - naturalH.value * scale.value) / 2
  clampOffsets()
}

/** 以裁剪框中心为锚点缩放 */
function onZoom(value: number | number[]) {
  const percent = Array.isArray(value) ? value[0] : value
  const newScale = coverScale.value * (percent / 100)
  const anchorX = cropX.value + cropW.value / 2
  const anchorY = cropY.value + cropH.value / 2
  const imgX = (anchorX - offsetX.value) / scale.value
  const imgY = (anchorY - offsetY.value) / scale.value
  scale.value = newScale
  offsetX.value = anchorX - imgX * newScale
  offsetY.value = anchorY - imgY * newScale
  clampOffsets()
}

/* ---------------- 拖拽 ---------------- */
let dragging = false
let startX = 0
let startY = 0
let startOffX = 0
let startOffY = 0

function onPointerMove(e: PointerEvent) {
  if (!dragging) return
  offsetX.value = startOffX + (e.clientX - startX)
  offsetY.value = startOffY + (e.clientY - startY)
  clampOffsets()
}

function onPointerUp() {
  dragging = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
}

function onPointerDown(e: PointerEvent) {
  if (e.button !== 0) return
  dragging = true
  startX = e.clientX
  startY = e.clientY
  startOffX = offsetX.value
  startOffY = offsetY.value
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

/** 滚轮缩放 */
function onWheel(e: WheelEvent) {
  const next = Math.min(300, Math.max(100, zoomPercent.value + (e.deltaY < 0 ? 10 : -10)))
  if (next === zoomPercent.value) return
  zoomPercent.value = next
  onZoom(next)
}

onBeforeUnmount(onPointerUp)

/** 输出裁剪后的图片 Blob */
function toBlob(): Promise<Blob | null> {
  const img = imgRef.value
  if (!img || !naturalW.value) return Promise.resolve(null)
  const outW = props.outWidth
  const outH = Math.round(props.outWidth / props.aspect)
  const canvas = document.createElement('canvas')
  canvas.width = outW
  canvas.height = outH
  const ctx = canvas.getContext('2d')
  if (!ctx) return Promise.resolve(null)
  // 透明背景转白，避免 JPEG 黑块
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, outW, outH)
  ctx.imageSmoothingQuality = 'high'
  const sx = (cropX.value - offsetX.value) / scale.value
  const sy = (cropY.value - offsetY.value) / scale.value
  const sw = cropW.value / scale.value
  const sh = cropH.value / scale.value
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, outW, outH)

  return new Promise<Blob | null>((resolve) => {
    canvas.toBlob((blob) => resolve(blob), props.format, props.quality)
  })
}

/** 输出为 File（按实际 MIME 命名后缀） */
async function toFile(baseName = 'image'): Promise<File | null> {
  const blob = await toBlob()
  if (!blob) return null
  const ext = blob.type === 'image/webp' ? 'webp' : blob.type === 'image/png' ? 'png' : 'jpg'
  return new File([blob], `${baseName}_${Date.now()}.${ext}`, { type: blob.type })
}

defineExpose({ toBlob, toFile })
</script>

<template>
  <div class="cropper-wrap">
    <div
      class="cropper"
      :style="{ width: `${BOX}px`, height: `${BOX}px` }"
      @pointerdown="onPointerDown"
      @wheel.prevent="onWheel"
    >
      <img ref="imgRef" :src="src" :style="imgStyle" alt="" draggable="false" @load="onLoad" />
      <!-- 遮罩 + 裁剪框 -->
      <div class="hole" :style="holeStyle">
        <span class="grid grid-v1" />
        <span class="grid grid-v2" />
        <span class="grid grid-h1" />
        <span class="grid grid-h2" />
      </div>
    </div>
    <div class="zoom-row">
      <span class="zoom-label">缩放</span>
      <el-slider v-model="zoomPercent" :min="100" :max="300" :show-tooltip="false" @input="onZoom" />
    </div>
    <p class="tips">
      拖动图片调整位置，滑动缩放；输出 {{ outWidth }}×{{ Math.round(outWidth / aspect) }} ·
      {{ format.replace('image/', '').toUpperCase() }}
    </p>
  </div>
</template>

<style scoped>
.cropper-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cropper {
  position: relative;
  overflow: hidden;
  background: #16161f;
  border-radius: 10px;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.cropper:active {
  cursor: grabbing;
}

.cropper img {
  position: absolute;
  left: 0;
  top: 0;
  max-width: none;
  pointer-events: none;
  -webkit-user-drag: none;
}

.hole {
  position: absolute;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
  border: 2px solid rgba(255, 255, 255, 0.9);
  pointer-events: none;
}

.grid {
  position: absolute;
  background: rgba(255, 255, 255, 0.45);
}

.grid-v1,
.grid-v2 {
  top: 0;
  bottom: 0;
  width: 1px;
}

.grid-v1 {
  left: 33.33%;
}

.grid-v2 {
  left: 66.66%;
}

.grid-h1,
.grid-h2 {
  left: 0;
  right: 0;
  height: 1px;
}

.grid-h1 {
  top: 33.33%;
}

.grid-h2 {
  top: 66.66%;
}

.zoom-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 330px;
  margin-top: 14px;
}

.zoom-label {
  font-size: 12.5px;
  color: #6b6b85;
}

.tips {
  margin: 4px 0 0;
  color: #9a9ab0;
  font-size: 12px;
  text-align: center;
}
</style>
