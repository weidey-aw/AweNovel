<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Download, Edit, MoreFilled, Plus, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import {
  cancelRoleAuthUser,
  cancelRoleAuthUsers,
  changeRoleStatus,
  createRole,
  deleteRoles,
  exportRoles,
  getMenuTreeselect,
  getRoleAllocatedUsers,
  getRoleDetail,
  getRoleList,
  getRoleMenuTreeselect,
  getRoleUnallocatedUsers,
  selectRoleAuthUsers,
  updateRole,
} from '@gal/shared'
import type { MenuTreeNode, SysRole, SystemUser } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { saveBlob } from '@/utils/download'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/** 超级管理员角色（roleId=1）不可修改/删除/停用 */
const isSuperAdmin = (row: SysRole) => row.roleId === 1

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<SysRole[]>([])
const total = ref(0)
const selection = ref<SysRole[]>([])

const query = reactive({
  roleName: '',
  roleKey: '',
  status: '',
  dateRange: null as [string, string] | null,
  pageNum: 1,
})
const pageSize = 10

/** 组装查询参数（时间区间转 params[beginTime] / params[endTime]） */
function buildParams(withPage = true): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (query.roleName) params.roleName = query.roleName
  if (query.roleKey) params.roleKey = query.roleKey
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
    const res = await getRoleList(buildParams())
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '角色列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.roleName = ''
  query.roleKey = ''
  query.status = ''
  query.dateRange = null
  query.pageNum = 1
  load()
}

function onSelectionChange(rows: SysRole[]) {
  selection.value = rows
}

/* ==================== 状态切换 ==================== */
async function onStatusChange(row: SysRole, value: string | number | boolean) {
  const nextStatus = value ? '0' : '1'
  try {
    await changeRoleStatus(row.roleId, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === '0' ? '已启用' : '已停用')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '状态修改失败')
    load()
  }
}

