package com.esb.icrm.code.definition.base;

import java.util.Objects;

import com.esb.icrm.code.application.facade.CodeTreeFacade;
import com.esb.icrm.code.definition.*;

import org.springframework.stereotype.Component;

/**
 * CodeTree 定義節點聚合
 * <p>
 * 統一提供各代碼節點定義的存取入口，避免使用端注入多個 definition Bean
 * </p>
 */
@Component
public final class CodeNodes {

    /** CodeTree 外觀服務 */
    private final CodeTreeFacade codeTreeFacade;

    /** 顧客身分別代碼定義 (延遲載入) */
    private volatile CustomerTypeCode customerTypeCode;

    /** ICRM 角色代碼定義 */
    private volatile RoleCode icrmRoleCode;

    /** AuM 門檻值代碼定義 */
    private volatile AmountValue amountValue;

    /** 職系 */
    private volatile MarketingJobSeriesCode marketingJobSeriesCode;

    /** 資金活動水代碼定義 */
    private volatile FundFlowCode fundFlowCode;

    /** 取得歐盟國家代碼定義 */
    private volatile EuCountryCode euCountryCode;

    /** 取得理財會員降等提醒代碼定義 */
    private volatile MemberDowngradeAlertCode memberDowngradeAlertCode;

    /** 取得大額金流流入代碼定義 */
    private volatile LargeFundInflowAlertCode largeFundInflowAlertCode;

    /** 取得顧客 KYC 類別代碼定義 */
    private volatile CustomerKycTypeCode customerKycTypeCode;

    /** 取得理財會員等級 AuM 門檻值代碼定義 */
    private volatile WealthMemberRankAumThresholdCode wealthMemberRankAumThresholdCode;

    /** 取得客群群組名稱定義 */
    private volatile SegmentNameCode segmentNameCode;

    /**
     * 建構子
     *
     * @param codeTreeFacade CodeTree 外觀服務
     */
    public CodeNodes(CodeTreeFacade codeTreeFacade) {
        this.codeTreeFacade = Objects.requireNonNull(codeTreeFacade, "codeTreeFacade must not be null");
    }

    /**
     * 取得顧客身分別代碼定義
     *
     * @return CustomerTypeCode
     */
    public CustomerTypeCode customerType() {
        CustomerTypeCode local = customerTypeCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (customerTypeCode == null) {
                customerTypeCode = new CustomerTypeCode(codeTreeFacade);
            }
            return customerTypeCode;
        }
    }

    /**
     * 取得 ICRM 角色代碼定義
     *
     * @return IcrmRoleCode
     */
    public RoleCode icrmRole() {
        RoleCode local = icrmRoleCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (icrmRoleCode == null) {
                icrmRoleCode = new RoleCode(codeTreeFacade);
            }
            return icrmRoleCode;
        }
    }

    /**
     * 取得 AuM 門檻值代碼定義
     *
     * @return AmountValue
     */
    public AmountValue amountValue() {
        AmountValue local = amountValue;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (amountValue == null) {
                amountValue = new AmountValue(codeTreeFacade);
            }
            return amountValue;
        }
    }

    /**
     * 取得職系
     *
     * @return MarketingJobSeriesCode
     */
    public MarketingJobSeriesCode marketingJobSeriesCode() {
        MarketingJobSeriesCode local = marketingJobSeriesCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (marketingJobSeriesCode == null) {
                marketingJobSeriesCode = new MarketingJobSeriesCode(codeTreeFacade);
            }
            return marketingJobSeriesCode;
        }
    }

    /**
     * 取得資金活動水代碼定義
     *
     * @return FundFlowCode
     */
    public FundFlowCode fundFlow() {
        FundFlowCode local = fundFlowCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (fundFlowCode == null) {
                fundFlowCode = new FundFlowCode(codeTreeFacade);
            }
            return fundFlowCode;
        }
    }

    /**
     * 取得歐盟國家代碼定義
     *
     * @return EuCountryCode
     */
    public EuCountryCode euCountry() {
        EuCountryCode local = euCountryCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (euCountryCode == null) {
                euCountryCode = new EuCountryCode(codeTreeFacade);
            }
            return euCountryCode;
        }
    }

    /**
     * 取得理財會員降等提醒代碼定義
     *
     * @return MemberDowngradeAlertCode
     */
    public MemberDowngradeAlertCode memberDowngradeAlert() {
        MemberDowngradeAlertCode local = memberDowngradeAlertCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (memberDowngradeAlertCode == null) {
                memberDowngradeAlertCode = new MemberDowngradeAlertCode(codeTreeFacade);
            }
            return memberDowngradeAlertCode;
        }
    }

    /**
     * 取得大額金流流入代碼定義
     *
     * @return LargeFundInflowAlertCode
     */
    public LargeFundInflowAlertCode largeFundInflowAlert() {
        LargeFundInflowAlertCode local = largeFundInflowAlertCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (largeFundInflowAlertCode == null) {
                largeFundInflowAlertCode = new LargeFundInflowAlertCode(codeTreeFacade);
            }
            return largeFundInflowAlertCode;
        }
    }

    /**
     * 取得顧客 KYC 類別代碼定義
     *
     * @return CustomerKycTypeCode
     */
    public CustomerKycTypeCode customerKycType() {
        CustomerKycTypeCode local = customerKycTypeCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (customerKycTypeCode == null) {
                customerKycTypeCode = new CustomerKycTypeCode(codeTreeFacade);
            }
            return customerKycTypeCode;
        }
    }

    /**
     * 取得理財會員等級 AuM 門檻值代碼定義
     *
     * @return WealthMemberRankAumThresholdCode
     */
    public WealthMemberRankAumThresholdCode wealthMemberRankAumThreshold() {
        WealthMemberRankAumThresholdCode local = wealthMemberRankAumThresholdCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (wealthMemberRankAumThresholdCode == null) {
                wealthMemberRankAumThresholdCode = new WealthMemberRankAumThresholdCode(codeTreeFacade);
            }
            return wealthMemberRankAumThresholdCode;
        }
    }

    /**
     * 取得客群群組名稱定義
     *
     * @return IcrmRoleCode
     */
    public SegmentNameCode segmentNameCode() {
        SegmentNameCode local = segmentNameCode;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (segmentNameCode == null) {
                segmentNameCode = new SegmentNameCode(codeTreeFacade);
            }
            return segmentNameCode;
        }
    }
}