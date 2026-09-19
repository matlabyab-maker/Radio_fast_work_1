# Fast Radio Final 3 — Android Build + Real Server Transcoding

این نسخه برای GitHub Actions تنظیم شده است.

نکته مهم: این Workflow به `gradlew` یا `gradle-wrapper.jar` نیاز ندارد. GitHub Actions خودش Gradle 8.7 را آماده می‌کند و با دستور `gradle assembleDebug` پروژه را می‌سازد.

ساختار اصلی:
- app/ — برنامه Android
- server/ — سرویس واقعی FFmpeg برای transcoding سمت سرور
- .github/workflows/android.yml — Workflow ساخت APK

پس نبودن `gradlew` در ریشه پروژه در این نسخه مانع Build نیست.
