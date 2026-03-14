package com.bao.calculator

import kotlin.math.*

/**
 * 复数数据类：z = re + im * i
 * 支持四则运算、共轭、幅角、模、幂运算及格式化输出。
 */
data class Complex(val re: Double, val im: Double) {

    operator fun plus(other: Complex): Complex =
        Complex(re + other.re, im + other.im)

    operator fun minus(other: Complex): Complex =
        Complex(re - other.re, im - other.im)

    operator fun times(other: Complex): Complex =
        Complex(
            re * other.re - im * other.im,
            re * other.im + im * other.re
        )

    operator fun div(other: Complex): Complex {
        val d = other.re * other.re + other.im * other.im
        if (d == 0.0) throw ArithmeticException("除数不能为 0")
        return Complex(
            (re * other.re + im * other.im) / d,
            (im * other.re - re * other.im) / d
        )
    }

    /** 是否为零 */
    fun isZero(): Boolean = re == 0.0 && im == 0.0

    /** 共轭复数 */
    fun conjugate(): Complex = Complex(re, -im)

    /** 幅角（弧度），范围 (-π, π] */
    fun argument(): Double = atan2(im, re)

    /** 幅角（度） */
    fun argumentDegrees(): Double = Math.toDegrees(argument())

    /** 模（绝对值） */
    fun magnitude(): Double = sqrt(re * re + im * im)

    /** 复数幂 z^n，n 为实数 */
    fun pow(n: Double): Complex {
        if (isZero()) return if (n > 0) this else throw ArithmeticException("0 的负数或零次幂无定义")
        val r = magnitude()
        val theta = argument()
        val rn = r.pow(n)
        val nTheta = n * theta
        return Complex(rn * cos(nTheta), rn * sin(nTheta))
    }

    /** 极坐标形式字符串：r = ..., θ = ...° */
    fun toPolarString(): String {
        val r = magnitude()
        val deg = argumentDegrees()
        return "r = ${formatNum(r)}, θ = ${formatNum(deg)}°"
    }

    override fun toString(): String {
        val reStr = formatNum(re)
        return when {
            im == 0.0 -> reStr
            im > 0 -> "$reStr + ${formatNum(im)}i"
            else -> "$reStr - ${formatNum(-im)}i"
        }
    }

    companion object {
        private const val DECIMAL_PLACES = 2

        private fun formatNum(x: Double): String {
            if (x == x.toLong().toDouble()) return x.toLong().toString()
            return "%.${DECIMAL_PLACES}f".format(x).trimEnd('0').trimEnd('.')
        }
    }
}
