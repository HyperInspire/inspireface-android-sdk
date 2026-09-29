# Android SDK 1.2.4.post1 API audit

## Source and scope

This audit covers the user-supplied **single-library SDK archive**, its public C headers, Java sources, API manifest and Android binaries. The native SDK version is **1.2.4**. The Android wrapper and publication version is **1.2.4.post1**.

Archive SHA256: `3ab827f17891fb0065b04c7778d230497e44d2d3008a0add0d79b913275cf422`.

| Item | Count or contract |
| --- | --- |
| Public C functions in `include/inspireface.h` | 125 |
| Entries in `java/api-manifest.json` | 125 |
| Public C bindings in `jni.Native` | 125 |
| Data structures in `NativeTypes` | 34 |
| Constants, including error codes, in `NativeConstants` | 160 |
| Native methods in the original Android facade | 46 |
| Additional Android capture/snapshot Java files | 8, including 5 data-transfer classes |
| Android capture and snapshot native methods | 8 + 2 |
| `CPUEngine` native methods outside the C API manifest | 2 |
| Portable binding helpers | 2: ABI verification and native structure sizes |
| Android ABIs | `arm64-v8a`, `armeabi-v7a`, `x86_64` |
| Libraries per ABI | **One: `libInspireFace.so`** |
| Total Android shared libraries | **3** |

The C header, manifest and `Native` method-name sets match exactly in this archive. The Java/native ABI fingerprint is `0811cb2f50f00176c443331ad595dc9c33db502c1b7e032f91d2a431523715d5`. Generated descriptors, constants and bindings must remain paired with the binaries that supplied them.

Android loads all C, legacy JNI, portable JNI, capture and CPU-control entry points from `libInspireFace.so`. The supplied `NativeLibrary` detects Android Runtime or Dalvik and calls `System.loadLibrary("InspireFace")`. Its desktop-JVM loading branch is outside the Android packaging contract. Retaining a loader from a different archive would break this arrangement even if the public method signatures matched.

## Wrapper adaptation

- Preserve all original public Java signatures. Versioned source headers, the API manifest and source checksums are retained under `inspireface/sdk/1.2.4/`.
- Map all ten session and pipeline options explicitly. Preserve the historical eight-argument `CreateCustomParameter` behavior, add a nine-argument overload for emotion, and add a ten-argument overload with independent legacy-landmark and pose options.
- Convert full native detection results into independent Java arrays, including `trackCounts` and copied tokens. Empty results expose empty arrays. `FaceDetectionSnapshot.getFaces()` follows the same copy contract.
- Use explicit V2 match state for single FeatureHub searches, avoiding null native-feature dereferences when no match passes the threshold. Preserve signed 64-bit IDs and return an independent `long[]` when enumerating IDs.
- Clear the session handle after successful release and make repeated session close harmless. `Session` and `ImageStream` implement `AutoCloseable`. Streams must still be released through the API layer that created them because the layers retain input buffers differently.
- Provide convenience methods for V2 sessions, resource validation, C API level, diagnostic and component-version text, five-point landmarks, tracking controls, supported detector sizes, landmark augmentation and disabling logs. The complete low-level surface remains available through `Native`.
- Preserve JNI-resolved class names, constructors, native methods and data fields in consumer shrinker rules. Publish the Android artifact as `1.2.4.post1`.

## Complete interface inventory

Every row below has a public method in `com.insightface.sdk.inspireface.jni.Native`. The versioned manifest records the complete Java parameter and return-type signatures for automated comparison. The original-facade column describes compatibility with the pre-upgrade Android API. A dash means that facade had no direct equivalent; it does not claim that the C function itself first appeared in version 1.2.4.

### Error descriptions (1)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 1 | `HFGetErrorMessage` | `long` | — |

