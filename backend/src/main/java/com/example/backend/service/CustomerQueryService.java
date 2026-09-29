package com.example.backend.service;

// package com.esb.icrm.management.customer.application.query;

import com.esb.icrm.client.integration.esip.system.sacs.dto.response.AttentionCustomerDto;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryCstmrInStockResponseBody;
import com.esb.icrm.client.integration.esip.system.wcp.dto.response.WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody;
import com.esb.icrm.management.customer.application.port.CustomerQueryPort;
import com.esb.icrm.management.customer.application.port.dto.CustomerAssignmentProfile;
import com.esb.icrm.management.customer.interfaces.dto.*;
import com.esb.icrm.management.insight.application.port.InsightQueryPort;
import com.esb.icrm.management.segment.application.port.SegmentQueryPort;
import com.esb.icrm.marketing.campaign.application.port.CampaignQueryPort;
import com.esb.icrm.persistence.edb.customer.repository.CustomerEdbRepository;
import com.esb.icrm.persistence.mongodb.customer.document.CustomerDocument;
import com.esb.icrm.persistence.mongodb.customer.querylog.document.CustomerQueryLogDocument;
import com.esb.icrm.persistence.mongodb.customer.querylog.repository.CustomerQueryLogDocumentRepository;
import com.esb.icrm.persistence.mongodb.customer.repository.CustomerDocumentRepository;
import com.esb.icrm.persistence.mongodb.dispatch.document.DispatchDocument;
import com.esb.icrm.persistence.mongodb.dispatch.repository.DispatchDocumentRepository;
import com.esb.icrm.security.domain.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 個人 360 查詢服務
 */
@Slf4j
@Service
public class CustomerQueryService implements CustomerQueryPort {

    /**
     * 外部系統整合服務 (SEAL, CIP, WCP, EDLS)
     */
    private final CustomerExternalIntegrationService customerExternalIntegrationService;

    /**
     * 專屬活動查詢 Port
     */
    private final CampaignQueryPort campaignQueryPort;

    /**
     * 經營洞見查詢 Port
     */
    private final InsightQueryPort insightQueryPort;

    /**
     * 客群360 圖表查詢
     */
    private final SegmentQueryPort segmentQueryPort;

    /**
     * CustomerAssembler CustomerDTO 資料組裝
     */
    private final CustomerAssembler customerAssembler;

    /**
     * 顧客查詢 Repository
     */
    private final CustomerDocumentRepository customerDocumentRepository;

    /**
     * 顧客標籤判斷服務 (不宜行銷, 廣資產, 專業投資人等)
     */
    private final CustomerFlaggingsService customerFlaggingsService;

    /**
     * 個人 360 進入前檢核服務 (停用個資, 特殊重要顧客, 不宜行銷, 應注意名單)
     */
    private final CustomerAccessControlService customerAccessControlService;

    /**
     * 資金活水提醒服務 (定存到期、債券/基金到期與獲利、買賣外幣)
     */
    private final CustomerFundFlowAlertService customerFundFlowAlertService;

    /**
     * 即時提醒服務 (停損停利、大額金流、理財會員降等)
     */
    private final CustomerRealTimeAlertService customerRealTimeAlertService;

    /**
     * 會員等級圖表服務
     */
    private final CustomerMemberRankService customerMemberRankService;

    /**
     * 名單分派 Document 查詢 Repository (供現在生效名單分派人員/分行判斷)
     */
    private final DispatchDocumentRepository dispatchDocumentRepository;

    /**
     * 顧客 EDB 查詢 Repository (供 WISE 可視分行檔查詢單據專)
     */
    private final CustomerEdbRepository customerEdbRepository;

    /**
     * 個人 360 顧客查詢紀錄 Repository
     */
    private final CustomerQueryLogDocumentRepository customerQueryLogDocumentRepository;

