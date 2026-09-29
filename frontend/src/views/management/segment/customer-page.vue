<script setup lang="ts">
import '@/assets/styles/customer/base.css'
import '@/assets/styles/customer/customer.css'

import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type {
    SecureCustomerViewDto,
    CustomerDto,
    AuM,
    AuMData,
    LuM,
    LuMData,
    RadarChartViewData,
    RadarChartData,
    HighLightRadarChartData,
    HighLightRadarChartViewData,
} from '@/beans/customer-bean'
import type { RealTimeCustomerDto } from '@/beans/real-time-customer-bean.ts'
import type { CustomerAdvanceViewDto } from '@/beans/customer-advance-bean'
import { getCustomer, getRealTimeCustomer } from '@/views/management/customer/customer-api.ts'

import BarChart from '@/components/charts/bar.vue'
import ChartCard from '@/components/charts/chart-card.vue'
import RadarChart from '@/components/charts/radar.vue'
import RadialChart from '@/components/charts/radial.vue'
import Watermark from '@/components/shared/watermark.vue'
import AccessCheckModal from '@/components/shared/access-check-modal.vue'
import { useCustomerAdvance } from '@/composables/use-customer-advance'
import { useCustomerAccessCheck } from '@/composables/use-customer-access-check'
import { useCustomerList } from '@/composables/use-customer-list'
import { useAuthStore } from '@/stores/use-auth-store'
import { isWatermarkEnabled } from '@/utils/app-profile'
import CustomerAdvance from './customer-advance.vue'
import { formatAmount, formatTaiwaneseUnit, parseValidNumber } from '@/utils/formatters'
import {
    ACTIVITY_MODAL_CONTENT,
    CONSUME_TAGS,
    CUSTOMER_360_MOCK,
    CUSTOMER_360_REMINDERS,
} from './customer-360-view-mock'
import type { CustomerReminderItem } from './customer-360-view-mock'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isLoading, error, fetchSecureCustomerView } = useCustomerList()
const {
    isModalVisible: isAccessCheckModalVisible,
    modalMode: accessCheckModalMode,
    modalMessage: accessCheckModalMessage,
    handleModalClose: handleAccessCheckClose,
    handleModalCancel: handleAccessCheckCancel,
    handleModalConfirm: handleAccessCheckConfirm,
    runAuthorizationCheck,
    runAccessCheck,
} = useCustomerAccessCheck()
const {
    isLoading: isCustomerAdvanceLoading,
    error: customerAdvanceError,
    getCachedCustomerAdvance,
    clearCustomerAdvanceCache,
    fetchCustomerAdvanceBySerialNo,
} = useCustomerAdvance()

const secureCustomerView = ref<SecureCustomerViewDto | null>(null)
const isAccessGranted = ref(false)
const customerAdvanceData = ref<CustomerAdvanceViewDto | null>(null)
const customerInternalData = ref<CustomerDto | null>(null)
const customerRealTimeData = ref<RealTimeCustomerDto | null>(null)
const pageMessage = ref('')
const customerAdvanceMessage = ref('')
const viewMode = ref<'customer360' | 'customerAdvance'>('customer360')
const isActivityOpen = ref(false)
const selectedCampaignIdx = ref(0)
const initialTab = ref<'basic' | 'interaction' | null>(null)
const nonWealthTabVisible = ref(true)
const wealthTabVisible = ref(true)

const customerName = computed(
    () => secureCustomerView.value?.selectedCustomer?.customerName ?? CUSTOMER_360_MOCK.customerName,
)

const routeSerialNo = computed(() => {
    return typeof route.params.serialNo === 'string' ? route.params.serialNo : ''
})

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
    return `${employeeName} ${employeeNo} ${formatDate(new Date())}`
})

// 判斷是否為關係戶/親子戶 (決定是否隱藏進度條)
const isRelatedCustomer = computed(() => {
    const msg = customerInternalData.value?.memberRank?.upgradeOrDowngradeAlert?.template || ''
    return msg.includes('關係戶或親子戶')
})