### Image streams and bitmaps (18)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 2 | `HFCreateImageStream` | `long` | `CreateImageStreamFromBitmap / CreateImageStreamFromByteBuffer` |
| 3 | `HFCreateImageStreamEmpty` | `long` | — |
| 4 | `HFImageStreamSetBuffer` | `long` | — |
| 5 | `HFImageStreamSetRotation` | `long` | — |
| 6 | `HFImageStreamSetFormat` | `long` | — |
| 7 | `HFReleaseImageStream` | `long` | `ReleaseImageStream` |
| 8 | `HFCreateImageBitmap` | `long` | — |
| 9 | `HFCreateImageBitmapFromFilePath` | `long` | — |
| 10 | `HFImageBitmapCopy` | `long` | — |
| 11 | `HFReleaseImageBitmap` | `long` | — |
| 12 | `HFCreateImageStreamFromImageBitmap` | `long` | — |
| 13 | `HFCreateImageBitmapFromImageStreamProcess` | `long` | — |
| 14 | `HFImageBitmapWriteToFile` | `long` | — |
| 15 | `HFImageBitmapDrawRect` | `long` | — |
| 16 | `HFImageBitmapDrawCircleF` | `long` | — |
| 17 | `HFImageBitmapDrawCircle` | `long` | — |
| 18 | `HFImageBitmapGetData` | `long` | — |
| 19 | `HFImageBitmapShow` | `long` | — |

### Resource packs and global lifecycle (5)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 20 | `HFValidateResourcePack` | `int` | — |
| 21 | `HFLaunchInspireFace` | `long` | `GlobalLaunch` |
| 22 | `HFReloadInspireFace` | `long` | `GlobalReload` |
| 23 | `HFTerminateInspireFace` | `long` | `GlobalTerminate` |
| 24 | `HFQueryInspireFaceLaunchStatus` | `long` | `QueryLaunchStatus` |

### Platform and hardware controls (12)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 25 | `HFQueryExpansiveHardwareRGACompileOption` | `long` | — |
| 26 | `HFSetExpansiveHardwareRockchipDmaHeapPath` | `long` | — |
| 27 | `HFQueryExpansiveHardwareRockchipDmaHeapPath` | `long` | — |
| 28 | `HFQueryExpansiveHardwareRockchipDmaHeapPathWithSize` | `long` | — |
| 29 | `HFSwitchImageProcessingBackend` | `long` | — |
| 30 | `HFSetImageProcessAlignedWidth` | `long` | — |
| 31 | `HFSetAppleCoreMLInferenceMode` | `long` | — |
| 32 | `HFSetCudaDeviceId` | `long` | — |
| 33 | `HFGetCudaDeviceId` | `long` | — |
| 34 | `HFPrintCudaDeviceInfo` | `long` | — |
| 35 | `HFGetNumCudaDevices` | `long` | — |
| 36 | `HFCheckCudaDeviceSupport` | `long` | — |

### Landmark engines and session configuration (17)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 37 | `HFSwitchLandmarkEngine` | `long` | — |
| 38 | `HFQuerySupportedPixelLevelsForFaceDetection` | `long` | — |
| 39 | `HFCreateInspireFaceSession` | `long` | `CreateSession` |
| 40 | `HFCreateInspireFaceSessionOptional` | `long` | — |
| 41 | `HFCreateInspireFaceSessionV2` | `int` | — |
| 42 | `HFReleaseInspireFaceSession` | `long` | `ReleaseSession` |
| 43 | `HFSessionClearTrackingFace` | `long` | — |
| 44 | `HFSessionSetTrackLostRecoveryMode` | `long` | — |
| 45 | `HFSessionSetLightTrackConfidenceThreshold` | `long` | — |
| 46 | `HFSessionSetTrackPreviewSize` | `long` | `SetTrackPreviewSize` |
| 47 | `HFSessionGetTrackPreviewSize` | `long` | — |
| 48 | `HFSessionSetFilterMinimumFacePixelSize` | `long` | `SetFilterMinimumFacePixelSize` |
| 49 | `HFSessionSetFaceDetectThreshold` | `long` | `SetFaceDetectThreshold` |
| 50 | `HFSessionSetTrackModeSmoothRatio` | `long` | `SetTrackModeSmoothRatio` |
| 51 | `HFSessionSetTrackModeNumSmoothCacheFrame` | `long` | `SetTrackModeNumSmoothCacheFrame` |
| 52 | `HFSessionSetTrackModeDetectInterval` | `long` | `SetTrackModeDetectInterval` |
| 53 | `HFSessionSetLandmarkAugmentationNum` | `long` | — |

### Detection and owned snapshots (4)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 54 | `HFExecuteFaceTrack` | `long` | `ExecuteFaceTrack` |
| 55 | `HFExecuteFaceTrackSnapshot` | `long` | — |
| 56 | `HFGetFaceResultSnapshotData` | `long` | — |
| 57 | `HFReleaseFaceResultSnapshot` | `long` | — |