    /**
     * 建構子注入
     */
    public CustomerQueryService(CustomerExternalIntegrationService customerExternalIntegrationService,
            CampaignQueryPort campaignQueryPort,
            InsightQueryPort insightQueryPort,
            CustomerDocumentRepository customerDocumentRepository,
            CustomerFlaggingsService customerFlaggingsService,
            CustomerAccessControlService customerAccessControlService,
            CustomerFundFlowAlertService customerFundFlowAlertService,
            CustomerRealTimeAlertService customerRealTimeAlertService,
            CustomerMemberRankService customerMemberRankService,
            CustomerAssembler customerAssembler,
            DispatchDocumentRepository dispatchDocumentRepository,
            CustomerEdbRepository customerEdbRepository,
            CustomerQueryLogDocumentRepository customerQueryLogDocumentRepository,
            SegmentQueryPort segmentQueryPort) {
        this.customerExternalIntegrationService = customerExternalIntegrationService;
        this.campaignQueryPort = campaignQueryPort;
        this.insightQueryPort = insightQueryPort;
        this.customerDocumentRepository = customerDocumentRepository;
        this.customerFlaggingsService = customerFlaggingsService;
        this.customerAccessControlService = customerAccessControlService;
        this.customerFundFlowAlertService = customerFundFlowAlertService;
        this.customerRealTimeAlertService = customerRealTimeAlertService;
        this.customerMemberRankService = customerMemberRankService;
        this.customerAssembler = customerAssembler;
        this.dispatchDocumentRepository = dispatchDocumentRepository;
        this.customerEdbRepository = customerEdbRepository;
        this.customerQueryLogDocumentRepository = customerQueryLogDocumentRepository;
        this.segmentQueryPort = segmentQueryPort;
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
                ? null
                : customer.getBasicInformation().getWmBranch();
        var productStructure = customer.getProductStructure();
        var productStructureCustomer = productStructure == null ? null : productStructure.getCustomer();
        return new CustomerAssignmentProfile(customer.getCertNo(), faEmployeeNo, wmBranch,
                productStructureCustomer == null ? null : productStructureCustomer.getTotalAum());
    }

    private CustomerListItemDto toCustomerListItemDto(CustomerDocument customer) {
        String primaryBranch = customer.getBasicInformation() == null
                ? null
                : customer.getBasicInformation().getBusinessBank();
        String customerName = customer.getBasicInformation() == null
                ? null
                : customer.getBasicInformation().getName();
        String birthday = customer.getBirthday() == null
                : customer.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return new CustomerListItemDto(customer.getId(), customer.getCertNo(), primaryBranch, customerName,
                birthday);
    }