// 升等與防降提醒文案
const upgradeMessage = computed(() => {
    const alert = customerInternalData.value?.memberRank?.upgradeOrDowngradeAlert
    if (!alert) return CUSTOMER_360_MOCK.upgradeHint

    // 如果後端有傳 gapAmount (原始金額)，就用 formatTaiwaneseUnit 轉成億/萬後取代 %s
    if (alert.gapAmount && alert.template?.includes('%s')) {
        const formattedAmt = formatTaiwaneseUnit(alert.gapAmount)
        return alert.template.replace('%s', formattedAmt)
    }
    return alert.template || CUSTOMER_360_MOCK.upgradeHint
})

const tooltipCustomerAum = computed(() => formatAmount(customerInternalData.value?.memberRank?.customerAum))

const tooltipSegmentAum = computed(() => formatAmount(customerInternalData.value?.memberRank?.segmentAverageAum))

// 動態計算 5 個會員等級節點的狀態
const memberSteps = computed(() => {
    let currentRank = customerInternalData.value?.memberRank?.memberRankName || ''
    if (currentRank === '非理財會員') {
        currentRank = '無'
    } else if (currentRank === '極致/私銀' || currentRank === '極致') {
        currentRank = '私銀'
    }

    const ranks = ['無', '新富', '菁英', '登峰', '私銀']
    const currentIndex = ranks.indexOf(currentRank)

    const segmentAum = parseValidNumber(customerInternalData.value?.memberRank?.segmentAverageAum) || 0
    const segmentAumWan = Math.round(segmentAum / 10000)

    let peerIndex = 0
    if (segmentAumWan >= 10000) peerIndex = 4
    else if (segmentAumWan >= 3000) peerIndex = 3
    else if (segmentAumWan >= 1000) peerIndex = 2
    else if (segmentAumWan >= 300) peerIndex = 1
    else peerIndex = 0

    return ranks.map((rank, index) => {
        let status = ''
        if (index < currentIndex) {
            status = 'done'
        } else if (index === currentIndex) {
            status = 'current'
        }

        if (index === peerIndex && index !== currentIndex) {
            status = status ? `${status} peer` : 'peer'
        }

        return { label: rank, status }
    })
})

// 將後端的 KeyAlerts 轉換為畫面需要的陣列格式
const dynamicReminders = computed(() => {
    const alerts = customerRealTimeData.value?.keyAlerts
    if (!alerts) return CUSTOMER_360_REMINDERS

    const list: CustomerReminderItem[] = []
    if (alerts.wealthManagementMemberRankDowngradeAlert) {
        list.push({ title: '理財會員降等提醒', content: alerts.wealthManagementMemberRankDowngradeAlert })
    }
    if (alerts.largeFundInflowAlert?.length) {
        const items = alerts.largeFundInflowAlert.map((item) => {
            return item.template.replace('%s', formatAmount(item.amount))
        })
        list.push({ title: '大額資金流入', items })
    }
    if (alerts.fundFlowList?.length) {
        const items = alerts.fundFlowList.map((item) => {
            return item.template.replace('%s', formatAmount(item.amount))
        })
        list.push({ title: '資金流水', items })
    }
    if (alerts.takeProfitOrStopLossAlert) {
        list.push({ title: '停利停損', content: alerts.takeProfitOrStopLossAlert })
    }

    return list.length > 0 ? list : [{ title: '此顧客目前無提醒事項' }]
})

const openCustomerAdvance = async (): Promise<void> => {
    customerAdvanceMessage.value = ''
    viewMode.value = 'customerAdvance'
    await router.replace({
        query: {
            ...route.query,
            showPotentialLevel: 'false',
        },
    })

    const serialNo = routeSerialNo.value || secureCustomerView.value?.selectedCustomer?.serialNo
    if (!serialNo) {
        customerAdvanceMessage.value = '缺少顧客流水號，無法查詢個人資料'
        return
    }

    const cached = getCachedCustomerAdvance(serialNo)
    if (cached) {
        customerAdvanceData.value = cached
        return
    }

    const detail = await fetchCustomerAdvanceBySerialNo(serialNo)
    customerAdvanceData.value = detail
    customerAdvanceMessage.value = detail ? '' : '查詢個人資料失敗'
}

