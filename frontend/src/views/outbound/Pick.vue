<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="任务号 / 出库单号 / 物料" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load">
          <el-option value="NEW" label="待拣货" /><el-option value="DONE" label="已完成" /><el-option value="CANCELLED" label="已取消" />
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="code" label="任务号" width="190" />
        <el-table-column prop="orderCode" label="出库单号" width="190" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="120" />
        <el-table-column prop="fromLocation" label="拣货库位" width="120" />
        <el-table-column prop="toLocation" label="目标库位" width="110" />
        <el-table-column prop="qty" label="应拣" width="80" />
        <el-table-column prop="pickedQty" label="实拣" width="80" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="open(row)">拣货确认</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="visible" title="拣货确认" width="440px">
      <el-form label-width="90px">
        <el-form-item label="物料">{{ current.itemCode }} / 批次 {{ current.lotNo || '-' }}</el-form-item>
        <el-form-item label="拣货库位">{{ current.fromLocation }} → {{ current.toLocation }}</el-form-item>
        <el-form-item label="实拣数量"><el-input-number v-model="qty" :min="0" :max="current.qty" style="width: 100%" /></el-form-item>
      </el-form>
      <el-alert v-if="qty < current.qty" type="warning" :closable="false">少拣 {{ current.qty - qty }}，差异数量将释放分配。</el-alert>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirm">确认拣货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { outbound } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const current = ref({})
const qty = ref(0)
const query = reactive({ current: 1, size: 20, keyword: '', status: 'NEW' })

async function load() {
  loading.value = true
  try {
    const p = await outbound.pickPage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

function open(row) {
  current.value = row
  qty.value = row.qty
  visible.value = true
}

async function confirm() {
  if (qty.value <= 0) return ElMessage.warning('拣货数量必须大于0')
  saving.value = true
  try {
    await outbound.pick(current.value.id, qty.value)
    ElMessage.success('拣货完成')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
