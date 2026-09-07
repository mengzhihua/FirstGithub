<template>
  <CrudPage title="仓库" :api="basic.warehouse" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions([])
const columns = [
  { prop: 'code', label: '仓库编码', required: true, readonlyOnEdit: true, width: 120 },
  { prop: 'name', label: '仓库名称', required: true, minWidth: 160 },
  { prop: 'address', label: '地址', span: 24, minWidth: 200 },
  { prop: 'contact', label: '联系人' },
  { prop: 'phone', label: '电话' },
  statusCol
]
</script>
