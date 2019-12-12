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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
  public static *** d(...);
  public static *** w(...);
  public static *** v(...);
  public static *** i(...);
}

-dontwarn org.apache.log4j.**

-dontwarn android.support.**
-dontwarn java.**
-dontwarn javax.**
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.v1.** { *; }

-keep class org.java_websocket.** { *; }
-keep class com.stealthcopter.networktools.** { *; }
-keep class org.slf4j.** { *; }

-keep class * implements android.os.Parcelable {
   public static final android.os.Parcelable$Creator *;
}

# VerifyError in Android 4
# https://github.com/material-components/material-components-android/issues/397
-keep class com.google.android.material.tabs.TabLayout$Tab {
*;
}