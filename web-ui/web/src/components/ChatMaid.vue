<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Promotion } from '@element-plus/icons-vue'
import { streamChat } from '@gal/shared'
import type { ChatHistoryItem } from '@gal/shared'
import { useUserStore } from '@/stores/user'
import { createLive2D, type Live2DHandle } from './live2d'

/** Live2D 模型地址（模型目录不入 git，缺失时自动降级为静态小鱼） */
const MODEL_URL = '/live2d/c_0120.model3.json'

interface ChatItem {
  role: 'fish' | 'user'
  content: string
  streaming?: boolean
}

const userStore = useUserStore()

const open = ref(false)
const messages = ref<ChatItem[]>([
  {
    role: 'fish',
    content: '咕噜咕噜～主人终于来啦！我是蓝色大肥鱼，AweNovel 的看板娘，想聊游戏、动画，或者只是摸摸鱼都可以哦～',
  },
])
const input = ref('')
const sending = ref(false)
const unread = ref(0)
const listRef = ref<HTMLElement>()
const stageRef = ref<HTMLElement>()

/** Live2D 状态：idle 未初始化 / loading / ready / unavailable 无立绘 */
const live2dState = ref<'idle' | 'loading' | 'ready' | 'unavailable'>('idle')
let live2d: Live2DHandle | null = null
let abortCtrl: AbortController | null = null

const suggestions = ['推荐几款治愈系 Galgame', '大肥鱼今天吃什么', '讲讲废萌作的特点']

const statusText = ref('在线')

function syncStatus() {
  statusText.value =
    live2dState.value === 'ready'
      ? '在线 · 立绘模式'
      : live2dState.value === 'loading'
        ? '立绘加载中…'
        : '在线 · 电波模式'
}
watch(live2dState, syncStatus, { immediate: true })

/** 初始化立绘（只尝试一次；失败则一直用静态形象） */
async function initLive2D() {
  if (live2dState.value === 'loading' || live2dState.value === 'ready') return
  if (!stageRef.value) return
  live2dState.value = 'loading'
  live2d = await createLive2D(stageRef.value, MODEL_URL)
  live2dState.value = live2d ? 'ready' : 'unavailable'
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    unread.value = 0
    nextTick(() => {
      initLive2D()
      scrollToBottom()
    })
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
  // 去掉欢迎语与错误提示，最多带 12 条上文
  const usable = messages.value.filter((m) => !m.streaming && !m.content.startsWith('[连接中断'))
  const items: ChatHistoryItem[] = usable.map((m) => ({
    role: m.role === 'fish' ? 'assistant' : 'user',
    content: m.content,
  }))
  return items.slice(0, -1).slice(-12)
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后才能和大肥鱼聊天哦（每次对话消耗 1 积分）')
    return
  }

  const history = buildHistory()
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const fish: ChatItem = { role: 'fish', content: '', streaming: true }
  messages.value.push(fish)
  sending.value = true
  scrollToBottom()

  abortCtrl = new AbortController()
  try {
    await streamChat({
      message: text,
      history,
      signal: abortCtrl.signal,
      onChunk: (chunk) => {
        fish.content += chunk
        scrollToBottom()
      },
    })
  } catch (err: unknown) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      fish.content = fish.content || '（大肥鱼被叫走了）'
    } else if (!fish.content) {
      const msg = err instanceof Error ? err.message : '未知错误'
      fish.content = `呜…大肥鱼这边出了点问题：${msg}`
    } else {
      fish.content += '\n\n[连接中断，稍后再试试]'
    }
  } finally {
    fish.streaming = false
    sending.value = false
    if (!open.value) unread.value += 1
    scrollToBottom()
  }
}

/** 关闭时销毁渲染器，避免后台占用 GPU/内存 */
function destroyLive2D() {
  live2d?.destroy()
  live2d = null
  if (live2dState.value === 'ready') live2dState.value = 'idle'
}

watch(open, (value) => {
  if (!value) {
    abortCtrl?.abort()
    abortCtrl = null
  }
})

onBeforeUnmount(() => {
  abortCtrl?.abort()
  destroyLive2D()
})
</script>

