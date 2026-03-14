package com.bao.calculator

import kotlin.math.pow

/** 单期还款明细：期数、月供、本金、利息、剩余本金 */
data class LoanScheduleRow(
    val period: Int,
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val remaining: Double
)

/**
 * 贷款计算结果：月供、总利息、本息合计。
 * 等额本息时 monthlyPayment 为固定月供；等额本金时为首月月供。
 */
data class LoanResult(
    val monthlyPayment: Double,
    val totalInterest: Double,
    val totalPayment: Double,
    /** 每期明细（期数、月供、本金、利息、剩余本金），用于还款明细表 */
    val schedule: List<LoanScheduleRow> = emptyList()
)

/**
 * 房贷/车贷计算核心：等额本息、等额本金。
 * 约定：本金 P 元，年利率 annualRatePercent（如 4.2 表示 4.2%），年限 years 年。
 */
object LoanCalculator {

    /**
     * 等额本息：每月还款额 M = P * [i(1+i)^n] / [(1+i)^n - 1]
     * i = 月利率 = 年利率/12/100，n = 总期数 = 年限*12
     */
    fun equalPrincipalInterest(
        principal: Double,
        years: Int,
        annualRatePercent: Double
    ): LoanResult {
        val n = years * 12
        val i = annualRatePercent / 12.0 / 100.0
        val factor = (1 + i).pow(n.toDouble())
        val m = principal * (i * factor) / (factor - 1)
        val totalPayment = m * n
        val totalInterest = totalPayment - principal
        val schedule = mutableListOf<LoanScheduleRow>()
        var remaining = principal
        for (k in 1..n) {
            val interestK = remaining * i
            val principalK = m - interestK
            remaining -= principalK
            if (remaining < 0.01) remaining = 0.0
            schedule.add(LoanScheduleRow(k, m, principalK, interestK, remaining))
        }
        return LoanResult(m, totalInterest, totalPayment, schedule)
    }

    /**
     * 等额本金：每期应还本金 = P/n，第 k 期利息 = 剩余本金 * i，第 k 期月供 = 本金/n + 利息_k
     */
    fun equalPrincipal(
        principal: Double,
        years: Int,
        annualRatePercent: Double
    ): LoanResult {
        val n = years * 12
        val i = annualRatePercent / 12.0 / 100.0
        val principalPerMonth = principal / n
        var totalInterest = 0.0
        var totalPayment = 0.0
        var firstMonthPayment = 0.0
        val schedule = mutableListOf<LoanScheduleRow>()
        for (k in 1..n) {
            val remaining = principal - (k - 1) * principalPerMonth
            val interestK = remaining * i
            val paymentK = principalPerMonth + interestK
            if (k == 1) firstMonthPayment = paymentK
            totalInterest += interestK
            totalPayment += paymentK
            val remainingAfter = (remaining - principalPerMonth).coerceAtLeast(0.0)
            schedule.add(LoanScheduleRow(k, paymentK, principalPerMonth, interestK, remainingAfter))
        }
        return LoanResult(firstMonthPayment, totalInterest, totalPayment, schedule)
    }
}
