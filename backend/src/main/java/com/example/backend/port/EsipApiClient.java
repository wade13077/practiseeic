package com.example.backend.port;

public class EsipApiClient {
    
}
package com.esb.icrm.client.integration.esip.common;

import com.esb.icrm.client.config.MockProperties;
import com.esb.icrm.client.core.util.UrlBuilder;
import com.esb.icrm.client.integration.esip.config.EsipGatewayProperties;
import com.esb.icrm.constant.ForeignSystem;
import com.esb.icrm.exception.ExternalSystemException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownContentTypeException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class EsipApiClient {

    /** ESIP Gateway bankapi 前綴 */
    protected static final String ESIP_GATEWAY_PREFIX = "esunbankt/bankapi/";

    /** ESIP Gateway RestClient */
    protected final RestClient esipRestClient;

    /** ESIP Gateway 設定屬性 */
    protected final EsipGatewayProperties esipProps;

    /** BaseUrlBuilder 物件 */
    protected final UrlBuilder urlBuilder;

    /** Mock 設定屬性 */
    protected final MockProperties mockProps;

    /** JSON 反序列化工具 */
    protected final ObjectMapper objectMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    protected EsipApiClient(
            RestClient esipRestClient,
            EsipGatewayProperties esipProps,
            UrlBuilder urlBuilder,
            MockProperties mockProps,
            ObjectMapper objectMapper) {
        this.esipRestClient = esipRestClient;
        this.esipProps = esipProps;
        this.urlBuilder = urlBuilder;
        this.mockProps = mockProps;
        this.objectMapper = objectMapper;
    }

    protected static String generateMsgNo() {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        int randomNum = SECURE_RANDOM.nextInt(100000);
        return ForeignSystem.CRM_SYS_CODE + "_" + timestamp + String.format("%05d", randomNum);
    }

    protected static String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    protected static int getRandomNum(int bound) {
        return SECURE_RANDOM.nextInt(bound);
    }

    /**
     * 發送 ESIP POST 請求之通用方法
     *
     * @param <T> Request Body 型態
     * @param <R> Response Body 型態
     * @param apiAction API 動作名稱
     * @param baseUrl API 網址前綴
     * @param targetSystem 目標系統代碼
     * @param request 請求封裝物件
     * @param responseType 回應內容型別 ParameterizedTypeReference
     * @return 包含整體回應內文
     */
    protected <T, R> R executePost(
            String apiAction,
            String baseUrl,
            String targetSystem,
            T request,
            ParameterizedTypeReference<R> responseType) {

        String urlSuffix = resolveUrlSuffix(apiAction, baseUrl);
        String postUrl = urlBuilder.buildUrl(
                esipProps.serverHost(),
                apiAction,
                urlSuffix,
                esipProps.clientId());

        try {
            return esipRestClient.post()
                    .uri(postUrl)
                    .header("X-Target-System", targetSystem)
                    .body(request)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (req, res) -> {
                                throw new ExternalSystemException(targetSystem, "HTTP " + res.getStatusCode());
                            })
                    .body(responseType);
        } catch (UnknownContentTypeException ex) {
            return deserializeOctetStreamResponse(ex, responseType, targetSystem);
        } catch (RestClientException ex) {
            throw new ExternalSystemException(targetSystem, "呼叫失敗：" + ex.getMessage(), ex);
        }
    }

    /**
     * 依 mock 設定決定 API 與實際呼叫路徑
     * <p>
     * ESIP Gateway 需帶 bankapi 前綴；mockServer 則直接對應 API 路徑
     * </p>
     *
     * @param apiAction API 動作名稱
     * @param baseUrl API 網址前綴
     * @return 實際 URL 後綴
     */
    private String resolveUrlSuffix(String apiAction, String baseUrl) {
        String defaultUrlSuffix = joinUrl(baseUrl, apiAction);
        return isMockRouteEnabled(apiAction) ? stripGatewayPrefix(defaultUrlSuffix) : defaultUrlSuffix;
    }

    /**
     * 判斷指定 ESIP API 是否啟用 mock route
     *
     * @param apiAction API 動作名稱
     * @return true 表示改打 mockServer
     */
    private boolean isMockRouteEnabled(String apiAction) {
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
     * 移除 mockServer 不需要的 ESIP Gateway bankapi 前綴
     *
     * @param urlSuffix 原始 URL 後綴
     * @return 移除前綴後的 URL 後綴
     */
    private String stripGatewayPrefix(String urlSuffix) {
        if (urlSuffix.startsWith(ESIP_GATEWAY_PREFIX)) {
            return urlSuffix.substring(ESIP_GATEWAY_PREFIX.length());
        }
        return urlSuffix;
    }

    /**
     * 反序列化 mockServer 回傳的 octet-stream JSON
     *
     * @param ex UnknownContentTypeException
     * @param responseType 回應型態
     * @param <R> 回應內容型別
     * @param targetSystem 目標系統代碼
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
            throw new ExternalSystemException(targetSystem, "處理回應失敗：" + jsonEx.getMessage(), jsonEx);
        }
    }
}