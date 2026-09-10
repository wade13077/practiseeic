package com.esb.icrm.code.definition.base;

import java.util.Objects;

import com.esb.icrm.code.domain.Code;
import com.esb.icrm.code.application.facade.CodeTreeFacade;
import com.esb.icrm.code.node.AbstractCodeNode;
import com.esb.icrm.code.node.DefaultCodeNode;
import com.esb.icrm.code.node.FallbackCodeNode;
import com.esb.icrm.code.node.NullCodeNode;

/**
 * 宣告程式碼節點定義基底
 * <p>
 * 子類別可透過 {@link #fromChildValue(String)} 與 {@link #fromForeignMapping(String, String)} 快速取得對應節點
 * </p>
 */
public abstract class CodeNode {

    /** CodeTree 外觀服務 */
    private final CodeTreeFacade facade;

    /** 代碼大分類識別碼 */
    private final String code;

    /** 當前節點 */
    private final AbstractCodeNode currentNode;

    /**
     * 建構子
     *
     * @param facade CodeTree 外觀服務
     * @param code 代碼大分類識別碼
     */
    protected CodeNode(CodeTreeFacade facade, String code) {
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.code = normalizeCode(code);
        this.currentNode = facade.getNodeByCodeLevelAndValue(this.code, 0, CodeTreeFacade.ROOT_VALUE);
    }

    /**
     * 建構子 (內部巢狀節點)
     *
     * @param facade CodeTree 外觀服務
     * @param code 代碼大分類識別碼
     * @param currentNode 當前節點
     */
    protected CodeNode(CodeTreeFacade facade, String code, AbstractCodeNode currentNode) {
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.code = normalizeCode(code);
        this.currentNode = Objects.requireNonNullElse(currentNode, NullCodeNode.instance());
    }

    /**
     * 取得此定義的根節點
     *
     * @return 根節點
     */
    protected AbstractCodeNode root() {
        return facade.getNodeByCodeLevelAndValue(code, 0, CodeTreeFacade.ROOT_VALUE);
    }

    /**
     * 取得當前節點
     *
     * @return 當前節點
     */
    protected AbstractCodeNode current() {
        return currentNode;
    }

    /**
     * 取得當前節點 (對外)
     *
     * @return 當前節點
     */
    public AbstractCodeNode node() {
        return currentNode;
    }

    /**
     * 取得外觀服務
     *
     * @return CodeTree 外觀服務
     */
    protected CodeTreeFacade facade() {
        return facade;
    }

    /**
     * 依 child value 取得節點
     *
     * @param childValue 子節點 value
     * @return 命中節點或 fallback/null 節點
     */
    protected AbstractCodeNode fromChildValue(String childValue) {
        if (childValue == null) {
            return NullCodeNode.instance();
        }

        String normalized = childValue.trim();
        if (normalized.isEmpty()) {
            return new FallbackCodeNode(normalized, null, null);
        }

        return resolveChildNodeByValue(normalized);
    }

    @SuppressWarnings("null") // Optional.map(Code::getValue) 的 method reference 觸發 JDT 假陽性
    private AbstractCodeNode resolveChildNodeByValue(String normalizedValue) {
        return currentNode.getChildren().stream()
                .filter(node -> node.getCode().map(Code::getValue).filter(normalizedValue::equals).isPresent())
                .findFirst()
                .orElseGet(() -> resolveByFacade(normalizedValue));
    }

    private AbstractCodeNode resolveByFacade(String normalizedValue) {
        if (currentNode instanceof DefaultCodeNode defaultNode) {
            int targetLevel = defaultNode.getLevel() + 1;
            return facade.getNodeByCodeLevelAndValue(code, targetLevel, normalizedValue);
        }

        return facade.getNodesByCode(code).stream()
                .filter(node -> node.getCode().map(codeEntry -> normalizedValue.equals(codeEntry.getValue())).orElse(false))
                .findFirst()
                .orElseGet(() -> new FallbackCodeNode(normalizedValue, null, null));
    }

    /**
     * 依 child value 取得節點
     *
     * @param childValue 子節點 value
     * @return 命中節點或 fallback/null 節點
     */
    protected AbstractCodeNode fromChildValue(int childValue) {
        return fromChildValue(String.valueOf(childValue));
    }

    /**
     * 依 child value 取得強型別節點
     *
     * @param childValue 子節點 value
     * @param factory 節點工廠
     * @param <T> 目標型別
     * @return 強型別節點
     */
    protected <T extends CodeNode> T fromChildValue(String childValue, CodeNodeFactory<T> factory) {
        return factory.create(facade, code, fromChildValue(childValue));
    }

    /**
     * 依 child value 取得強型別節點
     *
     * @param childValue 子節點 value
     * @param factory 節點工廠
     * @param <T> 目標型別
     * @return 強型別節點
     */
    protected <T extends CodeNode> T fromChildValue(int childValue, CodeNodeFactory<T> factory) {
        return factory.create(facade, code, fromChildValue(childValue));
    }

    /**
     * 依外部映射取得節點
     *
     * @param system 系統名稱
     * @param foreignCode 外部代碼
     * @return 命中節點或 fallback 節點
     */
    protected AbstractCodeNode fromForeignMapping(String system, String foreignCode) {
        return facade.getNodeByCodeAndForeignCode(code, foreignCode);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        return code.trim();
    }

    /**
     * 強型別節點工廠
     *
     * @param <T> 節點型別
     */
    @FunctionalInterface
    protected interface CodeNodeFactory<T extends CodeNode> {
        /**
         * 建立節點
         *
         * @param facade CodeTree 外觀服務
         * @param code 代碼大分類識別碼
         * @param node 當前節點
         * @return 目標節點
         */
        T create(CodeTreeFacade facade, String code, AbstractCodeNode node);
    }
}