package com.example.backend.Repository;

// package com.esb.icrm.persistence.edb.customer.repository;

import com.esb.icrm.persistence.edb.customer.dto.CustomerInsurance;
import com.esb.icrm.persistence.edb.customer.dto.CustomerInteractionDto;
import com.esb.icrm.persistence.edb.customer.dto.DailyFxPurchaseDto;
import com.esb.icrm.persistence.edb.customer.port.CustomerEdbPort;
import com.example.backend.Repository.CustomerEdbRepository.DailyInflowDto;
import com.example.backend.Repository.CustomerEdbRepository.MonthlyAumDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 顧客 EDB 查詢 Repository
 *
 * <p>
 * 僅收送 EDB 查詢結果文件，資料轉換由前端負責
 * </p>
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "spring.datasource.dmp", name = "url")
public class CustomerEdbRepository implements CustomerEdbPort {

    /** EDB 查詢工具 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 建構子
     *
     * @param jdbcTemplate EDB 查詢工具
     */
    public CustomerEdbRepository(@Qualifier("dmpJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查詢是否為客訴糾紛名單
     *
     * @param cipCustId CIP 回傳的顧客 ID
     * @return {@code true} 代表為客訴顧客
     */
    public boolean isComplaintCustomer(String cipCustId) {
        if (!StringUtils.hasText(cipCustId)) {
            return false;
        }

        String sql = """
                SELECT COUNT(1)
                FROM ods_tw.tb_epic_grt_gcf_rule_source
                WHERE cip_cust_id = ?
                  AND (argue_ind = 1 OR argue_cc_ind = 1 OR argue_sacs_ind = 1)
                """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, Long.valueOf(cipCustId.trim()));
            return count != null && count > 0;
        } catch (RuntimeException exception) {
            log.warn("[ComplaintCheck] Failed to query EPIC DB for cipCustId: {}, error: {}",
                    cipCustId, exception.getMessage());
            return false;
        }
    }

    /**
     * 查詢單日外匯申購總額
     *
     * @param circiKey     顧客統編
     * @param businessType 業務別
     * @param currencyCode 幣別
     * @param startDate    查詢起日
     * @param endDate      查詢迄日
     * @return 每日加總的購買文件
     */
    public List<DailyFxPurchaseDto> findDailyPurchaseAmounts(String circiKey, String businessType,
            String currencyCode, LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(circiKey)) {
            return List.of();
        }

        String sql = """
                SELECT trans_date, SUM(trade_amount) AS total_trade_amount
                FROM ods_tw.tb_fbs_rpt_temp_ef1007
                WHERE cust_id = ?
                  AND business_type = ?
                  AND currency_code = ?
                  AND trans_date BETWEEN ? AND ?
                GROUP BY trans_date
                ORDER BY trans_date ASC
                """;