### Stateful face capture (8)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 58 | `HFGetDefaultFaceCaptureConfig` | `long` | — |
| 59 | `HFCreateFaceCaptureSession` | `long` | — |
| 60 | `HFUpdateFaceCaptureSession` | `long` | — |
| 61 | `HFUpdateFaceCaptureSessionWithSnapshot` | `long` | — |
| 62 | `HFGetFaceCaptureResults` | `long` | — |
| 63 | `HFFinishFaceCaptureSession` | `long` | — |
| 64 | `HFResetFaceCaptureSession` | `long` | — |
| 65 | `HFReleaseFaceCaptureSession` | `long` | — |

### Tokens, landmarks and timing (8)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 66 | `HFSessionLastFaceDetectionGetDebugPreviewImageSize` | `long` | — |
| 67 | `HFCopyFaceBasicToken` | `long` | — |
| 68 | `HFGetFaceBasicTokenSize` | `long` | — |
| 69 | `HFGetNumOfFaceDenseLandmark` | `long` | — |
| 70 | `HFGetFaceDenseLandmarkFromFaceToken` | `long` | `GetFaceDenseLandmarkFromFaceToken` |
| 71 | `HFGetFaceFiveKeyPointsFromFaceToken` | `long` | — |
| 72 | `HFSessionSetEnableTrackCostSpend` | `long` | — |
| 73 | `HFSessionPrintTrackCostSpend` | `long` | — |

### Feature extraction and alignment (7)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 74 | `HFFaceFeatureExtract` | `long` | `ExtractFaceFeature` |
| 75 | `HFFaceFeatureExtractTo` | `long` | — |
| 76 | `HFFaceFeatureExtractCpy` | `long` | — |
| 77 | `HFCreateFaceFeature` | `long` | — |
| 78 | `HFReleaseFaceFeature` | `long` | — |
| 79 | `HFFaceGetFaceAlignmentImage` | `long` | `GetFaceAlignmentImage` |
| 80 | `HFFaceFeatureExtractWithAlignmentImage` | `long` | — |

### FeatureHub and similarity (19)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 81 | `HFFeatureHubDataEnable` | `long` | `FeatureHubDataEnable` |
| 82 | `HFFeatureHubDataDisable` | `long` | `FeatureHubDataDisable` |
| 83 | `HFFeatureHubFaceSearchThresholdSetting` | `long` | `FeatureHubFaceSearchThresholdSetting` |
| 84 | `HFFaceComparison` | `long` | `FaceComparison` |
| 85 | `HFGetRecommendedCosineThreshold` | `long` | `GetRecommendedCosineThreshold` |
| 86 | `HFCosineSimilarityConvertToPercentage` | `long` | `CosineSimilarityConvertToPercentage` |
| 87 | `HFUpdateCosineSimilarityConverter` | `long` | `UpdateCosineSimilarityConverter` |
| 88 | `HFGetCosineSimilarityConverter` | `long` | `GetCosineSimilarityConverter` |
| 89 | `HFGetFeatureLength` | `long` | `GetFeatureLength` |
| 90 | `HFFeatureHubInsertFeature` | `long` | `FeatureHubInsertFeature` |
| 91 | `HFFeatureHubFaceSearch` | `long` | `FeatureHubFaceSearch` |
| 92 | `HFFeatureHubFaceSearchV2` | `long` | — |
| 93 | `HFFeatureHubFaceSearchTopK` | `long` | `FeatureHubFaceSearchTopK` |
| 94 | `HFFeatureHubFaceRemove` | `long` | `FeatureHubFaceRemove` |
| 95 | `HFFeatureHubFaceUpdate` | `long` | `FeatureHubFaceUpdate` |
| 96 | `HFFeatureHubGetFaceIdentity` | `long` | `FeatureHubGetFaceIdentity` |
| 97 | `HFFeatureHubGetFaceCount` | `long` | `FeatureHubGetFaceCount` |
| 98 | `HFFeatureHubViewDBTable` | `long` | — |
| 99 | `HFFeatureHubGetExistingIds` | `long` | — |

