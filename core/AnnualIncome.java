package com.esb.icrm.code.definition;

import java.math.BigDecimal;

import com.esb.icrm.code.application.facade.CodeTreeFacade;
import com.esb.icrm.code.definition.base.CodeNode;
import com.esb.icrm.code.definition.base.ThresholdNodeResolver;
import com.esb.icrm.code.node.AbstractCodeNode;

/**
 * 年收入
 */
public class AnnualIncome extends CodeNode {
    /** AmountValue code */
    public static final String CODE = "AnnualIncome";

    /**
     * 建構子
     *
     * @param facade CodeTree 外觀服務
     */
    public AnnualIncome(CodeTreeFacade facade) {
        super(facade, CODE);
    }

    /**
     * 建構子 (內部節點)
     *
     * @param facade CodeTree 外觀服務
     * @param node 當前節點
     */
    public AnnualIncome(CodeTreeFacade facade, AbstractCodeNode node) {
        super(facade, CODE, node);
    }

    /**
     * 依輸入金額取得年收入門檻節點
     *
     * @param amount 輸入金額
     * @return 最小的參數值大於等於輸入金額的節點；若超過最大門檻則回傳最大門檻節點
     */
    public AbstractCodeNode getByThreshold(BigDecimal amount) {
        return ThresholdNodeResolver.resolve(current(), amount);
    }
}