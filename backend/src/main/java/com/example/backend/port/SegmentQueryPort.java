package com.example.backend.port;

// package com.esb.icrm.management.segment.application.port;

// import com.esb.icrm.management.segment.interfaces.dto.SegmentDetailDto;

/**
 * 提供其他模組 Application 使用的客群圖表查詢契約
 */
public interface SegmentQueryPort {

    /**
     * 依客群代碼查詢 AUM 圖表資料
     *
     * @param segmentCode 客群代碼
     * @return AUM 圖表資料
     */
    SegmentDetailDto.RadialSegmentData findAumBySegmentCode(String segmentCode);

    /**
     * 依客群代碼查詢 LuM 圖表資料
     *
     * @param segmentCode 客群代碼
     * @return LuM 圖表資料
     */
    SegmentDetailDto.RadialSegmentData findLumBySegmentCode(String segmentCode);

    /**
     * 依客群代碼查詢財富潛力資料
     *
     * @param segmentCode 客群代碼
     * @return 財富潛力圖表資料
     */
    SegmentDetailDto.Comparison<SegmentDetailDto.WealthRadarSeries> findWealthPotentialBySegmentCode(
            String segmentCode);

    /**
     * 依客群代碼查詢投資產品行為資料
     *
     * @param segmentCode 客群代碼
     * @return 投資產品行為資料
     */
    SegmentDetailDto.ProductBehaviorData findProductBehaviorBySegmentCode(String segmentCode);

    /**
     * 依客群代碼查詢資產類產品意圖資料
     *
     * @param segmentCode 客群代碼
     * @return 資產類產品意圖雷達圖資料
     */
    SegmentDetailDto.Comparison<SegmentDetailDto.ProductIntentAssetSeries> findProductIntentAssetBySegmentCode(
            String segmentCode);

    /**
     * 依客群代碼查詢負債與現金流類產品意圖資料
     *
     * @param segmentCode 客群代碼
     * @return 負債與現金流類產品意圖雷達圖資料
     */
    SegmentDetailDto.Comparison<SegmentDetailDto.ProductIntentLiabilitySeries> findProductIntentLiabilityBySegmentCode(
            String segmentCode);

    /**
     * 依客群代碼查詢帳戶往來深度資料
     *
     * @param segmentCode 客群代碼
     * @return 帳戶往來深度雷達圖資料
     */
    SegmentDetailDto.Comparison<SegmentDetailDto.AccountDepthSeries> findAccountDepthBySegmentCode(String segmentCode);

    /**
     * 依客群代碼查詢消費行為與場景資料
     *
     * @param segmentCode 客群代碼
     * @return 消費行為與場景雷達圖資料
     */
    SegmentDetailDto.ConsumeBehaviorData findConsumeBehaviorBySegmentCode(String segmentCode);

    /**
     * 依客群代碼查詢通路行為資料
     *
     * @param segmentCode 客群代碼
     * @return 通路行為雷達圖資料
     */
    SegmentDetailDto.Comparison<SegmentDetailDto.ChannelBehaviorSeries> findChannelBehaviorBySegmentCode(
            String segmentCode);

    /**
     * 依客群代碼查詢數位行為資料
     *
     * @param segmentCode 客群代碼
     * @return 數位行為雷達圖資料
     */
    SegmentDetailDto.Comparison<SegmentDetailDto.DigitalBehaviorSeries> findDigitalBehaviorBySegmentCode(
            String segmentCode);
}