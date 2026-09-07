<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="入库单号 / 外部单号" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load">
          <el-option v-for="s in STATUSES" :key="s" :value="s"><StatusTag :value="s" /></el-option>
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="openForm()"><el-icon><Plus /></el-icon>新建入库单</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="code" label="入库单号" width="200" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="类型" width="90"><template #default="{ row }"><StatusTag :value="row.type" /></template></el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="80" />
        <el-table-column prop="ownerCode" label="货主" width="90" />
        <el-table-column prop="supplierCode" label="供应商" width="90" />
        <el-table-column prop="externalNo" label="外部单号" width="120" />
        <el-table-column prop="expectedDate" label="预计到货" width="110" />
        <el-table-column prop="totalQty" label="预期数量" width="90" />
        <el-table-column prop="receivedQty" label="已收货" width="80" />
        <el-table-column prop="putawayQty" label="已上架" width="80" />
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-if="['NEW', 'RECEIVING'].includes(row.status)" link type="success" size="small" @click="openReceive(row)">收货</el-button>
            <el-button v-if="row.status === 'RECEIVING'" link type="warning" size="small" @click="closeReceiving(row)">关闭收货</el-button>
            <el-popconfirm v-if="row.status === 'NEW'" title="确认取消该入库单?" @confirm="cancel(row)">
              <template #reference><el-button link type="danger" size="small">取消</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <!-- 新建/编辑 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑入库单' : '新建入库单'" width="900px" destroy-on-close>
      <el-form :model="form" label-width="90px" :inline="true">
        <el-form-item label="仓库" required><el-select v-model="form.warehouseCode" style="width: 180px"><el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="货主" required><el-select v-model="form.ownerCode" style="width: 180px" @change="form.lines = []"><el-option v-for="o in options.owner" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="供应商"><el-select v-model="form.supplierCode" clearable style="width: 180px"><el-option v-for="o in options.supplier" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.type" style="width: 180px"><el-option label="采购入库" value="PURCHASE" /><el-option label="退货入库" value="RETURN" /><el-option label="调拨入库" value="TRANSFER" /></el-select></el-form-item>
        <el-form-item label="预计到货"><el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 180px" /></el-form-item>
        <el-form-item label="外部单号"><el-input v-model="form.externalNo" style="width: 180px" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" style="width: 400px" /></el-form-item>
      </el-form>
      <el-divider content-position="left">明细</el-divider>
      <el-table :data="form.lines" size="small" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column label="物料" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.itemCode" filterable style="width: 100%">
              <el-option v-for="o in ownerItems" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="150"><template #default="{ row }"><el-input v-model="row.lotNo" /></template></el-table-column>
        <el-table-column label="效期" width="160"><template #default="{ row }"><el-date-picker v-model="row.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></template></el-table-column>
        <el-table-column label="预期数量" width="140"><template #default="{ row }"><el-input-number v-model="row.expectedQty" :min="0" style="width: 100%" /></template></el-table-column>
        <el-table-column label="备注" width="140"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
        <el-table-column width="60"><template #default="{ $index }"><el-button link type="danger" @click="form.lines.splice($index, 1)">删除</el-button></template></el-table-column>
      </el-table>
      <el-button style="margin-top: 8px" @click="form.lines.push({ expectedQty: 1 })"><el-icon><Plus /></el-icon>添加明细</el-button>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 收货 -->
    <el-dialog v-model="receiveVisible" :title="`收货 - ${current.code}`" width="900px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom: 8px">收货后库存进入收货暂存区并自动生成上架任务；批次管理物料必须填写批次号。</el-alert>
      <el-table :data="receiveLines" size="small" border>
        <el-table-column prop="itemCode" label="物料" width="110" />
        <el-table-column prop="expectedQty" label="预期" width="70" />
        <el-table-column prop="receivedQty" label="已收" width="70" />
        <el-table-column label="本次收货" width="140"><template #default="{ row }"><el-input-number v-model="row.qty" :min="0" style="width: 100%" /></template></el-table-column>
        <el-table-column label="批次号" width="150"><template #default="{ row }"><el-input v-model="row.lotNo" /></template></el-table-column>
        <el-table-column label="效期" width="160"><template #default="{ row }"><el-date-picker v-model="row.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></template></el-table-column>
        <el-table-column label="收货库位" min-width="140">
          <template #default="{ row }">
            <el-select v-model="row.locationCode" clearable placeholder="默认收货暂存区" style="width: 100%">
              <el-option v-for="o in stagingLocations" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="receiveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doReceive">确认收货</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="`入库单 ${current.code}`" size="60%">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="状态"><StatusTag :value="current.status" /></el-descriptions-item>
        <el-descriptions-item label="仓库">{{ current.warehouseCode }}</el-descriptions-item>
        <el-descriptions-item label="货主">{{ current.ownerCode }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ current.supplierCode }}</el-descriptions-item>
        <el-descriptions-item label="预计到货">{{ current.expectedDate }}</el-descriptions-item>
        <el-descriptions-item label="外部单号">{{ current.externalNo }}</el-descriptions-item>
      </el-descriptions>
      <h4>明细</h4>
      <el-table :data="current.lines || []" size="small" border>
        <el-table-column prop="lineNo" label="#" width="45" />
        <el-table-column prop="itemCode" label="物料" />
        <el-table-column prop="lotNo" label="批次" />
        <el-table-column prop="expiryDate" label="效期" width="110" />
        <el-table-column prop="expectedQty" label="预期" width="80" />
        <el-table-column prop="receivedQty" label="已收" width="80" />
        <el-table-column prop="putawayQty" label="已上架" width="80" />
      </el-table>
      <h4>上架任务</h4>
      <el-table :data="tasks" size="small" border>
        <el-table-column prop="code" label="任务号" width="190" />
        <el-table-column prop="itemCode" label="物料" />
        <el-table-column prop="lotNo" label="批次" />
        <el-table-column prop="qty" label="数量" width="70" />
        <el-table-column prop="fromLocation" label="源库位" />
        <el-table-column prop="suggestLocation" label="推荐库位" />
        <el-table-column prop="toLocation" label="实际库位" />
        <el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inbound } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'