const openActivityTab = async (): Promise<void> => {
    initialTab.value = null
    await nextTick()
    initialTab.value = 'interaction'
    await openCustomerAdvance()
}

const backToFirstLayer = (): void => {
    viewMode.value = 'customer360'
    const query = { ...route.query }
    delete query.showPotentialLevel
    void router.replace({ query })
}

watch(
    routeSerialNo,
    (newSerialNo, oldSerialNo) => {
        if (!oldSerialNo || newSerialNo === oldSerialNo) {
            return
        }

        clearCustomerAdvanceCache(oldSerialNo)
        customerAdvanceData.value = null
        customerAdvanceMessage.value = ''
        viewMode.value = 'customer360'
    },
)

type RadialChartConfig<T> = {
    ariaLabel: string
    radialClass: string
    legendClass: string
    categories: Array<{ key: keyof T; color: string; label: string; ratioList: Array<string>; amountList: Array<number> }>
    rings: Array<{ key: string; label: string; total: number }>
    max: number
    tickStep: number
    minorStep: number
}

type RadarChartConfig = {
    ariaLabel: string
    wrapperClass: string
    labels: string[]
    customerData: RadarChartData[] | HighLightRadarChartData[]
    clusterData: RadarChartData[] | HighLightRadarChartData[]
}

type BarChartConfig = {
    ariaLabel: string
    wrapperClass: string
    labels: string[] | string
    customerData: number[] | number
    clusterData: number[] | number
    yMax: number
    stepSize: number
    yTickSuffix?: string
}

const aumRadialChart = ref<RadialChartConfig<AuMData>>({
    ariaLabel: 'AuM 同心圓',
    radialClass: 'radial-host',
    legendClass: 'aum-legend-container',
    max: 0,
    tickStep: 150,
    minorStep: 30,
    categories: [
        { key: 'demandDeposit', color: '#FFD640', label: '活期+支票存款', ratioList: [], amountList: [] },
        { key: 'timeDeposit', color: '#FFEF91', label: '定存', ratioList: [], amountList: [] },
        { key: 'fund', color: '#FF9900', label: '基金', ratioList: [], amountList: [] },
        { key: 'financialProduct', color: '#EF4343', label: '金融商品', ratioList: [], amountList: [] },
        { key: 'insurance', color: '#FFA099', label: '保險', ratioList: [], amountList: [] },
        { key: 'financialAsset', color: '#E62978', label: '財金商品', ratioList: [], amountList: [] },
    ],
    rings: [
        { key: 'customer', label: '顧客', total: 0 },
        { key: 'avg', label: '客群', total: 0 },
        { key: 't10', label: 'Top10%', total: 0 },
    ],
})

const lumRadialChart = ref<RadialChartConfig<LuMData>>({
    ariaLabel: 'LuM 同心圓',
    radialClass: 'radial-host',
    legendClass: 'lum-legend-container',
    max: 0,
    tickStep: 600,
    minorStep: 120,
    categories: [
        { key: 'mtgPurchase', color: '#87E3BF', label: '房貸(購置)', ratioList: [], amountList: [] },
        { key: 'mtgRevolve', color: '#009973', label: '房貸(周轉金)', ratioList: [], amountList: [] },
        { key: 'collateral', color: '#FFC8C2', label: '副擔保', ratioList: [], amountList: [] },
        { key: 'creditLoan', color: '#00735A', label: '信貸', ratioList: [], amountList: [] },
        { key: 'finAssetLoan', color: '#EF4343', label: '金融資產融資(LL/PB)', ratioList: [], amountList: [] },
        { key: 'ccBalance', color: '#FC746F', label: '信用卡已使用額度', ratioList: [], amountList: [] },
    ],
    rings: [
        { key: 'customer', label: '顧客', total: 0 },
        { key: 'avg', label: '客群', total: 0 },
        { key: 't10', label: 'Top10%', total: 0 },
    ],
})