### Attributes and processing pipelines (10)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 100 | `HFMultipleFacePipelineProcess` | `long` | `MultipleFacePipelineProcess` |
| 101 | `HFMultipleFacePipelineProcessOptional` | `long` | — |
| 102 | `HFGetRGBLivenessConfidence` | `long` | `GetRGBLivenessConfidence` |
| 103 | `HFGetFaceMaskConfidence` | `long` | `GetFaceMaskConfidence` |
| 104 | `HFGetFaceQualityConfidence` | `long` | `GetFaceQualityConfidence` |
| 105 | `HFFaceQualityDetect` | `long` | — |
| 106 | `HFGetFaceInteractionStateResult` | `long` | `GetFaceInteractionStateResult` |
| 107 | `HFGetFaceInteractionActionsResult` | `long` | `GetFaceInteractionActionsResult` |
| 108 | `HFGetFaceAttributeResult` | `long` | `GetFaceAttributeResult` |
| 109 | `HFGetFaceEmotionResult` | `long` | `GetFaceEmotionResult` |

### Versions, diagnostics and logging (9)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 110 | `HFQueryInspireFaceVersion` | `long` | `QueryInspireFaceVersion` |
| 111 | `HFQueryCAPILevel` | `int` | — |
| 112 | `HFQueryInspireFaceComponentVersion` | `long` | — |
| 113 | `HFQueryInspireFaceComponentVersions` | `long` | — |
| 114 | `HFQueryInspireFaceDiagnosticInformation` | `long` | — |
| 115 | `HFQueryInspireFaceExtendedInformation` | `long` | — |
| 116 | `HFSetLogLevel` | `long` | `SetLogLevel` |
| 117 | `HFLogDisable` | `long` | — |
| 118 | `HFLogPrint` | `long` | — |

### Debugging and resource statistics (7)

| No. | `Native` method | Return type | Original facade equivalent |
| --- | --- | --- | --- |
| 119 | `HFDeBugImageStreamImShow` | `void` | — |
| 120 | `HFDeBugImageStreamDecodeSave` | `long` | `WriteImageStreamToFile` |
| 121 | `HFDeBugShowResourceStatistics` | `long` | — |
| 122 | `HFDeBugGetUnreleasedSessionsCount` | `long` | — |
| 123 | `HFDeBugGetUnreleasedSessions` | `long` | — |
| 124 | `HFDeBugGetUnreleasedStreamsCount` | `long` | — |
| 125 | `HFDeBugGetUnreleasedStreams` | `long` | — |

## Complete data-structure inventory

Import `NativeTypes` and `NativeConstants` without hand-editing generated field names, types or initializers. Each of the 34 structures and every public data field is listed below. `HFFaceFeature` also has a private `nativeOwner` field used by JNI; it must be retained.

