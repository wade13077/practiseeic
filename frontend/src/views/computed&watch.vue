const c1 = computed(() => `${authStore.employeeNo} ${authStore.employeeName}`.trim())
const c2 = computed(() => Math.ceil(totalCount.value / pageSize.value) || 1)
const c21 = computed(() => Math.floor((currentPage.value - 1) / PAGES_PER_CHUNK))
const c3 = computed(() => {
return !authStore.permissions?.[ModuleCode.DISPATCH]?.canEdit
})
const c4 = computed(() =>
totalPages.value > PAGES_PER_CHUNK ? 'totalPageMore' : `totalPage${totalPages.value}`,
)
const c5 = computed(() => {
if (!isSearched.value || !campaignStartDate.value) return ''
return campaignStartDate.value
})

const c6 = computed(() => {

const selectedJobSeriesOpt = marketingJobSeriesOptions.value.find(
(opt) => opt.value === selectedReassignJobSeries.value
)

const fallbackMapping = reassignJobSeriesOptions.find(
(opt) => opt.text === selectedJobSeriesOpt.text
)

const targetJobSeriesValue = fallbackMapping ? fallbackMapping.value : selectedJobSeriesOpt.text

return reassignMarketingEmployeeOptions.value.filter((agent) => {
return targetJobSeriesValue === agent.jobSeriesValue
})
})

const c7 = computed(() => {
return z
.object({
receiptNo: z
.string()
.nullable()
.refine((val) => (currentAction.value === FormAction.SEARCH ? !!val : true), {
message: '請選擇活動名稱',
}),
reassignJobSeries: z.union([z.string(), z.number(), z.array(z.string())]).nullable().optional(),
reassignAgent: z.union([z.string(), z.number()]).nullable().optional(),
checkedCount: z.number(),
})
.superRefine((val, ctx) => {
// 提早 Return：非儲存動作不檢核改派欄位
if (currentAction.value !== FormAction.SAVE) return
const hasJob = !!val.reassignJobSeries
const hasAgent = !!val.reassignAgent
const hasCheckedItem = val.checkedCount > 0
})
})

const c8 = computed(() => {
const chunks: number[][] = []
for (let i = 1; i <= totalPages.value; i +=PAGES_PER_CHUNK) { const chunk: number[]=[] for (let j=i; j < i +
  PAGES_PER_CHUNK && j <=totalPages.value; j++) { chunk.push(j) } chunks.push(chunk) } return chunks }) const {
  validate, setFieldValue }=useForm({ validationSchema: computed(()=> toTypedSchema(dispatchSchema.value)),
  initialValues: {
  receiptNo: null,
  reassignJobSeries: null,
  reassignAgent: null,
  checkedCount: 0,
  },
  })

  const isSelectAll = computed({
  get() {
  if (paginatedListData.value.length === 0) return false
  return paginatedListData.value.every((item) => item.isChecked)
  },
  set(val: boolean) {
  paginatedListData.value.forEach((item) => {
  item.isChecked = val
  })
  },
  })

  watch(selectedReassignJobSeries, (newVal, oldVal) => {
  if (oldVal !== null && newVal !== oldVal) {
  selectedReassignAgent.value = null
  }
  })

  watch(selectedCampaignReceiptNo, (newReceiptNo) => {
  loadDynamicFilterOptions(newReceiptNo)

  selectedMarketingSegment.value = null
  selectedSegmentIdentity.value = null
  selectedContactType.value = null
  })

  watch(
  [selectedReassignJobSeries, selectedReassignAgent, paginatedListData],
  ([job, agent]) => {
  setFieldValue('reassignJobSeries', job as ReassignValue)
  setFieldValue('reassignAgent', agent as string | number | null)

  const checkedCount = paginatedListData.value.filter((item) => item.isChecked).length
  setFieldValue('checkedCount', checkedCount)
  },
  { deep: true },
  )

  watch(
  [currentPage, pageSize, listSort],
  () => {
  if (isSearched.value) fetchData()
  },
  { deep: true },
  )

  watch(
  monthList,
  (months) => {
  if (months.length > 0 && !months.includes(datepicker.month)) {
  datepicker.month = months[0]
  }
  },
  { immediate: true },
  )

  watch(
  () => datepicker.show,
  (isShow) => {
  if (!isShow) return
  datepicker.year = selectedValue.value.getFullYear()
  datepicker.month = selectedValue.value.getMonth() + 1
  },
  )

  watch(
  () => props.modelValue,
  (current, previous) => {
  if (blurStatus.value) return

  const currentYmd = normalizeToYmd(current)
  const previousYmd = normalizeToYmd(previous)
  if (currentYmd === previousYmd) return

  if (!currentYmd) {
  showValue.value = ''
  return
  }

  setDisplayByYmd(currentYmd)
  },
  )

  watch(showValue, (value) => {
  if (!value) {
  emit('update:modelValue', '')
  return
  }
  })

  watch(
  () => props.modelValue,
  (v) => {
  internalValue.value = v
  },
  )

  watch(
  () => props.text,
  () => {
  refreshWatermark()
  },
  )

  watch(
  configRef,
  (newConfig) => {
  if (!chartInstance.value) return
  chartInstance.value.data = newConfig.data
  if (newConfig.options) {
  Object.assign(chartInstance.value.options, newConfig.options)
  }
  chartInstance.value.update()
  },
  { deep: true },
  )

  watch(
  themeMode,
  (mode) => {
  localStorage.setItem(THEME_KEY, mode)
  syncThemeClass(mode)
  },
  { immediate: true },
  )

  watch(
  isSidebarCollapsed,
  () => {
  localStorage.setItem(SIDEBAR_COLLAPSED_KEY, 'false')
  syncSidebarClass(false)
  },
  { immediate: true },
  )

  watch([totalPages, modelValue], () => {
  if (modelValue.value > totalPages.value) {
  modelValue.value = totalPages.value
  }
  if (modelValue.value < 1) { modelValue.value=1 } }) watch( ()=> form.effectiveDate,
    async (val) => {
    setFieldValue('effectiveDate', val)
    if (hasAttemptedSubmit.value) await performValidation()
    },
    )