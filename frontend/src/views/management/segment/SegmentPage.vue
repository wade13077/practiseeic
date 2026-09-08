<template>
    <div class="p-6 bg-gray-50 min-h-screen">
        <!-- 上方搜尋與標題 -->
        <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
            <div>
                <h1 class="text-2xl font-bold text-gray-800">iCRM 客群動態分析矩陣</h1>
                <p class="text-sm text-gray-500 mt-1">依據客戶財富度與客群型態畫分的九宮格總覽</p>
            </div>
            <div class="flex gap-2 w-full md:w-auto">
                <input v-model="searchQuery" type="text" placeholder="搜尋客群名稱或代碼..."
                    class="px-4 py-2 border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 outline-none w-full md:w-64" />
                <button @click="selectAll"
                    class="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg text-sm font-medium transition">
                    重置篩選
                </button>
            </div>
        </div>

        <!-- 核心：九宮格矩陣排版 -->
        <div class="grid grid-cols-4 gap-3 bg-white p-6 rounded-xl shadow-sm mb-6 overflow-x-auto">
            <!-- 左上角空白格 -->
            <div class="flex items-center justify-center font-bold text-gray-400 border-b border-r pb-2">縱軸 \ 橫軸</div>

            <!-- 頂部：橫軸欄位標題 (CLUSTER_MATRIX_COLS) -->
            <div v-for="(colName, ci) in CLUSTER_MATRIX_COLS" :key="ci"
                :class="['text-center font-semibold py-2 border-b transition-colors', matrixCol === ci ? 'text-blue-600 bg-blue-50' : 'text-gray-600']">
                {{ colName }}
            </div>

            <!-- 矩陣內容列 -->
            <template v-for="(rowName, ri) in CLUSTER_MATRIX_ROWS" :key="ri">
                <!-- 左側：縱軸列位標題 (CLUSTER_MATRIX_ROWS) -->
                <div
                    :class="['flex items-center font-semibold px-2 border-r transition-colors', matrixRow === ri ? 'text-blue-600 bg-blue-50' : 'text-gray-600']">
                    {{ rowName }}
                </div>

                <!-- 數據九宮格單元格 -->
                <div v-for="(_, ci) in CLUSTER_MATRIX_COLS" :key="ci" @click="selectCell(ri, ci)" :class="[
                    'border rounded-lg p-4 text-center cursor-pointer transition-all h-24 flex flex-col justify-center items-center select-none shadow-sm',
                    matrixRow === ri && matrixCol === ci
                        ? 'bg-blue-600 text-white ring-4 ring-blue-200 border-blue-600 scale-[1.02]'
                        : 'bg-gray-50 hover:bg-blue-50 hover:border-blue-300 border-gray-200'
                ]">
                    <span class="text-2xl font-bold"
                        :class="matrixRow === ri && matrixCol === ci ? 'text-white' : 'text-gray-800'">
                        {{ countClustersInCell(ri, ci) }}
                    </span>
                    <span class="text-xs mt-1"
                        :class="matrixRow === ri && matrixCol === ci ? 'text-blue-100' : 'text-gray-400'">
                        個標籤群組
                    </span>
                </div>
            </template>
        </div>

        <!-- 下方：客群卡片清單與 360 詳情連動 -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- 卡片清單列表 -->
            <div class="lg:col-span-2">
                <h2 class="text-lg font-bold text-gray-700 mb-3 flex items-center gap-2">
                    <span>客群清單</span>
                    <span class="bg-gray-200 text-gray-700 text-xs px-2 py-0.5 rounded-full">{{
                        filteredClusterCards.length
                        }}</span>
                </h2>

                <div v-if="loading" class="text-center py-12 text-gray-400">載入中...</div>
                <div v-else-if="filteredClusterCards.length === 0"
                    class="text-center py-12 text-gray-400 bg-white rounded-xl border border-dashed">
                    無符合目前篩選條件的客群卡片
                </div>

                <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div v-for="card in filteredClusterCards" :key="card.id" @click="selectedCode = card.code" :class="[
                        'p-4 bg-white rounded-xl border shadow-sm cursor-pointer transition-all hover:shadow-md',
                        selectedCode === card.code ? 'border-blue-500 ring-2 ring-blue-100' : 'border-gray-200'
                    ]">
                        <div class="flex justify-between items-start mb-2">
                            <h3 class="font-bold text-gray-800 text-base">{{ card.name }}</h3>
                            <span class="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded">{{ card.code }}</span>
                        </div>
                        <div class="flex gap-2 mt-3">
                            <span
                                class="text-xs bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded">
                                {{ getClusterWealthLabel(card) }}
                            </span>
                            <span
                                class="text-xs bg-purple-50 text-purple-700 border border-purple-200 px-2 py-0.5 rounded">
                                {{ getClusterCustomerTypeLabel(card) }}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右側：360 畫像詳情展示（整合快取機制） -->
            <div>
                <h2 class="text-lg font-bold text-gray-700 mb-3">客群 360 畫像詳情</h2>
                <div class="bg-white p-5 rounded-xl border shadow-sm sticky top-6">
                    <div v-if="!selectedCode" class="text-center py-12 text-gray-400 italic">
                        請點擊左側客群卡片查看 360 畫像細節
                    </div>
                    <div v-else-if="detailLoading" class="text-center py-12 text-gray-400">
                        正在透過 Cache 機制讀取詳情...
                    </div>
                    <div v-else-if="segmentDetail">
                        <div class="border-b pb-3 mb-4">
                            <div class="text-xs text-blue-600 font-semibold tracking-wider uppercase">客群畫像詳情</div>
                            <h3 class="text-xl font-bold text-gray-800 mt-1">{{ segmentDetail.name }}</h3>
                            <p class="text-sm text-gray-400 mt-1">編碼：{{ segmentDetail.code }}</p>
                        </div>
                        <!-- 渲染 JSON 結構（可擴充成漂亮的圖表） -->
                        <pre class="bg-gray-50 p-3 rounded-lg text-xs overflow-auto max-h-96 text-gray-600">{{ segmentDetail }}
                </pre>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSegmentList } from '@/composables/use-segment-list'
import { useSegmentDetailCache } from '@/composables/use-segment-detail'

// 1. 初始化列表與九宮格邏輯
const {
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
    CLUSTER_MATRIX_COLS
} = useSegmentList()

// 2. 當前選中的卡片 Code，用來連動詳情
const selectedCode = ref('')

// 3. 傳入選中的變數，內部自動觸發快取與 API 機制
const { segment: segmentDetail, loading: detailLoading } = useSegmentDetailCache(selectedCode)
</script>
