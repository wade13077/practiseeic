package com.example.backend.service;

package com.esb.icrm.management.customer.application.query;

import com.esb.icrm.exception.ExternalSystemException;
import com.esb.icrm.management.customer.application.exception.CustomerAccessCheckException;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAssetsDto;
import org.springframework.stereotype.Service;

import com.esb.icrm.management.customer.interfaces.dto.CustomerCreditDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerInfoDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerBasicDto;

/**
 * 個人 360 L2 查詢門面
 */
@Service
public class CustomerL2QueryService {

    /**
     * L2 摘要頁面查詢服務
     */
    private final CustomerL2SummaryService summaryService;

    /**
     * L2 基本資料查詢服務
     */
    private final CustomerL2BasicService basicService;

    /**
     * L2 消費行為與場景查詢服務
     */
    private final CustomerL2ConsumptionService consumptionService;

    /**
     * L2 資產負債
     */
    private final CustomerL2AssetsQueryService customerL2AssetsService;

    /**
     * 建構子注入
     *
     * @param summaryService          L2 摘要頁面查詢服務
     * @param basicService            L2 基本資料查詢服務
     * @param customerL2AssetsService L2 資產負債
     */
    public CustomerL2QueryService(
            CustomerL2SummaryService summaryService,
            CustomerL2BasicService basicService,
            CustomerL2ConsumptionService consumptionService,
            CustomerL2AssetsQueryService customerL2AssetsService) {
        this.summaryService = summaryService;
        this.basicService = basicService;
        this.consumptionService = consumptionService;
        this.customerL2AssetsService = customerL2AssetsService;
    }

    /**
     * 查詢 L2 摘要頁面
     *
     * @param serialNo 顧客識別值
     * @return 顧客資訊摘要
     */
    public CustomerInfoDto getSummary(String serialNo) {
        return summaryService.getCustomerSummary(serialNo);
    }

    /**
     * 查詢 L2 基本資料頁面
     *
     * @param serialNo 顧客識別值
     * @return 基本資料
     */
    public CustomerBasicDto getBasic(String serialNo) {
        return basicService.getCustomerBasic(serialNo);
    }

    /**
     * 查詢 L2 消費行為與場景頁面
     *
     * @param serialNo 顧客識別值
     * @return 消費行為與場景資料
     */
    public CustomerCreditDto getConsumption(String serialNo) {
        return consumptionService.getConsumption(serialNo);
    }

    /**
     * 查詢 L2 資產負債 - 總覽
     *
     * @param serialNo 顧客識別流水號
     * @return 總覽
     */
    public CustomerAssetsDto.Overview getAssetOverview(String serialNo) {
        return customerL2AssetsService.getOverview(serialNo);
    }

    /**
     * 查詢 L2 資產負債 - 存款
     *
     * @param serialNo 顧客識別值
     * @return 存款
     */
    public CustomerL2AssetsQueryService.DepositResult getAssetDeposit(String serialNo) {
        try {
            return customerL2AssetsService.getDeposit(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }

    /**
     * 查詢 L2 資產負債 - 基金/ETF/債券
     *
     * @param serialNo 顧客識別值
     * @return 基金/ETF/債券
     */
    public CustomerL2AssetsQueryService.FundResult getAssetFund(String serialNo) {
        try {
            return customerL2AssetsService.getFund(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }

    /**
     * 查詢 L2 資產負債 - 保險
     *
     * @param serialNo 顧客識別值
     * @return 保險
     */
    public CustomerL2AssetsQueryService.InsuranceResult getAssetInsurance(String serialNo) {
        try {
            return customerL2AssetsService.getInsurance(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }

    /**
     * 查詢 L2 資產負債 - 黃金存摺
     *
     * @param serialNo 顧客識別值
     * @return 保險
     */
    public CustomerL2AssetsQueryService.GoldResult getAssetGold(String serialNo) {
        try {
            return customerL2AssetsService.getGold(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }

    /**
     * 查詢 L2 資產負債 - 財金商品
     *
     * @param serialNo 顧客識別值
     * @return 保險
     */
    public CustomerL2AssetsQueryService.WealthResult getAssetWealth(String serialNo) {
        try {
            return customerL2AssetsService.getWealth(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }

    /**
     * 查詢 L2 資產負債 - 信託
     *
     * @param serialNo 顧客識別值
     * @return 保險
     */
    public CustomerL2AssetsQueryService.TrustResult getAssetTrust(String serialNo) {
        try {
            return customerL2AssetsService.getTrust(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }

    /**
     * 查詢 L2 資產負債 - 貸款
     *
     * @param serialNo 顧客識別值
     * @return 保險
     */
    public CustomerL2AssetsQueryService.LoanResult getAssetLoan(String serialNo) {
        try {
            return customerL2AssetsService.getLoan(serialNo);
        } catch (ExternalSystemException exception) {
            throw new CustomerAccessCheckException(exception);
        }
    }
}