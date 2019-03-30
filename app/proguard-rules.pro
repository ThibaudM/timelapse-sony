-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}