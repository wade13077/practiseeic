<script setup lang="ts">
import '@/assets/styles/customer/base.css'
import '@/assets/styles/customer/customer-advance.css'

import { computed, reactive, watch, ref } from 'vue'
import {
  getAssetOverview,
  getCustomerConsumption,
  getCustomerInteractions,
  getDeposits,
  getFundsEtfBonds,
  getInsurance,
  getGold,
  getWealth,
  getTrust,
  getLoan,
} from '@/views/management/customer/customer-l2-api.ts'

import type { CustomerAdvanceViewDto } from '@/beans/customer-advance-bean'
import type { ApiResponse } from '@/beans/shared-bean'
import type {
  Interaction,
  Overview,
  Deposit,
  Fund,
  InsuranceBlock,
  GoldBlock,
  Wealth,
  TrustBlock,
  LoanBlock,
  CustomerCredit,
} from '@/beans/customer-bean'
import RadarChart from '@/components/charts/radar.vue'
import Watermark from '@/components/shared/watermark.vue'
import { useAuthStore } from '@/stores/use-auth-store'
import { useAlertStore } from '@/stores/use-message-store'
import { isWatermarkEnabled } from '@/utils/app-profile'
import { isHandledError } from '@/utils/api-client'
import { dateFormat, formatAmount, profitAndLossFormat } from '@/utils/formatters'

const MAIN_TAB = {
  BASIC: 'basic',
  ASSETS: 'assets',
  CREDIT: 'credit',
  INTERACTION: 'interaction',
} as const

const ASSET_TAB = {
  OVERVIEW: 'stock-t',
  DEPOSIT: 'deposit',
  FUND: 'fund',
  INSURANCE: 'insurance',
  GOLD: 'gold',
  WEALTH: 'wealth',
  TRUST: 'trust',
  LOAN: 'loan',
} as const

type MainTab = (typeof MAIN_TAB)[keyof typeof MAIN_TAB]
type AssetTab = (typeof ASSET_TAB)[keyof typeof ASSET_TAB]

type Props = {
  data: CustomerAdvanceViewDto | null
  error: string | null
  initialTab?: MainTab | null
  /** 角色可視權限：L2 非理財頁籤是否可視（預設 true，避免未傳入時全部隱藏） */
  nonWealthTabVisible?: boolean
  /** 角色可視權限：L2 理財頁籤是否可視 */
  wealthTabVisible?: boolean
}

const props = defineProps<Props>()
const authStore = useAuthStore()
const alertStore = useAlertStore()

const emit = defineEmits<{
  back: []
}>()

/** 資產負債範疇內「理財」子頁籤（其餘均為非理財） */
const WEALTH_ASSET_TABS = new Set<AssetTab>([
  ASSET_TAB.FUND,
  ASSET_TAB.INSURANCE,
  ASSET_TAB.GOLD,
  ASSET_TAB.WEALTH,
  ASSET_TAB.TRUST,
])

const mainTabs: Array<{ key: MainTab; label: string; enabled: boolean }> = [
  { key: MAIN_TAB.BASIC, label: '基本資料', enabled: true },
  { key: MAIN_TAB.ASSETS, label: '資產負債', enabled: true },
  { key: MAIN_TAB.CREDIT, label: '消費行為與場景', enabled: true },
  { key: MAIN_TAB.INTERACTION, label: '互動紀錄', enabled: true },
]

const assetTabs: Array<{ key: AssetTab; label: string }> = [
  { key: ASSET_TAB.OVERVIEW, label: '總覽' },
  { key: ASSET_TAB.DEPOSIT, label: '存款' },
  { key: ASSET_TAB.FUND, label: '基金/ETF/債券' },
  { key: ASSET_TAB.INSURANCE, label: '保險' },
  { key: ASSET_TAB.GOLD, label: '黃金存摺' },
  { key: ASSET_TAB.WEALTH, label: '財金商品' },
  { key: ASSET_TAB.TRUST, label: '信託' },
  { key: ASSET_TAB.LOAN, label: '貸款' },
]

const visibleMainTabs = computed(() =>
  mainTabs.filter((tab) =>
    tab.key === MAIN_TAB.ASSETS
      ? (props.nonWealthTabVisible ?? true) || (props.wealthTabVisible ?? true)
      : (props.nonWealthTabVisible ?? true),
  ),
)

const visibleAssetTabs = computed(() =>
  assetTabs.filter((tab) =>
    WEALTH_ASSET_TABS.has(tab.key) ? (props.wealthTabVisible ?? true) : (props.nonWealthTabVisible ?? true),
  ),
)

const customerCredit = ref<CustomerCredit | null>(props.data?.customerCredit ?? null)
const isConsumptionLoading = ref(false)
const consumptionError = ref(false)
const loadedConsumptionSerialNo = ref<string | null>(props.data?.customerCredit ? props.data.serialNo : null)

const creditIntentRadar = computed(() => ({
  labels: customerCredit.value?.creditCardModel.labels ?? [],
  customerData: customerCredit.value?.creditCardModel.customerData ?? [],
  clusterData: [],
}))

const getCurrentMonthStr = () => {
  const now = new Date()
  return `${now.getFullYear()}/${String(now.getMonth() + 1).padStart(2, '0')}`
}

const viewState = reactive<{
  activeMainTab: MainTab
  activeAssetTab: AssetTab
  activeMonths: string[]
  activeCategories: string[]
}>({
  activeMainTab: MAIN_TAB.BASIC,
  activeAssetTab: ASSET_TAB.OVERVIEW,
  activeMonths: [getCurrentMonthStr()],
  activeCategories: [],
})

const customerTagClass = (className: string): string => {
  return ['chip', className].join(' ')
}

const customerSegmentName = computed(() => {
  const segment = props.data?.segment?.trim() ?? ''
  const separatorIndex = segment.indexOf('-')
  return separatorIndex >= 0 ? segment.slice(separatorIndex + 1).trim() : segment
})

const loadCustomerConsumption = async (): Promise<void> => {
  if (isConsumptionLoading.value || !props.data || loadedConsumptionSerialNo.value === props.data.serialNo) return

  isConsumptionLoading.value = true
  consumptionError.value = false
  const context = props.data.l2Context
  try {
    const response = await getCustomerConsumption({
      serialNo: props.data.serialNo,
      cipCustId: context.cipCustId,
      certNo: context.certNo,
      creditCardKey: context.creditCardKey,
      circiKey: context.circiKey,
      birthday: context.birthday,
    })
    customerCredit.value = response.data.data ?? null
    if (customerCredit.value) {
      loadedConsumptionSerialNo.value = props.data.serialNo
    }
    consumptionError.value = !customerCredit.value
  } catch (error: unknown) {
    consumptionError.value = true
    if (isHandledError(error)) return
    alertStore.showAlert('系統提示：服務連線異常，無法取得最新資料，請稍後重試。如持續發生，請聯繫資訊處連線管理部。')
  } finally {
    isConsumptionLoading.value = false
  }
}

watch([() => viewState.activeMainTab, () => props.data?.serialNo], ([newTab]) => {
  if (newTab === MAIN_TAB.CREDIT) {
    void loadCustomerConsumption()
  }
})

// 互動紀錄相關狀態與邏輯
const interactionList = ref<Interaction[] | null>(null)
const interactionError = ref(false)

watch(
  [() => viewState.activeMainTab, () => props.data?.serialNo],
  async ([newTab, serialNo]) => {
    if (newTab === MAIN_TAB.INTERACTION && interactionList.value?.length === 0 && serialNo) {
      interactionError.value = false
      try {
        const res = await getCustomerInteractions({
          serialNo,
          cipCustId: props.data?.l2Context.cipCustId ?? null,
          circiKey: props.data?.l2Context.circiKey ?? null,
          certNo: props.data?.l2Context.certNo ?? null,
          creditCardKey: props.data?.l2Context.creditCardKey ?? null,
          birthday: props.data?.l2Context.birthday ?? null,
        })
        if (res.data && res.data.data) {
          interactionList.value = res.data.data
        } else {
          interactionError.value = true
        }
      } catch (error: unknown) {
        interactionError.value = true
        if (isHandledError(error)) return
        alertStore.showAlert('系統提示：服務連線異常，無法取得最新資料，請稍後重試。如持續發生，請聯繫資訊處連線管理部。')
      }
    }
  },
)

// 是否值顯示
const YES = 'Y'
const NO = 'N'

// 資產負債 - 總覽
const overView = ref<Overview>()

// 資產負債 - 存款資料
const deposit = ref<Deposit>()

// 資產負債 - 基金/ETF/債券資料
const fund = ref<Fund>()

// 資產負債 - 保險資料
const insurance = ref<InsuranceBlock>()

