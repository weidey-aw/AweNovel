<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Coin, Calendar, Promotion, Edit, Lock } from '@element-plus/icons-vue'
import {
  getFavorites,
  getFollowing,
  getMessages,
  getPointLogs,
  markMessageRead,
  signDaily,
  resolveAssetUrl,
  updatePassword,
  updateProfile,
  uploadAvatar,
} from '@gal/shared'
import type { FollowItem, Favorite, Message, PointLog } from '@gal/shared'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref((route.query.tab as string) || 'overview')

/* 编辑资料 */
const editDialog = reactive({
  visible: false,
  saving: false,
  nickName: '',
  email: '',
  phonenumber: '',
  sex: '2',
  avatarFile: null as File | null,
  avatarPreview: '',
})

function openEdit() {
  const u = userStore.userInfo
  editDialog.nickName = u?.nickName || u?.nickname || ''
  editDialog.email = u?.email || ''
  editDialog.phonenumber = u?.phonenumber || ''
  editDialog.sex = u?.sex || '2'
  editDialog.avatarFile = null
  editDialog.avatarPreview = ''
  editDialog.visible = true
}

function onAvatarChange(file: { raw?: File }) {
  if (file.raw) {
    editDialog.avatarFile = file.raw
    editDialog.avatarPreview = URL.createObjectURL(file.raw)
  }
}

async function saveEdit() {
  editDialog.saving = true
  try {
    let avatar = userStore.userInfo?.avatar
    if (editDialog.avatarFile) {
      const res = (await uploadAvatar(editDialog.avatarFile)) as unknown as {
        data?: { imgUrl: string }
        imgUrl?: string
      }
      avatar = (res?.data?.imgUrl ?? res?.imgUrl) ?? avatar
    }
    await updateProfile({
      nickName: editDialog.nickName,
      email: editDialog.email,
      phonenumber: editDialog.phonenumber,
      sex: editDialog.sex,
      avatar,
    })
    ElMessage.success('资料已更新')
    editDialog.visible = false
    await userStore.fetchUserInfo()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    editDialog.saving = false
  }
}

/* 修改密码 */
const pwdDialog = reactive({ visible: false, saving: false, oldPassword: '', newPassword: '', confirmPassword: '' })

function openPwd() {
  pwdDialog.oldPassword = ''
  pwdDialog.newPassword = ''
  pwdDialog.confirmPassword = ''
  pwdDialog.visible = true
}

