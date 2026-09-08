package com.example.backend.service;

import com.esb.icrm.management.segment.infrastructure.document.SegmentDocument;
import com.esb.icrm.management.segment.infrastructure.repository.SegmentQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SegmentQueryService {

    private static final List<String> MATRIX_ROWS = List.of("高財富度", "中財富度", "低財富度");
    private static final List<String> MATRIX_COLS = List.of("企業主", "防霸戶", "其他顧客");

    private final SegmentQueryRepository segmentQueryRepository;
    private final CodeNodes codeNodes;

    /**
     * 共用內部結構：產品切片輸入模型（封裝名稱，平均現值，T10現值）
     */
    private record ProductSlice(String name, BigDecimal overallAmount, BigDecimal t10Amount) {
    }

    /**
     * 建構子注入
     *
     * @param segmentQueryRepository 客群查詢 Repository
     */
    public SegmentQueryService(SegmentQueryRepository segmentQueryRepository, CodeNodes codeNodes) {
        this.segmentQueryRepository = segmentQueryRepository;
        this.codeNodes = codeNodes;
    }

    /**
     * 查詢客群分類總覽（計算九宮格矩陣與前期人數異動率）
     *
     * @return 客群總覽回應資料
     */
    public SegmentListResponse getSegmentOverview() {
        // 1. 取得最新兩代資料日期
        List<String> latestDates = segmentQueryRepository.findLatestTwoDataDt();
        if (latestDates.isEmpty()) {
            log.warn("[SegmentQueryService] MongoDB 無任何客群資料");
            return createEmptyResponse();
        }

        String currentDt = latestDates.get(0);
        String prevDt = latestDates.size() > 1 ? latestDates.get(1) : null;
        log.info("[SegmentQueryService] 查詢總覽，當期日期: {}, 前期日期: {}", currentDt, prevDt);

        // 2. 撈取當期與前期資料
        List<SegmentDocument> currentDocs = segmentQueryRepository.findByDataDt(currentDt);
        List<SegmentDocument> prevDocs = (prevDt != null)
                ? segmentQueryRepository.findByDataDt(prevDt)
                : Collections.emptyList();

        // 3. 建立前期人數對照表 (Key: 客群唯一代碼, Value: 前期人數)
        Map<String, Long> prevCountMap = prevDocs.stream()
                .collect(Collectors.toMap(
                        doc -> buildSegmentCode(doc.custSegType(), doc.mlCustGrp()),
                        doc -> doc.custCount() != null ? doc.custCount() : 0L,
                        (existing, replacement) -> existing));

        // 4. 取得客群代碼與中文名稱對照表 (foreignCode -> name)
        Map<String, String> segmentNameMap = buildSegmentNameMap();

        // 5. 準備九宮格矩陣容器 (3 列 x 3 欄)
        int[][] countMatrix = new int[3][3];
        int[][] clusterCountMatrix = new int[3][3];

        // 6. 準備卡片容器
        List<SegmentCardDto> cards = new ArrayList<>(currentDocs.size());

        // 7. 遍歷當期資料：累加九宮格並建立卡片 DTO
        for (SegmentDocument doc : currentDocs) {
            String code = buildSegmentCode(doc.custSegType(), doc.mlCustGrp());
            long count = doc.custCount() != null ? doc.custCount() : 0L;

            int rowIndex = MATRIX_ROWS.indexOf(doc.custWmLv());
            int colIndex = MATRIX_COLS.indexOf(doc.custGrpType());

            if (rowIndex >= 0 && rowIndex < 3 && colIndex >= 0 && colIndex < 3) {
                countMatrix[rowIndex][colIndex] += (int) count;
                clusterCountMatrix[rowIndex][colIndex] += 1;
            }

            Long prevCount = prevCountMap.get(code);
            String growth = calculateGrowth(count, prevCount);
            String avgAumText = formatAumToWan(doc.summary() != null ? doc.summary().avgAumTwdAvgValNew() : null);
            String countDiffText = calculateCountDiffText(count, prevCount);

            // 查表取得中文名稱，若找不到則 fallback 回原本代碼
            String segmentName = segmentNameMap.getOrDefault(code, code);

            cards.add(new SegmentCardDto(
                    code,
                    segmentName,
                    count,
                    growth,
                    avgAumText,
                    countDiffText,
                    rowIndex >= 0 ? rowIndex : 2,
                    colIndex >= 0 ? colIndex : 2));
        }

        // 8. 封裝九宮格矩陣資料
        SegmentMatrixData matrixData = new SegmentMatrixData(countMatrix, clusterCountMatrix);

        // 9. 打包完整總覽回應
        return new SegmentListResponse(MATRIX_ROWS, MATRIX_COLS, matrixData, cards);
    }

    /**
     * 查詢單一客群 360 畫像詳情
     *
     * @param segmentCode 客群代碼
     * @return 客群詳情 DTO
     */
    public SegmentDetailDto getSegmentDetail(String segmentCode) {
        // 1. 取得最新資料期別
        List<String> latestDates = segmentQueryRepository.findLatestTwoDataDt();
        if (latestDates.isEmpty()) {
            throw new IllegalArgumentException("系統無可用客群資料");
        }

        String currentDt = latestDates.getFirst();
        log.info("[SegmentQueryService] 查詢客群詳情, segmentCode: {}, 當期日期: {}", segmentCode, currentDt);

        // 2. 撈取當期所有客群，並精準匹配目標客群
        List<SegmentDocument> currentDocs = segmentQueryRepository.findByDataDt(currentDt);
        SegmentDocument targetDoc = currentDocs.stream()
                .filter(doc -> segmentCode.equals(buildSegmentCode(doc.custSegType(), doc.mlCustGrp())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("查無客群代碼 [" + segmentCode + "] 於期別 " + currentDt));

        // 3. 解析 3.3.2 基礎指標與洞察建議
        // TODO: 待串接經營洞察服務
        String managementInsight = "此客群以壯年高潛力資產累積者為核心，多數正處於職涯收入高峰，家庭資產配置趨向積極成長。";
        SegmentBasicInfoDto basicInfo = buildBasicInfo(targetDoc, managementInsight);

        // 4. 透過 Builder 組裝完整 360 資料
        return SegmentDetailDto.builder()
                .basicInfo(basicInfo) // 3.3.2 客群基本資訊
                .aumSegment(buildAumRadialData(targetDoc)) // 3.3.3 資產同心圓
                .lumSegment(buildLumRadialData(targetDoc)) // 3.3.3 負債同心圓
                .memberDistribution(buildMemberManagementData(targetDoc)) // 3.3.4 會員經營
                .wealthPotential(buildWealthPotentialData(targetDoc)) // 3.3.5 財富潛力雷達圖
                .productBehavior(buildProductBehaviorData(targetDoc)) // 3.3.6 產品行為
                .productIntentAsset(buildProductIntentAssetData(targetDoc)) // 3.3.7 產品意圖 (資產類)
                .productIntentLiability(buildProductIntentLiabilityData(targetDoc)) // 3.3.8 負債與現金流競意圖雷達圖
                .accountDepth(buildAccountDepthData(targetDoc)) // 3.3.9 帳戶往來深度雷達圖
                .consumeBehavior(buildConsumeBehaviorData(targetDoc)) // 3.3.10 消費行為與場景 (含消費通路)
                .channelBehavior(buildChannelBehaviorData(targetDoc)) // 3.3.11 通路接觸與交易行為
                .digitalBehavior(buildDigitalBehaviorData(targetDoc)) // 3.3.12 數位通路活躍度與綁定
                .build();
    }

    /**
     * 建立客群代碼對應名稱 Map (foreignCode -> name)
     */
    private Map<String, String> buildSegmentNameMap() {
        try {
            return codeNodes.segmentNameCode().node().getActiveChildren().stream()
                    .map(AbstractCodeNode::getCode)
                    .flatMap(Optional::stream)
                    .map(code -> {
                        // 從 Code 取得 ICRM 外部代碼 (例如: "SB_HIGHGroup1")
                        String foreignCode = CodeTreeMappings.toMap(code).get("ICRM");
                        String name = code.getName();

                        return (foreignCode != null && name != null) ? Map.entry(foreignCode, name) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (existing, replacement) -> existing));
        } catch (Exception e) {
            log.error("[SegmentQueryService] 解析 SegmentNameCode 對照表失敗", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 組裝 3.3.3 資產配置 AuM 雙同心圓環圖資料（依 12 點鐘順時針 7 大項）
     */
    private SegmentDetailDto.RadialSegmentData buildAumRadialData(SegmentDocument targetDoc) {
        SegmentDocument.AssetLiabilityDoc al = targetDoc.assetLiability();
        if (al == null || al.overall() == null || al.overall().aum() == null) {
            return null;
        }

        SegmentDocument.AumDoc overall = al.overall().aum();
        SegmentDocument.T10AumDoc t10 = (al.t10() != null) ? al.t10().aum() : null;

        BigDecimal overallTotal = safeBigDecimal(overall.avgAumTwdAvgValNew());
        BigDecimal t10Total = (t10 != null) ? safeBigDecimal(t10.t10AvgAumTwdAvgValNew()) : BigDecimal.ZERO;

        List<ProductSlice> aumProducts = List.of(
                new ProductSlice("活期與支票存款",
                        sum(overall.avgAumTwdCdAvg(), overall.avgAumTwdMdAvg(), overall.avgAumTwdMyAvg()),
                        t10 != null ? sum(t10.t10AvgAumTwdCdAvg(), t10.t10AvgAumTwdMdAvg(), t10.t10AvgAumTwdMyAvg())
                                : BigDecimal.ZERO),
                new ProductSlice("定期存款",
                        sum(overall.avgAumTwdFdAvg(), overall.avgAumTwdFyAvg(), overall.avgAumTwdWmtstfdAvg()),
                        t10 != null
                                ? sum(t10.t10AvgAumTwdFdAvg(), t10.t10AvgAumTwdFyAvg(), t10.t10AvgAumTwdWmtstfdAvg())
                                : BigDecimal.ZERO),
                new ProductSlice("基金",
                        sum(overall.avgAumTwdWmmfdVal(), overall.avgAumTwdWmtstmfdVal()),
                        t10 != null ? sum(t10.t10AvgAumTwdWmmfdVal(), t10.t10AvgAumTwdWmtstmfdVal()) : BigDecimal.ZERO),
                new ProductSlice("保險",
                        safeBigDecimal(overall.avgAumTwdWmins()),
                        t10 != null ? safeBigDecimal(t10.t10AvgAumTwdWmins()) : BigDecimal.ZERO),
                new ProductSlice("財金商品",
                        sum(overall.avgAumTwdWmstival(), overall.avgAumTwdWmstnVal(), overall.avgAumTwdWmgddVal()),
                        t10 != null
                                ? sum(t10.t10AvgAumTwdWmstival(), t10.t10AvgAumTwdWmstnVal(),
                                        t10.t10AvgAumTwdWmgddVal())
                                : BigDecimal.ZERO),
                new ProductSlice("金融商品",
                        sum(overall.avgAumTwdWmbndVal(), overall.avgAumTwdWmetfVal()),
                        t10 != null ? sum(t10.t10AvgAumTwdWmbndVal(), t10.t10AvgAumTwdWmetfVal()) : BigDecimal.ZERO),
                new ProductSlice("證券 (複委託)",
                        sum(overall.avgAumTwdWmfskVal(), overall.avgAumTwdWmpskVal()),
                        t10 != null ? sum(t10.t10AvgAumTwdWmfskVal(), t10.t10AvgAumTwdWmpskVal()) : BigDecimal.ZERO));

        return assemblerRadialData(overallTotal, t10Total, aumProducts);
    }

    /**
     * 組裝 3.3.3 負債配置 LuM 雙同心圓環圖資料（依 12 點鐘順時針 6 大項）
     */
    private SegmentDetailDto.RadialSegmentData buildLumRadialData(SegmentDocument targetDoc) {
        SegmentDocument.AssetLiabilityDoc al = targetDoc.assetLiability();
        if (al == null || al.overall() == null || al.overall().lum() == null) {
            return null;
        }

        SegmentDocument.LumDoc overall = al.overall().lum();
        SegmentDocument.T10LumDoc t10 = (al.t10() != null) ? al.t10().lum() : null;

        BigDecimal overallTotal = safeBigDecimal(overall.avgTotalLum());
        BigDecimal t10Total = (t10 != null) ? safeBigDecimal(t10.t10AvgTotalLum()) : BigDecimal.ZERO;

        List<ProductSlice> lumProducts = List.of(
                new ProductSlice("房貸(購置)",
                        safeBigDecimal(overall.avgMtgPurchaseMonthly()),
                        t10 != null ? safeBigDecimal(t10.t10AvgMtgPurchaseMonthly()) : BigDecimal.ZERO),
                new ProductSlice("房貸(週轉金)",
                        safeBigDecimal(overall.avgMtgRevolveMonthly()),
                        t10 != null ? safeBigDecimal(t10.t10AvgMtgRevolveMonthly()) : BigDecimal.ZERO),
                new ProductSlice("副擔保",
                        safeBigDecimal(overall.avgCollateralMonthly()),
                        t10 != null ? safeBigDecimal(t10.t10AvgCollateralMonthly()) : BigDecimal.ZERO),
                new ProductSlice("金融資產融資 (LL/PB)",
                        safeBigDecimal(overall.avgFinAssetLoanMonthly()),
                        t10 != null ? safeBigDecimal(t10.t10AvgFinAssetLoanMonthly()) : BigDecimal.ZERO),
                new ProductSlice("信貸",
                        safeBigDecimal(overall.avgCreditLoanMonthly()),
                        t10 != null ? safeBigDecimal(t10.t10AvgCreditLoanMonthly()) : BigDecimal.ZERO),
                new ProductSlice("信用卡已使用額度",
                        safeBigDecimal(overall.avgCcBalanceMonthly()),
                        t10 != null ? safeBigDecimal(t10.t10AvgCcBalanceMonthly()) : BigDecimal.ZERO));

        return assemblerRadialData(overallTotal, t10Total, lumProducts);
    }

    /**
     * AuM 與 LuM 共用同心圓組裝引擎
     */
private SegmentDetailDto.RadialSegmentData assembleRadialData(.map(ProductSlice slice -> AmountFormatUtils.toWanAmount(slice.overallAmount()))
                .toList();
        List<Integer> ring2Values = activeSlices.stream().map(ProductSlice slice -> AmountFormatUtils.toWanAmount(slice.t10Amount()))
                .toList();

        SegmentDetailDto.RingData ring1 = new SegmentDetailDto.RingData(
                "ring1", "客群平均", "客群平均", AmountFormatUtils.toWanAmount(overallTotal), ring1Values
        );
        SegmentDetailDto.RingData ring2 = new SegmentDetailDto.RingData(
                "ring2", "客群T10%", "客群T10%", AmountFormatUtils.toWanAmount(t10Total), ring2Values
        );

        List<SegmentDetailDto.ChartCategory> categories = activeSlices.stream().map(ProductSlice s -> new SegmentDetailDto.ChartCategory(
                        s.name(),
                        List.of(
                                formatRatio(calcRatio(s.overallAmount(), overallTotal)),
                                formatRatio(calcRatio(s.t10Amount(), t10Total))
                        ),
                        List.of(
                                AmountFormatUtils.toWanAmount(s.overallAmount()),
                                AmountFormatUtils.toWanAmount(s.t10Amount())
                        )
                ))
                .toList();

        return new SegmentDetailDto.RadialSegmentData(ring1, ring2, categories);
    }

    /** 格式化圓餅圖比例 */
    private String formatRatio(double ratio) {
        return String.format("%.1f%%", ratio * 100.0);
    }

    /**
     * 組裝 3.3.4 會員經營卡片資料（潛力新富人數標籤 + 4層級升降等長條圖）
     */
    private SegmentDetailDto.MemberManagementData buildMemberManagementData(SegmentDocument targetDoc) {
        SegmentDocument.MembershipPotentialDoc mp = targetDoc.membershipPotential();
        if (mp == null) {
            return new SegmentDetailDto.MemberManagementData(0L, Collections.emptyList());
        }

        Long potentialNewRich = safeLong(mp.potentialWmMemberCount());

        List<SegmentDetailDto.MemberDistribution> distributions = List.of(
                new SegmentDetailDto.MemberDistribution("私銀極致", 0L, safeLong(mp.pbRetainCount()), 0L),
                new SegmentDetailDto.MemberDistribution("登峰", safeLong(mp.peakDowngradeCount()),
                        safeLong(mp.peakRetainCount()), 0L),
                new SegmentDetailDto.MemberDistribution("菁英", safeLong(mp.eliteDowngradeCount()),
                        safeLong(mp.eliteRetainCount()), safeLong(mp.eliteUpgradeCount())),
                new SegmentDetailDto.MemberDistribution("新富", safeLong(mp.newrichDowngradeCount()),
                        safeLong(mp.newrichRetainCount()), safeLong(mp.newrichUpgradeCount())));

        return new SegmentDetailDto.MemberManagementData(potentialNewRich, distributions);
    }

    /**
     * 組裝 3.3.5 財富潛力雷達圖數據（包含客群平均與客群 T10 雙層、泛型結構）
     */
    private SegmentDetailDto.Comparison<SegmentDetailDto.WealthRadarSeries> buildWealthPotentialData(
            SegmentDocument targetDoc) {
        SegmentDocument.WealthPotentialDoc wp = targetDoc.wealthPotential();
        if (wp == null) {
            return null;
        }

        SegmentDetailDto.WealthRadarSeries overallSeries = (wp.overall() != null)
                ? buildWealthSeries(
                        wp.overall().avgAnnualIncome(),
                        wp.overall().avgIncomeTaxAmt(),
                        wp.overall().avgTotalCreditCardLimit(),
                        wp.overall().avgAnnualCardAmt(),
                        wp.overall().avgTpMetroPropertyAppraisal(),
                        wp.overall().avgNonTpMetroPropertyAppraisal(),
                        wp.overall().avgDividendAmt())
                : null;

        SegmentDetailDto.WealthRadarSeries t10Series = (wp.t10() != null)
                ? buildWealthSeries(
                        wp.t10().t10AvgAnnualIncome(),
                        wp.t10().t10AvgIncomeTaxAmt(),
                        wp.t10().t10AvgTotalCreditCardLimit(),
                        wp.t10().t10AvgAnnualCardAmt(),
                        wp.t10().t10AvgTpMetroPropertyAppraisal(),
                        wp.t10().t10AvgNonTpMetroPropertyAppraisal(),
                        wp.t10().t10AvgDividendAmt())
                : null;

        return new SegmentDetailDto.Comparison<>("財富潛力", overallSeries, t10Series);
    }

    /**
     * 組裝單一層級 7 大向度數據（年收入、綜所稅、信用卡額度、年度刷卡金額、不動產估價、股利）
     */
}