// 資產負債 - 黃金存摺
const gold = ref<GoldBlock>()

// 資產負債 - 財金商品
const wealth = ref<Wealth>()
const dciAndSiOutstandingData = ref<Wealth['dciAndSiOutstandingData']>()
const fBondOutstandingData = ref<Wealth['fBondOutstandingData']>()
const rprsOutstandingData = ref<Wealth['rprsOutstandingData']>()

// 資產負債 - 信託
const trust = ref<TrustBlock>()

const loan = ref<LoanBlock>()

// 主分頁監控
watch(
  () => viewState.activeMainTab,
  async (newTab) => {
    if (newTab === MAIN_TAB.INTERACTION && interactionList.value.length === 0 && props.data?.serialNo) {
      interactionError.value = false
      try {
        const res = await getCustomerInteractions({
          serialNo: props.data.serialNo,
          cipCustId: props.data.l2Context.cipCustId ?? null,
          circiKey: props.data.l2Context.circiKey ?? null,
          certNo: props.data.l2Context.certNo ?? null,
          creditCardKey: props.data.l2Context.creditCardKey ?? null,
          birthday: props.data.l2Context.birthday ?? null,
        })
        if (res.data && res.data.data) {
          interactionList.value = res.data.data
        } else {
          interactionError.value = true
        }
      } catch (error: unknown) {
        interactionError.value = true
        if (isHandledError(error)) return
        alertStore.showAlert(
          '系統提示：服務連線異常，無法取得最新資料，請稍後重試。如持續發生，請聯繫資訊處連線管理部。',
        )
      }
    }
  },
)

// API錯誤處理
// realTimeDataErrors是否只會增加不會減少，若錯誤後成功會正常顯示嗎？新增測試情境<----是不是會發生
const errTab = ref<AssetTab | ''>('')
const errMsg = ref<string | null>(null)

interface ApiError {
  message?: string
  response?: {
    data?: {
      errorMessage?: string | null
    }
  }
}

const DEFAULT_ASSET_ERROR_MESSAGE = '系統無法取得資訊，請稍後重試。'

const setAssetError = (tab: AssetTab, message: string | null | undefined): void => {
  errTab.value = tab
  errMsg.value = message || DEFAULT_ASSET_ERROR_MESSAGE
}

const runWhen = <T,>(value: T | null | undefined, action: (value: T) => Promise<void>): Promise<void> =>
  value ? action(value) : Promise.resolve()

const loadAssetData = async <T,>({
  tab,
  request,
  assignData,
  handleResponse,
}: {
  tab: AssetTab
  request: () => Promise<{ data: ApiResponse<T> }>
  assignData: (data: T) => void
  handleResponse?: (response: ApiResponse<T>) => void
}): Promise<void> => {
  try {
    const response = await request()
    handleResponse?.(response.data)
    if (!response.data.success) {
      setAssetError(tab, response.data.errorMessage)
    }
    if (response.data.data) {
      assignData(response.data.data)
    }
  } catch (error: unknown) {
    const err = error as ApiError
    setAssetError(
      tab,
      err.response?.data?.errorMessage ||
      (isHandledError(error) ? err.message : DEFAULT_ASSET_ERROR_MESSAGE) || DEFAULT_ASSET_ERROR_MESSAGE,
    )
  }
}

// 資產子頁監控
watch(
  [() => viewState.activeMainTab, () => viewState.activeAssetTab, () => props.data?.serialNo],
  async ([mainTab, subTab, serialNo]) => {
    errTab.value = ''
    const identityNo = props.data?.identityNo
    const assetLoaders: Record<AssetTab, () => Promise<void>> = {
      [ASSET_TAB.OVERVIEW]: () =>
        runWhen(!overView.value ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData(
            ASSET_TAB.OVERVIEW,
            () =>
              getAssetOverview({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            (data) => (overView.value = data),
          ),
        ),
      [ASSET_TAB.DEPOSIT]: () =>
        runWhen(!deposit.value && identityNo ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.DEPOSIT,
            request: () =>
              getDeposits({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => (deposit.value = data),
          }),
        ),
      [ASSET_TAB.FUND]: () =>
        runWhen(!fund.value && identityNo ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.FUND,
            request: () =>
              getFundsEtfBonds({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => (fund.value = data),
          }),
        ),
      [ASSET_TAB.INSURANCE]: () =>
        runWhen(!insurance.value && identityNo ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.INSURANCE,
            request: () =>
              getInsurance({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => (insurance.value = data),
          }),
        ),
      [ASSET_TAB.GOLD]: () =>
        runWhen(!gold.value && identityNo ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.GOLD,
            request: () =>
              getGold({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => (gold.value = data),
          }),
        ),
      [ASSET_TAB.WEALTH]: () =>
        runWhen(serialNo, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.WEALTH,
            request: () =>
              getWealth({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => {
              wealth.value = data
              dciAndSiOutstandingData.value = data.dciAndSiOutstandingData
              fBondOutstandingData.value = data.fBondOutstandingData
              rprsOutstandingData.value = data.rprsOutstandingData
            },
          }),
        ),
      [ASSET_TAB.TRUST]: () =>
        runWhen(!trust.value ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.TRUST,
            request: () =>
              getTrust({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => (trust.value = data),
            handleResponse: (response) =>
              response.serviceCode === '2003' && setAssetError(ASSET_TAB.TRUST, response.errorMessage),
          }),
        ),
      [ASSET_TAB.LOAN]: () =>
        runWhen(!loan.value ? serialNo : undefined, (customerSerialNo) =>
          loadAssetData({
            tab: ASSET_TAB.LOAN,
            request: () =>
              getLoan({
                serialNo: customerSerialNo,
                cipCustId: props.data?.l2Context.cipCustId ?? null,
                certNo: props.data?.l2Context.certNo ?? identityNo ?? null,
                creditCardKey: props.data?.l2Context.creditCardKey ?? null,
                circiKey: props.data?.l2Context.circiKey ?? null,
                birthday: props.data?.l2Context.birthday ?? null,
              }),
            assignData: (data) => (loan.value = data),
          }),
        ),
    }

    await runWhen(mainTab === MAIN_TAB.ASSETS ? subTab : undefined, (activeAssetTab) => assetLoaders[activeAssetTab]())
  },
)

const totalSubLimitApproved = computed(() =>
  loan.value?.loanList.reduce((sum, data) => sum + Number(data.subLimitApproved), 0),
)
const totalLoanBalanceTwd = computed(() =>
  loan.value?.loanList.reduce((sum, data) => sum + Number(data.loanBalanceTwd), 0),
)

const availableMonths = computed(() => {
  const months = new Set<string>()
  months.add(getCurrentMonthStr())
  interactionList.value?.forEach((item) => {
    const timeStr = item.uiFields.interaction_time
    if (timeStr && timeStr.length >= 7) {
      months.add(timeStr.substring(0, 7).replace('-', '/'))
    }
  })
  return Array.from(months).sort((a, b) => b.localeCompare(a))
})

const availableCategories = computed(() => {
  const cats = new Set<string>()
  interactionList.value?.forEach((item) => {
    if (item.uiFields.category) cats.add(item.uiFields.category)
  })
  return ['全部', ...Array.from(cats)]
})

const finalDisplayInteractions = computed(() => {
  return interactionList.value?.filter((item) => {
    const timeStr = item.uiFields.interaction_time
    if (!timeStr) return false

    const monthStr = timeStr.substring(0, 7).replace('-', '/')
    const matchMonth = viewState.activeMonths.includes(monthStr)

    const matchCategory =
      viewState.activeCategories.length === 0 || viewState.activeCategories.includes(item.uiFields.category || '')

    return matchMonth && matchCategory
  })
})

const toggleMonth = (month: string): void => {
  selectedInteraction.value = null
  const idx = viewState.activeMonths.indexOf(month)
  if (idx > -1) {
    viewState.activeMonths.splice(idx, 1)
  } else {
    viewState.activeMonths.push(month)
  }
}

const toggleCategory = (category: string): void => {
  selectedInteraction.value = null
  if (category === '全部') {
    viewState.activeCategories = []
    return
  }
  const idx = viewState.activeCategories.indexOf(category)
  if (idx > -1) {
    viewState.activeCategories.splice(idx, 1)
  } else {
    viewState.activeCategories.push(category)
  }
}

const formatInteractionDate = (timeStr: string) => {
  if (!timeStr) return { date: '', time: '' }
  const match = timeStr.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})[Ts](\d{1,2}):(\d{1,2})/)
  if (match) {
    const m = match[1].padStart(2, '0')
    const d = match[2].padStart(2, '0')
    const h = match[3].padStart(2, '0')
    const min = match[4].padStart(2, '0')
    return {
      date: `${m}/${d}`,
      time: `${h}:${min}`,
    }
  }
  return { date: timeStr, time: '' }
}

