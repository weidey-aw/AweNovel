<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Delete,
  Download,
  Edit,
  Key,
  MoreFilled,
  Picture,
  Plus,
  Refresh,
  Search,
  Upload,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  changeUserStatus,
  createUser,
  deleteUsers,
  downloadUserTemplate,
  exportUsers,
  getRoleOptions,
  getUserAuthRole,
  getUserDetail,
  getUserList,
  importUsers,
  resetUserPwdAuto,
  resetUserPwdManual,
  setUserAuthRole,
  updateUser,
  uploadUserAvatar,
} from '@gal/shared'
import type { SysRole, SystemUser } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { saveBlob } from '@/utils/download'
import ImageCropper from '@/components/ImageCropper.vue'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

const SEX_OPTIONS = [
  { label: '男', value: '0' },
  { label: '女', value: '1' },
  { label: '未知', value: '2' },
]

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<SystemUser[]>([])
const total = ref(0)
const selection = ref<SystemUser[]>([])

const query = reactive({
  userName: '',
  phonenumber: '',
  status: '',
  dateRange: null as [string, string] | null,
  pageNum: 1,
})
const pageSize = 10

/** 组装查询参数（时间区间转为 params[beginTime] / params[endTime]） */
function buildParams(withPage = true): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (query.userName) params.userName = query.userName
  if (query.phonenumber) params.phonenumber = query.phonenumber
  if (query.status) params.status = query.status
  if (query.dateRange && query.dateRange.length === 2) {
    params['params[beginTime]'] = `${query.dateRange[0]} 00:00:00`
    params['params[endTime]'] = `${query.dateRange[1]} 23:59:59`
  }
  if (withPage) {
    params.pageNum = query.pageNum
    params.pageSize = pageSize
  }
  return params
}

