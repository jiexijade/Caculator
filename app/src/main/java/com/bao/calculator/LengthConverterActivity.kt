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

class LengthConverterActivity : AppCompatActivity() {

    private lateinit var etLengthFrom: EditText
    private lateinit var tvLengthTo: TextView
    private lateinit var spinnerLengthFrom: Spinner
    private lateinit var spinnerLengthTo: Spinner
    private lateinit var btnSwapLength: ImageButton

    // 长度单位列表（与 DESIGN_Activities 一致：m, km, cm, mm, inch, ft 等）
    private val units = arrayOf(
        "毫米 (mm)", "厘米 (cm)", "分米 (dm)", "米 (m)", "千米 (km)",
        "英寸 (in)", "英尺 (ft)", "码 (yd)", "英里 (mi)"
    )

    // 各单位相对于「米」的转换比例（基础单位 m = 1.0）
    private val factors = doubleArrayOf(
        0.001, 0.01, 0.1, 1.0, 1000.0,
        0.0254, 0.3048, 0.9144, 1609.344
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_length_converter)

        findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = "长度换算"
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
        etLengthFrom = findViewById(R.id.etLengthFrom)
        tvLengthTo = findViewById(R.id.tvLengthTo)
        spinnerLengthFrom = findViewById(R.id.spinnerLengthFrom)
        spinnerLengthTo = findViewById(R.id.spinnerLengthTo)
        btnSwapLength = findViewById(R.id.btnSwapLength)

        etLengthFrom.showSoftInputOnFocus = false
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerLengthFrom.adapter = adapter
        spinnerLengthTo.adapter = adapter

        spinnerLengthFrom.setSelection(3) // 米
        spinnerLengthTo.setSelection(1)  // 厘米
    }

    private fun setupCustomKeyboard() {
        // 与 activity_length_converter.xml 中键盘 ID 一致：7 8 9 / 4 5 6 / 1 2 3 / . 0 DEL
        val numberAndDotIds = intArrayOf(
            R.id.btnUnit7, R.id.btnUnit8, R.id.btnUnit9,
            R.id.btnUnit4, R.id.btnUnit5, R.id.btnUnit6,
            R.id.btnUnit1, R.id.btnUnit2, R.id.btnUnit3,
            R.id.btnUnitDot, R.id.btnUnit0
        )

        val keyboardListener = View.OnClickListener { v ->
            val btn = v as? Button ?: return@OnClickListener
            val input = btn.text.toString()
            val currentText = etLengthFrom.text.toString()

            if (input == ".") {
                if (currentText.contains(".")) return@OnClickListener
                if (currentText.isEmpty()) {
                    etLengthFrom.setText("0.")
                    etLengthFrom.setSelection(etLengthFrom.text?.length ?: 0)
                    return@OnClickListener
                }
            }

            if (currentText == "0" && input != ".") {
                etLengthFrom.setText(input)
            } else {
                etLengthFrom.append(input)
            }
        }

        for (id in numberAndDotIds) {
            findViewById<Button>(id).setOnClickListener(keyboardListener)
        }

        findViewById<Button>(R.id.btnUnitDEL).setOnClickListener {
            val currentText = etLengthFrom.text.toString()
            if (currentText.isNotEmpty()) {
                etLengthFrom.setText(currentText.substring(0, currentText.length - 1))
            }
        }

        findViewById<Button>(R.id.btnUnitDEL).setOnLongClickListener {
            etLengthFrom.setText("")
            true
        }
    }

    private fun setupListeners() {
        etLengthFrom.addTextChangedListener(object : TextWatcher {
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

        spinnerLengthFrom.onItemSelectedListener = spinnerListener
        spinnerLengthTo.onItemSelectedListener = spinnerListener

        btnSwapLength.setOnClickListener {
            val fromPos = spinnerLengthFrom.selectedItemPosition
            val toPos = spinnerLengthTo.selectedItemPosition
            spinnerLengthFrom.setSelection(toPos)
            spinnerLengthTo.setSelection(fromPos)
        }

        findViewById<Button>(R.id.btnLength1mToCm).setOnClickListener {
            spinnerLengthFrom.setSelection(3)
            spinnerLengthTo.setSelection(1)
            etLengthFrom.setText("1")
        }

        findViewById<Button>(R.id.btnLength1kmToM).setOnClickListener {
            spinnerLengthFrom.setSelection(4)
            spinnerLengthTo.setSelection(3)
            etLengthFrom.setText("1")
        }

        findViewById<Button>(R.id.btnLength1inToCm).setOnClickListener {
            spinnerLengthFrom.setSelection(5)
            spinnerLengthTo.setSelection(1)
            etLengthFrom.setText("1")
        }
    }

    private fun calculateConversion() {
        val inputStr = etLengthFrom.text.toString().trim()

        if (inputStr.isEmpty() || inputStr == ".") {
            tvLengthTo.text = "0"
            return
        }

        try {
            val inputValue = inputStr.toDouble()
            val fromIndex = spinnerLengthFrom.selectedItemPosition
            val toIndex = spinnerLengthTo.selectedItemPosition

            val valueInMeters = inputValue * factors[fromIndex]
            val resultValue = valueInMeters / factors[toIndex]

            val df = DecimalFormat("0.######")
            tvLengthTo.text = df.format(resultValue)
        } catch (e: NumberFormatException) {
            tvLengthTo.text = "格式错误"
        }
    }
}
