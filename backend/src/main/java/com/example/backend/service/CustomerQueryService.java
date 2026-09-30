package com.example.backend.service;

// package com.esb.icrm.management.customer.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.esb.icrm.client.integration.esip.system.sacs.dto.response.AttentionCustomerDto;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryCstmrInStockResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody;
import com.esb.icrm.exception.DomainException;
import com.esb.icrm.exception.ExternalSystemException;
import com.esb.icrm.management.customer.application.exception.CustomerAccessCheckException;
import com.esb.icrm.management.customer.application.port.CustomerQueryPort;
import com.esb.icrm.management.customer.application.port.dto.CustomerAssignmentProfile;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAccessCheckDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerAuthorizationCheckDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerDataSectionEnum;
import com.esb.icrm.management.customer.interfaces.dto.CustomerDto;
import com.esb.icrm.management.customer.interfaces.dto.CustomerListItemDto;
import com.esb.icrm.management.customer.interfaces.dto.RealTimeCustomerDto;
import com.esb.icrm.management.customer.interfaces.dto.SearchCustomerRequest;
import com.esb.icrm.management.customer.interfaces.dto.SecureCustomerDto;
import com.esb.icrm.management.insight.application.port.InsightQueryPort;
import com.esb.icrm.management.segment.application.port.SegmentQueryPort;
import com.esb.icrm.marketing.campaign.application.port.CampaignQueryPort;
import com.esb.icrm.organization.application.port.DepartmentQueryPort;
import com.esb.icrm.organization.application.port.EmployeeQueryPort;
import com.esb.icrm.organization.dto.DepartmentDto;
import com.esb.icrm.persistence.edb.customer.port.CustomerEdbQueryPort;
import com.esb.icrm.persistence.mongodb.customer.document.CustomerDocument;
import com.esb.icrm.persistence.mongodb.customer.querylog.document.CustomerQueryLogDocument;
import com.esb.icrm.persistence.mongodb.customer.querylog.repository.CustomerQueryLogDocumentRepository;
import com.esb.icrm.persistence.mongodb.customer.repository.CustomerDocumentRepository;
import com.esb.icrm.persistence.mongodb.dispatch.document.DispatchDocument;
import com.esb.icrm.persistence.mongodb.dispatch.repository.DispatchDocumentRepository;
import com.esb.icrm.platform.security.domain.SecurityUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 個人 360 查詢服務
 */
@Slf4j
@Service
public class CustomerQueryService implements CustomerQueryPort {

        /** 預設男性頭像路徑 */
        private static final String DEFAULT_BOY_IMAGE = "/images/customer/defaultBoy.png";

        /** 預設女性頭像路徑 */
        private static final String DEFAULT_GIRL_IMAGE = "/images/customer/defaultGirl.png";

        /** 外部系統整合服務 (SEAL, CIP, WCP, EDLS) */
        private final CustomerExternalIntegrationService customerExternalIntegrationService;

        /** 專題活動查詢 Port */
        private final CampaignQueryPort campaignQueryPort;

        /** 經營洞見查詢 Port */
        private final InsightQueryPort insightQueryPort;

        /** 客群 360 面板查詢 */
        private final SegmentQueryPort segmentQueryPort;

        /** 單位階層查詢 Port */
        private final DepartmentQueryPort departmentQueryPort;

        /** 員工查詢 Port */
        private final EmployeeQueryPort employeeQueryPort;

        /** CustomerAssembler CustomerDTO 資料組裝 */
        private final CustomerAssembler customerAssembler;

        /** 顧客查詢 Repository */
        private final CustomerDocumentRepository customerDocumentRepository;

        /** 顧客標籤判斷服務 (不宜行銷、高資產、專業投資人等) */
        private final CustomerFlaggingService customerFlaggingService;

        /** 個人 360 進入前檢核服務 (停用個資、特殊重要顧客、不宜行銷、應注意名單) */
        private final CustomerAccessControlService customerAccessControlService;

        /** 資金流水提醒服務 (定存到期、債券/基金到期與獲利、買賣外幣) */
        private final CustomerFundFlowAlertService customerFundFlowAlertService;

        /** 即時提醒服務 (停損停利、大額金流、理財會員降等) */
        private final CustomerRealTimeAlertService customerRealTimeAlertService;

        /** 會員等級圖表服務 */
        private final CustomerMemberRankService customerMemberRankService;

        /** 名單分派 Document Repository (供現在生效名單分派人員/分行判斷) */
        private final DispatchDocumentRepository dispatchDocumentRepository;

        /** 顧客 EDB 查詢 Repository (供 WISE 可視分行查詢辦理專專) */
        private final CustomerEdbQueryPort customerEdbPort;

        /** 個人 360 顧客查詢紀錄 Repository */
        private final CustomerQueryLogDocumentRepository customerQueryLogDocumentRepository;

