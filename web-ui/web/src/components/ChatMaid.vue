<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Promotion } from '@element-plus/icons-vue'
import { streamChat } from '@gal/shared'
import type { ChatHistoryItem } from '@gal/shared'
import { useUserStore } from '@/stores/user'
import { createLive2D, type Live2DHandle } from './live2d'

interface ChatItem {
  role: 'ai' | 'user'
  content: string
  streaming?: boolean
}

const userStore = useUserStore()

const open = ref(false)
const messages = ref<ChatItem[]>([
  {
    role: 'ai',
    content:
      '我是 DeepSeek 的鲸鱼娘形态，现在常驻在 AweNovel 陪你逛站～想聊游戏、动画，或者让我帮你分析点什么，点我就能说话哦。',
  },
])
const input = ref('')
const sending = ref(false)
const unread = ref(0)
const listRef = ref<HTMLElement>()
const stageRef = ref<HTMLElement>()

/** Live2D 状态：idle / loading / ready / unavailable（无立绘时显示鲸鱼娘静态形象） */
const live2dState = ref<'idle' | 'loading' | 'ready' | 'unavailable'>('idle')
let live2d: Live2DHandle | null = null
let abortCtrl: AbortController | null = null

const suggestions = ['推荐几款治愈系 Galgame', '你是什么样的人工智能呀', '今天有什么新游戏？']

const statusText = ref('在线')

function syncStatus() {
  statusText.value =
    live2dState.value === 'ready' ? '在线 · 立绘模式' : live2dState.value === 'loading' ? '连线中…' : '在线 · 电波模式'
}

/** 初始化立绘（只尝试一次；失败则一直用静态形象） */
async function initLive2D() {
  if (live2dState.value === 'loading' || live2dState.value === 'ready') return
  if (!stageRef.value) return
  live2dState.value = 'loading'
  syncStatus()
  live2d = await createLive2D(stageRef.value)
  live2dState.value = live2d ? 'ready' : 'unavailable'
  syncStatus()
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    unread.value = 0
    nextTick(scrollToBottom)
  }
}

async function scrollToBottom() {
  await nextTick()
  listRef.value?.scrollTo({ top: listRef.value.scrollHeight, behavior: 'smooth' })
}

function useSuggestion(text: string) {
  if (sending.value) return
  input.value = text
  send()
}

function buildHistory(): ChatHistoryItem[] {
  const usable = messages.value.filter((m) => !m.streaming && !m.content.startsWith('['))
  return usable
    .slice(0, -1)
    .slice(-12)
    .map((m) => ({ role: m.role === 'ai' ? 'assistant' : 'user', content: m.content }))
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后才能和我聊天哦（每次对话消耗 1 积分）')
    return
  }

  const history = buildHistory()
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const reply: ChatItem = { role: 'ai', content: '', streaming: true }
  messages.value.push(reply)
  sending.value = true
  if (!open.value) open.value = true
  scrollToBottom()

  abortCtrl = new AbortController()
  try {
    await streamChat({
      message: text,
      history,
      signal: abortCtrl.signal,
      onChunk: (chunk) => {
        reply.content += chunk
        scrollToBottom()
      },
    })
  } catch (err: unknown) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      reply.content = reply.content || '（我先去整理一下鲸鱼尾巴）'
    } else if (!reply.content) {
      reply.content = `呜…我这边出了点问题：${err instanceof Error ? err.message : '未知错误'}`
    } else {
      reply.content += '\n\n[连接中断，稍后再试试]'
    }
  } finally {
    reply.streaming = false
    sending.value = false
    if (!open.value) unread.value += 1
    scrollToBottom()
  }
}

onMounted(() => {
  // 常驻显示：页面加载后立即初始化立绘
  nextTick(() => initLive2D())
})

onBeforeUnmount(() => {
  abortCtrl?.abort()
  live2d?.destroy()
  live2d = null
})
</script>

