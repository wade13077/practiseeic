import axios, { type AxiosInstance, type AxiosResponse } from 'axios'

// 模擬 Pinia 錯誤彈窗狀態（對應圖中 useAlertStore）
const useAlertStore = () => ({
  showAlert: (msg: string) => alert(`[系統通知]: ${msg}`)
})

export interface ApiResponse<T = any> {
  success: boolean
  code: number
  message: string
  data: T
}

export class HandledError extends Error {
  handled = true
  constructor(message?: string) {
    super(message)
  }
}

function createHandledError(message: string): HandledError {
  const err = new HandledError(message)
  err.handled = true
  return err
}

const apiClient: AxiosInstance = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL as string) || '/api',
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

// 響應攔截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as ApiResponse
    if (body !== null && typeof body === 'object' && body.success === false) {
      const message = body.message || '系統內部錯誤'
      useAlertStore().showAlert(message)
      return Promise.reject(createHandledError(message))
    }
    return response
  },
  async (error) => {
    const status = error.response?.status
    if (status === 401) {
      window.location.href = '/login-error'
    } else if (status === 403) {
      window.location.href = '/forbidden'
    } else if (status >= 500) {
      window.location.href = '/internal-error'
    } else {
      const message = error.message || '系統內部錯誤'
      useAlertStore().showAlert(message)
      return Promise.reject(createHandledError(message))
    }
    throw error
  }
)

export { apiClient }
