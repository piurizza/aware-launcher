# Aware uses no reflection and no libraries; the default Android rules are enough.
# Strip debug and verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
