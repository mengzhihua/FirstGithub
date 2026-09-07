<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="关键字搜索" clearable @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <template v-for="c in filterCols" :key="c.prop">
          <el-select v-model="query[c.prop]" :placeholder="c.label" clearable @change="load">
            <el-option v-for="o in optionsOf(c)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </template>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="openForm()"><el-icon><Plus /></el-icon>新增{{ title }}</el-button>
        <slot name="toolbar" />
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column v-for="c in tableCols" :key="c.prop" :prop="c.prop" :label="c.label" :width="c.width" :min-width="c.minWidth || 100" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="c.type === 'switch' || c.type === 'bool'">
              <el-tag :type="row[c.prop] ? 'success' : 'info'" size="small">{{ row[c.prop] ? (c.trueText || '是') : (c.falseText || '否') }}</el-tag>
            </template>
            <template v-else-if="c.prop === 'status' && c.type === 'status'">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
            <template v-else-if="c.type === 'select'">{{ labelOf(c, row[c.prop]) }}</template>
            <template v-else>{{ row[c.prop] }}</template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="remove(row)">
              <template #reference><el-button link type="danger" size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50, 100]" @change="load" />
      </div>
    </div>

    <el-dialog v-model="visible" :title="(form.id ? '编辑' : '新增') + title" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col v-for="c in formCols" :key="c.prop" :span="c.span || 12">
            <el-form-item :label="c.label" :prop="c.prop">
              <el-select v-if="c.type === 'select'" v-model="form[c.prop]" filterable clearable style="width: 100%" :disabled="!!form.id && c.readonlyOnEdit">
                <el-option v-for="o in optionsOf(c)" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-input-number v-else-if="c.type === 'number'" v-model="form[c.prop]" :min="c.min ?? 0" :precision="c.precision ?? 0" style="width: 100%" />
              <el-switch v-else-if="c.type === 'switch' || c.type === 'bool'" v-model="form[c.prop]" />
              <el-switch v-else-if="c.type === 'status'" v-model="form[c.prop]" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
              <el-input v-else v-model="form[c.prop]" :disabled="!!form.id && c.readonlyOnEdit" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: { type: String, required: true },
  api: { type: Object, required: true },
  columns: { type: Array, required: true },
  /** map of option-source name -> array of {label,value} (for select columns using `options: 'name'`) */
  optionSources: { type: Object, default: () => ({}) }
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const formRef = ref()
const form = ref({})
const query = reactive({ current: 1, size: 20, keyword: '' })

const tableCols = computed(() => props.columns.filter((c) => !c.hideInTable))
const formCols = computed(() => props.columns.filter((c) => !c.hideInForm))
const filterCols = computed(() => props.columns.filter((c) => c.filter))

const rules = computed(() => {
  const r = {}
  props.columns.filter((c) => c.required).forEach((c) => { r[c.prop] = [{ required: true, message: `${c.label}不能为空`, trigger: 'blur' }] })
  return r
})

const optionsOf = (c) => (typeof c.options === 'string' ? props.optionSources[c.options] || [] : c.options || [])
const labelOf = (c, v) => optionsOf(c).find((o) => o.value === v)?.label ?? v

async function load() {
  loading.value = true
  try {
    const p = await props.api.page(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  const defaults = {}
  props.columns.forEach((c) => { if (c.default !== undefined) defaults[c.prop] = c.default })
  form.value = row ? { ...row } : defaults
  visible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.id) await props.api.update(form.value.id, form.value)
    else await props.api.create(form.value)
    ElMessage.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await props.api.remove(row.id)
  ElMessage.success('已删除')
  load()
}

watch(() => props.optionSources, () => {}, { deep: true })
onMounted(load)
defineExpose({ load })
</script>