        /**
         * 建構子注入
         */
        public CustomerQueryService(CustomerExternalIntegrationService customerExternalIntegrationService,
                        CampaignQueryPort campaignQueryPort,
                        InsightQueryPort insightQueryPort,
                        CustomerDocumentRepository customerDocumentRepository,
                        CustomerFlaggingService customerFlaggingService,
                        CustomerAccessControlService customerAccessControlService,
                        CustomerFundFlowAlertService customerFundFlowAlertService,
                        CustomerRealTimeAlertService customerRealTimeAlertService,
                        CustomerMemberRankService customerMemberRankService,
                        CustomerAssembler customerAssembler,
                        DispatchDocumentRepository dispatchDocumentRepository,
                        CustomerEdbQueryPort customerEdbPort,
                        CustomerQueryLogDocumentRepository customerQueryLogDocumentRepository,
                        SegmentQueryPort segmentQueryPort,
                        DepartmentQueryPort departmentQueryPort,
                        EmployeeQueryPort employeeQueryPort) {
                this.customerExternalIntegrationService = customerExternalIntegrationService;
                this.campaignQueryPort = campaignQueryPort;
                this.insightQueryPort = insightQueryPort;
                this.customerDocumentRepository = customerDocumentRepository;
                this.customerFlaggingService = customerFlaggingService;
                this.customerAccessControlService = customerAccessControlService;
                this.customerFundFlowAlertService = customerFundFlowAlertService;
                this.customerRealTimeAlertService = customerRealTimeAlertService;
                this.customerMemberRankService = customerMemberRankService;
                this.customerAssembler = customerAssembler;
                this.dispatchDocumentRepository = dispatchDocumentRepository;
                this.customerEdbPort = customerEdbPort;
                this.customerQueryLogDocumentRepository = customerQueryLogDocumentRepository;
                this.segmentQueryPort = segmentQueryPort;
                this.departmentQueryPort = departmentQueryPort;
                this.employeeQueryPort = employeeQueryPort;
        }

        /**
         * 依搜尋條件查詢顧客列表
         *
         * @param request 搜尋條件
         * @return 分頁結果
         */
        public Page<CustomerListItemDto> searchCustomers(SearchCustomerRequest request) {
                int page = request.page() == null ? 0 : request.page();
                int size = request.size() == null ? 10 : request.size();
                return customerDocumentRepository.search(request.identityNo(), request.name(), request.birthdayFrom(),
                                request.birthdayTo(), page, size).map(this::toCustomerListItemDto);
        }

        /**
         * 依 Secure API 外部條件取得顧客
         *
         * @param certNo   身分證字號
         * @param circiKey 歸戶鍵值
         * @param birthday 生日
         * @return return Secure API 顧客資料
         */
        public List<SecureCustomerDto> findSecureCustomers(String certNo, String circiKey, String birthday,
                        String customerName) {
                return customerDocumentRepository.find(certNo, null, circiKey, customerName,
                                parseBirthday(birthday)).stream()
                                .map(this::toSecureCustomerDto)
                                .toList();
        }

        /**
         * 依 token 內的 MongoDB 文件 ID 取得顧客
         *
         * @param serialNos MongoDB 文件 ID 清單
         * @return Secure API 顧客資料
         */
        public List<SecureCustomerDto> findSecureCustomersByIds(List<String> serialNos) {
                return customerDocumentRepository.findByIds(serialNos).stream()
                                .map(this::toSecureCustomerDto)
                                .toList();
        }

        private LocalDate parseBirthday(String birthday) {
                return StringUtils.hasText(birthday) ? LocalDate.parse(birthday.trim()) : null;
        }