| Structure | Public data fields |
| --- | --- |
| `HFaceRect` | `x`, `y`, `width`, `height` |
| `HPoint2f` | `x`, `y` |
| `HPoint2i` | `x`, `y` |
| `HColor` | `r`, `g`, `b` |
| `HFImageData` | `width`, `height`, `format`, `rotation`, `data` |
| `HFImageBitmapData` | `width`, `height`, `channels`, `data` |
| `HFResourcePackInfo` | `structSize`, `structVersion`, `archiveFileCount`, `modelCount`, `tag`, `version`, `major`, `releaseDate`, `reserved` |
| `HFSessionCustomParameter` | `enable_recognition`, `enable_liveness`, `enable_ir_liveness`, `enable_mask_detect`, `enable_face_quality`, `enable_face_attribute`, `enable_interaction_liveness`, `enable_detect_mode_landmark`, `enable_face_pose`, `enable_face_emotion` |
| `HFSessionConfigV2` | `structSize`, `structVersion`, `featureMask`, `detectMode`, `maxDetectFaceNum`, `detectPixelLevel`, `trackByDetectModeFPS`, `reserved` |
| `HFFaceDetectPixelList` | `size`, `pixel_level` |
| `HFFaceBasicToken` | `size`, `data` |
| `HFFaceEulerAngle` | `roll`, `yaw`, `pitch` |
| `HFMultipleFaceData` | `detectedNum`, `rects`, `trackIds`, `trackCounts`, `detConfidence`, `angles`, `tokens` |
| `HFFaceCaptureConfig` | `structSize`, `structVersion`, `filterMask`, `outputCount`, `minTrackCount`, `stableDurationMs`, `collectDurationMs`, `maxCollectDurationMs`, `trackLostGraceMs`, `minCandidateIntervalMs`, `minFaceWidthRatio`, `maxFaceWidthRatio`, `maxCenterOffsetX`, `maxCenterOffsetY`, `boundaryMarginRatio`, `maxCenterMotionRatio`, `maxSizeChangeRatio`, `maxAbsYaw`, `maxAbsPitch`, `maxAbsRoll`, `minQualityScore`, `minSharpnessScore`, `minBrightnessScore`, `maxBrightnessScore`, `reserved` |
| `HFFaceCaptureMetrics` | `availableMetrics`, `faceWidthRatio`, `centerOffsetX`, `centerOffsetY`, `stabilityScore`, `poseScore`, `qualityScore`, `sharpnessScore`, `brightnessScore` |
| `HFFaceCaptureProgress` | `state`, `candidateCount`, `frameId`, `timestampMs`, `trackId`, `trackCount`, `evaluatedFilters`, `rejectReasons`, `progress`, `currentScore`, `metrics` |
| `HFFaceCaptureResult` | `frameId`, `timestampMs`, `trackId`, `trackCount`, `score`, `roll`, `yaw`, `pitch`, `rect`, `token`, `metrics` |
| `HFFaceFeature` | `size`, `data` |
| `HFFeatureHubConfiguration` | `primaryKeyMode`, `enablePersistence`, `searchThreshold`, `searchMode`, `persistenceDbPath` |
| `HFFaceFeatureIdentity` | `id`, `feature` |
| `HFFeatureHubSearchResultV2` | `found`, `id`, `confidence`, `feature` |
| `HFSearchTopKResults` | `size`, `confidence`, `ids` |
| `HFSimilarityConverterConfig` | `threshold`, `middleScore`, `steepness`, `outputMin`, `outputMax` |
| `HFFeatureHubExistingIds` | `size`, `ids` |
| `HFRGBLivenessConfidence` | `num`, `confidence` |
| `HFFaceMaskConfidence` | `num`, `confidence` |
| `HFFaceQualityConfidence` | `num`, `confidence` |
| `HFFaceInteractionState` | `num`, `leftEyeStatusConfidence`, `rightEyeStatusConfidence` |
| `HFFaceInteractionsActions` | `num`, `normal`, `shake`, `jawOpen`, `headRaise`, `blink` |
| `HFFaceAttributeResult` | `num`, `race`, `gender`, `ageBracket` |
| `HFFaceEmotionResult` | `num`, `emotion` |
| `HFInspireFaceVersion` | `major`, `minor`, `patch` |
| `HFComponentVersion` | `major`, `minor`, `patch`, `state` |
| `HFInspireFaceExtendedInformation` | `information` |

The 160 constants include image formats and rotations, detection/search/key modes, feature masks, three landmark engines, ten capture-filter bits, twelve capture-rejection reasons, six capture states, component/version states, logging levels, API/structure versions and all error codes. The facade also exposes `STREAM_I420 = 6`, `STREAM_GRAY = 7` and `LOG_ERROR = 4` while preserving existing constant values.

## Ownership, compatibility and behavior contracts