// 拆分多行內容
const splitContent = (content: string) => {
  if (!content) return []
  return content.split('},{')
}

const getStatusClass = (status: string) => {
  if (!status) return ''
  if (status.includes('待補件') || status.includes('待追蹤') || status.includes('婉拒')) return 'warn'
  if (status.includes('未成交') || status.includes('未開啟')) return 'risk'
  return ''
}

watch(
  () => props.data?.serialNo,
  () => {
    viewState.activeMainTab = props.initialTab || visibleMainTabs.value[0]?.key || MAIN_TAB.BASIC
    viewState.activeAssetTab = visibleAssetTabs.value[0]?.key || ASSET_TAB.OVERVIEW
    viewState.activeMonths = [getCurrentMonthStr()]
    viewState.activeCategories = []
    interactionList.value = []
    interactionError.value = false
    customerCredit.value = props.data?.customerCredit ?? null
    loadedConsumptionSerialNo.value = props.data?.customerCredit ? props.data.serialNo : null
    isConsumptionLoading.value = false
    consumptionError.value = false
    selectedInteraction.value = null
  },
)

const selectedInteraction = ref<Interaction | null>(null)

const selectInteraction = (item: Interaction) => {
  selectedInteraction.value = selectedInteraction.value === item ? null : item
}

const formatDetailDateTime = (timeStr: string) => {
  if (!timeStr) return ''
  const match = timeStr.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})[Ts](\d{1,2}):(\d{1,2})/)
  if (match) {
    const y = match[1]
    const m = match[2].padStart(2, '0')
    const d = match[3].padStart(2, '0')
    const h = match[4].padStart(2, '0')
    const min = match[5].padStart(2, '0')
    return `${y}/${m}/${d} ${h}:${min}`
  }
  return timeStr.replace(/-/g, '/').replace('T', ' ')
}

watch(finalDisplayInteractions, (newList) => {
  if (selectedInteraction.value) {
    const isSelectedInList = newList.includes(selectedInteraction.value)
    if (!isSelectedInList) {
      selectedInteraction.value = null
    }
  }
})

watch(
  () => props.initialTab,
  (newTab) => {
    if (newTab) {
      viewState.activeMainTab = newTab
    }
  },
  { immediate: true },
)

// 角色可視權限變動或目前選中頁籤被隱藏時，自動切換至第一個可見頁籤
watch(
  [visibleMainTabs, visibleAssetTabs],
  ([mainTabList, assetTabList]) => {
    if (!mainTabList.some((tab) => tab.key === viewState.activeMainTab)) {
      viewState.activeMainTab = mainTabList[0]?.key ?? MAIN_TAB.BASIC
    }
    if (!assetTabList.some((tab) => tab.key === viewState.activeAssetTab)) {
      viewState.activeAssetTab = assetTabList[0]?.key ?? ASSET_TAB.OVERVIEW
    }
  },
  { immediate: true },
)

const onBackClick = (): void => {
  viewState.activeMainTab = MAIN_TAB.BASIC
  emit('back')
}

const switchMainTab = (tab: MainTab, enabled: boolean): void => {
  if (!enabled) {
    return
  }
  viewState.activeMainTab = tab
}

const switchAssetTab = (tab: AssetTab): void => {
  viewState.activeAssetTab = tab
}

const formatDate = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

const watermarkText = computed(() => {
  const employeeName = authStore.employeeName.trim() || '未具名使用者'
  const rawEmployeeNo = authStore.employeeNo.trim()
  const employeeNo = rawEmployeeNo.length === 0 ? '00000' : rawEmployeeNo.padStart(5, '0').slice(-5)
  return `${employeeNo} ${employeeName} ${formatDate(new Date())}`
})


</script>

