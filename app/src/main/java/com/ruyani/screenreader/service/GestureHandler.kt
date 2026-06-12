package com.ruyani.screenreader.service

import android.accessibilityservice.AccessibilityService

class GestureHandler(private val service: RuyaniAccessibilityService) {

    /**
     * Menangani gestur yang diterima dari layanan aksesibilitas.
     * Memetakan gestur ke tindakan navigasi yang sesuai.
     *
     * Pemetaan gestur:
     * - Geser kanan -> Navigasi ke elemen berikutnya
     * - Geser kiri -> Navigasi ke elemen sebelumnya
     * - Geser atas -> Ganti mode navigasi (naik)
     * - Geser bawah -> Ganti mode navigasi (turun)
     * - Geser atas-bawah -> Gulir ke bawah
     * - Geser bawah-atas -> Gulir ke atas
     * - Geser kanan-kiri -> Aktifkan elemen saat ini (klik)
     * - Geser kiri-kanan -> Kembali (tombol back)
     *
     * @param gestureId ID gestur dari AccessibilityService
     * @return true jika gestur berhasil ditangani, false jika tidak dikenali
     */
    fun handleGesture(gestureId: Int): Boolean {
        return when (gestureId) {
            AccessibilityService.GESTURE_SWIPE_RIGHT -> {
                service.navigateNext()
                true
            }

            AccessibilityService.GESTURE_SWIPE_LEFT -> {
                service.navigatePrevious()
                true
            }

            AccessibilityService.GESTURE_SWIPE_UP -> {
                service.changeNavigationMode(1)
                true
            }

            AccessibilityService.GESTURE_SWIPE_DOWN -> {
                service.changeNavigationMode(-1)
                true
            }

            AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN -> {
                service.scrollForward()
                true
            }

            AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP -> {
                service.scrollBackward()
                true
            }

            AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT -> {
                service.activateCurrentNode()
                true
            }

            AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT -> {
                service.goBack()
                true
            }

            else -> false
        }
    }
}
