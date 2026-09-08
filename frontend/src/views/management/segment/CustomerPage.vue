<script setup lang="ts">
import '@/assets/styles/customer/base.css'
import '@/assets/styles/customer/customer.css'

import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

// 1. 從客群 Bean 檔案引入所有的強型別 Dto (完美連結你剛建立的檔案)
import type {
    SecureCustomerViewDto,
    CustomerDto,
    AuM,
    AuMData,
    LuM,
    LuMData,
    RadarChartViewData,
    RadarChartData,
    HighlightRadarChartViewData
} from '@/beans/customer-bean'

import type { RealTimeCustomerDto } from '@/beans/real-time-customer-bean'
import type { CustomerAdvanceViewDto } from '@/beans/customer-advance-bean'

// 2. 引入 API 請求層與子元件
import { getCustomer, getRealTimeCustomer } from '@/views/management/customer/customer-api'
import ChartCard from '@/components/charts/chart-card.vue'
import RadarChart from '@/components/charts/radar.vue'
import RadialChart from '@/components/charts/radial.vue'

// 3. 引入 Composable 商業邏輯切片
import { useCustomerAdvance } from '@/composables/use-customer-advance'
import { useCustomerList } from '@/composables/use-customer-list' // 依架構推導對應圖中的 useCustomerList
import CustomerAdvance from './customer-advance.vue'

const route = useRoute()
const router = useRouter()

// 4. 從第一個 Composable 中解構基本顧客清單狀態
const { isLoading, error, fetchSecureCustomerView } = useCustomerList()

// 5. 從第二個 Composable 中解構進階畫像與快取清除方法
const {
    isLoading: isCustomerAdvanceLoading,
    error: customerAdvanceError,
    clearCustomerAdvanceCache
} = useCustomerAdvance()

// 6. 初始化頁面響應式變數狀態機
const secureCustomerView = ref<SecureCustomerViewDto | null>(null)
const customerAdvanceData = ref<CustomerAdvanceViewDto | null>(null)
const customerInternalData = ref<CustomerDto | null>(null)
const customerRealTimeData = ref<RealTimeCustomerDto | null>(null)

const pageMessage = ref('')
const customerAdvanceMessage = ref('')
const viewMode = ref<'customer360' | 'customerAdvance'>('customer360')

const isUnmarketableAlertOpen = ref(false)
const initialTab = ref<'basic' | 'interaction' | null>(null)



// 監聽路由參數並換算流水號 (對應圖2)
const routeSerialNo = computed(() => {
    return typeof route.params.serialNo === 'string' ? route.params.serialNo : ''
})

// 返回第一層 (對應圖2)
const backToFirstLayer = (): void => {
    viewMode.value = 'customer360'
    const query = { ...route.query }
    delete query.showPotentialLevel
    void router.replace({ query })
}

// 監聽流水號：變更時清理前一個客群的快取 (對應圖2)
watch(routeSerialNo, (newSerialNo, oldSerialNo) => {
    if (!oldSerialNo || newSerialNo === oldSerialNo) {
        return
    }
    clearCustomerAdvanceCache(oldSerialNo)
    customerAdvanceData.value = null
    customerAdvanceMessage.value = ''
    viewMode.value = 'customer360'
})

// --- 同心圓環圖配置型別定義 (對應圖2) ---
type RadialChartConfig<T> = {
    ariaLabel: string
    radialClass: string
    legendClass: string
    categories: Array<{
        key: keyof T
        color: string
        label: string
        ratioList: Array<string>
        amountList: Array<number>
    }>
    rings: Array<{
        key: string
        label: string
        total: number
    }>
    max: number
    tickStep: number
    minorStep: number
}

// --- 雷達圖配置型別定義 (對應圖2) ---
type RadarChartConfig = {
    ariaLabel: string
    wrapperClass: string
    labels: string[]
    customerData: RadarChartData[] | HighlightRadarChartData[]
    clusterData: RadarChartData[] | HighlightRadarChartData[]
}

// --- AuM 圓環初始配置數據（對應圖3） ---
const aumRadialChart = ref<RadialChartConfig<AuMData>>({
    ariaLabel: 'AuM 同心圓',
    radialClass: 'radial-host',
    legendClass: 'aum-legend-container',
    max: 0,
    tickStep: 150,
    minorStep: 30,
    categories: [
        { key: 'demandDeposit', color: '#FFD640', label: '活儲+支票存款', ratioList: [], amountList: [] },
        { key: 'timeDeposit', color: '#FFEF91', label: '定存', ratioList: [], amountList: [] },
        { key: 'fund', color: '#FF9900', label: '基金', ratioList: [], amountList: [] },
        { key: 'financialProduct', color: '#EF4343', label: '金融商品', ratioList: [], amountList: [] },
        { key: 'insurance', color: '#FFA099', label: '保險', ratioList: [], amountList: [] },
        { key: 'financialAsset', color: '#E62978', label: '財金商品', ratioList: [], amountList: [] }
    ],
    rings: [
        { key: 'customer', label: '顧客', total: 0 },
        { key: 'avg', label: '客群', total: 0 },
        { key: 't10', label: 'Top10%', total: 0 }
    ]
})

