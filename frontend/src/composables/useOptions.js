import { ref } from 'vue'
import { basic } from '../api'

/** Loads master-data drop-down options once per page. */
export function useOptions(kinds) {
  const options = ref({})
  const loaders = {
    warehouse: async () => (await basic.warehouse.list()).map((w) => ({ label: `${w.code} ${w.name}`, value: w.code })),
    zone: async () => (await basic.zone.list()).map((z) => ({ label: `${z.code} ${z.name}`, value: z.code, warehouseCode: z.warehouseCode })),
    location: async () => (await basic.location.list()).map((l) => ({ label: l.code, value: l.code, type: l.type, warehouseCode: l.warehouseCode })),
    owner: async () => (await basic.owner.list()).map((o) => ({ label: `${o.code} ${o.name}`, value: o.code })),
    item: async () => (await basic.item.list()).map((i) => ({ label: `${i.code} ${i.name}`, value: i.code, ownerCode: i.ownerCode, lotControl: i.lotControl })),
    supplier: async () => (await basic.supplier.list()).map((s) => ({ label: `${s.code} ${s.name}`, value: s.code })),
    customer: async () => (await basic.customer.list()).map((c) => ({ label: `${c.code} ${c.name}`, value: c.code }))
  }
  async function reload() {
    const out = {}
    await Promise.all(kinds.map(async (k) => { out[k] = await loaders[k]() }))
    options.value = out
  }
  reload()
  return { options, reload }
}

export const statusCol = { prop: 'status', label: '状态', type: 'status', width: 80, default: 1 }
