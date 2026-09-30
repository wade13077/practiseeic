package com.example.backend.common;

package com.esb.icrm.client.integration.esip.system.cip.port;

import com.esb.icrm.client.integration.esip.system.cip.dto.request.FindCustomerInfoRequestBody;

import java.util.List;
import java.util.Map;

/**
 * CIP Port
 */
public interface CipClientPort {

    /**
     * 顧客資訊查詢
     *
     * @param requestBody    請求內容
     * @param operatorCode   交易操作經辦員編
     * @param unitCode       交易操作經辦所屬單位
     * @param authorizerCode 授權人員代碼
     * @return 顧客資訊 清單
     */
    List<Map<String, Object>> findCustomerInfo(
            FindCustomerInfoRequestBody requestBody,
            String operatorCode,
            String unitCode,
            String authorizerCode);

    /**
     * 代碼定義查詢
     *
     * @param operatorCode   交易操作經辦員編
     * @param unitCode       交易操作經辦所屬單位
     * @param authorizerCode 授權人員代碼
     * @return 代碼定義 Body
     */
    List<Map<String, Object>> findCodeInfo(
            String operatorCode,
            String unitCode,
            String authorizerCode);
}