import axios from 'axios'

// 基礎路徑（可根據你的 Vite 代理設定調整）
const BASE_URL = '/api/segment'

// 封裝 Axios 實例（對應圖中的 apiClient）
const apiClient = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 定義 TypeScript 介面（對應圖中的 Dto 與 Response）
export interface SegmentListResponse {
  // 根據你的後端資料結構定義，例如：
  list: any[];
  total: number;
}

export interface SegmentDetailDto {
  // 根據你的後端資料結構定義，例如：
  id: string;
  name: string;
  description: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/** 
 * 1. 查詢九宮格總覽與客群清單 
 */
export const getSegmentList = async (): Promise<ApiResponse<SegmentListResponse>> => {
  const response = await apiClient.post<ApiResponse<SegmentListResponse>>(`${BASE_URL}/overview`)
  return response.data
}

/** 
 * 2. 查詢單一客群 360 畫像詳情 
 */
export const getSegmentDetail = async (segmentCode: string): Promise<ApiResponse<SegmentDetailDto>> => {
  const response = await apiClient.post<ApiResponse<SegmentDetailDto>>(
    `${BASE_URL}/${encodeURIComponent(segmentCode)}`
  )
  return response.data
}
