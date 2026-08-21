<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCaptcha, login } from '@gal/shared'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImg = ref('')
const uuid = ref('')

const form = reactive({
  username: '',
  password: '',
  code: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    uuid.value = res.uuid
    captchaImg.value = `data:image/gif;base64,${res.img}`
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '验证码加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login({ ...form, uuid: uuid.value })
    // 后端登录响应：{ code, msg, token }，token 位于顶层
    const token = (res as { token?: string }).token
    if (!token) throw new Error('登录响应缺少 token')
    userStore.saveToken(token)
    await userStore.fetchUserInfo()
    await userStore.fetchProfile()
    await userStore.fetchUnread()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '登录失败')
    loadCaptcha()
    form.code = ''
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="auth-page">
    <div class="auth-card panel-card">
      <div class="auth-head">
        <div class="auth-logo">✦ AweNovel</div>
        <h2>欢迎回来</h2>
        <p class="text-dim">登录后即可评分、评论、签到与召唤伊卡洛斯</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="验证码" class="code-input" />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              class="captcha"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-button class="grad-btn submit-btn" size="large" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="auth-foot">
        还没有账号？
        <router-link to="/register" class="link">立即注册</router-link>
        <span class="foot-divider">|</span>
        <router-link to="/forget" class="link">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 62px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.auth-card {
  width: 400px;
  max-width: 100%;
  padding: 36px 34px;
}

.auth-head {
  text-align: center;
  margin-bottom: 26px;
}

.auth-logo {
  font-size: 17px;
  font-weight: 800;
  background: var(--grad-main);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.auth-head h2 {
  margin: 14px 0 6px;
  font-size: 24px;
}

.auth-head p {
  font-size: 13px;
  margin: 0;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.captcha {
  width: 110px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid var(--border-glow);
  object-fit: cover;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
}

.auth-foot {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--text-dim);
}

.link {
  color: var(--accent);
  font-weight: 600;
}
</style>
