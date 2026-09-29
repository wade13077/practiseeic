package com.esb.icrm.code;

import java.math.BigDecimal;

import com.esb.icrm.code.application.facade.CodeTreeFacade;
import com.esb.icrm.code.definition.AnnualIncome;
import com.esb.icrm.code.node.AbstractCodeNode;

public class test {

	public static void main(String[] args) {
		BigDecimal income = new BigDecimal("1000000.50");
		BigDecimal bonus = BigDecimal.valueOf(25000.75);
		BigDecimal taxRate = new BigDecimal("0.05");

		System.out.println("年收入: " + income);
		System.out.println("獎金: " + bonus);
		System.out.println("合計: " + income.add(bonus));
		System.out.println("扣稅後: " + income.multiply(BigDecimal.ONE.subtract(taxRate)));
		System.out.println("收入是否至少 500000: "
				+ (income.compareTo(new BigDecimal("500000")) >= 0));

		// 金額請用字串或 BigDecimal.valueOf，避免 new BigDecimal(0.1) 的浮點數誤差。
		System.out.println("建議寫法: " + new BigDecimal("0.1"));
		System.out.println("不建議寫法: " + new BigDecimal(0.1));
	}

	public static AbstractCodeNode findAnnualIncome(CodeTreeFacade facade, String amount) {
		AnnualIncome annualIncome = new AnnualIncome(facade);
		AbstractCodeNode node = annualIncome.getByThreshold(new BigDecimal(amount));

		System.out.println("輸入年收入: " + amount);
		System.out.println("命中節點: " + node);
		return node;
	}
}
