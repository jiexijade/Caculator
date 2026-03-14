package com.bao.calculator

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.util.Locale

/**
 * 汇率计算器：多币种换算，使用本地预设汇率，支持双向换算与手动更新汇率。
 * 对应布局：activity_currency_calculator.xml
 */
class CurrencyCalculatorActivity : AppCompatActivity() {

    private lateinit var spFromCurrency: Spinner
    private lateinit var spToCurrency: Spinner
    private lateinit var etCurrencyAmount: EditText
    private lateinit var tvCurrencyResult: TextView
    private lateinit var tvExchangeRate: TextView
    private lateinit var btnCurrencyConvert: Button
    private lateinit var btnUpdateRate: Button

    private var currencyCodes: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_calculator)

        findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = "货币转换"
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        currencyCodes = ExchangeRates.getCurrencyCodes()
        bindViews()
        setupSpinners()
        setupConvertButton()
        setupUpdateRateButton()
        setupKeyboard()
        updateRateHint()
    }

    private fun bindViews() {
        spFromCurrency = findViewById(R.id.spFromCurrency)
        spToCurrency = findViewById(R.id.spToCurrency)
        etCurrencyAmount = findViewById(R.id.etCurrencyAmount)
        tvCurrencyResult = findViewById(R.id.tvCurrencyResult)
        tvExchangeRate = findViewById(R.id.tvExchangeRate)
        btnCurrencyConvert = findViewById(R.id.btnCurrencyConvert)
        btnUpdateRate = findViewById(R.id.btnUpdateRate)
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyCodes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFromCurrency.adapter = adapter
        spToCurrency.adapter = adapter

        val defaultFrom = currencyCodes.indexOf("USD").takeIf { it >= 0 } ?: 0
        val defaultTo = currencyCodes.indexOf("CNY").takeIf { it >= 0 } ?: 0
        spFromCurrency.setSelection(defaultFrom)
        spToCurrency.setSelection(defaultTo)

        val onCurrencySelected = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRateHint()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spFromCurrency.onItemSelectedListener = onCurrencySelected
        spToCurrency.onItemSelectedListener = onCurrencySelected
    }

    private fun setupConvertButton() {
        btnCurrencyConvert.setOnClickListener { performConvert() }
    }

    private fun performConvert() {
        val amountStr = etCurrencyAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount < 0) {
            Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show()
            return
        }

        val from = currencyCodes[spFromCurrency.selectedItemPosition]
        val to = currencyCodes[spToCurrency.selectedItemPosition]

        try {
            val result = ExchangeRates.convert(amount, from, to)
            tvCurrencyResult.text = String.format(Locale.CHINA, "%.4f", result)
            updateRateHint()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, e.message ?: "换算失败", Toast.LENGTH_SHORT).show()
        }
    }

    /** 更新底部“1 XXX = ? YYY”的汇率提示 */
    private fun updateRateHint() {
        val from = currencyCodes[spFromCurrency.selectedItemPosition]
        val to = currencyCodes[spToCurrency.selectedItemPosition]
        if (from == to) {
            tvExchangeRate.text = "相同币种无需换算"
            return
        }
        try {
            val oneToOther = ExchangeRates.convert(1.0, from, to)
            tvExchangeRate.text = String.format(Locale.CHINA, "1 %s = %.4f %s", from, oneToOther, to)
        } catch (_: Exception) {
            tvExchangeRate.text = ""
        }
    }

    private fun setupUpdateRateButton() {
        btnUpdateRate.setOnClickListener {
            val items = currencyCodes.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("选择要更新的币种")
                .setItems(items) { _, which ->
                    val code = items[which]
                    showUpdateRateDialog(code)
                }
                .show()
        }
    }

    private fun showUpdateRateDialog(currency: String) {
        val currentRate = ExchangeRates.getRateToCny(currency) ?: 1.0
        val input = EditText(this).apply {
            hint = "1 $currency = ? CNY"
            setText(currentRate.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(this)
            .setTitle("更新汇率：$currency")
            .setMessage("输入 1 $currency 可兑换的人民币数量（CNY）")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val newRateStr = input.text.toString().trim()
                if (newRateStr.isEmpty()) {
                    Toast.makeText(this, "请输入汇率", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newRate = newRateStr.toDoubleOrNull()
                if (newRate == null || newRate <= 0) {
                    Toast.makeText(this, "汇率必须为正数", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    ExchangeRates.updateRate(currency, newRate)
                    Toast.makeText(this, "已更新 $currency 汇率", Toast.LENGTH_SHORT).show()
                    updateRateHint()
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, e.message ?: "更新失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupKeyboard() {
        val keys = listOf(
            R.id.btnCurrency7 to "7", R.id.btnCurrency8 to "8", R.id.btnCurrency9 to "9",
            R.id.btnCurrency4 to "4", R.id.btnCurrency5 to "5", R.id.btnCurrency6 to "6",
            R.id.btnCurrency1 to "1", R.id.btnCurrency2 to "2", R.id.btnCurrency3 to "3",
            R.id.btnCurrencyDot to ".", R.id.btnCurrency0 to "0"
        )
        keys.forEach { (id, char) ->
            findViewById<Button>(id).setOnClickListener { appendToAmount(char) }
        }
        findViewById<Button>(R.id.btnCurrencyDEL).setOnClickListener { deleteLastAmount() }
    }

    private fun appendToAmount(char: String) {
        val et = etCurrencyAmount
        val cur = et.text.toString()
        if (char == "." && cur.contains(".")) return
        if (char == "." && cur.isEmpty()) {
            et.setText("0.")
            et.setSelection(et.text?.length ?: 0)
            return
        }
        if (cur == "0" && char != ".") {
            et.setText(char)
        } else {
            et.setText(cur + char)
        }
        et.setSelection(et.text?.length ?: 0)
    }

    private fun deleteLastAmount() {
        val et = etCurrencyAmount
        val cur = et.text.toString()
        if (cur.isEmpty()) return
        et.setText(cur.dropLast(1))
        et.setSelection(et.text?.length ?: 0)
    }
}
