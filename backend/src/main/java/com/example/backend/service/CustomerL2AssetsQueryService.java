package com.example.backend.service;

// package com.esb.icrm.management.customer.application.query;

import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryCustAccountInfoListResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryTdAccResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryCstmrInStockResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryTxHistoryInfoResponseBody;
import com.esb.icrm.code.definition.base.CodeNodes;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAssetsDto;
import com.esb.icrm.persistence.edb.customer.dto.CustomerInsurance;
import com.esb.icrm.persistence.edb.customer.port.CustomerEdbPort;
import com.esb.icrm.persistence.mongodb.customer.document.CustomerDocument;
import com.esb.icrm.persistence.mongodb.customer.repository.CustomerDocumentRepository;
import com.esb.icrm.shared.util.RatioFormatUtils;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 個人 360 L2 資產負債 查詢服務
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerL2AssetsQueryService {

    /** 外部系統整合服務 (SEAL, CIP, WCP, EDLS) */
    private final CustomerExternalIntegrationService externalService;

    /** 個人 360 共用查詢服務 */
    private final CustomerQueryService customerQueryService;

    /** 顧客查詢 Repository */
    private final CustomerDocumentRepository customerDocumentRepository;

    /** Customer EDB */
    private final CustomerEdbPort customerEdbPort;

    /** Code 節點集合 */
    private final CodeNodes codeNodes;

    private static final List<String> AUM_PRODUCT_CATEGORY = List.of("台幣活存 + 支存", "台幣定存", "外幣活存", "外幣定存", "基金", "金融商品",
            "保險", "財金商品");
    private static final List<String> LUM_PRODUCT_CATEGORY = List.of("房貸購置類", "房貸週轉金", "副擔保", "金融資產融資", "信貸",
            "信用卡使用額度");

    /** 產品種類 基金 */
    private static final String PRDT_TYPE_FUND = "PrdtTyp04";

    /** 產品種類 債券 */
    private static final String PRDT_TYPE_BOND = "PrdtTyp02";

    /** 產品分類 海外債券 */
    private static final String PRDT_CTGRY_BOND_F = "PrdtCtgryBNDF";

    /** 產品分類 結構型金融債券 */
    private static final String PRDT_CTGRY_BOND_D = "PrdtCtgryBNSD";

    /** 產品分類 境外結構型商品 */
    private static final String PRDT_CTGRY_BOND_SF = "PrdtCtgryBNSF";

    /** 投資方式 單筆 */
    private static final String INVST_TYPE_SINGLE = "InvstTyp01";

    /** 投資方式 定期 */
    private static final String INVST_TYPE_REGULAR = "InvstTyp02";

    /**
     * 總覽
     */
    public CustomerAssetsDto.Overview getOverview(String certNo) {

        Optional<CustomerDocument> customerOp = customerDocumentRepository.findById(certNo);

        if (customerOp.isEmpty())
            return null;

        CustomerDocument customer = customerOp.get();

        // TODO L2 資料庫失權欄位，先用假設資料
        customer.setL2AssetsOverview(
                new CustomerDocument.L2AssetsOverview(
                        BigDecimal.valueOf(156564),
                        BigDecimal.valueOf(266654),
                        BigDecimal.valueOf(233311),
                        BigDecimal.valueOf(456333),
                        BigDecimal.valueOf(456689)),
                new CustomerDocument.ApprovedAmountBlock(
                        BigDecimal.valueOf(1112345),
                        BigDecimal.valueOf(4569879),
                        BigDecimal.valueOf(3336644),
                        BigDecimal.valueOf(5656569),
                        BigDecimal.valueOf(8888889),
                        BigDecimal.valueOf(1222234)));

        CustomerDocument.L2AssetsOverview assetsOverview = customer.getL2AssetsOverview();
        CustomerDocument.ApprovedAmountBlock approvedAmount = assetsOverview.getApprovedAmount();
        CustomerDocument.AssetBlock assetBlock = customer.getAssetOverview().getCustomer();
        CustomerDocument.LiabilityBlock liabilityBlock = customer.getLiabilityOverview().getCustomer();
        CustomerDocument.ProductStructureBlock structureBlock = customer.getProductStructure().getCustomer();

        // =========== AuM ===========
        List<BigDecimal> aumAmountList = List.of(
                // 台幣活存 + 支存
                assetsOverview.getDemandAndCheckDepositNtdAmount(),
                // 台幣定存
                assetsOverview.getTimeDepositNtdAmount(),
                // 外幣活存
                assetsOverview.getDemandAndCheckDepositFcAmount(),
                // 外幣定存
                assetsOverview.getTimeDepositFcAmount(),
                // 基金
                assetBlock.getFund().getAmount(),
                // 金融商品
                assetsOverview.getFinancialProductAmount(),
                // 保險
                assetBlock.getInsurance().getAmount(),
                // 財金商品
                assetBlock.getTreasuryProduct().getAmount());

        // AuM 總額
        BigDecimal aumTotalAmount = sum(aumAmountList);

        List<CustomerAssetsDto.AuM> aumList = new ArrayList<>();
        for (int i = 0; i < AUM_PRODUCT_CATEGORY.size(); i++) {
            aumList.add(
                    new CustomerAssetsDto.AuM(
                            AUM_PRODUCT_CATEGORY.get(i),
                            aumAmountList.get(i),
                            RatioFormatUtils.calcRatio(aumAmountList.get(i), aumTotalAmount.doubleValue())));
        }

        // =========== LuM (現欠金額 & 核准額度) ===========
        // 信用卡使用額度
        BigDecimal creditCardUsed = liabilityBlock.getCreditCardUsed().getAmount();

        List<Pair<BigDecimal, BigDecimal>> lumPairList = List.of(
                // 房貸購置類
                Pair.of(liabilityBlock.getMortgagePurchase().getAmount(),
                        approvedAmount.getMortgagePurchaseApprovedAmount()),
                // 房貸週轉金
                Pair.of(liabilityBlock.getMortgageRevolving().getAmount(),
                        approvedAmount.getMortgageRevolvingApprovedAmount()),
                // 副擔保
                Pair.of(liabilityBlock.getSubCollateral().getAmount(), approvedAmount.getSubCollateralApprovedAmount()),
                // 金融資產融資
                Pair.of(liabilityBlock.getFinAssetLoan().getAmount(), approvedAmount.getFinAssetLoanApprovedAmount()),
                // 信貸
                Pair.of(liabilityBlock.getCreditLoan().getAmount(), approvedAmount.getCreditLoanApprovedAmount()),
                // 信用卡使用額度
                Pair.of(creditCardUsed, approvedAmount.getCreditCardUsedApprovedAmount()));

        // LuM 總額
        BigDecimal lumTotalAmount = sum(lumPairList.stream().map(Pair::getLeft).toList());

        List<CustomerAssetsDto.LuM> lumList = new ArrayList<>();
        for (int i = 0; i < LUM_PRODUCT_CATEGORY.size(); i++) {
            lumList.add(
                    new CustomerAssetsDto.LuM(
                            LUM_PRODUCT_CATEGORY.get(i),
                            lumPairList.get(i).getLeft(),
                            lumPairList.get(i).getRight(),
                            RatioFormatUtils.calcRatio(lumPairList.get(i).getLeft(), lumTotalAmount.doubleValue())));
        }

        // =========== 總覽 ===========
        return new CustomerAssetsDto.Overview(
                // AuM 總資產(含證券)
                assetBlock.getTotalAmount(),
                // 理財 AuM
                structureBlock.getTotalAuM(),
                // LuM 總負債
                structureBlock.getTotalLuM(),
                // LuM 總負債 + 信用卡已使用額度
                structureBlock.getTotalLuM().add(creditCardUsed),
                aumList,
                lumList);
    }

    /**
     * 存款
     */
    public CustomerAssetsDto.Deposit getDeposit(String certNo) {

        CustomerDocument customer = customerQueryService.loadCustomerForExternalQuery(certNo);
        String user = customerQueryService.resolveCurrentUser();
        String branch = customerQueryService.resolveCurrentBranch();

        // 查詢活存
        List<EdlsQueryCustAccountInfoListResponseBody.AccountInfo> accountInfoList = externalService
                .queryEdlsAccountInfoList(certNo, user, branch);

        // 查詢定存
        List<EdlsQueryTdAccResponseBody.TdAccountInfo> tdAccountInfoList = externalService.queryEdlsTdAccList(certNo,
                user, branch);

        // 台幣活存(含支存)
        List<CustomerAssetsDto.TwdDemandDeposit> twdDemandDeposit = accountInfoList.stream()
                .filter(Objects::nonNull)
                .filter(data -> "0".equals(data.accountStatus())
                        && "1".equals(data.accountBusinessType())
                        && "T".equals(data.currencyType()))
                .map(data -> new CustomerAssetsDto.TwdDemandDeposit(
                        data.account(),
                        data.realBalance(),
                        data.availableBalance()))
                .toList();

        BigDecimal twdDemandDepositRealBalanceTotal = sum(
                twdDemandDeposit.stream().map(CustomerAssetsDto.TwdDemandDeposit::realBalance).toList());
        BigDecimal twdDemandDepositAvailableBalanceTotal = sum(
                twdDemandDeposit.stream().map(CustomerAssetsDto.TwdDemandDeposit::availableBalance).toList());

        // 外幣活存
        List<CustomerAssetsDto.FxDemandDeposit> fxDemandDeposit = accountInfoList.stream()
                .filter(Objects::nonNull)
                .filter(accountInfo -> "0".equals(accountInfo.accountStatus())
                        && "1".equals(accountInfo.accountBusinessType())
                        && "F".equals(accountInfo.currencyType()))
                .flatMap(accountInfo -> accountInfo.currInfoList().stream()
                        .map(currInf -> new CustomerAssetsDto.FxDemandDeposit(
                                accountInfo.account(),
                                currInf.curr(),
                                currInf.realBalance(),
                                currInf.availableBalance(),
                                accountInfo.availableBalance())))
                .toList();

        BigDecimal fxDemandDepositTotal = sum(
                fxDemandDeposit.stream().map(CustomerAssetsDto.FxDemandDeposit::availableBalanceTwd).toList());

        // 台幣定存 & 外幣定存
        List<CustomerAssetsDto.TimeDeposit> twdTimeDepositList = new ArrayList<>();
        List<CustomerAssetsDto.TimeDeposit> fxTimeDepositList = new ArrayList<>();

        for (EdlsQueryTdAccResponseBody.TdAccountInfo tdAccount : tdAccountInfoList) {

            if (tdAccount == null)
                continue;

            CustomerAssetsDto.TimeDeposit timeDeposit = new CustomerAssetsDto.TimeDeposit(
                    tdAccount.tdAccNo(),
                    tdAccount.cdAmt(),
                    tdAccount.curr(),
                    tdAccount.cdInteRate(),
                    tdAccount.pledgeMark(),
                    tdAccount.maturityDate(),
                    tdAccount.autoRenewMark());

            if ("TWD".equals(tdAccount.curr())) {
                twdTimeDepositList.add(timeDeposit);
            } else {
                fxTimeDepositList.add(timeDeposit);
            }
        }

        BigDecimal twdTimeDepositTotal = sum(
                twdTimeDepositList.stream().map(CustomerAssetsDto.TimeDeposit::cdAmt).toList());

        return new CustomerAssetsDto.Deposit(
                new CustomerAssetsDto.TwdDemandDepositBlock(
                        twdDemandDeposit,
                        twdDemandDepositRealBalanceTotal,
                        twdDemandDepositAvailableBalanceTotal),
                new CustomerAssetsDto.TwdTimeDepositBlock(
                        twdTimeDepositList,
                        twdTimeDepositTotal),
                new CustomerAssetsDto.FxDemandDepositBlock(
                        fxDemandDeposit,
                        fxDemandDepositTotal),
                new CustomerAssetsDto.FxTimeDepositBlock(
                        fxTimeDepositList));
    }

    /**
     * 基金/ETF/債券
     */
    public CustomerAssetsDto.Fund getFund(String certNo) {

        String user = customerQueryService.resolveCurrentUser();
        String branch = customerQueryService.resolveCurrentBranch();
        String ipAddress = customerQueryService.resolveCurrentIpAddress();

        WcpQueryCstmrInStockResponseBody inStockRes = externalService.queryWcpCustomerInStock(certNo, user, branch,
                ipAddress);

        if (inStockRes == null || inStockRes.inStockprdtList().isEmpty() || inStockRes.sttstcsList().isEmpty()) {
            log.warn("呼叫 WCP 系統查詢顧客帳戶 [{}] 無庫存", certNo);
            return null;
        }

        List<WcpQueryCstmrInStockResponseBody.InStockPrdtInfo> inStockprdtList = inStockRes.inStockprdtList();
        List<WcpQueryCstmrInStockResponseBody.SttstcsInfo> sttstcsList = inStockRes.sttstcsList();

        List<CustomerAssetsDto.InvestmentAsset> singleFundList = new ArrayList<>();
        List<CustomerAssetsDto.InvestmentAsset> regularFundList = new ArrayList<>();
        List<CustomerAssetsDto.InvestmentAsset> structuredProductList = new ArrayList<>();
        List<CustomerAssetsDto.InvestmentAsset> bondList = new ArrayList<>();
        List<CustomerAssetsDto.InvestmentAsset> stockList = new ArrayList<>();

        List<String> stockProductCodeList = customerEdbPort.findStockProductCode();

        for (WcpQueryCstmrInStockResponseBody.InStockPrdtInfo prdtInfo : inStockprdtList) {

            // 基金單筆
            if (PRDT_TYPE_FUND.equals(prdtInfo.prdtTyp()) && INVST_TYPE_SINGLE.equals(prdtInfo.invstTyp())) {
                singleFundList.add(toInvestmentAsset(prdtInfo));
                continue;
            }

            // 基金定期
            if (PRDT_TYPE_FUND.equals(prdtInfo.prdtTyp()) && INVST_TYPE_REGULAR.equals(prdtInfo.invstTyp())) {
                regularFundList.add(toInvestmentAsset(prdtInfo));
                continue;
            }

            // 結構型商品
            if (PRDT_CTGRY_BOND_D.equals(prdtInfo.prdtCtgry()) || PRDT_CTGRY_BOND_SF.equals(prdtInfo.prdtCtgry())) {
                structuredProductList.add(toInvestmentAsset(prdtInfo));
                continue;
            }

            // 債券
            if (PRDT_TYPE_BOND.equals(prdtInfo.prdtTyp()) && PRDT_CTGRY_BOND_F.equals(prdtInfo.prdtCtgry())) {
                bondList.add(toInvestmentAsset(prdtInfo));
                continue;
            }

            // ETF / 海外股票
            if (prdtInfo.prdtCtgry().isEmpty() && stockProductCodeList.contains(prdtInfo.prdtCde())) {
                stockList.add(toInvestmentAsset(prdtInfo));
            }
        }

        // 未實現損益統計(不含在途)
        List<CustomerAssetsDto.UnrealizedPnl> unrealizedPnlList = sttstcsList.stream()
                .filter(Objects::nonNull)
                .map(data -> new CustomerAssetsDto.UnrealizedPnl(
                        data.invstCurr(),
                        data.totalInvstAmtWithFee(),
                        data.totalInvstPValue(),
                        data.totalDvdnd(),
                        data.totalInvstRtrnWithDvdnd(),
                        data.totalInvstRtrn(),
                        inStockRes.trustAccntEntireAmt()))
                .toList();

        // 在途中的明細
        List<WcpQueryTxHistoryInfoResponseBody> txHistoryInfoResList = externalService.queryWcpTxHistoryInfo(certNo,
                user, branch, ipAddress);

        List<CustomerAssetsDto.TxHistoryInfo> txHistoryInfoList = txHistoryInfoResList.stream()
                .filter(Objects::nonNull)
                .filter(data -> "Y".equals(data.isTrnst()))
                .map(
                        data -> new CustomerAssetsDto.TxHistoryInfo(
                                data.txDate(),
                                data.invstTyp(),
                                data.txCtgry(),
                                data.prdtNam(),
                                data.trustNo(),
                                data.invstCurr(),
                                data.invstAmt(),
                                data.prjctCtgry()))
                .toList();

        return new CustomerAssetsDto.Fund(
                singleFundList,
                regularFundList,
                structuredProductList,
                bondList,
                stockList,
                unrealizedPnlList,
                txHistoryInfoList);
    }

    private CustomerAssetsDto.InvestmentAsset toInvestmentAsset(
            WcpQueryCstmrInStockResponseBody.InStockPrdtInfo prdtInfo) {
        return new CustomerAssetsDto.InvestmentAsset(
                prdtInfo.prdtsNam(),
                prdtInfo.trustNo(),
                prdtInfo.productRiskRtrnLvl(),
                prdtInfo.productRiskRtrnLvlLatest(),
                prdtInfo.invstCurr(),
                prdtInfo.invstAmt(),
                prdtInfo.shrBal(),
                prdtInfo.invstPValue(),
                prdtInfo.totalDvdnd(),
                prdtInfo.invstRtrn(),
                prdtInfo.rtrnRate(),
                prdtInfo.cntnDeduct(),
                prdtInfo.deductAmt(),
                prdtInfo.buyTrnstAmt(),
                prdtInfo.rfrncSellPrice());
    }

    /**
     * 保險
     */
    public CustomerAssetsDto.InsuranceBlock getInsurance(String certNo) {

        List<CustomerInsurance> insuranceList = customerEdbPort.findInsuranceList(certNo);

        // TODO 假資料
        insuranceList = List.of(
                new CustomerInsurance(
                        "要保人",
                        "被保險人",
                        "XX公司",
                        "XX險種",
                        "1",
                        "2026/09/17",
                        "2026/09/18",
                        "2026/09/19",
                        "2026/09/20",
                        "TWD",
                        "3600",
                        "7200",
                        "年繳",
                        "0"));

        return new CustomerAssetsDto.InsuranceBlock(
                insuranceList.stream()
                        .filter(Objects::nonNull)
                        .map(data -> new CustomerAssetsDto.Insurance(
                                data.ownerId(),
                                data.insuredId(),
                                data.insCoName(),
                                data.planName(),
                                codeNodes.insuranceStatus().getName(data.poStatusCode()),
                                data.poIssueDate(),
                                data.coverageExpiredDate(),
                                data.nextPaidDate(),
                                data.payExpiredDate(),
                                data.cuyCpo(),
                                data.txnAmtCpo(),
                                data.premAmtTotalCpo(),
                                data.modx(),
                                data.mtyAmt()))
                        .toList());
    }

    /**
     * 安全數值加總
     */
    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}