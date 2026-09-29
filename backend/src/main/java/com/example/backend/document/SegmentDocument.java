package com.example.backend.document;

// package com.esb.icrm.management.segment.infrastructure.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 客群 360 MongoDB Document 實體模型
 * 對應 Collection: COLL_MANAGEMENT_SEGMENT (純資料欄位映射)
 */
@Document(collection = "COLL_MANAGEMENT_SEGMENT")
public record SegmentDocument(
        @Id String id,

        String dataDt,
        String custSegType,
        String m1CustGrp,
        String custhmlv,
        String custGrpType,
        Long custCount,

        SummaryDoc summary,
        RiskDistributionDoc riskDistribution,
        MembershipPotentialDoc membershipPotential,
        AssetLiabilityDoc assetliability,
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
            BigDecimal avgTotallum,
            BigDecimal avgAumTwdAvgValNew,
            Double dormantAccountRatio,
            Long cardHolderCount,
            Long onlyDepositCount,
            Long propertyOwnerCount,
            Double highWealthPotentialRatio,
            Double faCoverageRatio,
            Double wmAumTurnoverRatio,
            BigDecimal totalInvestPlTwd,
            BigDecimal totalInvestCostTwd) {
    }

    public record RiskDistributionDoc(
            String pdCumulative75Rank,
            Double pdCumulative75Ratio,
            String wmKycTopRank,
            Double wmKycTopRatio) {
    }

    /**
     * 會員經營增長條圖部分
     *
     * @param potentialWmMemberCount 潛力新富人數
     * @param newrichUpgradeCount    新富潛在升等人數
     * @param newrichRetainCount     新富維持客戶數
     * @param newrichDowngradeCount  新富潛在降等人數
     * @param eliteUpgradeCount      菁英潛在升等人數
     * @param eliteRetainCount       菁英維持客戶數
     * @param eliteDowngradeCount    菁英潛在降等人數
     * @param peakRetainCount        登峰維持客戶數
     * @param peakDowngradeCount     登峰潛在降等人數
     * @param pbRetainCount          PB/極致維持客戶數
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
            BigDecimal avgTotallum,
            BigDecimal avgMtgPurchaseMonthly,
            BigDecimal avgMtgRevolveMonthly,
            BigDecimal avgCollateralMonthly,
            BigDecimal avgFinAssetLoanMonthly,
            BigDecimal avgCreditLoanMonthly,
            BigDecimal avgCcBalanceMonthly) {
    }

    public record T10LumDoc(
            BigDecimal t10AvgTotallum,
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
            BigDecimal avgAumTwdWmstnVal,
            BigDecimal avgAumTwdWmgddVal,
            BigDecimal avgAumTwdWm,
            BigDecimal avgAumTwdAvg,
            BigDecimal avgAumTwdAvgVal,
            BigDecimal avgAumTwdWmVal,
            BigDecimal avgAumTwdFfnbtVal,
            BigDecimal avgAumTwdFfibuVal,
            BigDecimal avgAumTwdFstival,
            BigDecimal avgAumTwdFfsival,
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
            BigDecimal t10AvgAumTwdWmgddVal,
            BigDecimal t10AvgAumTwdWm,
            BigDecimal t10AvgAumTwdAvg,
            BigDecimal t10AvgAumTwdAvgVal,
            BigDecimal t10AvgAumTwdWmVal,
            BigDecimal t10AvgAumTwdFfnbtVal,
            BigDecimal t10AvgAumTwdFfibuVal,
            BigDecimal t10AvgAumTwdFstival,
            BigDecimal t10AvgAumTwdFfsival,
            BigDecimal t10AvgAumTwdFfnbndVal,
            BigDecimal t10AvgAumTwdAvgNew,
            BigDecimal t10AvgAumTwdAvgValNew,
            BigDecimal t10AvgAumTwdWmtstmdAvg,
            BigDecimal t10AvgAumTwdWmtstfdAvg,
            BigDecimal t10AvgAumTwdWmtstmyAvg,
            BigDecimal t10AvgAumTwdWmtstfyAvg,
            BigDecimal t10AvgAumTwdWmtstbndVal,
            BigDecimal t10AvgAumTwdWmtstmfdVal) {
    }

    public record WealthPotentialDoc(
            OverallWealthPotential overall,
            T10WealthPotential t10) {
    }

    public record OverallWealthPotential(
            BigDecimal avgAnnualIncome,
            BigDecimal avgIncomeTaxAmt,
            BigDecimal avgTotalCreditCardLimit,
            BigDecimal avgAnnualCardAmt,
            BigDecimal avgTpMetroPropertyAppraisal,
            BigDecimal avgNonTpMetroPropertyAppraisal,
            BigDecimal avgDividendAmt) {
    }

    public record T10WealthPotential(
            BigDecimal t10AvgAnnualIncome,
            BigDecimal t10AvgIncomeTaxAmt,
            BigDecimal t10AvgTotalCreditCardLimit,
            BigDecimal t10AvgAnnualCardAmt,
            BigDecimal t10AvgTpMetroPropertyAppraisal,
            BigDecimal t10AvgNonTpMetroPropertyAppraisal,
            BigDecimal t10AvgDividendAmt) {
    }

    public record ProductIntentDoc(
            OverallProductIntent overall,
            T10ProductIntent t10) {
    }

    public record OverallProductIntent(
            AssetIntentDoc assets,
            LiabilityAndFxIntentDoc liabilitiesAndFx) {
    }

    public record T10ProductIntent(
            T10AssetIntentDoc assets,
            T10LiabilityAndFxIntentDoc liabilitiesAndFx) {
    }

    public record AssetIntentDoc(
            Double avgBondFundProb,
            Double bondFundHighIntentRatio,
            Double avgEquityFundProb,
            Double equityFundHighIntentRatio,
            Double avgBalancedFundProb,
            Double balancedFundHighIntentRatio,
            Double avgSavingsInsuranceProb,
            Double savingsInsuranceHighIntentRatio,
            Double avgProtectionInsuranceProb,
            Double protectionInsuranceHighIntentRatio,
            Double avgForeignBondProb,
            Double foreignBondHighIntentRatio) {
    }

    public record T10AssetIntentDoc(
            Double t10AvgBondFundProb,
            Double t10BondFundHighIntentRatio,
            Double t10AvgEquityFundProb,
            Double t10EquityFundHighIntentRatio,
            Double t10AvgBalancedFundProb,
            Double t10BalancedFundHighIntentRatio,
            Double t10AvgSavingsInsuranceProb,
            Double t10SavingsInsuranceHighIntentRatio,
            Double t10AvgProtectionInsuranceProb,
            Double t10ProtectionInsuranceHighIntentRatio,
            Double t10AvgForeignBondProb,
            Double t10ForeignBondHighIntentRatio) {
    }

    public record LiabilityAndFxIntentDoc(
            Double avgFxUsdProb,
            Double fxUsdHighIntentRatio,
            Double avgFxJpyProb,
            Double fxJpyHighIntentRatio,
            Double avgPersonalLoanProb,
            Double personalLoanHighIntentRatio,
            Double avgHomeEquityLoanProb,
            Double homeEquityLoanHighIntentRatio,
            Double avgSbProb,
            Double sbHighIntentRatio,
            Double avgCcBillInstallmentProb,
            Double ccBillInstallmentHighIntentRatio,
            Double avgCcPurchaseInstallmentProb,
            Double ccPurchaseInstallmentHighIntentRatio) {
    }

    public record T10LiabilityAndFxIntentDoc(
            Double t10AvgFxUsdProb,
            Double t10FxUsdHighIntentRatio,
            Double t10AvgFxJpyProb,
            Double t10FxJpyHighIntentRatio,
            Double t10AvgPersonalLoanProb,
            Double t10PersonalLoanHighIntentRatio,
            Double t10AvgHomeEquityLoanProb,
            Double t10HomeEquityLoanHighIntentRatio,
            Double t10AvgSbProb,
            Double t10SbHighIntentRatio,
            Double t10AvgCcBillInstallmentProb,
            Double t10CcBillInstallmentHighIntentRatio,
            Double t10AvgCcPurchaseInstallmentProb,
            Double t10CcPurchaseInstallmentHighIntentRatio) {
    }

    public record AccountDepthDoc(
            OverallAccountDepth overall,
            T10AccountDepth t10) {
    }

    public record OverallAccountDepth(
            BigDecimal totalMtgApprdAmt,
            BigDecimal totalMtgBalance,
            BigDecimal totalCreditLoanApprdAmt,
            BigDecimal totalCreditLoanBalance,
            BigDecimal avgNetInflowAmt,
            Double avgUtilityAutoDebitItemCount,
            Integer avgSecuritiesSettlementTxnCount) {
    }

    public record T10AccountDepth(
            BigDecimal t10TotalMtgApprdAmt,
            BigDecimal t10TotalMtgBalance,
            BigDecimal t10TotalCreditLoanApprdAmt,
            BigDecimal t10TotalCreditLoanBalance,
            BigDecimal t10AvgNetInflowAmt,
            Double t10AvgUtilityAutoDebitItemCount,
            Integer t10AvgSecuritiesSettlementTxnCount) {
    }

    public record ConsumptionBehaviorDoc(
            OverallConsumptionBehavior overall,
            T10ConsumptionBehavior t10,
            List<String> preferenceTags) {
    }

    public record OverallConsumptionBehavior(
            BigDecimal avgUtilityPaymentAmt,
            BigDecimal avgDiningAmt,
            BigDecimal avgOnlineShoppingAmt,
            BigDecimal avgTelecom3CAmt,
            BigDecimal avgInsuranceAmt,
            BigDecimal avgAppPaymentAmt,
            BigDecimal avgLeisureTravelAmt,
            BigDecimal avgDepartmentStoreAmt,
            BigDecimal avgOtherConsumptionAmt) {
    }

    public record T10ConsumptionBehavior(
            BigDecimal t10AvgUtilityPaymentAmt,
            BigDecimal t10AvgDiningAmt,
            BigDecimal t10AvgOnlineShoppingAmt,
            BigDecimal t10AvgTelecom3CAmt,
            BigDecimal t10AvgInsuranceAmt,
            BigDecimal t10AvgAppPaymentAmt,
            BigDecimal t10AvgLeisureTravelAmt,
            BigDecimal t10AvgDepartmentStoreAmt,
            BigDecimal t10AvgOtherConsumptionAmt) {
    }

    public record ChannelBehaviorDoc(
            OverallChannelBehavior overall,
            T10ChannelBehavior t10) {
    }

    public record OverallChannelBehavior(
            Double avgBranchTxnCountMonthlyAvg,
            Double avgAtmTxnCountMonthlyAvg,
            Double avgCallCenterInboundCountMonthlyAvg,
            Double avgMbTxnCountMonthlyAvg,
            Double avgPibTxnCountMonthlyAvg,
            Double avgWalletTxnCountMonthlyAvg,
            Long totalLineClickCountMonthlyAvg,
            Long totalLineNotiCountMonthlyAvg) {
    }

    public record T10ChannelBehavior(
            Double t10AvgBranchTxnCountMonthlyAvg,
            Double t10AvgAtmTxnCountMonthlyAvg,
            Double t10AvgCallCenterInboundCountMonthlyAvg,
            Double t10AvgMbTxnCountMonthlyAvg,
            Double t10AvgPibTxnCountMonthlyAvg,
            Double t10AvgWalletTxnCountMonthlyAvg,
            Long t10TotalLineClickCountMonthlyAvg,
            Long t10TotalLineNotiCountMonthlyAvg) {
    }

    public record DigitalBehaviorDoc(
            OverallDigitalBehavior overall,
            T10DigitalBehavior t10) {
    }

    public record OverallDigitalBehavior(
            Long totalDigitalRespCount,
            Long totalDigitalNotiCount,
            Double avgOnlineLoginCountMonthly,
            Double avgOnlineSubscriptionCount,
            BigDecimal avgDigitalIncome,
            Double lineBindingRatio,
            Double mbBindingRatio) {
    }

    public record T10DigitalBehavior(
            Long t10TotalDigitalRespCount,
            Long t10TotalDigitalNotiCount,
            Double t10AvgOnlineLoginCountMonthly,
            Double t10AvgOnlineSubscriptionCount,
            BigDecimal t10AvgDigitalIncome,
            Double t10LineBindingRatio,
            Double t10MbBindingRatio) {
    }
}