// --- LuM 圓環初始配置數據（對應圖3） ---
const lumRadialChart = ref<RadialChartConfig<LuMData>>({
    ariaLabel: 'LuM 同心圓',
    radialClass: 'radial-host',
    legendClass: 'lum-legend-container',
    max: 0,
    tickStep: 600,
    minorStep: 120,
    categories: [
        { key: 'mtgPurchase', color: '#87E3BF', label: '房貸(購置)', ratioList: [], amountList: [] },
        { key: 'mtgRevolve', color: '#009973', label: '房貸(週轉金)', ratioList: [], amountList: [] },
        { key: 'collateral', color: '#FFC8C2', label: '副擔保', ratioList: [], amountList: [] },
        { key: 'creditLoan', color: '#00735A', label: '信貸', ratioList: [], amountList: [] },
        { key: 'finAssetLoan', color: '#EF4343', label: '金融資產融資(LL/PB)', ratioList: [], amountList: [] },
        { key: 'ccBalance', color: '#FC746F', label: '信用卡已使用額度', ratioList: [], amountList: [] }
    ],
    rings: [
        { key: 'customer', label: '顧客', total: 0 },
        { key: 'avg', label: '客群', total: 0 },
        { key: 't10', label: 'Top10%', total: 0 }
    ]
})

// --- 雷達圖群組初始配置數據（對應圖3、圖4、圖6） ---
const radarCharts = ref<Record<string, RadarChartConfig>>({
    wealthPotential: { ariaLabel: '財富潛力雷達圖', wrapperClass: 'mini-chart', labels: [], customerData: [], clusterData: [] },
    assetProduct: { ariaLabel: '資產產品雷達圖', wrapperClass: 'mini-chart', labels: [], customerData: [], clusterData: [] },
    liabilitiesAndFx: { ariaLabel: '負債與外匯雷達圖', wrapperClass: 'mini-chart', labels: [], customerData: [], clusterData: [] },
    accountRelationshipDepth: { ariaLabel: '往來深度雷達圖', wrapperClass: 'mini-chart', labels: [], customerData: [], clusterData: [] }
})

// --- 圓環圖與雷達圖清洗轉換器（對應圖4、圖5） ---
const setAumRadialChart = (aum: AuM) => {
    const totalList = [aum.customerTotal, aum.segmentTotal, aum.topTenTotal]
    aumRadialChart.value.categories.forEach((item) => {
        item.ratioList = aum.aumData[item.key].ratioList
        item.amountList = aum.aumData[item.key].amountList
    })
    aumRadialChart.value.rings.forEach((item, index) => {
        item.total = totalList[index]
    })
    const chartMaxAmount = Math.max(aum.customerTotal, aum.segmentTotal, aum.topTenTotal)
    aumRadialChart.value.max = chartMaxAmount > 0 ? chartMaxAmount : 1
}

const setLumRadialChart = (lum: LuM) => {
    const totalList = [lum.customerTotal, lum.segmentTotal, lum.topTenTotal]
    lumRadialChart.value.categories.forEach((item) => {
        item.ratioList = lum.lumData[item.key].ratioList
        item.amountList = lum.lumData[item.key].amountList
    })
    lumRadialChart.value.rings.forEach((item, index) => {
        item.total = totalList[index]
    })
    const chartMaxAmount = Math.max(lum.customerTotal, lum.segmentTotal, lum.topTenTotal)
    lumRadialChart.value.max = chartMaxAmount > 0 ? chartMaxAmount : 1
}

const setRadarChart = (radarChartData: RadarChartViewData, radarChartConfig: RadarChartConfig) => {
    radarChartConfig.labels = radarChartData.labels
    radarChartConfig.customerData = radarChartData.customerData
    radarChartConfig.clusterData = radarChartData.segmentData
}