1. **Single-library packaging and shrinking.** Package exactly one `libInspireFace.so` for each of the three supported ABIs. That library contains the native core and every Android JNI adapter. Consumer rules must protect the legacy `base` classes, capture/snapshot classes, `jni.Native`, nested `jni.NativeTypes` descriptors and `jni.CPUEngine` native methods. Preserving only method names is insufficient because JNI also resolves classes, fields and constructors.
2. **Versioned session configuration.** C API level 2 is additive; the original creation APIs remain available. `HFSessionConfigV2` contains `structSize`, version 1, a 64-bit feature mask and eight zero-initialized reserved fields. The supplied Java descriptor obtains the correct structure size from the running ABI. Do not hard-code a size or assume that zero means the documented `-1` default. Unknown or unavailable feature bits return `HERR_UNSUPPORTED`.
3. **Input image memory.** Portable bindings require writable direct `ByteBuffer` objects with sufficient remaining bytes. They honor position and limit and retain a strong reference until stream release or buffer replacement. Keep image contents unchanged during use. Use native byte order for typed data. NV12, NV21 and I420 require even width and height. Empty streams initially use NV21; set the intended format before installing a differently sized image buffer. Bitmap-derived streams own a pixel snapshot.
4. **Borrowed outputs and allocated resources.** Portable output pixel, token and feature buffers borrow native storage. Read or copy them before the corresponding owner or next invalidating operation changes that storage. Owned detection snapshots survive later detection and release of the original session and stream, and must be explicitly released. Android facade arrays and capture tokens are copied into Java-owned storage. Only features allocated by `HFCreateFaceFeature` may be released with `HFReleaseFaceFeature` or `close()`; borrowed features must not be closed. Feature close is not idempotent.
5. **Tracking counts.** The new native face-result structure includes `trackCounts`. The retained legacy JNI detector does not fill such a Java field, so adding only a DTO field would be incomplete. The updated facade converts the complete portable result and copies counts. Capture progress and results preserve `trackCount`; the default track-count filter requires five consecutive tracker observations.
6. **Capture lifecycle.** Capture supports ten filters, six states and up to eight ordered candidates. Defaults enable face count, size, position, boundary, stability and tracking count. Pose and quality filters require the corresponding options in the parent session. Capture is synchronous, creates no camera or worker and retains its parent session. Close capture before releasing that session. Regular update performs detection; snapshot update reuses the current frame's detection. Reusing one frozen snapshot cannot advance its tracking count. Use monotonically increasing frame IDs and timestamps. Finish preserves candidates, reset clears temporal state and results, and close releases resources.
7. **Capture results do not contain pixels.** A result contains frame ID, timestamp, rectangle, pose, token and metrics. Applications needing the selected crop or recognition feature must retain the matching original frame. A C/portable result token expires at the next update, reset, finish or release of its capture session. Android `FaceCapture.getResults()` copies token bytes.
8. **FeatureHub match state and IDs.** `HFFeatureHubFaceSearchV2.found` explicitly reports whether a candidate passes the threshold, including persisted records with the reserved sentinel ID. Do not infer V2 match state from an ID alone. New manual insertions reject only the reserved `-1` ID, not all negative IDs. Single-search feature buffers remain valid until the next single search on the same thread. Automatic insertions must write back their allocated ID. Preserve 64-bit IDs for Top-K and enumeration.
9. **Resource validation and diagnostics.** Resource-pack validation checks archive metadata, declared model configurations/payloads and build support without launching inference or changing launch state. Error descriptions, component-version text and diagnostic text support null/zero size queries; returned capacity includes the terminating NUL byte. Allocate a direct buffer of that capacity for the second call. Component-version and diagnostic queries work before launch. Component states distinguish known versions, disabled components and unknown versions. Component-version text is semicolon-separated `key=value` data, not JSON.
10. **Platform controls.** Hardware-specific functions remain exposed even when a given Android build does not support that backend. Query capabilities and check native return codes instead of assuming support. Prefer the size-aware hardware-path query to the legacy unbounded output variant.
11. **CPU configuration.** `CPUEngine.PowerMode` provides NORMAL, HIGH and LOW. A changed policy affects subsequently initialized CPU runtimes; existing runtime thread counts and precision remain unchanged. Launch, reload and terminate preserve the setting. Do not change it concurrently with session/model initialization. These two native methods sit outside the C API manifest and need separate export and runtime checks.
12. **Threading and status codes.** Serialize use of a session and process-wide launch/reload/terminate and FeatureHub state. Snapshot ownership provides result-lifetime isolation, not permission to run arbitrary operations concurrently on one session. Of the 125 C bindings, 121 return `long` status, three return `int` status and one returns `void`. Distinguish a successful query return code from its output launch-state flag. Preserve old boolean/null conventions where documented; strict helpers expose native status through `InspireFaceException`.
13. **Legacy compatibility details.** The original native session release did not clear its Java handle; the wrapper now does so after success. The historical eighth convenience-factory argument keeps its pose behavior. Emotion and the separate legacy-landmark option are available through additive overloads. New format aliases, five-point landmarks, supported detector sizes, tracking recovery/confidence controls, landmark augmentation and diagnostic helpers supplement the full `Native` interface.

## Verification matrix

