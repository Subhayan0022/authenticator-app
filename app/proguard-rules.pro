# Navigation routes and the backup payload are @Serializable. The generated
# serializers are reached through Companion.serializer(), which R8 cannot see.
-keepattributes *Annotation*, InnerClasses

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# BouncyCastle is called directly for Argon2id, never through the JCA provider,
# so only the reachable classes survive. It also references JVM-only APIs that
# do not exist on Android.
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