        private SecureCustomerDto toSecureCustomerDto(CustomerDocument customer) {
                CustomerDocument.BasicInformation basicInformation = customer.getBasicInformation();
                String primaryBranch = resolveTopDepartmentName(basicInformation);
                String customerName = basicInformation == null ? null : basicInformation.getName();
                String birthday = customer.getBirthday() == null ? null
                                : customer.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE);
                return new SecureCustomerDto(customer.getId(), customer.getCipCustId(), primaryBranch, customerName,
                                birthday);
        }

        private String firstText(String... values) {
                for (String value : values) {
                        if (StringUtils.hasText(value)) {
                                return value;
                        }
                }
                return null;
        }

        private String resolveSegmentCode(CustomerDocument.BasicInformation basicInformation) {
                if (basicInformation == null
                                || !StringUtils.hasText(basicInformation.getCustSegType())
                                || !StringUtils.hasText(basicInformation.getM1CustGrp())) {
                        return null;
                }
                return basicInformation.getCustSegType() + "_" + basicInformation.getM1CustGrp();
        }

        private String resolvePrimaryBranch(CustomerDocument.BasicInformation basicInformation) {
                if (basicInformation == null || !StringUtils.hasText(basicInformation.getBusinessBank())) {
                        return returnFirstText(
                                        basicInformation == null ? null : basicInformation.getBusinessBankDisplay(),
                                        basicInformation == null ? null : basicInformation.getBusinessBankName());
                }

                String branchCode = basicInformation.getBusinessBank().trim();
                DepartmentDto department = departmentQueryPort.findTopDepartmentByDeptCode(branchCode).orElse(null);
                if (department == null) {
                        return returnFirstText(basicInformation.getBusinessBankDisplay(),
                                        basicInformation.getBusinessBankName(),
                                        branchCode);
                }
                return firstText(department.deptCode(), branchCode) + " "
                                + firstText(department.deptName(), basicInformation.getBusinessBankName());
        }

        private String resolveFinancialConsultant(CustomerDocument.BasicInformation basicInformation) {
                if (basicInformation == null || !StringUtils.hasText(basicInformation.getFaId())) {
                        return returnBasicInformation == null ? null : firstText(basicInformation.getFaName());
                }
                return employeeQueryPort.findExtensionByEmpNo(basicInformation.getFaId().trim())
                                .map(employee -> firstText(employee.displayName(), basicInformation.getFaName(),
                                                basicInformation.getFaId()))
                                .orElseGet(() -> firstText(basicInformation.getFaName(), basicInformation.getFaId()));
        }

        private String resolveTopDepartmentName(CustomerDocument.BasicInformation basicInformation) {
                if (basicInformation == null) {
                        return null;
                }
                String deptCode = basicInformation.getBusinessBank();
                DepartmentDto topDepartment = departmentQueryPort.findTopDepartmentByDeptCode(deptCode).orElse(null);
                return topDepartment == null ? null : firstText(topDepartment.fullName());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<CustomerAssignmentProfile> findAssignmentProfilesByCertNos(
                        java.util.Collection<String> certNos) {
                return customerDocumentRepository.findByCertNos(certNos).stream()
                                .map(this::toCustomerAssignmentProfile)
                                .toList();
        }

    private CustomerAssignmentProfile toCustomerAssignmentProfile(CustomerDocument customer) {
        String faEmployeeNo = customer.getBasicInformation() == null
                ? null
                : customer.getBasicInformation().getFaId();
        String wmBranch = customer.getBasicInformation() == null
                : customer.getBasicInformation().getWmBranch();
        var productStructure = customer.getProductStructure();
        var productStructureCustomer = productStructure == null ? null : productStructure.getCustomer();
        return new CustomerAssignmentProfile(customer.getCertNo(), faEmployeeNo, wmBranch,
                productStructureCustomer == null ? null : productStructureCustomer.getTotalAum());
    }

        private CustomerListItemDto toCustomerListItemDto(CustomerDocument customer) {
                String primaryBranch = resolveTopDepartmentName(customer.getBasicInformation());
                String customerName = customer.getBasicInformation() == null
                                ? null
                                : customer.getBasicInformation().getName();
                String birthday = customer.getBirthday() == null
                                ? null
                                : customer.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE);
                return new CustomerListItemDto(customer.getId(), customer.getCertNo(), primaryBranch, customerName,
                                birthday);
        }

        /**
         * 取得個人 360 內部資料
         *
         * @param serialNo MongoDB 文件流水號
         * @return 個人 360 內部資料
         */
        public CustomerDto getCustomerInternalData(String serialNo) {
                CustomerDocument baseCustomer = loadCustomer(serialNo);

                // 內部 DB 需要數字型態的 cipCustId
                String cipCustId = baseCustomer.getCipCustId();

                CustomerDocument.BasicInformation basicInformation = baseCustomer.getBasicInformation();
                String segmentCode = resolveSegmentCode(basicInformation);

                // 取得客群代碼與經營洞見
                Optional<InsightQueryPort.SegmentInsightDto> segmentInsight = StringUtils.hasText(segmentCode)
                                ? insightQueryPort.findLatestBySegmentCode(segmentCode)
                                : Optional.empty();

                CustomerDto.SegmentData segmentData = getSegmentData(segmentCode);

                // 組裝 Profile 區塊
                CustomerDto.CustomerProfile customerProfile = customerAssembler.buildCustomerProfile(basicInformation,
                                segmentCode, segmentInsight, resolvePrimaryBranch(basicInformation),
                                resolveFinancialConsultant(basicInformation));

                // 組裝 Management 區塊 (帶入正確的內部數字 ID)
                List<CampaignQueryPort.ActiveCampaignDto> portCampaigns = campaignQueryPort
                                .findActiveCampaigns(cipCustId);
                CustomerDto.CustomerManagement customerManagement = customerAssembler.buildCustomerManagement(
                                portCampaigns,
                                segmentInsight);

                // 組裝 AuM 區塊
                CustomerDto.AuM auM = customerAssembler.buildCustomerAum(baseCustomer.getAssetOverview(),
                                segmentData.aumSegment());

                // 組裝 LuM 區塊
                CustomerDto.LuM luM = customerAssembler.buildCustomerLum(baseCustomer.getLiabilityOverview(),
                                segmentData.lumSegment());

                // 組裝 財富潛力 區塊
                CustomerDto.RadarChartViewData wealthPotential = customerAssembler.buildCustomerWealthPotential(
                                baseCustomer.getWealthPotential(),
                                segmentData.wealthPotential());

                // 組裝 資產偏好產品意圖 區塊
                CustomerDto.HighLightRadarChartViewData assetProduct = customerAssembler.buildCustomerAssetProduct(
                                baseCustomer.getAssetProductIntent(),
                                segmentData.productIntentAsset());

                // 組裝 負債與外匯產品意圖 區塊
                CustomerDto.HighLightRadarChartViewData liabilitiesAndFx = customerAssembler
                                .buildCustomerLiabilitiesAndFx(
                                                baseCustomer.getLiabilityFxProductIntent(),
                                                segmentData.productIntentLiability());

                // 組裝 帳戶往來深度 區塊
                CustomerDto.RadarChartViewData accountRelationshipDepth = customerAssembler
                                .buildCustomerAccountRelationshipDepth(
                                                baseCustomer.getAccountDepth(),
                                                segmentData.accountDepth());

                // 組裝 消費行為與場景 區塊
                CustomerDto.RadarChartDetailViewData consumptionBehavior = customerAssembler
                                .buildCustomerConsumptionBehavior(
                                                baseCustomer.getConsumptionBehavior(),
                                                segmentData.consumeBehavior());

                // 組裝 通路行為 區塊
                CustomerDto.RadarChartDetailViewData channelBehavior = customerAssembler.buildCustomerChannelBehavior(
                                baseCustomer.getChannelBehavior(),
                                segmentData.channelBehavior());

                // 組裝 數位行為 區塊
                CustomerDto.RadarChartDetailViewData digitalBehavior = customerAssembler.buildCustomerDigitalBehavior(
                                baseCustomer.getDigitalBehavior(),
                                segmentData.digitalBehavior());

                // 組裝 產品結構 區塊
                CustomerDto.ProductMix productMix = customerAssembler.buildCustomerProductMix(
                                baseCustomer.getProductStructure(),
                                segmentData.productBehavior());

                CustomerDto.MemberRank dynamicMemberRank = customerMemberRankService.buildMemberRank(baseCustomer);

                return new CustomerDto(
                                customerProfile,
                                new CustomerDto.CustomerProfileForL2Page(
                                                baseCustomer.getCipCustId(),
                                                baseCustomer.getCertNo(),
                                                baseCustomer.getCreditCardKey(),
                                                baseCustomer.getCirciKey(),
                                                baseCustomer.getBirthday() == null ? null
                                                                : baseCustomer.getBirthday().format(
                                                                                DateTimeFormatter.ISO_LOCAL_DATE)),
                                customerManagement,
                                auM,
                                luM,
                                dynamicMemberRank,
                                wealthPotential,
                                assetProduct,
                                liabilitiesAndFx,
                                accountRelationshipDepth,
                                consumptionBehavior,
                                channelBehavior,
                                digitalBehavior);
        }

        /**
         * 取得個人 360 外部即時資料
         */
        public RealTimeCustomerDto getCustomerExternalData(String serialNo) {
                return queryCustomerExternalData(serialNo).realTimeData();
        }

        /**
         * 查詢顧客頭像，印鑑系統失敗或無圖片時回傳性別預設頭像
         *
         * @param serialNo MongoDB 文件流水號
         * @return 顧客頭像
         */
        public RealTimeCustomerDto.ProfilePhoto getCustomerProfilePhoto(String serialNo) {
                CustomerDocument baseCustomer = loadCustomer(serialNo);
                String certNo = baseCustomer.getCertNo();
                String defaultImage = isFemaleCertNo(certNo) ? DEFAULT_GIRL_IMAGE : DEFAULT_BOY_IMAGE;

                try {
                        String picture = customerExternalIntegrationService.getSealProfilePhoto(
                                        certNo, resolveCurrentUser(), resolveCurrentBranch());
                        return new RealTimeCustomerDto.ProfilePhoto(
                                        StringUtils.hasText(picture) ? picture : defaultImage);
                } catch (ExternalSystemException exception) {
                        log.warn("取得顧客頭像失敗，改用預設頭像", exception);
                        return new RealTimeCustomerDto.ProfilePhoto(defaultImage);
                }
        }

        /**
         * 取得個人 360 外部查詢完整結果
         */
        public CustomerExternalQueryResult queryCustomerExternalData(String serialNo) {
                CustomerDocument baseCustomer = loadCustomer(serialNo);
                String certNo = baseCustomer.getCertNo();

                String currentUser = resolveCurrentUser();
                String currentBranch = resolveCurrentBranch();

                // 2. 從 Domain Model 取出打 API 需要的額外參數
                // 外部 API 需要身分證字號 certNo，內部 DB 需要 cipCustId
                String cipCustId = baseCustomer.getCipCustId();
                String circiKey = baseCustomer.getCirciKey();

                List<String> unavailableSections = new ArrayList<>();
                List<String> identityErrors = new ArrayList<>();

                // 3. 取得 CIP 顧客資訊 (外部 API 使用 certNo)
                Map<String, Object> cipData = Map.of();
                try {
                        cipData = customerExternalIntegrationService.fetchCipCustomerInfo(
                                        certNo,
                                        currentUser,
                                        currentBranch,
                                        baseCustomer.getCipCustId());
                } catch (ExternalSystemException exception) {
                        log.warn("取得顧客身分資料失敗", exception);
                        identityErrors.addAll(List.of("不宜行銷分類", "高資產顧客", "帶區顧客", "專業投資人"));
                }

                if (hasActiveIdentityCode(cipData, "wiseHighEndCustTypeCode")
                                && !hasValidIdentityExpiryDate(cipData, "wiseHighEndCustEndDate")) {
                        identityErrors.add("高資產到期日");
                }

                if (hasActiveIdentityCode(cipData, "wiseExpertInvestorCode")
                                && !hasValidIdentityExpiryDate(cipData, "wiseExpertInvestorEndDate")) {
                        identityErrors.add("專投到期日");
                }

                // 4. 先完成不宜行銷檢核，再將結果導入顧客身分亮燈組裝，避免重複判斷
                List<String> unmarketableTypes = List.of();
                List<RealTimeCustomerDto.CustomFlagging> flaggingList = List.of();
                try {
                        unmarketableTypes = customerAccessControlService.checkUnmarketableTypes(
                                        cipData, circiKey, currentUser, currentBranch);
                } catch (RuntimeException exception) {
                        log.warn("判斷不宜行銷分類失敗", exception);
                        identityErrors.add("不宜行銷分類");
                }

                try {
                        flaggingList = customerFlaggingService.buildCustomFlaggings(
                                        cipData, unmarketableTypes, cipCustId);
                } catch (RuntimeException exception) {
                        log.warn("組裝顧客身分亮燈失敗", exception);
                        identityErrors.addAll(List.of("高資產顧客", "帶區顧客", "專業投資人"));
                }

                // 5. 取得 WCP 庫存資訊 (外部 API 使用 circiKey)
                WcpQueryCstmrInStockResponseBody wcpRes = null;
                try {
                        wcpRes = customerExternalIntegrationService.fetchWcpCustomerInStock(
                                        circiKey, currentUser, currentBranch, resolveCurrentIpAddress());
                } catch (ExternalSystemException exception) {
                        log.warn("取得顧客庫存失敗", exception);
                        unavailableSections.add(CustomerDataSectionEnum.KEY_ALERTS.name());
                }

                // 6. 判斷停利停損與資金流水 (外部 API 使用 certNo)
                String takeProfitOrStopLossAlert = "";
                try {
                        takeProfitOrStopLossAlert = customerRealTimeAlertService.buildTakeProfitOrStopLossAlert(wcpRes);
                } catch (RuntimeException exception) {
                        log.warn("組裝停利停損提醒失敗", exception);
                        addUnavailableSection(unavailableSections, CustomerDataSectionEnum.KEY_ALERTS);
                }

                // 注意：這裡改為 List<AlertItem>
                List<RealTimeCustomerDto.AlertItem> largeFundInflowAlert = List.of();
                try {
                        largeFundInflowAlert = customerRealTimeAlertService.buildLargeFundInflowAlert(circiKey);
                } catch (RuntimeException exception) {
                        log.warn("組裝大額金流提醒失敗", exception);
                        addUnavailableSection(unavailableSections, CustomerDataSectionEnum.KEY_ALERTS);
                }

                // 探討會員降等提醒：memberRank 來源為 CRM顧客profile(baseCustomer)
                String memberRankCode = baseCustomer.getMemberLevel() != null
                                ? baseCustomer.getMemberLevel().getMemberRankCode()
                                : null;
                String downgradeAlert = "";
                try {
                        downgradeAlert = customerRealTimeAlertService.buildWealthMemberDowngradeAlert(circiKey,
                                        memberRankCode);
                } catch (RuntimeException exception) {
                        log.warn("組裝理財會員降等提醒失敗", exception);
                        addUnavailableSection(unavailableSections, CustomerDataSectionEnum.KEY_ALERTS);
                }

                // 注意：這裡改為 List<AlertItem>
                List<RealTimeCustomerDto.AlertItem> fundFlowList = List.of();
                try {
                        fundFlowList = customerFundFlowAlertService.buildFundFlowList(circiKey, wcpRes, currentUser,
                                        currentBranch);
                } catch (RuntimeException exception) {
                        log.warn("組裝資金流水提醒失敗", exception);
                        addUnavailableSection(unavailableSections, CustomerDataSectionEnum.KEY_ALERTS);
                }

                // 7. 組裝 KeyAlerts 並回傳
                RealTimeCustomerDto.KeyAlerts keyAlerts = new RealTimeCustomerDto.KeyAlerts(
                                downgradeAlert,
                                largeFundInflowAlert,
                                fundFlowList,
                                takeProfitOrStopLossAlert);

                RealTimeCustomerDto realTimeData = new RealTimeCustomerDto(
                                new RealTimeCustomerDto.ProfilePhoto(""),
                                flaggingList,
                                keyAlerts);

                String errorMessage = buildCustomerExternalErrorMessage(identityErrors, unavailableSections);
                return new CustomerExternalQueryResult(baseCustomer, cipData, realTimeData,
                                errorMessage, List.copyOf(unavailableSections));
        }

        private boolean isFemaleCertNo(String certNo) {
                return certNo != null && certNo.length() > 1
                                && (certNo.charAt(1) == '2' || certNo.charAt(1) == '9');
        }

        private void addUnavailableSection(List<String> unavailableSections, CustomerDataSectionEnum section) {
                if (!unavailableSections.contains(section.name())) {
                        unavailableSections.add(section.name());
                }
        }

        private boolean hasActiveIdentityCode(Map<String, Object> cipData, String codeKey) {
                Object code = cipData.get(codeKey);
                return code instanceof String codeValue && StringUtils.hasText(codeValue) && !"N".equals(codeValue);
        }

        private boolean hasValidIdentityExpiryDate(Map<String, Object> cipData, String dateKey) {
                Object date = cipData.get(dateKey);
                if (!(date instanceof String dateValue) || dateValue.length() != 8) {
                        return false;
                }
                try {
                        LocalDate.parse(dateValue, DateTimeFormatter.BASIC_ISO_DATE);
                        return true;
                } catch (java.time.format.DateTimeParseException exception) {
                        return false;
                }
        }

        private String buildCustomerExternalErrorMessage(List<String> identityErrors,
                        List<String> unavailableSections) {
                List<String> messages = new ArrayList<>();
                String identityTypes = identityErrors.stream().distinct().reduce((left, right) -> left + "、" + right)
                                .orElse(null);

                if (identityTypes != null && !unavailableSections.isEmpty()) {
                        messages.add("服務連線異常，請留意「" + identityTypes
                                        + "」之顧客身分無法正常判斷，另有部分資料異常，請稍後重試。如持續發生，請聯繫資訊處連線管理部。");
                } else if (identityTypes != null) {
                        messages.add("服務連線異常，請留意「" + identityTypes
                                        + "」之顧客身分無法正常判斷，請稍後重試。如持續發生，請聯繫資訊處連線管理部。");
                } else if (!unavailableSections.isEmpty()) {
                        messages.add("服務連線異常，請稍後重試。如持續發生，請聯繫資訊處連線管理部。");
                }

                return String.join("\n", messages);
        }

        /**
         * 取得外部顧客查詢所需的 MongoDB 顧客資料
         *
         * @param serialNo MongoDB 文件流水號
         * @return 顧客資料
         */
        CustomerDocument loadCustomerForExternalQuery(String serialNo) {
                return loadCustomer(serialNo);
        }

        public record CustomerExternalQueryResult(
                        CustomerDocument customer,
                        Map<String, Object> cipData,
                        RealTimeCustomerDto realTimeData,
                        String errorMessage,
                        List<String> unavailableSections) {

                CustomerExternalQueryResult(CustomerDocument customer, Map<String, Object> cipData,
                                RealTimeCustomerDto realTimeData) {
                        this(customer, cipData, realTimeData, null, List.of());
                }

                public boolean hasErrors() {
                        return !unavailableSections.isEmpty() || errorMessage != null;
                }
        }

        public record CustomerAccessCheckResult(CustomerAccessCheckDto data, String errorMessage) {
        }

        private CustomerDto.SegmentData getSegmentData(String segmentCode) {
                return new CustomerDto.SegmentData(
                                // AuM
                                segmentQueryPort.findAumBySegmentCode(segmentCode),
                                // LuM
                                segmentQueryPort.findLumBySegmentCode(segmentCode),
                                // 財富潛力
                                segmentQueryPort.findWealthPotentialBySegmentCode(segmentCode),
                                // 產品結構
                                segmentQueryPort.findProductBehaviorBySegmentCode(segmentCode),
                                // 資產類產品意圖
                                segmentQueryPort.findProductIntentAssetBySegmentCode(segmentCode),
                                // 負債與現金流
                                segmentQueryPort.findProductIntentLiabilityBySegmentCode(segmentCode),
                                // 帳戶往來深度
                                segmentQueryPort.findAccountDepthBySegmentCode(segmentCode),
                                // 消費行為與場景
                                segmentQueryPort.findConsumeBehaviorBySegmentCode(segmentCode),
                                // 通路行為
                                segmentQueryPort.findChannelBehaviorBySegmentCode(segmentCode),
                                // 數位行為
                                segmentQueryPort.findDigitalBehaviorBySegmentCode(segmentCode));
        }

        /**
         * 個人 360 純權限檢核，不呼叫外部顧客資料服務
         *
         * @param serialNo MongoDB 文件流水號
         * @return 純權限檢核結果
         */
        public CustomerAuthorizationCheckDto getCustomerAuthorizationCheck(String serialNo) {
                try {
                        CustomerDocument baseCustomer = loadCustomerForAccessCheck(serialNo);
                        CustomerVisibilityResult visibility = evaluateCustomerVisibility(baseCustomer);

                        return new CustomerAuthorizationCheckDto(
                                        visibility.l1Visible(),
                                        visibility.l2NonWealthVisible(),
                                        visibility.l2WealthVisible());
                } catch (ExternalSystemException exception) {
                        throw new CustomerAccessCheckException(exception);
                }
        }

        /**
         * 個人 360 進入前檢核 (停用個資、特殊重要顧客、不宜行銷、應注意名單)
         *
         * @param serialNo 顧客身分證字號
         * @return 進入前檢核結果
         */
        public CustomerAccessCheckDto getCustomerAccessCheck(String serialNo) {
                return queryCustomerAccessCheck(serialNo).data();
        }

        public CustomerAccessCheckResult queryCustomerAccessCheck(String serialNo) {
                try {
                        return doGetCustomerAccessCheck(serialNo);
                } catch (ExternalSystemException exception) {
                        throw new CustomerAccessCheckException(exception);
                }
        }

        private CustomerAccessCheckResult doGetCustomerAccessCheck(String serialNo) {
                CustomerDocument baseCustomer = loadCustomerForAccessCheck(serialNo);
                String certNo = baseCustomer.getCertNo();

                String currentUser = resolveCurrentUser();
                String currentBranch = resolveCurrentBranch();
                String currentIp = resolveCurrentIpAddress();
                String circiKey = baseCustomer.getCirciKey();

                Map<String, Object> cipData = customerExternalIntegrationService.fetchCipCustomerInfo(
                                certNo,
                                currentUser,
                                currentBranch,
                                baseCustomer.getCipCustId());

                List<AttentionCustomerDto> attentionCustomers = customerExternalIntegrationService
                                .fetchSacsAttentionCustomer(certNo, currentUser, currentBranch);

                WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody wcpWiseData = customerExternalIntegrationService
                                .fetchWcpWiseCstmrInfo(certNo, currentUser, currentBranch, currentIp);

                cipData = new java.util.HashMap<>(cipData);
                cipData.put("customerKycTypeCode", List.of("A1"));

                boolean stopUseCustomerData = customerAccessControlService.isStopUseCustomer(cipData);
                boolean specialImportantCustomer = customerAccessControlService.isSpecialImportantCustomer(cipData);
                List<String> unmarketableCategories = List.of();
                String identityErrorMessage = null;
                try {
                        unmarketableCategories = customerAccessControlService.checkUnmarketableTypes(
                                        cipData, circiKey, currentUser, currentBranch);
                } catch (ExternalSystemException exception) {
                        log.warn("取得不宜行銷分類失敗", exception);
                        identityErrorMessage = "服務連線異常，請留意「不宜行銷分類」之顧客身分無法正常判斷，請稍後重試。如持續發生，請聯繫資訊處連線管理部。";
                }
                boolean attentionOrUnmarketable = customerAccessControlService
                                .isAttentionCustomerNotEmpty(attentionCustomers) || !unmarketableCategories.isEmpty();

                Integer age = null;
                if (baseCustomer.getBasicInformation() != null) {
                        age = baseCustomer.getBasicInformation().getAge();
                }

                boolean specialCareCustomer = customerAccessControlService.isSpecialCareCustomer(cipData, wcpWiseData,
                                age);

                CustomerAccessCheckDto data = new CustomerAccessCheckDto(
                                stopUseCustomerData,
                                specialImportantCustomer,
                                attentionOrUnmarketable,
                                unmarketableCategories,
                                specialCareCustomer);

                return new CustomerAccessCheckResult(data, identityErrorMessage);
        }

        /**
         * 記錄個人 360 L1 顧客查詢
         *
         * @param serialNo    MongoDB 文件流水號
         * @param queryReason 特殊重要顧客查詢原因代碼，一般顧客無需提供
         */
        public void recordCustomerQuery(String serialNo, Integer queryReason) {
                CustomerAuthorizationCheckDto authorizationCheck = getCustomerAuthorizationCheck(serialNo);
                if (!authorizationCheck.hasCustomerViewPermission()) {
                        throw new AccessDeniedException("無此顧客查詢權限");
                }

                CustomerAccessCheckDto accessCheck = getCustomerAccessCheck(serialNo);
                boolean isSpecial = accessCheck.specialImportantCustomer();
                CustomerQueryReasonCode reason = isSpecial
                                ? CustomerQueryReasonCode.fromCode(queryReason)
                                : null;

                SecurityUser currentUser = resolveCurrentSecurityUser();
                if (currentUser == null || currentUser.getEmployeeNo() == null
                                || currentUser.getEmployeeNo().isBlank()) {
                        throw new AccessDeniedException("查詢人員資訊不存在");
                }

                CustomerDocument customer = loadCustomer(serialNo);
                customerQueryLogDocumentRepository.save(new CustomerQueryLogDocument(
                                customer.getCertNo(),
                                currentUser.getEmployeeNo(),
                                currentUser.getEmployeeDepartmentKey(),
                                Instant.now(),
                                reason == null ? null : reason.code(),
                                reason == null ? null : reason.description(),
                                isSpecial));
        }

        private CustomerDocument loadCustomerForAccessCheck(String certNo) {
                return loadCustomer(certNo);
        }

        private CustomerDocument loadCustomer(String certNo) {
                return customerDocumentRepository.findById(certNo)
                                .orElseThrow(() -> new DomainException("查無顧客資料: " + certNo));
        }

        private CustomerVisibilityResult evaluateCustomerVisibility(CustomerDocument baseCustomer) {
                String certNo = baseCustomer.getCertNo();
                CustomerDocument.BasicInformation basicInformation = baseCustomer.getBasicInformation();
                String circiKey = baseCustomer.getCirciKey();

                List<DispatchDocument> activeDispatches = dispatchDocumentRepository.findActiveByCertNo(certNo,
                                LocalDate.now());

                List<String> activeDispatchBranchCodes = activeDispatches.stream()
                                .map(DispatchDocument::getIcrmBcBranchCode)
                                .filter(StringUtils::hasText)
                                .distinct()
                                .toList();

                List<String> activeDispatchAssigneeEmpNos = activeDispatches.stream()
                                .map(DispatchDocument::getIcrmMarketingEmployeeNo)
                                .filter(StringUtils::hasText)
                                .distinct()
                                .toList();

                String assignedFaEmployeeNo = customerEdbPort.findAssignedFaEmployeeNoByCustomerNo(circiKey)
                                .orElse(null);

                CustomerVisibilityFields visibilityFields = new CustomerVisibilityFields(
                                basicInformation != null ? basicInformation.getBusinessBank() : null,
                                basicInformation != null ? basicInformation.getWmBranch() : null,
                                basicInformation != null ? basicInformation.getProfitBranch() : null,
                                basicInformation != null ? basicInformation.getMarketingUnit() : null,
                                assignedFaEmployeeNo,
                                activeDispatchBranchCodes,
                                activeDispatchAssigneeEmpNos);

                return customerAccessControlService.evaluateCustomerVisibility(visibilityFields,
                                resolveCurrentSecurityUser());
        }

        /**
         * 獲取當前請求來源 IP
         */
        String resolveCurrentIpAddress() {
                if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
                        return "127.0.0.1";
                }

                HttpServletRequest request = attrs.getRequest();
                String ip = request.getRemoteAddr();
                if (ip == null || ip.isBlank()) {
                        return "127.0.0.1";
                }

                return ip.trim();
        }

        /**
         * 獲取當前登入者員編
         */
        String resolveCurrentUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || authentication.getName() == null
                                || authentication.getName().isBlank()) {
                        return "SYS";
                }

                return authentication.getName().trim();
        }

        /**
         * 獲取當前登入者所屬分行/單位代碼
         */
        String resolveCurrentBranch() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
                        return "0000";
                }

                String branch = user.getEmployeeDepartmentKey();
                if (branch == null || branch.isBlank()) {
                        return "0000";
                }

                return branch.trim();
        }

        /**
         * 取得當前登入者的 SecurityUser (供角色與可視分行判斷使用)
         */
        private SecurityUser resolveCurrentSecurityUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
                        return null;
                }
                return user;
        }

        String resolveCurrentBranch() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
                        return "0000";
                }

                String branch = user.getEmployeeDepartmentKey();
                if (branch == null || branch.isBlank()) {
                        return "0000";
                }

                return branch.trim();
        }

        /**
         * 取得當前登入者的 SecurityUser (供角色與可視分行判斷使用)
         */
        private SecurityUser resolveCurrentSecurityUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
                        return null;
                }

                return user;
        }

}