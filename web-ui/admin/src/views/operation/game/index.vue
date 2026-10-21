<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import {
  deleteGames,
  getAdminGameList,
  getAllTags,
  getBrandList,
  offlineGame,
  publishGame,
  resolveAssetUrl,
  updateGame,
  uploadFile,
  withdrawGame,
} from '@gal/shared'
import type { Brand, Game, Tag } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import ImageCropper from '@/components/ImageCropper.vue'

const router = useRouter()
const auth = useAuthStore()
const can = (perm: string) => auth.hasPermi(perm)

/** 状态：0下架 1已发布 2待审核 3驳回 4草稿 */
const STATUS_MAP: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  '0': { label: '已下架', type: 'info' },
  '1': { label: '已发布', type: 'success' },
  '2': { label: '待审核', type: 'warning' },
  '3': { label: '审核驳回', type: 'danger' },
  '4': { label: '草稿', type: 'info' },
}

/* ==================== 列表 ==================== */
const loading = ref(false)
const list = ref<Game[]>([])
const total = ref(0)
const selection = ref<Game[]>([])
const brands = ref<Brand[]>([])
const tags = ref<Tag[]>([])

const query = reactive({
  keyword: '',
  brandId: undefined as number | undefined,
  tagIds: [] as number[],
  status: '',
  dateRange: null as [string, string] | null,
  pageNum: 1,
})
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize,
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.brandId) params.brandId = query.brandId
    if (query.tagIds.length) params.tagIds = query.tagIds.join(',')
    if (query.status) params.status = query.status
    if (query.dateRange && query.dateRange.length === 2) {
      params.beginDate = query.dateRange[0]
      params.endDate = query.dateRange[1]
    }
    const res = await getAdminGameList(params)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '游戏列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const res = (await getAllTags()) as unknown as { data?: Tag[] } | Tag[]
    tags.value = (Array.isArray(res) ? res : res?.data) ?? []
  } catch {
    tags.value = []
  }
  try {
    const res = (await getBrandList()) as unknown as { rows?: Brand[]; data?: Brand[] } | Brand[]
    brands.value = Array.isArray(res) ? res : (res?.rows ?? res?.data ?? [])
  } catch {
    brands.value = []
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.brandId = undefined
  query.tagIds = []
  query.status = ''
  query.dateRange = null
  query.pageNum = 1
  load()
}

/** 列表中的会社名 */
function brandLabel(row: Game) {
  if (row.brandName) return row.brandName
  const brand = brands.value.find((b) => b.brandId === row.brandId)
  return brand ? brand.name : '—'
}

/* ==================== 封面临行修改（裁剪 + WebP） ==================== */
const coverDialog = reactive({
  visible: false,
  gameId: 0,
  title: '',
  cropSrc: '',
  uploading: false,
})
const coverInputRef = ref<HTMLInputElement>()
const cropperRef = ref<{ toFile: (name: string) => Promise<File | null> } | null>(null)

function openCover(row: Game) {
  revokeCoverSrc()
  coverDialog.visible = true
  coverDialog.gameId = row.gameId
  coverDialog.title = row.titleCn || row.title
  coverDialog.cropSrc = ''
  coverDialog.uploading = false
}

function revokeCoverSrc() {
  if (coverDialog.cropSrc) {
    URL.revokeObjectURL(coverDialog.cropSrc)
    coverDialog.cropSrc = ''
  }
}

function pickCover() {
  coverInputRef.value?.click()
}

function onCoverChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('原图大小不能超过 20MB')
    return
  }
  revokeCoverSrc()
  coverDialog.cropSrc = URL.createObjectURL(file)
}

async function submitCover() {
  const cropper = cropperRef.value
  if (!cropper) {
    ElMessage.warning('请先选择图片')
    return
  }
  coverDialog.uploading = true
  try {
    const file = await cropper.toFile('game-cover')
    if (!file) {
      ElMessage.error('裁剪失败，请重新选择图片')
      return
    }
    const uploaded = await uploadFile(file)
    const path = uploaded.fileName || uploaded.url
    await updateGame({ gameId: coverDialog.gameId, cover: path })
    ElMessage.success('封面已更新')
    coverDialog.visible = false
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '封面上传失败')
  } finally {
    coverDialog.uploading = false
  }
}

