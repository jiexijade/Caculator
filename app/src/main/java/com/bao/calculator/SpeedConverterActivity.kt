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

class SpeedConverterActivity : AppCompatActivity() {

    private lateinit var etSpeedFrom: EditText
    private lateinit var tvSpeedTo: TextView
    private lateinit var spinnerSpeedFrom: Spinner
    private lateinit var spinnerSpeedTo: Spinner
    private lateinit var btnSwapSpeed: ImageButton

    // 速度单位列表（与 DESIGN_Activities 一致：m/s, km/h, mph 等）
    private val units = arrayOf(
        "米/秒 (m/s)",
        "千米/小时 (km/h)",
        "英里/小时 (mph)",
        "节 (knot)",
        "马赫 (Mach)",
        "英尺/秒 (ft/s)"
    )

    // 各单位相对于「米/秒(m/s)」的转换比例
    private val factors = doubleArrayOf(
        1.0,                    // 米/秒 (m/s)
        1.0 / 3.6,              // 千米/小时 (km/h) -> 1 km/h = 1/3.6 m/s
        0.44704,                // 英里/小时 (mph)
        1852.0 / 3600.0,        // 节 (knot) -> 1 knot = 1852m/3600s
        340.3,                  // 马赫 (Mach)，标准大气压约 340.3 m/s
        0.3048                  // 英尺/秒 (ft/s)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speed_converter)

        findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = "速度换算"
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
        // 布局中复用与长度换算相同的 ID：etLengthFrom / tvLengthTo / spinnerLengthFrom / spinnerLengthTo / btnSwapLength
        etSpeedFrom = findViewById(R.id.etLengthFrom)
        tvSpeedTo = findViewById(R.id.tvLengthTo)
        spinnerSpeedFrom = findViewById(R.id.spinnerLengthFrom)
        spinnerSpeedTo = findViewById(R.id.spinnerLengthTo)
        btnSwapSpeed = findViewById(R.id.btnSwapLength)

        etSpeedFrom.showSoftInputOnFocus = false
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerSpeedFrom.adapter = adapter
        spinnerSpeedTo.adapter = adapter

        spinnerSpeedFrom.setSelection(1) // 千米/小时
        spinnerSpeedTo.setSelection(0)  // 米/秒
    }

    private fun setupCustomKeyboard() {
        // 与 activity_speed_converter.xml 键盘 ID 一致：7 8 9 / 4 5 6 / 1 2 3 / . 0
        val numberAndDotIds = intArrayOf(
            R.id.btnUnit7, R.id.btnUnit8, R.id.btnUnit9,
            R.id.btnUnit4, R.id.btnUnit5, R.id.btnUnit6,
            R.id.btnUnit1, R.id.btnUnit2, R.id.btnUnit3,
            R.id.btnUnitDot, R.id.btnUnit0
        )

        val keyboardListener = View.OnClickListener { v ->
            val btn = v as? Button ?: return@OnClickListener
            val input = btn.text.toString()
            val currentText = etSpeedFrom.text.toString()

            if (input == ".") {
                if (currentText.contains(".")) return@OnClickListener
                if (currentText.isEmpty()) {
                    etSpeedFrom.setText("0.")
                    etSpeedFrom.setSelection(etSpeedFrom.text?.length ?: 0)
                    return@OnClickListener
                }
            }

            if (currentText == "0" && input != ".") {
                etSpeedFrom.setText(input)
            } else {
                etSpeedFrom.append(input)
            }
        }

        for (id in numberAndDotIds) {
            findViewById<Button>(id).setOnClickListener(keyboardListener)
        }

        findViewById<Button>(R.id.btnUnitDEL).setOnClickListener {
            val currentText = etSpeedFrom.text.toString()
            if (currentText.isNotEmpty()) {
                etSpeedFrom.setText(currentText.substring(0, currentText.length - 1))
            }
        }

        findViewById<Button>(R.id.btnUnitDEL).setOnLongClickListener {
            etSpeedFrom.setText("")
            true
        }
    }

    private fun setupListeners() {
        etSpeedFrom.addTextChangedListener(object : TextWatcher {
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
        spinnerSpeedFrom.onItemSelectedListener = spinnerListener
        spinnerSpeedTo.onItemSelectedListener = spinnerListener

        btnSwapSpeed.setOnClickListener {
            val fromPos = spinnerSpeedFrom.selectedItemPosition
            val toPos = spinnerSpeedTo.selectedItemPosition
            spinnerSpeedFrom.setSelection(toPos)
            spinnerSpeedTo.setSelection(fromPos)
        }

        // 1 m/s = 3.6 km/h
        findViewById<Button>(R.id.btnSpeed1msToKmh).setOnClickListener {
            spinnerSpeedFrom.setSelection(0)
            spinnerSpeedTo.setSelection(1)
            etSpeedFrom.setText("1")
        }

        // 1 km/h ≈ 0.28 m/s
        findViewById<Button>(R.id.btnSpeed1kmhToMs).setOnClickListener {
            spinnerSpeedFrom.setSelection(1)
            spinnerSpeedTo.setSelection(0)
            etSpeedFrom.setText("1")
        }

        // 1 mph → km/h
        findViewById<Button>(R.id.btnSpeed1mphToKmh).setOnClickListener {
            spinnerSpeedFrom.setSelection(2)
            spinnerSpeedTo.setSelection(1)
            etSpeedFrom.setText("1")
        }
    }

    private fun calculateConversion() {
        val inputStr = etSpeedFrom.text.toString().trim()

        if (inputStr.isEmpty() || inputStr == ".") {
            tvSpeedTo.text = "0"
            return
        }

        try {
            val inputValue = inputStr.toDouble()
            val fromIndex = spinnerSpeedFrom.selectedItemPosition
            val toIndex = spinnerSpeedTo.selectedItemPosition

            val valueInMetersPerSecond = inputValue * factors[fromIndex]
            val resultValue = valueInMetersPerSecond / factors[toIndex]

            val df = DecimalFormat("0.######")
            tvSpeedTo.text = df.format(resultValue)
        } catch (e: NumberFormatException) {
            tvSpeedTo.text = "格式错误"
        }
    }
}
