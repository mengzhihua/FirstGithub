<template>
  <CrudPage title="库位" :api="basic.location" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions(['warehouse', 'zone'])
const columns = [
  { prop: 'warehouseCode', label: '仓库', type: 'select', options: 'warehouse', required: true, filter: true, width: 120 },
  { prop: 'zoneCode', label: '库区', type: 'select', options: 'zone', required: true, filter: true, width: 100 },
  { prop: 'code', label: '库位编码', required: true, readonlyOnEdit: true, width: 130 },
  { prop: 'type', label: '库位类型', type: 'select', required: true, filter: true, width: 110, options: [
    { label: '存储位', value: 'STORAGE' }, { label: '拣货位', value: 'PICKING' }, { label: '收货暂存', value: 'STAGING_IN' },
    { label: '发货暂存', value: 'STAGING_OUT' }, { label: '质检位', value: 'QC' }, { label: '残次位', value: 'DAMAGE' }
  ] },
  { prop: 'abcClass', label: 'ABC分类', type: 'select', width: 90, options: [{ label: 'A', value: 'A' }, { label: 'B', value: 'B' }, { label: 'C', value: 'C' }] },
  { prop: 'aisle', label: '巷道', width: 70 },
  { prop: 'bay', label: '列', width: 60 },
  { prop: 'level', label: '层', width: 60 },
  { prop: 'pickSeq', label: '拣货顺序', type: 'number', width: 90, default: 0 },
  { prop: 'maxWeight', label: '最大承重(kg)', type: 'number', precision: 3, hideInTable: true },
  { prop: 'maxVolume', label: '最大容积(m³)', type: 'number', precision: 3, hideInTable: true },
  { prop: 'mixSku', label: '允许混SKU', type: 'bool', width: 100, default: true },
  { prop: 'mixLot', label: '允许混批次', type: 'bool', width: 100, default: true },
  { prop: 'status', label: '状态', type: 'select', width: 90, default: 'AVAILABLE', filter: true, options: [
    { label: '可用', value: 'AVAILABLE' }, { label: '冻结', value: 'FROZEN' }, { label: '禁用', value: 'DISABLED' }
  ] }
]
</script>
