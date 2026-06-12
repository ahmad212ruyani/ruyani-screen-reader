package com.ruyani.screenreader

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ruyani.screenreader.databinding.ActivityMainBinding
import com.ruyani.screenreader.service.RuyaniAccessibilityService
import com.ruyani.screenreader.utils.AccessibilityUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshServiceStatus()
    }

    private fun setupViews() {
        // Display app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: Exception) {
            binding.tvVersion.text = getString(R.string.version_format, "1.0.0")
        }
    }

    private fun setupListeners() {
        binding.btnEnable.setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(this)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun refreshServiceStatus() {
        val isEnabled = AccessibilityUtils.isServiceEnabled(this, RuyaniAccessibilityService::class.java)
        val isRunning = RuyaniAccessibilityService.isRunning

        val isActive = isEnabled && isRunning

        if (isActive) {
            // Service is active
            binding.statusIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.status_active)
            )
            binding.statusText.text = getString(R.string.status_active)
            binding.statusText.setTextColor(
                ContextCompat.getColor(this, R.color.status_active)
            )
            binding.statusDescription.text = getString(R.string.status_description_active)
            binding.btnEnable.text = getString(R.string.status_active)
            binding.btnEnable.isEnabled = false
            binding.btnEnable.alpha = 0.5f
        } else {
            // Service is inactive
            binding.statusIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.status_inactive)
            )
            binding.statusText.text = getString(R.string.status_inactive)
            binding.statusText.setTextColor(
                ContextCompat.getColor(this, R.color.status_inactive)
            )
            binding.statusDescription.text = getString(R.string.status_description_inactive)
            binding.btnEnable.text = getString(R.string.btn_enable)
            binding.btnEnable.isEnabled = true
            binding.btnEnable.alpha = 1.0f
        }
    }
}