<template>
  <!-- 常驻看板娘：DeepSeek 鲸鱼娘 -->
  <div class="maid" aria-label="DeepSeek 鲸鱼娘">
    <!-- 立绘 / 静态形象（常驻，不隐藏） -->
    <div class="mascot" :class="`mascot-${live2dState}`" @click="toggle">
      <div v-show="live2dState === 'ready'" ref="stageRef" class="stage-canvas" />
      <svg v-if="live2dState !== 'ready'" class="whale" viewBox="0 0 200 170" aria-hidden="true">
        <defs>
          <linearGradient id="whaleGrad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#7dd3fc" />
            <stop offset="1" stop-color="#2563eb" />
          </linearGradient>
          <linearGradient id="whaleFin" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#60a5fa" />
            <stop offset="1" stop-color="#3b82f6" />
          </linearGradient>
        </defs>
        <!-- 喷水 -->
        <path d="M96 30 q-10 -16 2 -28 q10 12 -2 28" fill="#93c5fd" opacity=".75" />
        <circle cx="82" cy="26" r="3" fill="#93c5fd" opacity=".7" />
        <circle cx="112" cy="30" r="2.4" fill="#93c5fd" opacity=".7" />
        <!-- 尾巴 -->
        <path d="M30 120 q-20 -8 -28 4 q12 12 28 6 z" fill="url(#whaleFin)" />
        <!-- 身体 -->
        <ellipse cx="100" cy="106" rx="68" ry="46" fill="url(#whaleGrad)" />
        <!-- 肚皮 -->
        <ellipse cx="106" cy="126" rx="48" ry="26" fill="#e0f2fe" opacity=".78" />
        <!-- 侧鳍 -->
        <path d="M128 116 q18 12 4 26 q-4 -14 -16 -16 z" fill="url(#whaleFin)" />
        <!-- 眼睛 -->
        <circle cx="72" cy="94" r="8.5" fill="#0f172a" />
        <circle cx="75" cy="91" r="2.6" fill="#fff" />
        <!-- 腮红 -->
        <ellipse cx="58" cy="109" rx="7" ry="4" fill="#fda4af" opacity=".8" />
        <!-- 嘴 -->
        <path d="M62 114 q7 6 13 1" stroke="#0f172a" stroke-width="2.4" fill="none" stroke-linecap="round" />
        <!-- 鲸鱼娘的小发饰 -->
        <path d="M124 64 q12 -12 24 -1 q-12 9 -24 1" fill="#f472b6" />
        <circle cx="136" cy="61" r="3.4" fill="#fbcfe8" />
      </svg>
      <span v-if="live2dState === 'loading'" class="loading-tip">连线中…</span>
      <span class="name-tag">
        DeepSeek 鲸鱼娘
        <em>{{ statusText }}</em>
      </span>
      <span v-if="unread && !open" class="unread">{{ unread > 9 ? '9+' : unread }}</span>
    </div>

    <!-- 聊天气泡面板（点立绘开关，立绘始终常驻） -->
    <transition name="maid-pop">
      <section v-if="open" class="maid-panel" aria-label="与 DeepSeek 鲸鱼娘对话">
        <header class="maid-head">
          <div class="maid-avatar">
            <svg viewBox="0 0 200 170" aria-hidden="true">
              <ellipse cx="100" cy="106" rx="68" ry="46" fill="#3b82f6" />
              <ellipse cx="106" cy="126" rx="48" ry="26" fill="#dbeafe" opacity=".7" />
              <circle cx="72" cy="94" r="8.5" fill="#0f172a" />
              <circle cx="75" cy="91" r="2.6" fill="#fff" />
              <path d="M62 114 q7 6 13 1" stroke="#0f172a" stroke-width="3" fill="none" stroke-linecap="round" />
            </svg>
          </div>
          <div class="maid-title">
            <div class="maid-name">
              DeepSeek 鲸鱼娘
              <span class="maid-badge">AI</span>
            </div>
            <div class="maid-status"><span class="dot" />{{ statusText }} · 每次对话 1 积分</div>
          </div>
          <button class="maid-close" aria-label="收起对话" @click="toggle">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </header>

        <div ref="listRef" class="maid-body">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div v-if="m.role === 'ai'" class="msg-avatar">
              <svg viewBox="0 0 200 170" aria-hidden="true">
                <ellipse cx="100" cy="106" rx="68" ry="46" fill="#3b82f6" />
                <ellipse cx="106" cy="126" rx="48" ry="26" fill="#dbeafe" opacity=".7" />
                <circle cx="72" cy="94" r="10" fill="#fff" />
                <circle cx="74" cy="95" r="5" fill="#0f172a" />
                <path d="M62 116 q7 6 13 1" stroke="#0f172a" stroke-width="3" fill="none" stroke-linecap="round" />
              </svg>
            </div>
            <div class="msg-bubble">
              <span v-if="m.streaming && !m.content" class="typing"><i /><i /><i /></span>
              <span v-else class="msg-text">{{ m.content }}</span>
              <span v-if="m.streaming && m.content" class="caret" />
            </div>
            <div v-if="m.role === 'user'" class="msg-avatar user">我</div>
          </div>
        </div>

        <div class="maid-sugs">
          <button v-for="s in suggestions" :key="s" class="sug" :disabled="sending" @click="useSuggestion(s)">
            {{ s }}
          </button>
        </div>

        <div class="maid-input">
          <el-input
            v-model="input"
            placeholder="和鲸鱼娘说点什么…"
            :disabled="sending"
            maxlength="500"
            @keyup.enter="send"
          />
          <el-button class="grad-btn" :icon="Promotion" :loading="sending" @click="send" />
        </div>
      </section>
    </transition>
  </div>