        try {
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> new DailyFxPurchaseDto(
                    resultSet.getObject("trans_date", LocalDate.class),
                    resultSet.getBigDecimal("total_trade_amount")), circiKey, businessType, currencyCode, startDate,
                    endDate);
        } catch (RuntimeException exception) {
            log.warn("[FundFlow] Failed to query ods_tw.tb_fbs_rpt_temp_ef1007 for circiKey: {}, error: {}",
                    circiKey, exception.getMessage());
            return List.of();
        }
    }

    /**
     * 查詢近幾個月的月平均 AUM（供理財會員降等判斷）
     */
    public List<MonthlyAumDto> findMonthlyAum(String circiKey, LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(circiKey)) {
            return List.of();
        }

        String sql = "SELECT * FROM oa0139.sp_get_icrm_customer_monthly_aum(?::varchar, ?::date, ?::date)";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                LocalDate snapshotDt = rs.getObject("dmp_snapshot_dt", LocalDate.class);
                BigDecimal aumAvgNew = rs.getBigDecimal("aum_twd_avg_new");
                return new MonthlyAumDto(snapshotDt, aumAvgNew);
            }, circiKey, startDate, endDate);
        } catch (Exception e) {
            log.warn("[AUM Query] 查詢月 AUM 失敗, circiKey: {}, error: {}", circiKey, e.getMessage());
            return List.of();
        }
    }

    /**
     * 查詢大額現金流入（取最新 3 筆）
     */
    public List<DailyInflowDto> findLargeCashInflow(String circiKey, LocalDate startDate, LocalDate endDate,
            BigDecimal threshold) {
        if (!StringUtils.hasText(circiKey)) {
            return List.of();
        }

        String sql = "SELECT * FROM oa0139.sp_get_icrm_customer_large_inflow(?::varchar, ?::date, ?::date, ?::numeric)";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                LocalDate snapshotDt = rs.getObject("dmp_snapshot_dt", LocalDate.class);
                BigDecimal netInflow = rs.getBigDecimal("net_inflow");
                return new DailyInflowDto(snapshotDt, netInflow);
            }, circiKey, startDate, endDate, threshold);
        } catch (Exception e) {
            log.warn("[AUM Query] 查詢大額金流失敗, circiKey: {}, error: {}", circiKey, e.getMessage());
            return List.of();
        }
    }

    public record MonthlyAumDto(LocalDate snapshotDate, BigDecimal aumTwdAvgNew) {
    }

    public record DailyInflowDto(LocalDate snapshotDate, BigDecimal netInflow) {
    }

    /**
     * 查詢 WISE 可視分行代碼（供使用者可視單位定義：分行單位主管、理專主管與 HR 留存單位解復後，比對顧客主經營行、利潤分行、理財分行等分行代碼）
     *
     * @param employeeNo 員編(wk004)
     * @return 分行代碼 (wk001，4 碼) 清單，篩選 wk003='400' (分行單位) 且 dmp_snapshot_dt 為最新快照
     */
    public List<String> findWiseVisibleBranchCodes(String employeeNo) {
        if (!StringUtils.hasText(employeeNo)) {
            return List.of();
        }

        String sql = """
                SELECT DISTINCT wk001
                FROM ods_tw.tb_d_tw_wise_wmm_witwo17a_d
                WHERE wk004 = ?
                  AND wk003 = '400'
                  AND dmp_snapshot_dt = (
                      SELECT MAX(dmp_snapshot_dt)
                      FROM ods_tw.tb_d_tw_wise_wmm_witwo17a_d
                      WHERE wk003 = '400'
                  )
                """;

        try {
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString("wk001"), employeeNo.trim());
        } catch (RuntimeException exception) {
            log.warn("[WiseVisibleBranch] Failed to query tb_tw_wise_wmm_witwo17a_d for employeeNo: {}, error: {}",
                    employeeNo, exception.getMessage());
            return List.of();
        }
    }

    /**
     * 依顧客編號查詢 WISE 顧客主檔中的彈壓理專員編
     *
     * @param circiKey 顧客編號，對應 WISE 顧客主檔 cstmr_no
     * @return 彈壓理專員編 (fin_advsr_cde)
     */
    public Optional<String> findAssignedFaEmployeeNoByCustomerNo(String circiKey) {
        if (!StringUtils.hasText(circiKey)) {
            return Optional.empty();
        }

        String sql = """
                SELECT fin_advsr_cde
                FROM ods_tw.tb_tw_wise_wmm_cstmr
                WHERE cstmr_no = ?
                LIMIT 1
                """;

        try {
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString("fin_advsr_cde"), circiKey.trim())
                    .stream().findFirst();
        } catch (RuntimeException exception) {
            log.warn("[AssignedFA] Failed to query tb_tw_wise_wmm_cstmr for circiKey: {}, error: {}",
                    circiKey, exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 查詢近 6 個月的顧客互動紀錄
     */
    public List<CustomerInteractionDto.IcrmInteractions> findInteractionsByCustomerKeys(
            String cipCustId, String circiKey, String certNo, LocalDate sixMonthsAgo) {

        if (!StringUtils.hasText(cipCustId) && !StringUtils.hasText(circiKey) && !StringUtils.hasText(certNo)) {
            return List.of();
        }

        String sql = """
                SELECT
                    uuid, cip_cust_id, circi_key, credit_card_key, cert_no, birthday, interaction_time, title, category, content, status, create_time
                FROM oa0139.tb_icrm_customer_interaction
                WHERE interaction_date >= ?
                  AND (cip_cust_id = ? OR circi_key = ? OR cert_no = ?)
                ORDER BY interaction_time DESC
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new CustomerInteractionDto.IcrmInteractions(
                    rs.getString("uuid"),
                    rs.getString("cip_cust_id"),
                    rs.getString("circi_key"),
                    rs.getString("credit_card_key"),
                    rs.getString("cert_no"),
                    rs.getString("birthday"),
                    rs.getTimestamp("interaction_time") != null ? rs.getTimestamp("interaction_time").toString() : "",
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("content"),
                    rs.getString("status"),
                    rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toString() : ""),
                    sixMonthsAgo, cipCustId, circiKey, certNo);
        } catch (Exception e) {
            log.error("[Interaction Query] 查詢互動紀錄失敗, error: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 查詢 產品資料檔 ETF/海外股票 對應 產品代碼
     */
    public List<String> findStockProductCode() {
        String sql = """
                SELECT
                    wkw001
                FROM
                    tb_tw_wise_wmt_witwo106_d
                WHERE
                    wkw004 = 'STKF' OR wkw004 = 'ETFF'
                """;

        try {
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString("wkw001"));
        } catch (RuntimeException exception) {
            log.warn(
                    "[WiseVisibleBranch] Failed to query tb_tw_wise_wmt_witwo106_d for productType: STKF or ETFF, error: {}",
                    exception.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 查詢 保單資料
     */
    public List<CustomerInsurance> findInsuranceList(String certNo) {
        String sql = """
                SELECT
                    main.owner_id,
                    detail.insured_id,
                    company.insco_name,
                    plan.plan_name,
                    main.po_status_code,
                    main.po_issue_date,
                    main.coverage_expired_date,
                    main.next_paid_date,
                    main.pay_expired_date,
                    main.cuy_cpo,
                    main.txn_amt_cpo,
                    main.prem_amt_total_cpo,
                    main.modx,
                    detail.mty_amt
                FROM
                    tb_nice_tbias_es_sas_polymstr main
                LEFT JOIN
                    tb_nice_tbias_es_sas_polydetl detail ON main.policy_no = detail.policy_no
                LEFT JOIN
                    tb_nice_tbias_es_sas_parm_insco company ON main.insco_no = company.insco_no
                LEFT JOIN
                    tb_nice_tbias_es_sas_parmprodplan plan ON main.plan_code = plan.plan_code
                WHERE
                    main.owner_id = ?
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new CustomerInsurance(
                    rs.getString("owner_id"),
                    rs.getString("insured_id"),
                    rs.getString("insco_name"),
                    rs.getString("plan_name"),
                    rs.getString("po_status_code"),
                    rs.getString("po_issue_date"),
                    rs.getString("coverage_expired_date"),
                    rs.getString("next_paid_date"),
                    rs.getString("pay_expired_date"),
                    rs.getString("cuy_cpo"),
                    rs.getString("txn_amt_cpo"),
                    rs.getString("prem_amt_total_cpo"),
                    rs.getString("modx"),
                    rs.getString("mty_amt")), certNo);
        } catch (RuntimeException exception) {
            log.warn(
                    "[WiseAssignedFa] Failed to query tb_nice_tbias_es_sas_polymstr for certNo: {}, error: {}",
                    certNo,
                    exception.getMessage());
            return Collections.emptyList();
        }
    }

}
