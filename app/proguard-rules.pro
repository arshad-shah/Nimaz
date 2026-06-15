# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers so Crashlytics can map obfuscated release stack traces
# back to source (the Crashlytics Gradle plugin uploads the mapping file).
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name while keeping line numbers.
-renamesourcefileattribute SourceFile

# Keep custom exception type names readable in crash reports.
-keep public class * extends java.lang.Exception