<template>
  <!-- 悬浮看板娘：蓝色大肥鱼 -->
  <div class="maid">
    <transition name="maid-pop">
      <section v-if="open" class="maid-panel" aria-label="蓝色大肥鱼 AI 看板娘">
        <header class="maid-head">
          <div class="maid-avatar">
            <svg viewBox="0 0 120 90" aria-hidden="true">
              <defs>
                <linearGradient id="fishHeadGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="#7dd3fc" />
                  <stop offset="1" stop-color="#2563eb" />
                </linearGradient>
              </defs>
              <path d="M14 45 L2 30 Q7 45 2 60 Z" fill="#38bdf8" opacity=".9" />
              <ellipse cx="62" cy="45" rx="46" ry="34" fill="url(#fishHeadGrad)" />
              <ellipse cx="66" cy="58" rx="30" ry="18" fill="#e0f2fe" opacity=".72" />
              <path d="M52 14 Q62 2 76 14 Z" fill="#38bdf8" />
              <path d="M74 46 Q86 52 76 62 Z" fill="#60a5fa" />
              <circle cx="44" cy="36" r="9" fill="#fff" />
              <circle cx="46" cy="37" r="5" fill="#0f172a" />
              <circle cx="48" cy="35" r="1.8" fill="#fff" />
              <path d="M26 52 q8 8 16 2" stroke="#0f172a" stroke-width="2.5" fill="none" stroke-linecap="round" />
              <ellipse cx="30" cy="48" rx="6" ry="3.5" fill="#fda4af" opacity=".75" />
            </svg>
          </div>
          <div class="maid-title">
            <div class="maid-name">
              蓝色大肥鱼
              <span class="maid-badge">AI</span>
            </div>
            <div class="maid-status">
              <span class="dot" />{{ statusText }} · 每次对话 1 积分
            </div>
          </div>
          <button class="maid-close" aria-label="关闭" @click="toggle">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </header>

        <!-- 立绘舞台：有 Live2D 用模型，没有则显示静态小鱼 -->
        <div class="stage" :class="`stage-${live2dState}`">
          <div v-show="live2dState === 'ready'" ref="stageRef" class="stage-canvas" />
          <div v-if="live2dState !== 'ready'" class="stage-fallback">
            <svg class="fish" viewBox="0 0 120 90" aria-hidden="true">
              <defs>
                <linearGradient id="fishStageGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="#7dd3fc" />
                  <stop offset="1" stop-color="#2563eb" />
                </linearGradient>
              </defs>
              <path d="M14 45 L2 30 Q7 45 2 60 Z" fill="#38bdf8" opacity=".9" />
              <ellipse cx="62" cy="45" rx="46" ry="34" fill="url(#fishStageGrad)" />
              <ellipse cx="66" cy="58" rx="30" ry="18" fill="#e0f2fe" opacity=".72" />
              <path d="M52 14 Q62 2 76 14 Z" fill="#38bdf8" />
              <path d="M74 46 Q86 52 76 62 Z" fill="#60a5fa" />
              <circle cx="44" cy="36" r="9" fill="#fff" />
              <circle cx="46" cy="37" r="5" fill="#0f172a" />
              <circle cx="48" cy="35" r="1.8" fill="#fff" />
              <path d="M26 52 q8 8 16 2" stroke="#0f172a" stroke-width="2.5" fill="none" stroke-linecap="round" />
              <ellipse cx="30" cy="48" rx="6" ry="3.5" fill="#fda4af" opacity=".75" />
            </svg>
            <div class="bubbles" aria-hidden="true"><i /><i /><i /></div>
          </div>
          <span v-if="live2dState === 'loading'" class="stage-tip">立绘加载中…</span>
        </div>

        <div ref="listRef" class="maid-body">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div v-if="m.role === 'fish'" class="msg-avatar">
              <svg viewBox="0 0 120 90" aria-hidden="true">
                <path d="M14 45 L2 30 Q7 45 2 60 Z" fill="#38bdf8" />
                <ellipse cx="62" cy="45" rx="46" ry="34" fill="#3b82f6" />
                <circle cx="44" cy="36" r="9" fill="#fff" />
                <circle cx="46" cy="37" r="5" fill="#0f172a" />
                <path d="M26 52 q8 8 16 2" stroke="#0f172a" stroke-width="3" fill="none" stroke-linecap="round" />
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
            placeholder="和大肥鱼说点什么…"
            :disabled="sending"
            maxlength="500"
            @keyup.enter="send"
          />
          <el-button class="grad-btn" :icon="Promotion" :loading="sending" @click="send" />
        </div>
      </section>
    </transition>

    <button class="maid-fab" aria-label="蓝色大肥鱼" @click="toggle">
      <svg viewBox="0 0 120 90" aria-hidden="true">
        <path d="M14 45 L2 30 Q7 45 2 60 Z" fill="#7dd3fc" />
        <ellipse cx="62" cy="45" rx="46" ry="34" fill="#3b82f6" />
        <ellipse cx="66" cy="58" rx="30" ry="18" fill="#dbeafe" opacity=".6" />
        <circle cx="44" cy="36" r="9" fill="#fff" />
        <circle cx="46" cy="37" r="5" fill="#0f172a" />
        <circle cx="48" cy="35" r="1.8" fill="#fff" />
        <path d="M26 52 q8 8 16 2" stroke="#0f172a" stroke-width="3" fill="none" stroke-linecap="round" />
      </svg>
      <span v-if="!open" class="fab-tip">蓝色大肥鱼</span>
      <span v-if="unread && !open" class="fab-dot">{{ unread > 9 ? '9+' : unread }}</span>
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

