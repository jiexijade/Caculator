package com.bao.calculator

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.util.Locale

/**
 * 房贷/车贷计算器：等额本息、等额本金。
 * 输入：贷款总额（元）、贷款年限（年）、年利率（%）、还款方式。
 * 设计依据：DESIGN_Activities.md §3、README.md §3。
 */
class LoanCalculatorActivity : AppCompatActivity() {

    private lateinit var etLoanAmount: android.widget.EditText
    private lateinit var etYears: android.widget.EditText
    private lateinit var etLoanRate: android.widget.EditText
    private lateinit var rgRepaymentMethod: RadioGroup
    private lateinit var btnLoanCalculate: android.widget.Button
    private lateinit var tvMonthlyPayment: TextView
    private lateinit var tvTotalInterest: TextView
    private lateinit var tvTotalPayment: TextView
    private lateinit var tvAIAnalysis: TextView
    private lateinit var layoutRepaymentDetails: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_calculator)
        bindViews()
        setupToolbar()
        setupCalculateButton()
    }

    private fun bindViews() {
        etLoanAmount = findViewById(R.id.etLoanAmount)
        etYears = findViewById(R.id.etYears)
        etLoanRate = findViewById(R.id.etLoanRate)
        rgRepaymentMethod = findViewById(R.id.rgRepaymentMethod)
        btnLoanCalculate = findViewById(R.id.btnLoanCalculate)
        tvMonthlyPayment = findViewById(R.id.tvMonthlyPayment)
        tvTotalInterest = findViewById(R.id.tvTotalInterest)
        tvTotalPayment = findViewById(R.id.tvTotalPayment)
        tvAIAnalysis = findViewById(R.id.tvAIAnalysis)
        layoutRepaymentDetails = findViewById(R.id.layoutRepaymentDetails)
    }

    private fun setupToolbar() {
        findViewById<Toolbar>(R.id.toolbar)?.apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupCalculateButton() {
        btnLoanCalculate.setOnClickListener { doCalculate() }
    }

    private fun doCalculate() {
        val amountStr = etLoanAmount.text.toString().trim()
        val yearsStr = etYears.text.toString().trim()
        val rateStr = etLoanRate.text.toString().trim()

        if (amountStr.isEmpty() || yearsStr.isEmpty() || rateStr.isEmpty()) {
            Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show()
            return
        }

        val principal = amountStr.toDoubleOrNull()
        val years = yearsStr.toIntOrNull()
        val annualRate = rateStr.toDoubleOrNull()

        if (principal == null || principal <= 0) {
            Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show()
            return
        }
        if (years == null || years !in 1..30) {
            Toast.makeText(this, "贷款年限请填写 1～30 年", Toast.LENGTH_SHORT).show()
            return
        }
        if (annualRate == null || annualRate < 0) {
            Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show()
            return
        }

        val isEqualPrincipalInterest = rgRepaymentMethod.checkedRadioButtonId == R.id.rbEqualInstallment
        val result = if (isEqualPrincipalInterest) {
            LoanCalculator.equalPrincipalInterest(principal, years, annualRate)
        } else {
            LoanCalculator.equalPrincipal(principal, years, annualRate)
        }

        val locale = Locale.CHINA
        tvMonthlyPayment.text = String.format(locale, "%.2f 元", result.monthlyPayment)
        tvTotalInterest.text = String.format(locale, "%.2f 元", result.totalInterest)
        tvTotalPayment.text = String.format(locale, "%.2f 元", result.totalPayment)

        tvAIAnalysis.text = if (isEqualPrincipalInterest) {
            "根据您的贷款条件，等额本息方式每月还款固定，适合收入稳定的借款人。"
        } else {
            "等额本金方式前期月供较高、逐月递减，总利息更少，适合当前收入较高、希望减轻长期负担的借款人。"
        }

        fillRepaymentDetails(result.schedule)
    }

    /** 填充还款明细表（展示前 12 期 + … + 最后 3 期，避免视图过多） */
    private fun fillRepaymentDetails(schedule: List<LoanScheduleRow>) {
        layoutRepaymentDetails.removeAllViews()
        if (schedule.isEmpty()) return

        val total = schedule.size
        val showFirst = minOf(12, total)
        val showLast = if (total > 15) 3 else 0

        for (idx in 0 until showFirst) {
            layoutRepaymentDetails.addView(buildDetailRow(schedule[idx]))
        }
        if (showLast > 0) {
            val ellipsis = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
            }
            ellipsis.addView(TextView(this).apply {
                text = "…"
                setTextColor(0xFF5A6573.toInt())
                textSize = 14f
            })
            layoutRepaymentDetails.addView(ellipsis)
            for (idx in total - showLast until total) {
                layoutRepaymentDetails.addView(buildDetailRow(schedule[idx]))
            }
        }
    }

    private fun buildDetailRow(row: LoanScheduleRow): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val texts = listOf(
            row.period.toString(),
            String.format(Locale.CHINA, "%.2f", row.payment),
            String.format(Locale.CHINA, "%.2f", row.principal),
            String.format(Locale.CHINA, "%.2f", row.interest),
            String.format(Locale.CHINA, "%.2f", row.remaining)
        )
        val widths = listOf(70, 90, 90, 90, 100)
        for (i in texts.indices) {
            val tv = TextView(this).apply {
                text = texts[i]
                setTextColor(0xFF2C3E50.toInt())
                textSize = 12f
                gravity = Gravity.CENTER
                this.layoutParams = LinearLayout.LayoutParams(widths[i].dp(), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = 4.dp()
                }
            }
            layout.addView(tv)
        }
        return layout
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
