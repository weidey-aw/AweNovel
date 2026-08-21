<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register, sendRegisterCode } from '@gal/shared'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  nickname: '',
  email: '',
  code: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度 2-20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 个字符', trigger: 'blur' },
  ],
  confirm: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) callback(new Error('请再次输入密码'))
        else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

async function sendCode() {
  if (!form.email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  sendingCode.value = true
  try {
    await sendRegisterCode(form.email)
    ElMessage.success('验证码已发送，请查收邮箱')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email,
      code: form.code,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card panel-card">
      <div class="auth-head">
        <div class="auth-logo">✦ AweNovel</div>
        <h2>创建账号</h2>
        <p class="text-dim">加入社区，开始你的 Galgame 之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="2-20 个字符" autocomplete="username" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="社区中展示的名字" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用于接收验证码" autocomplete="email" />
        </el-form-item>
        <el-form-item label="邮箱验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="6 位验证码" class="code-input" />
            <el-button :disabled="countdown > 0" :loading="sendingCode" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20 个字符" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="再次输入密码" show-password autocomplete="new-password" />
        </el-form-item>
        <el-button class="grad-btn submit-btn" size="large" :loading="loading" @click="submit">
          注 册
        </el-button>
      </el-form>

      <div class="auth-foot">
        已有账号？
        <router-link to="/login" class="link">去登录</router-link>
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
  width: 420px;
  max-width: 100%;
  padding: 34px 34px 28px;
}

.auth-head {
  text-align: center;
  margin-bottom: 24px;
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
  gap: 10px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
}

.auth-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--text-dim);
}

.link {
  color: var(--accent);
  font-weight: 600;
}
</style>
