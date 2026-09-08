/**
 * 基礎圖表資料結構
 */
export interface SimpleChartData {
  /** 名稱 */
  label: string;
  /** 值 */
  value: string;
}

/**
 * 雷達圖節點資料結構
 */
export interface RadarChartData {
  /** 等級 */
  rank: number;
  /** 指標資訊 */
  info: string;
}

/**
 * 帶有高亮（亮點）特性的雷達圖節點資料結構
 */
export interface HighlightRadarChartData extends RadarChartData {
  /** 亮點顯示 */
  highlight: boolean;
}

/**
 * 圓環圖/圓餅圖的分類數據結構
 */
export interface CategoryData {
  ratiolist: string[];
  amountlist: number[];
}

/**
 * 標準雷達圖檢視資料結構
 */
export interface RadarChartViewData {
  /** 指標名稱 */
  labels: string[];
  /** 顧客資料 */
  customerData: RadarChartData[];
  /** 客群資料 */
  segmentData: RadarChartData[];
}

/**
 * 高亮（亮點）雷達圖檢視資料結構
 */
export interface HighlightRadarChartViewData {
  /** 指標名稱 */
  labels: string[];
  /** 顧客現況 */
  customerData: HighlightRadarChartData[];
  // 註：依結構通常也會有 segmentData，可依後端需求保留擴充
}

// 補充於相應的 Bean 檔案中，用於對接圖中的進階金融資料模型
export interface AuMData {
  demandDeposit: CategoryData
  timeDeposit: CategoryData
  fund: CategoryData
  financialProduct: CategoryData
  insurance: CategoryData
  financialAsset: CategoryData
}

export interface AuM {
  customerTotal: number
  segmentTotal: number
  topTenTotal: number
  aumData: AuMData
}

export interface LuMData {
  mtgPurchase: CategoryData
  mtgRevolve: CategoryData
  collateral: CategoryData
  creditLoan: CategoryData
  finAssetLoan: CategoryData
  ccBalance: CategoryData
}

export interface LuM {
  customerTotal: number
  segmentTotal: number
  topTenTotal: number
  lumData: LuMData
}

export interface CustomerDto {
  id: string
  aum?: AuM
  lum?: LuM
  wealthPotential?: any
}

export interface SecureCustomerViewDto {
  viewMode: 'single' | 'multiple'
  selectedCustomer: {
    customerId: string
    customerName: string
  } | null
}
