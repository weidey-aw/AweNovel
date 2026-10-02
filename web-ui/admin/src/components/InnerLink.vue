<script setup lang="ts">
/**
 * 内链组件：以 iframe 展示后端菜单配置的外部/内部链接。
 * 对应后端菜单 component = 'InnerLink'（meta.link 为 http(s) 地址或站内路径）。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const src = computed(() => {
  const link = route.meta?.link as string | undefined
  if (link) return link
  return route.path
})
</script>

<template>
  <div class="inner-link">
    <iframe :src="src" frameborder="0" />
  </div>
</template>

<style scoped>
.inner-link {
  height: calc(100vh - 60px);
}

.inner-link iframe {
  width: 100%;
  height: 100%;
  display: block;
  background: #fff;
}
</style>
