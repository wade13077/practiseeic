package com.example.backend.port;

// package com.esb.icrm.management.customer.application.port;

// import ...

/**
 * 提供其他模組 Application 使用的顧客查詢契約
 */
public interface CustomerQueryPort {
    /**
     * 依身分證字號集合查詢名單分派所需的顧客資料
     *
     * @param certNos 身分證字號集合
     * @return 顧客分派資料列表
     */
    List<CustomerAssignmentProfile> findAssignmentProfilesByCertNos(Collection<String> certNos);
}