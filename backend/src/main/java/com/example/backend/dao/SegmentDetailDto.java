package com.example.backend.dao;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客群 360 畫像完整詳情 DTO
 */
@Builder
public record SegmentDetailDto(
        // 基本資訊
        String code,
        String name,
        Long count,
        String managementInsight,

        // 12 項核心指標
        Long totalCustomerCount,
        Long potentialCustomerCount,
        Double avgAge,
        Long homeOwnerCount,
        BigDecimal avgAnnualIncome,
        BigDecimal avgAnnualContribution,
        BigDecimal avgAUM,
        BigDecimal avgLUM,
        Double pureDepositRatio,
        Double inactiveRatio,
        String creditRiskLevel,
        String investmentRiskType,

        // 圖表結構
        RadialSegmentData aumSegment,
        RadialSegmentData lumSegment,
        MemberManagementData memberDistribution,
        ProductBehaviorData productBehavior,

        // 全部位圖結構（消費行為改用專屬 ConsumeBehaviorData）
        Comparison<WealthRadarSeries> wealthPotential,
        Comparison<ProductIntentAssetSeries> productIntentAsset,
        Comparison<ProductIntentLiabilitySeries> productIntentLiability,
        Comparison<AccountDepthSeries> accountDepth,
        ConsumeBehaviorData consumeBehavior,
        Comparison<ChannelBehaviorSeries> channelBehavior,
        Comparison<DigitalBehaviorSeries> digitalBehavior) {

    // =========================================================================
    // 核心共用元件 (Shared Building Blocks)
    // =========================================================================

    public record Comparison<T>(
            String label,
            T overall,
            T t10) {
    }

    public record ScoreMetric<T>(
            String label,
            Double score,
            T value,
            String displayValue) {
    }

    public record IntentMetric(
            String label,
            Double ratio,
            String displayRatio) {
    }

    public record ConsumeMetric(
            String label,
            Double ratio,
            BigDecimal amount,
            String displayAmount) {
    }

    // =========================================================================
    // 3.3.3 資產與負債雙同心圓環圖結構
    // =========================================================================

    public record RadialSegmentData(
            RingData ring1,
            RingData ring2,
            List<ChartCategory> categories) {
    }

    public record RingData(
            String key,
            String label,
            String name,
            Integer total,
            List<Integer> values) {
    }

    public record ChartCategory(
            String label,
            List<String> ratioList,
            List<Integer> amountList) {
    }

    // =========================================================================
    // 3.3.4 會員經營結構
    // =========================================================================

    public record MemberManagementData(
            Long potentialWmMemberCount,
            List<MemberDistribution> distributions) {
    }

    public record MemberDistribution(
            String level,
            Long atRiskCount,
            Long maintenanceCount,
            Long growthCount) {
    }

    // =========================================================================
    // 3.3.5 財富潛力系列 (使用 ScoreMetric)
    // =========================================================================

    public record WealthRadarSeries(
            ScoreMetric<BigDecimal> annualIncome,
            ScoreMetric<BigDecimal> taxes,
            ScoreMetric<BigDecimal> creditLimit,
            ScoreMetric<BigDecimal> annualSpending,
            RealEstateMetric realEstate,
            ScoreMetric<BigDecimal> dividend) {
    }

    public record RealEstateMetric(
            String label,
            Double score,
            BigDecimal tpMetroAmount,
            BigDecimal nonTpMetroAmount,
            String tooltipText) {
    }

    // =========================================================================
    // 3.3.6 產品行為結構
    // =========================================================================

    public record ProductBehaviorData(
            Double faCoverageRatio,
            Double wmAumTurnoverRatio,
            Double investmentYieldRate,
            Comparison<Double> loanDepositRatio,
            Comparison<Double> activeDepositRatio,
            Comparison<Double> wealthAumRatio) {
    }

    // =========================================================================
    // 3.3.7 ~ 3.3.12 各圖表 Series 宣告
    // =========================================================================

    public record ProductIntentAssetSeries(
            IntentMetric bondFund,
            IntentMetric equityFund,
            IntentMetric balancedFund,
            IntentMetric savingsInsurance,
            IntentMetric protectionInsurance,
            IntentMetric foreignBond) {
    }

    public record ProductIntentLiabilitySeries(
            IntentMetric fxUsd,
            IntentMetric fxJpy,
            IntentMetric creditLoan,
            IntentMetric mortgageRevolving,
            IntentMetric sb,
            IntentMetric ccBillInstallment,
            IntentMetric ccPurchaseInstallment) {
    }

    public record AccountDepthSeries(
            ScoreMetric<Double> mortgageRepayRatio,
            ScoreMetric<Double> creditLoanRepayRatio,
            ScoreMetric<BigDecimal> netInFlow,
            ScoreMetric<Double> utilityAutoDebit,
            ScoreMetric<Double> securitiesSettlement) {
    }

    public record ConsumeBehaviorData(
            String label,
            ConsumeBehaviorSeries overall,
            ConsumeBehaviorSeries t10,
            List<String> preferenceTags) {
    }

    public record ConsumeBehaviorSeries(
            ConsumeMetric livingBills,
            ConsumeMetric foodDining,
            ConsumeMetric onlineShopping,
            ConsumeMetric ict,
            ConsumeMetric insurance,
            ConsumeMetric app,
            ConsumeMetric leisure,
            ConsumeMetric department,
            ConsumeMetric other) {
    }

    public record ChannelBehaviorSeries(
            ScoreMetric<Double> branchTrans,
            ScoreMetric<Double> atmTrans,
            ScoreMetric<Double> customerService,
            ScoreMetric<Double> ebanking,
            ScoreMetric<Double> pib,
            ScoreMetric<Double> wallet,
            ScoreMetric<Double> lineResponse) {
    }

    public record DigitalBehaviorSeries(
            ScoreMetric<Double> activityResponse,
            ScoreMetric<Double> onlineLogin,
            ScoreMetric<Double> onlineTrans,
            ScoreMetric<BigDecimal> digitalRevenue,
            ScoreMetric<Double> lineBinding,
            ScoreMetric<Double> appBinding) {
    }

    // =========================================================================
    // 支援 Lombok @Builder 的自定義對映方法 (Custom Builder Method)
    // =========================================================================

    public static class SegmentDetailDtoBuilder {
        public SegmentDetailDtoBuilder basicInfo(SegmentBasicInfoDto info) {
            if (info == null)
                return this;
            this.code = info.segmentId();
            this.name = info.segmentName();
            this.count = info.custCount();
            this.managementInsight = info.managementInsight();

            this.totalCustomerCount = info.custCount();
            this.potentialCustomerCount = info.onlyDepositCount();
            this.avgAge = info.avgAge();
            this.homeOwnerCount = info.propertyOwnerCount();
            this.avgAnnualIncome = info.avgAnnualIncome();
            this.avgAnnualContribution = info.avgContribution();
            this.avgAUM = info.avgAUM();
            this.avgLUM = info.avgLUM();
            this.pureDepositRatio = info.highWealthPotentialRatio();
            this.inactiveRatio = info.dormantAccountRatio();
            this.creditRiskLevel = info.creditRisk();
            this.investmentRiskType = info.investmentRisk();
            return this;
        }
    }
}