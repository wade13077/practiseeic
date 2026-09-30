package com.example.backend.service;

package com.esb.icrm.management.customer.application.query;

import com.esb.icrm.client.integration.ecsg.system.edls.dto.request.EdlsQueryCustAccountInfoListRequestBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.request.EdlsQueryCustCreditDetailRequestBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.request.EdlsQueryTdAccRequestBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryCustAccountInfoListResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryCustCreditDetailResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryTdAccResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.port.EdlsClientPort;
import com.esb.icrm.client.integration.esip.system.cip.dto.request.FindCustomerInfoRequestBody;
import com.esb.icrm.client.integration.esip.system.cip.port.CipClientPort;
import com.esb.icrm.client.integration.esip.system.rise.dto.response.ReadGoldPassBookInventoryResponseBody;
import com.esb.icrm.client.integration.esip.system.rise.port.RiseClientPort;
import com.esb.icrm.client.integration.esip.system.sacs.dto.request.ReadAttentionCustomerRequestBody;
import com.esb.icrm.client.integration.esip.system.sacs.dto.response.AttentionCustomerDto;
import com.esb.icrm.client.integration.esip.system.sacs.port.SacsClientPort;
import com.esb.icrm.client.integration.esip.system.trust.dto.request.TrustCommonRequestBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.request.TrustContractRequestBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.ContractDepositInventoryDto;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.ContractFundInventoryDto;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.QueryCustomerDetailsResponseBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.TrustContractsOverviewDto;
import com.esb.icrm.client.integration.esip.system.trust.port.TrustClientPort;
import com.esb.icrm.client.integration.esip.system.tss.dto.response.DciOutstandingDataResponseBody;
import com.esb.icrm.client.integration.esip.system.tss.dto.response.FBondOutstandingDataResponseBody;
import com.esb.icrm.client.integration.esip.system.tss.dto.response.RprsOutstandingDataResponseBody;
import com.esb.icrm.client.integration.esip.system.tss.dto.response.SiOutstandingDataResponseBody;
import com.esb.icrm.client.integration.esip.system.tss.port.TssClientPort;
import com.esb.icrm.client.integration.esip.system.wcp.dto.request.WcpQueryCstmrInStockRequestBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.request.WcpQueryTxHistoryInfoRequestBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.request.WcpQueryWiseCstmrInfoFromDgtlChnlRequestBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryCstmrInStockResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryTxHistoryInfoResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.port.WcpClientPort;
import com.esb.icrm.client.integration.seal.dto.request.QueryImageRequestBody;
import com.esb.icrm.client.integration.seal.dto.response.QueryImageResponseBody;
import com.esb.icrm.client.integration.seal.port.SealClientPort;
import com.esb.icrm.exception.ExternalSystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 外部系統整合服務 (SEAL, CIP, WCP, SACS)
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

    /** RISE 系統介面 */
    private final RiseClientPort riseClientPort;

    /** TSS 系統介面 */
    private final TssClientPort tssClientPort;

    /** Trust 系統介面 */
    private final TrustClientPort trustClientPort;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 取得印鑑系統影像
     *
     * @param customerId    顧客 ID (10碼)
     * @param currentUser   查詢人員員號 (5碼)
     * @param currentBranch 查詢分行
     * @return 印鑑影像 Base64 字串，若無則回傳空字串
     */
    public String getSealProfilePhoto(String customerId, String currentUser, String currentBranch) {
        try {
            QueryImageRequestBody request = new QueryImageRequestBody(customerId, currentUser, currentBranch);
            QueryImageResponseBody response = sealClientPort.queryImage(request);

            if (response != null && response.pictures() != null && !response.pictures().isEmpty()) {
                String rawBase64 = response.pictures().getFirst();
                if (StringUtils.hasText(rawBase64)) {
                    return rawBase64.startsWith("data:image") ? rawBase64 : "data:image/jpeg;base64," + rawBase64;
                }
            }
        } catch (RuntimeException e) {
            log.warn("呼叫 SEAL 系統取得顧客 [{}] 頭像失敗: {}", customerId, e.getMessage());
            throw new ExternalSystemException("SEAL", "取得顧客圖像失敗", e);
        }
        return "";
    }

    /**
     * 取得 CIP 顧客資訊
     * 取回清單後，使用 cipCustId 取第一筆資料
     */
    public Map<String, Object> fetchCipCustomerInfo(String certNo,
            String currentUser,
            String currentBranch,
            String cipCustId) {
        try {
            FindCustomerInfoRequestBody cipReq = new FindCustomerInfoRequestBody(certNo, null, null, null, null, null);
            List<Map<String, Object>> customerInfoList = cipClientPort.findCustomerInfo(cipReq, currentUser,
                    currentBranch, currentUser);
            return customerInfoList.stream()
                    .filter(data -> Objects.equals(cipCustId, data.get("custId")))
                    .findFirst().orElse(Collections.emptyMap());

        } catch (RuntimeException e) {
            log.warn("呼叫 CIP 系統取得顧客 [{}] 資訊失敗: {}", certNo, e.getMessage());
            throw new ExternalSystemException("CIP", "取得顧客資訊失敗", e);
        }
    }

    /**
     * 統一查詢 WCP 顧客庫存
     */
    public WcpQueryCstmrInStockResponseBody fetchWcpCustomerInStock(String circiKey, String currentUser,
            String currentBranch, String currentIpAddress) {
        if (circiKey == null || circiKey.isBlank()) {
            return null;
        }
        try {
            WcpQueryCstmrInStockRequestBody wcpReq = new WcpQueryCstmrInStockRequestBody(circiKey, "", "", "", "", "");
            // 注意: 這裡接收外部傳入的 IP
            return wcpClientPort.queryCstmrInStock(wcpReq, currentUser, currentIpAddress, currentBranch);
        } catch (RuntimeException e) {
            log.warn("呼叫 WCP 系統查詢顧客賬戶 [{}] 庫存失敗: {}", circiKey, e.getMessage());
            throw new ExternalSystemException("WCP", "查詢顧客庫存失敗", e);
        }
    }

    /**
     * 查詢 WCP 特殊客群資訊 (華層/高大值集)
     */
    public WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody fetchWcpWiseCstmrInfo(String certNo, String currentUser,
            String currentBranch, String currentIpAddress) {
        if (certNo == null || certNo.isBlank()) {
            return null;
        }
        try {
            WcpQueryWiseCstmrInfoFromDgtlChnlRequestBody req = new WcpQueryWiseCstmrInfoFromDgtlChnlRequestBody(certNo);
            return wcpClientPort.queryWiseCstmrInfo(req, currentUser, currentIpAddress, currentBranch);
        } catch (Exception e) {
            log.warn("呼叫 WCP 系統查詢顧客特殊資訊 [{}] 失敗: {}", certNo, e.getMessage());
            throw new ExternalSystemException("WCP", "查詢顧客特殊資訊失敗", e);
        }
    }

    /**
     * 查詢 SACS 注意名單 (統一邏輯，僅回傳本人資料)
     *
     * @param certNo 顧客身分證號 (作為 SACS 統一查詢)
     */
    public List<AttentionCustomerDto> fetchSacsAttentionCustomer(String certNo, String currentUser,
            String currentBranch) {
        if (certNo == null || certNo.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ReadAttentionCustomerRequestBody request = new ReadAttentionCustomerRequestBody("1", certNo, null, null);
            List<AttentionCustomerDto> result = sacsClientPort.readAttentionCustomer(request, currentUser,
                    currentBranch, currentUser);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 SACS 系統查詢顧客 [{}] 應注意名單失敗: {}", certNo, e.getMessage());
            throw new ExternalSystemException("SACS", "查詢應注意名單失敗", e);
        }
    }

    /**
     * 查詢 EDLS 活戶帳號清單查詢
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
            throw new ExternalSystemException("EDLS", "查詢活存失敗", e);
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
            throw new ExternalSystemException("EDLS", "查詢定存失敗", e);
        }

        return tdAccList;
    }

    /**
     * 查詢 WCP 即時查詢顧客特定金融信託的庫存資訊(含基金/債券/股票)
     */
    public WcpQueryCstmrInStockResponseBody queryWcpCustomerInStock(
            String certNo,
            String user,
            String branch,
            String ipAddress) {
        try {
            WcpQueryCstmrInStockRequestBody req = new WcpQueryCstmrInStockRequestBody(certNo, "", "", "I", "", "");
            return wcpClientPort.queryCstmrInStock(req, user, ipAddress, branch);
        } catch (Exception e) {
            log.warn("呼叫 WCP 系統查詢顧客帳戶 [{}] 失敗: {}", certNo, e.getMessage());
            throw new ExternalSystemException("WCP", "查詢顧客帳戶失敗", e);
        }
    }

    /**
     * 查詢 WCP 查詢交易歷史資料
     */
    public List<WcpQueryTxHistoryInfoResponseBody> queryWcpTxHistoryInfo(
            String certNo,
            String user,
            String branch,
            String ipAddress,
            String prdtType) {
        try {

            LocalDate today = LocalDate.now();
            LocalDate lastMonth = today.minusMonths(1L);
            String todayFormated = today.format(TIME_FORMATTER);
            String lastMonthFormated = lastMonth.format(TIME_FORMATTER);

            WcpQueryTxHistoryInfoRequestBody req = new WcpQueryTxHistoryInfoRequestBody(
                    certNo,
                    lastMonthFormated,
                    todayFormated,
                    prdtType,
                    null,
                    null,
                    // QueryTxHistoryInfoOrderTyp01:交易日期、QueryTxHistoryInfoOrderTyp02:產品名稱、QueryTxHistoryInfoOrderTyp03:信託編號
                    "QueryTxHistoryInfoOrderTyp02",
                    null,
                    // TxActTyp01:當日、TxActTyp02:預約
                    "TxActTyp01");

            List<WcpQueryTxHistoryInfoResponseBody> res = wcpClientPort.queryTxHistoryInfo(req, user, ipAddress,
                    branch);

            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 WCP 查詢交易歷史資料 [{}] 失敗: {}", certNo, e.getMessage());
            throw new ExternalSystemException("WCP", "查詢交易歷史資料失敗", e);
        }
    }

    /**
     * 查詢 存款帳戶_黃金存摺庫存
     */
    public ReadGoldPassBookInventoryResponseBody queryGoldPassBookInventory(String cbCustId) {
        try {
            return riseClientPort.readGoldPassBookInventory(cbCustId, null, null, null);
        } catch (Exception e) {
            log.warn("呼叫 存款帳戶_黃金存摺庫存 [{}] 失敗: {}", cbCustId, e.getMessage());
            throw new ExternalSystemException("RISE", "查詢存款帳戶_黃金存摺庫存失敗", e);
        }
    }

    /**
     * 查詢 顧客承作財金雙元貨幣組合式商品(DCI)庫存資料
     */
    public DciOutstandingDataResponseBody queryDciOutstandingData(String custIdIn, String dateIn) {
        try {
            return tssClientPort.dciOutstandingData(custIdIn, dateIn, null, null, null);
        } catch (Exception e) {
            log.warn("呼叫 顧客承作財金雙元貨幣組合式商品(DCI)庫存資料 [{}] 失敗: {}", custIdIn, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 顧客承作財金非雙元貨幣組合式商品(SI)交易庫存資料
     */
    public SiOutstandingDataResponseBody querySiOutstandingData(String custIdIn, String dateIn) {
        try {
            return tssClientPort.siOutstandingData(custIdIn, dateIn, null, null, null);
        } catch (Exception e) {
            log.warn("呼叫 顧客承作財金非雙元貨幣組合式商品(SI)交易庫存資料 [{}] 失敗: {}", custIdIn, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 顧客承作財金外幣自營債券交易損益資訊
     */
    public FBondOutstandingDataResponseBody queryFBondOutstandingData(String custIdIn, String dateIn) {
        try {
            return tssClientPort.fBondOutstandingData(custIdIn, dateIn, null, null, null);
        } catch (Exception e) {
            log.warn("呼叫 顧客承作財金外幣自營債券交易損益資訊 [{}] 失敗: {}", custIdIn, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 顧客承作財金買賣債券附條件交易庫存資料
     */
    public RprsOutstandingDataResponseBody queryRprsOutstandingData(String custIdIn, String dateIn) {
        try {
            return tssClientPort.rprsOutstandingData(custIdIn, dateIn, null, null, null);
        } catch (Exception e) {
            log.warn("呼叫 顧客承作財金買賣債券附條件交易庫存資料 [{}] 失敗: {}", custIdIn, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 信託業務顧客基本資料查詢服務(TMI030)
     */
    public QueryCustomerDetailsResponseBody queryCustomerDetails(String code, String idno, String birth, String count) {
        try {
            TrustCommonRequestBody req = new TrustCommonRequestBody();
            req.setCode(code);
            req.setIdno(idno);
            req.setBirth(birth);
            req.setCount(count);
            return trustClientPort.queryCustomerDetails(req);
        } catch (Exception e) {
            log.warn("呼叫 信託業務顧客基本資料查詢服務(TMI030) [{}] 失敗: {}", code, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 個託信託契約總覽查詢(全部契約)服務(TMI031)
     */
    public List<TrustContractsOverviewDto> queryAllTrustContractsOverview(String code, String idno, String birth,
            String count) {
        try {
            TrustCommonRequestBody req = new TrustCommonRequestBody();
            req.setCode(code);
            req.setIdno(idno);
            req.setBirth(birth);
            req.setCount(count);
            List<TrustContractsOverviewDto> res = trustClientPort.queryAllTrustContractsOverview(req);
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 個託信託契約總覽查詢(全部契約)服務(TMI031) [{}] 失敗: {}", code, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 存款庫存明細(TMI015)
     */
    public List<ContractDepositInventoryDto> queryContractDepositInventory(String code, String idno, String birth,
            String contractNo, String assetType, String count) {
        try {
            TrustContractRequestBody req = new TrustContractRequestBody();
            req.setCode(code);
            req.setIdno(idno);
            req.setBirth(birth);
            req.setContractNo(contractNo);
            req.setAssetType(assetType);
            req.setCount(count);
            List<ContractDepositInventoryDto> res = trustClientPort.queryContractDepositInventory(req);
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 存款庫存明細(TMI015) [{}] 失敗: {}", code, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 基金庫存明細(TMI017)
     */
    public List<ContractFundInventoryDto> queryContractFundInventory(String code, String idno, String birth,
            String contractNo, String assetType, String count) {
        try {
            TrustContractRequestBody req = new TrustContractRequestBody();
            req.setCode(code);
            req.setIdno(idno);
            req.setBirth(birth);
            req.setContractNo(contractNo);
            req.setAssetType(assetType);
            req.setCount(count);
            List<ContractFundInventoryDto> res = trustClientPort.queryContractFundInventory(req);
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 基金庫存明細(TMI017) [{}] 失敗: {}", code, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 海外債券庫存明細(TMI018)
     */
    public List<ContractFundInventoryDto> queryContractForeignBondInventory(String code, String idno, String birth,
            String contractNo, String assetType, String count) {
        try {
            TrustContractRequestBody req = new TrustContractRequestBody();
            req.setCode(code);
            req.setIdno(idno);
            req.setBirth(birth);
            req.setContractNo(contractNo);
            req.setAssetType(assetType);
            req.setCount(count);
            List<ContractFundInventoryDto> res = trustClientPort.queryContractForeignBondInventory(req);
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 海外債券庫存明細(TMI018) [{}] 失敗: {}", code, e.getMessage());
            throw e;
        }
    }

    /**
     * 查詢 查詢融資授信明細 (queryCustCreditDetail)
     */
    public EdlsQueryCustCreditDetailResponseBody queryCustCreditDetail(String customerId,
            String closeMk, String businessType, String ifOutputOldClosedLoanAcc, String ifOutputRealBlncOtDeptAcc,
            String operatorCode, String unitCode) {
        try {
            EdlsQueryCustCreditDetailRequestBody req = new EdlsQueryCustCreditDetailRequestBody(customerId, closeMk,
                    businessType, ifOutputOldClosedLoanAcc, ifOutputRealBlncOtDeptAcc);
            return edlsClientPort.queryCustCreditDetail(req, operatorCode, unitCode);
        } catch (Exception e) {
            log.warn("呼叫 查詢融資授信明細 (queryCustCreditDetail) [{}] 失敗: {}", customerId, e.getMessage());
            throw e;
        }
    }
}