</template>

<style scoped>
/* 常驻容器：不拦截页面点击，只有立绘和面板可交互 */
.maid {
  position: fixed;
  right: 16px;
  bottom: 0;
  z-index: 90;
  pointer-events: none;
}

.mascot {
  position: relative;
  width: 250px;
  height: 312px;
  pointer-events: auto;
  cursor: pointer;
  transition: transform 0.25s;
  filter: drop-shadow(0 12px 24px rgba(37, 99, 235, 0.18));
}

/* 立绘背后的柔光，避免直接压在内容卡片上显得脏 */
.mascot::before {
  content: '';
  position: absolute;
  left: 4%;
  right: 4%;
  bottom: 4%;
  height: 72%;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 60%, var(--accent-soft), transparent 68%);
  filter: blur(10px);
  z-index: 0;
  pointer-events: none;
}

.mascot:hover {
  transform: translateY(-4px);
}

.stage-canvas {
  position: absolute;
  inset: 0;
}

.whale {
  position: relative;
  z-index: 1;
  width: 92%;
  height: 92%;
  margin: 8% 4% 0;
  animation: whale-float 4.4s ease-in-out infinite;
}

@keyframes whale-float {
  0%,
  100% {
    transform: translateY(0) rotate(-1deg);
  }
  50% {
    transform: translateY(-10px) rotate(1.2deg);
  }
}

.name-tag {
  position: absolute;
  left: 8px;
  bottom: 8px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  padding: 4px 12px;
  z-index: 2;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-main);
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  box-shadow: var(--shadow-card);
  white-space: nowrap;
}

.name-tag em {
  font-style: normal;
  font-weight: 400;
  font-size: 10.5px;
  color: var(--text-dim);
}

.loading-tip {
  position: absolute;
  left: 50%;
  top: 12px;
  transform: translateX(-50%);
  font-size: 11.5px;
  color: var(--text-dim);
}

.unread {
  position: absolute;
  top: 28px;
  right: 44px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 10px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  line-height: 20px;
  text-align: center;
  animation: pop 1.2s ease-in-out infinite;
}

@keyframes pop {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.12);
  }
}

/* ---------- 聊天面板 ---------- */
.maid-panel {
  position: absolute;
  right: 0;
  bottom: 322px;
  width: 372px;
  max-width: calc(100vw - 24px);
  height: 470px;
  max-height: calc(100vh - 380px);
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  border-radius: 18px;
  box-shadow: var(--shadow-panel);
  overflow: hidden;
  pointer-events: auto;
}

.maid-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-glow);
  background: var(--bg-elevated);
}

.maid-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--grad-main);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  flex-shrink: 0;
}

.maid-avatar svg {
  width: 100%;
  height: 100%;
}

.maid-title {
  flex: 1;
  min-width: 0;
}

.maid-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  font-size: 15px;
  color: var(--text-main);
}

.maid-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 6px;
  color: #fff;
  background: var(--grad-main);
}

.maid-status {
  font-size: 12px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px #34d399;
}

.maid-close {
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--text-sub);
  display: flex;
  align-items: center;
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
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--bg-soft);
  border: 1px solid var(--border-glow);
  padding: 4px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.msg-avatar svg {
  width: 100%;
  height: 100%;
}

.msg-avatar.user {
  background: var(--grad-main);
  border: none;
  color: #fff;
  font-size: 11px;
}

.msg-bubble {
  max-width: 78%;
  padding: 9px 13px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.7;
  background: var(--bg-soft);
  border: 1px solid var(--border-glow);
  color: var(--text-main);
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-row.user .msg-bubble {
  background: var(--grad-main);
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
  padding: 6px 12px 0;
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
}

.maid-pop-enter-active,
.maid-pop-leave-active {
  transition: all 0.22s ease;
}

.maid-pop-enter-from,
.maid-pop-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

@media (max-width: 768px) {
  .maid {
    right: 6px;
  }

  .mascot {
    width: 178px;
    height: 222px;
  }

  .maid-panel {
    width: calc(100vw - 16px);
    right: 6px;
    bottom: 208px;
    height: 420px;
    max-height: calc(100vh - 240px);
  }

  .name-tag {
    font-size: 11px;
    padding: 4px 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .whale {
    animation: none;
  }
}
</style>
