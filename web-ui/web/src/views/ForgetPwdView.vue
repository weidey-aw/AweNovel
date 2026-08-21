<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { forgetPassword, sendForgetCode } from '@gal/shared'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  email: '',
  code: '',
  password: '',
  confirm: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 个字符', trigger: 'blur' },
  ],
  confirm: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) callback(new Error('请再次输入新密码'))
        else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function sendCode() {
  if (!form.email) {
    ElMessage.warning('请先填写注册邮箱')
    return
  }
  sendingCode.value = true
  try {
    await sendForgetCode(form.email)
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
    await forgetPassword(form.email, form.code, form.password)
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '重置失败')
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
        <h2>重置密码</h2>
        <p class="text-dim">通过注册邮箱验证身份，设置新密码</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="submit">
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册时使用的邮箱" />
        </el-form-item>
        <el-form-item label="邮箱验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="验证码" />
            <el-button :loading="sendingCode" :disabled="countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
        <el-button class="grad-btn submit-btn" size="large" :loading="loading" @click="submit">
          重置密码
        </el-button>
      </el-form>

      <div class="auth-foot">
        想起来了？
        <router-link to="/login" class="link">返回登录</router-link>
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
  margin: 10px 0 6px;
  font-size: 22px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
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
  margin: 0 2px;
}
</style>
