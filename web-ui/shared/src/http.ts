import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'

/** localStorage 中保存 token 的键 */
export const TOKEN_KEY = 'token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 把后端返回的相对资源地址（封面图等）规整为可直接使用的地址 */
export function resolveAssetUrl(url?: string | null): string {
  if (!url) return ''
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  return url.startsWith('/') ? url : `/${url}`
}

const instance: AxiosInstance = axios.create({
  // 后端 context-path 为 `/`，真实接口路径即 /login、/community/...，开发环境由 Vite 代理转发
  baseURL: '',
  timeout: 30000,
})

// 请求拦截器：自动携带 Authorization: Bearer <token>
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一展开 response.data，并处理 AjaxResult 的非 200 分支
instance.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res && res.code !== 200) {
      return Promise.reject(new Error((res as { msg?: string }).msg || '请求失败'))
    }
    return res
  },
  (error) => {
    const status: number | undefined = error.response?.status
    if (status === 401) {
      removeToken()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    const msg: string = error.response?.data?.msg || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

/**
 * 类型安全的请求封装。
 * 注意：响应拦截器已把返回值展开为 response.data（AjaxResult / TableDataInfo / 原始对象），
 * 因此这里直接把 Promise 断言为目标类型。
 */
export const http = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config) as unknown as Promise<T>
  },
}

export default http
