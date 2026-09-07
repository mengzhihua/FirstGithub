<template>
  <CrudPage title="库区" :api="basic.zone" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions(['warehouse'])
const columns = [
  { prop: 'warehouseCode', label: '仓库', type: 'select', options: 'warehouse', required: true, filter: true, width: 140 },
  { prop: 'code', label: '库区编码', required: true, readonlyOnEdit: true, width: 120 },
  { prop: 'name', label: '库区名称', required: true, minWidth: 160 },
  { prop: 'type', label: '库区类型', type: 'select', required: true, filter: true, width: 120, options: [
    { label: '收货区', value: 'RECEIVING' }, { label: '存储区', value: 'STORAGE' }, { label: '拣货区', value: 'PICKING' },
    { label: '发货区', value: 'SHIPPING' }, { label: '质检区', value: 'QC' }, { label: '残次区', value: 'DAMAGE' }
  ] },
  statusCol
]
</script>
