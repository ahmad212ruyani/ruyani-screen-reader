package com.ruyani.screenreader.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.ruyani.screenreader.utils.PrefsManager
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
    }

    private var tts: TextToSpeech? = null
    private val prefsManager = PrefsManager(context)
    private val utteranceCounter = AtomicInteger(0)

    var isInitialized: Boolean = false
        private set

    /**
     * Menginisialisasi Text-to-Speech dengan mesin yang tersimpan di preferensi
     * atau menggunakan mesin default sistem jika belum ada yang disimpan.
     */
    fun initialize() {
        val savedEngine = prefsManager.ttsEngine
        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureTts()
                Log.i(TAG, "TTS berhasil diinisialisasi")
            } else {
                isInitialized = false
                Log.e(TAG, "Gagal menginisialisasi TTS, status: $status")
            }
        }

        tts = if (savedEngine != null) {
            TextToSpeech(context, initListener, savedEngine)
        } else {
            TextToSpeech(context, initListener)
        }
    }

    /**
     * Mengkonfigurasi pengaturan TTS seperti bahasa, kecepatan bicara, dan nada.
     */
    private fun configureTts() {
        tts?.let { engine ->
            // Mencoba menggunakan Bahasa Indonesia, fallback ke default jika tidak tersedia
            val indonesian = Locale("id", "ID")
            val result = engine.setLanguage(indonesian)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Bahasa Indonesia tidak tersedia, menggunakan bahasa default")
                engine.setLanguage(Locale.getDefault())
            }

            engine.setSpeechRate(prefsManager.speechRate)
            engine.setPitch(prefsManager.speechPitch)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "Mulai berbicara: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "Selesai berbicara: $utteranceId")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "Kesalahan saat berbicara: $utteranceId")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "Kesalahan saat berbicara: $utteranceId, kode: $errorCode")
                }
            })
        }
    }

    /**
     * Mengucapkan teks yang diberikan melalui TTS.
     * @param text Teks yang akan diucapkan
     * @param flush Jika true, menghentikan ucapan saat ini dan langsung mengucapkan teks baru.
     *              Jika false, menambahkan ke antrian ucapan.
     */
    fun speak(text: String, flush: Boolean = true) {
        if (!isInitialized) {
            Log.w(TAG, "TTS belum diinisialisasi, mengabaikan permintaan bicara: $text")
            return
        }

        if (text.isBlank()) {
            return
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "ruyani_utterance_${utteranceCounter.incrementAndGet()}"

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, queueMode, params, utteranceId)
    }

    /**
     * Menghentikan ucapan yang sedang berlangsung.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Mematikan dan melepas sumber daya TTS.
     */
    fun shutdown() {
        isInitialized = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /**
     * Mendapatkan daftar mesin TTS yang tersedia di perangkat.
     * @return Daftar EngineInfo dari mesin yang terinstal
     */
    fun getAvailableEngines(): List<TextToSpeech.EngineInfo> {
        return tts?.engines ?: emptyList()
    }

    /**
     * Mengganti mesin TTS yang digunakan.
     * Mesin saat ini akan dimatikan dan diinisialisasi ulang dengan mesin baru.
     * @param packageName Nama paket mesin TTS yang akan digunakan
     */
    fun setEngine(packageName: String) {
        prefsManager.ttsEngine = packageName
        shutdown()

        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureTts()
                Log.i(TAG, "Mesin TTS berhasil diganti ke: $packageName")
            } else {
                isInitialized = false
                Log.e(TAG, "Gagal mengganti mesin TTS ke: $packageName")
            }
        }, packageName)
    }

    /**
     * Mengatur kecepatan bicara TTS.
     * @param rate Kecepatan bicara (1.0 = normal)
     */
    fun setSpeechRate(rate: Float) {
        prefsManager.speechRate = rate
        tts?.setSpeechRate(rate)
    }

    /**
     * Mengatur nada suara TTS.
     * @param pitch Nada suara (1.0 = normal)
     */
    fun setPitch(pitch: Float) {
        prefsManager.speechPitch = pitch
        tts?.setPitch(pitch)
    }
}
