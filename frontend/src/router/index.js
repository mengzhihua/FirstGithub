import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

export const menus = [
  { path: '/dashboard', name: '工作台', icon: 'Odometer', component: () => import('../views/Dashboard.vue') },
  {
    path: '/basic', name: '基础数据', icon: 'Setting',
    children: [
      { path: 'warehouse', name: '仓库', component: () => import('../views/basic/Warehouse.vue') },
      { path: 'zone', name: '库区', component: () => import('../views/basic/Zone.vue') },
      { path: 'location', name: '库位', component: () => import('../views/basic/Location.vue') },
      { path: 'owner', name: '货主', component: () => import('../views/basic/Owner.vue') },
      { path: 'item', name: '物料', component: () => import('../views/basic/Item.vue') },
      { path: 'supplier', name: '供应商', component: () => import('../views/basic/Supplier.vue') },
      { path: 'customer', name: '客户', component: () => import('../views/basic/Customer.vue') }
    ]
  },
  {
    path: '/inbound', name: '入库管理', icon: 'Download',
    children: [
      { path: 'asn', name: '入库单(ASN)', component: () => import('../views/inbound/Asn.vue') },
      { path: 'putaway', name: '上架任务', component: () => import('../views/inbound/Putaway.vue') }
    ]
  },
  {
    path: '/outbound', name: '出库管理', icon: 'Upload',
    children: [
      { path: 'order', name: '出库单', component: () => import('../views/outbound/ShipOrder.vue') },
      { path: 'pick', name: '拣货任务', component: () => import('../views/outbound/Pick.vue') }
    ]
  },
  {
    path: '/inventory', name: '库内管理', icon: 'Box',
    children: [
      { path: 'stock', name: '库存查询', component: () => import('../views/inventory/Stock.vue') },
      { path: 'summary', name: '库存汇总', component: () => import('../views/inventory/Summary.vue') },
      { path: 'count', name: '盘点管理', component: () => import('../views/inventory/Count.vue') },
      { path: 'txn', name: '库存流水', component: () => import('../views/inventory/Txn.vue') }
    ]
  }
]

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: menus.flatMap((m) =>
      m.children
        ? m.children.map((c) => ({ path: `${m.path}/${c.path}`, name: c.name, component: c.component }))
        : [{ path: m.path, name: m.name, component: m.component }]
    )
  }
]

export default createRouter({ history: createWebHistory(), routes })
