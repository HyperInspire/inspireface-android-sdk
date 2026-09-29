# InspireFace Android SDK

[![GitHub release](https://img.shields.io/github/v/release/HyperInspire/InspireFace.svg?style=for-the-badge&color=blue&label=Github+release&logo=github)](https://github.com/HyperInspire/InspireFace/releases/latest)
[![Model](https://img.shields.io/github/v/release/HyperInspire/InspireFace.svg?style=for-the-badge&color=blue&label=Model+Zoo&logo=github)](https://github.com/HyperInspire/InspireFace/releases/tag/v1.x)
[![JitPack](https://img.shields.io/jitpack/v/github/HyperInspire/inspireface-android-sdk?style=for-the-badge&color=green&label=JitPack&logo=android)](https://jitpack.io/#HyperInspire/inspireface-android-sdk)

InspireFace is a cross-platform face recognition SDK developed in C/C++, supporting multiple operating systems and various backend types for inference, such as CPU, GPU, and NPU.

If you require further information on tracking development branches, CI/CD processes, or downloading pre-compiled libraries, please visit our [development repository](https://github.com/HyperInspire/InspireFace).

Please contact [contact@insightface.ai](mailto:contact@insightface.ai?subject=InspireFace) for commercial support, including obtaining and integrating higher accuracy models, as well as custom development.

## Source Code Project

If you need to learn about the build steps in the source code of the project, you can go : [InspireFace](https://github.com/HyperInspire/InspireFace).

## Supporting Platform
- arm64-v8a 
- armeabi-v7a
- x86_64

Minimum Android API: 24. Each ABI contains a single `libInspireFace.so`, including the core, Android JNI and complete portable JNI API.

## Add Dependency

- Step 1. Add the JitPack repository to your build file add it in your root **build.gradle** at the end of repositories:

  ```groovy
  allprojects {
      repositories {
         ...
         maven { url 'https://jitpack.io' }
      }
  }
  ```

- Step 2. Add the dependency

  ```groovy
  dependencies {
      implementation 'com.github.HyperInspire:inspireface-android-sdk:1.2.4.post1'
  }
  ```

## How to use the Android/Java Api

We provide a Java API for Android devices, which is implemented using Java Native Interface(JNI). 

```java
// Launch InspireFace, only need to call once
boolean launchStatus = InspireFace.GlobalLaunch(this, InspireFace.PIKACHU);
if (!launchStatus) {
    throw new IllegalStateException("Failed to launch InspireFace");
}

// Create a ImageStream
ImageStream stream = InspireFace.CreateImageStreamFromBitmap(img, InspireFace.CAMERA_ROTATION_0);

// Create a session
CustomParameter parameter = InspireFace.CreateCustomParameter()
                .enableRecognition(true)
                .enableFaceQuality(true)
                .enableFaceAttribute(true)
                .enableInteractionLiveness(true)
                .enableLiveness(true)
                .enableMaskDetect(true);
Session session = InspireFace.CreateSession(parameter, InspireFace.DETECT_MODE_ALWAYS_DETECT, 10, -1, -1);

if (session == null || stream == null) {
    if (stream != null) InspireFace.ReleaseImageStream(stream);
    if (session != null) InspireFace.ReleaseSession(session);
    InspireFace.GlobalTerminate();
    throw new IllegalStateException("Failed to create session or stream");
}
try {
    MultipleFaceData faces = InspireFace.ExecuteFaceTrack(session, stream);
    if (faces != null && faces.detectedNum > 0) {
        FaceFeature feature = InspireFace.ExtractFaceFeature(session, stream, faces.tokens[0]);
        // Use feature after checking for null.
    }
} finally {
    InspireFace.ReleaseImageStream(stream);
    InspireFace.ReleaseSession(session);
    InspireFace.GlobalTerminate();
}
```


## 1.2.4.post1 upgrade

This Android release packages InspireFace **1.2.4** and C API level **2** from the supplied **single-library SDK archive**. The Android/Maven version is **1.2.4.post1**; `QueryInspireFaceVersion()` reports the native version **1.2.4**. Existing Pikachu/Megatron model assets are retained because this archive does not contain replacement models. Use resource-pack validation before loading an external pack.

All existing public Java signatures are retained. The adapter now initializes every session/pipeline option, exposes `CustomParameter.enableDetectModeLandmark(boolean)` and `MultipleFaceData.trackCounts`, safely handles FeatureHub searches with no match, and clears released session handles. No match is represented by `id == -1` and `feature == null`; ID `-1` is reserved as the no-match sentinel (and requests allocation in auto-increment mode). Native failures in the old API retain their boolean/null conventions where applicable. New query/control helpers throw `InspireFaceException` with the native error code; session creation, key-point decoding and ID enumeration document their null-on-failure behavior.

On Android, all API layers load `InspireFace`; no separate JNI library is required.

The complete, individually enumerated interface coverage is in [the SDK audit](docs/sdk-1.2.4-api-audit.md). The versioned [API manifest](inspireface/sdk/1.2.4/api-manifest.json) and [C header](inspireface/sdk/1.2.4/include/inspireface.h) are retained for verification.

### Complete Java API

`com.insightface.sdk.inspireface.jni.Native` exposes every public C function, with descriptors in `NativeTypes` and all flags/error codes in `NativeConstants`. This includes V2 session creation and search, image bitmap/stream management, detector/tracker tuning, snapshot ownership, capture, feature allocation/extraction, FeatureHub ID enumeration, resource validation, component versions, diagnostics and debug resource counters. Hardware-specific functions are available to call but still depend on the capabilities of the supplied Android build; check their return status.

```java
import com.insightface.sdk.inspireface.jni.Native;
import com.insightface.sdk.inspireface.jni.NativeTypes.*;
import com.insightface.sdk.inspireface.jni.InspireFaceException;
import com.insightface.sdk.inspireface.jni.CPUEngine;
import static com.insightface.sdk.inspireface.jni.NativeConstants.*;

// Configure before creating CPU sessions. Existing sessions are unaffected.
CPUEngine.setGlobalPowerMode(CPUEngine.PowerMode.NORMAL);
HFResourcePackInfo pack = new HFResourcePackInfo();
InspireFaceException.check(Native.HFValidateResourcePack(modelPath, pack));
InspireFaceException.check(Native.HFLaunchInspireFace(modelPath));
HFSessionConfigV2 config = new HFSessionConfigV2(); // struct size/version set by JNI
config.featureMask = HF_ENABLE_FACE_RECOGNITION | HF_ENABLE_QUALITY;
config.detectMode = HF_DETECT_MODE_ALWAYS_DETECT;
config.maxDetectFaceNum = 10;
config.detectPixelLevel = -1;
config.trackByDetectModeFPS = -1;
long[] handle = new long[1];
InspireFaceException.check(Native.HFCreateInspireFaceSessionV2(config, handle));
try {
    // Use Native functions with handle[0].
} finally {
    InspireFaceException.check(Native.HFReleaseInspireFaceSession(handle[0]));
    InspireFaceException.check(Native.HFTerminateInspireFace());
}
```

`Native` preserves C status codes rather than converting failures to booleans. Use `InspireFaceException.check(status)` when exceptions are preferred. Direct buffers must be writable, sized for the data and use `ByteOrder.nativeOrder()`; position/limit are honored. Output pixel/token/feature buffers are borrowed views: copy them before the next operation that invalidates them and before releasing their owner. Only features allocated by `HFCreateFaceFeature` may be released with `HFReleaseFaceFeature`/`close()`.

Release each image stream through the API that created it (`InspireFace.ReleaseImageStream` for Android bitmap/byte-array helpers; `Native.HFReleaseImageStream` for portable streams). Do not mix these release paths: the adapters retain input buffers differently. Serialize all use of a session, and serialize global launch/reload/terminate and FeatureHub operations. Close all session-dependent resources before terminating or reloading.

### Face capture and immutable detection snapshots

`FaceCapture` and `FaceDetectionSnapshot` implement `AutoCloseable`. The application supplies frames and monotonically increasing frame IDs/timestamps; no camera or worker is created. Use a tracking session for the default `FILTER_TRACK_COUNT` requirement. Enable pose/quality in the session when selecting those capture filters.

```java
// session and stream must remain open throughout this block.
try (FaceCapture capture = FaceCapture.create(session, FaceCapture.defaultConfig())) {
    // For each camera frame, create a stream and then:
    try (FaceDetectionSnapshot snapshot = FaceDetectionSnapshot.create(session, stream)) {
        MultipleFaceData faces = snapshot.getFaces(); // owned copy, includes trackCounts
        FaceCaptureProgress progress = capture.update(stream, snapshot, frameId, timestampMs);
        // Update UI from progress.state / rejectReasons / metrics.
    }
    // Repeat the frame block above, without calling ExecuteFaceTrack again on the same frame.
    capture.finish();
    FaceCaptureResult[] selected = capture.getResults(); // copied tokens and metadata
}
```

A capture result contains metadata and a token, not an image. Retain the corresponding original frames if you need a selected crop or recognition feature. Close capture before its source session. Snapshot/result copies remain Java-owned after close; portable `Native` results follow C borrowing rules instead.

### Verification

```sh
python3 scripts/verify-sdk-api.py
./gradlew :inspireface:assembleRelease :inspireface:testDebugUnitTest \
  :inspireface:assembleDebugAndroidTest :app:assembleDebug
# Requires a connected Android device/emulator:
./gradlew :inspireface:connectedDebugAndroidTest
```

Consumer R8/ProGuard rules preserve JNI class names, fields and constructors for both API layers. Do not remove them when repackaging the library.
