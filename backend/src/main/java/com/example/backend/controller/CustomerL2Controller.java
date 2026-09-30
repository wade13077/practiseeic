package com.example.backend.controller;

// package com.esb.icrm.management.customer.interfaces.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esb.icrm.constant.ServiceCode;
import com.esb.icrm.management.customer.interfaces.dto.*;
import com.esb.icrm.constant.AuditModule;
import com.esb.icrm.management.customer.application.query.CustomerInteractionService;
import com.esb.icrm.management.customer.application.query.CustomerL2QueryService;
import com.esb.icrm.persistence.edb.customer.dto.CustomerInteractionDto;
import com.esb.icrm.shared.dto.ApiResponse;
import com.esb.icrm.shared.annotation.AuditLog;

/**
 * 個人 360 L2 查詢 Controller
 *
 * <p>
 * 左側顧客資訊摘要固定由 summary API 查詢，右側頁籤則依畫面需求分別查詢
 * </p>
 */
@RestController
@RequestMapping("/api/customer/{serialNo}/l2")
public class CustomerL2Controller {

    /** 互動紀錄查詢服務 */
    private final CustomerInteractionService customerInteractionService;

    /** 個人 360 L2 查詢門面 */
    private final CustomerL2QueryService customerL2QueryService;

    /**
     * 建構子
     */
    public CustomerL2Controller(CustomerInteractionService customerInteractionService,
            CustomerL2QueryService customerL2QueryService) {
        this.customerInteractionService = customerInteractionService;
        this.customerL2QueryService = customerL2QueryService;
    }

    /**
     * 查詢顧客資訊摘要
     *
     * @param serialNo 顧客識別流水號
     * @return 顧客資訊摘要
     */
    @GetMapping("/summary")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerInfoDto> getCustomerInfo(@PathVariable("serialNo") String serialNo) {
        return ApiResponse.ok(customerL2QueryService.getSummary(serialNo));
    }

    /**
     * 查詢顧客基本資料
     *
     * @param serialNo 顧客識別流水號
     * @return 顧客基本資料
     */
    @GetMapping("/basic")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerBasicDto> getCustomerBasic(@PathVariable("serialNo") String serialNo) {
        return ApiResponse.ok(customerL2QueryService.getBasic(serialNo));
    }

    /**
     * 查詢顧客資產負債 - 總覽
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 總覽
     */
    @GetMapping("/assets/overview")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.Overview> getAssetOverview(@PathVariable("serialNo") String serialNo) {
        return ApiResponse.ok(customerL2QueryService.getAssetOverview(serialNo));
    }

    /**
     * 查詢顧客資產負債 - 存款資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 存款資料
     */
    @GetMapping("/assets/deposits")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.Deposit> getDeposits(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetDeposit(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客資產負債 - 基金/ETF/債券資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 基金 / ETF / 債券資料
     */
    @GetMapping("/assets/funds-etf-bonds")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.Fund> getFundsEtfBonds(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetFund(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客資產負債 - 保險資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 保險資料
     */
    @GetMapping("/assets/insurance")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.InsuranceBlock> getInsurance(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetInsurance(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客資產負債 - 黃金存摺資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 黃金存摺資料
     */
    @GetMapping("/assets/gold")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.GoldBlock> getGold(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetGold(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客資產負債 - 財金商品資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 財金商品資料
     */
    @GetMapping("/assets/financial-products")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.Wealth> getFinancialProducts(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetWealth(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客資產負債 - 信託資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 信託資料
     */
    @GetMapping("/assets/trust")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.TrustBlock> getTrust(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetTrust(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客資產負債 - 貸款資料
     *
     * @param serialNo 顧客識別流水號
     * @return 資產負債 - 貸款資料
     */
    @GetMapping("/assets/loans")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAssetsDto.LoanBlock> getLoans(@PathVariable("serialNo") String serialNo) {
        var result = customerL2QueryService.getAssetLoan(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢顧客消費行為與場景
     *
     * @param serialNo 顧客識別流水號
     * @return 消費行為與場景資料
     */
    @GetMapping("/consumption")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerCreditDto> getConsumption(@PathVariable("serialNo") String serialNo) {
        return ApiResponse.ok(customerL2QueryService.getConsumption(serialNo));
    }

    /**
     * 查詢顧客互動紀錄
     *
     * @param serialNo 顧客識別流水號
     * @return 互動紀錄
     */
    @GetMapping("/interactions")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<List<CustomerInteractionDto>> getInteractions(@PathVariable("serialNo") String serialNo) {
        List<CustomerInteractionDto> interactions = customerInteractionService.getCustomerInteractions(serialNo);
        return ApiResponse.ok(interactions);
    }
}