async function savePwd() {
  if (!pwdDialog.oldPassword || !pwdDialog.newPassword) return ElMessage.warning('请输入完整密码')
  if (pwdDialog.newPassword.length < 5) return ElMessage.warning('新密码至少 5 位')
  if (pwdDialog.newPassword !== pwdDialog.confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  pwdDialog.saving = true
  try {
    await updatePassword(pwdDialog.oldPassword, pwdDialog.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialog.visible = false
    userStore.clearAuth()
    router.push('/login')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '修改失败')
  } finally {
    pwdDialog.saving = false
  }
}

/* 签到 */
const signing = ref(false)
const signedToday = ref(false)

async function doSign() {
  if (signedToday.value) return
  signing.value = true
  try {
    const res = (await signDaily()) as unknown as
      | { data?: { continuous: number; points: number; totalPoints: number } }
      | { continuous: number; points: number; totalPoints: number }
    const r = ('data' in res && res.data ? res.data : res) as { continuous: number; points: number; totalPoints: number }
    signedToday.value = true
    ElMessage.success(`签到成功！连续 ${r.continuous} 天，获得 ${r.points} 积分（当前 ${r.totalPoints}）`)
    await userStore.fetchProfile()
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '签到失败'
    if (/已签到|重复|exist/i.test(msg)) {
      signedToday.value = true
      ElMessage.info('今天已经签过到啦～')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    signing.value = false
  }
}

/* 积分流水 */
const logs = ref<PointLog[]>([])
const logsLoading = ref(false)

async function loadLogs() {
  logsLoading.value = true
  try {
    const res = await getPointLogs({ pageNum: 1, pageSize: 20 })
    logs.value = res.rows ?? []
  } catch {
    logs.value = []
  } finally {
    logsLoading.value = false
  }
}

/* 消息 */
const messages = ref<Message[]>([])
const messagesLoading = ref(false)

async function loadMessages() {
  messagesLoading.value = true
  try {
    const res = await getMessages({ pageNum: 1, pageSize: 20 })
    messages.value = res.rows ?? []
  } catch {
    messages.value = []
  } finally {
    messagesLoading.value = false
  }
}

async function readMessage(m: Message) {
  if (m.isRead) return
  try {
    await markMessageRead(m.messageId)
    m.isRead = true
    userStore.unread = Math.max(0, userStore.unread - 1)
  } catch {
    /* 忽略 */
  }
}

/* 收藏 */
const favorites = ref<Favorite[]>([])
const favLoading = ref(false)

async function loadFavorites() {
  favLoading.value = true
  try {
    const res = await getFavorites({ pageNum: 1, pageSize: 20 })
    favorites.value = res.rows ?? []
  } catch {
    favorites.value = []
  } finally {
    favLoading.value = false
  }
}

/* 关注 */
const following = ref<FollowItem[]>([])
const followLoading = ref(false)

async function loadFollowing() {
  followLoading.value = true
  try {
    const res = await getFollowing({ pageNum: 1, pageSize: 20 })
    following.value = res.rows ?? []
  } catch {
    following.value = []
  } finally {
    followLoading.value = false
  }
}

function onTabChange(name: string | number) {
  const key = String(name)
  if (key === 'logs') loadLogs()
  if (key === 'messages') loadMessages()
  if (key === 'favorites') loadFavorites()
  if (key === 'following') loadFollowing()
}

watch(
  () => route.query.tab,
  (v) => {
    if (v) activeTab.value = v as string
  },
)

onMounted(async () => {
  await Promise.all([userStore.fetchUserInfo(), userStore.fetchProfile(), userStore.fetchUnread()])
  onTabChange(activeTab.value)
})
</script>

<template>
  <div class="page-container">
    <div class="section-title">用户中心</div>

    <!-- 画像卡片 -->
    <div class="profile-card panel-card">
      <div class="profile-left">
        <el-avatar :size="72" :src="resolveAssetUrl(userStore.userInfo?.avatar) || undefined" class="p-avatar">
          {{ (userStore.userInfo?.nickName || userStore.userInfo?.userName || 'U').slice(0, 1) }}
        </el-avatar>
        <div class="p-info">
          <div class="p-name">
            {{ userStore.userInfo?.nickName || userStore.userInfo?.userName || '用户' }}
            <el-tag v-if="userStore.profile?.level" size="small" class="p-level" effect="dark">
              Lv.{{ userStore.profile?.level }}
            </el-tag>
          </div>
          <div class="p-email text-dim">{{ userStore.userInfo?.email || '未绑定邮箱' }}</div>
        </div>
      </div>

      <div class="profile-stats">
        <div class="pstat">
          <el-icon :size="20" color="#f59e0b"><Coin /></el-icon>
          <b>{{ userStore.profile?.points ?? 0 }}</b>
          <span>积分</span>
        </div>
        <div class="pstat">
          <el-icon :size="20" color="#a78bfa"><Promotion /></el-icon>
          <b>{{ userStore.profile?.exp ?? 0 }}</b>
          <span>经验</span>
        </div>
        <div class="pstat">
          <el-icon :size="20" color="#34d399"><Calendar /></el-icon>
          <b>{{ userStore.profile?.signStreak ?? 0 }}</b>
          <span>连续签到(天)</span>
        </div>
      </div>

      <div class="profile-sign">
        <el-button
          class="grad-btn"
          size="large"
          :loading="signing"
          :disabled="signedToday"
          @click="doSign"
        >
          {{ signedToday ? '今日已签到' : '每日签到' }}
        </el-button>
        <div class="sign-tip text-dim">
          签到可获得积分与经验，连续签到奖励更多～
        </div>
      </div>

      <div class="profile-actions">
        <el-button text :icon="Edit" @click="openEdit">编辑资料</el-button>
        <el-button text :icon="Lock" @click="openPwd">修改密码</el-button>
      </div>
    </div>

    <!-- 经验进度（占位：每 100 经验升一级的视觉进度） -->
    <div class="exp-bar panel-card">
      <div class="exp-row">
        <span>经验进度</span>
        <span class="text-dim">Lv.{{ userStore.profile?.level ?? 1 }} · {{ userStore.profile?.exp ?? 0 }} / {{ (Math.floor((userStore.profile?.exp ?? 0) / 100) + 1) * 100 }}</span>
      </div>
      <el-progress
        :percentage="Math.min(100, ((userStore.profile?.exp ?? 0) % 100))"
        :stroke-width="10"
        :show-text="false"
        color="#8b5cf6"
      />
    </div>

    <!-- 内容 Tabs -->
    <div class="panel-card tabs-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="积分流水" name="logs">
          <el-skeleton v-if="logsLoading" :rows="4" animated />
          <el-table v-else-if="logs.length" :data="logs" size="default">
            <el-table-column prop="changeType" label="类型" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="Number(row.changeAmount) >= 0 ? 'success' : 'danger'">
                  {{ row.changeType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="变动" width="110">
              <template #default="{ row }">
                <span :class="Number(row.changeAmount) >= 0 ? 'gain' : 'loss'">
                  {{ Number(row.changeAmount) >= 0 ? '+' : '' }}{{ row.changeAmount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="变动后余额" width="110" />
            <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
          <el-empty v-else description="暂无积分流水" />
        </el-tab-pane>

        <el-tab-pane :label="`消息${userStore.unread ? `（${userStore.unread}）` : ''}`" name="messages">
          <el-skeleton v-if="messagesLoading" :rows="4" animated />
          <div v-else-if="messages.length" class="msg-list">
            <div
              v-for="m in messages"
              :key="m.messageId"
              class="msg-item"
              :class="{ unread: !m.isRead }"
              @click="readMessage(m)"
            >
              <span v-if="!m.isRead" class="msg-dot" />
              <div class="msg-main">
                <div class="msg-content">{{ m.content }}</div>
                <div class="msg-time text-dim">{{ m.createTime || '' }}</div>
              </div>
              <el-button v-if="!m.isRead" size="small" text @click.stop="readMessage(m)">标为已读</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无消息" />
        </el-tab-pane>

        <el-tab-pane label="我的收藏" name="favorites">
          <el-skeleton v-if="favLoading" :rows="4" animated />
          <div v-else-if="favorites.length" class="fav-list">
            <router-link
              v-for="f in favorites"
              :key="f.favoriteId ?? f.targetId"
              :to="f.gameId ? `/game/${f.gameId}` : f.targetId ? `/game/${f.targetId}` : '/'"
              class="fav-item"
            >
              <div class="fav-cover">
                <img
                  v-if="f.game?.cover && resolveAssetUrl(f.game.cover)"
                  :src="resolveAssetUrl(f.game.cover)"
                  :alt="f.game?.title"
                />
                <span v-else>🎮</span>
              </div>
              <div class="fav-info">
                <div class="fav-title">{{ f.game?.titleCn || f.game?.title || `资源 #${f.targetId ?? f.gameId ?? ''}` }}</div>
                <div class="fav-time text-dim">收藏于 {{ f.createTime || '未知' }}</div>
              </div>
            </router-link>
          </div>
          <el-empty v-else description="还没有收藏" />
        </el-tab-pane>

        <el-tab-pane label="我的关注" name="following">
          <el-skeleton v-if="followLoading" :rows="4" animated />
          <div v-else-if="following.length" class="follow-list">
            <div v-for="f in following" :key="f.targetUserId" class="follow-item">
              <el-avatar :size="40" :src="resolveAssetUrl(f.avatar) || undefined" class="f-avatar">
                {{ (f.nickname || 'U').slice(0, 1) }}
              </el-avatar>
              <div class="follow-name">{{ f.nickname || `用户 ${f.targetUserId}` }}</div>
              <div class="follow-time text-dim">{{ f.createTime || '' }}</div>
            </div>
          </div>
          <el-empty v-else description="还没有关注任何人" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editDialog.visible" title="编辑资料" width="460px">
      <el-form label-width="76px" label-position="left">
        <el-form-item label="头像">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="onAvatarChange"
          >
            <el-avatar
              :size="72"
              :src="editDialog.avatarPreview || resolveAssetUrl(userStore.userInfo?.avatar) || undefined"
              class="edit-avatar"
            >
              {{ (editDialog.nickName || userStore.userInfo?.userName || 'U').slice(0, 1) }}
            </el-avatar>
            <div class="avatar-hint">点击更换头像</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editDialog.nickName" maxlength="30" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editDialog.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editDialog.phonenumber" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editDialog.sex">
            <el-radio value="0">男</el-radio>
            <el-radio value="1">女</el-radio>
            <el-radio value="2">保密</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialog.visible" title="修改密码" width="420px">
      <el-form label-width="90px" label-position="left">
        <el-form-item label="旧密码">
          <el-input v-model="pwdDialog.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdDialog.newPassword" type="password" show-password placeholder="至少 5 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdDialog.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pwdDialog.saving" @click="savePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  gap: 30px;
  flex-wrap: wrap;
  padding: 26px 28px;
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.p-avatar {
  background: var(--grad-main);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
}

.p-name {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
}

.p-level {
  background: var(--grad-main);
  border: none;
}

.p-email {
  font-size: 13px;
  margin-top: 6px;
}

.profile-stats {
  display: flex;
  gap: 36px;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
}

.pstat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.pstat b {
  font-size: 22px;
}

.pstat span {
  font-size: 12px;
  color: var(--text-dim);
}

.profile-sign {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.sign-tip {
  font-size: 12px;
  max-width: 180px;
  text-align: center;
}

.exp-bar {
  margin-top: 18px;
  padding: 16px 22px;
}

.exp-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 10px;
}

.tabs-card {
  margin-top: 18px;
  padding: 10px 24px 24px;
}

.gain {
  color: #34d399;
  font-weight: 700;
}

.loss {
  color: #f87171;
  font-weight: 700;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-soft);
  border: 1px solid rgba(46, 46, 72, 0.6);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.msg-item.unread {
  border-color: rgba(139, 92, 246, 0.5);
}

.msg-item:hover {
  border-color: var(--accent);
}

.msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f472b6;
  flex-shrink: 0;
}

.msg-main {
  flex: 1;
  min-width: 0;
}

.msg-content {
  font-size: 14px;
  line-height: 1.6;
}

.msg-time {
  font-size: 12px;
  margin-top: 4px;
}

.fav-list,
.follow-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fav-item {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 12px;
  background: var(--bg-soft);
  border: 1px solid rgba(46, 46, 72, 0.6);
  border-radius: 10px;
  transition: border-color 0.2s;
}

.fav-item:hover {
  border-color: var(--accent);
}

.fav-cover {
  width: 64px;
  height: 84px;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(160deg, #23233a, #14141f);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 26px;
}

.fav-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fav-title {
  font-weight: 600;
}

.fav-time {
  font-size: 12px;
  margin-top: 6px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: var(--bg-soft);
  border: 1px solid rgba(46, 46, 72, 0.6);
  border-radius: 10px;
}

.f-avatar {
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}

.follow-name {
  flex: 1;
  font-weight: 600;
}

.follow-time {
  font-size: 12px;
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  padding-left: 16px;
  border-left: 1px solid rgba(46, 46, 72, 0.6);
}

.edit-avatar {
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}

.avatar-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
  text-align: center;
}

@media (max-width: 768px) {
  .profile-card {
    flex-direction: column;
    text-align: center;
  }
  .profile-left {
    flex-direction: column;
  }
  .profile-actions {
    border-left: none;
    padding-left: 0;
    flex-direction: row;
  }
}
</style>
