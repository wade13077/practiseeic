<script setup lang="ts">
import '@assets/styles/customer/base.css'
import '@assets/styles/customer/customer.css'
import '@assets/styles/customer/segment.css'

import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSegmentDetailCache } from '@/composables/use-segment-detail'
import Bar from '@/components/charts/bar.vue'
import MemberOperationChart from '@/components/charts/member-operation-chart.vue'
import Radar from '@/components/charts/radar.vue'
import Radial from '@/components/charts/radial.vue'

const route = useRoute()

// 從路由參數取得客群代碼
const segmentCode = computed(() => {
  const code = route.params.segmentCode
  return typeof code === 'string' ? code : (code?.[0] ?? '')
})

// 取得詳情資料
const { segment, loading } = useSegmentDetailCache(segmentCode.value)

// 彈出卡片控制
const memberPopoverOpen = ref(false)
const potentialPopoverOpen = ref(false)
const productBehaviorPopoverOpen = ref(false)
const productIntentAssetPopoverOpen = ref(false)
const channelBehaviorPopoverOpen = ref(false)
const digitalBehaviorPopoverOpen = ref(false)

function closeAllPopovers() {
  memberPopoverOpen.value = false
  potentialPopoverOpen.value = false
  productBehaviorPopoverOpen.value = false
  productIntentAssetPopoverOpen.value = false
  channelBehaviorPopoverOpen.value = false
  digitalBehaviorPopoverOpen.value = false
}

function toggleMemberPopover() {
  const next = !memberPopoverOpen.value
  closeAllPopovers()
  memberPopoverOpen.value = next
}

function closeMemberPopover() {
  memberPopoverOpen.value = false
}

function togglePotentialPopover() {
  const next = !potentialPopoverOpen.value
  closeAllPopovers()
  potentialPopoverOpen.value = next
}

function toggleProductBehaviorPopover() {
  const next = !productBehaviorPopoverOpen.value
  closeAllPopovers()
  productBehaviorPopoverOpen.value = next
}

// 格式化整數數字
function formatNumber(num: number | undefined): string {
  if (num === undefined || num === null) return '0'
  return Math.round(num).toLocaleString('zh-TW')
}

// 格式化金額為萬元
function formatWan(amount: number | undefined): string {
  if (amount === undefined || amount === null) return '0'
  return (amount / 10000).toLocaleString('zh-TW', { maximumFractionDigits: 1 })
}

/**
 * 百分比圖表動態 Y 軸計算
 * - 最大值 <= 100%: 固定 yMax = 100, stepSize = 20 (5個區間)
 * - 最大值 > 100%: 依最大值向上取整，動態產生 4~5 個整數區間
 */
function calculatePercentScale(values: (number | undefined | null)[]) {
  const validValues = values.filter((v): v is number => typeof v === 'number' && !isNaN(v))
  const maxVal = Math.max(0, ...validValues)

  // 1. 小於等於 100%: 固定 0% ~ 100%
  if (maxVal <= 100) {
    return { yMax: 100, stepSize: 20 }
  }

  // 2. 超過 100%: 動態計算 stepSize 與 yMax (維持 4~5 個刻度)
  const rawStep = maxVal / 5
  let stepSize = 20

  if (rawStep > 50) stepSize = Math.ceil(rawStep / 50) * 50
  else if (rawStep > 25) stepSize = 50
  else if (rawStep > 20) stepSize = 25
  else stepSize = Math.ceil(rawStep / 10) * 10

  const yMax = Math.ceil(maxVal / stepSize) * stepSize

  return { yMax, stepSize }
}

// 3.3.3 AuM 與 LuM 圓環圖配色盤
const AUM_COLORS = ['#FFD640', '#FFEF91', '#FF9900', '#EF4343', '#FFA099', '#E62978']
const LUM_COLORS = ['#87E3BF', '#009973', '#FFC8C2', '#00735A', '#EF4343', '#FC746F']