| Layer | Required evidence | Current single-library status |
| --- | --- | --- |
| Archive identity | Archive SHA256 and internal file checksums | Archive identity recorded above; all 73 internal SHA256 entries match |
| API signatures | C header, manifest and Java methods; all descriptors/constants | Archive name sets and counts agree: 125 APIs, 34 descriptors, 160 constants |
| Native exports | C, legacy JNI, portable JNI, capture, CPU and ABI helpers in each core library | All 125 C functions, 125 portable bindings, 46 original JNI entry points and 180 currently required Java native symbols are present on every ABI |
| Packaging | Three core libraries, consumer rules, versioned publication, sample/test APKs | Clean build passed; AAR, debug/release app APKs and instrumentation APK each contain exactly three core libraries |
| Java contracts | Buffer copies, position/length handling, empty faces, all ten options, capture bounds | All 12 host tests passed with no failures |
| Shrinking | Release APK and preservation of JNI-resolved names | Minified release build and required JNI-name checks passed |
| Android runtime | ABI initialization, versions, resource validation, CPU state, buffer rejection | Passed on Xiaomi 2608BPX34C, Android 17/API 37, `arm64-v8a`, 4096-byte pages |
| Inference and ownership | Detection, feature extraction, pipeline, FeatureHub, snapshots and capture | Passed with the bundled Pikachu model and `kun.jpg` fixture on that device |

The build results below were obtained after removing prior build outputs and rebuilding with the replacement single-library archive. The archive contains no replacement model assets. Device execution validates the bundled Pikachu model and the tested image fixture on the configuration recorded below; other models and device configurations require separate validation.

### Single-library verification record (2026-09-29)

- Independently checked the archive SHA256 and all 73 entries in its internal checksum list.
- The final `scripts/verify-sdk-api.py` audit passed against the single-library archive, including Java/C signatures, source provenance, native symbols and preservation of all 194 original public declarations.
- Used NDK `llvm-nm -D --defined-only` on each replacement `libInspireFace.so`: all public C functions and all currently declared Java native methods resolve directly in that one library. The 180 required JNI symbols include legacy adapters, capture/snapshot methods, CPU controls and ABI helpers; they are a different count from the 125 public C functions.
- Parsed each ELF dynamic section. All three ABIs depend only on `libdl.so`, `libjnigraphics.so`, `liblog.so`, `libm.so` and `libc.so`. None depends on a secondary JNI adapter or another bundled shared library.
- A clean single-library Gradle build passed for the release AAR, debug app APK, minified release app APK, instrumentation APK, source JAR and publication POM. The publication version is `1.2.4.post1`.
- All four packaged Android artifacts were checked independently: each contains exactly one core shared library per supported ABI, for three shared libraries total. All 180 required JNI symbols and the public C exports are retained, and ELF dependencies are limited to the system libraries listed above.
- The packaged ELF files support 16 KiB alignment. The release APK passed `zipalign -P 16`, and its shrinker mapping preserves the names required by JNI. These are packaging checks; they do not replace execution on an Android device with that page size.
- `:inspireface:testDebugUnitTest` passed all **12 tests**, with zero failures: six capture-configuration boundary tests and six native-conversion ownership, position, length, empty-result and complete-option-mapping tests.
- `:inspireface:lintRelease` and `:app:lintDebug` passed with zero errors.
- `:inspireface:connectedDebugAndroidTest` passed all **8 instrumentation tests**, with zero failures, errors or skipped tests. The recorded test time was **6.089 seconds**; the Gradle invocation completed in **41 seconds**. Coverage includes version/resource/error/CPU queries, legacy recognition and FeatureHub behavior, native feature ownership and V2 search, snapshot lifetime, capture through both API layers, image conversion, direct-buffer rejection and odd YUV dimensions.

### Device execution details

| Property | Tested configuration |
| --- | --- |
| Manufacturer and model | Xiaomi 2608BPX34C |
| Android version | Android 17, API 37 |
| Executed ABI | `arm64-v8a` |
| System page size | 4096 bytes |
| Model | Bundled Pikachu resource pack |
| Image fixture | Bundled `kun.jpg` |
| Result | 8 passed; 0 failures, 0 errors, 0 skipped |

The local build artifacts include the [JUnit XML](<../inspireface/build/outputs/androidTest-results/connected/debug/TEST-2608BPX34C - 17-_inspireface-.xml>) and [HTML test report](../inspireface/build/reports/androidTests/connected/debug/index.html).

This run did not exercise a live camera, other model packs, 32-bit or x86 devices, or an Android device using 16 KiB pages. The 16 KiB ELF and APK alignment checks above remain packaging evidence rather than runtime validation on that page size.
