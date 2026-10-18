<script setup lang="ts">
/**
 * 轻量图片裁剪组件（无第三方依赖）
 * - 固定正方形裁剪框，支持拖拽平移、滑块缩放
 * - 通过 expose 的 toBlob() 输出裁剪后的正方形 JPEG Blob
 */
import { computed, onBeforeUnmount, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 图片地址（blob: / data: / http） */
    src: string
    /** 输出图片边长（px） */
    size?: number
  }>(),
  { size: 300 },
)

/** 画布可视区边长 */
const BOX = 320
/** 裁剪框边长 */
const CROP = 260

const imgRef = ref<HTMLImageElement>()
const naturalW = ref(0)
const naturalH = ref(0)
const scale = ref(1)
const coverScale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const zoomPercent = ref(100)

const cropX = (BOX - CROP) / 2
const cropY = cropX

const imgStyle = computed(() => ({
  width: `${naturalW.value}px`,
  height: `${naturalH.value}px`,
  transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${scale.value})`,
  transformOrigin: '0 0',
}))

const holeStyle = computed(() => ({
  left: `${cropX}px`,
  top: `${cropY}px`,
  width: `${CROP}px`,
  height: `${CROP}px`,
}))

/** 限制平移范围，保证裁剪框始终被图片覆盖 */
function clampOffsets() {
  const w = naturalW.value * scale.value
  const h = naturalH.value * scale.value
  offsetX.value = Math.min(cropX, Math.max(cropX + CROP - w, offsetX.value))
  offsetY.value = Math.min(cropY, Math.max(cropY + CROP - h, offsetY.value))
}

function onLoad() {
  const img = imgRef.value
  if (!img) return
  naturalW.value = img.naturalWidth
  naturalH.value = img.naturalHeight
  coverScale.value = CROP / Math.min(naturalW.value, naturalH.value)
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
  const anchorX = cropX + CROP / 2
  const anchorY = cropY + CROP / 2
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

/** 输出裁剪后的正方形图片 */
function toBlob(): Promise<Blob | null> {
  const img = imgRef.value
  if (!img || !naturalW.value) return Promise.resolve(null)
  const out = props.size
  const canvas = document.createElement('canvas')
  canvas.width = out
  canvas.height = out
  const ctx = canvas.getContext('2d')
  if (!ctx) return Promise.resolve(null)
  // 透明背景转白，避免 JPEG 出现黑块
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, out, out)
  const sw = CROP / scale.value
  const sx = (cropX - offsetX.value) / scale.value
  const sy = (cropY - offsetY.value) / scale.value
  ctx.drawImage(img, sx, sy, sw, sw, 0, 0, out, out)
  return new Promise<Blob | null>((resolve) => {
    canvas.toBlob((blob) => resolve(blob), 'image/jpeg', 0.92)
  })
}

defineExpose({ toBlob })
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
    <p class="tips">拖动图片调整位置，滑动缩放，裁剪框内的正方形区域将作为头像</p>
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
  width: 320px;
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
}
</style>
