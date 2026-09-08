import { ref, watch, unref, type MaybeRef } from 'vue'
import { getSegmentDetail, type SegmentCardDto } from '@/views/management/segment/segment-api'

// 全域快取池 (同一 SPA 生命週期內避免重複查詢)
const globalSegmentCache = new Map<string, any>()

export function useSegmentDetailCache(segmentCodeRef: MaybeRef<string>) {
  const segment = ref<any | null>(null)
  const loading = ref(false)

  async function fetchDetail(code: string) {
    if (!code) {
      segment.value = null
      return
    }

    // 1. 優先快取讀取
    if (globalSegmentCache.has(code)) {
      segment.value = globalSegmentCache.get(code) ?? null
      return
    }

    loading.value = true
    try {
      // 2. 正式向後端發送請求
      const res = await getSegmentDetail(code)
      if (res.data.success && res.data.data) {
        const detail = res.data.data
        segment.value = detail
        globalSegmentCache.set(code, detail)
      } else {
        segment.value = null
      }
    } catch (error) {
      console.error(`[useSegmentDetailCache] 取得客群 [${code}] 詳情失敗:`, error)
      segment.value = null
    } finally {
      loading.value = false
    }
  }

  // 監聽傳入的客群代碼變化
  watch(
    () => unref(segmentCodeRef),
    (newCode) => {
      if (newCode) {
        fetchDetail(newCode)
      }
    },
    { immediate: true }
  )

  return {
    segment,
    loading
  }
}

export function clearGlobalSegmentCache() {
  globalSegmentCache.clear()
}
