// package com.example.backend.common;

package com.esb.icrm.client.integration.csip.common;

import com.esb.icrm.client.config.MockProperties;
import com.esb.icrm.client.core.util.UrlBuilder;
import com.esb.icrm.client.integration.csip.config.CsipGatewayProperties;
import com.esb.icrm.exception.ExternalSystemException;
import com.fasterxmljackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownContentTypeException;

public abstract class CsipApiClient {

    /** CSIP Gateway bankapi 前綴 */
    protected static final String CSIP_GATEWAY_PREFIX = "esunbankt/bankapi/";

    /** CSIP Gateway RestClient */
    protected final RestClient csipRestClient;

    /** CSIP Gateway 設定屬性 */
    protected final CsipGatewayProperties csipProps;

    /** BaseUrlBuilder 物件 */
    protected final UrlBuilder urlBuilder;

    /** Mock 設定屬性 */
    protected final MockProperties mockProps;

    /** JSON 反序列化工具 */
    protected final ObjectMapper objectMapper;

    /**
     * 建構子
     *
     * @param csipRestClient CSIP Gateway RestClient Bean
     * @param csipProps      CSIP Gateway 設定屬性
     * @param urlBuilder     URL 建立工具
     * @param mockProps      Mock 設定屬性
     * @param objectMapper   JSON 反序列化工具
     */
    public CsipApiClient(RestClient csipRestClient, CsipGatewayProperties csipProps, UrlBuilder urlBuilder,
            MockProperties mockProps, ObjectMapper objectMapper) {
        this.csipRestClient = csipRestClient;
        this.csipProps = csipProps;
        this.urlBuilder = urlBuilder;
        this.mockProps = mockProps;
        this.objectMapper = objectMapper;
    }

    /**
     * 發送 CSIP POST 請求之通用方法
     *
     * @param <T>          Request Body 型態
     * @param <R>          Response Body 型態
     * @param postUrl      API 網址
     * @param targetSystem 目標系統代碼
     * @param request      請求封裝物件
     * @param responseType 回應類別別 ParameterizedTypeReference
     * @return CSIP 包含回應 Header 與 Body
     */
    protected <T, R> R post(
            String postUrl,
            String targetSystem,
            T request,
            ParameterizedTypeReference<R> responseType) {

        R response;

        try {
            response = csipRestClient.post()
                    .uri(postUrl)
                    .header("X-Target-System", targetSystem)
                    .body(request)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (req, res) -> {
                                throw new ExternalSystemException(targetSystem,
                                        "HTTP " + res.getStatusCode());
                            })
                    .body(responseType);
        } catch (UnknownContentTypeException ex) {
            response = deserializeOctetStreamResponse(ex, responseType, targetSystem);
        } catch (RestClientException ex) {
            throw new ExternalSystemException(targetSystem, "呼叫失敗: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ExternalSystemException(targetSystem, "處理回應失敗: " + ex.getMessage(), ex);
        }

        return response;
    }

    protected String getPostUrl(String apiAction, String baseUrl) {
        String urlSuffix = resolveUrlSuffix(apiAction, baseUrl);
        return urlBuilder.buildUrl(
                csipProps.serverHost(),
                apiAction,
                urlSuffix,
                csipProps.clientId());
    }

    /**
     * 依 mock 設定決定 CPMP 實際呼叫路徑
     * <p>
     * CSIP Gateway 需帶 bankapi 前綴；mockServer 則直接對應 CPMP API 路徑
     * </p>
     *
     * @param apiAction API 動作名稱
     * @return 實際 URL 後綴
     */
    private String resolveUrlSuffix(String apiAction, String baseUrl) {
        String defaultUrlSuffix = joinUrl(baseUrl, apiAction);
        return isMockRouteEnabled(apiAction) ? stripGatewayPrefix(defaultUrlSuffix) : defaultUrlSuffix;
    }

    /**
     * 判斷特定 CPMP API 是否啟用 mock route
     *
     * @param apiAction API 動作名稱
     * @return true 表示打 mockServer
     */
    protected boolean isMockRouteEnabled(String apiAction) {
        return mockProps != null
                && mockProps.mockService() != null
                && mockProps.mockService().contains(apiAction)
                && mockProps.mockServerHost() != null
                && !mockProps.mockServerHost().isBlank();
    }

    /**
     * 安全拼接 baseUrl 與 API path，避免重複斜線
     *
     * @param baseUrl baseUrl
     * @param apiPath API path
     * @return 拼接後路徑
     */
    private String joinUrl(String baseUrl, String apiPath) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedApiPath = apiPath.startsWith("/") ? apiPath.substring(1) : apiPath;
        return normalizedBaseUrl + "/" + normalizedApiPath;
    }

    /**
     * 移除 mockServer 不需要之 CSIP Gateway bankapi 前綴
     *
     * @param urlSuffix 原始 URL 後綴
     * @return 移除前綴後的 URL 後綴
     */
    private String stripGatewayPrefix(String urlSuffix) {
        if (urlSuffix.startsWith(CSIP_GATEWAY_PREFIX)) {
            return urlSuffix.substring(CSIP_GATEWAY_PREFIX.length());
        }
        return urlSuffix;
    }

    /**
     * 反序列化 mockServer 回傳的 octet-stream JSON
     *
     * @param ex           UnknownContentTypeException
     * @param responseType 回應型別
     * @param <R>          回應內容型別
     * @return 反序列化後的回應物件
     */
    private <R> R deserializeOctetStreamResponse(
            UnknownContentTypeException ex,
            ParameterizedTypeReference<R> responseType,
            String targetSystem) {
        try {
            return objectMapper.readValue(
                    ex.getResponseBodyAsString(),
                    objectMapper.constructType(responseType.getType()));
        } catch (Exception jsonEx) {
            throw new ExternalSystemException(targetSystem, "處理回應失敗: " + jsonEx.getMessage(), jsonEx);
        }
    }
}