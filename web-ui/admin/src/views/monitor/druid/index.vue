<script setup lang="ts">
/**
 * 数据监控（Druid）
 * 直接以 iframe 嵌入后端 Druid 监控台（/druid/），登录账号密码为
 * application-*.yml 中 druid.statViewServlet.login-username/login-password。
 */
import { ref } from 'vue'

const iframeKey = ref(0)
const src = '/druid/index.html'

function refresh() {
  iframeKey.value += 1
}

function openNew() {
  window.open(src, '_blank')
}
</script>

<template>
  <div class="page">
    <div class="toolbar table-card">
      <span class="label">Druid 监控台</span>
      <el-tag type="info" effect="plain" size="small">默认账号 admin / 123456（见后端配置）</el-tag>
      <span class="spacer" />
      <el-button class="grad-btn" @click="refresh">刷新</el-button>
      <el-button link type="primary" @click="openNew">新窗口打开</el-button>
    </div>

    <div class="table-card iframe-card">
      <iframe :key="iframeKey" :src="src" class="druid-frame" frameborder="0" />
    </div>
  </div>
</template>

<style scoped>
.label {
  font-weight: 700;
}

.iframe-card {
  padding: 0;
  overflow: hidden;
}

.druid-frame {
  width: 100%;
  height: calc(100vh - 190px);
  min-height: 520px;
  display: block;
  background: #fff;
}
</style>