/* ==================== 删除 ==================== */
async function handleDelete(row?: SysRole) {
  const targets = row ? [row] : selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的角色')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 个角色吗？已分配用户的角色无法删除。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteRoles(targets.map((r) => r.roleId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

/* ==================== 导出 ==================== */
async function handleExport() {
  try {
    const blob = await exportRoles(buildParams(false))
    saveBlob(blob, `角色数据_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
}

/* ==================== 新增 / 修改（含菜单权限） ==================== */
interface TreeExposed {
  getCheckedKeys: (leafOnly?: boolean) => number[]
  getHalfCheckedKeys: () => number[]
  setCheckedKeys: (keys: number[]) => void
}

const formRef = ref<FormInstance>()
const treeRef = ref<TreeExposed | null>(null)
const menuTree = ref<MenuTreeNode[]>([])
const treeProps = { label: 'label', children: 'children' }

const formDialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<SysRole>>({
  roleId: undefined,
  roleName: '',
  roleKey: '',
  roleSort: 1,
  status: '0',
  menuCheckStrictly: true,
  remark: '',
  menuIds: [],
})

const formRules: FormRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 30, message: '长度不能超过 30 个字符', trigger: 'blur' },
  ],
  roleKey: [
    { required: true, message: '请输入权限字符', trigger: 'blur' },
    { max: 100, message: '长度不能超过 100 个字符', trigger: 'blur' },
  ],
  roleSort: [{ required: true, message: '请输入显示顺序', trigger: 'blur' }],
}

/** 父子联动开关：strictly = !menuCheckStrictly */
const treeCheckStrictly = computed(() => !form.menuCheckStrictly)

async function loadMenuTree(roleId?: number): Promise<number[]> {
  try {
    if (roleId) {
      // 注意：checkedKeys / menus 是顶层字段（不在 data 内）
      const res = await getRoleMenuTreeselect(roleId)
      menuTree.value = res.menus ?? []
      return res.checkedKeys ?? []
    }
    const res = await getMenuTreeselect()
    menuTree.value = res.data ?? []
    return []
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '菜单树加载失败')
    menuTree.value = []
    return []
  }
}

async function openAdd() {
  formDialog.isEdit = false
  formDialog.visible = true
  Object.assign(form, {
    roleId: undefined,
    roleName: '',
    roleKey: '',
    roleSort: 1,
    status: '0',
    menuCheckStrictly: true,
    remark: '',
    menuIds: [],
  })
  formRef.value?.clearValidate()
  const checked = await loadMenuTree()
  await nextTick()
  treeRef.value?.setCheckedKeys(checked)
}

async function openEdit(row: SysRole) {
  formDialog.isEdit = true
  formDialog.visible = true
  formRef.value?.clearValidate()
  try {
    const detail = await getRoleDetail(row.roleId)
    const role = detail.data ?? row
    Object.assign(form, {
      roleId: role.roleId,
      roleName: role.roleName,
      roleKey: role.roleKey,
      roleSort: role.roleSort ?? 1,
      status: role.status ?? '0',
      menuCheckStrictly: role.menuCheckStrictly ?? true,
      remark: role.remark ?? '',
      menuIds: [],
    })
    const checked = await loadMenuTree(row.roleId)
    await nextTick()
    treeRef.value?.setCheckedKeys(checked)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '角色详情加载失败')
  }
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 勾选节点 + 半选父节点（后端按 menuIds 全量覆盖）
  const checked = treeRef.value?.getCheckedKeys() ?? []
  const half = treeRef.value?.getHalfCheckedKeys() ?? []
  const menuIds = Array.from(new Set([...checked, ...half])).map(Number)

  formDialog.loading = true
  try {
    const payload: Partial<SysRole> = {
      roleId: form.roleId,
      roleName: form.roleName,
      roleKey: form.roleKey,
      roleSort: form.roleSort,
      status: form.status,
      menuCheckStrictly: form.menuCheckStrictly,
      remark: form.remark,
      menuIds,
    }
    if (formDialog.isEdit) {
      await updateRole(payload)
      ElMessage.success('修改成功')
    } else {
      await createRole(payload)
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

/* ==================== 分配用户 ==================== */
const userDialog = reactive({
  visible: false,
  roleId: 0,
  roleName: '',
  activeTab: 'allocated' as 'allocated' | 'unallocated',
  loading: false,
})

/* 已分配用户 */
const allocLoading = ref(false)
const allocList = ref<SystemUser[]>([])
const allocTotal = ref(0)
const allocSelection = ref<SystemUser[]>([])
const allocQuery = reactive({ userName: '', phonenumber: '', pageNum: 1 })

/* 未分配用户 */
const unallocLoading = ref(false)
const unallocList = ref<SystemUser[]>([])
const unallocTotal = ref(0)
const unallocSelection = ref<SystemUser[]>([])
const unallocQuery = reactive({ userName: '', phonenumber: '', pageNum: 1 })

const userPageSize = 10

async function loadAllocated() {
  if (!userDialog.roleId) return
  allocLoading.value = true
  try {
    const res = await getRoleAllocatedUsers({
      roleId: userDialog.roleId,
      pageNum: allocQuery.pageNum,
      pageSize: userPageSize,
      userName: allocQuery.userName || undefined,
      phonenumber: allocQuery.phonenumber || undefined,
    })
    allocList.value = res.rows ?? []
    allocTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '已分配用户加载失败')
  } finally {
    allocLoading.value = false
  }
}

async function loadUnallocated() {
  if (!userDialog.roleId) return
  unallocLoading.value = true
  try {
    const res = await getRoleUnallocatedUsers({
      roleId: userDialog.roleId,
      pageNum: unallocQuery.pageNum,
      pageSize: userPageSize,
      userName: unallocQuery.userName || undefined,
      phonenumber: unallocQuery.phonenumber || undefined,
    })
    unallocList.value = res.rows ?? []
    unallocTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '未分配用户加载失败')
  } finally {
    unallocLoading.value = false
  }
}

/** 两个列表都刷新（授权关系变化后互相影响） */
function reloadBoth() {
  loadAllocated()
  loadUnallocated()
}

async function openAuthUser(row: SysRole) {
  Object.assign(userDialog, {
    visible: true,
    roleId: row.roleId,
    roleName: row.roleName,
    activeTab: 'allocated',
    loading: true,
  })
  allocQuery.userName = ''
  allocQuery.phonenumber = ''
  allocQuery.pageNum = 1
  unallocQuery.userName = ''
  unallocQuery.phonenumber = ''
  unallocQuery.pageNum = 1
  await reloadBoth()
  userDialog.loading = false
}

async function onTabChange(name: string | number) {
  userDialog.activeTab = name as 'allocated' | 'unallocated'
  if (name === 'allocated') await loadAllocated()
  else await loadUnallocated()
}

/** 取消单个用户授权 */
async function cancelOne(row: SystemUser) {
  try {
    await ElMessageBox.confirm(`确认取消用户「${row.userName}」的该角色授权吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await cancelRoleAuthUser(userDialog.roleId, row.userId)
    ElMessage.success('取消授权成功')
    reloadBoth()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '取消授权失败')
  }
}

/** 批量取消授权 */
async function cancelBatch() {
  if (!allocSelection.value.length) {
    ElMessage.warning('请先选择要取消授权的用户')
    return
  }
  try {
    await ElMessageBox.confirm(`确认取消 ${allocSelection.value.length} 个用户的该角色授权吗？`, '提示', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await cancelRoleAuthUsers(
      userDialog.roleId,
      allocSelection.value.map((u) => u.userId),
    )
    ElMessage.success('批量取消授权成功')
    reloadBoth()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '批量取消授权失败')
  }
}

/** 批量授权 */
async function grantBatch() {
  if (!unallocSelection.value.length) {
    ElMessage.warning('请先选择要授权的用户')
    return
  }
  try {
    await selectRoleAuthUsers(
      userDialog.roleId,
      unallocSelection.value.map((u) => u.userId),
    )
    ElMessage.success('授权成功')
    reloadBoth()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '授权失败')
  }
}

