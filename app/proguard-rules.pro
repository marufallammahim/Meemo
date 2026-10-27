# Meemo ProGuard / R8 Rules
# Keep Kotlinx Serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.marufallammahim.meemo.**$$serializer { *; }
-keepclassmembers class com.marufallammahim.meemo.** {
    *** Companion;
}
-keepclasseswithmembers class com.marufallammahim.meemo.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Hilt generated classes
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
