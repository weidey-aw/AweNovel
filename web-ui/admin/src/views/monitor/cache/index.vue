<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getCacheInfo, getCacheNames } from '@gal/shared'
import type { CacheInfo, CacheName } from '@gal/shared'

const loading = ref(false)
const info = ref<CacheInfo | null>(null)
const names = ref<CacheName[]>([])

async function load() {
  loading.value = true
  try {
    const [infoRes, nameRes] = await Promise.all([getCacheInfo(), getCacheNames()])
    info.value = infoRes.data ?? null
    names.value = nameRes.data ?? []
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '缓存信息加载失败')
  } finally {
    loading.value = false
  }
}

/** Redis INFO 中挑出常用指标展示 */
const basicInfo = computed(() => {
  const info_ = info.value?.info ?? {}
  const pick: Array<[string, string]> = [
    ['redis_version', 'Redis 版本'],
    ['redis_mode', '运行模式'],
    ['used_memory_human', '占用内存'],
    ['used_cpu_sys', '系统 CPU'],
    ['connected_clients', '客户端连接数'],
    ['uptime_in_days', '运行天数'],
    ['keyspace_hits', '命中次数'],
    ['keyspace_misses', '未命中次数'],
    ['expired_keys', '过期 key 数'],
    ['total_commands_processed', '处理命令总数'],
  ]
  return pick
    .filter(([key]) => info_[key] !== undefined)
    .map(([key, label]) => ({ label, value: String(info_[key]) }))
})

const commandStats = computed(() => info.value?.commandStats ?? [])

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="toolbar table-card">
      <span class="label">缓存监控（Redis）</span>
      <el-tag type="success" effect="plain" size="small">key 总数：{{ info?.dbSize ?? 0 }}</el-tag>
      <span class="spacer" />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="table-card block">
      <h4>基本信息</h4>
      <el-descriptions v-if="basicInfo.length" :column="3" border>
        <el-descriptions-item v-for="item in basicInfo" :key="item.label" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无缓存信息" :image-size="80" />
    </div>

    <div class="table-card block">
      <h4>命令统计</h4>
      <el-table :data="commandStats" border stripe max-height="320">
        <el-table-column prop="name" label="命令" min-width="140" />
        <el-table-column prop="value" label="调用次数" min-width="140" />
      </el-table>
    </div>

    <div class="table-card block">
      <h4>缓存分类</h4>
      <el-table :data="names" border stripe>
        <el-table-column prop="cacheName" label="缓存名称" min-width="240" />
        <el-table-column prop="remark" label="备注" min-width="200" />
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.label {
  font-weight: 700;
}

.block {
  margin-bottom: 16px;
}

.block h4 {
  margin: 0 0 14px;
  font-size: 14px;
  color: #4b4b63;
}
</style>