const radarCharts = ref<Record<string, RadarChartConfig>>({
    wealthPotential: {
        ariaLabel: '財富潛力雷達圖',
        wrapperClass: 'chart-box h150',
        labels: [],
        customerData: [],
        clusterData: [],
    },
    accountRelationshipDepth: {
        ariaLabel: '帳戶往來深度雷達圖',
        wrapperClass: 'chart-box h150',
        labels: [],
        customerData: [],
        clusterData: [],
    },
    consume: {
        ariaLabel: '消費行為場景雷達圖',
        wrapperClass: 'chart-box h160',
        labels: ['生活繳費', '美食餐飲', '網購消費', '3C電信', '保險投資', 'APP及小額支付', '休閒旅遊', '百貨購物', '其他'],
        customerData: [],
        clusterData: [],
    },
    assetProduct: {
        ariaLabel: '產品意圖雷達圖',
        wrapperClass: 'chart-box h150',
        labels: [],
        customerData: [],
        clusterData: [],
    },
    liabilitiesAndFx: {
        ariaLabel: '負債與外匯雷達圖',
        wrapperClass: 'chart-box h150',
        labels: [],
        customerData: [],
        clusterData: [],
    },
    channel: {
        ariaLabel: '通路行為雷達圖',
        wrapperClass: 'chart-box h150',
        labels: ['分行月交易', 'ATM月交易', '客服往來撥打', '行銷月交易', '玉山Wallet月交易', '網銀月交易', 'Line回應率'],
        customerData: [],
        clusterData: [],
    },
    digital: {
        ariaLabel: '數位行為雷達圖',
        wrapperClass: 'chart-box h150',
        labels: ['數位活動響應率', '線上登入次數', '線上交易次數', '數位收入'],
        customerData: [],
        clusterData: [],
    },
})


const barCharts: Record<string, BarChartConfig> = {
    productBar1: {
        ariaLabel: '產品結構長條圖-存貸比',
        wrapperClass: 'product-structure-panel-sm',
        labels: '存貸比',
        customerData: 820,
        clusterData: 1740,
        yMax: 2000,
        stepSize: 500,
    },
    productBar2: {
        ariaLabel: '產品結構長條圖-活存比與理財AuM比',
        wrapperClass: 'product-structure-panel-lg',
        labels: ['活存比', '理財AuM比'],
        customerData: [59, 51],
        clusterData: [93, 33],
        yMax: 100,
        stepSize: 20,
    },
}

</script>
< !-- < template> 內接續部分(col col - 3)-- >

    <!--一般理財會員：顯示進度條與權益 -->
    <template v -else>
            <div class="member-progress" aria - label="理財會員等級進度" >
                <div class="member-track" >
                    <div class="member-line" />
                        <div
                    class="member-line-cust"
                    : style = "{ width: customerInternalData?.memberRank?.progressPercent }"
    />
    <div
                    class="member-line-clus"
                    : style = "{ width: customerInternalData?.memberRank?.segmentProgressPercent }"
    />
    </div>

    < div
v -for= "step in memberSteps"
                  : key = "step.label"
class="member-step has-tooltip"
                  : class="step.status"
    >
    <span class="member-dot" />
        <span class="member-label" > {{ step.label }}</span>

            < div class="custom-tooltip" >
                <div class="tooltip-list" >
                    <div class="tooltip-item text-cust" >
                        <span class="tooltip-dot dot-cust" />
                            <span>顧客: { { tooltipCustomerAum } } </span>
                                </div>
                                < div class="tooltip-item text-clus" >
                                    <span class="tooltip-dot dot-clus" />
                                        <span>客群平均: { { tooltipSegmentAum } } </span>
                                            </div>
                                            </div>
                                            </div>
                                            </div>
                                            </div>

                                            < div class="member-upgrade" >
                                                <!--升等 / 防降提示文案-- >
                                                <div class="member-upgrade-chip" > {{ upgradeMessage }}</div>

                                                    < !--權益區塊(排除私銀現況不需要動的文案) -->
                                                        <div
                  v -if= "customerInternalData?.memberRank?.upgradeOrDowngradeAlert?.benefitContentList?.length"
                  class="member-benefit-box"
    >
    <div class="member-benefit-title" >
        {{ customerInternalData.memberRank.upgradeOrDowngradeAlert.benefitTitle }}
</div>
    < ol class="member-benefit-list" >
        <li
                      v -for= "benefit in customerInternalData.memberRank.upgradeOrDowngradeAlert.benefitContentList"
                      : key = "benefit"
        >
        {{ benefit }}
