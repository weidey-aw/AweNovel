<script setup lang="ts">
/**
 * 404 / 组件缺失兜底页
 * - 访问不存在的路径时展示 404
 * - 后端菜单配置了 component 但前端没有对应 .vue 文件时，通过 meta.missingComponent 展示缺失提示
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const missing = computed(() => route.meta?.missingComponent as string | undefined)
</script>

<template>
  <div class="page">
    <div class="table-card err-card">
      <template v-if="missing">
        <h2>页面组件未实现</h2>
        <p>
          后端菜单配置的组件路径为
          <code>{{ missing }}</code>
          ，前端未找到对应的
          <code>src/views/{{ missing }}.vue</code>。
        </p>
        <p class="dim">请补齐该文件，或在「系统管理 → 菜单管理」中修正组件路径。</p>
      </template>
      <template v-else>
        <h2>404</h2>
        <p>抱歉，你访问的页面不存在或已被移除。</p>
      </template>
      <el-button class="grad-btn" @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.err-card {
  max-width: 620px;
  margin: 60px auto;
  text-align: center;
  padding: 40px 28px;
}

.err-card h2 {
  margin: 0 0 12px;
  font-size: 22px;
}

.err-card p {
  color: #5b5b73;
  line-height: 1.8;
  margin: 0 0 10px;
}

.err-card code {
  background: #f3f0ff;
  color: #7c5cf0;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12.5px;
}

.dim {
  color: #8a8aa3;
  font-size: 12.5px;
}

.grad-btn {
  margin-top: 14px;
}
</style>
