package com.bao.calculator

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

/**
 * 关于页：显示应用名称与版本，由设置页「关于」进入。
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: "1.0"
        findViewById<TextView>(R.id.tvVersion).text = getString(R.string.settings_about_summary, versionName)
    }

    companion object {
        const val EXTRA_VERSION_NAME = "version_name"
    }
}
