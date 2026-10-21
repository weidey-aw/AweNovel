<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createMenu, deleteMenu, getMenuDetail, getMenuList, updateMenu } from '@gal/shared'
import type { SysMenu } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { iconOf } from '@/utils/icon'
import { buildTree, countTree } from '@/utils/tree'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/* ==================== 列表（树形） ==================== */
const loading = ref(false)
const list = ref<SysMenu[]>([])
const total = ref(0)
const expandAll = ref(true)
const tableKey = ref(0)

const query = reactive({
  menuName: '',
  status: '',
})

const typeMap: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
const typeTag: Record<string, 'primary' | 'success' | 'info'> = {
  M: 'primary',
  C: 'success',
  F: 'info',
}

async function load() {
  loading.value = true
  try {
    const res = await getMenuList({
      menuName: query.menuName || undefined,
      status: query.status || undefined,
    })
    // 后端返回扁平列表，这里构建成树供 el-table 树形展示
    list.value = buildTree(res.data ?? [])
    total.value = countTree(list.value)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '菜单列表加载失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.menuName = ''
  query.status = ''
  load()
}

function toggleExpand() {
  expandAll.value = !expandAll.value
  tableKey.value += 1
}

/** 是否外链（isFrame = '0' 表示是外链） */
const isExternal = (row: SysMenu) => row.isFrame === '0'

async function copyPerms(text?: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制权限标识')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

/* ==================== 新增 / 修改 ==================== */
const formRef = ref<FormInstance>()
const formDialog = reactive({ visible: false, isEdit: false, loading: false })

const form = reactive<Partial<SysMenu>>({
  menuId: undefined,
  parentId: 0,
  menuName: '',
  menuType: 'M',
  orderNum: 1,
  path: '',
  component: '',
  query: '',
  isFrame: '1',
  isCache: '0',
  visible: '0',
  status: '0',
  perms: '',
  icon: '',
  remark: '',
})

const formRules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入显示顺序', trigger: 'blur' }],
}

/** 父级下拉：菜单树 + 「主目录」根节点 */
const parentOptions = computed(() => {
  const mapNode = (nodes: SysMenu[]): Array<{ value: number; label: string; children?: unknown[] }> =>
    nodes
      .filter((n) => n.menuType !== 'F')
      .map((n) => ({
        value: n.menuId,
        label: n.menuName,
        children: n.children?.length ? mapNode(n.children) : undefined,
      }))
  return [{ value: 0, label: '主目录', children: mapNode(list.value) }]
})

/** 常用图标（可自由输入其它 Element Plus 图标名） */
const ICON_PRESETS = [
  'system', 'monitor', 'operation', 'dashboard', 'user', 'user-filled', 'menu',
  'notebook', 'edit', 'message', 'tickets', 'connection', 'timer', 'trend-charts',
  'cpu', 'coin', 'video-play', 'office-building', 'price-tag', 'folder-opened',
  'document', 'chat-dot-round', 'finished', 'setting', 'star',
]

function resetForm() {
  Object.assign(form, {
    menuId: undefined,
    parentId: 0,
    menuName: '',
    menuType: 'M',
    orderNum: 1,
    path: '',
    component: '',
    query: '',
    isFrame: '1',
    isCache: '0',
    visible: '0',
    status: '0',
    perms: '',
    icon: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

/** 新增（可指定父级） */
function openAdd(parent?: SysMenu) {
  formDialog.isEdit = false
  formDialog.visible = true
  resetForm()
  if (parent) {
    form.parentId = parent.menuId
    // 父级是按钮时不允许挂子级，统一回退到根
    if (parent.menuType === 'F') form.parentId = 0
    // 目录下新增默认为菜单，菜单下新增默认为按钮
    form.menuType = parent.menuType === 'M' ? 'C' : 'F'
  }
  nextTick(() => formRef.value?.clearValidate())
}

async function openEdit(row: SysMenu) {
  formDialog.isEdit = true
  formDialog.visible = true
  resetForm()
  try {
    const res = await getMenuDetail(row.menuId)
    const menu = res.data ?? row
    Object.assign(form, {
      menuId: menu.menuId,
      parentId: menu.parentId ?? 0,
      menuName: menu.menuName,
      menuType: menu.menuType ?? 'M',
      orderNum: menu.orderNum ?? 1,
      path: menu.path ?? '',
      component: menu.component ?? '',
      query: menu.query ?? '',
      isFrame: menu.isFrame ?? '1',
      isCache: menu.isCache ?? '0',
      visible: menu.visible ?? '0',
      status: menu.status ?? '0',
      perms: menu.perms ?? '',
      icon: menu.icon ?? '',
      remark: menu.remark ?? '',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '菜单详情加载失败')
  }
  nextTick(() => formRef.value?.clearValidate())
}

const needPath = computed(() => form.menuType !== 'F')
const needComponent = computed(() => form.menuType === 'C')
const needPerms = computed(() => form.menuType === 'F')

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (needPath.value && !form.path) {
    ElMessage.warning('请输入路由地址')
    return
  }
  if (needComponent.value && !form.component && form.isFrame !== '0') {
    ElMessage.warning('请输入组件路径')
    return
  }
  if (needPerms.value && !form.perms) {
    ElMessage.warning('按钮菜单必须填写权限标识')
    return
  }
  if (form.isFrame === '0' && form.path && !/^https?:\/\//i.test(form.path)) {
    ElMessage.warning('外链地址必须以 http:// 或 https:// 开头')
    return
  }

  formDialog.loading = true
  try {
    const payload: Partial<SysMenu> = {
      menuId: form.menuId,
      parentId: form.parentId ?? 0,
      menuName: form.menuName,
      menuType: form.menuType,
      orderNum: form.orderNum,
      path: form.path,
      component: form.menuType === 'C' ? form.component : '',
      query: form.query,
      isFrame: form.isFrame,
      isCache: form.isCache,
      visible: form.visible,
      status: form.status,
      perms: form.menuType === 'F' || form.menuType === 'C' ? form.perms : '',
      icon: form.menuType === 'F' ? '' : form.icon,
      remark: form.remark,
    }
    if (formDialog.isEdit) {
      await updateMenu(payload)
      ElMessage.success('修改成功')
    } else {
      await createMenu(payload)
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
async function handleDelete(row: SysMenu) {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMenu(row.menuId)
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.menuName"
        placeholder="菜单名称"
        clearable
        style="width: 180px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
        <el-option label="正常" value="0" />
        <el-option label="停用" value="1" />
      </el-select>
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <el-button @click="toggleExpand">{{ expandAll ? '折叠全部' : '展开全部' }}</el-button>
      <span class="spacer" />
      <el-tag type="info" effect="plain">共 {{ total }} 个节点</el-tag>
      <el-button v-if="can('system:menu:add')" type="primary" :icon="Plus" @click="openAdd()">新增</el-button>
    </div>

    <!-- 树形表格 -->
    <div class="table-card">
      <el-table
        :key="tableKey"
        v-loading="loading"
        :data="list"
        row-key="menuId"
        border
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column label="菜单名称" min-width="220">
          <template #default="{ row }">
            <span class="menu-name">
              <el-icon v-if="row.menuType !== 'F' && row.icon" class="menu-icon">
                <component :is="iconOf(row.icon)" />
              </el-icon>
              <span>{{ row.menuName }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag[row.menuType as string] || 'info'" effect="plain">
              {{ typeMap[row.menuType as string] || row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column label="路由地址 / 外链" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="row.path">
              <el-tag v-if="isExternal(row)" size="small" type="warning" effect="plain" class="ext-tag">外链</el-tag>
              <span :class="{ 'ext-url': isExternal(row) }">{{ row.path }}</span>
            </template>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件路径" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.component || '—' }}</template>
        </el-table-column>
        <el-table-column label="权限标识" min-width="200">
          <template #default="{ row }">
            <span v-if="row.perms" class="perms" @click="copyPerms(row.perms)">
              <code>{{ row.perms }}</code>
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="显示" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visible === '0' ? 'success' : 'info'" effect="plain">
              {{ row.visible === '0' ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="can('system:menu:add') && row.menuType !== 'F'"
              link
              type="primary"
              :icon="Plus"
              @click="openAdd(row)"
            >
              新增
            </el-button>
            <el-button v-if="can('system:menu:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="can('system:menu:remove')" link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="formDialog.visible" :title="formDialog.isEdit ? '修改菜单' : '新增菜单'" width="680px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                :props="{ label: 'label', children: 'children', value: 'value' }"
                value-key="value"
                check-strictly
                default-expand-all
                placeholder="选择上级菜单"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型">
              <el-radio-group v-model="form.menuType">
                <el-radio-button value="M">目录</el-radio-button>
                <el-radio-button value="C">菜单</el-radio-button>
                <el-radio-button value="F">按钮</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="如：用户管理" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="菜单图标">
              <div class="icon-row">
                <el-select
                  v-model="form.icon"
                  filterable
                  allow-create
                  default-first-option
                  clearable
                  placeholder="选择或输入图标名"
                  style="flex: 1"
                >
                  <el-option v-for="name in ICON_PRESETS" :key="name" :label="name" :value="name">
                    <span class="icon-option">
                      <el-icon><component :is="iconOf(name)" /></el-icon>
                      <span>{{ name }}</span>
                    </span>
                  </el-option>
                </el-select>
                <el-icon v-if="form.icon" class="icon-preview"><component :is="iconOf(form.icon)" /></el-icon>
              </div>
            </el-form-item>
          </el-col>

          <template v-if="form.menuType !== 'F'">
            <el-col :span="12">
              <el-form-item label="是否外链">
                <el-radio-group v-model="form.isFrame">
                  <el-radio value="0">是</el-radio>
                  <el-radio value="1">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="form.isFrame === '0' ? '外链地址' : '路由地址'">
                <el-input
                  v-model="form.path"
                  :placeholder="form.isFrame === '0' ? 'https://example.com' : '如：user'"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType === 'C' && form.isFrame !== '0'" :span="12">
              <el-form-item label="组件路径">
                <el-input v-model="form.component" placeholder="如：system/user/index" />
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType === 'C'" :span="12">
              <el-form-item label="路由参数">
                <el-input v-model="form.query" placeholder='如：{"id":1}' />
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType === 'C'" :span="12">
              <el-form-item label="是否缓存">
                <el-radio-group v-model="form.isCache">
                  <el-radio value="0">缓存</el-radio>
                  <el-radio value="1">不缓存</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType === 'C'" :span="12">
              <el-form-item label="权限标识">
                <el-input v-model="form.perms" placeholder="如：system:user:list" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="显示状态">
                <el-radio-group v-model="form.visible">
                  <el-radio value="0">显示</el-radio>
                  <el-radio value="1">隐藏</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="菜单状态">
                <el-radio-group v-model="form.status">
                  <el-radio value="0">正常</el-radio>
                  <el-radio value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </template>

          <template v-else>
            <el-col :span="12">
              <el-form-item label="权限标识" prop="perms">
                <el-input v-model="form.perms" placeholder="如：system:user:add" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="菜单状态">
                <el-radio-group v-model="form.status">
                  <el-radio value="0">正常</el-radio>
                  <el-radio value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </template>

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
  </div>
</template>

<style scoped>
.menu-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.menu-icon {
  color: #7c5cf0;
}

.ext-tag {
  margin-right: 6px;
}

.ext-url {
  color: #e6a23c;
}

.perms {
  cursor: pointer;
}

.perms code {
  background: #f5f3fe;
  color: #6b4fe0;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12.5px;
}

.perms:hover code {
  background: #e9e3fd;
}

.icon-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.icon-preview {
  font-size: 18px;
  color: #7c5cf0;
}

.icon-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