const STATUSES = ['NEW', 'RECEIVING', 'RECEIVED', 'PUTAWAY', 'CLOSED', 'CANCELLED']
const { options } = useOptions(['warehouse', 'owner', 'supplier', 'item', 'location'])

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })

const formVisible = ref(false)
const form = ref({ lines: [] })
const receiveVisible = ref(false)
const receiveLines = ref([])
const detailVisible = ref(false)
const current = ref({})
const tasks = ref([])

const ownerItems = computed(() => (options.value.item || []).filter((i) => i.ownerCode === form.value.ownerCode))
const stagingLocations = computed(() => (options.value.location || []).filter((l) => l.warehouseCode === current.value.warehouseCode && ['STAGING_IN', 'QC'].includes(l.type)))

async function load() {
  loading.value = true
  try {
    const p = await inbound.page(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

async function openForm(row) {
  form.value = row ? await inbound.get(row.id) : { type: 'PURCHASE', lines: [{ expectedQty: 1 }] }
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await inbound.update(form.value.id, form.value)
    else await inbound.create(form.value)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openReceive(row) {
  current.value = await inbound.get(row.id)
  receiveLines.value = current.value.lines.map((l) => ({
    lineId: l.id, itemCode: l.itemCode, expectedQty: l.expectedQty, receivedQty: l.receivedQty,
    qty: Math.max(0, l.expectedQty - l.receivedQty), lotNo: l.lotNo, expiryDate: l.expiryDate, locationCode: ''
  }))
  receiveVisible.value = true
}

async function doReceive() {
  const lines = receiveLines.value.filter((l) => l.qty > 0)
  if (!lines.length) return ElMessage.warning('请输入收货数量')
  saving.value = true
  try {
    await inbound.receive(current.value.id, lines)
    ElMessage.success('收货成功，已生成上架任务')
    receiveVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function closeReceiving(row) {
  await inbound.closeReceiving(row.id)
  ElMessage.success('已关闭收货')
  load()
}

async function cancel(row) {
  await inbound.cancel(row.id)
  ElMessage.success('已取消')
  load()
}

async function openDetail(row) {
  current.value = await inbound.get(row.id)
  tasks.value = await inbound.tasks(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>
