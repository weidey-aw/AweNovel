<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Link, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createOfficialResource,
  createResource,
  deleteOfficialResources,
  deleteResources,
  getAdminGameList,
  getAdminResourceDetail,
  getAdminResourceList,
  getOfficialResources,
  getResourceTypes,
  resolveAssetUrl,
  updateOfficialResource,
  updateResource,
  uploadFile,
} from '@gal/shared'
import type { DictItem, Game, OfficialResource, Resource, ResourceSource } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/** 从游戏列表「编辑资源」跳转时携带 gameId */
const routeGameId = computed(() => {
  const raw = route.query.gameId
  return raw ? Number(raw) : undefined
})

const activeTab = ref<'community' | 'official'>('community')

/* ==================== 字典 / 游戏 ==================== */
const resourceTypes = ref<DictItem[]>([])
const officialTypes = ref<DictItem[]>([])
const games = ref<Game[]>([])

const typeLabel = (value?: string, list = resourceTypes.value) =>
  list.find((d) => d.dictValue === value)?.dictLabel ?? value ?? '—'

async function loadOptions() {
  try {
    const res = await getResourceTypes()
    resourceTypes.value = res.data?.resource ?? []
    officialTypes.value = res.data?.official ?? []
  } catch {
    resourceTypes.value = []
    officialTypes.value = []
  }
  try {
    const res = await getAdminGameList({ pageNum: 1, pageSize: 500 })
    games.value = res.rows ?? []
  } catch {
    games.value = []
  }
}

function gameLabel(gameId?: number) {
  const g = games.value.find((item) => item.gameId === gameId)
  return g ? g.titleCn || g.title : gameId ? `#${gameId}` : '—'
}

