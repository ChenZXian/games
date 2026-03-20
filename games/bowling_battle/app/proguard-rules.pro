-keep class com.android.boot.MainActivity { *; }
-keep class com.android.boot.ui.GameView { *; }
-keepclassmembers class ** {
    public <init>(android.content.Context, android.util.AttributeSet);
}
