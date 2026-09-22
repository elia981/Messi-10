# Keep JavaScript bridge methods exposed to the trusted local page.
-keepclassmembers class com.elia.messifan.MainActivity$AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
