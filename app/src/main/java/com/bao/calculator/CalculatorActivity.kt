package com.bao.calculator

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import kotlin.math.sqrt

class CalculatorActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navDrawer: View
    private var navItemBasicCalc: View? = null

    // 计算器显示与状态
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvHexResult: TextView
    private lateinit var tvMode: TextView
    private lateinit var gridKeyboard: GridLayout

    private val currentExpression = StringBuilder()
    private var isDecimalMode = true
    private var justCalculated = false
    private var decimalPlaces = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeFromPreference()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (savedInstanceState == null) {
            isDecimalMode = prefs.getString("default_calc_mode", "decimal") != "hex"
        }
        decimalPlaces = prefs.getString("decimal_places", "6")?.toIntOrNull() ?: 6

        drawerLayout = findViewById(R.id.drawerLayout)
        navDrawer = findViewById(R.id.navDrawer)

        // 设置抽屉宽度为屏幕的 3/4
        navDrawer.post {
            val width = (resources.displayMetrics.widthPixels * 0.75f).toInt()
            val params = (navDrawer.layoutParams as? DrawerLayout.LayoutParams)
                ?: DrawerLayout.LayoutParams(width, DrawerLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.START }
            params.width = width
            params.gravity = Gravity.START
            navDrawer.layoutParams = params
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }
        // 标题栏右侧“标准模式”区域点击也打开抽屉，解决点击无反应
        findViewById<View>(R.id.toolbarContent)?.setOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }
        // 扩大左侧菜单图标触摸区域（至少 48dp），提升点击响应
        toolbar.post {
            if (toolbar.childCount > 0) {
                val navBtn = toolbar.getChildAt(0)
                val minSize = (48 * resources.displayMetrics.density).toInt()
                navBtn.minimumWidth = minSize
                navBtn.minimumHeight = minSize
            }
        }

        navItemBasicCalc = findViewById(R.id.navItemBasicCalc)
        navItemBasicCalc?.isActivated = true

        setupNavClicks()
        setupCalculator()
    }

    override fun onResume() {
        super.onResume()
        decimalPlaces = PreferenceManager.getDefaultSharedPreferences(this).getString("decimal_places", "6")?.toIntOrNull() ?: 6
    }

    private fun setupNavClicks() {
        findViewById<View>(R.id.navItemBasicCalc)?.setOnClickListener {
            clearNavSelection()
            it.isActivated = true
            drawerLayout.closeDrawer(Gravity.START)
        }
        findViewById<View>(R.id.navItemComplexCalc)?.setOnClickListener {
            navTo(ComplexCalculatorActivity::class.java)
        }
        findViewById<View>(R.id.navItemLoan)?.setOnClickListener {
            navTo(LoanCalculatorActivity::class.java)
        }
        findViewById<View>(R.id.navItemCurrency)?.setOnClickListener {
            navTo(CurrencyCalculatorActivity::class.java)
        }
        findViewById<View>(R.id.navItemLength)?.setOnClickListener {
            navTo(LengthConverterActivity::class.java)
        }
        findViewById<View>(R.id.navItemSpeed)?.setOnClickListener {
            navTo(SpeedConverterActivity::class.java)
        }
        findViewById<View>(R.id.navItemWeight)?.setOnClickListener {
            navTo(WeightConverterActivity::class.java)
        }
        findViewById<View>(R.id.navItemData)?.setOnClickListener {
            navTo(DataConverterActivity::class.java)
        }
        findViewById<View>(R.id.navItemSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupCalculator() {
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        tvHexResult = findViewById(R.id.tvHexResult)
        tvMode = findViewById(R.id.tvMode)
        gridKeyboard = findViewById<GridLayout>(R.id.gridKeyboard)

        findViewById<Button>(R.id.btnDecimalMode)?.setOnClickListener {
            if (!isDecimalMode) {
                isDecimalMode = true
                tvMode.text = "标准模式"
                tvHexResult.visibility = View.GONE
                updateModeButtonState()
                refreshDisplay()
            }
        }
        findViewById<Button>(R.id.btnHexMode)?.setOnClickListener {
            if (isDecimalMode) {
                isDecimalMode = false
                tvMode.text = "十六进制"
                tvHexResult.visibility = View.VISIBLE
                updateModeButtonState()
                refreshDisplay()
            }
        }
        updateModeButtonState()

        for (i in 0 until gridKeyboard.childCount) {
            val child = gridKeyboard.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener { onKey(child.text.toString()) }
            }
        }
        refreshDisplay()
    }

    private fun updateModeButtonState() {
        findViewById<Button>(R.id.btnDecimalMode)?.isActivated = isDecimalMode
        findViewById<Button>(R.id.btnHexMode)?.isActivated = !isDecimalMode
    }

    private fun onKey(key: String) {
        when (key) {
            "AC" -> {
                currentExpression.clear()
                tvResult.text = "0"
                justCalculated = false
            }
            "DEL" -> {
                if (currentExpression.isNotEmpty()) {
                    currentExpression.deleteCharAt(currentExpression.length - 1)
                    justCalculated = false
                }
            }
            "=" -> {
                if (currentExpression.isEmpty()) return
                val result = evaluate(currentExpression.toString())
                if (result != null) {
                    val resultStr = if (isDecimalMode) formatDecimal(result) else result.toLong().toString(16).uppercase()
                    tvResult.text = resultStr
                    currentExpression.clear()
                    currentExpression.append(resultStr)
                    justCalculated = true
                } else {
                    tvResult.text = "错误"
                }
            }
            "+/-" -> {
                if (!isDecimalMode) return
                toggleSign()
                justCalculated = false
            }
            "DEC (十进制)", "HEX (十六进制)" -> return
            else -> {
                if (justCalculated) {
                    when {
                        key.length == 1 && key[0].isDigit() || key == "." || (key.length == 1 && key[0] in 'A'..'F') -> {
                            currentExpression.clear()
                            currentExpression.append(key)
                        }
                        key in listOf("+", "-", "×", "÷", "%", "(", ")", "x²", "√") -> {
                            currentExpression.clear()
                            if (key == "x²") currentExpression.append(tvResult.text).append("²")
                            else if (key == "√") currentExpression.append("√").append(tvResult.text)
                            else currentExpression.append(tvResult.text).append(key)
                        }
                        else -> return
                    }
                    justCalculated = false
                } else {
                    when {
                        key == "x²" -> currentExpression.append("²")
                        key == "√" -> currentExpression.append("√")
                        key in listOf("(", ")", "+", "-", "×", "÷", "%") -> currentExpression.append(key)
                        key == "." -> { if (isDecimalMode) currentExpression.append(key) }
                        key.length == 1 && key[0].isDigit() -> currentExpression.append(key)
                        key.length == 1 && key[0] in 'A'..'F' -> { if (!isDecimalMode) currentExpression.append(key.uppercase()) }
                        else -> currentExpression.append(key)
                    }
                }
            }
        }
        refreshDisplay()
    }

    private fun toggleSign() {
        val s = currentExpression.toString().trim()
        if (s.isEmpty()) return
        var i = s.length - 1
        while (i >= 0 && (s[i].isDigit() || s[i] == '.')) i--
        while (i >= 0 && (s[i] == '+' || s[i] == '-')) i--
        val numStart = i + 1
        if (numStart >= s.length) return
        val num = s.substring(numStart, s.length)
        if (num == "0" || num.isEmpty()) return
        val prefix = s.substring(0, numStart)
        val newNum = if (num.startsWith("-")) num.drop(1) else "-$num"
        currentExpression.clear()
        currentExpression.append(prefix).append(newNum)
    }

    private fun refreshDisplay() {
        val expr = currentExpression.toString()
        tvExpression.text = if (expr.isEmpty()) "0" else expr
        if (!justCalculated && tvResult.text.toString() != "错误") {
            tvResult.text = if (expr.isEmpty()) "0" else expr
        }
        if (!isDecimalMode && expr.isNotEmpty()) {
            val result = evaluate(expr)
            tvHexResult.text = "HEX: ${if (result != null) result.toLong().toString(16).uppercase() else "?"}"
        } else {
            tvHexResult.text = "HEX: 0"
        }
    }

    private fun formatDecimal(d: Double): String {
        return if (d == d.toLong().toDouble() && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
            d.toLong().toString()
        } else {
            "%.${decimalPlaces}f".format(d).trimEnd('0').trimEnd('.')
        }
    }

    private fun applyThemeFromPreference() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("theme", "follow_system") ?: "follow_system"
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun evaluate(expr: String): Double? {
        if (expr.isBlank()) return null
        val s = expr.replace("×", "*").replace("÷", "/").replace(" ", "").trim()
        if (s.isEmpty()) return null
        return try {
            if (isDecimalMode) evaluateDecimal(s) else evaluateHex(s)
        } catch (e: Exception) {
            null
        }
    }

    private fun evaluateDecimal(s: String): Double {
        val parser = object {
            var i = 0
            fun skipSpace() { while (i < s.length && s[i] == ' ') i++ }
            fun parseNumber(): Double {
                skipSpace()
                if (i >= s.length) throw RuntimeException("expected number")
                val start = i
                if (s[i] == '-') { i++; if (i >= s.length) throw RuntimeException("expected number") }
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                val sub = s.substring(start, i)
                return sub.toDoubleOrNull() ?: throw RuntimeException("invalid number: $sub")
            }
            fun parseExpr(): Double {
                var v = parseTerm()
                while (i < s.length) {
                    skipSpace()
                    if (i >= s.length) break
                    when (s[i]) {
                        '+' -> { i++; v += parseTerm() }
                        '-' -> { i++; v -= parseTerm() }
                        else -> break
                    }
                }
                return v
            }
            fun parseFactor(): Double {
                skipSpace()
                if (i >= s.length) throw RuntimeException("expected factor")
                when (s[i]) {
                    '√' -> {
                        i++
                        val v = parseFactor()
                        if (v < 0) throw RuntimeException("sqrt of negative")
                        return sqrt(v)
                    }
                    '(' -> {
                        i++
                        val v = parseExpr()
                        skipSpace()
                        if (i >= s.length || s[i] != ')') throw RuntimeException("expected )")
                        i++
                        skipSpace()
                        if (i < s.length && s[i] == '²') { i++; return v * v }
                        return v
                    }
                    '-' -> { i++; return -parseFactor() }
                    '+' -> { i++; return parseFactor() }
                    else -> {
                        val v = parseNumber()
                        skipSpace()
                        if (i < s.length && s[i] == '²') { i++; return v * v }
                        return v
                    }
                }
            }
            fun parseTerm(): Double {
                var v = parseFactor()
                while (i < s.length) {
                    skipSpace()
                    if (i >= s.length) break
                    val op = when {
                        s[i] == '*' -> { i++; "*" }
                        s[i] == '/' -> { i++; "/" }
                        s[i] == '%' -> { i++; "%" }
                        else -> null
                    } ?: break
                    val b = parseFactor()
                    v = when (op) {
                        "*" -> v * b
                        "/" -> if (b == 0.0) throw RuntimeException("divide by zero") else v / b
                        "%" -> v % b
                        else -> v
                    }
                }
                return v
            }
        }
        val result = parser.parseExpr()
        parser.skipSpace()
        if (parser.i != s.length) throw RuntimeException("unexpected")
        return result
    }

    private fun evaluateHex(s: String): Double {
        val parser = object {
            var i = 0
            fun skipSpace() { while (i < s.length && s[i] == ' ') i++ }
            fun parseHexNumber(): Long {
                skipSpace()
                if (i >= s.length) throw RuntimeException("expected hex number")
                val start = i
                if (s[i] == '-') { i++; if (i >= s.length) throw RuntimeException("expected number") }
                while (i < s.length && (s[i].isDigit() || s[i] in 'A'..'F' || s[i] in 'a'..'f')) i++
                val sub = s.substring(start, i)
                return sub.toLongOrNull(16) ?: throw RuntimeException("invalid hex: $sub")
            }
            fun parseExpr(): Long {
                var v = parseTerm()
                while (i < s.length) {
                    skipSpace()
                    if (i >= s.length) break
                    when (s[i]) {
                        '+' -> { i++; v += parseTerm() }
                        '-' -> { i++; v -= parseTerm() }
                        else -> break
                    }
                }
                return v
            }
            fun parseFactor(): Long {
                skipSpace()
                if (i >= s.length) throw RuntimeException("expected factor")
                when (s[i]) {
                    '√' -> {
                        i++
                        val v = parseFactor()
                        if (v < 0) throw RuntimeException("sqrt of negative")
                        return sqrt(v.toDouble()).toLong()
                    }
                    '(' -> {
                        i++
                        val v = parseExpr()
                        skipSpace()
                        if (i >= s.length || s[i] != ')') throw RuntimeException("expected )")
                        i++
                        skipSpace()
                        if (i < s.length && s[i] == '²') { i++; return v * v }
                        return v
                    }
                    '-' -> { i++; return -parseFactor() }
                    '+' -> { i++; return parseFactor() }
                    else -> {
                        val v = parseHexNumber()
                        skipSpace()
                        if (i < s.length && s[i] == '²') { i++; return v * v }
                        return v
                    }
                }
            }
            fun parseTerm(): Long {
                var v = parseFactor()
                while (i < s.length) {
                    skipSpace()
                    if (i >= s.length) break
                    val op = when {
                        s[i] == '*' -> { i++; "*" }
                        s[i] == '/' -> { i++; "/" }
                        s[i] == '%' -> { i++; "%" }
                        else -> null
                    } ?: break
                    val b = parseFactor()
                    v = when (op) {
                        "*" -> v * b
                        "/" -> if (b == 0L) throw RuntimeException("divide by zero") else v / b
                        "%" -> v % b
                        else -> v
                    }
                }
                return v
            }
        }
        val result = parser.parseExpr()
        parser.skipSpace()
        if (parser.i != s.length) throw RuntimeException("unexpected")
        return result.toDouble()
    }

    private fun clearNavSelection() {
        navItemBasicCalc?.isActivated = false
        findViewById<View>(R.id.navItemComplexCalc)?.isActivated = false
        findViewById<View>(R.id.navItemLoan)?.isActivated = false
        findViewById<View>(R.id.navItemCurrency)?.isActivated = false
        findViewById<View>(R.id.navItemLength)?.isActivated = false
        findViewById<View>(R.id.navItemSpeed)?.isActivated = false
        findViewById<View>(R.id.navItemWeight)?.isActivated = false
        findViewById<View>(R.id.navItemData)?.isActivated = false
    }

    private fun navTo(activityClass: Class<*>) {
        clearNavSelection()
        drawerLayout.closeDrawer(Gravity.START)
        startActivity(Intent(this, activityClass))
    }
}