<template>
  <section class="customer-advance-root" aria-label="個人資料第二層頁面">
    <watermark v-if="isWatermarkEnabled" :text="watermarkText" />
    <button class="customer-advance-back" type="button" aria-label="回上一頁" @click="onBackClick">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M15 5L8 12L15 19" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
          stroke-width="2" />
      </svg>
      回到畫像
    </button>

    <div v-if="error" class="customer-advance-status">{{ error }}</div>
    <div v-else-if="data" class="customer-page">
      <main class="page-shell">
        <aside class="left-rail">
          <article class="card r20 customer-card">
            <div class="customer-band customer-segment-name">{{ customerSegmentName }}</div>
            <div class="customer-body">
              <div class="customer-photo"><img :src="data.photoUrl" :alt="data.customerName" /></div>
              <div class="customer-meta">
                <div class="customer-name">
                  {{ data.customerName }}
                </div>
                <span class="senior-age" :class="{ 'senior-age': data.customerAge > 65 }">({{ data.customerAge
                }})</span>
              </div>
              <div class="customer-tags">
                <span v-for="tag in data.tags" :key="tag.text" :class="customerTagClass(tag.className)">{{
                  tag.text
                }}</span>
              </div>
            </div>
    </div>
    <div class="customer-identity">
      <span>{{ data.identityNo }}</span>
      <span>生日{{ data.birthday }}</span>
    </div>
    <div class="customer-basic">
      <span class="label">年收入</span><span class="value">{{ data.annualIncome }}</span>
      <span class="label">近12個月貢獻</span><span class="value">{{ data.annualContribution }}</span>
    </div>
    </article>

    <article class="compact-remind">
      <div class="card-head">
        <div class="card-title">關鍵提醒</div>
      </div>
      <div class="remind-list">
        <div v-for="reminder in data.reminders" :key="reminder.title" class="remind-item">
          <div class="remind-t">{{ reminder.title }}</div>
          <div v-if="reminder.content" class="remind-d">{{ reminder.content }}</div>
          <div v-if="reminder.items" class="remind-d">
            <ul>
              <li v-for="item in reminder.items" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </div>
    </article>
    </aside>

    <section class="detail-main">
      <button v-for="tab in visibleMainTabs" :key="tab.key" class="tab formLabel"
        :class="{ select: viewState.activeMainTab === tab.key, 'is-disabled': !tab.enabled }" type="button"
        @click="switchMainTab(tab.key, tab.enabled)">
        {{ tab.label }}
      </button>

      <div v-show="viewState.activeMainTab === MAIN_TAB.BASIC" class="tab-panel" data-panel="MAIN_TAB.BASIC">
        <article class="info-card info-grid-3">
          <section v-for="section in data.basicSections" :key="section.title" class="info-section">
            <div class="section-title">{{ section.title }}</div>
            <div class="data-grid">
              <template v-for="item in section.items" :key="`${section.title}-${item.label}`">
                <div class="data-label">{{ item.label }}</div>
                <div class="data-value">{{ item.value }}</div>
              </template>
            </div>
          </section>
        </article>

        <div class="lower-grid">
          <article class="info-card identity-card">
            <div class="section-title">身份註記</div>
            <div class="kv-grid">
              <template v-for="item in data.identityItems" :key="item.label">
                <div class="data-label">{{ item.label }}</div>
                <div class="data-value">{{ item.value }}</div>
              </template>
            </div>
          </article>

          <article class="info-card risk-card">
            <div class="section-title">風險屬性</div>
            <div class="risk-summary">
              <div class="risk-label">理財風險</div>
              <div class="risk-value">{{ data.riskSummary.wealthRisk }}</div>
            </div>
            <div class="risk-label">CDD風險</div>
            <div class="risk-value">{{ data.riskSummary.cddRisk }}</div>
            <div class="kv-grid">
              <template v-for="item in data.riskItems" :key="item.label">
                <div class="data-label">{{ item.label }}</div>
                <div class="data-value">{{ item.value }}</div>
              </template>
            </div>
          </article>

          <article class="info-card potential-card-detail">
            <div class="card-head">
              <div class="section-title">潛力資訊</div>
            </div>
            <div v-for="property in data.properties" :key="`${property.address}-${property.evaluationDate}`"
              class="asset-item">
              <div class="asset-head">
                <span class="asset-tag">{{ property.type }}</span>
                <span class="asset-eval-date">評估日 {{ property.evaluationDate }}</span>
              </div>
              <div class="asset-address">{{ property.address }}</div>
              <div class="asset-values">
                <div>
                  <div class="muted">評估總值</div>
                  <div class="data-value">{{ property.estimatedValue }}</div>
                </div>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div v-show="viewState.activeMainTab === MAIN_TAB.ASSETS" class="tab-panel" data-panel="MAIN_TAB.ASSETS">
        <div class="sub-tabs formLabelContainer mediumType1" role="tablist" aria-label="資產負債二階選單">
          <button v-for="tab in visibleAssetTabs" :key="tab.key" class="sub-tab formLabel"
            :class="{ select: viewState.activeAssetTab === tab.key }" type="button" @click="switchAssetTab(tab.key)">
            {{ tab.label }}
          </button>
        </div>

        <article class="info-card panel-card">
          <!-- 總覽 -->
          <div v-show="viewState.activeAssetTab === ASSET_TAB.OVERVIEW" class="sub-panel"
            data-subpanel="ASSET_TAB.OVERVIEW">
            <div class="stock-summary">
              <div class="stock-metric">
                <div class="stock-label">AuM 總資產(含證券)</div>
                <div v-if="errTab === ASSET_TAB.OVERVIEW" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-value">${{ formatAmount(overview?.balTwdVal, { maxDecimals: 2 }) }}</div>
              </div>
              <div class="stock-metric">
                <div class="stock-label">理財AuM</div>
                <div v-if="errTab === ASSET_TAB.OVERVIEW" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-value">
                  ${{ formatAmount(overview?.balTwdWmVal, { maxDecimals: 2 }) }}
                </div>
              </div>
              <div class="stock-metric">
                <div class="stock-label">LuM 總負債</div>
                <div v-if="errTab === ASSET_TAB.OVERVIEW" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-value">
                  ${{ formatAmount(overview?.totalLumDaily, { maxDecimals: 2 }) }}
                </div>
              </div>
              <div class="stock-metric">
                <div class="stock-label">LuM 總負債+信用卡已使用額度</div>
                <div v-if="errTab === ASSET_TAB.OVERVIEW" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-value">
                  ${{ formatAmount(overview?.totalLumDailyAndCcBalanceDaily, { maxDecimals: 2 }) }}
                </div>
              </div>
            </div>

            <div class="stock-tables">
              <section class="stock-table-card">
                <div class="stock-table-title">
                  <span>AuM</span>
                  <span class="stock-unit">單位：元</span>
                </div>
                <div v-if="errTab === ASSET_TAB.OVERVIEW" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-row stock-head lum">
                  <div>產品類別</div>
                  <div>現欠餘額</div>
                  <div>占比</div>
                </div>
                <div v-if="!overview?.aum && errTab !== ASSET_TAB.OVERVIEW" class="stock-row mid-text">
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                </div>
                <div v-for="item in overview?.aum" :key="item.productCategory" class="stock-row">
                  <div class="stock-name">{{ item.productCategory }}</div>
                  <div class="stock-amount">
                    {{
                      (item.amount == null ? '' : '$') +
                      formatAmount(item.amount, { fallback: '-', maxDecimals: 2 })
                    }}
                  </div>
                  <div class="stock-rate">
                    {{
                      item.amount == null ? '-' : formatAmount(item.percentage * 100, { maxDecimals: 2 }) + '%'
                    }}
                  </div>
                </div>
              </section>

              <section class="stock-table-card">
                <div class="stock-table-title">
                  <span>LuM+信用卡</span>
                  <span class="stock-unit">單位：元</span>
                </div>
                <div v-if="errTab === ASSET_TAB.OVERVIEW" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-row stock-head lum">
                  <div>產品類別</div>
                  <div>現欠餘額</div>
                  <div>核准額度</div>
                  <div>占比</div>
                </div>
                <div v-if="!overview?.lum && errTab !== ASSET_TAB.OVERVIEW" class="stock-row lum mid-text">
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                </div>
                <div v-for="item in overview?.lum" :key="item.productCategory" class="stock-row lum">
                  <div class="stock-name">{{ item.productCategory }}</div>
                  <div class="stock-amount stock-debt">
                    {{
                      (item.outStandingAmt == null ? '' : '$') +
                      formatAmount(item.outStandingAmt, { fallback: '-', maxDecimals: 2 })
                    }}
                  </div>
                  <div class="stock-rate">
                    {{
                      (item.apprAmt == null ? '' : '$') +
                      formatAmount(item.apprAmt, { fallback: '-', maxDecimals: 2 })
                    }}
                  </div>
                  <div class="stock-term">
                    {{
                      item.outStandingAmt == null
                        ? '-'
                        : formatAmount(item.percentage * 100, { maxDecimals: 2 }) + '%'
                    }}
                  </div>
                </div>
              </section>
            </div>
          </div>

          <!-- 存款 -->
          <div v-show="viewState.activeAssetTab === ASSET_TAB.DEPOSIT" class="sub-panel"
            data-subpanel="ASSET_TAB.DEPOSIT">
            <div class="deposit-sections">
              <section class="stock-table-card deposit-table">
                <div class="stock-table-title">
                  <span>台幣活存(含支存)</span>
                  <button class="deposit-expand" type="button">展開</button>
                </div>
                <div v-if="errTab === ASSET_TAB.DEPOSIT" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-row stock-head deposit-row-3">
                  <div>帳號</div>
                  <div>帳戶餘額</div>
                  <div>可用餘額</div>
                </div>
                <div v-if="!deposit?.twdDemandDeposit?.demandDepositList?.length && errTab !== ASSET_TAB.DEPOSIT"
                  class="stock-row deposit-row-3 mid-text">
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                </div>
                <div v-else>
                  <div v-for="item in deposit?.twdDemandDeposit?.demandDepositList" :key="item.account"
                    class="stock-row deposit-row-3">
                    <div>{{ item.account }}</div>
                    <div class="stock-amount">{{ formatAmount(item.realBalance) }}</div>
                    <div class="stock-amount">{{ formatAmount(item.availableBalance) }}</div>
                  </div>
                </div>
                <div v-if="errTab === ASSET_TAB.DEPOSIT" class="stock-row deposit-row-3 deposit-total">
                  <div>小計</div>
                  <div class="stock-amount">${{ formatAmount(deposit?.twdDemandDeposit?.realBalanceTotal) }}</div>
                  <div class="stock-amount">${{ formatAmount(deposit?.twdDemandDeposit?.availableBalanceTotal) }}</div>
                </div>
              </section>

              <section class="stock-table-card deposit-table">
                <div class="stock-table-title"><span>台幣定存</span></div>
                <div v-if="errTab === ASSET_TAB.DEPOSIT" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-row stock-head deposit-twd-fixed-row">
                  <div>帳號</div>
                  <div>存單面額</div>
                  <div>利率</div>
                  <div>存款起日</div>
                  <div>到期日</div>
                  <div>是否約定自動轉期</div>
                </div>
                <div v-if="!deposit?.twdTimeDeposit?.twdTimeDepositList?.length && errTab !== ASSET_TAB.DEPOSIT"
                  class="stock-row deposit-twd-fixed-row mid-text">
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                </div>
                <div v-else>
                  <div v-for="item in deposit?.twdTimeDeposit?.twdTimeDepositList" :key="item.tdAccNo"
                    class="stock-row deposit-twd-fixed-row">
                    <div>{{ item.tdAccNo }}</div>
                    <div class="stock-amount">${{ formatAmount(item.cdAmt) }}</div>
                    <div class="stock-rate">{{ formatAmount(item.cdInterRate * 100, { maxDecimals: 2 }) }}%</div>
                    <div>{{ item.pledgeMark }}</div>
                    <div>{{ dateFormat(item.maturityDate) }}</div>
                    <div>{{ item.autoRenewMark === '1' || item.autoRenewMark === '2' ? YES : NO }}</div>
                  </div>
                </div>
                <div v-if="errTab === ASSET_TAB.DEPOSIT" class="stock-row deposit-twd-fixed-row deposit-total">
                  <div>小計</div>
                  <div class="stock-amount">${{ formatAmount(deposit?.twdTimeDeposit?.cdAmtTotal) }}</div>
                  <div class="stock-rate">-</div>
                </div>
              </section>

              <section class="stock-table-card deposit-table">
                <div class="stock-table-title"><span>外幣活存</span></div>
                <div v-if="errTab === ASSET_TAB.DEPOSIT" class="api-error">{{ errMsgs }}</div>
                <div v-else class="stock-row stock-head deposit-row-6">
                  <div>帳號</div>
                  <div>幣別</div>
                  <div>可用餘額</div>
                  <div>帳戶餘額(原幣)</div>
                  <div>帳戶餘額(折台)</div>
                </div>
                <div v-if="!deposit?.fxDemandDeposit?.demandDepositList?.length && errTab !== ASSET_TAB.DEPOSIT"
                  class="stock-row deposit-row-6 mid-text">
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                </div>
                <div v-else>
                  <div v-for="item in deposit?.fxDemandDeposit?.demandDepositList" :key="item.account"
                    class="stock-row deposit-row-6">

                    <div>{{ item.account }}</div>
                    <div>{{ item.curr }}</div>
                    <div class="stock-amount">{{ formatAmount(item.realBalance, { maxDecimals: 2 }) }}</div>
                    <div class="stock-amount">{{ formatAmount(item.availableBalance, { maxDecimals: 2 }) }}</div>
                    <div class="stock-amount">
                      ${{ formatAmount(item.availableBalanceTwd, { maxDecimals: 2 }) }}
                    </div>
                  </div>
                </div>

                <div v-if="errTab !== ASSET_TAB.DEPOSIT" class="stock-row deposit-row-6 deposit-total">
                  <div>小計</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div>-</div>
                  <div class="stock-amount">
                    ${{ formatAmount(deposit?.fxDemandDeposit?.availableBalanceTwdTotal, { maxDecimals: 2 }) }}
                  </div>
                </div>
            </div>
    </section>

    <section class="stock-table-card deposit-table">
      <div class="stock-table-title"><span>外幣定存</span></div>
      <div v-if="errTab === ASSET_TAB.DEPOSIT" class="api-error">{{ errMsgs }}</div>
      <div v-else class="stock-row stock-head deposit-row-7">
        <div>帳號</div>
        <div>存單面額</div>
        <div>幣別</div>
        <div>利率</div>
        <div>存單起訖</div>
        <div>到期日</div>
        <div>是否約定自動轉期</div>
      </div>
      <div v-if="!deposit?.fxTimeDeposit?.fxTimeDepositList?.length && errTab !== ASSET_TAB.DEPOSIT"
        class="stock-row deposit-row-7 mid-text">
        <div>-</div>
        <div>-</div>
        <div>-</div>
        <div>-</div>
        <div>-</div>
        <div>-</div>
        <div>-</div>
      </div>
      <div v-else>
        <div v-for="item in deposit?.fxTimeDeposit?.fxTimeDepositList" :key="item.tdAccNo"
          class="stock-row deposit-row-7">
          <div>{{ item.tdAccNo }}</div>
          <div class="stock-amount">{{ formatAmount(item.cdAmt, { maxDecimals: 2 }) }}</div>
          <div>{{ item.curr }}</div>
          <div class="stock-rate">{{ formatAmount(item.cdInterRate * 100, { maxDecimals: 2 }) }}%</div>
          <div>{{ item.pledgeMark }}</div>
          <div>{{ dateFormat(item.maturityDate) }}</div>
          <div>{{ item.autoRenewMark === '1' || item.autoRenewMark === '2' ? YES : NO }}</div>
        </div>
      </div>
    </section>


    </div>
    </div>

    <!-- 基金/ETF/債券 -->
    <div v-show="viewState.activeAssetTab === ASSET_TAB.FUND" class="sub-panel" data-subpanel="ASSET_TAB.FUND">
      <div class="fund-sections">
        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title"><span>基金 - 單筆</span></div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-single dual-header">
            <div class="span-row">產品名稱</div>
            <div class="span-row">信託編號</div>
            <div class="merged-col">風險報酬等級</div>
            <div class="sub-col-1">交易</div>
            <div class="sub-col-2">最新</div>
            <div class="span-row">幣別</div>
            <div class="span-row">投資金額<br />(含手續費)</div>
            <div class="span-row">投資單位數</div>
            <div class="span-row">投資現值</div>
            <div class="span-row">累計配息</div>
            <div class="span-row">損益<br />(含累計配息)</div>
            <div class="span-row">報酬率</div>
            <div class="span-row">申購價</div>
            <div class="span-row">參考贖回價</div>
          </div>
          <div v-if="!fund?.singleFundList?.length && errTab !== ASSET_TAB.FUND" class="stock-row fund-row-single">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.singleFundList" :key="index" class="stock-row fund-row-single">
              <div class="stock-name">{{ item.prdtSNam }}</div>
              <div>{{ item.trustNo }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvl }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvlLatest }}</div>
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstAmtWithFee, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.shrBal, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.invstPValue, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalDvdnd, { maxDecimals: 2 }) }}</div>
              <div class="fund-number fund-gain">{{ profitAndLossFormat(item.invstRtrn) }}</div>
              <div class="fund-number">{{ formatAmount(item.rtrnRate, { maxDecimals: 2 }) }}%</div>
              <div class="fund-number">{{ formatAmount(item.buyTrnstAmt, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.rfrncSellPrice, { maxDecimals: 2 }) }}</div>
            </div>
          </div>
        </section>

        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title"><span>基金 - 定期(不)定額</span></div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-dca dual-header">
            <div class="span-row">產品名稱</div>
            <div class="span-row">信託編號</div>
            <div class="merged-col">風險報酬等級</div>
            <div class="sub-col-1">交易</div>
            <div class="sub-col-2">最新</div>
            <div class="span-row">幣別</div>
            <div class="span-row">投資金額<br />(含手續費)</div>
            <div class="span-row">投資單位數</div>
            <div class="span-row">投資現值</div>
            <div class="span-row">累計配息</div>
            <div class="span-row">損益<br />(含累計配息)</div>
            <div class="span-row">報酬率</div>
            <div class="span-row">是否續扣</div>
            <div class="span-row">單次扣款金額</div>
            <div class="span-row">參考贖回價</div>
          </div>
          <div v-if="!fund?.regularFundList?.length && errTab !== ASSET_TAB.FUND" class="stock-row fund-row-dca">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.regularFundList" :key="index" class="stock-row fund-row-dca">
              <div class="stock-name">{{ item.prdtSNam }}</div>
              <div>{{ item.trustNo }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvl }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvlLatest }}</div>
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstAmtWithFee, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.shrBal, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.invstPValue, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalDvdnd, { maxDecimals: 2 }) }}</div>
              <div class="fund-number fund-gain">{{ profitAndLossFormat(item.invstRtrn) }}</div>
              <div class="fund-number">{{ formatAmount(item.rtrnRate, { maxDecimals: 2 }) }}%</div>
              <div class="fund-number">{{ item.cntnDeduct }}</div>
              <div class="fund-number">{{ formatAmount(item.deductAmt, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.rfrncSellPrice, { maxDecimals: 2 }) }}</div>
            </div>
          </div>
        </section>

        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title"><span>結構型商品</span></div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-main dual-header">
            <div class="span-row">產品名稱</div>
            <div class="span-row">信託編號</div>
            <div class="merged-col">風險報酬等級</div>
            <div class="sub-col-1">交易</div>
            <div class="sub-col-2">最新</div>
            <div class="span-row">幣別</div>
            <div class="span-row">投資金額<br />(含手續費)</div>
            <div class="span-row">投資單位數</div>
            <div class="span-row">投資現值</div>
            <div class="span-row">累計配息</div>
            <div class="span-row">損益<br />(含累計配息)</div>
            <div class="span-row">報酬率</div>
          </div>
          <div v-if="!fund?.structuredProductList?.length && errTab !== ASSET_TAB.FUND" class="stock-row fund-row-main">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.structuredProductList" :key="index" class="stock-row fund-row-main">
              <div class="stock-name">{{ item.prdtSNam }}</div>
              <div>{{ item.trustNo }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvl }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvlLatest }}</div>
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstAmtWithFee, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.shrBal, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.invstPValue, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalDvdnd, { maxDecimals: 2 }) }}</div>
              <div class="fund-number fund-gain">{{ profitAndLossFormat(item.invstRtrn) }}</div>
              <div class="fund-number">{{ formatAmount(item.rtrnRate, { maxDecimals: 2 }) }}%</div>
            </div>
          </div>
        </section>

        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title"><span>債券</span></div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-main dual-header">
            <div class="span-row">產品名稱</div>
            <div class="span-row">信託編號</div>
            <div class="merged-col">風險報酬等級</div>
            <div class="sub-col-1">交易</div>
            <div class="sub-col-2">最新</div>
            <div class="span-row">幣別</div>
            <div class="span-row">投資金額<br />(含手續費)</div>
            <div class="span-row">投資單位數</div>
            <div class="span-row">投資現值</div>
            <div class="span-row">累計配息</div>
            <div class="span-row">損益<br />(含累計配息)</div>
            <div class="span-row">報酬率</div>
          </div>
          <div v-if="!fund?.bondList?.length && errTab !== ASSET_TAB.FUND" class="stock-row fund-row-main">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.bondList" :key="index" class="stock-row fund-row-main">
              <div class="stock-name">{{ item.prdtSNam }}</div>
              <div>{{ item.trustNo }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvl }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvlLatest }}</div>
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstAmtWithFee, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.shrBal, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.invstPValue, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalDvdnd, { maxDecimals: 2 }) }}</div>
              <div class="fund-number fund-gain">{{ profitAndLossFormat(item.invstRtrn) }}</div>
              <div class="fund-number">{{ formatAmount(item.rtrnRate, { maxDecimals: 2 }) }}%</div>
            </div>
          </div>
        </section>

        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title"><span>ETF/海外股票</span></div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-main dual-header">
            <div class="span-row">產品名稱</div>
            <div class="span-row">信託編號</div>
            <div class="merged-col">風險報酬等級</div>
            <div class="sub-col-1">交易</div>
            <div class="sub-col-2">最新</div>
            <div class="span-row">幣別</div>
            <div class="span-row">投資金額<br />(含手續費)</div>
            <div class="span-row">投資單位數</div>
            <div class="span-row">投資現值</div>
            <div class="span-row">累計配息</div>
            <div class="span-row">損益<br />(含累計配息)</div>
            <div class="span-row">報酬率</div>
          </div>
          <div v-if="!fund?.stockList?.length && errTab !== ASSET_TAB.FUND" class="stock-row fund-row-main">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.stockList" :key="index" class="stock-row fund-row-main">
              <div class="stock-name">{{ item.prdtSNam }}</div>
              <div>{{ item.trustNo }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvl }}</div>
              <div class="fund-risk">{{ item.productRiskRtrnLvlLatest }}</div>
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstAmtWithFee, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.shrBal, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.invstPValue, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalDvdnd, { maxDecimals: 2 }) }}</div>
              <div class="fund-number fund-gain">{{ profitAndLossFormat(item.invstRtrn) }}</div>
              <div class="fund-number">{{ formatAmount(item.rtrnRate, { maxDecimals: 2 }) }}%</div>
            </div>
          </div>
        </section>

        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title">
            <span>未實現損益統計(不含在途中)</span>
          </div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-summary">
            <div>幣別</div>
            <div>投資金額<br />(含手續費)</div>
            <div>投資現值</div>
            <div>累計配息</div>
            <div>總損益<br />(含累計配息)</div>
            <div>原幣報酬率<br />(含累計配息)</div>
            <div>往來總計<br />(折台臺幣總參考現值，不含配息)</div>
          </div>
          <div v-if="!fund?.unrealizedPnlList?.length && errTab !== ASSET_TAB.FUND" class="stock-row fund-row-summary">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.unrealizedPnlList" :key="index" class="stock-row fund-row-summary">
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstAmtWithFee, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstPValue, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalDvdnd, { maxDecimals: 2 }) }}</div>
              <div class="fund-number fund-gain">{{ profitAndLossFormat(item.totalInvstRtrnWithDvdnd) }}</div>
              <div class="fund-number">{{ formatAmount(item.totalInvstRtrn, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ formatAmount(item.trustAccntEntireAmt, { maxDecimals: 2 }) }}</div>
            </div>
          </div>
        </section>

        <section class="stock-table-card deposit-table fund-table">
          <div class="stock-table-title"><span>在途中明細</span></div>
          <div v-if="errTab === ASSET_TAB.FUND" class="api-error">{{ errMsgs }}</div>
          <div v-else class="stock-row stock-head fund-row-history">
            <div>交易日期</div>
            <div>信託性質</div>
            <div>交易別</div>
            <div>基金名稱</div>
            <div>信託編號</div>
            <div>幣別</div>
            <div>投資金額</div>
            <div>專案類別</div>
          </div>
          <div v-if="!fund?.txHistoryInfoList?.length && errTab !== ASSET_TAB.FUND"
            class="stock-row fund-row-history mid-text">
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
            <div>-</div>
          </div>
          <div v-else>
            <div v-for="(item, index) in fund?.txHistoryInfoList" :key="index" class="stock-row fund-row-history">
              <div class="fund-number">{{ dateFormat(item.txDate) }}</div>
              <div class="fund-number">{{ item.invstTyp }}</div>
              <div class="fund-number">{{ item.txCtgry }}</div>
              <div class="stock-name">{{ item.prdtNam }}</div>
              <div class="fund-number">{{ item.trustNo }}</div>
              <div class="fund-currency">{{ item.invstCurr }}</div>
              <div class="fund-number">{{ formatAmount(item.invstAmt, { maxDecimals: 2 }) }}</div>
              <div class="fund-number">{{ item.prjctCtgry }}</div>
            </div>
          </div>
        </section>

        <!-- 保險 -->
        <div v-show="viewState.activeAssetTab === ASSET_TAB.INSURANCE" class="sub-panel"
          data-subpanel="ASSET_TAB.INSURANCE">
          <div class="warning">
            【合規叮嚀】嚴禁勸誘顧客「解約舊保單、改買新商品」（含新保單或其他理財商品），請務必落實合規審查，切勿觸法。
          </div>
          <div class="asset-detail-sections">
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>保險</span></div>
              <div v-if="errTab === ASSET_TAB.INSURANCE" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head insurance-row">
                <div>要保人</div>
                <div>被保險人</div>
                <div>保險公司</div>
                <div>保單狀態</div>
                <div>險種名稱</div>
                <div>承保日</div>
                <div>到期日</div>
                <div>下次繳費日</div>
                <div>繳費終期</div>
                <div>幣別</div>
                <div>每期保費</div>
                <div>累計已繳保費</div>
                <div>繳別</div>
                <div>生存/期滿保險金</div>
              </div>
              <div v-if="!insurance?.insuranceList?.length && errTab !== ASSET_TAB.INSURANCE"
                class="stock-row insurance-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-else>
                <div v-for="(item, index) in insurance?.insuranceList" :key="index" class="stock-row insurance-row">
                  <div class="stock-name">{{ item.ownerId }}</div>
                  <div>{{ item.insuredId }}</div>
                  <div>{{ item.insConName }}</div>
                  <div>{{ item.postStatusCode }}</div>
                  <div>{{ item.planName }}</div>
                  <div>{{ dateFormat(item.poIssueDate) }}</div>
                  <div>{{ dateFormat(item.coverageExpiredDate) }}</div>
                  <div>{{ dateFormat(item.nextPaidDate) }}</div>
                  <div>{{ dateFormat(item.payExpiredDate) }}</div>
                  <div class="asset-currency">{{ item.cuyCpo }}</div>
                  <div class="asset-number">{{ formatAmount(item.txnAmtCpo, { maxDecimals: 2 }) }}</div>
                  <div class="asset-number">{{ formatAmount(item.premAmtTotalCpo, { maxDecimals: 2 }) }}</div>
                  <div class="asset-currency">{{ item.modx }}</div>
                  <div class="asset-number">{{ formatAmount(item.mtyAmt, { maxDecimals: 2 }) }}</div>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 黃金存摺 -->
        <div v-show="viewState.activeAssetTab === ASSET_TAB.GOLD" class="sub-panel" data-subpanel="ASSET_TAB.GOLD">
          <div class="asset-detail-sections">
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>黃金存摺</span></div>
              <div v-if="errTab === ASSET_TAB.GOLD" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head gold-row">
                <div>黃金存摺帳號</div>
                <div>幣別</div>
                <div>可用餘額(公克/盎司)</div>
                <div>現值(原幣)</div>
                <div>預估損益(原幣)</div>
                <div>預估損益率</div>
              </div>
              <div v-if="!gold?.goldList?.length && errTab !== ASSET_TAB.GOLD" class="stock-row gold-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-else>
                <div v-for="(item, index) in gold?.goldList" :key="index" class="stock-row gold-row">
                  <div class="stock-name">{{ item.pmAcct }}</div>
                  <div class="asset-currency">{{ item.invstCurr }}</div>
                  <div class="asset-number">{{ item.bookingDateRealBlnc }}</div>
                  <div class="asset-number">{{ formatAmount(item.presentValue) }}</div>
                  <div class="asset-number fund-loss">{{ profitAndLossFormat(item.esProfitLoss) }}</div>
                  <div class="asset-number">{{ item.esProfitLossPct }}%</div>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 財富管理/組合式商品 -->
        <div v-show="viewState.activeAssetTab === ASSET_TAB.WEALTH" class="sub-panel" data-subpanel="ASSET_TAB.WEALTH">
          <div class="asset-detail-sections">
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>組合式商品</span></div>
              <div v-if="errTab === ASSET_TAB.WEALTH" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head wealth-structured-row">
                <div>交易編號</div>
                <div>產品名稱</div>
                <div>幣別<br />相對幣別</div>
                <div>本金<br />轉換匯率</div>
                <div>訂約日<br />比價日</div>
                <div>起息日<br />到期日</div>
                <div>評估價格(%)<br />評估價格日期</div>
                <div>現值<br />稅前收益</div>
                <div>稅前<br />報酬率(%)</div>
                <div>預估損益<br />(原幣)</div>
              </div>
              <div v-if="!dciAndSiOutstandingData?.length && errTab !== ASSET_TAB.WEALTH"
                class="stock-row wealth-structured-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-else>
                <div v-for="(item, index) in dciAndSiOutstandingData" :key="index"
                  class="stock-row wealth-structured-row">
                  <div class="stock-name">{{ item.product_no }}</div>
                  <div>{{ item.product_name }}</div>
                  <div class="asset-currency">{{ item.trx_ccy }}<br />{{ item.exchange_ccy }}</div>
                  <div class="asset-number">
                    {{ formatAmount(item.trx_amt) }}<br />{{ item.strike_price }}
                  </div>
                  <div>{{ item.trade_date }}<br />{{ item.fixing_date }}</div>
                  <div>{{ item.value_date }}<br />{{ item.maturity_date }}</div>
                  <div class="asset-number">
                    <span>{{ item.mtm_price }}</span><br />{{ item.mtm_date }}
                  </div>
                  <div class="asset-number">
                    {{ formatAmount(item.mtm_amt) }}<br />{{ item.profit_amt }}
                  </div>
                  <div class="asset-number">
                    {{ item.profit_rate }}<span v-if="item.profit_rate !== '-'">%</span>
                  </div>
                  <div class="asset-number fund-gain">{{ profitAndLossFormat(Number(item.pl)) }}</div>
                </div>
              </div>
            </section>

            <!-- 外幣自營債券 -->
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>外幣自營債券</span></div>
              <div v-if="errTab === ASSET_TAB.WEALTH" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head wealth-bond-row">
                <div>發行機構</div>
                <div>幣別</div>
                <div>最近成交日期</div>
                <div>成交均價(%)</div>
                <div>成交匯率</div>
                <div>本金(原幣)</div>
                <div>到期日</div>
                <div>前手利息</div>
                <div>預估損益<br />(含累計配息)</div>
                <div>庫存面額</div>
              </div>
              <div v-if="!fBondOutstandingData && errTab !== ASSET_TAB.WEALTH"
                class="stock-row wealth-bond-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-else>
                <div class="stock-row wealth-bond-row">
                  <div class="stock-name">{{ fBondOutstandingData.issuerName }}</div>
                  <div class="asset-currency">{{ fBondOutstandingData.ccy }}</div>
                  <div>{{ fBondOutstandingData.lastTradeDate }}</div>
                  <div class="asset-number">{{ fBondOutstandingData.lastTradePrice }}</div>
                  <div class="asset-number">{{ fBondOutstandingData.lastTradeExchangeRateTWD }}</div>
                  <div class="asset-number">{{ formatAmount(fBondOutstandingData.principalAmount) }}</div>
                  <div class="asset-number">{{ fBondOutstandingData.maturityDate }}</div>
                  <div class="asset-number">{{ formatAmount(fBondOutstandingData.accruedInterest) }}</div>
                  <div class="asset-number fund-gain">
                    {{ profitAndLossFormat(Number(fBondOutstandingData.pl)) }}
                  </div>
                  <div class="asset-number">{{ formatAmount(fBondOutstandingData.outstandingAmount) }}</div>
                </div>
              </div>
            </section>

            <!-- 票債券附條件交易 -->
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>票債券附條件交易</span></div>
              <div v-if="errTab === ASSET_TAB.WEALTH" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head wealth-rp-row">
                <div>交易類型</div>
                <div>交易序號</div>
                <div>交易金額</div>
                <div>承作利率</div>
                <div>交易日期</div>
                <div>利息金額</div>
                <div>稅後到期金額</div>
                <div>交易類型</div>
              </div>
              <div v-if="!rprsOutstandingData && errTab !== ASSET_TAB.WEALTH" class="stock-row wealth-rp-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-else>
                <div class="stock-row wealth-rp-row">
                  <div class="stock-name">{{ rprsOutstandingData.deal_type }}</div>
                  <div>{{ rprsOutstandingData.deal_no }}</div>
                  <div class="asset-number">{{ formatAmount(rprsOutstandingData.trx_amt) }}</div>
                  <div class="asset-number">{{ rprsOutstandingData.deal_no }}</div>
                  <div class="asset-number">{{ rprsOutstandingData.trade_date }}</div>
                  <div class="asset-number">{{ formatAmount(rprsOutstandingData.trx_amt) }}</div>
                  <div class="asset-number">{{ formatAmount(rprsOutstandingData.maturity_amt) }}</div>
                  <div class="asset-number">{{ rprsOutstandingData.deal_type }}</div>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 信託契約 -->
        <div v-show="viewState.activeAssetTab === ASSET_TAB.TRUST" class="sub-panel" data-subpanel="ASSET_TAB.TRUST">
          <div class="asset-detail-sections">
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>信託契約 - 總覽</span></div>
              <div v-if="errTab === ASSET_TAB.TRUST" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head trust-row">
                <div>契約編號</div>
                <div>契約名稱</div>
                <div>信託財產(折台台幣)</div>
                <div>信託關係</div>
                <div>信託財產庫存資訊</div>
              </div>
              <div v-if="!trust?.trustList?.length && errTab !== ASSET_TAB.TRUST" class="stock-row trust-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-else>
                <div v-for="(item, index) in trust?.trustList" :key="index" class="stock-row trust-row">
                  <div class="stock-name">{{ item.contractNo }}</div>
                  <div>{{ item.contractName }}</div>
                  <div class="asset-number">{{ formatAmount(item.trustPropertyValue) }}</div>
                  <div>
                    <span class="trust-rel" style="white-space: pre-line;">{{
                      item.trustorAndTrustBeneficiary
                    }}</span>
                  </div>
                  <div>
                    <div v-if="item.currencyAndDeposit === '015_017_018_Error'" class="api-error">{{ errMsgs }}</div>
                    <div v-else>
                      <span class="trust-inventory" style="white-space: pre-line;">
                        <template v-for="(line, lineIndex) in item.currencyAndDeposit.split('\n')" :key="lineIndex">
                          {{ line.split(':')[0] }} : ${{ formatAmount(line.split(':')[1]) }}<br />
                        </template>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 貸款 -->
        <div v-show="viewState.activeAssetTab === ASSET_TAB.LOAN" class="sub-panel" data-subpanel="ASSET_TAB.LOAN">
          <div class="asset-detail-sections">
            <div class="warning">
              【合規叮嚀】嚴禁以任何形式鼓勵、勸誘或要求客戶於辦理授信業務時搭配理財商品，包括但不限於「授信回存」、「搭配銷售（如綁約購...」
            </div>
            <section class="stock-table-card deposit-table asset-detail-table">
              <div class="stock-table-title"><span>貸款</span></div>
              <div v-if="errTab === ASSET_TAB.LOAN" class="api-error">{{ errMsgs }}</div>
              <div v-else class="stock-row stock-head loan-row">
                <div>帳號</div>
                <div>貸款額度</div>
                <div>撥款日</div>
                <div>到期日</div>
                <div>章限期限</div>
                <div>幣別</div>
                <div>貸款金額</div>
                <div>目前餘額</div>
                <div>當前貸款利率</div>
              </div>
              <div v-if="!loan?.loanList?.length && errTab !== ASSET_TAB.LOAN" class="stock-row loan-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>

              <div v-if="!loan?.loanList?.length && errTab !== ASSET_TAB.LOAN" class="stock-row loan-row mid-text">
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
              </div>
              <div v-for="(item, index) in loan?.loanList" :key="index" class="stock-row loan-row">
                <div>{{ item.loanNo }}</div>
                <div>{{ item.productType }}</div>
                <div>{{ item.loanStartDate }}</div>
                <div>{{ item.loanMaturityDate }}</div>
                <div>{{ item.principalGracePeriodStartDate }}</div>
                <div class="asset-currency">{{ item.limitCcy }}</div>
                <div class="asset-number">{{ formatAmount(item.subLimitApproved) }}</div>
                <div class="asset-number">{{ formatAmount(item.loanBalanceTwd) }}</div>
                <div class="asset-number">{{ item.nominalInterestRate }}</div>
              </div>
              <div v-if="loan?.loanList?.length" class="stock-row loan-row deposit-total">
                <div>小計</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div>-</div>
                <div class="asset-number">{{ totalSubLimitApproved }}</div>
                <div class="asset-number">{{ totalLoanBalanceTwd }}</div>
              </div>
            </section>
          </div>
        </div>
        </article>
      </div>

      <!-- 信用卡 -->
      <div v-show="viewState.activeMainTab === MAIN_TAB.CREDIT" class="tab-panel" data-panel="MAIN_TAB.CREDIT">
        <article class="info-card panel-card">
          <div v-if="isConsumptionLoading" class="customer-advance-status">資料載入中</div>
          <div v-else-if="consumptionError || !customerCredit" class="customer-advance-status">
            系統無法取得資訊
          </div>
          <div v-else class="credit-sections">
            <section class="stock-table-card deposit-table asset-detail-table credit-table">
              <div class="stock-table-title"><span>信用卡總覽</span></div>
              <div class="stock-row stock-head credit-bill-row">
                <div>帳單月份</div>
                <div>應繳金額</div>
                <div>循環餘額</div>
                <div>最低應繳</div>
                <div>付費分期餘額</div>
                <div>自動扣款</div>
                <div>總歸戶額度</div>
                <div>已用額度</div>
                <div>可用額度</div>
              </div>
              <div class="stock-row credit-bill-row">
                <div>{{ customerCredit.creditCardStatement.BYM6 || '-' }}</div>
                <div class="credit-number">{{ customerCredit.creditCardStatement.TAMT || '-' }}</div>
                <div class="credit-number">{{ customerCredit.creditCardStatement.tm_rev_int_bal || '-' }}</div>
                <div class="credit-number">{{ customerCredit.creditCardStatement.MIMPY || '-' }}</div>
                <div class="credit-number">{{ customerCredit.creditCardStatement.remaining_amount || '-' }}</div>
                <div class="credit-center">
                  {{ customerCredit.accountOverview.twdAndFx_auto_debit_ind || '-' }}
                </div>
                <div class="credit-number">{{ customerCredit.accountOverview.bill_limit || '-' }}</div>
                <div class="credit-number">{{ customerCredit.accountOverview.used_limit || '-' }}</div>
                <div class="credit-number">{{ customerCredit.accountOverview.available_bal || '-' }}</div>
              </div>
            </section>

            <div class="credit-detail-grid">
              <section class="stock-table-card deposit-table asset-detail-table credit-table">
                <div class="stock-table-title"><span>持卡明細</span></div>
                <div class="stock-row stock-head credit-card-row">
                  <div>卡別名稱</div>
                  <div>類型</div>
                  <div>卡片等級</div>
                  <div>正附卡</div>
                  <div>已開卡</div>
                </div>
                <div v-for="card in customerCredit.cardDetail"
                  :key="`${card.coBrandedName}-${card.cardBrandName}-${card.isPrimaryCard}`"
                  class="stock-row credit-card-row">
                  <div class="stock-name">{{ card.coBrandedName || '-' }}</div>
                  <div class="credit-center">{{ card.cardType || '-' }}</div>
                  <div class="credit-center">{{ card.cardBrandName || '-' }}</div>
                  <div class="credit-center">{{ card.isPrimaryCard || '-' }}</div>
                  <div class="credit-center">{{ card.activatedCd || '-' }}</div>
                </div>
                <div v-if="customerCredit.cardDetail.length === 0" class="stock-row credit-card-row">
                  <div class="credit-center">-</div>
                  <div class="credit-center">-</div>
                  <div class="credit-center">-</div>
                  <div class="credit-center">-</div>
                  <div class="credit-center">-</div>
                </div>
              </section>

              <section class="stock-table-card deposit-table asset-detail-table credit-table credit-radar-card">
                <div class="stock-table-title"><span>信用卡意圖模型雷達圖</span></div>
                <div class="credit-radar-wrap">
                  <radar-chart aria-label="信用卡意圖模型雷達圖" :labels="creditIntentRadar.labels"
                    :customer-data="creditIntentRadar.customerData" :cluster-data="creditIntentRadar.clusterData"
                    wrapper-class="credit-radar-canvas" />
                </div>
              </section>
            </div>
          </div>
        </article>
      </div>

      <!-- 互動紀錄 -->
      <div v-show="viewState.activeMainTab === MAIN_TAB.INTERACTION" class="tab-panel"
        data-panel="MAIN_TAB.INTERACTION">
        <article class="info-card panel-card">
          <div v-if="interactionError" class="customer-advance-status">系統無法取得資訊</div>
          <div v-else class="stock-table-card interaction-table">
            <div class="stock-table-title"><span>近六個月互動時間軸</span></div>

            <div class="interaction-month-filter" aria-label="互動紀錄月份篩選">
              <button v-for="month in availableMonths" :key="month" class="interaction-month-btn"
                :class="{ active: viewState.activeMonths.includes(month) }" type="button" @click="toggleMonth(month)">
                {{ month }}
              </button>
            </div>

            <div class="interaction-attribute-filter" aria-label="互動紀錄大類篩選">
              <button v-for="cat in availableCategories" :key="cat" class="interaction-attribute-btn" :class="{
                active:
                  cat === '全部'
                    ? viewState.activeCategories.length === 0
                    : viewState.activeCategories.includes(cat),
              }" type="button" @click="toggleCategory(cat)">
                {{ cat }}
              </button>
            </div>

            <div class="interaction-timeline-wrap">
              <div v-if="finalDisplayInteractions.length === 0"
                style="padding: 30px; text-align: center; color: #666; margin-left: -116px">
                此區間無符合條件的互動紀錄
              </div>
              <div v-else class="interaction-axis active">
                <div v-for="item in finalDisplayInteractions" :key="item.icrmInteractions.uuid"
                  class="interaction-event" :class="{
                    'is-selected': selectedInteraction === item,
                  }" style="cursor: pointer" @click="selectInteraction(item)">
                  <div class="interaction-time">
                    <strong>{{ formatInteractionDate(item.uiFields.interaction_time).date }}</strong>
                    <span>{{ formatInteractionDate(item.uiFields.interaction_time).time }}</span>
                  </div>
                  <div class="interaction-dot"></div>
                  <div class="interaction-bubble">
                    <div class="interaction-bubble-main">
                      <span class="interaction-pill">{{ item.uiFields.category || '系統紀錄' }}</span>
                      <strong>{{ item.uiFields.title }}</strong>
                      <span v-if="item.uiFields.status" class="interaction-status-cell">
                        <span class="interaction-pill" :class="getStatusClass(item.uiFields.status)">
                          {{ item.uiFields.status }}
                        </span>
                      </span>
                    </div>
                    <div class="interaction-detail" :class="getStatusClass(item.uiFields.status)">
                      <template v-for="(line, idx) in splitContent(item.uiFields.content)" :key="idx">
                        {{ line }}<br v-if="idx < splitContent(item.uiFields.content).length - 1" />
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
  </section>

  <!-- 3. 右側：改用 stock-table-card，並增加 interaction-detail-body 包住內文 -->
  <aside class="stock-table-card interaction-detail-card">
    <div class="stock-table-title"><span>互動詳細資訊</span></div>

    <!-- 用這個新的 div 來取代原本寫在卡片上的 padding -->
    <div class="interaction-detail-body">
      <template v-if="selectedInteraction">
        <div class="interaction-detail-summary">
          <span class="interaction-pill">{{ selectedInteraction.uiFields.category || '系統紀錄' }}</span>
          <strong class="interaction-detail-title">{{ selectedInteraction.uiFields.title }}</strong>
        </div>

        <div class="interaction-detail-meta">
          <div class="meta-row">
            <span>日期與時間：</span>
            <span class="meta-time">
              {{ formatDetailDateTime(selectedInteraction.uiFields.interaction_time) }}
            </span>
          </div>
        </div>

        <section class="interaction-detail-content">
          <div class="section-title">互動內文</div>
          <ul>
            <li v-for="(line, idx) in splitContent(selectedInteraction.uiFields.content)" :key="idx">
              {{ line }}
            </li>
          </ul>
        </section>
      </template>
    </div>
  </aside>
  </div>
  </article>
  </div>
  </section>
  </main>
  </div>
  </section>
