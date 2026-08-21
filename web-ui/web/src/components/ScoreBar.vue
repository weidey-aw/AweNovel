<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 平均分（0-10） */
    value?: number
    /** 评分人数 */
    count?: number
    /** 是否显示数字 */
    showText?: boolean
  }>(),
  { value: 0, count: 0, showText: true },
)

const rounded = computed(() => Math.round(props.value))
const text = computed(() => Number(props.value).toFixed(1))
</script>

<template>
  <span class="score-bar">
    <el-rate
      :model-value="rounded"
      :max="10"
      disabled
      allow-half
      class="score-rate"
    />
    <span v-if="showText" class="score-num">
      <b>{{ text }}</b>
      <em v-if="count">（{{ count }}）</em>
    </span>
  </span>
</template>

<style scoped>
.score-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.score-rate {
  --el-rate-fill-color: #f59e0b;
  --el-rate-disabled-void-color: #3a3a58;
  --el-rate-disabled-fill-color: #f59e0b;
  height: 18px;
}

.score-rate :deep(.el-rate__item) {
  margin-right: 1px;
}

.score-num {
  font-size: 13px;
  color: var(--text-sub);
  white-space: nowrap;
}

.score-num b {
  color: #f59e0b;
  font-size: 15px;
}

.score-num em {
  font-style: normal;
  color: var(--text-dim);
  font-size: 12px;
}
</style>
