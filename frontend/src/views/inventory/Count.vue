<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="盘点单号" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load">
          <el-option v-for="s in STATUSES" :key="s" :value="s"><StatusTag :value="s" /></el-option>
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="formVisible = true"><el-icon><Plus /></el-icon>新建盘点</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="code" label="盘点单号" width="190" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="80" />
        <el-table-column prop="zoneCode" label="库区" width="80"><template #default="{ row }">{{ row.zoneCode || '全仓' }}</template></el-table-column>
        <el-table-column prop="lineCount" label="明细数" width="80" />
        <el-table-column prop="diffCount" label="差异数" width="80" />
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column prop="createdAt" label="创建时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openLines(row)">{{ ['NEW', 'COUNTING'].includes(row.status) ? '录入盘点' : '查看' }}</el-button>
            <el-button v-if="row.status === 'COUNTED'" link type="success" size="small" @click="post(row)">过账</el-button>
            <el-popconfirm v-if="['NEW', 'COUNTING', 'COUNTED'].includes(row.status)" title="确认取消该盘点单?" @confirm="cancel(row)">
              <template #reference><el-button link type="danger" size="small">取消</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" title="新建盘点单" width="460px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库" required><el-select v-model="form.warehouseCode" style="width: 100%"><el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="库区"><el-select v-model="form.zoneCode" clearable placeholder="不选则全仓盘点" style="width: 100%"><el-option v-for="o in zones" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <el-alert type="info" :closable="false">系统将按当前库存快照生成盘点明细（盲盘时请勿参考系统数量）。</el-alert>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">生成盘点单</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="linesVisible" :title="`盘点单 ${current.code}`" size="65%">
      <el-table :data="lines" size="small" border>
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column prop="ownerCode" label="货主" width="90" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="120" />
        <el-table-column prop="systemQty" label="账面数量" width="100" />
        <el-table-column label="实盘数量" width="160">
          <template #default="{ row }">
            <el-input-number v-if="editable" v-model="row.countQty" :min="0" size="small" style="width: 100%" />
            <span v-else>{{ row.countQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="90">
          <template #default="{ row }">
            <span :style="{ color: diff(row) === 0 ? '' : diff(row) > 0 ? '#67c23a' : '#f56c6c' }">{{ row.countQty == null ? '-' : diff(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
      </el-table>
      <div v-if="editable" style="margin-top: 12px; text-align: right">
        <el-button @click="fillSystem">按账面填充</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交盘点结果</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inventory } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const STATUSES = ['NEW', 'COUNTING', 'COUNTED', 'POSTED', 'CANCELLED']
const { options } = useOptions(['warehouse', 'zone'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })
const formVisible = ref(false)
const form = ref({})
const linesVisible = ref(false)
const current = ref({})
const lines = ref([])

const zones = computed(() => (options.value.zone || []).filter((z) => z.warehouseCode === form.value.warehouseCode))
const editable = computed(() => ['NEW', 'COUNTING'].includes(current.value.status))
const diff = (row) => (row.countQty == null ? 0 : row.countQty - row.systemQty)

async function load() {
  loading.value = true
  try {
    const p = await inventory.countPage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!form.value.warehouseCode) return ElMessage.warning('请选择仓库')
  saving.value = true
  try {
    const o = await inventory.countCreate(form.value)
    ElMessage.success(`已生成盘点单 ${o.code}，共 ${o.lineCount} 条明细`)
    formVisible.value = false
    form.value = {}
    load()
  } finally {
    saving.value = false
  }
}

async function openLines(row) {
  current.value = row
  lines.value = await inventory.countLines(row.id)
  linesVisible.value = true
}

function fillSystem() {
  lines.value.forEach((l) => { if (l.countQty == null) l.countQty = l.systemQty })
}

async function submit() {
  const pending = lines.value.filter((l) => l.countQty == null)
  if (pending.length) return ElMessage.warning(`还有 ${pending.length} 行未录入实盘数量`)
  saving.value = true
  try {
    const counts = {}
    lines.value.forEach((l) => { counts[l.id] = l.countQty })
    await inventory.countSubmit(current.value.id, counts)
    ElMessage.success('盘点结果已提交')
    linesVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function post(row) {
  await inventory.countPost(row.id)
  ElMessage.success('盘点已过账，库存已按实盘调整')
  load()
}
async function cancel(row) {
  await inventory.countCancel(row.id)
  ElMessage.success('已取消')
  load()
}

onMounted(load)
</script>
