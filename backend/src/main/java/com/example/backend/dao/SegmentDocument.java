package com.example.backend.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 客群 360 MongoDB Document 實體模型
 * 對應 Collection: COLL_MANAGEMENT_SEGMENT (純資料庫欄位映射)
 */
@Document(collection = "COLL_MANAGEMENT_SEGMENT")
public record SegmentDocument(
        @Id String id,

        String dataDt,
        String custSegType,
        String mlCustGrp,
        String custWmLv,
        String custGrpType,
        Long custCount,

        SummaryDoc summary,
        List<RiskDistributionDoc> riskDistribution,
        MembershipPotentialDoc membershipPotential,
        AssetLiabilityDoc assetLiability,
        WealthPotentialDoc wealthPotential,
        ProductIntentDoc productIntent,
        AccountDepthDoc accountDepth,
        ConsumptionBehaviorDoc consumptionBehavior,
        ChannelBehaviorDoc channelBehavior,
        DigitalBehaviorDoc digitalBehavior,
        Instant syncTime) {

    public record SummaryDoc(
            Double avgAge,
            BigDecimal avgContribution,
            BigDecimal avgAnnualIncome,
            BigDecimal avgTotalLum,
            BigDecimal avgAumTwdAvgValNew,
            Double dormantAccountRatio,
            Long cardHolderCount,
            Long onlyDepositCount,
            Long propertyOwnerCount,
            Double highWealthPotentialRatio,
            Double faCoverageRatio,
            Double wmAumTurnoverRatio,
            BigDecimal totalInvestPLTwd,
            BigDecimal totalInvestCostTwd) {
    }

    public record RiskDistributionDoc(
            String riskType,
            String riskRank,
            Long count,
            Double ratio) {
    }

    /**
     * 會員經營長條圖部分
     *
     * @param potentialWmMemberCount 潛力新富人數
     * @param newrichUpgradeCount    新富潛在升等人數
     * @param newrichRetainCount     新富維持戶人數
     * @param newrichDowngradeCount  新富潛在降等人數
     * @param eliteUpgradeCount      菁英潛在升等人數
     * @param eliteRetainCount       菁英維持戶人數
     * @param eliteDowngradeCount    菁英潛在降等人數
     * @param peakRetainCount        登峰維持戶人數
     * @param peakDowngradeCount     登峰潛在降等人數
     * @param pbRetainCount          PB/極致維持戶人數
     */
    public record MembershipPotentialDoc(
            Long potentialWmMemberCount,
            Long newrichUpgradeCount,
            Long newrichRetainCount,
            Long newrichDowngradeCount,
            Long eliteUpgradeCount,
            Long eliteRetainCount,
            Long eliteDowngradeCount,
            Long peakRetainCount,
            Long peakDowngradeCount,
            Long pbRetainCount) {
    }

    public record AssetLiabilityDoc(
            OverallAssetLiability overall,
            T10AssetLiability t10) {
    }

    public record OverallAssetLiability(
            LumDoc lum,
            AumDoc aum) {
    }

    public record T10AssetLiability(
            T10LumDoc lum,
            T10AumDoc aum) {
    }

    public record LumDoc(
            BigDecimal avgTotalLum,
            BigDecimal avgMtgPurchaseMonthly,
            BigDecimal avgMtgRevolveMonthly,
            BigDecimal avgCollateralMonthly,
            BigDecimal avgFinAssetLoanMonthly,
            BigDecimal avgCreditLoanMonthly,
            BigDecimal avgCcBalanceMonthly) {
    }

    public record T10LumDoc(
            BigDecimal t10AvgTotalLum,
            BigDecimal t10AvgMtgPurchaseMonthly,
            BigDecimal t10AvgMtgRevolveMonthly,
            BigDecimal t10AvgCollateralMonthly,
            BigDecimal t10AvgFinAssetLoanMonthly,
            BigDecimal t10AvgCreditLoanMonthly,
            BigDecimal t10AvgCcBalanceMonthly) {
    }

    public record AumDoc(
            BigDecimal avgAumTwdCdAvg,
            BigDecimal avgAumTwdMdAvg,
            BigDecimal avgAumTwdFdAvg,
            BigDecimal avgAumTwdMyAvg,
            BigDecimal avgAumTwdFyAvg,
            BigDecimal avgAumTwdWmins,
            BigDecimal avgAumTwdWmmfdVal,
            BigDecimal avgAumTwdWmbndVal,
            BigDecimal avgAumTwdWmetfVal,
            BigDecimal avgAumTwdWmfskVal,
            BigDecimal avgAumTwdWmpskVal,
            BigDecimal avgAumTwdWmstival,
            BigDecimal avgAumTwdFfsiVal,
            BigDecimal avgAumTwdFfnbndVal,
            BigDecimal avgAumTwdAvgNew,
            BigDecimal avgAumTwdAvgValNew,
            BigDecimal avgAumTwdWmtstmdAvg,
            BigDecimal avgAumTwdWmtstfdAvg,
            BigDecimal avgAumTwdWmtstmyAvg,
            BigDecimal avgAumTwdWmtstfyAvg,
            BigDecimal avgAumTwdWmtstbndVal,
            BigDecimal avgAumTwdWmtstmfdVal) {
    }

    public record T10AumDoc(
            BigDecimal t10AvgAumTwdCdAvg,
            BigDecimal t10AvgAumTwdMdAvg,
            BigDecimal t10AvgAumTwdFdAvg,
            BigDecimal t10AvgAumTwdMyAvg,
            BigDecimal t10AvgAumTwdFyAvg,
            BigDecimal t10AvgAumTwdWmins,
            BigDecimal t10AvgAumTwdWmmfdVal,
            BigDecimal t10AvgAumTwdWmbndVal,
            BigDecimal t10AvgAumTwdWmetfVal,
            BigDecimal t10AvgAumTwdWmfskVal,
            BigDecimal t10AvgAumTwdWmpskVal,
            BigDecimal t10AvgAumTwdWmstival,
            BigDecimal t10AvgAumTwdWmstnVal,
            BigDecimal t10AvgAumTwdWmgddVa,
            BigDecimal t10AvgAumTwdWm,
            BigDecimal t10AvgAumTwdAvg,
            BigDecimal t10AvgAumTwdAvgVal,
            BigDecimal t10AvgAumTwdWmVal,
            BigDecimal t10AvgAumTwdFfnbtVal,
            BigDecimal t10AvgAumTwdFfibuVal) {
    }
}