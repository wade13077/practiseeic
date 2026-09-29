package com.example.backend.port;

public class TrustApiClient {
    
}
package com.esb.icrm.client.integration.esip.system.trust;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.esb.icrm.annotation.ClientSnapshot;
import com.esb.icrm.annotation.OutboundLog;
import com.esb.icrm.client.config.MockProperties;
import com.esb.icrm.client.core.util.UrlBuilder;
import com.esb.icrm.client.integration.esip.common.EsipApiClient;
import com.esb.icrm.client.integration.esip.config.EsipGatewayProperties;
import com.esb.icrm.client.integration.esip.system.trust.dto.request.TrustCommonRequestBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.request.TrustContractRequestBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.ContractDepositInventoryDto;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.ContractFundInventoryDto;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.QueryCustomerDetailsResponseBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.TrustArrayResponseBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.TrustCommonResponseBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.TrustContractOverviewDto;
import com.esb.icrm.constant.ForeignSystem;
import com.esb.icrm.exception.ExternalSystemException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * TRUST 系統 API 客戶端
 * <p>
 * 透過 ESIP Gateway 呼叫 TRUST 系統，取得台灣信託業務資訊。每個方法均標記 {@code @OutboundLog}，由 AOP 自動記錄 Outbound Log。
 * </p>
 */
@Component
@Slf4j
public class TrustApiClient extends EsipApiClient {

    /**
     * TRUST BASE URL
     */
    @Value("${icrm.client.esip.trust.baseUrl}")
    private String trustBaseUrl;

    /**
     * TRUST 查詢信託業務顧客基本資料查詢服務(TMI030) API 名稱
     */
    private static final String API_QUERY_CUSTOMER_DETAILS = "queryCustomerDetails";

    /**
     * TRUST 查詢提供個人信託契約總覽查詢(全部契約)服務(TMI031) API 名稱
     */
    private static final String API_QUERY_ALL_TRUST_CONTRACTS_OVERVIEW = "queryAllTrustContractsOverview";

    /**
     * TRUST 查詢存款庫存明細(TMI015) API 名稱
     */
    private static final String API_QUERY_CONTRACT_DEPOSIT_INVENTORY = "queryContractDepositInventory";

    /**
     * TRUST 查詢基金重存明細(TMI017) API 名稱
     */
    private static final String API_QUERY_CONTRACT_FUND_INVENTORY = "queryContractFundInventory";

    /**
     * TRUST 查詢海外債券庫存明細(TMI018) API 名稱
     */
    private static final String API_QUERY_CONTRACT_FOREIGN_BOND_INVENTORY = "queryContractForeignBondInventory";

    /**
     * 系統代碼 (對應 Outbound Log 與 Header)
     */
    private static final String TARGET_SYSTEM = ForeignSystem.RISE_SYS_ID;

    /**
     * 建構子
     *
     * @param esipRestClient ESIP Gateway RestClient Bean
     * @param esipProps ESIP Gateway 設定屬性
     * @param urlBuilder URL 建立工具
     * @param mockProps Mock 設定屬性
     * @param objectMapper JSON 反序列化工具
     */
    public TrustApiClient(
            RestClient esipRestClient,
            EsipGatewayProperties esipProps,
            UrlBuilder urlBuilder,
            MockProperties mockProps,
            ObjectMapper objectMapper) {
        super(esipRestClient,
                esipProps,
                urlBuilder,
                mockProps,
                objectMapper);
    }

    /**
     * 信託業務顧客基本資料查詢服務(TMI030)
     *
     * @param requestBody 查詢條件
     * @return 基本資料
     */
    @OutboundLog(targetSystem = TARGET_SYSTEM)
    public QueryCustomerDetailsResponseBody queryCustomerDetails(
            TrustCommonRequestBody requestBody) {

        requestBody.setCode("TMI030");

        QueryCustomerDetailsResponseBody response = executePost(
                API_QUERY_CUSTOMER_DETAILS,
                trustBaseUrl,
                TARGET_SYSTEM,
                requestBody,
                new ParameterizedTypeReference<>() {
                });

        validateResponse(response);

        return response;
    }

