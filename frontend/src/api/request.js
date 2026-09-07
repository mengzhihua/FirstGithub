import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

/** Errors are already shown via ElMessage; callers only need the rejection to abort their flow. */
function reported(err) {
  err.reported = true
  return Promise.reject(err)
}
window.addEventListener('unhandledrejection', (e) => {
  if (e.reason && e.reason.reported) e.preventDefault()
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== undefined && body.code !== 0) {
      ElMessage.error(body.msg || '请求失败')
      return reported(new Error(body.msg))
    }
    return body ? body.data : body
  },
  (err) => {
    const msg = err.response?.data?.msg || err.message || '网络错误'
    ElMessage.error(msg)
    return reported(err)
  }
)

export default http