// 格式化 AuM 圓環 (元轉萬元並補上色彩)
const formattedAumSegment = computed(() => {
  const aum = segment.value?.aumSegment
  if (!aum) return null

  return {
    ring1: {
      ...aum.ring1,
    },
    ring2: {
      ...aum.ring2,
    },
    categories: aum.categories.map((c, i) => ({
      ...c,
      color: AUM_COLORS[i % AUM_COLORS.length],
    })),
  }
})

// 格式化 LuM 圓環 (元轉萬元並補上色彩)
const formattedLumSegment = computed(() => {
  const lum = segment.value?.lumSegment
  if (!lum) return null

  return {
    ring1: {
      ...lum.ring1,
    },
    ring2: {
      ...lum.ring2,
    },
    categories: lum.categories.map((c, i) => ({
      ...c,
      color: LUM_COLORS[i % LUM_COLORS.length],
    })),
  }
})

// 3.3.3 自適應動態刻度演算法
function calculateDynamicScale(maxVal: number) {
  const max = Math.max(maxVal, 1)
  const rawStep = max / 3.5
  const power = Math.pow(10, Math.floor(Math.log10(rawStep)))
  const fraction = rawStep / power

  let niceFraction = 1
  if (fraction > 5) niceFraction = 10
  else if (fraction > 2) niceFraction = 5
  else if (fraction > 1) niceFraction = 2

  const tickStep = Math.max(1, Math.round(niceFraction * power))

  return {
    max,
    tickStep,
    minorStep: Math.max(1, Math.round(tickStep / 5)),
  }
}

const aumScale = computed(() => {
  const aum = formattedAumSegment.value
  if (!aum) return { max: 600, tickStep: 100, minorStep: 20 }
  return calculateDynamicScale(Math.max(aum.ring1.total, aum.ring2.total))
})

const lumScale = computed(() => {
  const lum = formattedLumSegment.value
  if (!lum) return { max: 2500, tickStep: 500, minorStep: 100 }
  return calculateDynamicScale(Math.max(lum.ring1.total, lum.ring2.total))
})

// 3.3.5 財富潛力評級雷達圖數據
const wealthBarChartData = computed(() => {
  const wp = segment.value?.wealthPotential
  if (!wp || !wp.overall) return null

  return {
    labels: [
      wp.overall.annualIncome?.label ?? '',
      wp.overall.taxes?.label ?? '',
      wp.overall.creditLimit?.label ?? '',
      wp.overall.annualSpending?.label ?? '',
      wp.overall.realEstate?.label ?? '',
      wp.overall.dividend?.label ?? '',
    ],
    clusterData: [
      wp.overall.annualIncome?.score ?? 1,
      wp.overall.taxes?.score ?? 1,
      wp.overall.creditLimit?.score ?? 1,
      wp.overall.annualSpending?.score ?? 1,
      wp.overall.realEstate?.score ?? 1,
      wp.overall.dividend?.score ?? 1,
    ],
    customerData: wp.t10
      ? [
          wp.t10.annualIncome?.score ?? 1,
          wp.t10.taxes?.score ?? 1,
          wp.t10.creditLimit?.score ?? 1,
          wp.t10.annualSpending?.score ?? 1,
          wp.t10.realEstate?.score ?? 1,
          wp.t10.dividend?.score ?? 1,
        ]
      : [1, 1, 1, 1, 1, 1],
    clusterTooltips: [
      wp.overall.annualIncome?.displayValue ?? '',
      wp.overall.taxes?.displayValue ?? '',
      wp.overall.creditLimit?.displayValue ?? '',
      wp.overall.annualSpending?.displayValue ?? '',
      wp.overall.realEstate?.tooltipText ?? '',
      wp.overall.dividend?.displayValue ?? '',
    ],
    customerTooltips: wp.t10
      ? [
          wp.t10.annualIncome?.displayValue ?? '',
          wp.t10.taxes?.displayValue ?? '',
          wp.t10.creditLimit?.displayValue ?? '',
          wp.t10.annualSpending?.displayValue ?? '',
          wp.t10.realEstate?.tooltipText ?? '',
          wp.t10.dividend?.displayValue ?? '',
        ]
      : [],
    yMax: 3,
    stepSize: 1,
  }
})

