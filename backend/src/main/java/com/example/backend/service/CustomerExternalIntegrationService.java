package com.example.backend.service;

// package com.esb.icrm.management.customer.application.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.esb.icrm.client.integration.ecsg.system.edls.dto.request.EdlsQueryCustAccountInfoListRequestBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.request.EdlsQueryTdAccRequestBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryCustAccountInfoListResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryTdAccResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.port.EdlsClientPort;
import com.esb.icrm.client.integration.esip.system.wcp.dto.request.WcpQueryTxHistoryInfoRequestBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryTxHistoryInfoResponseBody;
import org.springframework.stereotype.Service;

import com.esb.icrm.client.integration.esip.system.cip.dto.request.FindCustomerInfoRequestBody;
import com.esb.icrm.client.integration.esip.system.cip.port.CipClientPort;
import com.esb.icrm.client.integration.esip.system.sacs.dto.request.ReadAttentionCustomerRequestBody;
import com.esb.icrm.client.integration.esip.system.sacs.dto.response.AttentionCustomerDto;
import com.esb.icrm.client.integration.esip.system.sacs.port.SacsClientPort;
import com.esb.icrm.client.integration.esip.system.wcp.dto.request.WcpQueryCstmrInStockRequestBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.request.WcpQueryWiseCstmrInfoFromDgtlChnlRequestBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryCstmrInStockResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.port.WcpClientPort;
import com.esb.icrm.client.integration.seal.dto.request.QueryImageRequestBody;
import com.esb.icrm.client.integration.seal.dto.response.QueryImageResponseBody;
import com.esb.icrm.client.integration.seal.port.SealClientPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 外部系統整合服務層 (SEAL, CIP, WCP, SACS)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerExternalIntegrationService {

    /** SEAL 印鑑系統介面 */
    private final SealClientPort sealClientPort;

    /** CIP 顧客資訊整合平台介面 */
    private final CipClientPort cipClientPort;

    /** WCP 財富管理系統介面 */
    private final WcpClientPort wcpClientPort;

    /** SACS 應注意名單系統介面 */
    private final SacsClientPort sacsClientPort;

    /** EDLS 玉山存放款系統介面 */
    private final EdlsClientPort edlsClientPort;

    /**
     * 取得印鑑系統頭像
     *
     * @param customerId    顧客 ID (10碼)
     * @param currentUser   查詢者員編 (5碼)
     * @param currentBranch 查詢者分行
     *
     * @return 印鑑頭像 Base64 字串，若無則回傳空字串
     */
    public String getSealProfilePhoto(String customerId, String currentUser, String currentBranch) {
        try {
            QueryImageRequestBody request = new QueryImageRequestBody(customerId, currentUser, currentBranch);
            QueryImageResponseBody response = sealClientPort.queryImage(request);

            if (response != null && response.pictures() != null && !response.pictures().isEmpty()) {
                String rawBase64 = response.pictures().getFirst();
                return rawBase64.startsWith("data:image") ? rawBase64 : "data:image/jpeg;base64," + rawBase64;
            }
        } catch (Exception e) {
            log.warn("呼叫 SEAL 系統取得頭像 [{}] 失敗: {}", customerId, e.getMessage());
        }
        return "";
    }

    /**
     * 取得 CIP 顧客資訊
     */
    public Map<String, Object> fetchCipCustomerInfo(String customerId, String birthdayStr, String circiKey,
            String currentUser, String currentBranch) {
        try {
            FindCustomerInfoRequestBody cipReq = new FindCustomerInfoRequestBody(customerId, birthdayStr, circiKey,
                    null, null, null);
            Map<String, Object> res = cipClientPort.findCustomerInfo(cipReq, currentUser, currentBranch, currentUser);
            return res != null ? res : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("呼叫 CIP 系統取得顧客 [{}] 資訊失敗: {}", customerId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 統一查詢 WCP 顧客庫存
     */
    public WcpQueryCstmrInStockResponseBody fetchWcpCustomerInStock(String customerId, String circiKey,
            String currentUser, String currentIpAddress, String currentBranch) {
        if (circiKey == null || circiKey.isBlank()) {
            return null;
        }
        try {
            WcpQueryCstmrInStockRequestBody wcpReq = new WcpQueryCstmrInStockRequestBody(customerId, "", "", "", "",
                    "");
            // 注意: 這裡接收外部傳入的 IP
            return wcpClientPort.queryCstmrInStock(wcpReq, currentUser, currentIpAddress, currentBranch);
        } catch (Exception e) {
            log.warn("呼叫 WCP 系統查詢顧客賬戶 [{}] 庫存失敗: {}", circiKey, e.getMessage());
            return null;
        }
    }

    /**
     * 查詢 WCP 特殊客群資訊 (單篇/重大傷病)
     */
    public WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody fetchWcpWiseCstmrInfoFromDgtlChnl(String certNo,
            String currentUser, String currentBranch, String currentIpAddress) {
        if (certNo == null || certNo.isBlank()) {
            return null;
        }
        try {
            WcpQueryWiseCstmrInfoFromDgtlChnlRequestBody req = new WcpQueryWiseCstmrInfoFromDgtlChnlRequestBody(certNo);
            return wcpClientPort.queryWiseCstmrInfo(req, currentUser, currentIpAddress, currentBranch);
        } catch (Exception e) {
            log.warn("呼叫 WCP 系統查詢顧客特殊資訊 [{}] 失敗: {}", certNo, e.getMessage());
            return null;
        }
    }

    /**
     * 查詢 SACS 應注意名單 (統一編號查詢，僅回傳本人資料)
     *
     * @param certNo 顧客身分證號 (作為 SACS 統一編號)
     */
    public List<AttentionCustomerDto> fetchSacsAttentionCustomer(String certNo, String currentUser,
            String currentBranch) {
        if (certNo == null || certNo.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ReadAttentionCustomerRequestBody request = new ReadAttentionCustomerRequestBody("1", certNo, null, null);
            List<AttentionCustomerDto> result = sacsClientPort.readAttentionCustomer(request, currentUser,
                    currentBranch);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 SACS 系統查詢顧客 [{}] 應注意名單失敗: {}", certNo, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 查詢 EDLS 帳戶號碼清單查詢
     */
    public List<EdlsQueryCustAccountInfoListResponseBody.AccountInfo> queryEdlsAccountInfoList(
            String certNo,
            String user,
            String branch) {

        EdlsQueryCustAccountInfoListRequestBody req = new EdlsQueryCustAccountInfoListRequestBody(
                certNo,
                null,
                null,
                null,
                "Y",
                "Y",
                "N",
                "N",
                "N",
                "N",
                "N",
                "N",
                "N",
                "N",
                List.of("0"),
                "Y",
                "N",
                null,
                1,
                50);

        List<EdlsQueryCustAccountInfoListResponseBody.AccountInfo> accountInfoList = Collections.emptyList();

        try {
            EdlsQueryCustAccountInfoListResponseBody res = edlsClientPort.queryCustAccountInfoList(req, user, branch);

            if (res != null && res.accountInfoList() != null) {
                accountInfoList = res.accountInfoList();
            }
        } catch (Exception e) {
            log.warn("呼叫 EDLS 系統，查詢活存失敗: {}", e.getMessage());
        }

        return accountInfoList;
    }

    /**
     * 查詢 EDLS 定存查詢
     */
    public List<EdlsQueryTdAccResponseBody.TdAccountInfo> queryEdlsTdAccList(
            String certNo,
            String user,
            String branch) {

        EdlsQueryTdAccRequestBody req = new EdlsQueryTdAccRequestBody(certNo, null, "0", 1, 50);
        List<EdlsQueryTdAccResponseBody.TdAccountInfo> tdAccList = Collections.emptyList();
        try {
            EdlsQueryTdAccResponseBody res = edlsClientPort.queryTdAcc(req, user, branch);

            if (res != null && res.tdAccList() != null) {
                tdAccList = res.tdAccList();
            }
        } catch (Exception e) {
            log.warn("呼叫 EDLS 系統查詢定存失敗: {}", e.getMessage());
        }

        return tdAccList;
    }

    /**
     * 查詢 WCP 即時查詢顧客特定金融信託的庫存資訊 (含基金/債券/股票)
     */
    public WcpQueryCstmrInStockResponseBody queryWcpCustomerInStock(
            String certNo,
            String user,
            String branch,
            String ipAddress) {
        try {
            WcpQueryCstmrInStockRequestBody req = new WcpQueryCstmrInStockRequestBody(certNo, "", "", "", "", "");
            return wcpClientPort.queryCstmrInStock(req, user, ipAddress, branch);
        } catch (Exception e) {
            log.warn("呼叫 WCP 系統查詢顧客帳戶 [{}] 失敗: {}", certNo, e.getMessage());
            return null;
        }
    }

    /**
     * 查詢 WCP 查詢交易歷史資料
     */
    public List<WcpQueryTxHistoryInfoResponseBody> queryWcpTxHistoryInfo(
            String certNo,
            String user,
            String branch,
            String ipAddress) {
        try {
            WcpQueryTxHistoryInfoRequestBody req = new WcpQueryTxHistoryInfoRequestBody(
                    certNo,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            List<WcpQueryTxHistoryInfoResponseBody> res = wcpClientPort.queryTxHistoryInfo(req, user, ipAddress,
                    branch);
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 WCP 查詢交易歷史資料 [{}] 失敗: {}", certNo, e.getMessage());
            return Collections.emptyList();
        }
    }
}