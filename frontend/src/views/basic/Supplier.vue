<template>
  <CrudPage title="供应商" :api="basic.supplier" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions([])
const columns = [
  { prop: 'code', label: '编码', required: true, readonlyOnEdit: true, width: 120 },
  { prop: 'name', label: '名称', required: true, minWidth: 180 },
  { prop: 'contact', label: '联系人' },
  { prop: 'phone', label: '电话' },
  { prop: 'address', label: '地址', span: 24, minWidth: 200 },
  statusCol
]
</script>
