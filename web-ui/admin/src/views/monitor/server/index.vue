<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getServerInfo } from '@gal/shared'
import type { ServerInfo } from '@gal/shared'

const loading = ref(false)
const info = ref<ServerInfo | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await getServerInfo()
    info.value = res.data ?? null
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '服务器信息加载失败')
  } finally {
    loading.value = false
  }
}

const usageType = (usage: number) => (usage >= 80 ? 'exception' : usage >= 60 ? 'warning' : 'success')

const jvmMemory = computed(() => {
  if (!info.value) return []
  const { jvm } = info.value
  return [
    { label: '总内存', value: `${jvm.total} MB` },
    { label: '已用', value: `${jvm.used} MB` },
    { label: '空闲', value: `${jvm.free} MB` },
    { label: '最大可用', value: `${jvm.max} MB` },
  ]
})

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="toolbar table-card">
      <span class="label">服务器监控</span>
      <span class="spacer" />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <template v-if="info">
      <!-- 概览 -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(120deg, #8b5cf6, #ec4899)">CPU</div>
          <div>
            <b>{{ info.cpu.used }}%</b>
            <span>{{ info.cpu.cpuNum }} 核心</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(120deg, #0ea5e9, #22d3ee)">MEM</div>
          <div>
            <b>{{ info.mem.usage }}%</b>
            <span>{{ info.mem.used }} / {{ info.mem.total }} GB</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(120deg, #f59e0b, #fbbf24)">JVM</div>
          <div>
            <b>{{ info.jvm.usage }}%</b>
            <span>{{ info.jvm.version }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(120deg, #10b981, #34d399)">OS</div>
          <div>
            <b>{{ info.sys.osName }}</b>
            <span>{{ info.sys.computerIp }} · {{ info.sys.osArch }}</span>
          </div>
        </div>
      </div>

      <!-- CPU -->
      <div class="table-card block">
        <h4>CPU</h4>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="核心数">{{ info.cpu.cpuNum }}</el-descriptions-item>
          <el-descriptions-item label="用户使用率">{{ info.cpu.used }}%</el-descriptions-item>
          <el-descriptions-item label="系统使用率">{{ info.cpu.sys }}%</el-descriptions-item>
          <el-descriptions-item label="当前等待率">{{ info.cpu.wait }}%</el-descriptions-item>
          <el-descriptions-item label="当前空闲率">{{ info.cpu.free }}%</el-descriptions-item>
          <el-descriptions-item label="总使用率">{{ info.cpu.total }}%</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 内存 -->
      <div class="table-card block">
        <h4>物理内存</h4>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="总内存">{{ info.mem.total }} GB</el-descriptions-item>
          <el-descriptions-item label="已用内存">{{ info.mem.used }} GB</el-descriptions-item>
          <el-descriptions-item label="剩余内存">{{ info.mem.free }} GB</el-descriptions-item>
          <el-descriptions-item label="使用率">
            <el-progress :percentage="info.mem.usage" :status="usageType(info.mem.usage)" :stroke-width="12" />
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- JVM -->
      <div class="table-card block">
        <h4>JVM 信息</h4>
        <el-descriptions :column="3" border>
          <el-descriptions-item v-for="m in jvmMemory" :key="m.label" :label="m.label">{{ m.value }}</el-descriptions-item>
          <el-descriptions-item label="JDK 版本">{{ info.jvm.version }}</el-descriptions-item>
          <el-descriptions-item label="运行时长">{{ info.jvm.runTime }}</el-descriptions-item>
          <el-descriptions-item label="启动时间">{{ info.jvm.startTime }}</el-descriptions-item>
          <el-descriptions-item label="安装路径" :span="2">{{ info.jvm.home }}</el-descriptions-item>
          <el-descriptions-item label="使用率">{{ info.jvm.usage }}%</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 服务器 -->
      <div class="table-card block">
        <h4>服务器信息</h4>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="服务器名称">{{ info.sys.computerName }}</el-descriptions-item>
          <el-descriptions-item label="服务器IP">{{ info.sys.computerIp }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ info.sys.osName }}</el-descriptions-item>
          <el-descriptions-item label="系统架构">{{ info.sys.osArch }}</el-descriptions-item>
          <el-descriptions-item label="项目路径" :span="2">{{ info.sys.userDir }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 磁盘 -->
      <div class="table-card block">
        <h4>磁盘状态</h4>
        <el-table :data="info.sysFiles" border stripe>
          <el-table-column prop="dirName" label="盘符路径" min-width="140" />
          <el-table-column prop="sysTypeName" label="文件系统" min-width="120" />
          <el-table-column prop="typeName" label="盘符类型" min-width="180" show-overflow-tooltip />
          <el-table-column prop="total" label="总大小" width="120" />
          <el-table-column prop="free" label="可用大小" width="120" />
          <el-table-column prop="used" label="已用大小" width="120" />
          <el-table-column label="已用百分比" min-width="180">
            <template #default="{ row }">
              <el-progress :percentage="row.usage" :status="usageType(row.usage)" :stroke-width="12" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="暂无服务器信息" />
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

.stat-icon {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
</style>
