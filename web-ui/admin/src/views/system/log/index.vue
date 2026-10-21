<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Refresh, Search, Unlock } from '@element-plus/icons-vue'
import {
  cleanLogininfor,
  cleanOperLog,
  deleteLogininfor,
  deleteOperLogs,
  exportLogininfor,
  exportOperLog,
  getLogininforList,
  getOperlogList,
  unlockUser,
} from '@gal/shared'
import type { SysLogininfor, SysOperLog } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { saveBlob } from '@/utils/download'

const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

const activeTab = ref<'oper' | 'loginin'>('oper')

const BUSINESS_TYPE: Record<number, string> = {
  0: '其它',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '授权',
  5: '导出',
  6: '导入',
  7: '强退',
  8: '生成代码',
  9: '清空数据',
}

/* ==================== 操作日志 ==================== */
const operLoading = ref(false)
const operList = ref<SysOperLog[]>([])
const operTotal = ref(0)
const operSelection = ref<SysOperLog[]>([])
const operQuery = reactive({
  title: '',
  operName: '',
  status: '' as '' | '0' | '1',
  businessType: '' as '' | number,
  pageNum: 1,
})
const operPageSize = 10

function operParams(withPage = true): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (operQuery.title) params.title = operQuery.title
  if (operQuery.operName) params.operName = operQuery.operName
  if (operQuery.status !== '') params.status = operQuery.status
  if (operQuery.businessType !== '') params.businessType = operQuery.businessType
  if (withPage) {
    params.pageNum = operQuery.pageNum
    params.pageSize = operPageSize
  }
  return params
}

async function loadOper() {
  operLoading.value = true
  try {
    const res = await getOperlogList(operParams())
    operList.value = res.rows ?? []
    operTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '操作日志加载失败')
  } finally {
    operLoading.value = false
  }
}

function resetOperQuery() {
  operQuery.title = ''
  operQuery.operName = ''
  operQuery.status = ''
  operQuery.businessType = ''
  operQuery.pageNum = 1
  loadOper()
}

async function deleteOper(rows?: SysOperLog[]) {
  const targets = rows ?? operSelection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条操作日志吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteOperLogs(targets.map((l) => l.operId).join(','))
    ElMessage.success('删除成功')
    loadOper()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function cleanOper() {
  try {
    await ElMessageBox.confirm('确认清空全部操作日志吗？该操作不可恢复。', '清空确认', {
      type: 'warning',
      confirmButtonText: '确认清空',
    })
  } catch {
    return
  }
  try {
    await cleanOperLog()
    ElMessage.success('清空成功')
    loadOper()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '清空失败')
  }
}