async function load() {
  loading.value = true
  try {
    const res = await getUserList(buildParams())
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.userName = ''
  query.phonenumber = ''
  query.status = ''
  query.dateRange = null
  query.pageNum = 1
  load()
}

function onSelectionChange(rows: SystemUser[]) {
  selection.value = rows
}

/* ==================== 新增 / 修改 ==================== */
const formRef = ref<FormInstance>()
const formDialog = reactive({
  visible: false,
  isEdit: false,
  loading: false,
})
const form = reactive<Partial<SystemUser> & { password?: string }>({
  userId: undefined,
  userName: '',
  nickName: '',
  password: '',
  email: '',
  phonenumber: '',
  sex: '2',
  status: '0',
  remark: '',
  roleIds: [],
})
const roleOptions = ref<SysRole[]>([])

const formRules: FormRules = {
  userName: [
    { required: true, message: '请输入用户账号', trigger: 'blur' },
    { min: 2, max: 30, message: '长度需在 2 到 30 个字符之间', trigger: 'blur' },
  ],
  nickName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

async function loadRoleOptions() {
  try {
    const res = await getRoleOptions()
    roleOptions.value = res.data ?? []
  } catch {
    roleOptions.value = []
  }
}

function openAdd() {
  formDialog.isEdit = false
  formDialog.visible = true
  Object.assign(form, {
    userId: undefined,
    userName: '',
    nickName: '',
    password: '',
    email: '',
    phonenumber: '',
    sex: '2',
    status: '0',
    remark: '',
    roleIds: [],
  })
  formRef.value?.clearValidate()
  loadRoleOptions()
}

async function openEdit(row: SystemUser) {
  formDialog.isEdit = true
  formDialog.visible = true
  formRef.value?.clearValidate()
  loadRoleOptions()
  try {
    const res = await getUserDetail(row.userId)
    const user = res.data ?? row
    Object.assign(form, {
      userId: user.userId,
      userName: user.userName,
      nickName: user.nickName,
      password: '',
      email: user.email ?? '',
      phonenumber: user.phonenumber ?? '',
      sex: user.sex ?? '2',
      status: user.status ?? '0',
      remark: user.remark ?? '',
      roleIds: res.roleIds ?? [],
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '用户详情加载失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!formDialog.isEdit) {
    if (!form.password) {
      ElMessage.warning('请输入初始密码')
      return
    }
    if (form.password.length < 6 || form.password.length > 20) {
      ElMessage.warning('密码长度需在 6 到 20 个字符之间')
      return
    }
  }
  formDialog.loading = true
  try {
    if (formDialog.isEdit) {
      const { password: _password, ...payload } = form
      await updateUser(payload)
      ElMessage.success('修改成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('新增成功')
    }
    formDialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    formDialog.loading = false
  }
}

/* ==================== 删除 ==================== */
async function handleDelete(row?: SystemUser) {
  const targets = row ? [row] : selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的用户')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 个用户吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteUsers(targets.map((u) => u.userId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

/* ==================== 状态切换 ==================== */
async function onStatusChange(row: SystemUser, value: string | number | boolean) {
  const nextStatus = value ? '0' : '1'
  try {
    await changeUserStatus(row.userId, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === '0' ? '已启用' : '已停用')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '状态修改失败')
    load()
  }
}

/* ==================== 重置密码 ==================== */
const pwdDialog = reactive({
  visible: false,
  mode: 'auto' as 'auto' | 'manual',
  userId: 0,
  userName: '',
  password: '',
  confirm: '',
  loading: false,
})

const canManualReset = computed(() => can('system:user:resetPwdManual'))

function openResetPwd(row: SystemUser) {
  Object.assign(pwdDialog, {
    visible: true,
    mode: canManualReset.value ? 'manual' : 'auto',
    userId: row.userId,
    userName: row.userName ?? '',
    password: '',
    confirm: '',
    loading: false,
  })
}

async function submitResetPwd() {
  if (pwdDialog.mode === 'manual') {
    if (!pwdDialog.password) {
      ElMessage.warning('请输入新密码')
      return
    }
    if (pwdDialog.password.length < 8 || pwdDialog.password.length > 20) {
      ElMessage.warning('密码长度需在 8 到 20 个字符之间')
      return
    }
    if (pwdDialog.password !== pwdDialog.confirm) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }
  }
  pwdDialog.loading = true
  try {
    if (pwdDialog.mode === 'auto') {
      await resetUserPwdAuto(pwdDialog.userId)
      ElMessage.success('已重置为系统随机密码，新密码已发送至用户邮箱')
    } else {
      await resetUserPwdManual(pwdDialog.userId, pwdDialog.password)
      ElMessage.success('密码重置成功，新密码已发送至用户邮箱')
    }
    pwdDialog.visible = false
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '重置密码失败')
  } finally {
    pwdDialog.loading = false
  }
}

/* ==================== 分配角色 ==================== */
const roleDialog = reactive({
  visible: false,
  userId: 0,
  userName: '',
  roles: [] as SysRole[],
  checked: [] as number[],
  loading: false,
})

async function openAuthRole(row: SystemUser) {
  Object.assign(roleDialog, {
    visible: true,
    userId: row.userId,
    userName: row.userName ?? '',
    roles: [],
    checked: [],
    loading: true,
  })
  try {
    const res = await getUserAuthRole(row.userId)
    roleDialog.roles = res.roles ?? []
    roleDialog.checked = roleDialog.roles.filter((r) => r.flag).map((r) => r.roleId)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '角色加载失败')
  } finally {
    roleDialog.loading = false
  }
}

async function submitAuthRole() {
  roleDialog.loading = true
  try {
    await setUserAuthRole(roleDialog.userId, roleDialog.checked)
    ElMessage.success('角色分配成功')
    roleDialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '角色分配失败')
  } finally {
    roleDialog.loading = false
  }
}

/* ==================== 更多操作 ==================== */

/** 「更多」下拉菜单命令 */
function onMoreCommand(command: string, row: SystemUser) {
  if (command === 'role') openAuthRole(row)
  else if (command === 'pwd') openResetPwd(row)
}

/* ==================== 修改头像（点击头像触发，带裁剪） ==================== */
const avatarDialog = reactive({
  visible: false,
  userId: 0,
  userName: '',
  avatar: '',
  /** 待裁剪图片的 blob 地址 */
  cropSrc: '',
  uploading: false,
})
const avatarInputRef = ref<HTMLInputElement>()
const cropperRef = ref<{ toFile: (name: string) => Promise<File | null> } | null>(null)

function openAvatar(row: SystemUser) {
  revokeCropSrc()
  Object.assign(avatarDialog, {
    visible: true,
    userId: row.userId,
    userName: row.userName ?? '',
    avatar: row.avatar ?? '',
    cropSrc: '',
    uploading: false,
  })
}

/** 释放 blob 地址，避免内存泄漏 */
function revokeCropSrc() {
  if (avatarDialog.cropSrc) {
    URL.revokeObjectURL(avatarDialog.cropSrc)
    avatarDialog.cropSrc = ''
  }
}

function pickAvatarFile() {
  avatarInputRef.value?.click()
}

function onAvatarFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\/(jpeg|png|gif|bmp|webp)$/.test(file.type)) {
    ElMessage.warning('仅支持 jpg / png / gif / bmp / webp 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  revokeCropSrc()
  avatarDialog.cropSrc = URL.createObjectURL(file)
}

/** 裁剪并上传 */
async function submitAvatar() {
  const cropper = cropperRef.value
  if (!cropper) {
    ElMessage.warning('请先选择图片')
    return
  }
  avatarDialog.uploading = true
  try {
    const file = await cropper.toFile('avatar')
    if (!file) {
      ElMessage.error('图片裁剪失败，请重新选择图片')
      return
    }
    const res = await uploadUserAvatar(avatarDialog.userId, file)
    if (res.imgUrl) avatarDialog.avatar = res.imgUrl
    ElMessage.success('头像修改成功')
    avatarDialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '头像上传失败')
  } finally {
    avatarDialog.uploading = false
  }
}

