<template>
  <el-tag :type="meta.type" size="small" effect="light">{{ meta.text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const DICT = {
  NEW: ['新建', 'info'],
  RECEIVING: ['收货中', 'warning'],
  RECEIVED: ['已收货', ''],
  PUTAWAY: ['上架中', 'warning'],
  CLOSED: ['已完成', 'success'],
  CANCELLED: ['已取消', 'danger'],
  ALLOCATED: ['已分配', ''],
  PART_ALLOCATED: ['部分分配', 'warning'],
  PICKING: ['拣货中', 'warning'],
  PICKED: ['已拣货', ''],
  SHIPPED: ['已发运', 'success'],
  DONE: ['已完成', 'success'],
  AVAILABLE: ['可用', 'success'],
  FROZEN: ['冻结', 'danger'],
  DISABLED: ['禁用', 'info'],
  COUNTING: ['盘点中', 'warning'],
  COUNTED: ['已盘点', ''],
  POSTED: ['已过账', 'success'],
  PENDING: ['待盘', 'info'],
  RECEIVE: ['收货', 'success'],
  STAGE: ['暂存', ''],
  PICK: ['拣货', 'warning'],
  SHIP: ['发运', 'danger'],
  MOVE: ['移库', ''],
  ADJUST: ['调整', 'warning'],
  FREEZE: ['冻结', 'danger'],
  UNFREEZE: ['解冻', 'success'],
  ALLOCATE: ['分配', 'info'],
  RELEASE: ['释放', 'info'],
  PURCHASE: ['采购入库', ''],
  RETURN: ['退货', 'warning'],
  TRANSFER: ['调拨', 'info'],
  SALES: ['销售出库', '']
}

const props = defineProps({ value: String })
const meta = computed(() => {
  const d = DICT[props.value]
  return d ? { text: d[0], type: d[1] } : { text: props.value, type: 'info' }
})
</script>
