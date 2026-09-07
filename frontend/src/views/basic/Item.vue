<template>
  <CrudPage title="物料" :api="basic.item" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions(['owner'])
const columns = [
  { prop: 'ownerCode', label: '货主', type: 'select', options: 'owner', required: true, filter: true, readonlyOnEdit: true, width: 110 },
  { prop: 'code', label: '物料编码', required: true, readonlyOnEdit: true, width: 110 },
  { prop: 'name', label: '物料名称', required: true, minWidth: 160 },
  { prop: 'spec', label: '规格' },
  { prop: 'unit', label: '单位', width: 70, default: 'EA' },
  { prop: 'packQty', label: '箱规', type: 'number', width: 70 },
  { prop: 'barcode', label: '条码', width: 130 },
  { prop: 'category', label: '分类', width: 100 },
  { prop: 'lotControl', label: '批次管理', type: 'bool', width: 90, default: false },
  { prop: 'shelfLifeDays', label: '保质期(天)', type: 'number', width: 90 },
  { prop: 'abcClass', label: 'ABC', type: 'select', width: 60, options: [{ label: 'A', value: 'A' }, { label: 'B', value: 'B' }, { label: 'C', value: 'C' }] },
  { prop: 'weight', label: '重量(kg)', type: 'number', precision: 3, hideInTable: true },
  { prop: 'volume', label: '体积(m³)', type: 'number', precision: 4, hideInTable: true },
  { prop: 'minStock', label: '安全库存', type: 'number', width: 90 },
  { prop: 'maxStock', label: '最大库存', type: 'number', hideInTable: true },
  statusCol
]
</script>
