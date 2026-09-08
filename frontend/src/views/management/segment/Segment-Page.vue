<template>
    <div class="segment-detail-wrapper">
        <!-- 載入中狀態 -->
        <div v-if="loading" class="loading-container">
            <div class="loading-text">載入客群詳情中...</div>
        </div>

        <!-- 錯誤處理狀態 -->
        <div v-else-if="!segment" class="error-container">
            <div class="error-text">客群資訊不存在</div>
        </div>

        <!-- 主要看板資料 -->
        <div v-else class="board">

            <!-- 第一縱列 (col-2): 包含 AuM 與 LuM 的圓環圖表 -->
            <section class="col col-2">

                <!-- 1. AuM 資產管理規模卡片 -->
                <article v-if="formattedAumSegment" class="card r20 pad12 chart-card aum-card">
                    <div class="card-head">
                        <div class="card-title">
                            AuM <span class="inline-unit">單位：萬元</span><span class="ic-info"></span>
                        </div>
                    </div>
                    <!-- 圓環圖元件 (Radial) -->
                    <radial aria-label="資產管理規模分布" :categories="formattedAumSegment.categories"
                        :rings="[formattedAumSegment.ring1, formattedAumSegment.ring2]" :max="aumScale.max"
                        :tick-step="aumScale.tickStep" :minor-step="aumScale.minorStep" :sweep-deg="270"
                        active-ring-key="ring1" radial-class="radial-host" legend-class="aum-legend-container" />
                </article>

                <!-- 2. LuM 貸款和信用卡分期卡片 -->
                <article v-if="formattedLumSegment" class="card r20 pad12 chart-card lum-card">
                    <div class="card-head">
                        <div class="card-title">
                            LuM+信用卡 <span class="inline-unit">單位：萬元</span><span class="ic-info"></span>
                        </div>
                    </div>
                    <!-- 圓環圖元件 (Radial) -->
                    <radial aria-label="貸款和信用卡分布" :categories="formattedLumSegment.categories"
                        :rings="[formattedLumSegment.ring1, formattedLumSegment.ring2]" :max="lumScale.max"
                        :tick-step="lumScale.tickStep" :minor-step="lumScale.minorStep" :sweep-deg="270"
                        active-ring-key="ring1" radial-class="radial-host" legend-class="lum-legend-container" />
                </article>
            </section>

            <!-- 第二縱列 (col-3): 包含財富潛力雷達圖 -->
            <section class="col col-3">
                <!-- 財富潛力卡片 -->
                <article class="card r20 pad12 potential-card" @click="togglePotentialPopover">
                    <div class="card-head">
                        <div class="card-title">財富潛力 <span class="ic-info"></span></div>
                    </div>
                    <!-- 雷達圖元件 (Radar) -->
                    <radar v-if="wealthBarChartData" aria-label="財富潛力" :labels="wealthBarChartData.labels"
                        :customer-data="wealthBarChartData.customerData" :cluster-data="wealthBarChartData.clusterData"
                        :customer-tooltips="wealthBarChartData.customerTooltips"
                        :cluster-tooltips="wealthBarChartData.clusterTooltips" :max="wealthBarChartData.yMax"
                        customer-label="客群Top10%" cluster-label="客群" wrapper-class="mini-chart" :show-legend="false" />
                </article>
            </section>

        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSegmentDetailCache } from '@/composables/use-segment-detail'
import Radar from '@/components/charts/radar.vue'
import Radial from '@/components/charts/radial.vue'

// 樣式表引入 (還原圖2)
import '@/assets/styles/customer/base.css'
import '@/assets/styles/customer/customer.css'
import '@/assets/styles/customer/segment.css'

const route = useRoute()

// 從路由參數取得客群代碼
const segmentCode = computed(() => {
    const code = route.params.segmentCode
    return typeof code === 'string' ? code : (code?.[0] ?? '')
})

// 透過快取機制取得詳情資料
const { segment, loading } = useSegmentDetailCache(segmentCode.value)

// 彈窗與卡片控制狀態
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

function togglePotentialPopover() {
    const next = !potentialPopoverOpen.value
    closeAllPopovers()
    potentialPopoverOpen.value = next
}

