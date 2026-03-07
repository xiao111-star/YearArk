import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import router from '@/router'

const TOKEN_KEY = 'ya-auth-token'
const ANON_TOKEN_KEY_PREFIX = 'ya-anon-token:'

/**
 * 获取匿名 token 的 localStorage key
 * 按 inviteCode 隔离，避免多个纪念册的 token 互相覆盖
 */
export function getAnonTokenKey(inviteCode: string): string {
  return `${ANON_TOKEN_KEY_PREFIX}${inviteCode}`
}

/**
 * 获取当前活跃的匿名 token（最近一次 verify 设置的）
 */
let activeAnonToken: string | null = null

export function setActiveAnonToken(token: string | null) {
  activeAnonToken = token
}

export function getActiveAnonToken(): string | null {
  return activeAnonToken
}

const request = axios.create({
  baseURL: '/',
  timeout: 30000,
})

// 请求拦截：自动添加认证 header
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 已登录用户 token（需要加 Bearer 前缀，与 Sa-Token 全局 token-prefix 配置一致）
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers['Ya-Auth'] = `Bearer ${token}`
    }

    // 匿名用户 token（按 inviteCode 隔离）
    const anonToken = getActiveAnonToken()
    if (anonToken) {
      config.headers['Ya-Anon-Auth'] = `Bearer ${anonToken}`
    }

    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// 响应拦截：统一错误处理
request.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError<{ msg?: string; message?: string }>) => {
    const status = error.response?.status
    const message = error.response?.data?.msg || error.response?.data?.message || '请求失败'

    if (status === 401) {
      // 清除 token 并跳转登录
      localStorage.removeItem(TOKEN_KEY)
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if (status === 400) {
      console.warn('[API 400]', message)
    }

    return Promise.reject(error)
  },
)

export default request
