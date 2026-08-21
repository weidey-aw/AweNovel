<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <div class="footer-logo">✦ AweNovel</div>
        <p>一个属于 Galgame 爱好者的家：游戏库、攻略评测、资源分享与 AI 看板娘。</p>
      </div>
      <div class="footer-links">
        <span class="link" @click="router.push('/games')">游戏库</span>
        <span class="link" @click="router.push('/articles')">文章</span>
        <span class="link" @click="router.push('/login')">登录</span>
        <span class="link" @click="router.push('/register')">注册</span>
      </div>
    </div>
    <div class="footer-copy">© {{ year }} AweNovel · 本站内容仅供学习交流，请支持正版</div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 40px;
  border-top: 1px solid var(--border-glow);
  background: rgba(13, 13, 21, 0.9);
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 20px 20px;
  display: flex;
  justify-content: space-between;
  gap: 30px;
  flex-wrap: wrap;
}

.footer-logo {
  font-size: 18px;
  font-weight: 800;
  background: var(--grad-main);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.footer-brand p {
  color: var(--text-dim);
  font-size: 13px;
  margin: 10px 0 0;
  max-width: 420px;
  line-height: 1.7;
}

.footer-links {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding-top: 6px;
}

.link {
  color: var(--text-sub);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
}

.link:hover {
  color: var(--accent);
}

.footer-copy {
  text-align: center;
  padding: 14px 20px;
  border-top: 1px solid rgba(46, 46, 72, 0.5);
  color: var(--text-dim);
  font-size: 12px;
}
</style>
