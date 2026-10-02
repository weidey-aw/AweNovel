<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getLogininforList, getOperlogList } from '@gal/shared'
import type { SysLogininfor, SysOperLog } from '@gal/shared'

const activeTab = ref('oper')

/* ---------------- 操作日志 ---------------- */
const operLoading = ref(false)
const operList = ref<SysOperLog[]>([])
const operTotal = ref(0)
const operQuery = reactive({ title: '', operName: '', pageNum: 1 })

const businessTypeMap: Record<number, string> = {
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

async function loadOper() {
  operLoading.value = true
  try {
    const res = await getOperlogList({
      pageNum: operQuery.pageNum,
      pageSize: 10,
      title: operQuery.title || undefined,
      operName: operQuery.operName || undefined,
    })
    operList.value = res.rows ?? []
    operTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '操作日志加载失败')
  } finally {
    operLoading.value = false
  }
}

function resetOper() {
  operQuery.title = ''
  operQuery.operName = ''
  operQuery.pageNum = 1
  loadOper()
}

/* ---------------- 登录日志 ---------------- */
const loginLoading = ref(false)
const loginList = ref<SysLogininfor[]>([])
const loginTotal = ref(0)
const loginQuery = reactive({ userName: '', ipaddr: '', status: '', pageNum: 1 })

async function loadLogin() {
  loginLoading.value = true
  try {
    const res = await getLogininforList({
      pageNum: loginQuery.pageNum,
      pageSize: 10,
      userName: loginQuery.userName || undefined,
      ipaddr: loginQuery.ipaddr || undefined,
      status: loginQuery.status || undefined,
    })
    loginList.value = res.rows ?? []
    loginTotal.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '登录日志加载失败')
  } finally {
    loginLoading.value = false
  }
}

function resetLogin() {
  loginQuery.userName = ''
  loginQuery.ipaddr = ''
  loginQuery.status = ''
  loginQuery.pageNum = 1
  loadLogin()
}

onMounted(() => {
  loadOper()
  loadLogin()
})
</script>

<template>
  <div class="page">
    <el-tabs v-model="activeTab" class="log-tabs">
      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="oper">
        <div class="toolbar table-card">
          <el-input
            v-model="operQuery.title"
            placeholder="操作模块"
            clearable
            style="width: 180px"
            @keyup.enter="loadOper"
            @clear="loadOper"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="operQuery.operName"
            placeholder="操作人员"
            clearable
            style="width: 160px"
            @keyup.enter="loadOper"
            @clear="loadOper"
          />
          <el-button class="grad-btn" @click="loadOper">查询</el-button>
          <el-button :icon="Refresh" @click="resetOper">重置</el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="operLoading" :data="operList" border stripe>
            <el-table-column prop="operId" label="ID" width="80" />
            <el-table-column prop="title" label="模块" width="120" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">
                  {{ businessTypeMap[row.businessType as number] || row.businessType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="requestMethod" label="请求方式" width="100" />
            <el-table-column prop="operName" label="操作人员" width="120" />
            <el-table-column prop="operIp" label="IP" width="140" />
            <el-table-column prop="operUrl" label="请求地址" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '正常' : '异常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="costTime" label="耗时(ms)" width="100" />
            <el-table-column prop="operTime" label="操作时间" width="170">
              <template #default="{ row }">{{ row.operTime || '—' }}</template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="operTotal"
              :page-size="10"
              :current-page="operQuery.pageNum"
              @current-change="(p: number) => { operQuery.pageNum = p; loadOper() }"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 登录日志 -->
      <el-tab-pane label="登录日志" name="login">
        <div class="toolbar table-card">
          <el-input
            v-model="loginQuery.userName"
            placeholder="用户账号"
            clearable
            style="width: 160px"
            @keyup.enter="loadLogin"
            @clear="loadLogin"
          />
          <el-input
            v-model="loginQuery.ipaddr"
            placeholder="登录IP"
            clearable
            style="width: 160px"
            @keyup.enter="loadLogin"
            @clear="loadLogin"
          />
          <el-select v-model="loginQuery.status" placeholder="状态" clearable style="width: 140px" @change="loadLogin">
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
          <el-button class="grad-btn" @click="loadLogin">查询</el-button>
          <el-button :icon="Refresh" @click="resetLogin">重置</el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="loginLoading" :data="loginList" border stripe>
            <el-table-column prop="infoId" label="ID" width="80" />
            <el-table-column prop="userName" label="用户账号" min-width="130" />
            <el-table-column prop="ipaddr" label="登录IP" min-width="140" />
            <el-table-column prop="loginLocation" label="登录地点" min-width="140">
              <template #default="{ row }">{{ row.loginLocation || '—' }}</template>
            </el-table-column>
            <el-table-column prop="browser" label="浏览器" min-width="130">
              <template #default="{ row }">{{ row.browser || '—' }}</template>
            </el-table-column>
            <el-table-column prop="os" label="操作系统" min-width="130">
              <template #default="{ row }">{{ row.os || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                  {{ row.status === '0' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="msg" label="提示消息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="loginTime" label="访问时间" width="170">
              <template #default="{ row }">{{ row.loginTime || '—' }}</template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="loginTotal"
              :page-size="10"
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
.log-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}
</style>
