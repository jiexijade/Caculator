package com.bao.calculator

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/**
 * 设置项 Fragment：主题、默认计算模式、小数位数、关于。
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        updateAboutSummary()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null) return
        when (key) {
            "theme" -> {
                val theme = sharedPreferences?.getString(key, "follow_system") ?: "follow_system"
                val mode = when (theme) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
            "decimal_places", "default_calc_mode" -> {
                // 仅持久化，主计算器读取即可
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == "about") {
            val versionName = getAppVersionName()
            startActivity(Intent(requireContext(), AboutActivity::class.java).apply {
                putExtra(AboutActivity.EXTRA_VERSION_NAME, versionName)
            })
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun updateAboutSummary() {
        findPreference<Preference>("about")?.let { about ->
            about.summary = getString(R.string.settings_about_summary, getAppVersionName())
        }
    }

    private fun getAppVersionName(): String = try {
        @Suppress("DEPRECATION")
        requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }
}
