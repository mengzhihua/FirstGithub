<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="波次号" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load">
          <el-option v-for="s in STATUSES" :key="s" :value="s"><StatusTag :value="s" /></el-option>
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>创建波次</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="code" label="波次号" width="200" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="80" />
        <el-table-column prop="orderCount" label="出库单数" width="90" />
        <el-table-column prop="totalQty" label="应拣" width="80" />
        <el-table-column prop="pickedQty" label="实拣" width="80" />
        <el-table-column prop="sowedQty" label="已播种" width="80" />
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情/作业</el-button>
            <el-button v-if="row.status === 'SOWED'" link type="success" size="small" @click="ship(row)">发运</el-button>
            <el-popconfirm v-if="row.status === 'NEW'" title="取消波次，拣货任务回到普通拣货池?" @confirm="cancel(row)">
              <template #reference><el-button link type="danger" size="small">取消</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <!-- 创建波次 -->
    <el-dialog v-model="createVisible" title="创建波次" width="900px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom: 10px">
        选择同一仓库、已分配(ALLOCATED / PART_ALLOCATED)且未加入波次的出库单，系统按库位+物料+批次合并生成总拣任务；总拣完成后按出库单播种。
      </el-alert>
      <div class="toolbar">
        <el-select v-model="createWarehouse" placeholder="仓库" @change="loadCandidates" style="width: 160px">
          <el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input v-model="createRemark" placeholder="备注" style="width: 300px" />
      </div>
      <el-table ref="candidateTable" :data="candidates" size="small" border max-height="360" @selection-change="(v) => (selected = v)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="code" label="出库单号" width="200" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="customerCode" label="客户" width="90" />
        <el-table-column prop="priority" label="优先级" width="70" />
        <el-table-column prop="totalQty" label="订单数量" width="90" />
        <el-table-column prop="allocatedQty" label="已分配" width="80" />
        <el-table-column prop="expectedShipDate" label="预计发运" width="110" />
      </el-table>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selected.length" @click="create">生成波次 ({{ selected.length }} 单)</el-button>
      </template>
    </el-dialog>

    <!-- 详情 / 作业 -->
    <el-drawer v-model="detailVisible" :title="`波次 ${current.code || ''}`" size="70%">
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="状态"><StatusTag :value="current.status" /></el-descriptions-item>
        <el-descriptions-item label="仓库">{{ current.warehouseCode }}</el-descriptions-item>
        <el-descriptions-item label="出库单数">{{ current.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="应拣/实拣/已播种">{{ current.totalQty }} / {{ current.pickedQty }} / {{ current.sowedQty }}</el-descriptions-item>
      </el-descriptions>

      <el-steps :active="stepIndex" finish-status="success" simple style="margin: 14px 0">
        <el-step title="创建波次" />
        <el-step title="总拣" />
        <el-step title="播种" />
        <el-step title="发运" />
      </el-steps>

      <h4>出库单 (播种位 = 序号)</h4>
      <el-table :data="current.orders || []" size="small" border>
        <el-table-column type="index" label="播种位" width="70" />
        <el-table-column prop="code" label="出库单号" width="200" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="customerCode" label="客户" width="90" />
        <el-table-column prop="priority" label="优先级" width="70" />
        <el-table-column prop="totalQty" label="订单" width="80" />
        <el-table-column prop="allocatedQty" label="已分配" width="80" />
        <el-table-column prop="pickedQty" label="已拣" width="80" />
        <el-table-column prop="shippedQty" label="已发运" width="80" />
      </el-table>

      <h4>总拣任务 (按库位 / 物料 / 批次汇总)</h4>
      <el-table :data="current.pickTasks || []" size="small" border>
        <el-table-column prop="code" label="任务号" width="190" />
        <el-table-column prop="fromLocation" label="拣货库位" width="110" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="110" />
        <el-table-column prop="orderCount" label="涉及单数" width="80" />
        <el-table-column prop="qty" label="应拣" width="80" />
        <el-table-column prop="pickedQty" label="实拣" width="80" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="openPick(row)">总拣确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <h4>播种任务 (分拨到各出库单播种位)</h4>
      <el-table :data="current.sowTasks || []" size="small" border>
        <el-table-column prop="slotNo" label="播种位" width="70" />
        <el-table-column prop="orderCode" label="出库单号" width="200" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="110" />
        <el-table-column prop="qty" label="应播" width="80" />
        <el-table-column prop="sowedQty" label="已播" width="80" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="sow(row)">播种确认</el-button>
            <el-button v-if="row.status === 'NEW'" link size="small" @click="openSow(row)">部分播种</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="current.status === 'SOWED'" style="margin-top: 14px; text-align: right">
        <el-button type="success" :loading="saving" @click="ship(current)">波次发运</el-button>
      </div>
    </el-drawer>

    <el-dialog v-model="pickVisible" title="总拣确认" width="440px">
      <el-form label-width="90px">
        <el-form-item label="物料">{{ pickTask.itemCode }} / 批次 {{ pickTask.lotNo || '-' }}</el-form-item>
        <el-form-item label="拣货库位">{{ pickTask.fromLocation }} → {{ pickTask.toLocation }}</el-form-item>
        <el-form-item label="实拣数量"><el-input-number v-model="pickQty" :min="0" :max="pickTask.qty" style="width: 100%" /></el-form-item>
      </el-form>
      <el-alert v-if="pickQty < pickTask.qty" type="warning" :closable="false">少拣 {{ pickTask.qty - pickQty }}，按出库单优先级依次满足，缺口释放分配。</el-alert>
      <template #footer>
        <el-button @click="pickVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmPick">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sowVisible" title="播种确认" width="400px">
      <el-form label-width="90px">
        <el-form-item label="播种位">{{ sowTask.slotNo }} / {{ sowTask.orderCode }}</el-form-item>
        <el-form-item label="物料">{{ sowTask.itemCode }}</el-form-item>
        <el-form-item label="本次数量"><el-input-number v-model="sowQty" :min="1" :max="sowTask.qty - (sowTask.sowedQty || 0)" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sowVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSow">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { outbound } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { useOptions } from '../../composables/useOptions'
import { fmt } from '../../utils'

const STATUSES = ['NEW', 'PICKING', 'SOWING', 'SOWED', 'SHIPPED', 'CANCELLED']
const STEP = { NEW: 1, PICKING: 1, SOWING: 2, SOWED: 3, SHIPPED: 4, CANCELLED: 0 }

const { options } = useOptions(['warehouse'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })

const createVisible = ref(false)
const createWarehouse = ref('')
const createRemark = ref('')
const candidates = ref([])
const selected = ref([])

const detailVisible = ref(false)
const current = ref({})
const stepIndex = computed(() => STEP[current.value.status] ?? 0)

const pickVisible = ref(false)
const pickTask = ref({})
const pickQty = ref(0)
const sowVisible = ref(false)
const sowTask = ref({})
const sowQty = ref(0)

async function load() {
  loading.value = true
  try {
    const p = await outbound.wavePage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  createVisible.value = true
  selected.value = []
  createRemark.value = ''
  if (!createWarehouse.value && options.value.warehouse?.length) createWarehouse.value = options.value.warehouse[0].value
  await loadCandidates()
}

async function loadCandidates() {
  const [a, b] = await Promise.all([
    outbound.page({ current: 1, size: 200, status: 'ALLOCATED', warehouseCode: createWarehouse.value }),
    outbound.page({ current: 1, size: 200, status: 'PART_ALLOCATED', warehouseCode: createWarehouse.value })
  ])
  candidates.value = [...a.records, ...b.records].sort((x, y) => (x.priority ?? 5) - (y.priority ?? 5) || x.id - y.id)
}

async function create() {
  saving.value = true
  try {
    const w = await outbound.waveCreate({ warehouseCode: createWarehouse.value, orderIds: selected.value.map((o) => o.id), remark: createRemark.value })
    ElMessage.success(`波次 ${w.code} 已创建，生成 ${w.pickTasks.length} 个总拣任务`)
    createVisible.value = false
    await load()
    current.value = w
    detailVisible.value = true
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  current.value = await outbound.waveGet(row.id)
  detailVisible.value = true
}

function openPick(row) {
  pickTask.value = row
  pickQty.value = row.qty
  pickVisible.value = true
}

async function confirmPick() {
  saving.value = true
  try {
    current.value = await outbound.wavePick(pickTask.value.id, pickQty.value)
    ElMessage.success(current.value.status === 'SOWING' ? '总拣完成，已生成播种任务' : '总拣确认成功')
    pickVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function sow(row) {
  saving.value = true
  try {
    current.value = await outbound.waveSow(row.id, null)
    ElMessage.success(current.value.status === 'SOWED' ? '播种全部完成，可发运' : '播种确认成功')
    await load()
  } finally {
    saving.value = false
  }
}

function openSow(row) {
  sowTask.value = row
  sowQty.value = row.qty - (row.sowedQty || 0)
  sowVisible.value = true
}

async function confirmSow() {
  saving.value = true
  try {
    current.value = await outbound.waveSow(sowTask.value.id, sowQty.value)
    sowVisible.value = false
    ElMessage.success('播种确认成功')
    await load()
  } finally {
    saving.value = false
  }
}

async function ship(row) {
  saving.value = true
  try {
    current.value = await outbound.waveShip(row.id)
    ElMessage.success('波次发运完成')
    await load()
  } finally {
    saving.value = false
  }
}

async function cancel(row) {
  await outbound.waveCancel(row.id)
  ElMessage.success('波次已取消')
  await load()
}

onMounted(load)
</script>
