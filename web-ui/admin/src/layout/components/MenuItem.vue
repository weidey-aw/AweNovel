<script setup lang="ts">
/**
 * 侧边栏菜单项（递归组件）
 * - 有子菜单 → el-sub-menu（可展开）
 * - 无子菜单 → el-menu-item（点击跳转，配合 el-menu 的 router 模式）
 */
import type { SideMenuItem } from '@/stores/permission'
import { iconOf } from '@/utils/icon'

defineProps<{ item: SideMenuItem }>()
</script>

<template>
  <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
    <template #title>
      <el-icon><component :is="iconOf(item.icon)" /></el-icon>
      <span>{{ item.title }}</span>
    </template>
    <menu-item v-for="child in item.children" :key="child.path" :item="child" />
  </el-sub-menu>

  <el-menu-item v-else :index="item.path">
    <el-icon><component :is="iconOf(item.icon)" /></el-icon>
    <span>{{ item.title }}</span>
  </el-menu-item>
</template>
