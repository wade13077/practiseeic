import { apiClient } from '@/utils/api-client'
import type { ApiResponse } from '@/beans/shared-bean'
import type { CustomerBasic, CustomerInfo, Deposit, Fund, InsuranceBlock, Interaction, Overview } from '@/beans/customer-bean'

/** 個人 360 L2 畫像 API 基準路徑 */
const BASE_URL = '/api/customer'

/**
 * 查詢個人 360 L2 顧客資訊摘要
 *
 * @param customerId 顧客識別流水號
 * @returns 顧客資訊摘要
 */
export const getCustomerSummary = (customerId: string) =>
  apiClient.get<ApiResponse<CustomerInfo>>(`${BASE_URL}/${customerId}/l2/summary`)

/**
 * 查詢個人 360 L2 基本資料
 *
 * @param customerId 顧客識別流水號
 * @returns 基本資料
 */
export const getCustomerBasic = (customerId: string) =>
  apiClient.get<ApiResponse<CustomerBasic>>(`${BASE_URL}/${customerId}/l2/basic`)

/**
 * 查詢個人 360 L2 互動紀錄
 *
 * @param customerId 顧客識別流水號
 * @returns 互動紀錄 DTO 陣列
 */
export const getCustomerInteractions = (customerId: string) =>
  apiClient.get<ApiResponse<Interaction[]>>(`${BASE_URL}/${customerId}/l2/interactions`)

/**
 * 查詢個人 360 L2 資產負債 - 總覽
 *
 * @param customerId 顧客識別流水號
 * @returns 總覽 DTO
 */
export const getAssetOverview = (customerId: string) =>
  apiClient.get<ApiResponse<Overview>>(`${BASE_URL}/${customerId}/l2/assets/overview`)

/**
 * 查詢個人 360 L2 資產負債 - 存款資料
 *
 * @param customerId 顧客識別流水號
 * @returns 存款資料 DTO
 */
export const getDeposits = (customerId: string) =>
  apiClient.get<ApiResponse<Deposit>>(`${BASE_URL}/${customerId}/l2/assets/deposits`)

/**
 * 查詢個人 360 L2 資產負債 - 基金/ETF/債券資料
 *
 * @param customerId 顧客識別流水號
 * @returns 基金/ETF/債券資料 DTO
 */
export const getFundsEtfBonds = (customerId: string) =>
  apiClient.get<ApiResponse<Fund>>(`${BASE_URL}/${customerId}/l2/assets/funds-etf-bonds`)

/**
 * 查詢個人 360 L2 資產負債 - 保險資料
 *
 * @param customerId 顧客識別流水號
 * @returns 保險資料 DTO
 */
export const getInsurance = (customerId: string) =>
  apiClient.get<ApiResponse<InsuranceBlock>>(`${BASE_URL}/${customerId}/l2/assets/insurance`)