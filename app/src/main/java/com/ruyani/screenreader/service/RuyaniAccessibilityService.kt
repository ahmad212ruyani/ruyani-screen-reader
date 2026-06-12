package com.ruyani.screenreader.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ruyani.screenreader.overlay.FocusOverlayView
import com.ruyani.screenreader.tts.TTSManager
import com.ruyani.screenreader.utils.PrefsManager

class RuyaniAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RuyaniService"
        var instance: RuyaniAccessibilityService? = null
            private set
        var isRunning: Boolean = false
            private set
    }

    lateinit var ttsManager: TTSManager
    lateinit var prefsManager: PrefsManager
    var focusOverlay: FocusOverlayView? = null
    lateinit var nodeNavigator: NodeNavigator
    lateinit var gestureHandler: GestureHandler
    var currentFocusedNode: AccessibilityNodeInfo? = null

    private val navigationModeNames = arrayOf(
        "Semua elemen",
        "Judul",
        "Tautan",
        "Kontrol"
    )

    /**
     * Dipanggil saat layanan aksesibilitas terhubung dan siap digunakan.
     * Menginisialisasi semua komponen: TTS, preferensi, overlay, navigator, dan penangan gestur.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()

        instance = this
        isRunning = true

        // Inisialisasi komponen
        prefsManager = PrefsManager(this)
        ttsManager = TTSManager(this)
        ttsManager.initialize()
        nodeNavigator = NodeNavigator()
        gestureHandler = GestureHandler(this)

        // Inisialisasi overlay sorotan fokus
        if (prefsManager.highlightEnabled) {
            focusOverlay = FocusOverlayView(this)
            focusOverlay?.show()
        }

        // Konfigurasi layanan
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            flags = flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES or
                    AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 100
        }

        // Umumkan bahwa layanan sudah aktif
        ttsManager.speak("Ruyani Screen Reader aktif")
        vibrateShort()

        Log.i(TAG, "Ruyani Screen Reader berhasil terhubung dan aktif")
    }

    /**
     * Menangani event aksesibilitas yang diterima dari sistem.
     * Memproses berbagai jenis event seperti fokus, perubahan jendela, notifikasi, dan klik.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> handleFocusEvent(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowStateChanged(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleContentChanged(event)
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> handleNotification(event)
            AccessibilityEvent.TYPE_VIEW_CLICKED -> handleViewClicked(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextChanged(event)
            AccessibilityEvent.TYPE_VIEW_SELECTED -> handleViewSelected(event)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleViewScrolled(event)
        }
    }

    /**
     * Menangani event fokus pada elemen.
     * Membacakan deskripsi elemen yang mendapat fokus dan memperbarui overlay sorotan.
     */
    private fun handleFocusEvent(event: AccessibilityEvent) {
        val source = event.source ?: return

        currentFocusedNode = source
        val description = nodeNavigator.getNodeDescription(source)

        if (description.isNotBlank()) {
            ttsManager.speak(description)
        }

        updateOverlayForNode(source)
        vibrateShort()
    }

    /**
     * Menangani perubahan status jendela (berpindah aplikasi, dialog muncul, dll.).
     * Mengumumkan nama jendela/aktivitas baru.
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val windowTitle = event.text?.joinToString(", ")?.trim()
        val packageName = event.packageName?.toString() ?: ""
        val className = event.className?.toString()?.substringAfterLast('.') ?: ""

        val announcement = when {
            !windowTitle.isNullOrBlank() -> windowTitle
            className.isNotBlank() -> className
            else -> return
        }

        // Hindari membacakan nama paket yang tidak berguna untuk pengguna
        if (announcement.contains('.') && announcement.length > 30) return

        ttsManager.speak(announcement)
        Log.d(TAG, "Jendela berubah: $announcement (paket: $packageName)")
    }

    /**
     * Menangani perubahan konten jendela.
     * Memperbarui posisi overlay jika node yang sedang difokuskan berubah posisinya.
     */
    private fun handleContentChanged(event: AccessibilityEvent) {
        currentFocusedNode?.let { node ->
            try {
                node.refresh()
                updateOverlayForNode(node)
            } catch (e: Exception) {
                Log.d(TAG, "Gagal memperbarui node setelah perubahan konten: ${e.message}")
            }
        }
    }

    /**
     * Menangani notifikasi yang masuk.
     * Membacakan isi notifikasi jika pengaturan mengizinkan.
     */
    private fun handleNotification(event: AccessibilityEvent) {
        if (!prefsManager.readNotifications) return

        val notificationText = event.text?.joinToString(". ")?.trim()
        if (!notificationText.isNullOrBlank()) {
            val appName = getAppLabel(event.packageName?.toString())
            val announcement = if (appName != null) {
                "Notifikasi dari $appName: $notificationText"
            } else {
                "Notifikasi: $notificationText"
            }
            ttsManager.speak(announcement, flush = false)
        }
    }

    /**
     * Menangani event klik pada elemen.
     * Memberikan umpan balik bahwa elemen telah diaktifkan.
     */
    private fun handleViewClicked(event: AccessibilityEvent) {
        val source = event.source ?: return
        val description = nodeNavigator.getNodeDescription(source)
        if (description.isNotBlank()) {
            vibrateShort()
        }
    }

    /**
     * Menangani perubahan teks pada kolom input.
     * Membacakan karakter yang baru diketik.
     */
    private fun handleTextChanged(event: AccessibilityEvent) {
        val addedText = event.beforeText?.let { before ->
            val current = event.text?.firstOrNull()?.toString() ?: ""
            if (current.length > before.length) {
                current.substring(before.length)
            } else if (current.length < before.length) {
                "dihapus"
            } else {
                null
            }
        }

        if (!addedText.isNullOrBlank()) {
            ttsManager.speak(addedText, flush = true)
        }
    }

    /**
     * Menangani event seleksi pada elemen.
     */
    private fun handleViewSelected(event: AccessibilityEvent) {
        val source = event.source ?: return
        val description = nodeNavigator.getNodeDescription(source)
        if (description.isNotBlank()) {
            ttsManager.speak("Dipilih: $description")
        }
    }

    /**
     * Menangani event gulir pada elemen.
     */
    private fun handleViewScrolled(event: AccessibilityEvent) {
        val fromIndex = event.fromIndex
        val toIndex = event.toIndex
        val itemCount = event.itemCount

        if (itemCount > 0 && fromIndex >= 0) {
            val position = fromIndex + 1
            ttsManager.speak("Posisi $position dari $itemCount", flush = true)
        }
    }

    /**
     * Mendelegasikan penanganan gestur ke GestureHandler.
     */
    override fun onGesture(gestureId: Int): Boolean {
        return gestureHandler.handleGesture(gestureId) || super.onGesture(gestureId)
    }

    /**
     * Navigasi ke elemen berikutnya dalam pohon aksesibilitas.
     * Menggunakan mode navigasi yang sedang aktif untuk menyaring elemen.
     */
    fun navigateNext() {
        val root = rootInActiveWindow ?: return
        val nextNode = nodeNavigator.findNext(root, currentFocusedNode, prefsManager.navigationMode)

        if (nextNode != null) {
            focusNode(nextNode)
        } else {
            ttsManager.speak("Tidak ada elemen berikutnya")
            vibrateDouble()
        }
    }

    /**
     * Navigasi ke elemen sebelumnya dalam pohon aksesibilitas.
     */
    fun navigatePrevious() {
        val root = rootInActiveWindow ?: return
        val prevNode = nodeNavigator.findPrevious(root, currentFocusedNode, prefsManager.navigationMode)

        if (prevNode != null) {
            focusNode(prevNode)
        } else {
            ttsManager.speak("Tidak ada elemen sebelumnya")
            vibrateDouble()
        }
    }

    /**
     * Memfokuskan node dan membacakan deskripsinya.
     * Memperbarui overlay sorotan ke posisi node baru.
     */
    private fun focusNode(node: AccessibilityNodeInfo) {
        currentFocusedNode = node

        // Mencoba memberikan fokus aksesibilitas pada node
        node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)

        // Membacakan deskripsi node
        val description = nodeNavigator.getNodeDescription(node)
        if (description.isNotBlank()) {
            ttsManager.speak(description)
        }

        // Memperbarui overlay sorotan
        updateOverlayForNode(node)
        vibrateShort()
    }

    /**
     * Memperbarui posisi overlay sorotan untuk node yang diberikan.
     */
    private fun updateOverlayForNode(node: AccessibilityNodeInfo) {
        if (!prefsManager.highlightEnabled) return

        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (rect.width() > 0 && rect.height() > 0) {
            focusOverlay?.updatePosition(rect)
        }
    }

    /**
     * Mengaktifkan (mengklik) elemen yang sedang difokuskan.
     */
    fun activateCurrentNode() {
        val node = currentFocusedNode
        if (node != null) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked) {
                // Mencoba mengklik parent jika node itu sendiri tidak bisa diklik
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        break
                    }
                    parent = parent.parent
                }
            }
            vibrateShort()
        } else {
            ttsManager.speak("Tidak ada elemen yang dipilih")
        }
    }

    /**
     * Menggulir ke depan (ke bawah) pada elemen yang dapat digulir.
     * Pertama mencoba pada node saat ini, lalu mencari parent yang dapat digulir.
     */
    fun scrollForward() {
        val scrollPerformed = tryScrollOnNode(currentFocusedNode, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

        if (!scrollPerformed) {
            // Fallback ke global action
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            ttsManager.speak("Gulir ke bawah")
        }
    }

    /**
     * Menggulir ke belakang (ke atas) pada elemen yang dapat digulir.
     */
    fun scrollBackward() {
        val scrollPerformed = tryScrollOnNode(currentFocusedNode, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)

        if (!scrollPerformed) {
            ttsManager.speak("Gulir ke atas")
        }
    }

    /**
     * Mencoba melakukan aksi gulir pada node atau parent yang dapat digulir.
     * @return true jika berhasil menggulir, false jika tidak ada elemen yang dapat digulir
     */
    private fun tryScrollOnNode(node: AccessibilityNodeInfo?, scrollAction: Int): Boolean {
        if (node == null) return false

        // Coba gulir pada node saat ini
        if (node.isScrollable) {
            return node.performAction(scrollAction)
        }

        // Cari parent yang dapat digulir
        var parent = node.parent
        while (parent != null) {
            if (parent.isScrollable) {
                return parent.performAction(scrollAction)
            }
            parent = parent.parent
        }

        return false
    }

    /**
     * Mengubah mode navigasi (berputar melalui: Semua, Judul, Tautan, Kontrol).
     * @param direction 1 untuk naik (mode berikutnya), -1 untuk turun (mode sebelumnya)
     */
    fun changeNavigationMode(direction: Int) {
        val totalModes = navigationModeNames.size
        var newMode = prefsManager.navigationMode + direction

        // Wrap around
        if (newMode >= totalModes) newMode = 0
        if (newMode < 0) newMode = totalModes - 1

        prefsManager.navigationMode = newMode

        val modeName = navigationModeNames[newMode]
        ttsManager.speak("Mode navigasi: $modeName")
        vibrateShort()

        Log.d(TAG, "Mode navigasi berubah ke: $modeName (mode $newMode)")
    }

    /**
     * Kembali ke layar sebelumnya (tombol Back).
     */
    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        ttsManager.speak("Kembali")
    }

    /**
     * Mendapatkan label aplikasi dari nama paket.
     * @return Label aplikasi atau null jika tidak ditemukan
     */
    private fun getAppLabel(packageName: String?): String? {
        if (packageName == null) return null
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Memberikan umpan balik getaran singkat.
     */
    private fun vibrateShort() {
        if (!prefsManager.vibrationEnabled) return
        performVibration(30L)
    }

    /**
     * Memberikan umpan balik getaran ganda (untuk peringatan seperti akhir daftar).
     */
    private fun vibrateDouble() {
        if (!prefsManager.vibrationEnabled) return
        performVibration(longArrayOf(0, 40, 80, 40))
    }

    /**
     * Melakukan getaran dengan durasi tertentu.
     */
    private fun performVibration(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Gagal memberikan umpan balik getaran: ${e.message}")
        }
    }

    /**
     * Melakukan pola getaran.
     */
    private fun performVibration(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Gagal memberikan umpan balik getaran: ${e.message}")
        }
    }

    /**
     * Dipanggil saat sistem meminta layanan untuk berhenti sementara.
     */
    override fun onInterrupt() {
        ttsManager.stop()
        Log.d(TAG, "Layanan diinterupsi")
    }

    /**
     * Dipanggil saat layanan dihancurkan.
     * Membersihkan semua sumber daya: TTS, overlay, dan referensi statis.
     */
    override fun onDestroy() {
        ttsManager.speak("Ruyani Screen Reader dinonaktifkan")
        ttsManager.shutdown()
        focusOverlay?.remove()
        focusOverlay = null
        currentFocusedNode = null
        isRunning = false
        instance = null

        Log.i(TAG, "Ruyani Screen Reader dinonaktifkan dan dihancurkan")
        super.onDestroy()
    }
}