// 3.3.6 產品行為長條圖數據
const productBehaviorChartData = computed(() => {
  const pb = segment.value?.productBehavior
  if (!pb) return null

  return {
    labels: [
      pb.loanDepositRatio?.label ?? '',
      pb.activeDepositRatio?.label ?? '',
      pb.wealthAumRatio?.label ?? '',
    ],
    clusterData: [
      (pb.loanDepositRatio?.overall ?? 0) * 100,
      (pb.activeDepositRatio?.overall ?? 0) * 100,
      (pb.wealthAumRatio?.overall ?? 0) * 100,
    ],
    customerData: [
      (pb.loanDepositRatio?.t10 ?? 0) * 100,
      (pb.activeDepositRatio?.t10 ?? 0) * 100,
      (pb.wealthAumRatio?.t10 ?? 0) * 100,
    ],
    yMax: 100,
    stepSize: 20,
  }
})

// 3.3.6 產品行為左圖（存貸比）
const productBehaviorLeftChartData = computed(() => {
  const pb = segment.value?.productBehavior
  if (!pb) return null

  const customerVal = (pb.loanDepositRatio?.t10 ?? 0) * 100
  const clusterVal = (pb.loanDepositRatio?.overall ?? 0) * 100

  // 傳入顧客與客群數值動態計算刻度
  const { yMax, stepSize } = calculatePercentScale([customerVal, clusterVal])

  return {
    labels: [pb.loanDepositRatio?.label ?? ''],
    customerData: [customerVal],
    clusterData: [clusterVal],
    yMax,
    stepSize,
  }
})

// 3.3.6 產品行為右圖（活存比、理財AuM比）
const productBehaviorRightChartData = computed(() => {
  const pb = segment.value?.productBehavior
  if (!pb) return null

  const activeCustomer = (pb.activeDepositRatio?.t10 ?? 0) * 100
  const activeCluster = (pb.activeDepositRatio?.overall ?? 0) * 100
  const wealthCustomer = (pb.wealthAumRatio?.t10 ?? 0) * 100
  const wealthCluster = (pb.wealthAumRatio?.overall ?? 0) * 100

  // 傳入右側四個數值動態計算刻度
  const { yMax, stepSize } = calculatePercentScale([
    activeCustomer,
    activeCluster,
    wealthCustomer,
    wealthCluster,
  ])

  return {
    labels: [
      pb.activeDepositRatio?.label ?? '',
      pb.wealthAumRatio?.label ?? '',
    ],
    customerData: [activeCustomer, wealthCustomer],
    clusterData: [activeCluster, wealthCluster],
    yMax,
    stepSize,
  }
})

