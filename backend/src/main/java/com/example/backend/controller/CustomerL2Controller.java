package com.example.backend.controller;

// package com.esb.icrm.management.customer.interfaces.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.esb.icrm.management.customer.application.query.CustomerInteractionService;
import com.esb.icrm.management.customer.application.query.CustomerL2QueryService;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAssetsDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerBasicDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerCreditDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerInfoDto;
import com.esb.icrm.persistence.edb.customer.dto.CustomerInteractionDto;
import com.esb.icrm.shared.dto.ApiResponse;

/**
 * 個人 360 L2 查詢 Controller
 *
 * <p>
 * 左側顧客資訊摘要固定由 summary API 查詢，右側頁籤則依畫面需求分別查詢
 * </p>
 */
@RestController
@RequestMapping("/api/customer/{customerId}/l2")
public class CustomerL2Controller {

    /**
     * 互動紀錄查詢服務
     */
    private final CustomerInteractionService customerInteractionService;

    /**
     * 個人 360 L2 查詢門面
     */
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
     * @param customerId 顧客識別流水號
     * @return 顧客資訊摘要
     */
    @GetMapping("/summary")
    public ApiResponse<CustomerInfoDto> getCustomerInfo(@PathVariable String customerId) {
        return ApiResponse.ok(customerL2QueryService.getSummary(customerId));
    }

    /**
     * 查詢顧客基本資料
     *
     * @param customerId 顧客識別流水號
     * @return 顧客基本資料
     */
    @GetMapping("/basic")
    public ApiResponse<CustomerBasicDto> getCustomerBasic(@PathVariable String customerId) {
        return ApiResponse.ok(customerL2QueryService.getBasic(customerId));
    }

    /**
     * 查詢顧客資產負債 - 總覽
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 總覽
     */
    @GetMapping("/assets/overview")
    public ApiResponse<CustomerAssetsDto.Overview> getAssetOverview(@PathVariable String customerId) {
        return ApiResponse.ok(customerL2QueryService.getAssetOverview("6aaa0608bd81fc50a9dcfc2e"));
    }

    /**
     * 查詢顧客資產負債 - 存款資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 存款資料
     */
    @GetMapping("/assets/deposits")
    public ApiResponse<CustomerAssetsDto.Deposit> getDeposits(@PathVariable String customerId) {
        return ApiResponse.ok(customerL2QueryService.getAssetDeposit(customerId));
    }

    /**
     * 查詢顧客資產負債 - 基金 / ETF / 債券資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 基金 / ETF / 債券資料
     */
    @GetMapping("/assets/funds-etf-bonds")
    public ApiResponse<CustomerAssetsDto.Fund> getFundsEtfBonds(@PathVariable String customerId) {
        return ApiResponse.ok(customerL2QueryService.getAssetFund(customerId));
    }

    /**
     * 查詢顧客資產負債 - 保險資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 保險資料
     */
    @GetMapping("/assets/insurance")
    public ApiResponse<CustomerAssetsDto.InsuranceBlock> getInsurance(@PathVariable String customerId) {
        return ApiResponse.ok(customerL2QueryService.getAssetInsurance(customerId));
    }

    /**
     * 查詢顧客資產負債 - 黃金存折資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 黃金存折資料
     */
    @GetMapping("/assets/gold")
    public ApiResponse<CustomerAssetsDto.Gold> getGold(@PathVariable String customerId) {
        return notImplemented("L2 黃金存折查詢尚未接上查詢服務");
    }

    /**
     * 查詢顧客資產負債 - 財金商品資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 財金商品資料
     */
    @GetMapping("/assets/financial-products")
    public ApiResponse<CustomerAssetsDto.Wealth> getFinancialProducts(@PathVariable String customerId) {
        return notImplemented("L2 財金商品查詢尚未接上查詢服務");
    }

    /**
     * 查詢顧客資產負債 - 信託資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 信託資料
     */
    @GetMapping("/assets/trust")
    public ApiResponse<CustomerAssetsDto.Trust> getTrust(@PathVariable String customerId) {
        return notImplemented("L2 信託查詢尚未接上查詢服務");
    }

    /**
     * 查詢顧客資產負債 - 貸款資料
     *
     * @param customerId 顧客識別流水號
     * @return 資產負債 - 貸款資料
     */
    @GetMapping("/assets/loans")
    public ApiResponse<CustomerAssetsDto.Loan> getLoans(@PathVariable String customerId) {
        return notImplemented("L2 貸款查詢尚未接上查詢服務");
    }

    /**
     * 查詢顧客消費行為與場景
     *
     * @param customerId 顧客識別流水號
     * @return 消費行為與場景資料
     */
    @GetMapping("/consumption")
    public ApiResponse<CustomerCreditDto> getConsumption(@PathVariable String customerId) {
        return notImplemented("L2 消費行為與場景查詢尚未接上查詢服務");
    }

    /**
     * 查詢顧客互動紀錄
     *
     * @param serialNo 顧客識別流水號
     * @return 互動紀錄
     */
    @GetMapping("/interactions")
    public ApiResponse<List<CustomerInteractionDto>> getInteractions(@PathVariable("customerId") String serialNo) {
        List<CustomerInteractionDto> interactions = customerInteractionService.getCustomerInteractions(serialNo);
        return ApiResponse.ok(interactions);
    }

    private <T> ApiResponse<T> notImplemented(String message) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, message);
    }
}