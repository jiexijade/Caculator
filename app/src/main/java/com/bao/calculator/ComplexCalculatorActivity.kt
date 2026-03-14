package com.bao.calculator

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout

/**
 * 复数科学计算器：支持两个复数 A、B 的四则运算、幂运算、共轭、幅角与极坐标/代数式显示。
 * 对应布局：activity_complex_calculator.xml；顶部标题栏带抽屉，可切换到其他功能页。
 */
class ComplexCalculatorActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navDrawer: View
    private var navItemComplexCalc: View? = null

    private lateinit var etComplexAReal: EditText
    private lateinit var etComplexAImag: EditText
    private lateinit var etComplexBReal: EditText
    private lateinit var etComplexBImag: EditText
    private lateinit var etPowerN: EditText
    private var tvComplexAResult: TextView? = null
    private var tvComplexBResult: TextView? = null
    private lateinit var tvComplexResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complex_calculator)

        drawerLayout = findViewById(R.id.drawerLayout)
        navDrawer = findViewById(R.id.navDrawer)
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
        // 标题栏右侧区域点击也打开抽屉，解决点击无反应
        findViewById<View>(R.id.toolbarTitle)?.setOnClickListener {
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

        navItemComplexCalc = findViewById(R.id.navItemComplexCalc)
        navItemComplexCalc?.isActivated = true

        setupNavClicks()
        bindViews()
        setupOperationButtons()
        setupUnaryButtons()
        setupClearButton()
        etPowerN.visibility = View.VISIBLE
        refreshABDisplay()
    }

    private fun setupNavClicks() {
        findViewById<View>(R.id.navItemBasicCalc)?.setOnClickListener {
            clearNavSelection()
            it.isActivated = true
            navTo(CalculatorActivity::class.java)
        }
        findViewById<View>(R.id.navItemComplexCalc)?.setOnClickListener {
            clearNavSelection()
            it.isActivated = true
            drawerLayout.closeDrawer(Gravity.START)
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
            Toast.makeText(this, "设置功能敬请期待", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(Gravity.START)
        }
    }

    private fun clearNavSelection() {
        findViewById<View>(R.id.navItemBasicCalc)?.isActivated = false
        navItemComplexCalc?.isActivated = false
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
        finish()
    }

    private fun bindViews() {
        etComplexAReal = findViewById(R.id.etComplexAReal)
        etComplexAImag = findViewById(R.id.etComplexAImag)
        etComplexBReal = findViewById(R.id.etComplexBReal)
        etComplexBImag = findViewById(R.id.etComplexBImag)
        etPowerN = findViewById(R.id.etPowerN)

        tvComplexResult = findViewById(R.id.tvComplexResult)
    }

    private fun setupOperationButtons() {
        findViewById<Button>(R.id.btnComplexAdd)?.setOnClickListener {
            runBinary { a, b -> a + b }
        }
        findViewById<Button>(R.id.btnComplexSubtract)?.setOnClickListener {
            runBinary { a, b -> a - b }
        }
        findViewById<Button>(R.id.btnComplexMultiply)?.setOnClickListener {
            runBinary { a, b -> a * b }
        }
        findViewById<Button>(R.id.btnComplexDivide)?.setOnClickListener {
            runBinary { a, b ->
                if (b.isZero()) throw ArithmeticException("除数不能为 0")
                a / b
            }
        }
        findViewById<Button>(R.id.btnComplexPower)?.setOnClickListener {
            runPower()
        }
    }

    private fun setupUnaryButtons() {
        findViewById<Button>(R.id.btnConjugate)?.setOnClickListener {
            showResult(parseA().conjugate())
        }
        findViewById<Button>(R.id.btnComplexConjugate)?.setOnClickListener {
            showResult(parseB().conjugate())
        }
        findViewById<Button>(R.id.btnArgument)?.setOnClickListener {
            val a = parseA()
            val deg = a.argumentDegrees()
            tvComplexResult.text = "幅角: ${formatNum(deg)}°"
        }
        findViewById<Button>(R.id.btnToPolar)?.setOnClickListener {
            tvComplexResult.text = parseA().toPolarString()
        }
        findViewById<Button>(R.id.btnToRectangular)?.setOnClickListener {
            showResult(parseA())
        }
    }

    private fun setupClearButton() {
        findViewById<Button>(R.id.btnComplexClear)?.setOnClickListener {
            etComplexAReal.text.clear()
            etComplexAImag.text.clear()
            etComplexBReal.text.clear()
            etComplexBImag.text.clear()
            etPowerN.text.clear()
            tvComplexResult.text = "0 + 0i"
            refreshABDisplay()
        }
    }

    private fun parseDouble(et: EditText): Double =
        et.text.toString().trim().toDoubleOrNull() ?: 0.0

    private fun parseA(): Complex =
        Complex(parseDouble(etComplexAReal), parseDouble(etComplexAImag))

    private fun parseB(): Complex =
        Complex(parseDouble(etComplexBReal), parseDouble(etComplexBImag))

    private fun runBinary(op: (Complex, Complex) -> Complex) {
        try {
            val a = parseA()
            val b = parseB()
            showResult(op(a, b))
        } catch (e: ArithmeticException) {
            Toast.makeText(this, e.message ?: "计算出错", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runPower() {
        try {
            val a = parseA()
            val nStr = etPowerN.text.toString().trim()
            val n = nStr.toDoubleOrNull() ?: run {
                Toast.makeText(this, "请输入有效的幂次 n", Toast.LENGTH_SHORT).show()
                return
            }
            showResult(a.pow(n))
        } catch (e: ArithmeticException) {
            Toast.makeText(this, e.message ?: "计算出错", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResult(z: Complex) {
        tvComplexResult.text = z.toString()
    }

    private fun refreshABDisplay() {
        tvComplexAResult?.text = "A = ${parseA()}"
        tvComplexBResult?.text = "B = ${parseB()}"
    }

    private fun formatNum(x: Double): String {
        if (x == x.toLong().toDouble()) return x.toLong().toString()
        return "%.2f".format(x).trimEnd('0').trimEnd('.')
    }
}
