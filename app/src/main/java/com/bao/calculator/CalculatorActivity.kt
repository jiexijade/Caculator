package com.bao.calculator

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout

class CalculatorActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navDrawer: View
    private var navItemBasicCalc: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

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

        // 当前是基本计算器，高亮“标准”
        navItemBasicCalc = findViewById(R.id.navItemBasicCalc)
        navItemBasicCalc?.isActivated = true

        setupNavClicks()
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
            Toast.makeText(this, "设置功能敬请期待", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(Gravity.START)
        }
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
