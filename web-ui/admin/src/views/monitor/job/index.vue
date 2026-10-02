<script setup lang="ts">
/**
 * 定时任务
 *
 * 说明：后端已移除 Quartz（原 sys_job 表与 JobController 均不存在），
 * 现由 weidey-framework 的 ScheduleConfig（Spring @Scheduled）承担定时逻辑，
 * 暂无查询/管理接口。本页先占位，待后端补齐接口后再实现。
 */
import { onMounted, ref } from 'vue'
import { getScheduledHint } from './task-info'

const tasks = ref(getScheduledHint())

onMounted(() => {
  tasks.value = getScheduledHint()
})
</script>

<template>
  <div class="page">
    <el-alert
      type="warning"
      show-icon
      :closable="false"
      title="后端暂未提供定时任务接口"
      description="项目已移除 Quartz 调度（sys_job 表与 JobController 均不存在），当前定时逻辑由 Spring @Scheduled 实现，没有可查询的任务列表。本页为占位页，待后端补充 /monitor/job 接口后再实现。"
      style="margin-bottom: 16px"
    />

    <div class="table-card">
      <el-table :data="tasks" border stripe>
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column prop="bean" label="实现位置" min-width="320" show-overflow-tooltip />
        <el-table-column prop="remark" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default>
            <el-tag type="info" size="small">不可查询</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">
        以上为前端静态说明，不代表后端实际调度配置；仅用于提示该模块的现状。
      </p>
    </div>
  </div>
</template>

<style scoped>
.tip {
  margin: 14px 2px 0;
  color: #8a8aa3;
  font-size: 12.5px;
}
</style>
