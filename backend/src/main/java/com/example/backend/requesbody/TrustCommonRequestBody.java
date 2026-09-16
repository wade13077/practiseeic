package com.example.backend.requesbody;

// package com.esb.icrm.client.integration.esip.system.trust.dto.request;

/**
 * 台灣信託業務應用系統 共用 Request DTO
 */
@Data
public class TrustCommonRequestBody {

    /**
     * code 序號
     */
    @JsonProperty("code")
    private String code;

    /**
     * idno 身分證號
     */
    @JsonProperty("idno")
    private String idno;

    /**
     * count 出生年月日 (YYYYMMDD)
     */
    @JsonProperty("birth")
    private String birth;

    /**
     * dateIn 本文count (向前補0到長度上限)
     */
    @JsonProperty("count")
    private String count = "0001";
}