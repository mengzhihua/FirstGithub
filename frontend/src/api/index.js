import http from './request'

export const crud = (base) => ({
  page: (params) => http.get(`${base}/page`, { params }),
  list: (params) => http.get(`${base}/list`, { params }),
  get: (id) => http.get(`${base}/${id}`),
  create: (data) => http.post(base, data),
  update: (id, data) => http.put(`${base}/${id}`, data),
  remove: (id) => http.delete(`${base}/${id}`)
})

export const basic = {
  warehouse: crud('/basic/warehouse'),
  zone: crud('/basic/zone'),
  location: crud('/basic/location'),
  owner: crud('/basic/owner'),
  item: crud('/basic/item'),
  supplier: crud('/basic/supplier'),
  customer: crud('/basic/customer')
}

export const inbound = {
  page: (params) => http.get('/inbound/asn/page', { params }),
  get: (id) => http.get(`/inbound/asn/${id}`),
  create: (data) => http.post('/inbound/asn', data),
  update: (id, data) => http.put(`/inbound/asn/${id}`, data),
  cancel: (id) => http.post(`/inbound/asn/${id}/cancel`),
  receive: (id, lines) => http.post(`/inbound/asn/${id}/receive`, lines),
  closeReceiving: (id) => http.post(`/inbound/asn/${id}/close-receiving`),
  tasks: (id) => http.get(`/inbound/asn/${id}/tasks`),
  putawayPage: (params) => http.get('/inbound/putaway/page', { params }),
  putaway: (taskId, toLocation) => http.post(`/inbound/putaway/${taskId}/confirm`, { toLocation })
}

export const outbound = {
  page: (params) => http.get('/outbound/order/page', { params }),
  get: (id) => http.get(`/outbound/order/${id}`),
  create: (data) => http.post('/outbound/order', data),
  update: (id, data) => http.put(`/outbound/order/${id}`, data),
  allocate: (id) => http.post(`/outbound/order/${id}/allocate`),
  deallocate: (id) => http.post(`/outbound/order/${id}/deallocate`),
  ship: (id) => http.post(`/outbound/order/${id}/ship`),
  cancel: (id) => http.post(`/outbound/order/${id}/cancel`),
  tasks: (id) => http.get(`/outbound/order/${id}/tasks`),
  pickPage: (params) => http.get('/outbound/pick/page', { params }),
  pick: (taskId, qty) => http.post(`/outbound/pick/${taskId}/confirm`, { qty })
}

export const inventory = {
  page: (params) => http.get('/inventory/page', { params }),
  summary: () => http.get('/inventory/summary'),
  txnPage: (params) => http.get('/inventory/txn/page', { params }),
  move: (data) => http.post('/inventory/move', data),
  adjust: (data) => http.post('/inventory/adjust', data),
  freeze: (data) => http.post('/inventory/freeze', data),
  countPage: (params) => http.get('/inventory/count/page', { params }),
  countLines: (id) => http.get(`/inventory/count/${id}/lines`),
  countCreate: (data) => http.post('/inventory/count', data),
  countSubmit: (id, counts) => http.post(`/inventory/count/${id}/submit`, counts),
  countPost: (id) => http.post(`/inventory/count/${id}/post`),
  countCancel: (id) => http.post(`/inventory/count/${id}/cancel`)
}

export const dashboard = () => http.get('/dashboard')
