package com.example.backend.port;

// package com.esb.icrm.client.integration.esip.system.trust.port;

import com.esb.icrm.client.integration.esip.system.trust.dto.request.TrustCommonRequestBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.request.TrustContractRequestBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.ContractDepositInventoryDto;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.ContractFundInventoryDto;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.QueryCustomerDetailsResponseBody;
import com.esb.icrm.client.integration.esip.system.trust.dto.response.TrustContractOverviewDto;

import java.util.List;

/**
 * TSS Port
 */
public interface TrustClientPort {

    /**
     * 信託業務顧客基本資料查詢服務(TMI030)
     *
     * @param requestBody 查詢條件
     * @return 基本資料
     */
    QueryCustomerDetailsResponseBody queryCustomerDetails(
            TrustCommonRequestBody requestBody);

    /**
     * 提供個人信託契約總覽查詢(全部契約)服務(TMI031)
     *
     * @param requestBody 查詢條件
     * @return 契約清單
     */
    List<TrustContractOverviewDto> queryAllTrustContractOverview(
            TrustCommonRequestBody requestBody);

    /**
     * 存款庫存明細(TMI015)
     *
     * @param requestBody 查詢條件
     * @return 存款庫存清單
     */
    List<ContractDepositInventoryDto> queryContractDepositInventory(
            TrustContractRequestBody requestBody);

    /**
     * 基金重存明細(TMI017)
     *
     * @param requestBody 查詢條件
     * @return 基金重存清單
     */
    List<ContractFundInventoryDto> queryContractFundInventory(
            TrustContractRequestBody requestBody);

    /**
     * 海外債券庫存明細(TMI018)
     *
     * @param requestBody 查詢條件
     * @return 海外債券庫存清單
     */
    List<ContractFundInventoryDto> queryContractForeignBondInventory(
            TrustContractRequestBody requestBody);
}