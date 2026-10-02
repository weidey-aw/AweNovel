<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMenuList } from '@gal/shared'
import type { SysMenu } from '@gal/shared'

const loading = ref(false)
const list = ref<SysMenu[]>([])
const total = ref(0)

const query = reactive({
  menuName: '',
  status: '',
})

const typeMap: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }

async function load() {
  loading.value = true
  try {
    const res = await getMenuList({
      menuName: query.menuName || undefined,
      status: query.status || undefined,
    })
    list.value = res.data ?? []
    total.value = list.value.length
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '菜单列表加载失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  query.menuName = ''
  query.status = ''
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <el-input
        v-model="query.menuName"
        placeholder="菜单名称"
        clearable
        style="width: 200px"
        @keyup.enter="load"
        @clear="load"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="load">
        <el-option label="正常" value="0" />
        <el-option label="停用" value="1" />
      </el-select>
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-tag type="info" effect="plain">共 {{ total }} 个菜单节点</el-tag>
    </div>

    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="list"
        row-key="menuId"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.menuType === 'F' ? 'info' : 'primary'" effect="plain">
              {{ typeMap[row.menuType as string] || row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="80" />
        <el-table-column prop="path" label="路由地址" min-width="130">
          <template #default="{ row }">{{ row.path || '—' }}</template>
        </el-table-column>
        <el-table-column prop="component" label="组件路径" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.component || '—' }}</template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.perms || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
