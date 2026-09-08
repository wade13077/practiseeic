import { ref, computed, onMounted } from 'vue'
import { getSegmentList, type SegmentCardDto, type SegmentMatrixData } from '@/views/management/segment/segment-api'

export function useSegmentList() {
  const loading = ref(false)
  const searchQuery = ref('')
  const matrixRow = ref<number | null>(null)
  const matrixCol = ref<number | null>(null)

  // 矩陣維度定義
  const CLUSTER_MATRIX_ROWS = ref<string[]>(['高財富度', '中財富度', '低財富度'])
  const CLUSTER_MATRIX_COLS = ref<string[]>(['企業主', '薪資戶', '其他顧客'])

  // 九宮格統計數據
  const CLUSTER_MATRIX_DATA = ref<SegmentMatrixData>({
    count: [,
 ,
      [0, 0, 0]
    ],
    clusterCount: [,
 ,
      [0, 0, 0]
    ]
  })

  // 所有客群卡片清單
  const allClusterCards = ref<SegmentCardDto[]>([])

  // 向後端發送交易獲取資料
  async function fetchSegmentList() {
    loading.value = true
    try {
      const res = await getSegmentList()
      const data = res.data.data
      if (res.data.success && data) {
        if (data.matrixRows) CLUSTER_MATRIX_ROWS.value = data.matrixRows
        if (data.matrixCols) CLUSTER_MATRIX_COLS.value = data.matrixCols
        if (data.matrixData) CLUSTER_MATRIX_DATA.value = data.matrixData
        if (data.clusterCards) allClusterCards.value = data.clusterCards
      }
    } catch (error) {
      console.error('Failed to fetch segment list:', error)
    } finally {
      loading.value = false
    }
  }

  onMounted(async () => {
    await fetchSegmentList()
  })

  // 篩選後的客群卡片 (九宮格 + 關鍵字)
  const filteredClusterCards = computed<SegmentCardDto[]>(() => {
    let cards = allClusterCards.value

    if (matrixRow.value !== null && matrixCol.value !== null) {
      cards = cards.filter((c) => c.wealthLevel === matrixRow.value && c.customerType === matrixCol.value)
    } else if (matrixRow.value !== null) {
      cards = cards.filter((c) => c.wealthLevel === matrixRow.value)
    } else if (matrixCol.value !== null) {
      cards = cards.filter((c) => c.customerType === matrixCol.value)
    }

    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      cards = cards.filter((c) => c.name.toLowerCase().includes(q) || c.code.toLowerCase().includes(q))
    }

    return cards
  })

  // 矩陣單元格計數
  function countClustersInCell(ri: number, ci: number): number {
    return CLUSTER_MATRIX_DATA.value.clusterCount?.[ri]?.[ci] ?? 0
  }

  // 標籤轉換
  function getClusterWealthLabel(card: SegmentCardDto): string {
    return CLUSTER_MATRIX_ROWS.value[card.wealthLevel] ?? '其他財富'
  }

  function getClusterCustomerTypeLabel(card: SegmentCardDto): string {
    return CLUSTER_MATRIX_COLS.value[card.customerType] ?? '其他客群'
  }

  // 矩陣選擇事件
  function selectCell(ri: number, ci: number) {
    if (matrixRow.value === ri && matrixCol.value === ci) {
      matrixRow.value = null
      matrixCol.value = null
    } else {
      matrixRow.value = ri
      matrixCol.value = ci
    }
    searchQuery.value = ''
  }

  function selectAll() {
    matrixRow.value = null
    matrixCol.value = null
    searchQuery.value = ''
  }

  return {
    loading,
    searchQuery,
    matrixRow,
    matrixCol,
    filteredClusterCards,
    countClustersInCell,
    getClusterWealthLabel,
    getClusterCustomerTypeLabel,
    selectCell,
    selectAll,
    CLUSTER_MATRIX_ROWS,
    CLUSTER_MATRIX_COLS,
    CLUSTER_MATRIX_DATA
  }
}
