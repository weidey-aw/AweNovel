<script setup lang="ts">
/**
 * 路由标签卡
 * - 访问页面时自动追加标签；点击切换；关闭当前标签后自动跳到上一个
 * - 右键菜单：刷新当前 / 关闭当前 / 关闭其他 / 关闭全部
 * - 仪表盘为固定标签，不可关闭
 */
import { computed, onBeforeUnmount, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Refresh } from '@element-plus/icons-vue'
import { useTagsViewStore, type TagView } from '@/stores/tagsView'

const emit = defineEmits<{ (e: 'refresh'): void }>()

const route = useRoute()
const router = useRouter()
const tagsStore = useTagsViewStore()

const tags = computed(() => tagsStore.tags)

const isActive = (tag: TagView) => tag.path === route.path

/** 右键菜单状态 */
const menu = reactive({
  visible: false,
  left: 0,
  top: 0,
  tag: null as TagView | null,
})

watch(
  () => route.path,
  () => {
    tagsStore.addView(route)
  },
  { immediate: true },
)

function go(tag: TagView) {
  if (tag.path !== route.path) router.push(tag.fullPath)
}

function closeTag(tag: TagView) {
  if (tag.affix) return
  const wasActive = isActive(tag)
  tagsStore.delView(tag.path)
  if (!wasActive) return
  const remain = tagsStore.tags
  const fallback = remain[remain.length - 1] ?? remain[0]
  router.push(fallback ? fallback.fullPath : '/dashboard')
}

function openMenu(event: MouseEvent, tag: TagView) {
  menu.visible = true
  menu.left = Math.min(event.clientX, window.innerWidth - 160)
  menu.top = event.clientY
  menu.tag = tag
}

function closeMenu() {
  menu.visible = false
  menu.tag = null
}

function handleRefresh() {
  if (menu.tag && !isActive(menu.tag)) router.push(menu.tag.fullPath)
  emit('refresh')
  closeMenu()
}

function handleClose() {
  if (menu.tag) closeTag(menu.tag)
  closeMenu()
}

function handleCloseOthers() {
  if (menu.tag) {
    tagsStore.delOthers(menu.tag.path)
    if (!isActive(menu.tag)) router.push(menu.tag.fullPath)
  }
  closeMenu()
}

function handleCloseAll() {
  tagsStore.delAll()
  router.push('/dashboard')
  closeMenu()
}

function onScrollClose() {
  if (menu.visible) closeMenu()
}

onMounted(() => {
  document.addEventListener('click', onScrollClose)
  window.addEventListener('resize', onScrollClose)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onScrollClose)
  window.removeEventListener('resize', onScrollClose)
})
</script>

<template>
  <div class="tags-view">
    <el-scrollbar class="tags-scroll">
      <div class="tags-inner">
        <span
          v-for="tag in tags"
          :key="tag.path"
          class="tag-item"
          :class="{ active: isActive(tag) }"
          @click="go(tag)"
          @contextmenu.prevent="openMenu($event, tag)"
        >
          <span class="tag-title">{{ tag.title }}</span>
          <el-icon v-if="!tag.affix" class="tag-close" @click.stop="closeTag(tag)">
            <Close />
          </el-icon>
        </span>
      </div>
    </el-scrollbar>

    <ul
      v-show="menu.visible"
      class="ctx-menu"
      :style="{ left: `${menu.left}px`, top: `${menu.top}px` }"
      @click.stop
    >
      <li @click="handleRefresh"><el-icon><Refresh /></el-icon>刷新当前</li>
      <li v-if="menu.tag && !menu.tag.affix" @click="handleClose"><el-icon><Close /></el-icon>关闭当前</li>
      <li @click="handleCloseOthers">关闭其他</li>
      <li @click="handleCloseAll">关闭全部</li>
    </ul>
  </div>
</template>

<style scoped>
.tags-view {
  background: #fff;
  border-bottom: 1px solid #ececf5;
  padding: 6px 12px;
  flex-shrink: 0;
}

.tags-scroll {
  width: 100%;
}

.tags-inner {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  border: 1px solid #e4e4f0;
  background: #f8f8fc;
  color: #5b5b73;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.tag-item:hover {
  color: #7c5cf0;
  border-color: #cdc4f7;
}

.tag-item.active {
  background: linear-gradient(120deg, #8b5cf6, #ec4899);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.35);
}

.tag-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 1px;
}

.tag-close:hover {
  background: rgba(0, 0, 0, 0.18);
}

.ctx-menu {
  position: fixed;
  z-index: 3000;
  margin: 0;
  padding: 5px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #ececf5;
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(30, 30, 60, 0.14);
  min-width: 132px;
}

.ctx-menu li {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  color: #4b4b63;
  cursor: pointer;
}

.ctx-menu li:hover {
  background: #f5f3fe;
  color: #7c5cf0;
}
</style>