</template>

<style scoped>
.customer-advance-root {
  position: relative;
  width: 100%;
  min-height: 100vh;
  margin: 0 !important;
  padding: 0 !important;
  background: var(--color-bg-global);
}

.customer-advance-root .customer-page {
  width: 100%;
  min-width: 0;
  margin: 0 !important;
  padding: 0 !important;
  overflow-x: hidden;
}

.customer-advance-root .page-shell {
  width: 100%;
  margin: 0 !important;
  position: static;
  transform: none;
  box-sizing: border-box;
  grid-template-columns: 310px minmax(0, 1fr);
  padding: 16px 22px 30px;
}

.customer-advance-back {
  position: fixed;
  top: 7px;
  right: 20px;
  z-index: 1001;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  min-height: 40px;
  padding: 0 12px 0 10px;
  border: 1px solid #bfdae1;
  border-radius: 999px;
  background: #ecfaf7;
  color: #2a7e70;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 6px 16px rgba(42, 126, 112, 0.18);
  cursor: pointer;
}

.customer-advance-back svg {
  width: 18px;
  height: 18px;
}

.customer-advance-status {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  color: #3a4b5f;
  font-size: 16px;
}

.tab.is-disabled {
  opacity: 0.55;
}

.mid-text>div {
  display: block;
  text-align: center;
}

.warning {
  color: #ff0000;
  font-size: 18px;
  font-weight: bold;
  line-height: 1.5;
  padding-bottom: 16px;
}
</style>