<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.warehouseCode" placeholder="仓库" clearable @change="load"><el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" /></el-select>
        <el-select v-model="query.ownerCode" placeholder="货主" clearable @change="load"><el-option v-for="o in options.owner" :key="o.value" :label="o.label" :value="o.value" /></el-select>
        <el-input v-model="query.locationCode" placeholder="库位" clearable @keyup.enter="load" @clear="load" style="width: 140px" />
        <el-input v-model="query.itemCode" placeholder="物料" clearable @keyup.enter="load" @clear="load" style="width: 140px" />
        <el-input v-model="query.lotNo" placeholder="批次" clearable @keyup.enter="load" @clear="load" style="width: 140px" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load" style="width: 120px">
          <el-option value="AVAILABLE" label="可用" /><el-option value="FROZEN" label="冻结" />
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="warehouseCode" label="仓库" width="80" />
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column prop="ownerCode" label="货主" width="90" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="120" />
        <el-table-column prop="qty" label="库存量" width="90" />
        <el-table-column prop="allocatedQty" label="已分配" width="90" />
        <el-table-column label="可用量" width="90"><template #default="{ row }">{{ row.status === 'AVAILABLE' ? row.qty - row.allocatedQty : 0 }}</template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="receiveDate" label="收货日期" width="110" />
        <el-table-column prop="expiryDate" label="效期" width="110" />
        <el-table-column prop="refNo" label="来源单号" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :disabled="row.status !== 'AVAILABLE'" @click="openMove(row)">移库</el-button>
            <el-button link type="warning" size="small" @click="openAdjust(row)">调整</el-button>
            <el-button link :type="row.status === 'FROZEN' ? 'success' : 'danger'" size="small" @click="openFreeze(row)">{{ row.status === 'FROZEN' ? '解冻' : '冻结' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg.visible" :title="dlg.title" width="460px">
      <el-form label-width="90px">
        <el-form-item label="库存">{{ cur.locationCode }} / {{ cur.itemCode }} / {{ cur.lotNo || '-' }} / 库存 {{ cur.qty }} 已分配 {{ cur.allocatedQty }}</el-form-item>
        <template v-if="dlg.mode === 'move'">
          <el-form-item label="移库数量"><el-input-number v-model="req.qty" :min="0" :max="cur.qty - cur.allocatedQty" style="width: 100%" /></el-form-item>
          <el-form-item label="目标库位">
            <el-select v-model="req.toLocation" filterable style="width: 100%">
              <el-option v-for="o in targetLocations" :key="o.value" :label="`${o.label} (${o.type})`" :value="o.value" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else-if="dlg.mode === 'adjust'">
          <el-form-item label="调整后数量"><el-input-number v-model="req.newQty" :min="0" style="width: 100%" /></el-form-item>
          <el-form-item label="调整原因"><el-input v-model="req.reason" placeholder="如：盘亏/破损/系统纠错" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="原因"><el-input v-model="req.reason" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inventory } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'

const { options } = useOptions(['warehouse', 'owner', 'location'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, warehouseCode: '', ownerCode: '', locationCode: '', itemCode: '', lotNo: '', status: '' })
const dlg = reactive({ visible: false, mode: '', title: '' })
const cur = ref({})
const req = ref({})

const targetLocations = computed(() => (options.value.location || []).filter((l) => l.warehouseCode === cur.value.warehouseCode && l.value !== cur.value.locationCode))

async function load() {
  loading.value = true
  try {
    const p = await inventory.page(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

function openMove(row) {
  cur.value = row
  req.value = { inventoryId: row.id, qty: row.qty - row.allocatedQty, toLocation: '' }
  Object.assign(dlg, { visible: true, mode: 'move', title: '库存移库' })
}
function openAdjust(row) {
  cur.value = row
  req.value = { inventoryId: row.id, newQty: row.qty, reason: '' }
  Object.assign(dlg, { visible: true, mode: 'adjust', title: '库存调整' })
}
function openFreeze(row) {
  cur.value = row
  req.value = { inventoryId: row.id, frozen: row.status !== 'FROZEN', reason: '' }
  Object.assign(dlg, { visible: true, mode: 'freeze', title: row.status === 'FROZEN' ? '库存解冻' : '库存冻结' })
}

async function submit() {
  saving.value = true
  try {
    if (dlg.mode === 'move') {
      if (!req.value.toLocation) return ElMessage.warning('请选择目标库位')
      await inventory.move(req.value)
    } else if (dlg.mode === 'adjust') await inventory.adjust(req.value)
    else await inventory.freeze(req.value)
    ElMessage.success('操作成功')
    dlg.visible = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