    /**
     * 提供個人信託契約總覽查詢(全部契約)服務(TMI031)
     *
     * @param requestBody 查詢條件
     * @return 契約清單
     */
    @OutboundLog(targetSystem = TARGET_SYSTEM)
    @ClientSnapshot
    public List<TrustContractOverviewDto> queryAllTrustContractOverview(
            TrustCommonRequestBody requestBody) {

        requestBody.setCode("TMI031");
        return executePagedPost(
                API_QUERY_ALL_TRUST_CONTRACTS_OVERVIEW,
                requestBody,
                new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 存款庫存明細(TMI015)
     *
     * @param requestBody 查詢條件
     * @return 存款庫存清單
     */
    @OutboundLog(targetSystem = TARGET_SYSTEM)
    @ClientSnapshot
    public List<ContractDepositInventoryDto> queryContractDepositInventory(
            TrustContractRequestBody requestBody) {

        requestBody.setCode("TMI015");
        return executePagedPost(
                API_QUERY_CONTRACT_DEPOSIT_INVENTORY,
                requestBody,
                new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 基金重存明細(TMI017)
     *
     * @param requestBody 查詢條件
     * @return 基金重存清單
     */
    @OutboundLog(targetSystem = TARGET_SYSTEM)
    @ClientSnapshot
    public List<ContractFundInventoryDto> queryContractFundInventory(
            TrustContractRequestBody requestBody) {

        requestBody.setCode("TMI017");
        return executePagedPost(
                API_QUERY_CONTRACT_FUND_INVENTORY,
                requestBody,
                new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 海外債券庫存明細(TMI018)
     *
     * @param requestBody 查詢條件
     * @return 海外債券庫存清單
     */
    @OutboundLog(targetSystem = TARGET_SYSTEM)
    @ClientSnapshot
    public List<ContractFundInventoryDto> queryContractForeignBondInventory(
            TrustContractRequestBody requestBody) {

        requestBody.setCode("TMI018");
        return executePagedPost(
                API_QUERY_CONTRACT_FOREIGN_BOND_INVENTORY,
                requestBody,
                new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 發送電文 若下行 pageCode="N"，則需以上一頁電文上行的 count + 下行的 count，代入上行 count 再打一次電文直到 pageCode="E" 再將每次下行的 array 組合回傳
     *
     * @param api TRUST API
     * @param request 請求內容
     * @param typeReference 回傳物件型態
     */
    private <T> List<T> executePagedPost(
            String api,
            TrustCommonRequestBody request,
            ParameterizedTypeReference<TrustArrayResponseBody<T>> typeReference) {
        List<T> resultList = new ArrayList<>();
        int count = 1;

        while (true) {
            request.setCount(String.format("%04d", count));

            TrustArrayResponseBody<T> response = executePost(
                    api,
                    trustBaseUrl,
                    TARGET_SYSTEM,
                    request,
                    typeReference);

            validateResponse(response);

            resultList.addAll(response.getArray());
            String pageCode = response.getPageCode();

            if ("E".equals(pageCode)) {
                break;
            }

            count += response.getCount();
        }

        return resultList;
    }

    /**
     * 驗證 TRUST API 回應內容
     *
     * @param response TRUST API 回應物件
     */
    private void validateResponse(TrustCommonResponseBody response) {
        if (response == null) {
            throw new ExternalSystemException(TARGET_SYSTEM, "API 回應為空或格式錯誤");
        }

        if (response.getErrorMessage() != null && !response.getErrorMessage().isBlank()) {
            throw new ExternalSystemException(
                    TARGET_SYSTEM,
                    String.format("系統錯誤[%s]", response.getErrorMessage()));
        }
    }
}