/** 弹窗关闭后清理裁剪资源 */
function onAvatarDialogClosed() {
  revokeCropSrc()
  avatarDialog.uploading = false
}

/* ==================== 导入 / 导出 / 模板 ==================== */
const importDialog = reactive({ visible: false, updateSupport: false, importing: false })
const importInputRef = ref<HTMLInputElement>()

function openImport() {
  importDialog.updateSupport = false
  importDialog.visible = true
}

function pickImportFile() {
  importInputRef.value?.click()
}

async function onImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  importDialog.importing = true
  try {
    const res = await importUsers(file, importDialog.updateSupport)
    ElMessage.success(res.msg || '导入完成')
    importDialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导入失败')
  } finally {
    importDialog.importing = false
  }
}

async function handleExport() {
  try {
    const blob = await exportUsers(buildParams(false))
    saveBlob(blob, `用户数据_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
}

async function handleTemplate() {
  try {
    const blob = await downloadUserTemplate()
    saveBlob(blob, `用户导入模板_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '模板下载失败')
  }
}

/** 超管账号（userId=1）不允许被修改/删除/停用 */
const isSuperAdmin = (row: SystemUser) => row.userId === 1

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.userName"
        placeholder="用户账号"
        clearable
        style="width: 170px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="query.phonenumber"
        placeholder="手机号"
        clearable
        style="width: 160px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="search">
        <el-option label="正常" value="0" />
        <el-option label="停用" value="1" />
      </el-select>
      <el-date-picker
        v-model="query.dateRange"
        type="daterange"
        unlink-panels
        range-separator="至"
        start-placeholder="创建开始"
        end-placeholder="创建结束"
        value-format="YYYY-MM-DD"
        style="width: 250px"
        @change="search"
      />
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('system:user:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button v-if="can('system:user:edit')" :disabled="selection.length !== 1" @click="openEdit(selection[0])">
        修改
      </el-button>
      <el-button
        v-if="can('system:user:remove')"
        type="danger"
        plain
        :disabled="!selection.length"
        @click="handleDelete()"
      >
        删除
      </el-button>
      <el-dropdown v-if="can('system:user:import') || can('system:user:export')">
        <el-button :icon="Upload">
          导入/导出
          <el-icon class="el-icon--right"><Download /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="can('system:user:import')" @click="openImport">导入用户</el-dropdown-item>
            <el-dropdown-item v-if="can('system:user:import')" @click="handleTemplate">下载导入模板</el-dropdown-item>
            <el-dropdown-item v-if="can('system:user:export')" @click="handleExport">导出用户</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" :selectable="(row: SystemUser) => !isSuperAdmin(row)" />
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-tooltip
              :content="can('system:user:edit') ? '点击修改头像' : '无修改头像权限'"
              placement="top"
            >
              <el-avatar
                :size="34"
                :src="row.avatar || undefined"
                class="u-avatar"
                :class="{ 'u-avatar-clickable': can('system:user:edit') }"
                @click="can('system:user:edit') && openAvatar(row)"
              >
                {{ (row.nickName || row.userName || 'U').slice(0, 1) }}
              </el-avatar>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="用户账号" min-width="130" />
        <el-table-column prop="nickName" label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phonenumber" label="手机号" width="125">
          <template #default="{ row }">{{ row.phonenumber || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '0'"
              :disabled="isSuperAdmin(row) || !can('system:user:edit')"
              @change="(v: string | number | boolean) => onStatusChange(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="215" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="can('system:user:edit')"
              link
              type="primary"
              :icon="Edit"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-if="can('system:user:remove')"
              link
              type="danger"
              :icon="Delete"
              :disabled="isSuperAdmin(row)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-dropdown
              v-if="can('system:user:edit') || can('system:user:resetPwd')"
              trigger="click"
              class="more-dropdown"
              @command="(cmd: string) => onMoreCommand(cmd, row)"
            >
              <el-button link type="info" :icon="MoreFilled">
                更多
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="can('system:user:edit')" command="role">
                    <el-icon class="act-role"><UserFilled /></el-icon>
                    <span>分配角色</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="can('system:user:resetPwd')"
                    command="pwd"
                    :disabled="isSuperAdmin(row)"
                  >
                    <el-icon class="act-pwd"><Key /></el-icon>
                    <span>重置密码</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="query.pageNum"
          @current-change="(p: number) => { query.pageNum = p; load() }"
        />
      </div>
    </div>

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="formDialog.visible" :title="formDialog.isEdit ? '修改用户' : '新增用户'" width="620px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="用户账号" prop="userName">
              <el-input v-model="form.userName" :disabled="formDialog.isEdit" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="昵称" />
            </el-form-item>
          </el-col>
          <el-col v-if="!formDialog.isEdit" :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="用于接收通知邮件" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.sex" style="width: 100%">
                <el-option v-for="s in SEX_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.loading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="520px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="重置成功后系统会自动向用户邮箱发送包含新密码的通知邮件"
        style="margin-bottom: 14px"
      />
      <el-form label-width="90px">
        <el-form-item label="用户账号">
          <el-input :model-value="pwdDialog.userName" disabled />
        </el-form-item>
        <el-form-item label="重置方式">
          <el-radio-group v-model="pwdDialog.mode">
            <el-radio value="auto">系统自动重置</el-radio>
            <el-radio value="manual" :disabled="!canManualReset">手动重置</el-radio>
          </el-radio-group>
          <div class="hint">
            <template v-if="pwdDialog.mode === 'auto'">
              系统随机生成 <b>8 位数字 + 字母</b> 密码，并通过邮件发送给用户。
            </template>
            <template v-else>
              由管理员指定新密码（<b>需要「手动重置密码」权限</b>），并通过邮件发送给用户。
            </template>
          </div>
        </el-form-item>
        <template v-if="pwdDialog.mode === 'manual'">
          <el-form-item label="新密码" required>
            <el-input v-model="pwdDialog.password" type="password" show-password placeholder="8-20 位" />
          </el-form-item>
          <el-form-item label="确认密码" required>
            <el-input v-model="pwdDialog.confirm" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pwdDialog.loading" @click="submitResetPwd">确定重置</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleDialog.visible" title="分配角色" width="520px">
      <el-form v-loading="roleDialog.loading" label-width="90px">
        <el-form-item label="用户账号">
          <el-input :model-value="roleDialog.userName" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="roleDialog.checked" class="role-group">
            <el-checkbox v-for="r in roleDialog.roles" :key="r.roleId" :value="r.roleId">
              {{ r.roleName }}<span class="role-key">（{{ r.roleKey }}）</span>
            </el-checkbox>
          </el-checkbox-group>
          <div v-if="!roleDialog.roles.length" class="hint">暂无可分配角色</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="roleDialog.loading" @click="submitAuthRole">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改头像弹窗（点击头像列触发，支持裁剪） -->
    <el-dialog
      v-model="avatarDialog.visible"
      title="修改头像"
      width="420px"
      append-to-body
      @closed="onAvatarDialogClosed"
    >
      <div class="avatar-box" v-loading="avatarDialog.uploading">
        <template v-if="!avatarDialog.cropSrc">
          <el-avatar :size="110" :src="avatarDialog.avatar || undefined" class="u-avatar big">
            {{ (avatarDialog.userName || 'U').slice(0, 1) }}
          </el-avatar>
          <p class="hint">用户：{{ avatarDialog.userName }}</p>
          <el-button type="primary" :icon="Picture" @click="pickAvatarFile">选择图片</el-button>
          <p class="hint">支持 jpg / png / gif / bmp / webp，大小不超过 5MB</p>
        </template>
        <template v-else>
          <ImageCropper ref="cropperRef" :src="avatarDialog.cropSrc" :size="300" />
        </template>
      </div>
      <input ref="avatarInputRef" type="file" accept="image/*" hidden @change="onAvatarFileChange" />
      <template #footer>
        <el-button @click="avatarDialog.visible = false">取消</el-button>
        <el-button v-if="avatarDialog.cropSrc" @click="pickAvatarFile">重新选择</el-button>
        <el-button
          v-if="avatarDialog.cropSrc"
          type="primary"
          :loading="avatarDialog.uploading"
          @click="submitAvatar"
        >
          保存头像
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importDialog.visible" title="导入用户" width="480px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请先下载导入模板，按模板格式填写后再上传"
        style="margin-bottom: 14px"
      />
      <el-checkbox v-model="importDialog.updateSupport">已存在的用户进行更新</el-checkbox>
      <input ref="importInputRef" type="file" accept=".xlsx,.xls" hidden @change="onImportFileChange" />
      <div class="import-actions">
        <el-button type="primary" :loading="importDialog.importing" @click="pickImportFile">
          选择 Excel 并导入
        </el-button>
        <el-button @click="handleTemplate">下载模板</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.u-avatar {
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}

.u-avatar.big {
  font-size: 34px;
}

/* 头像可点击修改 */
.u-avatar-clickable {
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
  outline: 2px dashed transparent;
}

.u-avatar-clickable:hover {
  transform: scale(1.08);
  box-shadow: 0 0 0 2px #cdc4f7;
  outline-color: #cdc4f7;
}

/* 「更多」下拉：图标颜色区分 */
.more-dropdown {
  margin-left: 8px;
  vertical-align: middle;
}

.act-role {
  color: #409eff;
  margin-right: 6px;
}

.act-pwd {
  color: #e6a23c;
  margin-right: 6px;
}

.hint {
  color: #8a8aa3;
  font-size: 12.5px;
  line-height: 1.7;
  margin: 4px 0 0;
}

.role-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-key {
  color: #9a9ab0;
  font-size: 12px;
}

.avatar-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 6px 0 10px;
}

.import-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
</style>
