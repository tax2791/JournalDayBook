# ====================================================================
# ProGuard & R8 Optimization Rules for Journal DayBook
# Enables High Obfuscation & Maximum DEX Code Shrinking
# ====================================================================

# Preserve Line Numbers for Crash Reporting & Debugging Stack Traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Generic Types, Annotations, and Inner Classes for Reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Ignore unresolved optional references in external libraries
-dontwarn **

# --------------------------------------------------------------------
# 1. Local App Package Keep Rules (Entities, ViewModels, Repositories)
# --------------------------------------------------------------------
-keep class database.** { *; }
-keep class com.kushal.mealapp.** { *; }
-keep class users.** { *; }

# --------------------------------------------------------------------
# 2. Gson & Serialized Models
# --------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --------------------------------------------------------------------
# 3. Room Database
# --------------------------------------------------------------------
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# --------------------------------------------------------------------
# 4. Retrofit & OkHttp
# --------------------------------------------------------------------
-keep class retrofit2.** { *; }

# --------------------------------------------------------------------
# 5. AdMob, Google Play Services & Google Drive API
# --------------------------------------------------------------------
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.play.core.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
