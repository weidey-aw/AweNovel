<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Delete } from '@element-plus/icons-vue'
import { deleteOwnComment, getCommentList, publishComment, resolveAssetUrl } from '@gal/shared'
import type { Comment } from '@gal/shared'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ targetType: string; targetId: number }>()
const emit = defineEmits<{ (e: 'changed', total: number): void }>()

const userStore = useUserStore()
const list = ref<Comment[]>([])
const total = ref(0)
const loading = ref(false)
const content = ref('')
const submitting = ref(false)
const replyPid = ref<number | undefined>(undefined)
const replyName = ref('')

const isMine = (c: Comment) =>
  !!userStore.userInfo?.userId && Number(c.userId) === Number(userStore.userInfo.userId)

async function load() {
  loading.value = true
  try {
    const res = await getCommentList({
      pageNum: 1,
      pageSize: 50,
      targetType: props.targetType,
      targetId: props.targetId,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? list.value.length
    emit('changed', total.value)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '评论加载失败')
  } finally {
    loading.value = false
  }
}

function startReply(c: Comment) {
  replyPid.value = c.commentId
  replyName.value = c.nickName || '该用户'
}

function cancelReply() {
  replyPid.value = undefined
  replyName.value = ''
}

/** 删除自己的评论 */
async function removeOwn(c: Comment) {
  try {
    await ElMessageBox.confirm('确认删除这条评论吗？', '删除评论', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteOwnComment(c.commentId)
    ElMessage.success('已删除')
    await load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function submit() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再发表评论')
    return
  }
  const text = content.value.trim()
  if (!text) return
  submitting.value = true
  try {
    await publishComment({
      targetType: props.targetType,
      targetId: props.targetId,
      pid: replyPid.value,
      content: text,
    })
    ElMessage.success('评论成功')
    content.value = ''
    cancelReply()
    await load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '评论失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
watch(() => props.targetId, load)
</script>

<template>
  <section class="comments">
    <div class="comments-head">
      <span class="comments-title">评论</span>
      <span class="comments-total">{{ total }}</span>
    </div>

    <el-empty v-if="!loading && !list.length" description="还没有评论，来抢沙发～" :image-size="80" />

    <el-skeleton v-else-if="loading" :rows="3" animated />

    <div v-else class="comment-list">
      <div v-for="c in list" :key="c.commentId" class="comment-item">
        <el-avatar :size="38" :src="c.avatar ? resolveAssetUrl(c.avatar) : undefined" class="c-avatar">
          {{ (c.nickName || 'U').slice(0, 1) }}
        </el-avatar>
        <div class="c-body">
          <div class="c-meta">
            <span class="c-name">{{ c.nickName || '匿名用户' }}</span>
            <span class="c-time">{{ c.createTime || '' }}</span>
          </div>
          <div class="c-content">
            <template v-if="c.pid">
              <span class="c-reply">回复 #{{ c.pid }}：</span>
            </template>
            {{ c.content }}
          </div>
          <div class="c-actions">
            <span class="c-like">👍 {{ c.likeCount || 0 }}</span>
            <span class="c-reply-btn" @click="startReply(c)">回复</span>
            <span v-if="isMine(c)" class="c-del-btn" @click="removeOwn(c)">
              <el-icon><Delete /></el-icon>删除
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="comment-editor">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        :placeholder="userStore.isLoggedIn ? '友善交流，理性讨论～' : '登录后即可发表评论'"
      />
      <div class="editor-bar">
        <span v-if="replyPid" class="replying">
          回复 @{{ replyName }}
          <el-icon class="cancel-reply" @click="cancelReply"><Close /></el-icon>
        </span>
        <el-button
          class="grad-btn"
          :loading="submitting"
          :disabled="!content.trim()"
          @click="submit"
        >
          发表评论
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.comments {
  margin-top: 28px;
}

.comments-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 14px;
}

.comments-title {
  font-size: 18px;
  font-weight: 700;
}

.comments-total {
  color: var(--text-dim);
  font-size: 13px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.c-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--grad-main);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
  font-size: 14px;
}

.c-body {
  flex: 1;
  background: var(--bg-soft);
  border: 1px solid rgba(46, 46, 72, 0.6);
  border-radius: 10px;
  padding: 10px 14px;
}

.c-meta {
  display: flex;
  gap: 10px;
  align-items: center;
}

.c-name {
  font-weight: 600;
  font-size: 13.5px;
  color: var(--accent);
}

.c-time {
  font-size: 12px;
  color: var(--text-dim);
}

.c-content {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

.c-reply {
  color: var(--text-dim);
}

.c-actions {
  margin-top: 6px;
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--text-dim);
}

.c-reply-btn {
  cursor: pointer;
  transition: color 0.2s;
}

.c-reply-btn:hover {
  color: var(--accent);
}

.c-del-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  transition: color 0.2s;
}

.c-del-btn:hover {
  color: #f56c6c;
}

.comment-editor {
  background: var(--bg-soft);
  border: 1px solid rgba(46, 46, 72, 0.6);
  border-radius: 12px;
  padding: 14px;
}

.editor-bar {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.replying {
  font-size: 12.5px;
  color: var(--accent);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.cancel-reply {
  cursor: pointer;
}
</style>
