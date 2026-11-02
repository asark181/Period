-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.nuanyue.app.**$$serializer { *; }
-keepclassmembers class com.nuanyue.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
