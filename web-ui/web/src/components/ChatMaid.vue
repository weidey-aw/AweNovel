<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Promotion } from '@element-plus/icons-vue'
import { streamChat } from '@gal/shared'
import { useUserStore } from '@/stores/user'

interface ChatItem {
  role: 'user' | 'maid'
  content: string
  streaming?: boolean
}

const userStore = useUserStore()

const open = ref(false)
const messages = ref<ChatItem[]>([
  { role: 'maid', content: '欢迎回来，我是伊卡洛斯 ✦ 有什么想聊的都可以告诉我哦～' },
])
const input = ref('')
const sending = ref(false)
const listRef = ref<HTMLElement>()
let abortCtrl: AbortController | null = null

const suggestions = ['推荐几款治愈系 Galgame', '今天有什么新游戏？', '讲讲废萌作的特点']

function toggle() {
  open.value = !open.value
  if (open.value) scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  listRef.value?.scrollTo({ top: listRef.value.scrollHeight, behavior: 'smooth' })
}

function useSuggestion(text: string) {
  input.value = text
  send()
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再与伊卡洛斯聊天（每次对话消耗 1 积分）')
    return
  }
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const maid: ChatItem = { role: 'maid', content: '', streaming: true }
  messages.value.push(maid)
  sending.value = true
  scrollToBottom()

  abortCtrl = new AbortController()
  try {
    await streamChat({
      message: text,
      signal: abortCtrl.signal,
      onChunk: (chunk) => {
        maid.content += chunk
        scrollToBottom()
      },
    })
  } catch (err: unknown) {
    if (err instanceof DOMException && err.name === 'AbortError') return
    const msg = err instanceof Error ? err.message : '未知错误'
    maid.content = maid.content
      ? `${maid.content}\n\n[连接中断，请稍后再试]`
      : `抱歉，我这边出了点问题：${msg}`
  } finally {
    maid.streaming = false
    sending.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <!-- 悬浮看板娘 -->
  <div class="maid">
    <transition name="maid-pop">
      <div v-if="open" class="maid-panel">
        <div class="maid-head">
          <div class="maid-avatar">✦</div>
          <div class="maid-title">
            <div class="maid-name">伊卡洛斯</div>
            <div class="maid-status">
              <span class="dot" />在线 · 每次对话消耗 1 积分
            </div>
          </div>
          <el-icon class="maid-close" :size="18" @click="toggle"><Close /></el-icon>
        </div>

        <div ref="listRef" class="maid-body">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div v-if="m.role === 'maid'" class="msg-avatar">✦</div>
            <div class="msg-bubble">
              <span v-if="m.streaming && !m.content" class="typing"><i /><i /><i /></span>
              <span v-else class="msg-text">{{ m.content }}</span>
              <span v-if="m.streaming && m.content" class="caret" />
            </div>
            <div v-if="m.role === 'user'" class="msg-avatar user">我</div>
          </div>
        </div>

        <div class="maid-sugs">
          <button
            v-for="s in suggestions"
            :key="s"
            class="sug"
            :disabled="sending"
            @click="useSuggestion(s)"
          >
            {{ s }}
          </button>
        </div>

        <div class="maid-input">
          <el-input
            v-model="input"
            placeholder="和伊卡洛斯说点什么…"
            :disabled="sending"
            @keyup.enter="send"
          />
          <el-button
            class="grad-btn"
            :icon="Promotion"
            :loading="sending"
            @click="send"
          />
        </div>
      </div>
    </transition>

    <button class="maid-fab" @click="toggle">
      <span class="fab-icon">✦</span>
      <span v-if="!open" class="fab-tip">伊卡洛斯</span>
    </button>
  </div>
</template>

<style scoped>
.maid {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 999;
}

.maid-fab {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: var(--grad-main);
  color: #fff;
  box-shadow: 0 8px 26px rgba(236, 72, 153, 0.45), 0 0 0 4px rgba(139, 92, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: transform 0.25s;
}

.maid-fab:hover {
  transform: scale(1.08) rotate(8deg);
}

.fab-tip {
  position: absolute;
  right: 68px;
  white-space: nowrap;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  color: var(--text-main);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  box-shadow: var(--shadow-card);
}

.maid-panel {
  position: absolute;
  right: 0;
  bottom: 72px;
  width: 360px;
  max-width: calc(100vw - 32px);
  height: 500px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: linear-gradient(170deg, rgba(139, 92, 246, 0.08), transparent 30%), var(--bg-card);
  border: 1px solid var(--border-glow);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}

.maid-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-glow);
}

.maid-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--grad-main);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  flex-shrink: 0;
}

.maid-title {
  flex: 1;
}

.maid-name {
  font-weight: 700;
  font-size: 15px;
}

.maid-status {
  font-size: 12px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px #34d399;
}

.maid-close {
  cursor: pointer;
  color: var(--text-sub);
}

.maid-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--grad-main);
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-avatar.user {
  background: #3b3b5c;
  font-size: 11px;
}

.msg-bubble {
  max-width: 78%;
  padding: 9px 13px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.7;
  background: var(--bg-soft);
  border: 1px solid rgba(46, 46, 72, 0.6);
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-row.user .msg-bubble {
  background: linear-gradient(120deg, rgba(139, 92, 246, 0.85), rgba(236, 72, 153, 0.85));
  color: #fff;
  border: none;
}

.caret {
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--accent);
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.2;
  }
}

.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: bounce 1s infinite;
}

.typing i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes bounce {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  50% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.maid-sugs {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  flex-wrap: wrap;
}

.sug {
  border: 1px solid var(--border-glow);
  background: transparent;
  color: var(--text-sub);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.sug:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--accent);
}

.sug:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.maid-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--border-glow);
}

.maid-pop-enter-active,
.maid-pop-leave-active {
  transition: all 0.22s ease;
}

.maid-pop-enter-from,
.maid-pop-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}

@media (max-width: 480px) {
  .maid {
    right: 14px;
    bottom: 14px;
  }
}
</style>
