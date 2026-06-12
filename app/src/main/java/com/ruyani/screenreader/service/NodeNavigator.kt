package com.ruyani.screenreader.service

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.ruyani.screenreader.utils.PrefsManager

class NodeNavigator {

    /**
     * Meratakan pohon aksesibilitas menjadi daftar berurutan dari node yang dapat difokuskan.
     * Traversal dilakukan secara depth-first sesuai dengan urutan visual di layar.
     * @param root Node akar dari pohon aksesibilitas
     * @return Daftar node yang dapat difokuskan secara berurutan
     */
    private fun flattenTree(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (root == null) return result
        traverseNode(root, result)
        return result
    }

    /**
     * Melakukan traversal rekursif pada pohon aksesibilitas.
     * Menambahkan setiap node yang relevan (dapat difokuskan atau memiliki teks) ke daftar hasil.
     */
    private fun traverseNode(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (isNodeRelevant(node)) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, result)
            }
        }
    }

    /**
     * Memeriksa apakah sebuah node relevan untuk navigasi.
     * Node dianggap relevan jika memiliki teks/deskripsi dan terlihat di layar.
     */
    private fun isNodeRelevant(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false

        val hasText = !node.text.isNullOrBlank()
        val hasDescription = !node.contentDescription.isNullOrBlank()
        val isClickable = node.isClickable
        val isFocusable = node.isFocusable
        val isCheckable = node.isCheckable
        val isEditable = node.isEditable
        val isScrollable = node.isScrollable

        return hasText || hasDescription || isClickable || isFocusable ||
                isCheckable || isEditable || isScrollable
    }

    /**
     * Menyaring daftar node berdasarkan mode navigasi.
     * @param nodes Daftar node yang akan disaring
     * @param mode Mode navigasi (0=semua, 1=judul, 2=tautan, 3=kontrol)
     * @return Daftar node yang sesuai dengan mode navigasi
     */
    private fun filterByMode(nodes: List<AccessibilityNodeInfo>, mode: Int): List<AccessibilityNodeInfo> {
        return when (mode) {
            PrefsManager.NAV_MODE_ALL -> nodes

            PrefsManager.NAV_MODE_HEADINGS -> nodes.filter { node ->
                isHeading(node)
            }

            PrefsManager.NAV_MODE_LINKS -> nodes.filter { node ->
                isLink(node)
            }

            PrefsManager.NAV_MODE_CONTROLS -> nodes.filter { node ->
                isControl(node)
            }

            else -> nodes
        }
    }

    /**
     * Memeriksa apakah node adalah judul (heading).
     */
    private fun isHeading(node: AccessibilityNodeInfo): Boolean {
        if (node.isHeading) return true

        // Memeriksa properti tambahan untuk kompatibilitas
        val extras = node.extras
        if (extras != null && extras.containsKey("AccessibilityNodeInfo.isHeading")) {
            return extras.getBoolean("AccessibilityNodeInfo.isHeading", false)
        }

        // Memeriksa role description
        val roleDescription = node.extras?.getCharSequence("AccessibilityNodeInfo.roleDescription")
        if (roleDescription != null) {
            val role = roleDescription.toString().lowercase()
            return role.contains("heading") || role.contains("judul")
        }

        // Memeriksa nama kelas untuk kemungkinan heading dari WebView
        val className = node.className?.toString() ?: ""
        return className.contains("Heading", ignoreCase = true)
    }

    /**
     * Memeriksa apakah node adalah tautan (link).
     */
    private fun isLink(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""

        // Memeriksa URL scheme di teks
        val text = node.text?.toString() ?: ""
        val hasUrl = text.startsWith("http://") || text.startsWith("https://")

        // Memeriksa action URL
        val actions = node.actionList
        val hasUrlAction = actions.any { action ->
            action.id == AccessibilityNodeInfo.ACTION_CLICK
        } && className.contains("Link", ignoreCase = true)

        // Memeriksa apakah node berasal dari WebView dan merupakan anchor
        val isWebLink = className.contains("android.webkit", ignoreCase = true) && node.isClickable

        // Memeriksa role description
        val roleDescription = node.extras?.getCharSequence("AccessibilityNodeInfo.roleDescription")
        val hasLinkRole = roleDescription?.toString()?.lowercase()?.let {
            it.contains("link") || it.contains("tautan")
        } ?: false

        return hasUrl || hasUrlAction || isWebLink || hasLinkRole
    }

    /**
     * Memeriksa apakah node adalah kontrol interaktif (tombol, input, checkbox, dll.).
     */
    private fun isControl(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""

        val controlClasses = listOf(
            "android.widget.Button",
            "android.widget.ImageButton",
            "android.widget.EditText",
            "android.widget.CheckBox",
            "android.widget.RadioButton",
            "android.widget.ToggleButton",
            "android.widget.Switch",
            "android.widget.Spinner",
            "android.widget.SeekBar",
            "android.widget.RatingBar",
            "android.widget.CompoundButton",
            "android.widget.AutoCompleteTextView",
            "android.widget.MultiAutoCompleteTextView"
        )

        if (controlClasses.any { className.equals(it, ignoreCase = true) }) {
            return true
        }

        // Kontrol kustom yang bisa diklik dan memiliki teks/deskripsi
        if (node.isClickable && (node.isCheckable || node.isEditable)) {
            return true
        }

        // Memeriksa apakah memiliki kelas turunan dari kontrol standar
        return controlClasses.any { className.startsWith(it, ignoreCase = true) }
    }

    /**
     * Menemukan node berikutnya yang sesuai dengan mode navigasi.
     * @param root Node akar pohon aksesibilitas
     * @param current Node yang saat ini difokuskan
     * @param mode Mode navigasi
     * @return Node berikutnya atau null jika sudah di akhir
     */
    fun findNext(
        root: AccessibilityNodeInfo?,
        current: AccessibilityNodeInfo?,
        mode: Int
    ): AccessibilityNodeInfo? {
        if (root == null) return null

        val allNodes = flattenTree(root)
        val filteredNodes = filterByMode(allNodes, mode)

        if (filteredNodes.isEmpty()) return null

        if (current == null) {
            return filteredNodes.firstOrNull()
        }

        val currentIndex = findCurrentIndex(filteredNodes, current)

        return if (currentIndex == -1) {
            // Node saat ini tidak ditemukan dalam daftar, mulai dari awal
            filteredNodes.firstOrNull()
        } else if (currentIndex < filteredNodes.size - 1) {
            filteredNodes[currentIndex + 1]
        } else {
            // Sudah di akhir daftar, kembali ke awal (wrap around)
            filteredNodes.firstOrNull()
        }
    }

    /**
     * Menemukan node sebelumnya yang sesuai dengan mode navigasi.
     * @param root Node akar pohon aksesibilitas
     * @param current Node yang saat ini difokuskan
     * @param mode Mode navigasi
     * @return Node sebelumnya atau null jika sudah di awal
     */
    fun findPrevious(
        root: AccessibilityNodeInfo?,
        current: AccessibilityNodeInfo?,
        mode: Int
    ): AccessibilityNodeInfo? {
        if (root == null) return null

        val allNodes = flattenTree(root)
        val filteredNodes = filterByMode(allNodes, mode)

        if (filteredNodes.isEmpty()) return null

        if (current == null) {
            return filteredNodes.lastOrNull()
        }

        val currentIndex = findCurrentIndex(filteredNodes, current)

        return if (currentIndex == -1) {
            // Node saat ini tidak ditemukan, mulai dari akhir
            filteredNodes.lastOrNull()
        } else if (currentIndex > 0) {
            filteredNodes[currentIndex - 1]
        } else {
            // Sudah di awal daftar, kembali ke akhir (wrap around)
            filteredNodes.lastOrNull()
        }
    }

    /**
     * Menemukan indeks node saat ini dalam daftar node yang disaring.
     * Pencocokan dilakukan berdasarkan kesamaan hashCode dan properti node.
     */
    private fun findCurrentIndex(
        nodes: List<AccessibilityNodeInfo>,
        current: AccessibilityNodeInfo
    ): Int {
        // Mencoba pencocokan langsung terlebih dahulu
        val directIndex = nodes.indexOfFirst { it == current }
        if (directIndex != -1) return directIndex

        // Fallback: mencocokkan berdasarkan properti
        return nodes.indexOfFirst { node ->
            node.className == current.className &&
                    node.text?.toString() == current.text?.toString() &&
                    node.contentDescription?.toString() == current.contentDescription?.toString() &&
                    node.viewIdResourceName == current.viewIdResourceName
        }
    }

    /**
     * Menghasilkan deskripsi yang dapat dibaca untuk sebuah node aksesibilitas.
     * Deskripsi menggunakan Bahasa Indonesia.
     * @param node Node yang akan dideskripsikan
     * @return String deskripsi lengkap dalam Bahasa Indonesia
     */
    fun getNodeDescription(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()

        // Teks utama dari node
        val contentDescription = node.contentDescription?.toString()?.trim()
        val nodeText = node.text?.toString()?.trim()
        val hintText = node.hintText?.toString()?.trim()

        when {
            !contentDescription.isNullOrBlank() -> parts.add(contentDescription)
            !nodeText.isNullOrBlank() -> parts.add(nodeText)
            !hintText.isNullOrBlank() -> parts.add(hintText)
        }

        // Jenis elemen dalam Bahasa Indonesia
        val roleDescription = getIndonesianRoleDescription(node)
        if (roleDescription.isNotBlank()) {
            parts.add(roleDescription)
        }

        // Status elemen
        val stateDescription = getIndonesianStateDescription(node)
        if (stateDescription.isNotBlank()) {
            parts.add(stateDescription)
        }

        // Jika tidak ada deskripsi sama sekali, berikan label default
        if (parts.isEmpty()) {
            val className = node.className?.toString()?.substringAfterLast('.') ?: ""
            parts.add(getIndonesianClassName(className))
        }

        return parts.joinToString(", ")
    }

    /**
     * Mendapatkan deskripsi peran/jenis elemen dalam Bahasa Indonesia.
     */
    private fun getIndonesianRoleDescription(node: AccessibilityNodeInfo): String {
        val className = node.className?.toString() ?: return ""

        return when {
            className.contains("Button") && className.contains("Image") -> "tombol gambar"
            className.contains("Button") && className.contains("Toggle") -> "tombol alih"
            className.contains("Button") -> "tombol"
            className.contains("EditText") -> "kolom teks"
            className.contains("CheckBox") -> "kotak centang"
            className.contains("RadioButton") -> "tombol radio"
            className.contains("Switch") -> "sakelar"
            className.contains("Spinner") -> "daftar pilihan"
            className.contains("SeekBar") -> "penggeser"
            className.contains("RatingBar") -> "penilaian bintang"
            className.contains("ProgressBar") -> "bilah kemajuan"
            className.contains("ImageView") -> "gambar"
            className.contains("TextView") -> "" // Teks biasa tidak perlu label peran
            className.contains("RecyclerView") || className.contains("ListView") -> "daftar"
            className.contains("ScrollView") -> "area gulir"
            className.contains("WebView") -> "halaman web"
            className.contains("TabWidget") || className.contains("TabLayout") -> "tab"
            className.contains("ViewPager") -> "halaman geser"
            node.isHeading -> "judul"
            node.isScrollable -> "dapat digulir"
            else -> ""
        }
    }

    /**
     * Mendapatkan deskripsi status elemen dalam Bahasa Indonesia.
     */
    private fun getIndonesianStateDescription(node: AccessibilityNodeInfo): String {
        val states = mutableListOf<String>()

        if (node.isCheckable) {
            states.add(if (node.isChecked) "dicentang" else "tidak dicentang")
        }

        if (node.isSelected) {
            states.add("dipilih")
        }

        if (!node.isEnabled) {
            states.add("dinonaktifkan")
        }

        if (node.isPassword) {
            states.add("kata sandi")
        }

        if (node.isEditable && node.text.isNullOrBlank()) {
            states.add("kosong")
        }

        if (node.isExpandable()) {
            states.add(if (node.isExpanded()) "diperluas" else "diciutkan")
        }

        return states.joinToString(", ")
    }

    /**
     * Menerjemahkan nama kelas widget Android ke Bahasa Indonesia.
     */
    private fun getIndonesianClassName(className: String): String {
        return when (className.lowercase()) {
            "button" -> "tombol"
            "imagebutton" -> "tombol gambar"
            "edittext" -> "kolom teks"
            "textview" -> "teks"
            "imageview" -> "gambar"
            "checkbox" -> "kotak centang"
            "radiobutton" -> "tombol radio"
            "switch" -> "sakelar"
            "togglebutton" -> "tombol alih"
            "spinner" -> "daftar pilihan"
            "seekbar" -> "penggeser"
            "progressbar" -> "bilah kemajuan"
            "ratingbar" -> "penilaian"
            "view" -> "elemen"
            "framelayout", "linearlayout", "relativelayout", "constraintlayout" -> "wadah"
            else -> "elemen"
        }
    }

    /**
     * Ekstensi untuk memeriksa apakah node dapat diperluas.
     */
    private fun AccessibilityNodeInfo.isExpandable(): Boolean {
        return actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_EXPAND ||
                    it.id == AccessibilityNodeInfo.ACTION_COLLAPSE
        }
    }

    /**
     * Ekstensi untuk memeriksa apakah node sedang diperluas.
     */
    private fun AccessibilityNodeInfo.isExpanded(): Boolean {
        return actionList.any { it.id == AccessibilityNodeInfo.ACTION_COLLAPSE }
    }
}