async function exportOper() {
  try {
    const blob = await exportOperLog(operParams(false))
    saveBlob(blob, `操作日志_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
}

/* ==================== 登录日志 ==================== */
const loginLoading = ref(false)
const loginList = ref<SysLogininfor[]>([])
const loginTotal = ref(0)
const loginSelection = ref<SysLogininfor[]>([])
const loginQuery = reactive({ userName: '', ipaddr: '', status: '', pageNum: 1 })
const loginPageSize = 10

function loginParams(withPage = true): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (loginQuery.userName) params.userName = loginQuery.userName
  if (loginQuery.ipaddr) params.ipaddr = loginQuery.ipaddr
  if (loginQuery.status) params.status = loginQuery.status
  if (withPage) {
    params.pageNum = loginQuery.pageNum
    params.pageSize = loginPageSize
  }
  return params
}

async function loadLogin() {
  loginLoading.value = true
  try {
    const res = await getLogininforList(loginParams())
    loginList.value = res.rows ?? []
    loginTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '登录日志加载失败')
  } finally {
    loginLoading.value = false
  }
}

function resetLoginQuery() {
  loginQuery.userName = ''
  loginQuery.ipaddr = ''
  loginQuery.status = ''
  loginQuery.pageNum = 1
  loadLogin()
}

async function deleteLogin(rows?: SysLogininfor[]) {
  const targets = rows ?? loginSelection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条登录日志吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteLogininfor(targets.map((l) => l.infoId).join(','))
    ElMessage.success('删除成功')
    loadLogin()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function cleanLogin() {
  try {
    await ElMessageBox.confirm('确认清空全部登录日志吗？该操作不可恢复。', '清空确认', {
      type: 'warning',
      confirmButtonText: '确认清空',
    })
  } catch {
    return
  }
  try {
    await cleanLogininfor()
    ElMessage.success('清空成功')
    loadLogin()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '清空失败')
  }
}

async function exportLogin() {
  try {
    const blob = await exportLogininfor(loginParams(false))
    saveBlob(blob, `登录日志_${Date.now()}.xlsx`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '导出失败')
  }
}

/** 账户解锁（清除密码错误次数） */
async function handleUnlock(row: SysLogininfor) {
  if (!row.userName) {
    ElMessage.warning('该日志缺少用户账号')
    return
  }
  const userName = row.userName
  try {
    await ElMessageBox.confirm(`确认解除用户「${userName}」的登录锁定吗？`, '账户解锁', { type: 'warning' })
  } catch {
    return
  }
  try {
    await unlockUser(userName)
    ElMessage.success(`用户「${userName}」解锁成功`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '解锁失败')
  }
}

/** 页签切换时懒加载 */
function onTabChange(name: string | number) {
  activeTab.value = name as 'oper' | 'loginin'
  if (name === 'loginin') loadLogin()
  else loadOper()
}

onMounted(() => {
  loadOper()
  loadLogin()
})
</script>

<template>
  <div class="page">
    <el-tabs :model-value="activeTab" @tab-change="onTabChange">
      <!-- ================= 操作日志 ================= -->
      <el-tab-pane label="操作日志" name="oper">
        <div class="toolbar table-card">
          <el-input
            v-model="operQuery.title"
            placeholder="系统模块"
            clearable
            style="width: 150px"
            @keyup.enter="loadOper"
            @clear="loadOper"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="operQuery.operName"
            placeholder="操作人员"
            clearable
            style="width: 140px"
            @keyup.enter="loadOper"
            @clear="loadOper"
          />
          <el-select v-model="operQuery.businessType" placeholder="操作类型" clearable style="width: 130px" @change="loadOper">
            <el-option v-for="(label, value) in BUSINESS_TYPE" :key="value" :label="label" :value="Number(value)" />
          </el-select>
          <el-select v-model="operQuery.status" placeholder="状态" clearable style="width: 120px" @change="loadOper">
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
          <el-button class="grad-btn" @click="loadOper">查询</el-button>
          <el-button :icon="Refresh" @click="resetOperQuery">重置</el-button>
          <span class="spacer" />
          <el-button
            v-if="can('monitor:operlog:remove')"
            type="danger"
            plain
            :disabled="!operSelection.length"
            @click="deleteOper()"
          >
            删除
          </el-button>
          <el-button v-if="can('monitor:operlog:remove')" type="danger" plain @click="cleanOper">清空</el-button>
          <el-button v-if="can('monitor:operlog:export')" :icon="Download" @click="exportOper">导出</el-button>
        </div>

        <div class="table-card">
          <el-table
            v-loading="operLoading"
            :data="operList"
            border
            stripe
            row-key="operId"
            @selection-change="(r: SysOperLog[]) => (operSelection = r)"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-box">
                  <p><b>请求参数：</b><span>{{ row.operParam || '—' }}</span></p>
                  <p><b>返回结果：</b><span>{{ row.jsonResult || '—' }}</span></p>
                  <p><b>异常信息：</b><span>{{ row.errorMsg || '—' }}</span></p>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="operId" label="ID" width="80" />
            <el-table-column prop="title" label="系统模块" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ BUSINESS_TYPE[row.businessType] ?? row.businessType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operName" label="操作人员" width="120">
              <template #default="{ row }">{{ row.operName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="operIp" label="操作IP" width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operTime" label="操作时间" width="165">
              <template #default="{ row }">{{ row.operTime || '—' }}</template>
            </el-table-column>
            <el-table-column label="耗时" width="90" align="right">
              <template #default="{ row }">{{ row.costTime != null ? `${row.costTime} ms` : '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="can('monitor:operlog:remove')"
                  link
                  type="danger"
                  :icon="Delete"
                  @click="deleteOper([row])"
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
              :total="operTotal"
              :page-size="operPageSize"
              :current-page="operQuery.pageNum"
              @current-change="(p: number) => { operQuery.pageNum = p; loadOper() }"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ================= 登录日志 ================= -->
      <el-tab-pane label="登录日志" name="loginin">
        <div class="toolbar table-card">
          <el-input
            v-model="loginQuery.userName"
            placeholder="用户账号"
            clearable
            style="width: 150px"
            @keyup.enter="loadLogin"
            @clear="loadLogin"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="loginQuery.ipaddr"
            placeholder="登录IP"
            clearable
            style="width: 150px"
            @keyup.enter="loadLogin"
            @clear="loadLogin"
          />
          <el-select v-model="loginQuery.status" placeholder="状态" clearable style="width: 120px" @change="loadLogin">
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
          <el-button class="grad-btn" @click="loadLogin">查询</el-button>
          <el-button :icon="Refresh" @click="resetLoginQuery">重置</el-button>
          <span class="spacer" />
          <el-button
            v-if="can('monitor:logininfor:remove')"
            type="danger"
            plain
            :disabled="!loginSelection.length"
            @click="deleteLogin()"
          >
            删除
          </el-button>
          <el-button v-if="can('monitor:logininfor:remove')" type="danger" plain @click="cleanLogin">清空</el-button>
          <el-button v-if="can('monitor:logininfor:export')" :icon="Download" @click="exportLogin">导出</el-button>
        </div>

        <div class="table-card">
          <el-table
            v-loading="loginLoading"
            :data="loginList"
            border
            stripe
            @selection-change="(r: SysLogininfor[]) => (loginSelection = r)"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="infoId" label="ID" width="80" />
            <el-table-column prop="userName" label="用户账号" min-width="120" />
            <el-table-column prop="ipaddr" label="登录IP" width="140" />
            <el-table-column prop="loginLocation" label="登录地点" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.loginLocation || '—' }}</template>
            </el-table-column>
            <el-table-column prop="browser" label="浏览器" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.browser || '—' }}</template>
            </el-table-column>
            <el-table-column prop="os" label="操作系统" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.os || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="msg" label="操作信息" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.msg || '—' }}</template>
            </el-table-column>
            <el-table-column prop="loginTime" label="登录时间" width="165">
              <template #default="{ row }">{{ row.loginTime || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="can('monitor:logininfor:unlock')"
                  link
                  type="primary"
                  :icon="Unlock"
                  @click="handleUnlock(row)"
                >
                  解锁
                </el-button>
                <el-button
                  v-if="can('monitor:logininfor:remove')"
                  link
                  type="danger"
                  :icon="Delete"
                  @click="deleteLogin([row])"
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
              :total="loginTotal"
              :page-size="loginPageSize"
              :current-page="loginQuery.pageNum"
              @current-change="(p: number) => { loginQuery.pageNum = p; loadLogin() }"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.expand-box {
  padding: 6px 16px 6px 48px;
  color: #4a4a5e;
  font-size: 12.5px;
  line-height: 1.8;
  word-break: break-all;
}

.expand-box p {
  margin: 2px 0;
}

.expand-box b {
  color: #2b2b3c;
}
</style>
