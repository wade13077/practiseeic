package com.example.backend.service;

// package com.esb.icrm.management.customer.application.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.esb.icrm.client.integration.ecsg.system.edls.dto.request.EdlsQueryEventDataRequestBody;
import com.esb.icrm.client.integration.ecsg.system.edls.dto.response.EdlsQueryEventDataResponseBody;
import com.esb.icrm.client.integration.ecsg.system.edls.port.EdlsClientPort;
import com.esb.icrm.client.integration.esip.system.sacs.dto.response.AttentionCustomerDto;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody;
import com.esb.icrm.code.application.facade.CodeTreeFacade;
import com.esb.icrm.code.definition.GDPRContryCode;
import com.esb.icrm.code.definition.base.CodeNodes;
import com.esb.icrm.code.node.AbstractCodeNode;
import com.esb.icrm.exception.ExternalSystemException;
import com.esb.icrm.platform.security.domain.SecurityUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 個人 360 進入前檢核服務 (停用個資、特殊重要顧客、不宜行銷、應注意名單)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAccessControlService {

    /** 特殊重要顧客身分代碼 (董事長、董事、經理人、私銀會員、私銀關係戶) */
    private static final Set<String> SPECIAL_IMPORTANT_KYC_CODES = Set.of("A1", "A2", "A3", "G1", "G2");

    /** 個人 360 可視權限判斷用之角色 value (對應 RolePermissionMatrix) */
    private static final String ROLE_HEAD_OFFICE_QUERY_USER = "7";
    private static final String ROLE_REGION_QUERY_USER = "8";
    private static final String ROLE_FINANCIAL_ADVISOR_SUPERVISOR = "9";
    private static final String ROLE_FINANCIAL_ADVISOR_STAFF = "10";
    private static final String ROLE_ACCOUNT_OFFICER_SUPERVISOR = "11";
    private static final String ROLE_ACCOUNT_OFFICER_STAFF = "12";
    private static final String ROLE_BACKEND_ADMINISTRATOR = "13";
    private static final String ROLE_SYSTEM_ADMINISTRATOR = "14";
    private static final String ROLE_BRANCH_UNIT_SUPERVISOR = "15";

    /** EDLS 玉山存款數位系統介面 */
    private final EdlsClientPort edlsClientPort;

    /** Code 節點集合 */
    private final CodeNodes codeNodes;

    /** CodeTree 外圍服務 */
    private final CodeTreeFacade codeTreeFacade;

    /**
     * 檢核 1：是否為停止蒐集利用個人資料顧客
     */
    public boolean isStopUseCustomer(Map<String, Object> cipData) {
        return cipData != null && "Y".equals(cipData.get("stopUseCustomerDataFlag"));
    }

    /**
     * 檢核 2：是否為特殊重要顧客 (身分代碼 A1/A2/A3/G1/G2 或 AML 政治敏感人物/PEP 關係人)
     */
    public boolean isSpecialImportantCustomer(Map<String, Object> cipData) {
        if (cipData == null) {
            return false;
        }

        boolean kycMatch = extractKycCodes(cipData.get("customerKycTypeCode")).stream()
                .anyMatch(SPECIAL_IMPORTANT_KYC_CODES::contains);
        boolean amlMatch = "Y".equals(cipData.get("amlPoliticallySensitivePartyFlag"))
                || "Y".equals(cipData.get("amlPepRelatedPartyFlag"));

        return kycMatch || amlMatch;
    }

    /**
     * 檢核 3：不宜行銷分類 (歐盟國家/成年監護/A061身分別/事故註記)
     */
    public List<String> checkUnmarketableTypes(
            Map<String, Object> cipData, String circiKey, String currentUser, String currentBranch) {
        List<String> unmarketableTypes = new ArrayList<>();
        if (cipData == null) {
            return unmarketableTypes;
        }

        // 歐盟國家
        if (isGDPRCountry((String) cipData.get("regCountryCode"))) {
            unmarketableTypes.add("歐盟國家");
        }

        // 成年監護
        if ("Y".equals(cipData.get("grownUpGuardianshipCode"))) {
            unmarketableTypes.add("成年監護");
        }

        // A061
        Object kycObj = cipData.get("customerKycTypeCode");
        if (kycObj instanceof List<?> kycList) {
            Map<String, String> kycNameByForeignCode = buildKycNameByForeignCode();
            kycList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(code -> code.trim().toUpperCase())
                    .map(kycNameByForeignCode::get)
                    .filter(Objects::nonNull)
                    .forEach(unmarketableTypes::add);
        }

        // 事故註記
        if (circiKey != null && !circiKey.isBlank()) {
            try {
                List<EdlsQueryEventDataRequestBody.QueryEventCodeInfo> eventCodes = Arrays
                        .asList("04", "05", "54", "84", "94", "14", "34", "44", "64", "73", "74", "91")
                        .stream().map(EdlsQueryEventDataRequestBody.QueryEventCodeInfo::new).toList();
                EdlsQueryEventDataRequestBody edlsReq = new EdlsQueryEventDataRequestBody(
                        "3", circiKey, null, null, null, null, null, eventCodes);
                EdlsQueryEventDataResponseBody edlsRes = edlsClientPort.queryEventData(edlsReq, currentUser,
                        currentBranch);
                if (edlsRes != null && edlsRes.pageDepositEventList() != null
                        && !edlsRes.pageDepositEventList().isEmpty()) {
                    unmarketableTypes.add("事故註記");
                }
            } catch (Exception e) {
                log.warn("呼叫 EDLS 事故查詢失敗: {}", e.getMessage());
                throw new ExternalSystemException("EDLS", "查詢事故註記失敗", e);
            }
        }

        return unmarketableTypes;
    }

    /**
     * 檢核 4：應注意名單本人清單不為空
     */
    public boolean isAttentionCustomerNotEmpty(List<AttentionCustomerDto> attentionCustomers) {
        return !(attentionCustomers == null || attentionCustomers.isEmpty());
    }

    /**
     * 檢核 5：是否為特殊客群 (年齡>65、國中以下學歷、重大傷病)
     */
    public boolean isSpecialCareCustomer(
            Map<String, Object> cipData,
            WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody wcpData,
            Integer age) {

        // 條件 C：年齡大於 65 歲
        if (age != null && age > 65) {
            return true;
        }

        // 條件 A (WCP)：重大傷病為 "Y"
        if (wcpData != null && "Y".equalsIgnoreCase(wcpData.isIllnessCrd())) {
            return true;
        }

        // 條件 A (WCP/CIP 併集)：學歷判斷
        String wcpEdu = wcpData != null ? wcpData.highEdu() : null;
        if (wcpEdu != null && !wcpEdu.isBlank()) {
            // WCP 判斷是否為學齡前/小學(01) 或 國中(02)
            if ("Education01".equals(wcpEdu) || "Education02".equals(wcpEdu)) {
                return true;
            }
        } else if (cipData != null) {
            // WCP 為空或呼叫失敗，改取 CIP 的 eduLevelCode
            String cipEdu = (String) cipData.get("eduLevelCode");
            if ("6".equals(cipEdu) || "7".equals(cipEdu)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 個人 360 各角色可視權限判斷 (先依使用者可視單位確認範圍，再依顧客主權單位判斷 L1/L2 可視權限)
     */
    public CustomerVisibilityResult evaluateCustomerVisibility(CustomerVisibilityFields fields,
            SecurityUser currentUser) {
        if (currentUser == null || currentUser.getAuthorities() == null) {
            return CustomerVisibilityResult.none();
        }

        Set<String> visibleBranchCodes = resolveVisibleBranchCodes(currentUser);
        String currentEmployeeNo = currentUser.getEmployeeNo();
        Set<String> userRoleValues = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // 若使用個人出身等多角色，採取優先順序最高者判斷 (不取聯集)：
        // 總行查詢人員 > 區域直轄人員 > 分行單位主管 > 營管主管 > AO主管 > 理專主管 > AO經辦
        if (containsAny(userRoleValues, ROLE_HEAD_OFFICE_QUERY_USER, ROLE_BACKEND_ADMINISTRATOR,
                ROLE_SYSTEM_ADMINISTRATOR)) {
            return new CustomerVisibilityResult(true, true, true);
        }

        if (userRoleValues.contains(ROLE_REGION_QUERY_USER)) {
            boolean cond = inSet(visibleBranchCodes, fields.businessBank())
                    || inSet(visibleBranchCodes, fields.wmBranch())
                    || anyInSet(visibleBranchCodes, fields.profitBranch())
                    || anyInSet(visibleBranchCodes, fields.activeDispatchBranchCodes());
            return new CustomerVisibilityResult(cond, cond, true);
        }

        if (userRoleValues.contains(ROLE_BRANCH_UNIT_SUPERVISOR)) {
            boolean cond = inSet(visibleBranchCodes, fields.businessBank())
                    || inSet(visibleBranchCodes, fields.wmBranch())
                    || anyInSet(visibleBranchCodes, fields.profitBranch())
                    || anyInSet(visibleBranchCodes, fields.activeDispatchBranchCodes());
            return new CustomerVisibilityResult(cond, cond, true);
        }

        if (userRoleValues.contains(ROLE_FINANCIAL_ADVISOR_SUPERVISOR)) {
            boolean cond = inSet(visibleBranchCodes, fields.businessBank())
                    || inSet(visibleBranchCodes, fields.wmBranch())
                    || anyInSet(visibleBranchCodes, fields.activeDispatchBranchCodes());
            return new CustomerVisibilityResult(cond, cond, true);
        }

        if (userRoleValues.contains(ROLE_ACCOUNT_OFFICER_SUPERVISOR)) {
            boolean cond = inSet(visibleBranchCodes, fields.businessBank())
                    || anyInSet(visibleBranchCodes, fields.profitBranch())
                    || anyInSet(visibleBranchCodes, fields.activeDispatchBranchCodes())
                    || inSet(visibleBranchCodes, fields.marketingUnit());
            return new CustomerVisibilityResult(cond, cond, true);
        }

        if (userRoleValues.contains(ROLE_FINANCIAL_ADVISOR_STAFF)) {
            boolean l1Cond = inSet(visibleBranchCodes, fields.businessBank())
                    || inSet(visibleBranchCodes, fields.wmBranch())
                    || anyEqualsNonBlank(fields.activeDispatchAssigneeEmpNos(), currentEmployeeNo);
            // 「理財經辦」取 WISE 顧客主管 fin_advsr_cde，而非 MongoDB 顧客主管 faId
            boolean l2Cond = equalsNonBlank(fields.assignedFaEmployeeNo(), currentEmployeeNo)
                    || anyEqualsNonBlank(fields.activeDispatchAssigneeEmpNos(), currentEmployeeNo);
            return new CustomerVisibilityResult(l1Cond, l2Cond, l2Cond);
        }

        if (userRoleValues.contains(ROLE_ACCOUNT_OFFICER_STAFF)) {
            // AO 經辦無理財頁面權限，故 l2Wealth 固定為 false
            boolean cond = inSet(visibleBranchCodes, fields.businessBank())
                    || anyInSet(visibleBranchCodes, fields.profitBranch())
                    || anyEqualsNonBlank(fields.activeDispatchAssigneeEmpNos(), currentEmployeeNo)
                    || inSet(visibleBranchCodes, fields.marketingUnit());
            return new CustomerVisibilityResult(cond, cond, false);
        }

        // 其他角色 (顧客經理/活動設定專案放行等) 與個人 360 可視權限無關，視為無權限
        return CustomerVisibilityResult.none();
    }

    /**
     * 依個人 360 使用的 HR 單位 key 轉為四碼分行代碼
     *
     * @param currentUser 當前登入者
     * @return 個人 360 授權用分行代碼
     */
    private static Set<String> resolveVisibleBranchCodes(SecurityUser currentUser) {
        Set<String> visibleBranchCodes = new HashSet<>();
        List<String> visibleDeptKeys = currentUser.getVisibleDeptKey();
        if (visibleDeptKeys != null) {
            visibleDeptKeys.forEach(deptKey -> addVisibleBranchCode(visibleBranchCodes, deptKey));
        }
        addVisibleBranchCode(visibleBranchCodes, currentUser.getEmployeeDepartmentKey());
        return Set.copyOf(visibleBranchCodes);
    }

    private static void addVisibleBranchCode(Set<String> visibleBranchCodes, String deptKey) {
        if (deptKey != null && deptKey.trim().length() >= 4) {
            visibleBranchCodes.add(deptKey.trim().substring(0, 4));
        }
    }

    /**
     * 顧客身分代碼可能為單一字串或陣列，統一轉為字串清單
     */
    private List<String> extractKycCodes(Object kycObj) {
        if (kycObj instanceof String kycStr) {
            return kycStr.isBlank() ? List.of() : List.of(kycStr.trim().toUpperCase());
        }
        if (kycObj instanceof List<?> kycList) {
            return kycList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(code -> code.trim().toUpperCase())
                    .toList();
        }
        return List.of();
    }

    /**
     * 建立 A061 KYC 外部代碼對照表
     */
    private Map<String, String> buildKycNameByForeignCode() {
        List<AbstractCodeNode> targetNodes = List.of(
                codeNodes.customerKycType().f2(),
                codeNodes.customerKycType().f4(),
                codeNodes.customerKycType().c6(),
                codeNodes.customerKycType().f6());

        Map<String, String> nameByForeignCode = new LinkedHashMap<>();
        targetNodes.stream()
                .filter(AbstractCodeNode::isActive)
                .map(AbstractCodeNode::getCode)
                .flatMap(Optional::stream)
                .forEach(codeObj -> codeObj.foreignMappings().values()
                        .forEach(foreignCode -> nameByForeignCode.putIfAbsent(foreignCode, codeObj.name())));

        return nameByForeignCode;
    }

    /**
     * 判斷指定代碼是否為歐盟國家
     */
    private boolean isGDPRCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        try {
            AbstractCodeNode node = codeTreeFacade.getNodeByCodeAndForeignCode(GDPRContryCode.CODE,
                    countryCode.trim().toUpperCase());
            return AbstractCodeNode.isActive(node);
        } catch (Exception e) {
            log.warn("尋找/查詢 CRM 歐盟國家代碼表失敗: foreignCode={}, 原因: {}", countryCode, e.getMessage());
            return false;
        }
    }

    /**
     * 判斷欄位值是否存在於可視單位集合內 (null/空白一律視為不成立)
     */
    private static boolean inSet(Set<String> visibleUnits, String value) {
        return value != null && !value.isBlank() && visibleUnits.contains(value);
    }

    /**
     * 判斷清單中是否有任一值存在於可視單位集合內 (供現在生效名單分派人員所屬分行等多欄位使用)
     */
    private static boolean anyInSet(Set<String> visibleUnits, List<String> values) {
        return values != null && values.stream().anyMatch(value -> inSet(visibleUnits, value));
    }

    /**
     * 判斷兩欄位是否相等 (null/空白一律視為不成立)，用於「當事者本人」比對
     */
    private static boolean equalsNonBlank(String value, String currentEmployeeNo) {
        return value != null && !value.isBlank() && value.equals(currentEmployeeNo);
    }

    /**
     * 判斷清單中是否有任一值等於當前員編 (供現在生效名單分派人員 (當事者) 等多欄位使用)
     */
    private static boolean anyEqualsNonBlank(List<String> values, String currentEmployeeNo) {
        return values != null && values.stream().anyMatch(value -> equalsNonBlank(value, currentEmployeeNo));
    }

    /**
     * 判斷角色集合是否包含任一指定角色值
     */
    private static boolean containsAny(Set<String> roleValues, String... candidates) {
        for (String candidate : candidates) {
            if (roleValues.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
