<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="物料编码过滤" clearable style="width: 200px" />
        <el-button type="primary" @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
      <el-table :data="filtered" v-loading="loading" border stripe size="small" show-summary :summary-method="sum">
        <el-table-column prop="warehouseCode" label="仓库" width="100" />
        <el-table-column prop="ownerCode" label="货主" width="110" />
        <el-table-column prop="itemCode" label="物料" width="140" />
        <el-table-column prop="qty" label="库存总量" width="120" />
        <el-table-column prop="allocatedQty" label="已分配" width="120" />
        <el-table-column prop="availableQty" label="可用量" width="120" />
        <el-table-column prop="frozenQty" label="冻结量" width="120" />
        <el-table-column label="可用占比" min-width="200">
          <template #default="{ row }"><el-progress :percentage="row.qty ? Math.round((row.availableQty / row.qty) * 100) : 0" /></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { inventory } from '../../api'

const rows = ref([])
const keyword = ref('')
const loading = ref(false)
const NUM = ['qty', 'allocatedQty', 'availableQty', 'frozenQty']

const filtered = computed(() => rows.value.filter((r) => !keyword.value || String(r.itemCode).toUpperCase().includes(keyword.value.toUpperCase())))

/** H2/MySQL may return upper-case keys for aliased aggregate columns; normalise to camelCase. */
function normalize(r) {
  const out = {}
  for (const [k, v] of Object.entries(r)) {
    const key = { WAREHOUSECODE: 'warehouseCode', OWNERCODE: 'ownerCode', ITEMCODE: 'itemCode', QTY: 'qty', ALLOCATEDQTY: 'allocatedQty', AVAILABLEQTY: 'availableQty', FROZENQTY: 'frozenQty' }[k.toUpperCase()] || k
    out[key] = NUM.includes(key) ? Number(v) : v
  }
  return out
}

async function load() {
  loading.value = true
  try {
    rows.value = (await inventory.summary()).map(normalize)
  } finally {
    loading.value = false
  }
}

function sum({ columns, data }) {
  return columns.map((c, i) => (i === 0 ? '合计' : NUM.includes(c.property) ? data.reduce((a, r) => a + (r[c.property] || 0), 0) : ''))
}

onMounted(load)
</script>