function onCoverClosed() {
  revokeCoverSrc()
}

/* ==================== 操作 ==================== */
function goEdit(row?: Game) {
  router.push({
    path: '/operation/game/edit',
    query: row ? { id: String(row.gameId) } : undefined,
  })
}

async function handlePublish(row: Game) {
  try {
    await ElMessageBox.confirm(
      `确认提交「${row.titleCn || row.title}」进入审核吗？审核通过后才会在前台展示。`,
      '提交发布',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await publishGame(row.gameId)
    ElMessage.success('已提交审核')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '提交失败')
  }
}

/** 撤回审核：待审核 → 草稿 */
async function handleWithdraw(row: Game) {
  try {
    await ElMessageBox.confirm(
      `确认撤回「${row.titleCn || row.title}」的审核吗？撤回后状态回到草稿。`,
      '撤回审核',
      { type: 'warning', confirmButtonText: '确认撤回' },
    )
  } catch {
    return
  }
  try {
    await withdrawGame(row.gameId)
    ElMessage.success('已撤回为草稿')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '撤回失败')
  }
}

/** 下架：已发布 → 已下架 */
async function handleOffline(row: Game) {
  try {
    await ElMessageBox.confirm(
      `确认下架「${row.titleCn || row.title}」吗？下架后前台立即不可见。`,
      '下架确认',
      { type: 'warning', confirmButtonText: '确认下架' },
    )
  } catch {
    return
  }
  try {
    await offlineGame(row.gameId)
    ElMessage.success('已下架')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '下架失败')
  }
}

