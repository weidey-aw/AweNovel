<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { createNotice, deleteNotices, getNoticeDetail, getNoticeList, updateNotice } from '@gal/shared'
import type { SysNotice } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

const NOTICE_TYPE: Record<string, { label: string; type: 'primary' | 'success' }> = {
  '1': { label: '通知', type: 'primary' },
  '2': { label: '公告', type: 'success' },
}

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<SysNotice[]>([])
const total = ref(0)
const selection = ref<SysNotice[]>([])
const query = reactive({ noticeTitle: '', createBy: '', noticeType: '', status: '', pageNum: 1 })
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const res = await getNoticeList({
      pageNum: query.pageNum,
      pageSize,
      noticeTitle: query.noticeTitle || undefined,
      createBy: query.createBy || undefined,
      noticeType: query.noticeType || undefined,
      status: query.status || undefined,
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '公告列表加载失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.noticeTitle = ''
  query.createBy = ''
  query.noticeType = ''
  query.status = ''
  query.pageNum = 1
  load()
}

/* ==================== 详情 ==================== */
const detailDialog = reactive({ visible: false, loading: false, notice: null as SysNotice | null })

async function openDetail(row: SysNotice) {
  detailDialog.visible = true
  detailDialog.loading = true
  detailDialog.notice = row
  try {
    const res = await getNoticeDetail(row.noticeId)
    if (res.data) detailDialog.notice = res.data
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '公告详情加载失败')
  } finally {
    detailDialog.loading = false
  }
}

/* ==================== 新增 / 修改 ==================== */
const formRef = ref<FormInstance>()
const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<SysNotice>>({
  noticeId: undefined,
  noticeTitle: '',
  noticeType: '1',
  noticeContent: '',
  status: '0',
  remark: '',
})
const rules: FormRules = {
  noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeContent: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

function openAdd() {
  dialog.isEdit = false
  dialog.visible = true
  Object.assign(form, {
    noticeId: undefined,
    noticeTitle: '',
    noticeType: '1',
    noticeContent: '',
    status: '0',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function openEdit(row: SysNotice) {
  dialog.isEdit = true
  dialog.visible = true
  formRef.value?.clearValidate()
  try {
    const res = await getNoticeDetail(row.noticeId)
    const detail = res.data ?? row
    Object.assign(form, {
      noticeId: detail.noticeId,
      noticeTitle: detail.noticeTitle,
      noticeType: detail.noticeType ?? '1',
      noticeContent: detail.noticeContent ?? '',
      status: detail.status ?? '0',
      remark: detail.remark ?? '',
    })
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '公告详情加载失败')
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialog.loading = true
  try {
    if (dialog.isEdit) {
      await updateNotice({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createNotice({ ...form })
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

/* ==================== 删除 ==================== */
async function handleDelete(rows?: SysNotice[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的公告')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条公告吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteNotices(targets.map((n) => n.noticeId).join(','))
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
        v-model="query.noticeTitle"
        placeholder="公告标题"
        clearable
        style="width: 180px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="query.createBy"
        placeholder="创建者"
        clearable
        style="width: 140px"
        @keyup.enter="load"
        @clear="load"
      />
      <el-select v-model="query.noticeType" placeholder="类型" clearable style="width: 120px" @change="load">
        <el-option label="通知" value="1" />
        <el-option label="公告" value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
        <el-option label="正常" value="0" />
        <el-option label="关闭" value="1" />
      </el-select>
      <el-button class="grad-btn" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('system:notice:add')" type="primary" :icon="Plus" @click="openAdd">新增</el-button>
      <el-button
        v-if="can('system:notice:remove')"
        type="danger"
        plain
        :disabled="!selection.length"
        @click="handleDelete()"
      >
        删除
      </el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: SysNotice[]) => (selection = r)">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="noticeId" label="ID" width="70" />
        <el-table-column prop="noticeTitle" label="公告标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="NOTICE_TYPE[row.noticeType as string]?.type ?? 'info'" effect="plain">
              {{ NOTICE_TYPE[row.noticeType as string]?.label ?? row.noticeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '正常' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建者" width="120">
          <template #default="{ row }">{{ row.createBy || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-if="can('system:notice:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-if="can('system:notice:remove')" link type="danger" :icon="Delete" @click="handleDelete([row])">
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

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '修改公告' : '新增公告'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-radio-group v-model="form.noticeType">
            <el-radio value="1">通知</el-radio>
            <el-radio value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公告内容" prop="noticeContent">
          <el-input
            v-model="form.noticeContent"
            type="textarea"
            :rows="7"
            placeholder="支持纯文本，内容会原样展示在用户端"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialog.visible" title="公告详情" width="620px">
      <div v-loading="detailDialog.loading" class="notice-detail">
        <h3 class="notice-title">{{ detailDialog.notice?.noticeTitle }}</h3>
        <div class="notice-meta">
          <el-tag size="small" effect="plain">
            {{ NOTICE_TYPE[detailDialog.notice?.noticeType ?? '']?.label ?? '—' }}
          </el-tag>
          <span>{{ detailDialog.notice?.createBy || '—' }}</span>
          <span>{{ detailDialog.notice?.createTime || '—' }}</span>
        </div>
        <p class="notice-content">{{ detailDialog.notice?.noticeContent || '暂无内容' }}</p>
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
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

.notice-detail {
  min-height: 120px;
}

.notice-title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #2b2b3c;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #8a8aa3;
  font-size: 12.5px;
  margin-bottom: 14px;
}

.notice-content {
  margin: 0;
  line-height: 1.8;
  color: #4a4a5e;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
