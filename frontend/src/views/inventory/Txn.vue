<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.txnType" placeholder="流水类型" clearable @change="load" style="width: 150px">
          <el-option v-for="t in TYPES" :key="t" :value="t"><StatusTag :value="t" /></el-option>
        </el-select>
        <el-input v-model="query.itemCode" placeholder="物料" clearable @keyup.enter="load" @clear="load" style="width: 150px" />
        <el-input v-model="query.refNo" placeholder="单号" clearable @keyup.enter="load" @clear="load" style="width: 200px" />
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="100"><template #default="{ row }"><StatusTag :value="row.txnType" /></template></el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="80" />
        <el-table-column prop="ownerCode" label="货主" width="90" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="120" />
        <el-table-column prop="fromLocation" label="源库位" width="110" />
        <el-table-column prop="toLocation" label="目标库位" width="110" />
        <el-table-column prop="qty" label="数量" width="90"><template #default="{ row }"><span :style="{ color: row.qty < 0 ? '#f56c6c' : '#67c23a' }">{{ row.qty }}</span></template></el-table-column>
        <el-table-column prop="refNo" label="单号" width="200" />
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column prop="createdAt" label="时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { inventory } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const TYPES = ['RECEIVE', 'PUTAWAY', 'PICK', 'STAGE', 'SHIP', 'MOVE', 'ADJUST', 'FREEZE', 'UNFREEZE', 'COUNT']
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, txnType: '', itemCode: '', refNo: '' })

async function load() {
  loading.value = true
  try {
    const p = await inventory.txnPage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
