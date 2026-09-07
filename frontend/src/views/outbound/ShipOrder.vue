<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="出库单号 / 外部单号 / 客户" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load">
          <el-option v-for="s in STATUSES" :key="s" :value="s"><StatusTag :value="s" /></el-option>
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="openForm()"><el-icon><Plus /></el-icon>新建出库单</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="code" label="出库单号" width="190" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="类型" width="90"><template #default="{ row }"><StatusTag :value="row.type" /></template></el-table-column>
        <el-table-column prop="priority" label="优先级" width="70" />
        <el-table-column prop="warehouseCode" label="仓库" width="80" />
        <el-table-column prop="ownerCode" label="货主" width="90" />
        <el-table-column prop="customerCode" label="客户" width="90" />
        <el-table-column prop="expectedShipDate" label="要求发货" width="110" />
        <el-table-column prop="carrier" label="承运商" width="100" />
        <el-table-column prop="totalQty" label="订单量" width="80" />
        <el-table-column prop="allocatedQty" label="已分配" width="80" />
        <el-table-column prop="pickedQty" label="已拣货" width="80" />
        <el-table-column prop="shippedQty" label="已发运" width="80" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-if="['NEW', 'PART_ALLOCATED'].includes(row.status)" link type="success" size="small" @click="act(outbound.allocate, row, '分配完成')">分配</el-button>
            <el-button v-if="['ALLOCATED', 'PART_ALLOCATED'].includes(row.status)" link type="warning" size="small" @click="act(outbound.deallocate, row, '已取消分配')">取消分配</el-button>
            <el-button v-if="row.status === 'PICKED'" link type="success" size="small" @click="act(outbound.ship, row, '发运完成')">发运</el-button>
            <el-popconfirm v-if="['NEW', 'ALLOCATED', 'PART_ALLOCATED'].includes(row.status)" title="确认取消该出库单?" @confirm="act(outbound.cancel, row, '已取消')">
              <template #reference><el-button link type="danger" size="small">取消</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑出库单' : '新建出库单'" width="900px" destroy-on-close>
      <el-form :model="form" label-width="90px" :inline="true">
        <el-form-item label="仓库" required><el-select v-model="form.warehouseCode" style="width: 180px"><el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="货主" required><el-select v-model="form.ownerCode" style="width: 180px" @change="form.lines = []"><el-option v-for="o in options.owner" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="客户"><el-select v-model="form.customerCode" clearable style="width: 180px"><el-option v-for="o in options.customer" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.type" style="width: 180px"><el-option label="销售出库" value="SALES" /><el-option label="调拨出库" value="TRANSFER" /><el-option label="退货出库" value="RETURN" /></el-select></el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="1" :max="9" style="width: 180px" /></el-form-item>
        <el-form-item label="要求发货"><el-date-picker v-model="form.expectedShipDate" type="date" value-format="YYYY-MM-DD" style="width: 180px" /></el-form-item>
        <el-form-item label="承运商"><el-input v-model="form.carrier" style="width: 180px" /></el-form-item>
        <el-form-item label="外部单号"><el-input v-model="form.externalNo" style="width: 180px" /></el-form-item>
        <el-form-item label="收货地址"><el-input v-model="form.address" style="width: 400px" /></el-form-item>
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
        <el-table-column label="指定批次(可选)" width="160"><template #default="{ row }"><el-input v-model="row.lotNo" /></template></el-table-column>
        <el-table-column label="订单数量" width="140"><template #default="{ row }"><el-input-number v-model="row.orderQty" :min="0" style="width: 100%" /></template></el-table-column>
        <el-table-column label="备注" width="160"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
        <el-table-column width="60"><template #default="{ $index }"><el-button link type="danger" @click="form.lines.splice($index, 1)">删除</el-button></template></el-table-column>
      </el-table>
      <el-button style="margin-top: 8px" @click="form.lines.push({ orderQty: 1 })"><el-icon><Plus /></el-icon>添加明细</el-button>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="`出库单 ${current.code}`" size="60%">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="状态"><StatusTag :value="current.status" /></el-descriptions-item>
        <el-descriptions-item label="仓库">{{ current.warehouseCode }}</el-descriptions-item>
        <el-descriptions-item label="货主">{{ current.ownerCode }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ current.customerCode }}</el-descriptions-item>
        <el-descriptions-item label="承运商">{{ current.carrier }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ current.address }}</el-descriptions-item>
      </el-descriptions>
      <h4>明细</h4>
      <el-table :data="current.lines || []" size="small" border>
        <el-table-column prop="lineNo" label="#" width="45" />
        <el-table-column prop="itemCode" label="物料" />
        <el-table-column prop="lotNo" label="指定批次" />
        <el-table-column prop="orderQty" label="订单" width="80" />
        <el-table-column prop="allocatedQty" label="已分配" width="80" />
        <el-table-column prop="pickedQty" label="已拣" width="80" />
        <el-table-column prop="shippedQty" label="已发" width="80" />
      </el-table>
      <h4>拣货任务</h4>
      <el-table :data="tasks" size="small" border>
        <el-table-column prop="code" label="任务号" width="190" />
        <el-table-column prop="itemCode" label="物料" />
        <el-table-column prop="lotNo" label="批次" />
        <el-table-column prop="fromLocation" label="拣货库位" />
        <el-table-column prop="toLocation" label="目标" />
        <el-table-column prop="qty" label="数量" width="70" />
        <el-table-column prop="pickedQty" label="已拣" width="70" />
        <el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { outbound } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'

const STATUSES = ['NEW', 'ALLOCATED', 'PART_ALLOCATED', 'PICKING', 'PICKED', 'SHIPPED', 'CANCELLED']
const { options } = useOptions(['warehouse', 'owner', 'customer', 'item'])

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })
const formVisible = ref(false)
const form = ref({ lines: [] })
const detailVisible = ref(false)
const current = ref({})
const tasks = ref([])

const ownerItems = computed(() => (options.value.item || []).filter((i) => i.ownerCode === form.value.ownerCode))

async function load() {
  loading.value = true
  try {
    const p = await outbound.page(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

async function openForm(row) {
  form.value = row ? await outbound.get(row.id) : { type: 'SALES', priority: 5, lines: [{ orderQty: 1 }] }
  formVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await outbound.update(form.value.id, form.value)
    else await outbound.create(form.value)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function act(fn, row, msg) {
  const res = await fn(row.id)
  if (res && res.status === 'PART_ALLOCATED') ElMessage.warning('库存不足，仅部分分配')
  else ElMessage.success(msg)
  load()
}

async function openDetail(row) {
  current.value = await outbound.get(row.id)
  tasks.value = await outbound.tasks(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>