/* ==================== 社区资源 ==================== */
const loading = ref(false)
const list = ref<Resource[]>([])
const total = ref(0)
const selection = ref<Resource[]>([])
const query = reactive({
  keyword: '',
  type: '',
  status: '',
  gameId: routeGameId.value as number | undefined,
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getAdminResourceList({
      pageNum: query.pageNum,
      pageSize,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      status: query.status || undefined,
      gameId: query.gameId,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '资源列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.type = ''
  query.status = ''
  query.gameId = routeGameId.value
  query.pageNum = 1
  load()
}

/* ---------- 资源编辑弹窗 ---------- */
const dialog = reactive({ visible: false, isEdit: false, loading: false })
const formRef = ref<FormInstance>()
const form = reactive<Partial<Resource>>({
  resourceId: undefined,
  gameId: routeGameId.value,
  title: '',
  type: 'netdisk',
  version: '',
  size: '',
  points: 0,
  checksum: '',
  status: '1',
})
const sources = ref<ResourceSource[]>([])

const rules: FormRules = {
  gameId: [{ required: true, message: '请选择所属游戏', trigger: 'change' }],
  title: [{ required: true, message: '请输入资源标题', trigger: 'blur' }],
}

function newSource(): ResourceSource {
  return { sourceType: 'netdisk', url: '', extractPwd: '', unzipPwd: '', remark: '', sort: 0 }
}

function addSource() {
  sources.value.push(newSource())
}

function removeSource(index: number) {
  sources.value.splice(index, 1)
}

function openAdd() {
  dialog.isEdit = false
  dialog.visible = true
  Object.assign(form, {
    resourceId: undefined,
    gameId: query.gameId,
    title: '',
    type: 'netdisk',
    version: '',
    size: '',
    points: 0,
    checksum: '',
    status: '1',
  })
  sources.value = [newSource()]
  formRef.value?.clearValidate()
}

async function openEdit(row: Resource) {
  dialog.isEdit = true
  dialog.visible = true
  formRef.value?.clearValidate()
  try {
    const res = await getAdminResourceDetail(row.resourceId)
    const detail = res.data ?? row
    Object.assign(form, {
      resourceId: detail.resourceId,
      gameId: detail.gameId,
      title: detail.title,
      type: detail.type ?? 'netdisk',
      version: detail.version ?? '',
      size: detail.size ?? '',
      points: detail.points ?? 0,
      checksum: detail.checksum ?? '',
      status: detail.status ?? '1',
    })
    sources.value = (detail.sources ?? []).map((s) => ({ ...s }))
    if (!sources.value.length) sources.value = [newSource()]
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '资源详情加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload: Partial<Resource> = {
    ...form,
    sources: sources.value.filter((s) => s.url && s.url.trim()),
  }
  dialog.loading = true
  try {
    if (dialog.isEdit) {
      await updateResource(payload)
      ElMessage.success('修改成功')
    } else {
      await createResource(payload)
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    dialog.loading = false
  }
}

async function handleDelete(rows?: Resource[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的资源')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${targets.length} 个资源吗？其资源来源会一并删除。`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteResources(targets.map((r) => r.resourceId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

/* ==================== 正版资源 ==================== */
const officialLoading = ref(false)
const officialList = ref<OfficialResource[]>([])
const officialDialog = reactive({ visible: false, isEdit: false, loading: false })
const officialFormRef = ref<FormInstance>()
const officialForm = reactive<Partial<OfficialResource>>({
  officialId: undefined,
  gameId: routeGameId.value,
  title: '',
  sourceType: 'official',
  url: '',
  icon: '',
  sort: 0,
})
const officialRules: FormRules = {
  gameId: [{ required: true, message: '请选择所属游戏', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  url: [{ required: true, message: '请输入官方链接', trigger: 'blur' }],
}

async function loadOfficial() {
  if (!query.gameId) {
    officialList.value = []
    return
  }
  officialLoading.value = true
  try {
    const res = await getOfficialResources(query.gameId)
    officialList.value = res.data ?? []
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '正版资源加载失败')
  } finally {
    officialLoading.value = false
  }
}

function openOfficialAdd() {
  officialDialog.isEdit = false
  officialDialog.visible = true
  Object.assign(officialForm, {
    officialId: undefined,
    gameId: query.gameId,
    title: '',
    sourceType: 'official',
    url: '',
    icon: '',
    sort: officialList.value.length + 1,
  })
  officialFormRef.value?.clearValidate()
}

function openOfficialEdit(row: OfficialResource) {
  officialDialog.isEdit = true
  officialDialog.visible = true
  Object.assign(officialForm, { ...row })
  officialFormRef.value?.clearValidate()
}

async function submitOfficial() {
  if (!officialFormRef.value) return
  const valid = await officialFormRef.value.validate().catch(() => false)
  if (!valid) return
  officialDialog.loading = true
  try {
    if (officialDialog.isEdit) {
      await updateOfficialResource({ ...officialForm })
      ElMessage.success('修改成功')
    } else {
      await createOfficialResource({ ...officialForm })
      ElMessage.success('新增成功')
    }
    officialDialog.visible = false
    loadOfficial()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    officialDialog.loading = false
  }
}

async function removeOfficial(row: OfficialResource) {
  try {
    await ElMessageBox.confirm(`确认删除正版资源「${row.title}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteOfficialResources(String(row.officialId))
    ElMessage.success('删除成功')
    loadOfficial()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

/* 图标上传 */
const iconInputRef = ref<HTMLInputElement>()
const iconUploading = ref(false)

function pickIcon() {
  iconInputRef.value?.click()
}

async function onIconChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图标不能超过 2MB')
    return
  }
  iconUploading.value = true
  try {
    const res = await uploadFile(file)
    officialForm.icon = res.fileName || res.url
    ElMessage.success('图标已上传')
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败')
  } finally {
    iconUploading.value = false
  }
}

function onTabChange(name: string | number) {
  activeTab.value = name as 'community' | 'official'
  if (name === 'official') loadOfficial()
  else load()
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<template>
  <div class="page">
    <el-tabs :model-value="activeTab" @tab-change="onTabChange">
      <!-- ==================== 社区资源 ==================== -->
      <el-tab-pane label="社区资源" name="community">
        <div class="toolbar table-card">
          <el-tag
            v-if="query.gameId"
            closable
            type="info"
            effect="plain"
            @close="
              () => {
                query.gameId = undefined
                search()
              }
            "
          >
            所属游戏：{{ gameLabel(query.gameId) }}
          </el-tag>
          <el-input
            v-model="query.keyword"
            placeholder="资源标题"
            clearable
            style="width: 180px"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.type" placeholder="资源类型" clearable style="width: 140px" @change="search">
            <el-option v-for="d in resourceTypes" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
            <el-option label="待审核" value="0" />
            <el-option label="已通过" value="1" />
            <el-option label="已拒绝" value="2" />
          </el-select>
          <el-button class="grad-btn" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
          <span class="spacer" />
          <el-button v-if="can('community:resource:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
          <el-button
            v-if="can('community:resource:remove')"
            type="danger"
            plain
            :disabled="!selection.length"
            @click="handleDelete()"
          >
            删除
          </el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: Resource[]) => (selection = r)">
            <el-table-column type="selection" width="46" />
            <el-table-column prop="resourceId" label="ID" width="70" />
            <el-table-column prop="title" label="资源标题" min-width="200" show-overflow-tooltip />
            <el-table-column label="所属游戏" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.gameTitle || gameLabel(row.gameId) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="来源" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="(row.sourceCount ?? 0) > 0 ? 'success' : 'info'">
                  {{ row.sourceCount ?? 0 }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="积分" width="80" align="center">
              <template #default="{ row }">{{ row.points ?? 0 }}</template>
            </el-table-column>
            <el-table-column prop="downloadCount" label="下载" width="80" align="center" />
            <el-table-column label="发布者" width="130">
              <template #default="{ row }">{{ row.userName || `#${row.userId}` }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === '1' ? 'success' : row.status === '2' ? 'danger' : 'warning'">
                  {{ row.status === '1' ? '已通过' : row.status === '2' ? '已拒绝' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button v-if="can('community:resource:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
                  编辑
                </el-button>
                <el-button
                  v-if="can('community:resource:remove')"
                  link
                  type="danger"
                  :icon="Delete"
                  @click="handleDelete([row])"
                >
                  删除
                </el-button>
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
      </el-tab-pane>

      <!-- ==================== 正版资源 ==================== -->
      <el-tab-pane label="正版资源" name="official">
        <div class="toolbar table-card">
          <el-tag v-if="query.gameId" type="info" effect="plain">所属游戏：{{ gameLabel(query.gameId) }}</el-tag>
          <span v-if="!query.gameId" class="hint">请先在「社区资源」页签选择所属游戏，再维护正版资源</span>
          <span class="spacer" />
          <el-button
            v-if="can('community:resource:edit') && query.gameId"
            type="primary"
            :icon="Plus"
            @click="openOfficialAdd"
          >
            新增正版资源
          </el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="officialLoading" :data="officialList" border stripe>
            <el-table-column prop="officialId" label="ID" width="70" />
            <el-table-column label="图标" width="80" align="center">
              <template #default="{ row }">
                <el-avatar v-if="row.icon" :size="32" shape="square" :src="resolveAssetUrl(row.icon)" />
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column label="类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ typeLabel(row.sourceType, officialTypes) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="链接" min-width="240">
              <template #default="{ row }">
                <a :href="row.url" target="_blank" rel="noopener noreferrer" class="site-link">
                  <el-icon><Link /></el-icon>{{ row.url }}
                </a>
              </template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="can('community:resource:edit')"
                  link
                  type="primary"
                  :icon="Edit"
                  @click="openOfficialEdit(row)"
                >
                  修改
                </el-button>
                <el-button
                  v-if="can('community:resource:edit')"
                  link
                  type="danger"
                  :icon="Delete"
                  @click="removeOfficial(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!query.gameId" description="请选择所属游戏后维护正版资源" :image-size="72" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 资源编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '修改资源' : '新增资源'" width="880px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属游戏" prop="gameId">
              <el-select v-model="form.gameId" filterable placeholder="选择游戏" style="width: 100%">
                <el-option
                  v-for="g in games"
                  :key="g.gameId"
                  :label="g.titleCn ? `${g.titleCn}（${g.title}）` : g.title"
                  :value="g.gameId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资源标题" prop="title">
              <el-input v-model="form.title" placeholder="如：汉化硬盘版 v1.2" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="资源类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="d in resourceTypes" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="版本">
              <el-input v-model="form.version" placeholder="如：1.2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大小">
              <el-input v-model="form.size" placeholder="如：8.5GB" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所需积分">
              <el-input-number v-model="form.points" :min="0" :max="99999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="待审核" value="0" />
                <el-option label="已通过" value="1" />
                <el-option label="已拒绝" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="校验码">
              <el-input v-model="form.checksum" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="block-head">
          资源来源
          <span class="block-sub">每个来源可单独设置链接、提取码、解压码</span>
        </div>
        <div class="source-list">
          <div v-for="(s, index) in sources" :key="index" class="source-row">
            <el-select v-model="s.sourceType" style="width: 120px">
              <el-option v-for="d in resourceTypes" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
            </el-select>
            <el-input v-model="s.url" placeholder="资源链接" class="s-url" />
            <el-input v-model="s.extractPwd" placeholder="提取码" style="width: 110px" />
            <el-input v-model="s.unzipPwd" placeholder="解压码" style="width: 110px" />
            <el-input v-model="s.remark" placeholder="备注" style="width: 130px" />
            <el-button text type="danger" :icon="Delete" @click="removeSource(index)" />
          </div>
          <el-button text type="primary" :icon="Plus" @click="addSource">添加来源</el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 正版资源弹窗 -->
    <el-dialog
      v-model="officialDialog.visible"
      :title="officialDialog.isEdit ? '修改正版资源' : '新增正版资源'"
      width="560px"
    >
      <el-form ref="officialFormRef" :model="officialForm" :rules="officialRules" label-width="90px">
        <el-form-item label="所属游戏" prop="gameId">
          <el-select v-model="officialForm.gameId" filterable placeholder="选择游戏" style="width: 100%">
            <el-option
              v-for="g in games"
              :key="g.gameId"
              :label="g.titleCn ? `${g.titleCn}（${g.title}）` : g.title"
              :value="g.gameId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="officialForm.title" placeholder="如：官方商店（DLsite）" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="officialForm.sourceType" style="width: 100%">
            <el-option v-for="d in officialTypes" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="官方链接" prop="url">
          <el-input v-model="officialForm.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="小图标">
          <div class="icon-row">
            <el-avatar v-if="officialForm.icon" :size="40" shape="square" :src="resolveAssetUrl(officialForm.icon)" />
            <el-button :loading="iconUploading" @click="pickIcon">
              {{ officialForm.icon ? '更换图标' : '上传图标' }}
            </el-button>
            <el-button v-if="officialForm.icon" text type="danger" @click="officialForm.icon = ''">移除</el-button>
            <input ref="iconInputRef" type="file" accept="image/*" hidden @change="onIconChange" />
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="officialForm.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="officialDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="officialDialog.loading" @click="submitOfficial">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.muted {
  color: #a0a0b8;
}

.hint {
  color: #9a9ab0;
  font-size: 12.5px;
}

.block-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-weight: 600;
  color: #4a4a5e;
  margin: 4px 0 10px;
  padding-top: 12px;
  border-top: 1px solid #f2f2f8;
}

.block-sub {
  color: #9a9ab0;
  font-weight: 400;
  font-size: 12px;
}

.source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.source-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.s-url {
  flex: 1;
  min-width: 160px;
}

.icon-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.site-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #6b4fe0;
  text-decoration: none;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.site-link:hover {
  text-decoration: underline;
}
</style>
