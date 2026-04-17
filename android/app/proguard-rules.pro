# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-defaults.txt.

# Keep Room entities
-keep class com.tricare.manuals.data.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Keep WorkManager workers
-keep class com.tricare.manuals.worker.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Markwon
-keep class io.noties.markwon.** { *; }

# PDF viewer
-keep class com.github.barteksc.pdfviewer.** { *; }

# Coil
-keep class coil.** { *; }

# Markwon optional modules — SVG and GIF support are not included in this project
-dontwarn com.caverock.androidsvg.**
-dontwarn pl.droidsonroids.gif.**

# Markwon HTML optional — CommonMark strikethrough extension not included
-dontwarn org.commonmark.ext.gfm.strikethrough.**

# Jsoup — jspecify compile-time annotations not included at runtime
-dontwarn org.jspecify.annotations.**
