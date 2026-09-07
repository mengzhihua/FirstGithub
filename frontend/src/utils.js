export const fmt = (v) => (v ? String(v).replace('T', ' ').substring(0, 19) : '')
export const today = () => new Date().toISOString().substring(0, 10)