// 3.3.7 產品意圖（資產類）管道圖數據
const productIntentAssetChartData = computed(() => {
  const pia = segment.value?.productIntentAsset
  if (!pia || !pia.overall) return null

  return {
    labels: [
      pia.overall.bondFund?.label ?? '',
      pia.overall.equityFund?.label ?? '',
      pia.overall.balancedFund?.label ?? '',
      pia.overall.savingsInsurance?.label ?? '',
      pia.overall.protectionInsurance?.label ?? '',
      pia.overall.foreignBond?.label ?? '',
    ],
    clusterData: [
      (pia.overall.bondFund?.ratio ?? 0) * 100,
      (pia.overall.equityFund?.ratio ?? 0) * 100,
      (pia.overall.balancedFund?.ratio ?? 0) * 100,
      (pia.overall.savingsInsurance?.ratio ?? 0) * 100,
      (pia.overall.protectionInsurance?.ratio ?? 0) * 100,
      (pia.overall.foreignBond?.ratio ?? 0) * 100,
    ],
    customerData: pia.t10
      ? [
          (pia.t10.bondFund?.ratio ?? 0) * 100,
          (pia.t10.equityFund?.ratio ?? 0) * 100,
          (pia.t10.balancedFund?.ratio ?? 0) * 100,
          (pia.t10.savingsInsurance?.ratio ?? 0) * 100,
          (pia.t10.protectionInsurance?.ratio ?? 0) * 100,
          (pia.t10.foreignBond?.ratio ?? 0) * 100,
        ]
      : [0, 0, 0, 0, 0, 0],
    clusterTooltips: [
      `高意圖顧客佔比:${((pia.overall.bondFund?.ratio ?? 0) * 100).toFixed(2)}%`,
      `高意圖顧客佔比:${((pia.overall.equityFund?.ratio ?? 0) * 100).toFixed(2)}%`,
      `高意圖顧客佔比:${((pia.overall.balancedFund?.ratio ?? 0) * 100).toFixed(2)}%`,
      `高意圖顧客佔比:${((pia.overall.savingsInsurance?.ratio ?? 0) * 100).toFixed(2)}%`,
      `高意圖顧客佔比:${((pia.overall.protectionInsurance?.ratio ?? 0) * 100).toFixed(2)}%`,
      `高意圖顧客佔比:${((pia.overall.foreignBond?.ratio ?? 0) * 100).toFixed(2)}%`,
    ],
    customerTooltips: pia.t10
      ? [
          `高意圖顧客佔比:${((pia.t10.bondFund?.ratio ?? 0) * 100).toFixed(2)}%`,
          `高意圖顧客佔比:${((pia.t10.equityFund?.ratio ?? 0) * 100).toFixed(2)}%`,
          `高意圖顧客佔比:${((pia.t10.balancedFund?.ratio ?? 0) * 100).toFixed(2)}%`,
          `高意圖顧客佔比:${((pia.t10.savingsInsurance?.ratio ?? 0) * 100).toFixed(2)}%`,
          `高意圖顧客佔比:${((pia.t10.protectionInsurance?.ratio ?? 0) * 100).toFixed(2)}%`,
          `高意圖顧客佔比:${((pia.t10.foreignBond?.ratio ?? 0) * 100).toFixed(2)}%`,
        ]
      : [],
    yMax: 100,
    stepSize: 100 / 3,
  }
})
</script>

