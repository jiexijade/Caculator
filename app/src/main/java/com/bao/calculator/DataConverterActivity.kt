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

class DataConverterActivity : AppCompatActivity() {

    private lateinit var etLengthFrom: EditText
    private lateinit var tvLengthTo: TextView
    private lateinit var spinnerLengthFrom: Spinner
    private lateinit var spinnerLengthTo: Spinner
    private lateinit var btnSwapLength: ImageButton

    // 数据单位：比特(十进制)、字节(十进制)、二进制单位
    private val units = arrayOf(
        "比特 (b)", "千比特 (Kb)", "兆比特 (Mb)", "吉比特 (Gb)", "太比特 (Tb)",
        "字节 (B)", "千字节 (KB)", "兆字节 (MB)", "吉字节 (GB)", "太字节 (TB)",
        "KiB", "MiB", "GiB", "TiB"
    )

    // 各单位相对于「比特(b)」的转换比例（1 字节 = 8 比特）
    // 十进制：K=1000；二进制：Ki=1024
    private val factorsBits: DoubleArray by lazy {
        val k = 1000.0
        val ki = 1024.0
        doubleArrayOf(
            1.0, k, k * k, k * k * k, k * k * k * k,           // b, Kb, Mb, Gb, Tb
            8.0, 8 * k, 8 * k * k, 8 * k * k * k, 8 * k * k * k * k,  // B, KB, MB, GB, TB
            8 * ki, 8 * ki * ki, 8 * ki * ki * ki, 8 * ki * ki * ki * ki   // KiB, MiB, GiB, TiB
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_converter)

        findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = "数据换算"
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
        spinnerLengthFrom.setSelection(7)  // MB
        spinnerLengthTo.setSelection(2)    // Mb
    }

    private fun setupCustomKeyboard() {
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
            val s = etLengthFrom.text.toString()
            if (s.isNotEmpty()) etLengthFrom.setText(s.dropLast(1))
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
        // 常用换算快捷按钮
        findViewById<Button>(R.id.btnData1MBToMb).setOnClickListener {
            spinnerLengthFrom.setSelection(7)   // MB
            spinnerLengthTo.setSelection(2)    // Mb
            etLengthFrom.setText("1")
        }
        findViewById<Button>(R.id.btnData1GBToMB).setOnClickListener {
            spinnerLengthFrom.setSelection(12) // GiB
            spinnerLengthTo.setSelection(11)   // MiB -> 1 GiB = 1024 MiB
            etLengthFrom.setText("1")
        }
        findViewById<Button>(R.id.btnData1GiBToMiB).setOnClickListener {
            spinnerLengthFrom.setSelection(12) // GiB
            spinnerLengthTo.setSelection(11)   // MiB
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
            val valueInBits = inputValue * factorsBits[fromIndex]
            val resultValue = valueInBits / factorsBits[toIndex]
            val df = DecimalFormat("0.######")
            tvLengthTo.text = df.format(resultValue)
        } catch (e: NumberFormatException) {
            tvLengthTo.text = "格式错误"
        }
    }
}