async function handleDelete(rows?: Game[]) {
  const targets = rows ?? selection.value
  if (!targets.length) {
    ElMessage.warning('请先选择要删除的游戏')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 个游戏吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteGames(targets.map((g) => g.gameId).join(','))
    ElMessage.success('删除成功')
    load()
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(() => {
  load()
  loadOptions()
})
</script>

<template>
  <div class="page">
    <!-- 查询条件 -->
    <div class="toolbar table-card">
      <el-input
        v-model="query.keyword"
        placeholder="游戏原名 / 译名"
        clearable
        style="width: 190px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.brandId" placeholder="制作会社" clearable filterable style="width: 170px" @change="search">
        <el-option v-for="b in brands" :key="b.brandId" :label="b.name" :value="b.brandId" />
      </el-select>
      <el-select
        v-model="query.tagIds"
        placeholder="标签（可多选）"
        multiple
        collapse-tags
        collapse-tags-tooltip
        clearable
        filterable
        style="width: 210px"
        @change="search"
      >
        <el-option v-for="t in tags" :key="t.tagId" :label="t.name" :value="t.tagId" />
      </el-select>
      <el-date-picker
        v-model="query.dateRange"
        type="daterange"
        unlink-panels
        range-separator="至"
        start-placeholder="发售开始"
        end-placeholder="发售结束"
        value-format="YYYY-MM-DD"
        style="width: 250px"
        @change="search"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
        <el-option label="已发布" value="1" />
        <el-option label="待审核" value="2" />
        <el-option label="审核驳回" value="3" />
        <el-option label="草稿" value="4" />
        <el-option label="已下架" value="0" />
      </el-select>
      <el-button class="grad-btn" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <span class="spacer" />
      <el-button v-if="can('community:game:add')" type="primary" :icon="Plus" @click="goEdit()">新增</el-button>
      <el-button
        v-if="can('community:game:remove')"
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
      <el-table v-loading="loading" :data="list" border stripe @selection-change="(r: Game[]) => (selection = r)">
        <el-table-column type="selection" width="46" />
        <el-table-column label="封面" width="96" align="center">
          <template #default="{ row }">
            <div class="cover-cell" @click="can('community:game:edit') && openCover(row)">
              <el-image
                v-if="row.cover"
                :src="resolveAssetUrl(row.cover)"
                fit="cover"
                class="cover-img"
                :preview-src-list="[resolveAssetUrl(row.cover)]"
                preview-teleported
                @click.stop
              />
              <div v-else class="cover-empty">无封面</div>
              <span v-if="can('community:game:edit')" class="cover-mask">更换</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="游戏名称" min-width="200">
          <template #default="{ row }">
            <div class="game-title">{{ row.title }}</div>
            <div class="game-sub">{{ row.titleCn || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="制作会社" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ brandLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="170">
          <template #default="{ row }">
            <template v-if="row.tags && row.tags.length">
              <el-tag v-for="t in row.tags.slice(0, 3)" :key="t.tagId" size="small" effect="plain" class="tag-item">
                {{ t.name }}
              </el-tag>
              <el-tag v-if="row.tags.length > 3" size="small" type="info" effect="plain">+{{ row.tags.length - 3 }}</el-tag>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="发售日" width="120" align="center">
          <template #default="{ row }">{{ row.releaseDate || '—' }}</template>
        </el-table-column>
        <el-table-column label="评分" width="90" align="center">
          <template #default="{ row }">
            <span class="rating">{{ row.ratingAvg ?? '—' }}</span>
            <span class="muted"> / {{ row.ratingCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status ?? '']?.type ?? 'info'" size="small">
              {{ STATUS_MAP[row.status ?? '']?.label ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="165">
          <template #default="{ row }">{{ row.updateTime || row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="255" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('community:game:edit')" link type="primary" :icon="Edit" @click="goEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="can('community:game:publish')"
              link
              type="warning"
              :icon="Upload"
              :disabled="row.status === '2' || row.status === '1'"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="can('community:game:publish') && row.status === '2'"
              link
              type="warning"
              @click="handleWithdraw(row)"
            >
              撤回
            </el-button>
            <el-button
              v-if="can('community:game:publish') && row.status === '1'"
              link
              type="danger"
              @click="handleOffline(row)"
            >
              下架
            </el-button>
            <el-button
              v-if="can('community:game:remove')"
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

    <!-- 封面临行修改 -->
    <el-dialog
      v-model="coverDialog.visible"
      :title="`更换封面 · ${coverDialog.title}`"
      width="440px"
      append-to-body
      @closed="onCoverClosed"
    >
      <div class="cover-dialog">
        <template v-if="!coverDialog.cropSrc">
          <div class="cover-placeholder">点击下方按钮选择图片，裁剪后统一保存为 WebP（900×1200）</div>
          <el-button type="primary" @click="pickCover">选择图片</el-button>
        </template>
        <ImageCropper v-else ref="cropperRef" :src="coverDialog.cropSrc" :aspect="3 / 4" :out-width="900" />
        <input ref="coverInputRef" type="file" accept="image/*" hidden @change="onCoverChange" />
      </div>
      <template #footer>
        <el-button @click="coverDialog.visible = false">取消</el-button>
        <el-button v-if="coverDialog.cropSrc" @click="pickCover">重新选择</el-button>
        <el-button
          v-if="coverDialog.cropSrc"
          type="primary"
          :loading="coverDialog.uploading"
          @click="submitCover"
        >
          保存封面
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cover-cell {
  position: relative;
  width: 64px;
  height: 86px;
  margin: 0 auto;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  background: #f4f4fa;
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a0a0b8;
  font-size: 12px;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12.5px;
  opacity: 0;
  transition: opacity 0.15s;
}

.cover-cell:hover .cover-mask {
  opacity: 1;
}

.game-title {
  font-weight: 600;
  color: #2b2b3c;
}

.game-sub {
  color: #8a8aa3;
  font-size: 12.5px;
}

.tag-item {
  margin-right: 4px;
}

.muted {
  color: #a0a0b8;
}

.rating {
  color: #e6a23c;
  font-weight: 600;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.cover-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-height: 160px;
  justify-content: center;
}

.cover-placeholder {
  color: #8a8aa3;
  font-size: 13px;
  text-align: center;
  line-height: 1.7;
}
</style>
