# Ruyani Screen Reader

**Screen reader Android mirip Jieshuo — pembaca layar aksesibilitas untuk pengguna tunanetra dan low-vision.**

[![Build APK](https://github.com/user/ruyani-screen-reader/actions/workflows/build-apk.yml/badge.svg)](https://github.com/user/ruyani-screen-reader/actions/workflows/build-apk.yml)

---

## 📖 Deskripsi

Ruyani Screen Reader adalah aplikasi pembaca layar (screen reader) untuk Android yang dirancang untuk membantu pengguna tunanetra dan low-vision dalam mengoperasikan perangkat Android mereka. Terinspirasi oleh Jieshuo, Ruyani menyediakan navigasi layar berbasis suara yang intuitif dan mudah digunakan.

## ✨ Fitur

- 🔊 **Text-to-Speech (TTS)** — Membacakan teks di layar menggunakan mesin TTS bawaan Android
- 👆 **Navigasi Sentuhan** — Jelajahi elemen di layar dengan sentuhan jari
- 📱 **Gesture Navigation** — Kontrol aplikasi menggunakan gesture (swipe, tap ganda, dll.)
- 🔔 **Notifikasi Suara** — Membacakan notifikasi yang masuk secara otomatis
- ⚙️ **Pengaturan Kustomisasi** — Atur kecepatan bicara, pitch, dan volume suara
- 🌐 **Dukungan Bahasa Indonesia** — Optimasi untuk bahasa Indonesia
- 📋 **Eksplorasi Aplikasi** — Membaca label tombol, menu, dan elemen UI lainnya
- 🔄 **Auto Update** — Pembaruan otomatis dari GitHub Releases

## 📥 Cara Install

### Download dari GitHub Actions

1. Buka halaman [**Actions**](../../actions) di repository ini
2. Klik workflow **Build APK** yang terbaru dengan status ✅ (berhasil)
3. Scroll ke bawah ke bagian **Artifacts**
4. Download file **ruyani-screen-reader-debug**
5. Ekstrak file ZIP dan install file `.apk` ke perangkat Android Anda

### Persyaratan Minimum

- Android 10 (API 29) atau lebih baru
- Ruang penyimpanan minimal 50 MB

## ⚙️ Cara Mengaktifkan

1. Install APK Ruyani Screen Reader di perangkat Anda
2. Buka **Pengaturan** (Settings) di perangkat Android
3. Pilih **Aksesibilitas** (Accessibility)
4. Cari dan pilih **Ruyani Screen Reader**
5. Aktifkan toggle untuk mengaktifkan layanan
6. Berikan izin yang diperlukan saat diminta
7. Ruyani Screen Reader sekarang aktif dan siap digunakan!

## 🛠️ Build dari Source Code

### Prasyarat

- [Android Studio](https://developer.android.com/studio) (Iguana atau lebih baru)
- JDK 17
- Android SDK dengan API Level 34

### Langkah Build

```bash
# Clone repository
git clone https://github.com/user/ruyani-screen-reader.git
cd ruyani-screen-reader

# Build debug APK
./gradlew assembleDebug
```

File APK hasil build akan berada di:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK

```bash
./gradlew assembleRelease
```

> **Catatan:** Untuk release build, Anda perlu mengkonfigurasi signing key terlebih dahulu.

## 🏗️ Tech Stack

| Komponen | Teknologi |
|---|---|
| **Bahasa** | Kotlin 1.9.22 |
| **Min SDK** | 29 (Android 10) |
| **Target SDK** | 34 (Android 14) |
| **Build System** | Gradle 8.6 + AGP 8.3.2 |
| **UI Framework** | Android View + ViewBinding |
| **Design** | Material Design 3 |
| **CI/CD** | GitHub Actions |
| **Dependencies** | AndroidX Core KTX, AppCompat, Material, ConstraintLayout |

## 📁 Struktur Proyek

```
ruyani-screen-reader/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ruyani/screenreader/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── build-apk.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## 🤝 Kontribusi

Kontribusi sangat diterima! Silakan:

1. Fork repository ini
2. Buat branch fitur baru (`git checkout -b fitur/fitur-baru`)
3. Commit perubahan Anda (`git commit -m 'Menambahkan fitur baru'`)
4. Push ke branch (`git push origin fitur/fitur-baru`)
5. Buat Pull Request

## 📄 Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE).

```
MIT License

Copyright (c) 2024 Ruyani Screen Reader

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

**Dibuat dengan ❤️ untuk komunitas tunanetra Indonesia**