const setHighlightRadarChart = (radarChartData: HighlightRadarChartViewData, radarChartConfig: RadarChartConfig) => {
    radarChartConfig.labels = radarChartData.labelsradarChartConfig.customerData = radarChartData.customerData// 亮點雷達圖底層一般對接客群資料，若介面未定義可安全防空}// --- 生命週期掛載與交易調用 (對應圖5、圖6) ---onMounted(async () => {const ticket = route.query.ticketif (typeof ticket !== 'string' || ticket.trim().length === 0) {pageMessage.value = '缺少 ticket 參數，無法查詢個人 360 畫像'return}try {isLoading.value = trueconst result = await fetchSecureCustomerView(ticket)if (result) {if (result.viewMode === 'multiple') {await router.replace({ name: 'customerList', query: { ticket } })return}secureCustomerView.value = resultconst customerId = result.selectedCustomer?.customerIdif (customerId) {// 同步拉取核心客戶詳情與動態資料const [internalRes, realTimeRes] = await Promise.all([getCustomer(customerId),getRealTimeCustomer(customerId)])customerInternalData.value = internalRes?.data || nullcustomerRealTimeData.value = realTimeRes?.data || null// 檢查行銷合規控管狀態const hasUnmarketable = customerRealTimeData.value?.customFlaggingList?.some((flag: any) => flag.flagTitle === '不宜行銷')if (hasUnmarketable) {isUnmarketableAlertOpen.value = true}// 對接並渲染圓環與雷達圖群組 (精準還原圖6第223-231行)if (customerInternalData.value) {const detail = customerInternalData.valueif (detail.aum) setAumRadialChart(detail.aum)if (detail.lum) setLumRadialChart(detail.lum)if (detail.wealthPotential) {setRadarChart(detail.wealthPotential, radarCharts.value.wealthPotential)}if (detail.assetProduct) {setHighlightRadarChart(detail.assetProduct, radarCharts.value.assetProduct)}if (detail.liabilitiesAndFx) {setHighlightRadarChart(detail.liabilitiesAndFx, radarCharts.value.liabilitiesAndFx)}if (detail.accountRelationshipDepth) {setRadarChart(detail.accountRelationshipDepth, radarCharts.value.accountRelationshipDepth)}}}}} catch (e) {console.error('取得個人 360 內部/外部資料失敗', e)pageMessage.value = '查詢個人 360 畫像失敗' // 圖6第242行} finally {await nextTick()isLoading.value = false}})








        < template >
        <!--進階畫像視圖切換(對應圖6第249行) -->
            <div v -if= "viewMode === 'customerAdvance'" class="app" >
                <!--進階元件區塊，可留空或引入 customer - advance 元件-- >
                    </div>

                    < !--資料載入中(對應圖6第258行) -->
                        <div v -else -if= "isLoading" class="loading-status p-6 text-center text-gray-500" >
                            資料載入中...
    </div>

        < !--系統錯誤提示(對應圖6第259行) -->
            <div v -else -if= "error" class="error-status p-6 text-center text-red-500" >
                {{ error }
}
</div>

    < !--頁面自訂訊息提示(對應圖6第260行) -->
        <div v -else -if= "pageMessage" class="page-message-status p-6 text-center text-amber-500" >
            {{ pageMessage }}
</div>

    < !--主要 360 個人客群畫像看板(對應圖6第261行)-- >
        <div v -else -if= "secureCustomerView?.selectedCustomer" class="app" >
            <div class="board" >

                <!--第一縱列(col - 2): 包含 AuM 與 LuM 的同心圓環圖表-- >
                    <section class="col col-2" >

                        <!--1. AuM 資產管理規模卡片-- >
                            <chart-card class="card r20 pad12 chart-card aum-card" >
                                <template #title >
                                AuM < span class="inline-unit" > 單位：萬元 < /span><span class="ic-info"></span >
                                    </template>
                                    < !--圓環圖元件(Radial) -->
                                        <radial-chart
            : aria - label="aumRadialChart.ariaLabel"
            : categories = "aumRadialChart.categories"
            : rings = "aumRadialChart.rings"
            : max = "aumRadialChart.max"
            : tick - step="aumRadialChart.tickStep"
            : minor - step="aumRadialChart.minorStep"
            : radial - class="aumRadialChart.radialClass"
            : legend - class="aumRadialChart.legendClass"
    />
    </chart-card>

    < !--2. LuM 貸款與信用卡分期卡片-- >
        <chart-card class="card r20 pad12 chart-card lum-card" >
            <template #title >
            LuM + 信用卡 < span class="inline-unit" > 單位：萬元 < /span><span class="ic-info"></span >
                </template>
                < !--圓環圖元件(Radial) -->
                    <radial-chart
            : aria - label="lumRadialChart.ariaLabel"
            : categories = "lumRadialChart.categories"
            : rings = "lumRadialChart.rings"
            : max = "lumRadialChart.max"
            : tick - step="lumRadialChart.tickStep"
            : minor - step="lumRadialChart.minorStep"
            : radial - class="lumRadialChart.radialClass"
            : legend - class="lumRadialChart.legendClass"
    />
    </chart-card>
    </section>

    < !--第二縱列(col - 3): 財富潛力等雷達圖群-- >
        <section class="col col-3" >
            <chart-card class="card r20 pad12 potential-card" >
                <template #title >
                財富潛力 < span class="ic-info" > </span>
                    </template>
                    < !--雷達圖元件(Radar) -->
                        <radar-chart
            : aria - label="radarCharts.wealthPotential.ariaLabel"
            : labels = "radarCharts.wealthPotential.labels"
            : customer - data="radarCharts.wealthPotential.customerData"
            : cluster - data="radarCharts.wealthPotential.clusterData"
            : wrapper - class="radarCharts.wealthPotential.wrapperClass"
    />
    </chart-card>
    </section>

    </div>
    </div>
    </template>