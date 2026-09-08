/**
 * 客群 360 畫像 - 資料傳輸模型（對齊後端 SegmentDetailDto.java）
 * 
 *   ?: = 可以不傳
 */


//  --------------------1. 客群分類總覽（九宮格矩陣與清單）
export interface SegmentListResponse {
  matrixRows: string[]
  matrixCols: string[]
  matrixData: SegmentMatrixData
  clusterCards: SegmentCardDto[]
}

export interface SegmentMatrixData {
  count: number[][]
  clusterCount: number[][]
}

export interface SegmentCardDto {
  code: string
  name: string
  count: number
  growth: string
  avgAum: string
  highlight: string
  wealthLevel: number
  customerType: number
}
/**
const rawData: SegmentListResponse = {
    matrixRows: ["高資產", "中資產", "一般資產"],
    matrixCols: ["高活躍", "中活躍", "低活躍"],
    matrixData: {
      count: [
        [120, 45, 15],
        [350, 890, 210],
        [80, 1500, 3200]
      ],
      clusterCount: [
        [2, 1, 0],
        [1, 3, 1],
        [0, 2, 4]
      ]
    },
    clusterCards: [
      {
        code: "SEG-001",
        name: "尊榮頂級 VIP",
        count: 120,
        growth: "+5.2%",
        avgAum: "$15,200,000",
        highlight: "主力偏好高收益債券與海外基金",
        wealthLevel: 1,
        customerType: 1
      },
      {
        code: "SEG-002",
        name: "潛力數位新星",
        count: 890,
        growth: "+12.8%",
        avgAum: "$1,850,000",
        highlight: "App 使用頻率極高，偏好定期定額 ETF",
        wealthLevel: 2,
        customerType: 2
      }
    ]
  }
    
 */




// --------------------2. 核心共用元件
export interface Comparison<T> {
  label?: string
  overall: T
  t10: T
}

/** 
const productComparison: Comparison<string> = {
  label: "主力偏好產品",
  overall: "台股定額 ETF",
  t10: "海外美債"
}

const loanRatioComparison: Comparison<number> = {
  label: "貸存比",
  overall: 65.5,
  t10: 88.2
}

const loanRatioComparison: Comparison<string[]> = {
  label: "熱門偏好標籤",
  overall: ["儲蓄險", "台股 ETF"],
  t10: ["海外債券", "私募基金", "家族信託"]
}
*/


export interface ScoreMetric<T = number> {
  label: string
  score: number
  value: T
  displayValue: string
}
/**
// 寫法 A：不傳泛型參數（預設 T = number，value 必須是數字）
const annualIncomeMetric: ScoreMetric = {
  label: "年收入",
  score: 85,
  value: 1200000,
  displayValue: "$1,200,000"
}

// 寫法 B：自訂 T = string（value 變成字串）
const levelMetric: ScoreMetric<string> = {
  label: "會員等級",
  score: 95,
  value: "DIAMOND",
  displayValue: "鑽石級會員"
}
 */


export interface IntentMetric {
  label: string
  ratio: number
  displayRatio: string
}

export interface ConsumeMetric {
  label: string
  ratio: number
  amount: number
  displayAmount: string
}


//  --------------------3. 客群 360 畫像詳情 DTO
export interface SegmentDetailDto {
  code: string
  name: string
  count: number
  managementInsight: string

  // 12 項核心指標
  totalCustomerCount: number
  potentialCustomerCount: number
  avgAge: number
  homeOwnerCount: number
  avgAnnualIncome: number
  avgAnnualContribution: number
  avgAUM: number
  avgLUM: number
  pureDepositRatio: number
  inactiveRatio: number
  creditRiskLevel: string
  investmentRiskType: string

  // 圖表結構
  aumSegment: RadialSegmentData
  lumSegment: RadialSegmentData
  memberDistribution: MemberManagementData
  productBehavior: ProductBehaviorData

  // 雙層對比雷達圖結構
  wealthPotential: Comparison<WealthRadarSeries>
  productIntentAsset: Comparison<ProductIntentAssetSeries>
  productIntentLiability: Comparison<ProductIntentLiabilitySeries>
  accountDepth: Comparison<AccountDepthSeries>
  consumeBehavior: ConsumeBehaviorData // 改為對齊後端的專屬結構
  channelBehavior: Comparison<ChannelBehaviorSeries>
  digitalBehavior: Comparison<DigitalBehaviorSeries>
}

//  --------------------4. 子結構定義
export interface RadialSegmentData {
  ring1: RingData
  ring2: RingData
  categories: ChartCategory[]
}

export interface RingData {
  key: string
  label: string
  name: string
  total: number
  values: number[]
}

export interface ChartCategory {
  label: string
  color?: string
  ratioList: string[]
  amountList: number[]
}

export interface MemberManagementData {
  potentialMemberCount: number
  distributions: MemberDistribution[]
}

