package com.example.backend.controller;

// package com.esb.icrm.management.customer.interfaces.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esb.icrm.management.customer.application.query.CustomerQueryService;
import com.esb.icrm.constant.AuditModule;
import com.esb.icrm.constant.ServiceCode;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAccessCheckDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAuthorizationCheckDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerListItemDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerQueryLogRequest;
import com.esb.icrm.management.customer.interfaces.dto.RealTimeCustomerDto;
import com.esb.icrm.management.customer.interfaces.dto.SearchCustomerRequest;
import com.esb.icrm.shared.dto.ApiResponse;
import com.esb.icrm.shared.dto.PageResponse;
import com.esb.icrm.shared.annotation.AuditLog;

import jakarta.validation.Valid;

/**
 * 個人 360 資料查詢 Controller
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    /** 個人 360 查詢服務 */
    private final CustomerQueryService customerQueryService;

    /**
     * 建構子
     */
    public CustomerController(CustomerQueryService customerQueryService) {
        this.customerQueryService = customerQueryService;
    }

    /**
     * 查詢顧客列表
     *
     * <p>
     * 依搜尋條件分頁查詢顧客列表
     * </p>
     *
     * @param request 搜尋條件
     * @return 分頁顧客列表
     */
    @GetMapping
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<PageResponse<CustomerListItemDto>> searchCustomers(
            @Valid SearchCustomerRequest request) {
        var page = customerQueryService.searchCustomers(request);
        return ApiResponse.ok(PageResponse.of(page));
    }

    /**
     * 查詢個人 360 內部資料
     *
     * @param serialNo MongoDB 文件流水號
     * @return 個人 360 內部資料
     */
    @GetMapping("/{serialNo}")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerDto> getCustomer(@PathVariable String serialNo) {
        CustomerDto customerData = customerQueryService.getCustomerInternalData(serialNo);
        return ApiResponse.ok(customerData);
    }

    /**
     * 查詢個人 360 即時外部資料
     *
     * @param serialNo MongoDB 文件流水號
     * @return 個人 360 即時外部資料
     */
    @GetMapping("/{serialNo}/real-time")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<RealTimeCustomerDto> getRealTimeCustomer(@PathVariable String serialNo) {
        var result = customerQueryService.queryCustomerExternalData(serialNo);
        if (result.hasErrors()) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(),
                    result.realTimeData(), result.unavailableSections());
        }
        return ApiResponse.ok(result.realTimeData());
    }

    /**
     * 查詢個人 360 進入前檢核 (停用個資、特殊重要顧客、不直行銷、應注意名單)
     *
     * @param serialNo MongoDB 文件流水號
     * @return 進入前檢核結果
     */
    @GetMapping("/{serialNo}/access-check")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAccessCheckDto> getCustomerAccessCheck(@PathVariable String serialNo) {
        var result = customerQueryService.queryCustomerAccessCheck(serialNo);
        if (result.errorMessage() != null) {
            return ApiResponse.degraded(ServiceCode.CUSTOMER_DATA_ERROR, result.errorMessage(), result.data(),
                    List.of());
        }
        return ApiResponse.ok(result.data());
    }

    /**
     * 查詢個人 360 顧客頭像
     *
     * @param serialNo MongoDB 文件流水號
     * @return 顧客頭像
     */
    @GetMapping("/{serialNo}/image")
    public ApiResponse<RealTimeCustomerDto.ProfilePhoto> getCustomerImage(@PathVariable String serialNo) {
        return ApiResponse.ok(customerQueryService.getCustomerProfilePhoto(serialNo));
    }

    /**
     * 查詢個人 360 純權限檢核，不呼叫外部顧客資料服務
     *
     * @param serialNo MongoDB 文件流水號
     * @return 純權限檢核結果
     */
    @GetMapping("/{serialNo}/authorization-check")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<CustomerAuthorizationCheckDto> getCustomerAuthorizationCheck(
            @PathVariable String serialNo) {
        return ApiResponse.ok(customerQueryService.getCustomerAuthorizationCheck(serialNo));
    }

    /**
     * 記錄個人 360 L1 顧客查詢
     *
     * @param serialNo MongoDB 文件流水號
     * @param request  查詢原因
     * @return 空成功回應
     */
    @PostMapping("/{serialNo}/query-log")
    @AuditLog(module = AuditModule.CUSTOMER)
    public ApiResponse<Void> recordCustomerQuery(@PathVariable String serialNo,
            @Valid @RequestBody CustomerQueryLogRequest request) {
        customerQueryService.recordCustomerQuery(serialNo, request.queryReason());
        return ApiResponse.ok();
    }
}
