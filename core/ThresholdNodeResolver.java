package com.esb.icrm.code.definition.base;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;

import com.esb.icrm.code.node.AbstractCodeNode;
import com.esb.icrm.code.node.NullCodeNode;

/**
 * 依子節點參數值解析門檻節點
 */
public final class ThresholdNodeResolver {

    private ThresholdNodeResolver() {
    }

    /**
     * 取得第一個大於等於輸入金額的門檻節點
     *
     * @param current 當前節點
     * @param amount 輸入金額
     * @return 命中的門檻節點；超過最大門檻時回傳最大門檻，無有效門檻時回傳空節點
     */
    public static AbstractCodeNode resolve(AbstractCodeNode current, BigDecimal amount) {
        if (amount == null) {
            return NullCodeNode.instance();
        }

        ThresholdNode[] thresholdNodes = current.getChildren().stream()
                .map(node -> new ThresholdNode(node, extractThreshold(node)))
                .filter(node -> node.threshold() != null)
                .toArray(ThresholdNode[]::new);
        if (thresholdNodes.length == 0) {
            return NullCodeNode.instance();
        }

        Arrays.sort(thresholdNodes, Comparator.comparing(t -> t.threshold()));

        int low = 0;
        int high = thresholdNodes.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (thresholdNodes[middle].threshold().compareTo(amount) >= 0) {
                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }

        return thresholdNodes[Math.min(low, thresholdNodes.length - 1)].node();
    }

    private static BigDecimal extractThreshold(AbstractCodeNode node) {
        return node.getCode()
                .map(c -> c.getParameterValue())
                .map(p -> p.value())
                .orElse(null);
    }

    private record ThresholdNode(AbstractCodeNode node, BigDecimal threshold) {
    }
}