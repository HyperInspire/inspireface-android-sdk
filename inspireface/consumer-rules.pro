# Both Android and portable JNI resolve these classes, fields and constructors by name.
-keep class com.insightface.sdk.inspireface.InspireFace { *; }
-keep class com.insightface.sdk.inspireface.FaceCapture { *; }
-keep class com.insightface.sdk.inspireface.FaceDetectionSnapshot { *; }
-keep class com.insightface.sdk.inspireface.base.** { *; }
-keep class com.insightface.sdk.inspireface.jni.Native { *; }
-keep class com.insightface.sdk.inspireface.jni.CPUEngine { native <methods>; }
-keep class com.insightface.sdk.inspireface.jni.NativeTypes$* { *; }
