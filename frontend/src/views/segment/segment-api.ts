import { apiClient } from '@/utils/api-client'

const BASE_URL = '/api/segment'

export interface SegmentCardDto {
  id: string
  name: string
  code: string
  wealthLevel: number
  customerType: number
  [key: string]: any
}

export interface SegmentMatrixData {
  count: number[][]
  clusterCount: number[][]
  matrixRows?: string[]
  matrixCols?: string[]
  allClusterCards?: SegmentCardDto[]
  clusterCards?: SegmentCardDto[]
}

export const getSegmentList = () => {
  return apiClient.post(`${BASE_URL}/overview`)
}

export const getSegmentDetail = (segmentCode: string) => {
  return apiClient.post(`${BASE_URL}/${encodeURIComponent(segmentCode)}`)
}
