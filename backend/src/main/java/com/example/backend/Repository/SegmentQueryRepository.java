package com.example.backend.Repository;

import com.esb.icrm.management.segment.infrastructure.document.SegmentDocument;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class SegmentQueryRepository {
    /** MongoDB Collection 名稱 */
    private static final String COLL_MANAGEMENT_SEGMENT = "COLL_MANAGEMENT_SEGMENT";

    /** MongoTemplate */
    private final MongoTemplate mongoTemplate;

    public SegmentQueryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 取得資料庫中最新兩筆資料日期 (dataDt 降冪排序)
     *
     * @return 最多 2 筆 dataDt (index 0 為最新代, index 1 為前代)
     */
    public List<String> findLatestTwoDataDt() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("dataDt"),
                Aggregation.sort(Sort.Direction.DESC, "_id"),
                Aggregation.limit(2));

        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "COLL_MANAGEMENT_SEGMENT", Document.class);
        return results.getMappedResults().stream()
                .map(doc -> doc.getString("_id"))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 查詢指定資料日期的所有客群
     *
     * @param dataDt 資料日期 (YYYYMMDD)
     * @return 客群 Document 清單
     */
    public List<SegmentDocument> findByDataDt(String dataDt) {
        Query query = new Query(Criteria.where("dataDt").is(dataDt));
        return mongoTemplate.find(query, SegmentDocument.class);
    }

    /**
     * 查詢指定日期與客群代碼的客群詳情
     *
     * @param segmentCode 客群代碼
     * @param dataDt      資料日期 (YYYYMMDD)
     * @return 客群 Document
     */
    public Optional<SegmentDocument> findByCodeAndDataDt(String segmentCode, String dataDt) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("dataDt").is(dataDt),
                new Criteria().orOperator(
                        Criteria.where("code").is(segmentCode),
                        Criteria.where("segmentCode").is(segmentCode)));
        Query query = new Query(criteria);
        return Optional.ofNullable(mongoTemplate.findOne(query, SegmentDocument.class));
    }
}