export interface MemberDistribution {
  level: string
  atRiskCount: number
  maintenanceCount: number
  growthCount: number
}

export interface WealthRadarSeries {
  annualIncome: ScoreMetric<number>
  taxes: ScoreMetric<number>
  creditLimit: ScoreMetric<number>
  annualSpending: ScoreMetric<number>
  realEstate: {
    label: string
    score: number
    tpMetroAmount: number
    nonTpMetroAmount: number
    tooltipText: string
  }
  dividend: ScoreMetric<number>
}



export interface ProductBehaviorData {
  faCoverageRatio: number
  wmAumTurnoverRatio: number
  investmentYieldRate: number
  loanDepositRatio: Comparison<number>
  activeDepositRatio: Comparison<number>
  wealthAumRatio: Comparison<number>
}


//  --------------------3.3.10 消費行為專屬完整結構（含偏好標籤）
export interface ConsumeBehaviorData {
  label?: string
  overall: ConsumeBehaviorSeries
  t10: ConsumeBehaviorSeries
  preferenceTags: string[]
}

export interface ConsumeBehaviorSeries {
  livingBills: ConsumeMetric
  foodDining: ConsumeMetric
  onlineShopping: ConsumeMetric
  ict: ConsumeMetric
  insurance: ConsumeMetric
  app: ConsumeMetric
  leisure: ConsumeMetric
  department: ConsumeMetric
  other: ConsumeMetric
}



//  --------------------IntentMetric
export interface ProductIntentAssetSeries {
  bondFund: IntentMetric
  equityFund: IntentMetric
  balancedFund: IntentMetric
  savingsInsurance: IntentMetric
  protectionInsurance: IntentMetric
  foreignBond: IntentMetric
}

export interface ProductIntentLiabilitySeries {
  fxUsd: IntentMetric
  fxJpy: IntentMetric
  creditLoan: IntentMetric
  mortgageRevolving: IntentMetric
  sb: IntentMetric
  ccBillInstallment: IntentMetric
  ccPurchaseInstallment: IntentMetric
}
/**
 const productIntentLiabilityData: ProductIntentLiabilitySeries = {
  fxUsd: {
    label: "美元外幣意圖",
    ratio: 0.45,
    displayRatio: "45%"
  },
  fxJpy: {
    label: "日圓外幣意圖",
    ratio: 0.30,
    displayRatio: "30%"
  },
  creditLoan: {
    label: "信用貸款意圖",
    ratio: 0.08,
    displayRatio: "8%"
  },
  mortgageRevolving: {
    label: "房屋增貸意圖",
    ratio: 0.02,
    displayRatio: "2%"
  },
  sb: {
    label: "複委託交易意圖",
    ratio: 0.10,
    displayRatio: "10%"
  },
  ccBillInstallment: {
    label: "帳單分期意圖",
    ratio: 0.03,
    displayRatio: "3%"
  },
  ccPurchaseInstallment: {
    label: "消費特店分期意圖",
    ratio: 0.02,
    displayRatio: "2%"
  }
}
 */


//  --------------------ScoreMetric
export interface AccountDepthSeries {
  mortgageRepayRatio: ScoreMetric<number>
  creditLoanRepayRatio: ScoreMetric<number>
  netInflow: ScoreMetric<number>
  utilityAutoDebit: ScoreMetric<number>
  securitiesSettlement: ScoreMetric<number>
}

export interface ChannelBehaviorSeries {
  branchTrans: ScoreMetric<number>
  atmTrans: ScoreMetric<number>
  customerService: ScoreMetric<number>
  ebanking: ScoreMetric<number>
  pib: ScoreMetric<number>
  wallet: ScoreMetric<number>
  lineResponse: ScoreMetric<number>
}

export interface DigitalBehaviorSeries {
  activityResponse: ScoreMetric<number>
  onlineLogin: ScoreMetric<number>
  onlineTrans: ScoreMetric<number>
  digitalRevenue: ScoreMetric<number>
  lineBinding: ScoreMetric<number>
  appBinding: ScoreMetric<number>
}
/**
const accountDepthData: AccountDepthSeries = {
  mortgageRepayRatio: {
    label: "房屋貸款扣繳比例",
    score: 85,
    value: 0.85,
    displayValue: "85%"
  },
  creditLoanRepayRatio: {
    label: "信用貸款扣繳比例",
    score: 0,
    value: 0,
    displayValue: "0%"
  },
  netInflow: {
    label: "近半年資金淨流入",
    score: 78,
    value: 250000,
    displayValue: "+$250,000"
  },
  utilityAutoDebit: {
    label: "公用事業扣繳項目",
    score: 100,
    value: 4,
    displayValue: "4 項已綁定"
  },
  securitiesSettlement: {
    label: "證券交割戶簽約狀況",
    score: 90,
    value: 1,
    displayValue: "主要交割戶"
  }
}
 */