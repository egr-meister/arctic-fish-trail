# ---- Arctic Fish Trail R8 rules ----
# R8 is DISABLED for the first release (isMinifyEnabled=false). These rules are kept ready so
# enabling minify + shrinkResources later is a one-line change.

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.arcticfishtrail.game.**$$serializer { *; }
-keepclassmembers class com.arcticfishtrail.game.** {
    *** Companion;
}
-keepclasseswithmembers class com.arcticfishtrail.game.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# @Serializable classes used for persistence and type-safe navigation routes
-keep @kotlinx.serialization.Serializable class com.arcticfishtrail.game.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