<template>
  <div class="segment-detail-wrapper">
    <div v-if="loading" class="loading-container">
      <div class="loading-text">載入客群詳情中...</div>
    </div>

    <div v-else-if="!segment" class="error-container">
      <div class="error-text">客群資訊不存在</div>
    </div>

    <div v-else class="board">
      <section class="col col-1">
        <article class="card r20 customer-card segment-empty-customer-card">
          <div class="customer-band">{{ segment.code }}-{{ segment.name }}</div>
          <div class="segment-customer-profile">
            <div class="segment-avatar" aria-hidden="true">
              <svg viewBox="0 0 64 64">
                <circle cx="32" cy="24" r="12" />
                <path d="M12 54c3.8-12.5 11.1-18.8 20-18.8S48.2 41.5 52 54" />
                <path d="M22 24c2.8-5.4 6.1-8.1 10-8.1s7.2 2.7 10 8.1" />
              </svg>
            </div>
            <div class="segment-customer-operation">
              <div class="remind-t">經營洞見：</div>
              <div class="remind-d">{{ segment.managementInsight }}</div>
            </div>
          </div>
        </article>
      </section>

      <section class="col col-2">
        <article v-if="formattedAumSegment" class="card r20 pad12 chart-card aum-card">
          <div class="card-head">
            <div class="card-title">AuM <span class="inline-unit">單位：萬元</span><span class="ic-info"></span></div>
          </div>
          <radial
            aria-label="資產管理規模分布"
            :categories="formattedAumSegment.categories"
            :rings="[formattedAumSegment.ring1, formattedAumSegment.ring2]"
            :max="aumScale.max"
            :tick-step="aumScale.tickStep"
            :minor-step="aumScale.minorStep"
            :sweep-deg="270"
            active-ring-key="ring1"
            radial-class="radial-host"
            legend-class="aum-legend-container"
          />
        </article>

        <article v-if="formattedLumSegment" class="card r20 pad12 chart-card lum-card">
          <div class="card-head">
            <div class="card-title">
              LuM+信用卡 <span class="inline-unit">單位：萬元</span><span class="ic-info"></span>
            </div>
          </div>
          <radial
            aria-label="貸款和信用卡分布"
            :categories="formattedLumSegment.categories"
            :rings="[formattedLumSegment.ring1, formattedLumSegment.ring2]"
            :max="lumScale.max"
            :tick-step="lumScale.tickStep"
            :minor-step="lumScale.minorStep"
            :sweep-deg="270"
            active-ring-key="ring1"
            radial-class="radial-host"
            legend-class="lum-legend-container"
          />
        </article>
      </section>

      <section class="col col-3">
        <article class="card r20 pad12 member-operation-card" @click="toggleMemberPopover">
          <div class="card-head">
            <div class="card-title">會員經營 <span class="ic-info"></span></div>
          </div>
        <member-operation-chart
          :rows="segment.memberDistribution?.distributions ?? []"
          :potential-count="segment.memberDistribution?.potentialWmMemberCount"
        />
        <transition name="fade">
          <div v-if="memberPopoverOpen" class="member-popover active" @click.stop="memberPopoverOpen = true">
            <button class="wealth-popover-close" @click.stop="closeMemberPopover">&times;</button>
            <div class="wealth-popover-title">會員經營指標說明</div>
            <div class="wealth-popover-divider"></div>
            <div class="wealth-popover-grid" style="display: block; margin-top: 16px">
              <div style="font-size: 13px; color: #666666; margin-bottom: 20px; text-align: left; line-height: 1.5">
                針對具會員身份的顧客依資產總值區分為潛在降等/維持戶及潛力升等等統計戶數，各會員等級標註之門檻設定如下：
              </div>
              <div class="wealth-popover-grid-inner" style="display: grid; grid-template-columns: 1fr 1fr; column-gap: 40px; row-gap: 20px">
                <div class="wealth-indicator-col">
                  <div class="indicator-block">
                    <div class="indicator-header">
                      <span class="indicator-icon">&gt;</span>
                      <span class="indicator-name">極致/私銀</span>
                    </div>
                    <div class="indicator-desc">僅呈現極致/私銀戶數</div>
                  </div>
                  <div class="indicator-block">
                    <div class="indicator-header">
                      <span class="indicator-icon">&gt;</span>
                      <span class="indicator-name">登峰</span>
                    </div>
                    <div class="indicator-desc">潛在降等: 前三個月綜合價值皆小於3,300萬</div>
                  </div>
                </div>
                <div class="wealth-indicator-col">
                  <div class="indicator-block">
                    <div class="indicator-header">
                      <span class="indicator-icon">&gt;</span>
                      <span class="indicator-name">菁英</span>
                    </div>
                    <div class="indicator-desc">
                      (1) 潛在降等: 前三個月綜合價值皆小於1,200萬<br />
                      (2) 潛力升等: 前月綜合價值大於2,800萬
                    </div>
                  </div>
                  <div class="indicator-block">
                    <div class="indicator-header">
                      <span class="indicator-icon">&gt;</span>
                      <span class="indicator-name">新富</span>
                    </div>
                    <div class="indicator-desc">
                      (1) 潛在降等: 前三個月綜合價值皆小於350萬<br />
                      (2) 潛力升等: 前月綜合價值大於900萬
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </article>

      <article class="card r20 pad12 potential-card" @click="togglePotentialPopover">
        <div class="card-head">
          <div class="card-title">財富潛力 <span class="ic-info"></span></div>
        </div>
        <radar
          v-if="wealthBarChartData"
          aria-label="財富潛力"
          :labels="wealthBarChartData.labels"
          :customer-data="wealthBarChartData.customerData"
          :cluster-data="wealthBarChartData.clusterData"
          :customer-tooltips="wealthBarChartData.customerTooltips"
          :cluster-tooltips="wealthBarChartData.clusterTooltips"
          :max="wealthBarChartData.yMax"
          customer-label="客群Top10%"
          cluster-label="客群"
          wrapper-class="mini-chart"
          :show-legend="false"
        />
      </article>

      <article
        v-if="productBehaviorChartData"
        class="card r20 pad12 grow-chart product-behavior-card"
        @click="toggleProductBehaviorPopover"
      >
        <div class="card-head">
          <div class="card-title">投資產品行為 <span class="ic-info"></span></div>
        </div>
        <div class="product-rate-list">
          <div class="rate-row"><span class="rate-label">理專管戶率</span><span class="pct-tag">{{ ((segment.productBehavior?.faCoverageRatio ?? 0) * 100).toFixed(1) }}%</span></div>
          <div class="rate-row"><span class="rate-label">理財AUM週轉率</span><span class="pct-tag">{{ ((segment.productBehavior?.wmAumTurnoverRatio ?? 0) * 100).toFixed(1) }}%</span></div>
          <div class="rate-row"><span class="rate-label">理財顧客存量收益率</span><span class="pct-tag">{{ ((segment.productBehavior?.investmentYieldRate ?? 0) * 100).toFixed(1) }}%</span></div>
        </div>
        <div class="chart-box h190 product-behavior-split">
          <div class="product-behavior-split-left">
            <bar
              v-if="productBehaviorLeftChartData"
              aria-label="產品行為指標 (存貸比)"
              :labels="productBehaviorLeftChartData.labels"
              :customer-data="productBehaviorLeftChartData.customerData"
              :cluster-data="productBehaviorLeftChartData.clusterData"
              :y-max="productBehaviorLeftChartData.yMax"
              :step-size="productBehaviorLeftChartData.stepSize"
              customer-label="客群Top10%"
              cluster-label="客群"
              y-tick-suffix="%"
              wrapper-class="product-structure-panel-sm"
            />
          </div>
          <div class="product-behavior-split-right">
            <bar
              v-if="productBehaviorRightChartData"
              :show-y-axis="false"
              aria-label="產品行為指標 (活存比與理財AUM比)"
              :labels="productBehaviorRightChartData.labels"
              :customer-data="productBehaviorRightChartData.customerData"
              :cluster-data="productBehaviorRightChartData.clusterData"
              :y-max="productBehaviorRightChartData.yMax"
              :step-size="productBehaviorRightChartData.stepSize"
              customer-label="客群Top10%"
              cluster-label="客群"
              y-tick-suffix="%"
              wrapper-class="product-structure-panel-lg"
            />
          </div>
        </div>
      </article>

      <!-- 3.3.7 產品意圖-資產類 -->
      <article v-if="productIntentAssetChartData" class="card r20 pad12 product-intent-card">
        <div class="card-head">
          <div class="card-title">產品意圖-資產類 <span class="ic-info"></span></div>
        </div>
        <radar
          :labels="productIntentAssetChartData.labels"
          :customer-data="productIntentAssetChartData.customerData"
          :cluster-data="productIntentAssetChartData.clusterData"
          :customer-tooltips="productIntentAssetChartData.customerTooltips"
          :cluster-tooltips="productIntentAssetChartData.clusterTooltips"
          :max="productIntentAssetChartData.yMax"
          :step-size="productIntentAssetChartData.stepSize"
          tick-suffix="%"
          customer-label="客群Top10%"
          cluster-label="客群"
          wrapper-class="chart-box"
          :show-legend="false"
          aria-label="產品意圖-資產類"
        />
      </article>
    </section>
  </div>
</div>
</template>