    /**
     * 取得個人 360 內部資料
     *
     * @param certNo 顧客 ID
     * @return 個人 360 內部資料
     */
    public CustomerDto getCustomerInternalData(String certNo) {

        // ============ 假資料區塊 ↓ ============
        CustomerDocument baseCustomer = customerDocumentRepository.findById(certNo).orElseGet(() -> {
            CustomerDocument dummy = new CustomerDocument();
            dummy.setCipCustId("12345678"); // 置入假資料的 ID，讓專屬活動抓得到

            CustomerDocument.MemberLevel mockLevel = new CustomerDocument.MemberLevel();

            mockLevel.setMemberRankCode("M");
            mockLevel.setCustomerAum(new BigDecimal("32000000"));
            mockLevel.setSegmentAverageAum(new BigDecimal("32000000"));

            dummy.setMemberLevel(mockLevel);
            return dummy;
        });
        // ============ 假資料區塊 ↑ ============

        // 內部 DB 需要數字型態的 cipCustId
        String cipCustId = baseCustomer.getCipCustId();

        // 假資料對產
        CustomerDto mock = MOCK_CUSTOMER_360_INTERNAL_DATA;

        // 取得客群代碼與經營洞見
        String segmentCode = mock.customerProfile().segment();
        Optional<InsightQueryPort.SegmentInsightDto> segmentInsight = insightQueryPort
                .findLatestBySegmentCode(segmentCode);

        // TODO 確認如何取得 segmentCode
        CustomerDto.SegmentData segmentData = getSegmentData("OTHER_HIGH_GROUP2");

        // 組裝 Profile 區塊
        CustomerDto.CustomerProfile customerProfile = customerAssembler.buildCustomerProfile(mock.customerProfile(),
                segmentCode, segmentInsight);

        // 組裝 Management 區塊 (傳入正確的內部數字 ID)
        List<CampaignQueryPort.ActiveCampaignDto> portCampaigns = campaignQueryPort.findActiveCampaigns(cipCustId);
        CustomerDto.CustomerManagement customerManagement = customerAssembler.buildCustomerManagement(portCampaigns,
                mock.customerManagement(), segmentInsight);

        // 組裝 AUM 區塊
        CustomerDto.AuM auM = customerAssembler.buildCustomerAum(baseCustomer.getAssetOverview(),
                segmentData.aumSegment());

        // 組裝 LUM 區塊
        CustomerDto.LuM luM = customerAssembler.buildCustomerLum(baseCustomer.getLiabilityOverview(),
                segmentData.lumSegment());

        // 組裝 財富潛力 區塊
        CustomerDto.RadarChartViewData wealthPotential = customerAssembler.buildCustomerWealthPotential(
                baseCustomer.getWealthPotential(),
                segmentData.wealthPotential());

        // 組裝 資產類商品意圖 區塊
        CustomerDto.HighlightRadarChartViewData assetProduct = customerAssembler.buildCustomerAssetProduct(
                baseCustomer.getAssetProductIntent(),
                segmentData.productIntentAsset());

        // 組裝 負債與外匯類商品意圖 區塊
        CustomerDto.HighlightRadarChartViewData liabilitiesAndFx = customerAssembler.buildCustomerLiabilitiesAndFx(
                baseCustomer.getLiabilityFxProductIntent(),
                segmentData.productIntentLiability());

        // 組裝 帳戶往來深度 區塊
        CustomerDto.RadarChartViewData accountRelationshipDepth = customerAssembler
                .buildCustomerAccountRelationshipDepth(
                        baseCustomer.getAccountDepth(),
                        segmentData.accountDepth());

        // 組裝 消費行為與場景 區塊
        CustomerDto.RadarTagChartViewData consumptionBehavior = customerAssembler.buildCustomerConsumptionBehavior(
                baseCustomer.getConsumptionBehavior(),
                segmentData.consumeBehavior());

        // 組裝 通路行為 區塊
        CustomerDto.RadarChartViewData channelBehavior = customerAssembler.buildCustomerChannelBehavior(
                baseCustomer.getChannelBehavior(),
                segmentData.channelBehavior());

        // 組裝 數位行為 區塊
        CustomerDto.RadarChartViewData digitalBehavior = customerAssembler.buildCustomerDigitalBehavior(
                baseCustomer.getDigitalBehavior(),
                segmentData.digitalBehavior());

        // 組裝 產品結構 區塊
        CustomerDto.ProductMix productMix = customerAssembler.buildCustomerProductMix(
                baseCustomer.getProductStructure(),
                segmentData.productBehavior());

        CustomerDto.MemberRank dynamicMemberRank = customerMemberRankService.buildMemberRank(baseCustomer);

        return new CustomerDto(
                customerProfile,
                mock.customerProfileForL2Page(),
                customerManagement,
                auM,
                luM,
                dynamicMemberRank,
                wealthPotential,
                productMix,
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
    public RealTimeCustomerDto getCustomerExternalData(String certNo) {
        return queryCustomerExternalData(certNo).realTimeData();
    }

    CustomerExternalQueryResult queryCustomerExternalData(String certNo) {
        // ============ 假資料區塊 ↓ ============
        CustomerDocument baseCustomer = loadCustomerForExternalQuery(certNo);
        // ============ 假資料區塊 ↑ ============

        String currentUser = resolveCurrentUser();
        String currentBranch = resolveCurrentBranch();

        // 2. 從 Domain Model 取出打 API 需要的額外參數
        // 外部 API 側重身分證字號 certNo，內部 DB 需要 cipCustId
        String cipCustId = baseCustomer.getCipCustId();
        String circiKey = baseCustomer.getCirciKey();
        String birthdayStr = baseCustomer.getBirthday() != null
                ? baseCustomer.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : "";

        // 3. 取得印鑑照片 (外部 API 使用 certNo)
        String pictureBase64 = customerExternalIntegrationService.getSealProfilePhoto(certNo, currentUser,
                currentBranch);

        // 4. 取得 CIP 顧客資訊 (外部 API 使用 certNo)
        Map<String, Object> cipData = customerExternalIntegrationService.fetchCipCustomerInfo(certNo, birthdayStr,
                circiKey, currentUser, currentBranch);

        // 5. 組裝顧客標籤 (傳入 cipCustId 供查詢斷定)
        List<RealTimeCustomerDto.CustceFlagging> flaggingList = customerFlaggingsService.buildCustomFlaggings(cipData,
                cipCustId, circiKey, currentUser, currentBranch);

        // 6. 取得 WCP 庫存資訊 (外部 API 使用 certNo)
        WcpQueryCstmrInStockResponseBody wcpRes = customerExternalIntegrationService.fetchWcpCustomerInStock(certNo,
                circiKey, currentUser, currentBranch, resolveCurrentIpAddress());

        // 7. 判斷停利停損與資金活水 (外部 API 使用 certNo)
        String takeProfitOrStopLossAlert = customerRealTimeAlertService.buildTakeProfitOrStopLossAlert(wcpRes);

        // 注意：這裡改為 List<AlertItem>
        List<RealTimeCustomerDto.AlertItem> largeFundInflowAlert = customerRealTimeAlertService
                .buildLargeFundInflowAlert(circiKey);

        // 理財會員降等提醒：memberRank 來源為 CRM顧客profile(baseCustomer)
        String memberRankCode = baseCustomer.getMemberLevel() != null
                ? baseCustomer.getMemberLevel().getMemberRankCode()
                : null;
        String downgradeAlert = customerRealTimeAlertService.buildWealthMemberDowngradeAlert(circiKey, memberRankCode);

        // 注意：這裡改為 List<AlertItem>
        List<RealTimeCustomerDto.AlertItem> fundFlowList = customerFundFlowAlertService.buildFundFlowList(circiKey,
                wcpRes, currentUser, currentBranch);

        // 8. 組裝 KeyAlerts 並回傳
        RealTimeCustomerDto.KeyAlerts keyAlerts = new RealTimeCustomerDto.KeyAlerts(
                downgradeAlert,
                largeFundInflowAlert,
                fundFlowList,
                takeProfitOrStopLossAlert);

        RealTimeCustomerDto realTimeData = new RealTimeCustomerDto(
                new RealTimeCustomerDto.ProfilePhoto(pictureBase64),
                flaggingList,
                keyAlerts);

        return new CustomerExternalQueryResult(baseCustomer, cipData, realTimeData);
    }

    /**
     * 取得外部顧客查詢所需的 MongoDB 顧客資料
     *
     * @param certNo 顧客身分證字號
     * @return 顧客資料
     */
    CustomerDocument loadCustomerForExternalQuery(String certNo) {
        return customerDocumentRepository.findById(certNo).orElseGet(() -> {
            CustomerDocument dummy = new CustomerDocument();
            dummy.setCipCustId("12345678");
            dummy.setCirciKey("A823517790");
            return dummy;
        });
    }

    record CustomerExternalQueryResult(
            CustomerDocument customer,
            Map<String, Object> cipData,
            RealTimeCustomerDto realTimeData) {
    }

    private CustomerDto.SegmentData getSegmentData(String segmentCode) {
        return new CustomerDto.SegmentData(
                segmentQueryPort.findAumBySegmentCode(segmentCode),
                segmentQueryPort.findLumBySegmentCode(segmentCode),
                segmentQueryPort.findWealthPotentialBySegmentCode(segmentCode),
                segmentQueryPort.findProductBehaviorBySegmentCode(segmentCode),
                segmentQueryPort.findProductIntentAssetBySegmentCode(segmentCode),
                segmentQueryPort.findProductIntentLiabilityBySegmentCode(segmentCode),
                segmentQueryPort.findAccountDepthBySegmentCode(segmentCode),
                segmentQueryPort.findConsumeBehaviorBySegmentCode(segmentCode),
                segmentQueryPort.findChannelBehaviorBySegmentCode(segmentCode),
                segmentQueryPort.findDigitalBehaviorBySegmentCode(segmentCode));
    }

    /**
     * 個人 360 統權限檢核，不呼叫外部顧客資料服務
     *
     * @param certNo 顧客身分證字號
     * @return 統權限檢核結果
     */
    public CustomerAuthorizationCheckDto getCustomerAuthorizationCheck(String certNo) {
        CustomerDocument baseCustomer = loadCustomerForAccessCheck(certNo);
        CustomerVisibilityResult visibility = evaluateCustomerVisibility(certNo, baseCustomer);

        return new CustomerAuthorizationCheckDto(
                visibility.l1Visible(),
                visibility.l2NonWealthVisible(),
                visibility.l2WealthVisible());
    }

    /**
     * 個人 360 進入前檢核 (停用個資、特殊重要顧客、不宜行銷、應注意名單)
     *
     * @param certNo 顧客身分證字號
     * @return 進入檢核結果
     */
    public CustomerAccessCheckDto getCustomerAccessCheck(String certNo) {
        CustomerDocument baseCustomer = loadCustomerForAccessCheck(certNo);

        String currentUser = resolveCurrentUser();
        String currentBranch = resolveCurrentBranch();
        String currentIp = resolveCurrentIpAddress();
        String circiKey = baseCustomer.getCirciKey();
        String birthdayStr = baseCustomer.getBirthday() != null
                ? baseCustomer.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : "";

        Map<String, Object> cipData = customerExternalIntegrationService.fetchCipCustomerInfo(certNo, birthdayStr,
                circiKey, currentUser, currentBranch);
        List<AttentionCustomerDto> attentionCustomers = customerExternalIntegrationService
                .fetchSacsAttentionCustomer(certNo, currentUser, currentBranch);

        WcpQueryWiseCstmrInfoFromDgtlChnlResponseBody wcpWiseData = customerExternalIntegrationService
                .fetchWcpWiseCstmrInfo(certNo, currentUser, currentBranch, currentIp);

        cipData = new java.util.HashMap<>(cipData);
        cipData.put("customerKyctypeCode", List.of("A1"));

        boolean stopUseCustomerData = customerAccessControlService.isStopUseCustomer(cipData);
        boolean specialImportantCustomer = customerAccessControlService.isSpecialImportantCustomer(cipData);
        List<String> unmarketableCategories = customerAccessControlService.checkUnmarketableTypes(cipData, circiKey,
                currentUser, currentBranch);
        boolean attentionOrUnmarketable = customerAccessControlService.isAttentionCustomer(attentionCustomers)
                || !unmarketableCategories.isEmpty();

        Integer age = null;
        if (baseCustomer.getBasicInformation() != null) {
            age = baseCustomer.getBasicInformation().getAge();
        }

        boolean specialCareCustomer = customerAccessControlService.isSpecialCareCustomer(cipData, wcpWiseData, age);

        CustomerVisibilityResult visibility = evaluateCustomerVisibility(certNo, baseCustomer);

        return new CustomerAccessCheckDto(
                stopUseCustomerData,
                specialImportantCustomer,
                attentionOrUnmarketable,
                unmarketableCategories,
                specialCareCustomer,
                visibility.l1Visible(),
                visibility.l2NonWealthVisible(),
                visibility.l2WealthVisible());
    }

    /**
     * 紀錄個人 360 L1 顧客查詢
     *
     * @param certNo      顧客識別值
     * @param queryReason 特殊重要顧客查詢原因代碼，一般顧客不需提供
     */
    public void recordCustomerQuery(String certNo, Integer queryReason) {
        CustomerAccessCheckDto accessCheck = getCustomerAccessCheck(certNo);
        if (!accessCheck.hasCustomerViewPermission()) {
            throw new AccessDeniedException("無此顧客查詢權限");
        }

        boolean isSpecial = accessCheck.specialImportantCustomer();
        CustomerQueryReasonCode reason = isSpecial
                ? CustomerQueryReasonCode.fromCode(queryReason)
                : null;
        SecurityUser currentUser = resolveCurrentSecurityUser();
        if (currentUser == null || currentUser.getEmployeeNo() == null || currentUser.getEmployeeNo().isBlank()) {
            throw new AccessDeniedException("查詢人員資訊不存在");
        }

        customerQueryLogDocumentRepository.save(new CustomerQueryLogDocument(
                certNo,
                currentUser.getEmployeeNo(),
                currentUser.getEmployeeDepartmentKey(),
                Instant.now(),
                reason == null ? null : reason.code(),
                reason == null ? null : reason.description(),
                isSpecial));
    }

    private CustomerDocument loadCustomerForAccessCheck(String certNo) {
        return customerDocumentRepository.findById(certNo).orElseGet(() -> {
            CustomerDocument dummy = new CustomerDocument();
            dummy.setCipCustId("12345678");
            dummy.setCirciKey("A823517790");

            CustomerDocument.BasicInformation basic = new CustomerDocument.BasicInformation();
            basic.setAge(35);
            dummy.setBasicInformation(basic);
            return dummy;
        });
    }

    private CustomerVisibilityResult evaluateCustomerVisibility(String certNo, CustomerDocument baseCustomer) {
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
        String assignedFaEmployeeNo = customerEdbRepository.findAssignedFaEmployeeNoByCustomerNo(circiKey).orElse(null);

        CustomerVisibilityFields visibilityFields = new CustomerVisibilityFields(
                basicInformation != null ? basicInformation.getBusinessBank() : null,
                basicInformation != null ? basicInformation.getWmBranch() : null,
                basicInformation != null ? basicInformation.getProfitBranch() : null,
                basicInformation != null ? basicInformation.getMarketingUnit() : null,
                assignedFaEmployeeNo,
                activeDispatchBranchCodes,
                activeDispatchAssigneeEmpNos);
        return customerAccessControlService.evaluateCustomerVisibility(visibilityFields, resolveCurrentSecurityUser());
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
     * 獲取當前登入人員員編
     */
    String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
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

    /**
     * 個人 360 內部假資料
     */
    private static final CustomerDto MOCK_CUSTOMER_360_INTERNAL_DATA = new CustomerDto(
            new CustomerDto.CustomerProfile(
                    "王大明",
                    "35",
                    "SEG-002",
                    "0598 民生分行",
                    "陳小玉 #13456"),
            new CustomerDto.CustomerProfileForL2Page(
                    "A123456789",
                    "",
                    "",
                    "",
                    "1991-10-12"),
            new CustomerDto.CustomerManagement(
                    List.of(new CustomerDto.CampaignDetail("E起STAR", "2026Q2",
                            "本次金融產品行銷活動主打穩健收益與彈性配置，透過數位平台與專屬顧問服務，提供多元投資選擇。活動期間新開戶享手續費最低3折優惠。",
                            "本活動優先鎖定具備豐富資金、定存到期或近期投資互動訊號的顧客，依據顧客風險屬性與資產結構推薦合適商品組合。理專可搭配顧客目前AuM",
                            "數位投放將同步支援APP推播、EDM與網銀訊息版位，顧客點擊或回應後可回寫互動紀錄，協助理專掌握最新意向。活動結束後將彙整開啟率、點擊率與轉換業績",
                            "有", List.of(
                                    "附件一_2026 E起STAR第一季行銷活動說明.pdf",
                                    "附件二_產品地圖.pdf"))),
                    List.of("信貸", "理財型房貸", "債券型基金"),
                    "此客群於本行LUM遠大於AUM，需要以貸拉存，吸引新錢提升AUM，可透過存款專案、結構型產品吸引資金匯入，顧客對於負債觀念較能接受，故可挑選名下合適資產辦理貸款服務。"),
            new CustomerDto.AuM(
                    new CustomerDto.AuMData(
                            new CustomerDto.CategoryData(List.of("36%", "16.6%", "14%"),
                                    List.of(2880000, 1500000, 1120000)),
                            new CustomerDto.CategoryData(List.of("15%", "16.6%", "11%"),
                                    List.of(1200000, 1500000, 880000)),
                            new CustomerDto.CategoryData(List.of("11%", "16.6%", "13%"),
                                    List.of(880000, 1500000, 1040000)),
                            new CustomerDto.CategoryData(List.of("13%", "16.6%", "11%"),
                                    List.of(1040000, 1500000, 880000)),
                            new CustomerDto.CategoryData(List.of("11%", "16.6%", "15%"),
                                    List.of(880000, 1500000, 1200000)),
                            new CustomerDto.CategoryData(List.of("14%", "16.6%", "11%"),
                                    List.of(1120000, 1500000, 880000))),
                    8000000,
                    9000000,
                    6000000),
            new CustomerDto.LuM(
                    new CustomerDto.LuMData(
                            new CustomerDto.CategoryData(List.of("34%", "16.6%", "100%"),
                                    List.of(11940000, 2000000, 5000000)),
                            new CustomerDto.CategoryData(List.of("26%", "16.6%", "0%"), List.of(9130000, 2000000, 0)),
                            new CustomerDto.CategoryData(List.of("10%", "16.6%", "0%"), List.of(3510000, 2000000, 0)),
                            new CustomerDto.CategoryData(List.of("14%", "16.6%", "0%"), List.of(4920000, 2000000, 0)),
                            new CustomerDto.CategoryData(List.of("8%", "16.6%", "0%"), List.of(2810000, 2000000, 0)),
                            new CustomerDto.CategoryData(List.of("8%", "0%", "0%"), List.of(2810000, 0, 0))),
                    35130000,
                    10000000,
                    5000000),
            new CustomerDto.MemberRank(
                    "新苗",
                    "25%",
                    "30%",
                    new CustomerDto.UpgradeOrDownGradeAlert(
                            "提升AuM %s 可升級精英會員",
                            "150000",
                            "升級精英會員可享有以下權益",
                            List.of("特定交易減免手續費次數 每月4次", "機場接送 每年4次")),
                    "1000000",
                    "700000"),
            new CustomerDto.RadarChartViewData(
                    List.of("年收入", "綜所稅", "信用卡額度", "年度刷卡金額", "不動產估值", "股利"),
                    List.of(
                            new CustomerDto.RadarChartData(1, "12351元"),
                            new CustomerDto.RadarChartData(2, "12352元"),
                            new CustomerDto.RadarChartData(1, "12353元"),
                            new CustomerDto.RadarChartData(3, "12354元"),
                            new CustomerDto.RadarChartData(1, "雙北:50000元、非雙北:60000元"),
                            new CustomerDto.RadarChartData(1, "12356元")),
                    List.of(
                            new CustomerDto.RadarChartData(2, "12355元"),
                            new CustomerDto.RadarChartData(2, "26355元"),
                            new CustomerDto.RadarChartData(1, "32755元"),
                            new CustomerDto.RadarChartData(3, "42955元"),
                            new CustomerDto.RadarChartData(1, "52355元"),
                            new CustomerDto.RadarChartData(2, "62355元"))),
            new CustomerDto.ProductMix(
                    "1%",
                    new CustomerDto.ProductMixData(
                            1200,
                            List.of(20, 30)),
                    new CustomerDto.ProductMixData(
                            920,
                            List.of(66, 15))),
            new CustomerDto.HighlightRadarChartViewData(
                    List.of("年收入", "綜所稅", "信用卡額度", "年度刷卡金額", "不動產估值", "股利"),
                    List.of(
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(2.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(3.0, "30%", true),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(2.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(3.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false)),
                    List.of(
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false))),
            new CustomerDto.HighlightRadarChartViewData(
                    List.of("外匯(美元)", "外匯(日圓)", "信貸", "房貸週轉金", "SB", "信用卡帳單分期", "信用卡消費分期"),
                    List.of(
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(2.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(3.0, "30%", true),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(2.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(3.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false)),
                    List.of(
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false),
                            new CustomerDto.HighlightRadarChartData(1.0, "10%", false))),
            new CustomerDto.RadarChartViewData(
                    List.of("帳戶淨流入金額", "房貸自扣與還款", "證券自扣與交易", "公共事業費自扣", "信貸自扣與還款"),
                    List.of(
                            new CustomerDto.RadarChartData(1, "12351元"),
                            new CustomerDto.RadarChartData(1, "雙北:50000元、非雙北:60000元"),
                            new CustomerDto.RadarChartData(2, "52%"),
                            new CustomerDto.RadarChartData(2, "12353次"),
                            new CustomerDto.RadarChartData(3, "30%")),
                    List.of(
                            new CustomerDto.RadarChartData(1, "99元"),
                            new CustomerDto.RadarChartData(3, "雙北:10000元、非雙北:80000元"),
                            new CustomerDto.RadarChartData(1, "12%"),
                            new CustomerDto.RadarChartData(2, "30%"),
                            new CustomerDto.RadarChartData(3, "15%"))),
            new CustomerDto.RadarTagChartViewData(
                    List.of("生活雜費", "美食餐飲", "網購消費", "3C電信", "保險投資", "APP及小額支付", "休閒旅遊", "百貨購物", "其他"),
                    List.of(
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%")),
                    List.of(
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%"),
                            new CustomerDto.RadarChartData(2, "金額:123元 佔比:10%")),
                    List.of("高端精品消費族", "3C科技族", "日韓消費族", "數位購物行為", "寵物偏好")),
            new CustomerDto.RadarChartViewData(
                    List.of("分行月交易", "ATM月交易", "行銀月交易", "網銀月交易", "玉山Wallet月交易", "客服往來撥打", "Line回應率"),
                    List.of(
                            new CustomerDto.RadarChartData(2, "31次"),
                            new CustomerDto.RadarChartData(2, "32次"),
                            new CustomerDto.RadarChartData(1, "33次"),
                            new CustomerDto.RadarChartData(2, "34次"),
                            new CustomerDto.RadarChartData(1, "35次"),
                            new CustomerDto.RadarChartData(3, "36次"),
                            new CustomerDto.RadarChartData(1, "37%"),
                            new CustomerDto.RadarChartData(1, "38%")),
                    List.of(
                            new CustomerDto.RadarChartData(2, "37次"),
                            new CustomerDto.RadarChartData(1, "36次"),
                            new CustomerDto.RadarChartData(1, "35次"),
                            new CustomerDto.RadarChartData(3, "34次"),
                            new CustomerDto.RadarChartData(2, "33次"),
                            new CustomerDto.RadarChartData(1, "32次"),
                            new CustomerDto.RadarChartData(2, "31%"))),
            new CustomerDto.RadarChartViewData(
                    List.of("數位活動回應率", "線上登入次數", "線上交易次數", "數位收入"),
                    List.of(
                            new CustomerDto.RadarChartData(1, "10%"),
                            new CustomerDto.RadarChartData(2, "3666次"),
                            new CustomerDto.RadarChartData(3, "15644次"),
                            new CustomerDto.RadarChartData(2, "9000元")),
                    List.of(
                            new CustomerDto.RadarChartData(1, "10%"),
                            new CustomerDto.RadarChartData(2, "3666次"),
                            new CustomerDto.RadarChartData(3, "15644次"),
                            new CustomerDto.RadarChartData(2, "9000元"))));
}