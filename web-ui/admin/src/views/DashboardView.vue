<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection, Finished, User, VideoPlay } from '@element-plus/icons-vue'
import {
  getArticleList,
  getGameList,
  getReviewTasks,
  getSystemUserList,
} from '@gal/shared'

const router = useRouter()

const stats = ref([
  { label: '收录游戏', value: '—', icon: VideoPlay, color: '#8b5cf6', path: '/games' },
  { label: '社区文章', value: '—', icon: Collection, color: '#0ea5e9', path: '/articles' },
  { label: '注册用户', value: '—', icon: User, color: '#10b981', path: '/users' },
  { label: '待审核任务', value: '—', icon: Finished, color: '#f59e0b', path: '/review' },
])

const loading = ref(true)

onMounted(async () => {
  try {
    const [gameRes, articleRes, userRes, taskRes] = await Promise.all([
      getGameList({ pageNum: 1, pageSize: 1 }),
      getArticleList({ pageNum: 1, pageSize: 1 }),
      getSystemUserList({ pageNum: 1, pageSize: 1 }),
      getReviewTasks(),
    ])
    stats.value[0].value = String(gameRes.total ?? 0)
    stats.value[1].value = String(articleRes.total ?? 0)
    stats.value[2].value = String(userRes.total ?? 0)
    const tasks = (taskRes as unknown as { data?: unknown[] } | unknown[] | undefined)
    const taskList = Array.isArray(tasks) ? tasks : (tasks as { data?: unknown[] })?.data ?? []
    stats.value[3].value = String(taskList.length)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '统计数据加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="stat-grid">
      <div v-for="s in stats" :key="s.label" class="stat-card" @click="router.push(s.path)">
        <div class="stat-icon" :style="{ background: s.color }">
          <el-icon :size="22"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <b>{{ loading ? '—' : s.value }}</b>
          <span>{{ s.label }}</span>
        </div>
      </div>
    </div>

    <div class="table-card">
      <h3 class="block-title">快捷入口</h3>
      <div class="quick-links">
        <div v-for="l in [
          { t: '游戏管理', d: '新增 / 编辑 / 删除游戏条目', p: '/games' },
          { t: '内容审核', d: '处理待审核的游戏、资源与文章', p: '/review' },
          { t: '资源管理', d: '查看资源列表与下载状态', p: '/resources' },
          { t: '评论管理', d: '删除违规评论', p: '/comments' },
        ]" :key="l.t" class="quick" @click="router.push(l.p)">
          <b>{{ l.t }}</b>
          <span>{{ l.d }}</span>
        </div>
      </div>
    </div>

    <div class="table-card chart-card">
      <h3 class="block-title">内容占比（示意）</h3>
      <div class="bars">
        <div class="bar-row">
          <span>游戏</span>
          <div class="bar"><i class="fill" style="width: 62%; background: #8b5cf6" /></div>
          <em>62%</em>
        </div>
        <div class="bar-row">
          <span>文章</span>
          <div class="bar"><i class="fill" style="width: 28%; background: #0ea5e9" /></div>
          <em>28%</em>
        </div>
        <div class="bar-row">
          <span>资源</span>
          <div class="bar"><i class="fill" style="width: 10%; background: #f59e0b" /></div>
          <em>10%</em>
        </div>
      </div>
      <p class="tip">此图表为静态占位，接入真实统计接口后可替换。</p>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 22px rgba(30, 30, 60, 0.12);
}

.block-title {
  margin: 0 0 16px;
  font-size: 15px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.quick {
  border: 1px solid #ececf5;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafaff;
}

.quick:hover {
  border-color: #c9bdf8;
  background: #f5f2fe;
}

.quick b {
  display: block;
  margin-bottom: 6px;
}

.quick span {
  font-size: 12.5px;
  color: #8a8aa3;
}

.chart-card {
  margin-top: 16px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
}

.bar-row span {
  width: 44px;
  color: #6b6b85;
}

.bar {
  flex: 1;
  height: 14px;
  border-radius: 7px;
  background: #f0f0f8;
  overflow: hidden;
}

.fill {
  display: block;
  height: 100%;
  border-radius: 7px;
}

.bar-row em {
  font-style: normal;
  width: 40px;
  color: #8a8aa3;
}

.tip {
  margin-top: 16px;
  font-size: 12px;
  color: #b0b0c5;
}
</style>
