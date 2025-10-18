import axios, { type AxiosInstance } from 'axios'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://3.21.177.186:8080').replace(
  /\/+$/,
  ''
)
const AUTH_BASE_URL = (import.meta.env.VITE_AUTH_BASE_URL || '').replace(/\/+$/, '')
const USER_BASE_URL = (import.meta.env.VITE_USER_BASE_URL || '').replace(/\/+$/, '')

export const publicApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

export const authApi = axios.create({
  baseURL: AUTH_BASE_URL || API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

export const userApi = axios.create({
  baseURL: USER_BASE_URL || API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

/** 공통: Authorization 붙이는 인터셉터 */
function attachAuth(instance: AxiosInstance) {
  instance.interceptors.request.use(cfg => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      cfg.headers = cfg.headers ?? {}
      if (!('Authorization' in cfg.headers)) {
        cfg.headers.Authorization = `Bearer ${token}`
      }
      console.log('🟢 Authorization 헤더 적용됨:', token.slice(0, 30) + '...')
      // 디버깅용
      // console.log('➡️', (cfg.method || 'GET').toUpperCase(), instance.getUri({ ...cfg, url: cfg.url || '' }))
      // console.log('   Authorization:', (cfg.headers.Authorization as string)?.slice(0, 25) + '...')
    }

    return cfg
  })
}

// 디버그 로그
authApi.interceptors.request.use(c => {
  console.log('🔐 AUTH →', c.method?.toUpperCase(), authApi.getUri({ ...c, url: c.url || '' }))
  return c
})
userApi.interceptors.request.use(c => {
  console.log('👤 USER →', c.method?.toUpperCase(), userApi.getUri({ ...c, url: c.url || '' }))
  return c
})
publicApi.interceptors.request.use(c => {
  console.log('🌐 PUBLIC →', c.method?.toUpperCase(), publicApi.getUri({ ...c, url: c.url || '' }))
  return c
})
;[api, authApi, userApi, publicApi].forEach(attachAuth)

export default api
