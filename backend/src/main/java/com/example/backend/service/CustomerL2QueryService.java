package com.example.backend.service;

// package com.esb.icrm.management.customer.application.query;

import com.esb.icrm.management.customer.interfaces.dto.CustomerAssetsDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerInfoDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerBasicDto;
import org.springframework.stereotype.Service;

/**
 * 個人 360 L2 查詢門面
 */
@Service
public class CustomerL2QueryService {

    /**
     * L2 摘要頁籤查詢服務
     */
    private final CustomerL2SummaryService summaryService;

    /**
     * L2 基本資料查詢服務
     */
    private final CustomerL2BasicService basicService;

    /**
     * L2 資產負債
     */
    private final CustomerL2AssetsQueryService customerL2AssetsService;

    /**
     * 建構子注入
     *
     * @param summaryService          L2 摘要頁籤查詢服務
     * @param basicService            L2 基本資料查詢服務
     * @param customerL2AssetsService L2 資產負債
     */
    public CustomerL2QueryService(
            CustomerL2SummaryService summaryService,
            CustomerL2BasicService basicService,
            CustomerL2AssetsQueryService customerL2AssetsService) {
        this.summaryService = summaryService;
        this.basicService = basicService;
        this.customerL2AssetsService = customerL2AssetsService;
    }

    /**
     * 查詢 L2 摘要頁籤
     *
     * @param certNo 顧客識別值
     * @return 顧客資訊摘要
     */
    public CustomerInfoDto getSummary(String certNo) {
        return summaryService.getCustomerSummary(certNo);
    }

    /**
     * 查詢 L2 基本資料頁籤
     *
     * @param certNo 顧客識別值
     * @return 基本資料
     */
    public CustomerBasicDto getBasic(String certNo) {
        return basicService.getCustomerBasic(certNo);
    }

    /**
     * 查詢 L2 資產負債 - 總覽
     *
     * @param certNo 顧客識別值
     * @return 總覽
     */
    public CustomerAssetsDto.Overview getAssetOverview(String certNo) {
        return customerL2AssetsService.getOverview(certNo);
    }

    /**
     * 查詢 L2 資產負債 - 存款
     *
     * @param certNo 顧客識別值
     * @return 存款
     */
    public CustomerAssetsDto.Deposit getAssetDeposit(String certNo) {
        return customerL2AssetsService.getDeposit(certNo);
    }

    /**
     * 查詢 L2 資產負債 - 基金/ETF/債券
     *
     * @param certNo 顧客識別值
     * @return 基金/ETF/債券
     */
    public CustomerAssetsDto.Fund getAssetFund(String certNo) {
        return customerL2AssetsService.getFund(certNo);
    }

    /**
     * 查詢 L2 資產負債 - 保險
     *
     * @param certNo 顧客識別值
     * @return 保險
     */
    public CustomerAssetsDto.InsuranceBlock getAssetInsurance(String certNo) {
        return customerL2AssetsService.getInsurance(certNo);
    }
}