</li>
    </ol>
    </div>
    </div>
    </template>
    </article>

    < chart - card card - class="card r20 pad12 potential-card vc">
        <template #title> 財富潛力 < span class="ic-info" /> </template>
        < radar - chart : aria - label="radarCharts.wealthPotential.ariaLabel" :
            labels="radarCharts.wealthPotential.labels" : customer - data="radarCharts.wealthPotential.customerData" :
            cluster - data="radarCharts.wealthPotential.clusterData" : wrapper -
            class="radarCharts.wealthPotential.wrapperClass" />
        </chart-card>

        < chart - card card - class="card r20 pad12 grow-chart vc">
            <template #title> 產品結構 < span class="ic-info" /> </template>
            < div class="rate-row"> <span class="rate-label"> 理財顧客收益率 < /span><span class="pct-tag">1%</span> </div>
                < div class="chart-box h190 product-structure-split">
                    <bar-chart : aria - label="barCharts.productBar1.ariaLabel" : labels="barCharts.productBar1.labels"
                        : customer - data="barCharts.productBar1.customerData" : cluster -
                        data="barCharts.productBar1.clusterData" : y - max="barCharts.productBar1.yMax" : step -
                        size="barCharts.productBar1.stepSize" : wrapper - class="barCharts.productBar1.wrapperClass" />
                    <bar-chart : aria - label="barCharts.productBar2.ariaLabel" : labels="barCharts.productBar2.labels"
                        : customer - data="barCharts.productBar2.customerData" : cluster -
                        data="barCharts.productBar2.clusterData" : y - max="barCharts.productBar2.yMax" : step -
                        size="barCharts.productBar2.stepSize" : wrapper - class="barCharts.productBar2.wrapperClass" />
                    </div>
                    </chart-card>

                    < chart - card card - class="card r20 pad12 product-intent-card vc">
                        <template #title> 產品意圖 - 資產類 < span class="ic-info" /> </template>
                        < radar - chart : aria - label="radarCharts.assetProduct.ariaLabel" :
                            labels="radarCharts.assetProduct.labels" : customer -
                            data="radarCharts.assetProduct.customerData" : cluster -
                            data="radarCharts.assetProduct.clusterData" : wrapper -
                            class="radarCharts.assetProduct.wrapperClass" />
                        </chart-card>

                        < chart - card card - class="card r20 pad12 product-intent-card vc">
                            <template #title> 產品意圖 - 負債與外匯類 < span class="ic-info" /> </template>
                            < radar - chart : aria - label="radarCharts.liabilitiesAndFx.ariaLabel" :
                                labels="radarCharts.liabilitiesAndFx.labels" : customer -
                                data="radarCharts.liabilitiesAndFx.customerData" : cluster -
                                data="radarCharts.liabilitiesAndFx.clusterData" : wrapper -
                                class="radarCharts.liabilitiesAndFx.wrapperClass" />
                            </chart-card>

                            < chart - card card - class="card r20 pad12 potential-card vc">
                                <template #title>
        帳戶往來深度 < span class="ic-info" /> <span class="rate-label sm" >* 自扣=自扣本行帳戶 </span>
            </template>
                                < radar - chart : aria - label="radarCharts.accountRelationshipDepth.ariaLabel" :
                                    labels="radarCharts.accountRelationshipDepth.labels" : customer -
                                    data="radarCharts.accountRelationshipDepth.customerData" : cluster -
                                    data="radarCharts.accountRelationshipDepth.clusterData" : wrapper -
                                    class="radarCharts.accountRelationshipDepth.wrapperClass" />
                                </chart-card>

                                < chart - card card - class="card r20 pad12 grow-chart vc">
                                    <template #title>
        消費行為場景 < span class="ic-info" /> <span class="rate-label sm" > 指標為近3個月消費類型 </span>
            </template>
                                    < radar - chart : aria - label="radarCharts.consume.ariaLabel" :
                                        labels="radarCharts.consume.labels" : customer -
                                        data="radarCharts.consume.customerData" : cluster -
                                        data="radarCharts.consume.clusterData" : wrapper -
                                        class="radarCharts.consume.wrapperClass" />
                                    <div class="tag-line">
        <span class="tag-key" > 個人消費偏好: </span>
            < span v -for= "tag in CONSUME_TAGS" : key = "tag" class="blue-tag" > {{ tag }}</span>
                </div>
                                    </chart-card>

                                    < chart - card card - class="card r20 pad12 vc">
                                        <template #title>
                    通路行為 < span class="ic-info" /> <span class="rate-label sm" >* 回應率及交易次數 </span>
                        </template>
                                        < radar - chart : aria - label="radarCharts.channel.ariaLabel" :
                                            labels="radarCharts.channel.labels" : customer -
                                            data="radarCharts.channel.customerData" : cluster -
                                            data="radarCharts.channel.clusterData" : wrapper -
                                            class="radarCharts.channel.wrapperClass" />
                                        </chart-card>

                                        < chart - card card - class="card r20 pad12 vc">
                                            <template #title>
                                                數位行為
                                                < span class="ic-info" /> <span class="rate-label sm"> 近3個月統計數據 </span>
                                            </template>
                                            < radar - chart : aria - label="radarCharts.digital.ariaLabel" :
                                                labels="radarCharts.digital.labels" : customer -
                                                data="radarCharts.digital.customerData" : cluster -
                                                data="radarCharts.digital.clusterData" : wrapper -
                                                class="radarCharts.digital.wrapperClass" />
                                            </chart-card>
                                            </section>
                                            </div>

                                            < !--活動專案彈窗 -->
                                                <div v -if="isActivityOpen" class="activity-modal-backdrop"
                                                    @click.self="isActivityOpen = false">
    <section
          class="activity-modal"
