package com.ruyani.screenreader

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.ruyani.screenreader.databinding.ActivitySettingsBinding
import com.ruyani.screenreader.service.RuyaniAccessibilityService
import com.ruyani.screenreader.tts.TTSManager
import com.ruyani.screenreader.utils.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var ttsManager: TTSManager

    private var availableEngines: List<TextToSpeech.EngineInfo> = emptyList()
    private var isInitializing = true

    companion object {
        private const val RATE_MIN = 0.25f
        private const val RATE_MAX = 3.0f
        private const val RATE_STEP = 0.25f
        private const val PITCH_MIN = 0.25f
        private const val PITCH_MAX = 3.0f
        private const val PITCH_STEP = 0.25f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PrefsManager(this)
        ttsManager = TTSManager(this)
        ttsManager.initialize()

        setupToolbar()
        setupTtsEngineSpinner()
        setupSpeechRateSeekBar()
        setupSpeechPitchSeekBar()
        setupTestSpeechButton()
        setupToggles()

        // Allow initial values to settle before tracking user changes
        binding.root.post {
            isInitializing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ──────────────────────────────────────────────
    // Toolbar
    // ──────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.settings_title)
        }
    }

    // ──────────────────────────────────────────────
    // TTS Engine Spinner
    // ──────────────────────────────────────────────

    private fun setupTtsEngineSpinner() {
        availableEngines = ttsManager.getAvailableEngines()

        val engineLabels = availableEngines.map { it.label.ifEmpty { it.name } }.toMutableList()
        if (engineLabels.isEmpty()) {
            engineLabels.add(getString(R.string.default_engine))
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            engineLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerTtsEngine.adapter = adapter

        // Select current engine
        val currentEngine = prefsManager.ttsEngine
        if (currentEngine != null) {
            val index = availableEngines.indexOfFirst { it.name == currentEngine }
            if (index >= 0) {
                binding.spinnerTtsEngine.setSelection(index)
            }
        }

        binding.spinnerTtsEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializing) return
                if (position < availableEngines.size) {
                    val selectedEngine = availableEngines[position]
                    prefsManager.ttsEngine = selectedEngine.name
                    ttsManager.setEngine(selectedEngine.name)
                    updateLiveServiceTts()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ──────────────────────────────────────────────
    // Speech Rate SeekBar
    // ──────────────────────────────────────────────

    private fun setupSpeechRateSeekBar() {
        val totalSteps = ((RATE_MAX - RATE_MIN) / RATE_STEP).toInt()
        binding.seekSpeechRate.max = totalSteps

        // Set current value
        val currentRate = prefsManager.speechRate
        val currentStep = ((currentRate - RATE_MIN) / RATE_STEP).toInt().coerceIn(0, totalSteps)
        binding.seekSpeechRate.progress = currentStep
        binding.tvSpeechRateValue.text = formatRate(currentRate)

        binding.seekSpeechRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = RATE_MIN + (progress * RATE_STEP)
                binding.tvSpeechRateValue.text = formatRate(rate)
                if (fromUser) {
                    prefsManager.speechRate = rate
                    ttsManager.setSpeechRate(rate)
                    updateLiveServiceTts()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ──────────────────────────────────────────────
    // Speech Pitch SeekBar
    // ──────────────────────────────────────────────

    private fun setupSpeechPitchSeekBar() {
        val totalSteps = ((PITCH_MAX - PITCH_MIN) / PITCH_STEP).toInt()
        binding.seekSpeechPitch.max = totalSteps

        // Set current value
        val currentPitch = prefsManager.speechPitch
        val currentStep = ((currentPitch - PITCH_MIN) / PITCH_STEP).toInt().coerceIn(0, totalSteps)
        binding.seekSpeechPitch.progress = currentStep
        binding.tvSpeechPitchValue.text = formatRate(currentPitch)

        binding.seekSpeechPitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pitch = PITCH_MIN + (progress * PITCH_STEP)
                binding.tvSpeechPitchValue.text = formatRate(pitch)
                if (fromUser) {
                    prefsManager.speechPitch = pitch
                    ttsManager.setPitch(pitch)
                    updateLiveServiceTts()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ──────────────────────────────────────────────
    // Test Speech
    // ──────────────────────────────────────────────

    private fun setupTestSpeechButton() {
        binding.btnTestSpeech.setOnClickListener {
            ttsManager.speak(getString(R.string.test_speech_text), flush = true)
        }
    }

    // ──────────────────────────────────────────────
    // Toggle Switches
    // ──────────────────────────────────────────────

    private fun setupToggles() {
        // Read Notifications
        binding.switchNotifications.isChecked = prefsManager.readNotifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefsManager.readNotifications = isChecked
            }
        }

        // Vibration
        binding.switchVibration.isChecked = prefsManager.vibrationEnabled
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefsManager.vibrationEnabled = isChecked
            }
        }

        // Focus Highlight
        binding.switchHighlight.isChecked = prefsManager.highlightEnabled
        binding.switchHighlight.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) {
                prefsManager.highlightEnabled = isChecked
            }
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private fun formatRate(value: Float): String {
        return String.format("%.2f×", value)
    }

    /**
     * If the accessibility service is currently running, push the updated
     * TTS configuration to the live service instance so changes take
     * effect immediately without restarting the service.
     */
    private fun updateLiveServiceTts() {
        RuyaniAccessibilityService.instance?.ttsManager?.let { liveTts ->
            liveTts.setSpeechRate(prefsManager.speechRate)
            liveTts.setPitch(prefsManager.speechPitch)
            prefsManager.ttsEngine?.let { engine ->
                liveTts.setEngine(engine)
            }
        }
    }
}
