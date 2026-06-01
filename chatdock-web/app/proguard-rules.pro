-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.example.whatsappwebnative.WebAppInterface {
    *;
}

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*
-adaptclassstrings
-allowaccessmodification
