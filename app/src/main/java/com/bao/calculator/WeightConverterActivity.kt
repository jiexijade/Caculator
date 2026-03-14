package com.bao.calculator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.text.DecimalFormat

class WeightConverterActivity : AppCompatActivity() {

    private lateinit var etWeightFrom: EditText
    private lateinit var tvWeightTo: TextView
    private lateinit var spinnerWeightFrom: Spinner
    private lateinit var spinnerWeightTo: Spinner
    private lateinit var btnSwapWeight: ImageButton

    // 重量单位列表（与 DESIGN_Activities 一致：kg, g, t, lb 等）
    private val units = arrayOf(
        "毫克 (mg)",
        "克 (g)",
        "千克/公斤 (kg)",
        "吨 (t)",
        "磅 (lb)",
        "盎司 (oz)",
        "市斤 (斤)",
        "两"
    )

    // 各单位相对于「千克(kg)」的转换比例
    private val factors = doubleArrayOf(
        0.000001,       // 毫克 (mg)
        0.001,          // 克 (g)
        1.0,            // 千克/公斤 (kg)
        1000.0,         // 吨 (t)
        0.45359237,     // 磅 (lb)
        0.028349523,    // 盎司 (oz)
        0.5,            // 市斤 (斤) -> 1 斤 = 0.5 kg
        0.05            // 两 -> 1 两 = 0.05 kg
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_converter)

        findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = "重量换算"
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        initViews()
        setupSpinners()
        setupCustomKeyboard()
        setupListeners()
    }

    private fun initViews() {
        // 布局中复用与长度/速度相同的 ID：etLengthFrom / tvLengthTo / spinnerLengthFrom / spinnerLengthTo / btnSwapLength
        etWeightFrom = findViewById(R.id.etLengthFrom)
        tvWeightTo = findViewById(R.id.tvLengthTo)
        spinnerWeightFrom = findViewById(R.id.spinnerLengthFrom)
        spinnerWeightTo = findViewById(R.id.spinnerLengthTo)
        btnSwapWeight = findViewById(R.id.btnSwapLength)

        etWeightFrom.showSoftInputOnFocus = false
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerWeightFrom.adapter = adapter
        spinnerWeightTo.adapter = adapter

        spinnerWeightFrom.setSelection(2) // 千克
        spinnerWeightTo.setSelection(1)  // 克
    }

    private fun setupCustomKeyboard() {
        // 与 activity_weight_converter.xml 键盘 ID 一致：7 8 9 / 4 5 6 / 1 2 3 / . 0
        val numberAndDotIds = intArrayOf(
            R.id.btnUnit7, R.id.btnUnit8, R.id.btnUnit9,
            R.id.btnUnit4, R.id.btnUnit5, R.id.btnUnit6,
            R.id.btnUnit1, R.id.btnUnit2, R.id.btnUnit3,
            R.id.btnUnitDot, R.id.btnUnit0
        )

        val keyboardListener = View.OnClickListener { v ->
            val btn = v as? Button ?: return@OnClickListener
            val input = btn.text.toString()
            val currentText = etWeightFrom.text.toString()

            if (input == ".") {
                if (currentText.contains(".")) return@OnClickListener
                if (currentText.isEmpty()) {
                    etWeightFrom.setText("0.")
                    etWeightFrom.setSelection(etWeightFrom.text?.length ?: 0)
                    return@OnClickListener
                }
            }

            if (currentText == "0" && input != ".") {
                etWeightFrom.setText(input)
            } else {
                etWeightFrom.append(input)
            }
        }

        for (id in numberAndDotIds) {
            findViewById<Button>(id).setOnClickListener(keyboardListener)
        }

        findViewById<Button>(R.id.btnUnitDEL).setOnClickListener {
            val currentText = etWeightFrom.text.toString()
            if (currentText.isNotEmpty()) {
                etWeightFrom.setText(currentText.substring(0, currentText.length - 1))
            }
        }

        findViewById<Button>(R.id.btnUnitDEL).setOnLongClickListener {
            etWeightFrom.setText("")
            true
        }
    }

    private fun setupListeners() {
        etWeightFrom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateConversion()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerWeightFrom.onItemSelectedListener = spinnerListener
        spinnerWeightTo.onItemSelectedListener = spinnerListener

        btnSwapWeight.setOnClickListener {
            val fromPos = spinnerWeightFrom.selectedItemPosition
            val toPos = spinnerWeightTo.selectedItemPosition
            spinnerWeightFrom.setSelection(toPos)
            spinnerWeightTo.setSelection(fromPos)
        }

        // 1 kg = 1000 g
        findViewById<Button>(R.id.btnWeight1kgToG).setOnClickListener {
            spinnerWeightFrom.setSelection(2) // kg
            spinnerWeightTo.setSelection(1)  // g
            etWeightFrom.setText("1")
        }

        // 1 lb ≈ 0.45 kg
        findViewById<Button>(R.id.btnWeight1lbToKg).setOnClickListener {
            spinnerWeightFrom.setSelection(4) // lb
            spinnerWeightTo.setSelection(2)  // kg
            etWeightFrom.setText("1")
        }

        // 1 oz ≈ 28.35 g
        findViewById<Button>(R.id.btnWeight1ozToG).setOnClickListener {
            spinnerWeightFrom.setSelection(5) // oz
            spinnerWeightTo.setSelection(1)  // g
            etWeightFrom.setText("1")
        }
    }

    private fun calculateConversion() {
        val inputStr = etWeightFrom.text.toString().trim()

        if (inputStr.isEmpty() || inputStr == ".") {
            tvWeightTo.text = "0"
            return
        }

        try {
            val inputValue = inputStr.toDouble()
            val fromIndex = spinnerWeightFrom.selectedItemPosition
            val toIndex = spinnerWeightTo.selectedItemPosition

            val valueInKg = inputValue * factors[fromIndex]
            val resultValue = valueInKg / factors[toIndex]

            val df = DecimalFormat("0.######")
            tvWeightTo.text = df.format(resultValue)
        } catch (e: NumberFormatException) {
            tvWeightTo.text = "格式错误"
        }
    }
}