role = "dialog"
aria - modal="true"
aria - labelledby="activityModalTitle"
    >
    <div id="activityModalTitle" class="activity-modal-title" >
        {{ ACTIVITY_MODAL_CONTENT.title }}
</div>
                                                < div class="activity-modal-body">
                                                    <div class="activity-modal-content">
            <div class="activity-modal-row" >
                <span class="activity-modal-label activity-modal-name" > 活動名稱：</span>
                    <span>
{ { customerInternalData?.customerManagement?.campaignList?.[selectedCampaignIdx]?.campaignName } }
</span>
    </div>

                                                    < div class="activity-modal-row">
                                                        <span class="activity-modal-label"> 活動摘要：</span>
                                                        < div class="activity-summary-box">
                                                            <p style="white-space: pre-wrap">
                    {{ customerInternalData?.customerManagement?.campaignList?.[selectedCampaignIdx]?.campaignSummary }}
</p>
                                                            </div>
                                                            </div>

                                                            < div class="activity-modal-row">
                                                                <span
                                                                    class="activity-modal-label activity-modal-digital"> 數位投放開啟：</span>
                                                                <span>
{ { customerInternalData?.customerManagement?.campaignList?.[selectedCampaignIdx]?.campaignEnabled } }
</span>
                                                                </div>

                                                                < div class="activity-modal-row">
                                                                    <div class="activity-modal-files">
            <a
                    v -for= "attachment in customerInternalData?.customerManagement?.campaignList?.[selectedCampaignIdx]?.attachment"
                    : key = "attachment"
class="activity-file-card"
href = "#"
@click.prevent
                  >
    <span class="activity-file-name" > {{ attachment }}</span>
        </a>
        </div>
                                                                    </div>

                                                                    < div class="activity-modal-actions">
                                                                        <button class="activity-modal-close"
                                                                            type="button"
                                                                            @click="isActivityOpen = false">
    關閉視窗
    </button>
                                                                        </div>
                                                                        </div>
                                                                        </div>
                                                                        </section>
                                                                        </div>

                                                                        < !--權限 / 存取檢查彈窗-->
                                                                            <access-check - modal :
                                                                                visible="isAccessCheckModalVisible" :
                                                                                mode="accessCheckModalMode" :
                                                                                message="accessCheckModalMessage"
                                                                                @close="handleAccessCheckClose"
                                                                                @confirm="handleAccessCheckConfirm"
                                                                                @cancel="handleAccessCheckCancel" />
                                                                            </div>
                                                                            </div>
                                                                            </template>