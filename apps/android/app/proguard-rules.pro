# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.azuredevid.cassy.**$$serializer { *; }
-keepclassmembers class com.azuredevid.cassy.** {
    *** Companion;
}
-keepclasseswithmembers class com.azuredevid.cassy.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor & OkHttp
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Google Tink
-dontwarn com.google.crypto.tink.**