// 3.3.3 AuM 與 LuM 圓環圖配色盤
const AUM_COLORS = ['#2B6CB0', '#319795', '#48BB78', '#ECC94B', '#ED8936', '#E53E3E', '#805AD5']
const LUM_COLORS = ['#C53030', '#DD6B20', '#D69E2E', '#38A169', '#3182CE', '#805AD5']

// 格式化 AuM 圓環圖資料（元轉萬元並補上色彩）
const formattedAumSegment = computed(() => {
    const aum = segment.value?.aumSegment
    if (!aum) return null

    return {
        ring1: {
            ...aum.ring1,
            total: Math.round(aum.ring1.total / 10000),
            values: aum.ring1.values.map((v: number) => Math.round(v / 10000))
        },
        ring2: {
            ...aum.ring2,
            total: Math.round(aum.ring2.total / 10000),
            values: aum.ring2.values.map((v: number) => Math.round(v / 10000))
        },
        categories: aum.categories.map((c: any, i: number) => ({
            ...c,
            color: AUM_COLORS[i % AUM_COLORS.length]
        }))
    }
})

// 格式化 LuM 圓環圖資料（元轉萬元並補上色彩）
const formattedLumSegment = computed(() => {
    const lum = segment.value?.lumSegment
    if (!lum) return null

    return {
        ring1: {
            ...lum.ring1,
            total: Math.round(lum.ring1.total / 10000),
            values: lum.ring1.values.map((v: number) => Math.round(v / 10000))
        },
        ring2: {
            ...lum.ring2,
            total: Math.round(lum.ring2.total / 10000),
            values: lum.ring2.values.map((v: number) => Math.round(v / 10000))
        },
        categories: lum.categories.map((c: any, i: number) => ({
            ...c,
            color: LUM_COLORS[i % LUM_COLORS.length]
        }))
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
        minorStep: Math.max(1, Math.round(tickStep / 5))
    }
}

// 計算 AuM 刻度
const aumScale = computed(() => {
    const aum = formattedAumSegment.value
    if (!aum) return { max: 600, tickStep: 100, minorStep: 20 }
    return calculateDynamicScale(Math.max(aum.ring1.total, aum.ring2.total))
})

// 計算 LuM 刻度
const lumScale = computed(() => {
    const lum = formattedLumSegment.value
    if (!lum) return { max: 2500, tickStep: 500, minorStep: 100 }
    return calculateDynamicScale(Math.max(lum.ring1.total, lum.ring2.total))
})

// 3.3.5 財富潛力評級雷達圖數據模型轉換
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
            wp.overall.dividend?.label ?? ''
        ],
        clusterData: [
            wp.overall.annualIncome?.score ?? 1,
            wp.overall.taxes?.score ?? 1,
            wp.overall.creditLimit?.score ?? 1,
            wp.overall.annualSpending?.score ?? 1,
            wp.overall.realEstate?.score ?? 1,
            wp.overall.dividend?.score ?? 1
        ],
        customerData: wp.t10
            ? [
                wp.t10.annualIncome?.score ?? 1,
                wp.t10.taxes?.score ?? 1,
                wp.t10.creditLimit?.score ?? 1,
                wp.t10.annualSpending?.score ?? 1,
                wp.t10.realEstate?.score ?? 1,
                wp.t10.dividend?.score ?? 1
            ]
            :,
        clusterTooltips: [
            wp.overall.annualIncome?.displayValue ?? '',
            wp.overall.taxes?.displayValue ?? '',
            wp.overall.creditLimit?.displayValue ?? '',
            wp.overall.annualSpending?.displayValue ?? '',
            wp.overall.realEstate?.tooltipText ?? '',
            wp.overall.dividend?.displayValue ?? ''
        ],
        customerTooltips: wp.t10
            ? [
                wp.t10.annualIncome?.displayValue ?? '',
                wp.t10.taxes?.displayValue ?? '',
                wp.t10.creditLimit?.displayValue ?? '',
                wp.t10.annualSpending?.displayValue ?? '',
                wp.t10.realEstate?.tooltipText ?? '',
                wp.t10.dividend?.displayValue ?? ''
            ]
            : [],
        yMax: 3,
        stepSize: 1
    }
})
</script>
