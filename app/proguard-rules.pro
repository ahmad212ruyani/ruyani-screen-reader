# Proguard Rules for Ruyani Screen Reader
# Add project specific ProGuard rules here.

# Keep accessibility service
-keep class com.ruyani.screenreader.service.RuyaniAccessibilityService { *; }

# Keep TTS Manager
-keep class com.ruyani.screenreader.tts.TTSManager { *; }
