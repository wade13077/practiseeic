package com.esb.icrm.code.node;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.esb.icrm.code.domain.Code;
import com.esb.icrm.shared.util.StringExtUtils;

/**
 * CodeTree 節點抽象基底
 */
public sealed abstract class AbstractCodeNode permits DefaultCodeNode, FallbackCodeNode, NullCodeNode {

    /**
     * 取得節點對應的代碼資料
     *
     * @return 代碼資料，若為虛擬節點則為 empty
     */
    public abstract Optional<Code> getCode();

    /**
     * 取得節點層級
     *
     * @return 節點層級
     */
    public abstract int getLevel();

    /**
     * 取得直接子節點
     *
     * @return 直接子節點清單，永不為 null
     */
    public abstract List<AbstractCodeNode> getChildren();

    /**
     * 取得啟用中的直接子節點
     *
     * @return 啟用中的直接子節點清單，永不為 null
     */
    public abstract List<AbstractCodeNode> getActiveChildren();

    /**
     * 取得同層預設節點
     *
     * @return 預設節點，找不到時回傳 NullCodeNode
     */
    public abstract AbstractCodeNode getDefaultNode();

    /**
     * 依 value 查找節點
     *
     * @param value 目標 value
     * @return 命中節點，未命中回傳 FallbackCodeNode；value 為 null 回傳 NullCodeNode
     */
    public abstract AbstractCodeNode getByValue(String value);

    /**
     * 依系統代碼映射查找節點
     *
     * @param system 系統名稱
     * @param foreignCode 系統代碼
     * @return 命中節點，未命中回傳 FallbackCodeNode
     */
    public abstract AbstractCodeNode getByForeignCode(String system, String foreignCode);

    /**
     * 判斷子節點是否存在對應 foreignCode
     *
     * @param foreignCode 系統代碼
     * @return true 代表子節點中至少一筆 foreign mapping 命中
     */
    public abstract boolean exists(String foreignCode);

    /** 未指定 system 時預設查詢的系統名稱 */
    private static final String DEFAULT_SYSTEM = "ICRM";

    /**
     * 依預設 system 的系統代碼映射查找節點
     *
     * @param foreignCode 系統代碼
     * @return 命中節點，未命中回傳 FallbackCodeNode
     */
    public final AbstractCodeNode getByForeignCode(String foreignCode) {
        return getByForeignCode(DEFAULT_SYSTEM, foreignCode);
    }

    /**
     * 篩選啟用中的直接子節點裡符合任一 foreignCode 的節點，不限制 system
     *
     * @param foreignCodes 系統代碼清單
     * @return 命中節點清單，永不為 null；foreignCodes 為 null、空清單或無有效值時回傳空清單
     */
    public final List<AbstractCodeNode> getCodesByForeignCode(List<String> foreignCodes) {
        if (foreignCodes == null || foreignCodes.isEmpty()) {
            return List.of();
        }

        List<String> normalizedForeignCodes = foreignCodes.stream()
                .map(StringExtUtils::normalize)
                .filter(Objects::nonNull)
                .toList();

        if (normalizedForeignCodes.isEmpty()) {
            return List.of();
        }

        return getActiveChildren().stream()
                .filter(child -> child.getCode()
                        .map(code -> CodeTreeMappings.toMap(code).values().stream()
                                .anyMatch(normalizedForeignCodes::contains))
                        .orElse(false))
                .toList();
    }

    /**
     * 篩選啟用中的直接子節點裡符合指定 systems 與 foreignCodes 的節點
     *
     * @param systems 系統名稱清單
     * @param foreignCodes 系統代碼清單
     * @return 命中節點清單，永不為 null；systems 或 foreignCodes 無有效值時回傳空清單
     */
    public final List<AbstractCodeNode> getCodeNodesByForeignCode(List<String> systems, List<String> foreignCodes) {
        if (systems == null || systems.isEmpty() || foreignCodes == null || foreignCodes.isEmpty()) {
            return List.of();
        }

        List<String> normalizedSystems = systems.stream()
                .map(StringExtUtils::normalize)
                .filter(Objects::nonNull)
                .toList();

        List<String> normalizedForeignCodes = foreignCodes.stream()
                .map(StringExtUtils::normalize)
                .filter(Objects::nonNull)
                .toList();

        if (normalizedSystems.isEmpty() || normalizedForeignCodes.isEmpty()) {
            return List.of();
        }

        return getActiveChildren().stream()
                .filter(child -> child.getCode()
                        .map(code -> CodeTreeMappings.toMap(code).entrySet().stream()
                                .anyMatch(entry -> normalizedSystems.contains(entry.getKey())
                                        && normalizedForeignCodes.contains(entry.getValue())))
                        .orElse(false))
                .toList();
    }

    /**
     * 判斷兩個節點是否相等
     *
     * @param left 左側節點
     * @param right 右側節點
     * @return true 代表相等
     */
    public static boolean isEqual(AbstractCodeNode left, AbstractCodeNode right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }

        if (left instanceof FallbackCodeNode leftFallback && right instanceof FallbackCodeNode rightFallback) {
            return Objects.equals(leftFallback.requestedValue(), rightFallback.requestedValue())
                    && Objects.equals(leftFallback.requestedSystem(), rightFallback.requestedSystem())
                    && Objects.equals(leftFallback.requestedForeignCode(), rightFallback.requestedForeignCode());
        }

        Optional<Code> leftCode = left.getCode();
        Optional<Code> rightCode = right.getCode();
        if (leftCode.isEmpty() || rightCode.isEmpty()) {
            return false;
        }

        Code leftValue = leftCode.orElseThrow();
        Code rightValue = rightCode.orElseThrow();
        return Objects.equals(leftValue.getCode(), rightValue.getCode())
                && left.getLevel() == right.getLevel()
                && Objects.equals(leftValue.getValue(), rightValue.getValue());
    }

    /**
     * 判斷當前節點與目標節點是否相等
     *
     * @param other 目標節點
     * @return true 代表相等
     */
    public final boolean equals(AbstractCodeNode other) {
        return isEqual(this, other);
    }

    /**
     * 判斷當前節點是否與任一目標節點相等
     *
     * @param nodes 目標節點清單
     * @return true 代表至少有一個節點相等
     */
    public final boolean isAnyEqual(List<AbstractCodeNode> nodes) {
        return nodes != null && nodes.stream().anyMatch(node -> isEqual(this, node));
    }

    /**
     * 判斷節點是否啟用
     *
     * @param node 節點
     * @return true 代表啟用節點
     */
    public static boolean isActive(AbstractCodeNode node) {
        if (node instanceof DefaultCodeNode defaultNode) {
            return defaultNode.getCode().map(c -> c.isActive()).orElse(false);
        }
        return false;
    }
}