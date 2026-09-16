package com.example.backend.requesbody;

// package com.esb.icrm.client.integration.esip.system.trust.dto.request;

/**
 * Contract 共用 Request DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrustContractRequestBody extends TrustCommonRequestBody {

    /**
     * contractNo 信託契約編號
     */
    @JsonProperty("contractNo")
    private String contractNo;

    /**
     * assetType 資產種類
     */
    @JsonProperty("assetType")
    private String assetType;
}
