<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="任务号 / 入库单号 / 物料" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable @change="load">
          <el-option value="NEW" label="待上架" /><el-option value="DONE" label="已完成" />
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="code" label="任务号" width="190" />
        <el-table-column prop="asnCode" label="入库单号" width="200" />
        <el-table-column prop="itemCode" label="物料" width="100" />
        <el-table-column prop="lotNo" label="批次" width="120" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="fromLocation" label="源库位" width="110" />
        <el-table-column prop="suggestLocation" label="推荐库位" width="120" />
        <el-table-column prop="toLocation" label="实际库位" width="120" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="open(row)">上架确认</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="visible" title="上架确认" width="480px">
      <el-form label-width="90px">
        <el-form-item label="物料">{{ current.itemCode }} / 批次 {{ current.lotNo || '-' }} / 数量 {{ current.qty }}</el-form-item>
        <el-form-item label="推荐库位">{{ current.suggestLocation || '无（请手工指定）' }}</el-form-item>
        <el-form-item label="上架库位">
          <el-select v-model="toLocation" filterable style="width: 100%">
            <el-option v-for="o in storageLocations" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirm">确认上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inbound } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const { options } = useOptions(['location'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const current = ref({})
const toLocation = ref('')
const query = reactive({ current: 1, size: 20, keyword: '', status: 'NEW' })

const storageLocations = computed(() => (options.value.location || []).filter((l) => l.warehouseCode === current.value.warehouseCode && ['STORAGE', 'PICKING'].includes(l.type)))

async function load() {
  loading.value = true
  try {
    const p = await inbound.putawayPage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

function open(row) {
  current.value = row
  toLocation.value = row.suggestLocation || ''
  visible.value = true
}

async function confirm() {
  if (!toLocation.value) return ElMessage.warning('请选择上架库位')
  saving.value = true
  try {
    await inbound.putaway(current.value.id, toLocation.value)
    ElMessage.success('上架完成')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
