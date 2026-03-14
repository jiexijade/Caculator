package com.bao.calculator

/**
 * 本地汇率数据（不使用网络），所有汇率以“相对人民币 CNY”存储。
 * 即：1 单位某币种 = rateToCny 人民币。
 */
object ExchangeRates {
    private val ratesToCny = mutableMapOf(
        "CNY" to 1.0,
        "USD" to 7.20,
        "EUR" to 7.80,
        "JPY" to 0.05,
        "GBP" to 9.10,
        "HKD" to 0.92,
        "KRW" to 0.0052
    )

    /** 获取支持的币种列表（排序后） */
    fun getCurrencies(): List<String> = ratesToCny.keys.toList().sorted()

    /**
     * 换算：将 amount 从 from 币种转为 to 币种。
     * amountInCny = amount * ratesToCny[from]，result = amountInCny / ratesToCny[to]
     */
    fun convert(amount: Double, from: String, to: String): Double {
        val fromRate = ratesToCny[from] ?: return 0.0
        val toRate = ratesToCny[to] ?: return 0.0
        if (toRate == 0.0) return 0.0
        val amountInCny = amount * fromRate
        return amountInCny / toRate
    }

    /** 手动更新某币种相对人民币的汇率（1 单位该币种 = rateToCny 人民币） */
    fun updateRate(currency: String, rateToCny: Double) {
        if (rateToCny > 0) {
            ratesToCny[currency] = rateToCny
        }
    }

    /** 获取某币种相对人民币的汇率 */
    fun getRateToCny(currency: String): Double? = ratesToCny[currency]

    /** 计算 1 单位 from 币种 = ? to 币种 */
    fun rateFromTo(from: String, to: String): Double? {
        val rFrom = ratesToCny[from] ?: return null
        val rTo = ratesToCny[to] ?: return null
        if (rTo == 0.0) return null
        return rFrom / rTo
    }
}