/** 「更多」下拉命令 */
function onMoreCommand(command: string, row: SysRole) {
  if (command === 'authUser') openAuthUser(row)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.roleName"
        placeholder="角色名称"
        clearable
        style="width: 170px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="query.roleKey"
        placeholder="权限字符"
        clearable
        style="width: 170px"
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
      <el-button v-if="can('system:role:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button v-if="can('system:role:edit')" :disabled="selection.length !== 1" @click="openEdit(selection[0])">
        修改
      </el-button>
      <el-button
        v-if="can('system:role:remove')"
        type="danger"
        plain
        :disabled="!selection.length"
        @click="handleDelete()"
      >
        删除
      </el-button>
      <el-button v-if="can('system:role:export')" :icon="Download" @click="handleExport">导出</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="46" :selectable="(row: SysRole) => !isSuperAdmin(row)" />
        <el-table-column prop="roleId" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleKey" label="权限字符" min-width="140" />
        <el-table-column prop="roleSort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '0'"
              :disabled="isSuperAdmin(row) || !can('system:role:edit')"
              @change="(v: string | number | boolean) => onStatusChange(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="215" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('system:role:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button
              v-if="can('system:role:remove')"
              link
              type="danger"
              :icon="Delete"
              :disabled="isSuperAdmin(row)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-dropdown
              v-if="can('system:role:edit')"
              trigger="click"
              class="more-dropdown"
              @command="(cmd: string) => onMoreCommand(cmd, row)"
            >
              <el-button link type="info" :icon="MoreFilled">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="authUser">
                    <el-icon class="act-user"><UserFilled /></el-icon>
                    <span>分配用户</span>
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
    <el-dialog v-model="formDialog.visible" :title="formDialog.isEdit ? '修改角色' : '新增角色'" width="640px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="如：内容编辑" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限字符" prop="roleKey">
              <el-input v-model="form.roleKey" placeholder="如：editor" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="roleSort">
              <el-input-number v-model="form.roleSort" :min="0" :max="9999" style="width: 100%" />
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
            <el-form-item label="菜单权限">
              <div class="perm-head">
                <el-checkbox
                  :model-value="form.menuCheckStrictly"
                  @change="(v: string | number | boolean) => (form.menuCheckStrictly = Boolean(v))"
                >
                  父子联动
                </el-checkbox>
                <span class="hint">开启后勾选父级不会自动勾选全部子级</span>
              </div>
              <div class="menu-tree-box">
                <el-tree
                  ref="treeRef"
                  :data="menuTree"
                  show-checkbox
                  node-key="id"
                  :props="treeProps"
                  :check-strictly="treeCheckStrictly"
                  default-expand-all
                  :empty-text="'暂无菜单数据'"
                />
              </div>
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

    <!-- 分配用户弹窗 -->
    <el-dialog v-model="userDialog.visible" :title="`分配用户 · ${userDialog.roleName}`" width="860px">
      <el-tabs :model-value="userDialog.activeTab" @tab-change="onTabChange">
        <!-- 已分配用户 -->
        <el-tab-pane label="已分配用户" name="allocated">
          <div class="toolbar">
            <el-input
              v-model="allocQuery.userName"
              placeholder="用户账号"
              clearable
              style="width: 160px"
              @keyup.enter="loadAllocated"
              @clear="loadAllocated"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-input
              v-model="allocQuery.phonenumber"
              placeholder="手机号"
              clearable
              style="width: 150px"
              @keyup.enter="loadAllocated"
              @clear="loadAllocated"
            />
            <el-button class="grad-btn" @click="loadAllocated">查询</el-button>
            <el-button
              type="danger"
              plain
              :disabled="!allocSelection.length || !can('system:role:edit')"
              @click="cancelBatch"
            >
              批量取消授权
            </el-button>
          </div>
          <el-table
            v-loading="allocLoading"
            :data="allocList"
            border
            max-height="360"
            @selection-change="(rows: SystemUser[]) => (allocSelection = rows)"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="userId" label="ID" width="70" />
            <el-table-column prop="userName" label="用户账号" min-width="130" />
            <el-table-column prop="nickName" label="昵称" min-width="120">
              <template #default="{ row }">{{ row.nickName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="phonenumber" label="手机号" width="130">
              <template #default="{ row }">{{ row.phonenumber || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="can('system:role:edit')"
                  link
                  type="danger"
                  @click="cancelOne(row)"
                >
                  取消授权
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="allocTotal"
              :page-size="userPageSize"
              :current-page="allocQuery.pageNum"
              @current-change="(p: number) => { allocQuery.pageNum = p; loadAllocated() }"
            />
          </div>
        </el-tab-pane>

        <!-- 未分配用户 -->
        <el-tab-pane label="未分配用户" name="unallocated">
          <div class="toolbar">
            <el-input
              v-model="unallocQuery.userName"
              placeholder="用户账号"
              clearable
              style="width: 160px"
              @keyup.enter="loadUnallocated"
              @clear="loadUnallocated"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-input
              v-model="unallocQuery.phonenumber"
              placeholder="手机号"
              clearable
              style="width: 150px"
              @keyup.enter="loadUnallocated"
              @clear="loadUnallocated"
            />
            <el-button class="grad-btn" @click="loadUnallocated">查询</el-button>
            <el-button
              type="primary"
              :disabled="!unallocSelection.length || !can('system:role:edit')"
              @click="grantBatch"
            >
              批量授权
            </el-button>
          </div>
          <el-table
            v-loading="unallocLoading"
            :data="unallocList"
            border
            max-height="360"
            @selection-change="(rows: SystemUser[]) => (unallocSelection = rows)"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="userId" label="ID" width="70" />
            <el-table-column prop="userName" label="用户账号" min-width="130" />
            <el-table-column prop="nickName" label="昵称" min-width="120">
              <template #default="{ row }">{{ row.nickName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="phonenumber" label="手机号" width="130">
              <template #default="{ row }">{{ row.phonenumber || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="unallocTotal"
              :page-size="userPageSize"
              :current-page="unallocQuery.pageNum"
              @current-change="(p: number) => { unallocQuery.pageNum = p; loadUnallocated() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="userDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.more-dropdown {
  margin-left: 8px;
  vertical-align: middle;
}

.act-user {
  color: #409eff;
  margin-right: 6px;
}

.perm-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.hint {
  color: #8a8aa3;
  font-size: 12.5px;
}

.menu-tree-box {
  width: 100%;
  max-height: 260px;
  overflow: auto;
  border: 1px solid #e4e4f0;
  border-radius: 8px;
  padding: 8px;
  background: #fbfbfe;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
