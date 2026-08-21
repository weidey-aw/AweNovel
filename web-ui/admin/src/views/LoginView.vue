<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCaptcha, login } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

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
    auth.saveToken(token)
    await auth.fetchUserInfo()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
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
  <div class="login-page">
    <div class="login-card">
      <div class="login-head">
        <div class="login-logo">✦</div>
        <h2>AweNovel · 管理后台</h2>
        <p>请使用管理员账号登录</p>
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
            <img v-if="captchaImg" :src="captchaImg" class="captcha" alt="验证码" title="点击刷新" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button class="grad-btn submit-btn" size="large" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(700px 360px at 80% 0%, rgba(139, 92, 246, 0.16), transparent 60%),
    radial-gradient(600px 300px at 10% 100%, rgba(236, 72, 153, 0.12), transparent 60%),
    #f3f4f9;
  padding: 20px;
}

.login-card {
  width: 400px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 38px 36px 30px;
  box-shadow: 0 16px 48px rgba(30, 30, 60, 0.14);
  border: 1px solid #ececf5;
}

.login-head {
  text-align: center;
  margin-bottom: 26px;
}

.login-logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 16px;
  background: var(--grad-main);
  color: #fff;
  font-size: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 22px rgba(139, 92, 246, 0.4);
}

.login-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.login-head p {
  margin: 0;
  font-size: 13px;
  color: #8a8aa3;
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
  border: 1px solid #e5e5f0;
  object-fit: cover;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
}
</style>