/* ---------- 悬浮按钮 ---------- */
.maid-fab {
  position: relative;
  width: 62px;
  height: 62px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: var(--grad-main);
  box-shadow: var(--shadow-fab);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 9px;
  transition: transform 0.25s, box-shadow 0.25s;
}

.maid-fab:hover {
  transform: scale(1.08) rotate(-6deg);
}

.maid-fab svg {
  width: 100%;
  height: 100%;
  animation: fish-float 4s ease-in-out infinite;
}

.fab-tip {
  position: absolute;
  right: 72px;
  white-space: nowrap;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  color: var(--text-main);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  box-shadow: var(--shadow-card);
}

.fab-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

/* ---------- 面板 ---------- */
.maid-panel {
  position: absolute;
  right: 0;
  bottom: 76px;
  width: 380px;
  max-width: calc(100vw - 32px);
  height: 620px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  border-radius: 18px;
  box-shadow: var(--shadow-panel);
  overflow: hidden;
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
  padding: 7px;
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

/* ---------- 立绘舞台 ---------- */
.stage {
  position: relative;
  height: 210px;
  flex-shrink: 0;
  overflow: hidden;
  background:
    radial-gradient(120% 90% at 50% 110%, var(--stage-glow), transparent 65%),
    var(--bg-soft);
  border-bottom: 1px solid var(--border-glow);
}

.stage-canvas {
  position: absolute;
  inset: 0;
}

.stage-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fish {
  width: 190px;
  height: auto;
  animation: fish-float 3.6s ease-in-out infinite;
  filter: drop-shadow(0 10px 18px rgba(37, 99, 235, 0.28));
}

.bubbles {
  position: absolute;
  left: 30%;
  bottom: 10%;
  display: flex;
  gap: 12px;
}

.bubbles i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--bubble-color);
  animation: bubble-rise 3.2s ease-in infinite;
}

.bubbles i:nth-child(2) {
  animation-delay: 1s;
  width: 5px;
  height: 5px;
}

.bubbles i:nth-child(3) {
  animation-delay: 2s;
  width: 9px;
  height: 9px;
}

.stage-tip {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11.5px;
  color: var(--text-dim);
}

@keyframes fish-float {
  0%,
  100% {
    transform: translateY(0) rotate(-1deg);
  }
  50% {
    transform: translateY(-9px) rotate(1.5deg);
  }
}

@keyframes bubble-rise {
  0% {
    transform: translateY(0) scale(0.8);
    opacity: 0;
  }
  25% {
    opacity: 0.9;
  }
  100% {
    transform: translateY(-90px) scale(1.1);
    opacity: 0;
  }
}

/* ---------- 消息 ---------- */
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
  padding: 5px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
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

/* ---------- 输入区 ---------- */
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
  transform: translateY(16px) scale(0.96);
}

@media (max-width: 480px) {
  .maid {
    right: 14px;
    bottom: 14px;
  }

  .maid-panel {
    height: calc(100vh - 140px);
  }

  .stage {
    height: 170px;
  }
}
</style>
