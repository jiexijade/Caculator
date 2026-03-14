package com.bao.calculator

/**
 * 本地汇率数据与换算逻辑（不使用网络）。
 * 约定：所有汇率以“1 单位该币种 = ? 人民币”存储，即相对人民币的汇率。
 */
object ExchangeRates {

    /** 币种代码 -> 1 单位该币种可兑换的人民币数量 */
    private val ratesToCny: MutableMap<String, Double> = mutableMapOf(
        "CNY" to 1.0,
        "USD" to 7.20,
        "EUR" to 7.80,
        "JPY" to 0.05,
        "GBP" to 9.10,
        "HKD" to 0.92,
        "KRW" to 0.0052
    )

    /** 支持的币种代码列表（用于 Spinner） */
    fun getCurrencyCodes(): List<String> = ratesToCny.keys.toList().sorted()

    /**
     * 将 amount 从 from 币种换算为 to 币种。
     * @param amount 金额
     * @param from 源币种代码（如 "USD"）
     * @param to 目标币种代码（如 "CNY"）
     * @return 换算后的金额
     */
    fun convert(amount: Double, from: String, to: String): Double {
        val rateFrom = ratesToCny[from] ?: throw IllegalArgumentException("不支持的源币种: $from")
        val rateTo = ratesToCny[to] ?: throw IllegalArgumentException("不支持的目标币种: $to")
        val amountInCny = amount * rateFrom
        return amountInCny / rateTo
    }

    /**
     * 手动更新某币种相对人民币的汇率。
     * @param currency 币种代码
     * @param rateToCny 1 单位该币种 = rateToCny 人民币
     */
    fun updateRate(currency: String, rateToCny: Double) {
        if (rateToCny <= 0) throw IllegalArgumentException("汇率必须为正数")
        ratesToCny[currency] = rateToCny
    }

    /** 获取某币种当前相对人民币的汇率，用于显示或编辑 */
    fun getRateToCny(currency: String): Double? = ratesToCny[currency]
}
