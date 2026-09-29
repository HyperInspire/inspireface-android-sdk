package com.insightface.sdk.inspireface;

import android.content.Context;
import android.graphics.Bitmap;

import com.insightface.sdk.inspireface.base.CustomParameter;
import com.insightface.sdk.inspireface.base.FaceAttributeResult;
import com.insightface.sdk.inspireface.base.FaceBasicToken;
import com.insightface.sdk.inspireface.base.FaceEmotionResult;
import com.insightface.sdk.inspireface.base.FaceFeature;
import com.insightface.sdk.inspireface.base.FaceFeatureIdentity;
import com.insightface.sdk.inspireface.base.FaceInteractionState;
import com.insightface.sdk.inspireface.base.FaceInteractionsActions;
import com.insightface.sdk.inspireface.base.FaceMaskConfidence;
import com.insightface.sdk.inspireface.base.FaceQualityConfidence;
import com.insightface.sdk.inspireface.base.FeatureHubConfiguration;
import com.insightface.sdk.inspireface.base.ImageStream;
import com.insightface.sdk.inspireface.base.InspireFaceVersion;
import com.insightface.sdk.inspireface.base.MultipleFaceData;
import com.insightface.sdk.inspireface.base.Point2f;
import com.insightface.sdk.inspireface.base.RGBLivenessConfidence;
import com.insightface.sdk.inspireface.base.SearchTopKResults;
import com.insightface.sdk.inspireface.base.Session;
import com.insightface.sdk.inspireface.base.SimilarityConverterConfig;
import com.insightface.sdk.inspireface.base.TypeDefine;
import com.insightface.sdk.inspireface.utils.SDKUtils;
import com.insightface.sdk.inspireface.internal.NativeConversions;
import com.insightface.sdk.inspireface.jni.InspireFaceException;
import com.insightface.sdk.inspireface.jni.Native;
import com.insightface.sdk.inspireface.jni.NativeTypes;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Android convenience API. Serialize access to each session and to global runtime/FeatureHub state.
 * Full C API coverage, including platform-specific controls, is available through {@link Native}.
 */
public class InspireFace extends TypeDefine {
    static {
        System.loadLibrary("InspireFace");
    }

    /**
     * Copy resource file to application directory
     * @param context Context object
     * @return resource file path
     */
    public static String copyResourceFileToApplicationDir(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        File resourceDir = new File(externalFilesDir, INSPIREFACE_IN_ASSETS_FOLDER_NAME);

        String resourceFolderPath = resourceDir.getAbsolutePath();
        SDKUtils.copyFilesFromAssets(context, INSPIREFACE_IN_ASSETS_FOLDER_NAME, resourceFolderPath);

        return resourceFolderPath;
    }

    /**
     * Global launch
     * @param context Context object
     * @param modelName Model name
     * @return true if success, false otherwise
     */
    public static Boolean GlobalLaunch(Context context, String modelName) {
        String folder = InspireFace.copyResourceFileToApplicationDir(context);
        File modelFile = new File(folder, modelName);
        return InspireFace.GlobalLaunch(modelFile.getAbsolutePath());
    }

    /**
     * Create FeatureHubConfiguration object
     * @return FeatureHubConfiguration object
     */
    public static FeatureHubConfiguration CreateFeatureHubConfiguration() {
        return new FeatureHubConfiguration();
    }

    /**
     * Create FeatureHubConfiguration object
     * @param primaryKeyMode Primary key mode(The id increment mode is recommended)
     * @param enablePersistence Flag to enable or disable the use of the database
     * @param persistenceDbPath Path to the database file
     * @param searchThreshold Threshold for face search
     * @param searchMode Mode of face search
     * @return FeatureHubConfiguration object
     */
    public static FeatureHubConfiguration CreateFeatureHubConfiguration(int primaryKeyMode, boolean enablePersistence, String persistenceDbPath, float searchThreshold, int searchMode) {
        FeatureHubConfiguration configuration = new FeatureHubConfiguration();
        configuration.primaryKeyMode = primaryKeyMode;
        configuration.enablePersistence = enablePersistence ? 1 : 0;
        configuration.persistenceDbPath = persistenceDbPath;
        configuration.searchThreshold = searchThreshold;
        configuration.searchMode = searchMode;
        return configuration;
    }

    /**
     * Create CustomParameter object
     * @return CustomParameter object
     */
    public static CustomParameter CreateCustomParameter() {
        return new CustomParameter();
    }

    /**
     * Create CustomParameter object
     * @param enableRecognition Flag to enable or disable face recognition
     * @param enableLiveness Flag to enable or disable liveness detection
     * @param enableIrLiveness Flag to enable or disable IR liveness detection
     * @param enableMaskDetect Flag to enable or disable mask detection
     * @param enableFaceQuality Flag to enable or disable face quality detection
     * @param enableFaceAttribute Flag to enable or disable face attribute detection
     * @param enableInteractionLiveness Flag to enable or disable interaction liveness detection
     * @param enableFacePose Flag to enable or disable face pose estimation (the historical eighth argument)
     * @return CustomParameter object
     */
    public static CustomParameter CreateCustomParameter(boolean enableRecognition, boolean enableLiveness, boolean enableIrLiveness, boolean enableMaskDetect, boolean enableFaceQuality, boolean enableFaceAttribute, boolean enableInteractionLiveness, boolean enableFacePose) {
        CustomParameter parameter = new CustomParameter();
        parameter.enableRecognition = enableRecognition ? 1 : 0;
        parameter.enableLiveness = enableLiveness ? 1 : 0;
        parameter.enableIrLiveness = enableIrLiveness ? 1 : 0;
        parameter.enableMaskDetect = enableMaskDetect ? 1 : 0;
        parameter.enableFaceQuality = enableFaceQuality ? 1 : 0;
        parameter.enableFaceAttribute = enableFaceAttribute ? 1 : 0;
        parameter.enableInteractionLiveness = enableInteractionLiveness ? 1 : 0;
        parameter.enableFacePose = enableFacePose ? 1: 0;
        return parameter;
    }

    /** Create custom parameters including the emotion model option. */
    public static CustomParameter CreateCustomParameter(boolean enableRecognition, boolean enableLiveness,
            boolean enableIrLiveness, boolean enableMaskDetect, boolean enableFaceQuality, boolean enableFaceAttribute,
            boolean enableInteractionLiveness, boolean enableFacePose, boolean enableFaceEmotion) {
        return CreateCustomParameter(enableRecognition, enableLiveness, enableIrLiveness, enableMaskDetect,
                enableFaceQuality, enableFaceAttribute, enableInteractionLiveness, enableFacePose)
                .enableFaceEmotion(enableFaceEmotion);
    }

    /** Create all ten C custom parameter fields, keeping the legacy landmark flag separate from pose. */
    public static CustomParameter CreateCustomParameter(boolean enableRecognition, boolean enableLiveness,
            boolean enableIrLiveness, boolean enableMaskDetect, boolean enableFaceQuality, boolean enableFaceAttribute,
            boolean enableInteractionLiveness, boolean enableDetectModeLandmark, boolean enableFacePose, boolean enableFaceEmotion) {
        return CreateCustomParameter(enableRecognition, enableLiveness, enableIrLiveness, enableMaskDetect,
                enableFaceQuality, enableFaceAttribute, enableInteractionLiveness, enableFacePose, enableFaceEmotion)
                .enableDetectModeLandmark(enableDetectModeLandmark);
    }

    /**
     * Global launch
     * @param resourcePath Path to the model pack file
     * @return true if success, false otherwise
     */
    public static native boolean GlobalLaunch(String resourcePath);

    /**
     * Global reload
     * @param resourcePath Path to the model pack file
     * @return true if success, false otherwise
     */
    public static native boolean GlobalReload(String resourcePath);

    /**
     * Global terminate
     * @return true if success, false otherwise
     */
    public static native boolean GlobalTerminate();

    /**
     * Query launch status
     * @return true if success, false otherwise
     */
    public static native boolean QueryLaunchStatus();

    /**
     * Create session
     * @param customParameter Custom parameter object
     * @param detectMode Detect mode
     * @param maxDetectFaceNum Maximum number of detected faces
     * @param detectPixelLevel Detect pixel level
     * @param trackByDetectModeFPS Track by detect mode FPS
     * @return Session object
     */
    public static Session CreateSession(CustomParameter customParameter, int detectMode, int maxDetectFaceNum, int detectPixelLevel, int trackByDetectModeFPS) {
        if (customParameter == null) return null;
        long[] handle = new long[1];
        long status = Native.HFCreateInspireFaceSession(NativeConversions.toNative(customParameter), detectMode,
                maxDetectFaceNum, detectPixelLevel, trackByDetectModeFPS, handle);
        if (status != 0L) return null;
        Session result = new Session();
        result.handle = handle[0];
        return result;
    }

    /**
     * Release session
     * @param session Session object
     */
    public static void ReleaseSession(Session session) {
        if (session == null) return;
        synchronized (session) {
            if (session.handle == 0L) return;
            InspireFaceException.check(Native.HFReleaseInspireFaceSession(session.handle));
            session.handle = 0L;
        }
    }

    /**
     * Create ImageStream from Bitmap
     * @param bitmap Bitmap object
     * @param rotation Rotation
     * @return ImageStream object
     */
    public static native ImageStream CreateImageStreamFromBitmap(Bitmap bitmap, int rotation);

    /**
     * Create ImageStream from ByteBuffer
     * @param buffer ByteBuffer object
     * @param width Image width
     * @param height Image height
     * @param format Image format
     * @param rotation Image rotation
     * @return ImageStream object
     */
    public static native ImageStream CreateImageStreamFromByteBuffer(byte[] buffer, int width, int height, int format, int rotation);

    /**
     * Write ImageStream to file
     * @param imageStream ImageStream object
     * @param filePath Write sdcard path
     */
    public static native void WriteImageStreamToFile(ImageStream imageStream, String filePath);

    /**
     * Release ImageStream
     * @param imageStream ImageStream object
     */
    public static native void ReleaseImageStream(ImageStream imageStream);

    /**
     * Execute face track
     * @param session Session object
     * @param imageStream ImageStream object
     * @return MultipleFaceData object
     */
    public static MultipleFaceData ExecuteFaceTrack(Session session, ImageStream imageStream) {
        if (session == null || imageStream == null) return null;
        synchronized (session) {
            synchronized (imageStream) {
                if (session.handle == 0L || imageStream.handle == 0L) return null;
                NativeTypes.HFMultipleFaceData output = new NativeTypes.HFMultipleFaceData();
                if (Native.HFExecuteFaceTrack(session.handle, imageStream.handle, output) != 0L) return null;
                return NativeConversions.copyFaces(output);
            }
        }
    }
    
    /**
     * Get face dense landmark from face token
     * @param token FaceBasicToken object
     * @return Point2f array
     */
    public static native Point2f[] GetFaceDenseLandmarkFromFaceToken(FaceBasicToken token);

    /**
     * Extract face feature
     * @param session Session object
     * @param imageStream ImageStream object
     * @param token FaceBasicToken object
     * @return FaceFeature object
     */
    public static native FaceFeature ExtractFaceFeature(Session session, ImageStream imageStream, FaceBasicToken token);

    /**
     * Get face alignment image
     * @param session Session object
     * @param imageStream ImageStream object
     * @param token FaceBasicToken object
     * @return Bitmap object
     */
    public static native Bitmap GetFaceAlignmentImage(Session session, ImageStream imageStream, FaceBasicToken token);

    /**
     * Set track preview size
     * @param session Session object
     * @param previewSize The size of the preview for detection and tracking
     */
    public static native void SetTrackPreviewSize(Session session, int previewSize);

    /**
     * Set filter minimum face pixel size
     * @param session Session object
     * @param minSize Minimum face pixel size
     */
    public static native void SetFilterMinimumFacePixelSize(Session session, int minSize);

    /**
     * Set face detect threshold
     * @param session Session object
     * @param threshold Face detection threshold, not tracking threshold
     */
    public static native void SetFaceDetectThreshold(Session session, float threshold);

    /**
     * Set track mode smooth ratio, default value is 0.025
     * @param session Session object
     * @param ratio Smooth ratio
     */
    public static native void SetTrackModeSmoothRatio(Session session, float ratio);

    /**
     * Set track mode num smooth cache frame, default value is 15
     * @param session Session object
     * @param num Num smooth cache frame
     */
    public static native void SetTrackModeNumSmoothCacheFrame(Session session, int num);

    /**
     * Set track mode detect interval, default value is 20
     * @param session Session object
     * @param interval Detect interval
     */
    public static native void SetTrackModeDetectInterval(Session session, int interval);

    /**
     * Enable feature hub data
     * @param configuration FeatureHubConfiguration object
     * @return true if success, false otherwise
     */
    public static native boolean FeatureHubDataEnable(FeatureHubConfiguration configuration);

    /**
     * Disable feature hub data
     * @return true if success, false otherwise
     */
    public static native boolean FeatureHubDataDisable();
    
    /**
     * Insert feature to feature hub
     * @param featureIdentity FaceFeatureIdentity object
     * @return true if success, false otherwise
     */
    public static native boolean FeatureHubInsertFeature(FaceFeatureIdentity featureIdentity);

    /**
     * Search face feature from feature hub
     * @param searchFeature FaceFeature object
     * @return FaceFeatureIdentity, or null on failure. A successful search without a match returns
     *         id -1, searchConfidence -1, and a null feature.
     */
    public static FaceFeatureIdentity FeatureHubFaceSearch(FaceFeature searchFeature) {
        if (searchFeature == null || searchFeature.data == null) return null;
        NativeTypes.HFFeatureHubSearchResultV2 result = new NativeTypes.HFFeatureHubSearchResultV2();
        if (Native.HFFeatureHubFaceSearchV2(NativeConversions.toNative(searchFeature), result) != 0L) return null;
        FaceFeatureIdentity identity = new FaceFeatureIdentity();
        identity.id = result.id;
        identity.searchConfidence = result.confidence;
        // C API 1.2.4 returns success with no feature when nothing meets the threshold.
        identity.feature = result.found == 0 ? null : NativeConversions.copyFeature(result.feature);
        return identity;
    }

    /**
     * Search face feature from feature hub
     * @param searchFeature FaceFeature object
     * @param topK Top K results
     * @return SearchTopKResults object
     */
    public static native SearchTopKResults FeatureHubFaceSearchTopK(FaceFeature searchFeature, int topK);

    /**
     * Remove face feature from feature hub
     * @param id Face identity
     * @return true if success, false otherwise
     */
    public static native boolean FeatureHubFaceRemove(long id);

    /**
     * Update face feature in feature hub
     * @param featureIdentity FaceFeatureIdentity object
     * @return true if success, false otherwise
     */
    public static native boolean FeatureHubFaceUpdate(FaceFeatureIdentity featureIdentity);

    /**
     * Get face identity from feature hub
     * @param id Face identity
     * @return FaceFeatureIdentity object
     */
    public static native FaceFeatureIdentity FeatureHubGetFaceIdentity(long id);

    /**
     * Get face count in feature hub
     * @return Face count
     */
    public static native int FeatureHubGetFaceCount();

    /**
     * Set face search threshold in feature hub
     * @param threshold Face search threshold, greater than the threshold will be found
     */
    public static native void FeatureHubFaceSearchThresholdSetting(float threshold);

    /**
     * Get feature length
     * @return Feature length, default is 512
     */
    public static native int GetFeatureLength();


    /**
     * Get cosine similarity converter
     * @return SimilarityConverterConfig object
     */
    public static native SimilarityConverterConfig GetCosineSimilarityConverter();

    /**
     * Update cosine similarity converter
     * @param config SimilarityConverterConfig object
     */
    public static native void UpdateCosineSimilarityConverter(SimilarityConverterConfig config);

    /**
     * Convert cosine similarity to percentage
     * @param similarity Cosine similarity
     * @return Percentage similarity
     */
    public static native float CosineSimilarityConvertToPercentage(float similarity);

    /**
     * Get recommended cosine threshold
     * @return Recommended cosine threshold
     */
    public static native float GetRecommendedCosineThreshold();

    /**
     * Face comparison
     * @param feature1 FaceFeature object
     * @param feature2 FaceFeature object
     * @return Comparison result
     */
    public static native float FaceComparison(FaceFeature feature1, FaceFeature feature2);

    /**
     * Multiple face pipeline process
     * @param session Session object
     * @param imageStream ImageStream object
     * @param faces MultipleFaceData object
     * @param customParameter CustomParameter object
     * @return true if success, false otherwise
     */
    public static boolean MultipleFacePipelineProcess(Session session, ImageStream imageStream, MultipleFaceData faces, CustomParameter customParameter) {
        if (session == null || imageStream == null || faces == null || customParameter == null) return false;
        synchronized (session) {
            synchronized (imageStream) {
                if (session.handle == 0L || imageStream.handle == 0L) return false;
                NativeTypes.HFMultipleFaceData input;
                try {
                    input = NativeConversions.pipelineFaces(faces);
                } catch (IllegalArgumentException invalidFaces) {
                    return false;
                }
                return Native.HFMultipleFacePipelineProcess(session.handle, imageStream.handle, input,
                        NativeConversions.toNative(customParameter)) == 0L;
            }
        }
    }

    /**
     * Get FaceQualityConfidence
     * @param session Session object
     * @return FaceQualityConfidence object
     */
    public static native FaceQualityConfidence GetFaceQualityConfidence(Session session);
    
    /**
     * Get RGBLivenessConfidence
     * @param session Session object
     * @return RGBLivenessConfidence object
     */
    public static native RGBLivenessConfidence GetRGBLivenessConfidence(Session session);

    /**
     * Get FaceQualityConfidence
     * @param session Session object
     * @return FaceQualityConfidence object
     */

    public static native FaceMaskConfidence GetFaceMaskConfidence(Session session);

    /**
     * Get FaceInteractionState
     * @param session Session object
     * @return FaceInteractionState object
     */
    public static native FaceInteractionState GetFaceInteractionStateResult(Session session);

    /**
     * Get FaceInteractionActionsResult
     * @param session Session object
     * @return FaceInteractionsActions object
     */
    public static native FaceInteractionsActions GetFaceInteractionActionsResult(Session session);

    /**
     * Get FaceAttributeResult
     * @param session Session object
     * @return FaceAttributeResult object
     */
    public static native FaceAttributeResult GetFaceAttributeResult(Session session);

    /**
     * Get FaceEmotionResult
     * @param session Session object
     * @return FaceEmotionResult object
     */
    public static native FaceEmotionResult GetFaceEmotionResult(Session session);

    /**
     * Query InspireFace version
     * @return InspireFaceVersion object
     */
    public static native InspireFaceVersion QueryInspireFaceVersion();

    /**
     * Set log level
     * @param level Log level
     */
    public static native void SetLogLevel(int level);

    /** Create a session using the versioned C API level 2 descriptor; returns null on native failure. */
    public static Session CreateSessionV2(NativeTypes.HFSessionConfigV2 config) {
        if (config == null) return null;
        long[] handle = new long[1];
        if (Native.HFCreateInspireFaceSessionV2(config, handle) != 0) return null;
        Session session = new Session();
        session.handle = handle[0];
        return session;
    }

    /** Create a V2 session with a combination of NativeConstants.HF_ENABLE_* feature flags. */
    public static Session CreateSessionV2(long featureMask, int detectMode, int maxDetectFaceNum,
                                          int detectPixelLevel, int trackByDetectModeFPS) {
        NativeTypes.HFSessionConfigV2 config = new NativeTypes.HFSessionConfigV2();
        config.featureMask = featureMask;
        config.detectMode = detectMode;
        config.maxDetectFaceNum = maxDetectFaceNum;
        config.detectPixelLevel = detectPixelLevel;
        config.trackByDetectModeFPS = trackByDetectModeFPS;
        return CreateSessionV2(config);
    }

    /** Validate a resource pack without launching it. Throws InspireFaceException on validation failure. */
    public static NativeTypes.HFResourcePackInfo ValidateResourcePack(String resourcePath) {
        NativeTypes.HFResourcePackInfo info = new NativeTypes.HFResourcePackInfo();
        InspireFaceException.check(Native.HFValidateResourcePack(resourcePath, info));
        return info;
    }

    public static int QueryCAPILevel() {
        int[] level = new int[1];
        InspireFaceException.check(Native.HFQueryCAPILevel(level));
        return level[0];
    }

    /** Runtime/component diagnostic text, copied using the native size query. */
    public static String QueryInspireFaceDiagnosticInformation() {
        return queryInformation(false);
    }

    /** Native SDK component versions as semicolon-separated key=value pairs. */
    public static String QueryInspireFaceComponentVersions() {
        return queryInformation(true);
    }

    private static String queryInformation(boolean componentsOnly) {
        int[] required = new int[1];
        InspireFaceException.check(componentsOnly
                ? Native.HFQueryInspireFaceComponentVersions(null, 0, required)
                : Native.HFQueryInspireFaceDiagnosticInformation(null, 0, required));
        ByteBuffer data = NativeConversions.direct(required[0]);
        InspireFaceException.check(componentsOnly
                ? Native.HFQueryInspireFaceComponentVersions(data, data.capacity(), required)
                : Native.HFQueryInspireFaceDiagnosticInformation(data, data.capacity(), required));
        int length = 0;
        while (length < data.capacity() && data.get(length) != 0) length++;
        byte[] text = new byte[length];
        data.get(text);
        return new String(text, StandardCharsets.UTF_8);
    }

    /** Returns an independent copy of all IDs in the enabled FeatureHub, or null on native failure. */
    public static long[] FeatureHubGetExistingIds() {
        NativeTypes.HFFeatureHubExistingIds ids = new NativeTypes.HFFeatureHubExistingIds();
        if (Native.HFFeatureHubGetExistingIds(ids) != 0L) return null;
        return NativeConversions.longs(ids.ids, ids.size);
    }

    /** Five facial key points decoded from an owned token; returns null on invalid input/native failure. */
    public static Point2f[] GetFaceFiveKeyPointsFromFaceToken(FaceBasicToken token) {
        NativeTypes.HFFaceBasicToken input;
        try {
            input = NativeConversions.toNative(token);
        } catch (IllegalArgumentException invalidToken) {
            return null;
        }
        NativeTypes.HPoint2f[] points = new NativeTypes.HPoint2f[5];
        if (Native.HFGetFaceFiveKeyPointsFromFaceToken(input, points, points.length) != 0L) return null;
        Point2f[] result = new Point2f[points.length];
        for (int i = 0; i < points.length; i++) {
            result[i] = new Point2f();
            result[i].x = points[i].x;
            result[i].y = points[i].y;
        }
        return result;
    }

    public static void ClearTrackingFace(Session session) {
        synchronized (session) {
            InspireFaceException.check(Native.HFSessionClearTrackingFace(session.handle));
        }
    }

    public static void SetTrackLostRecoveryMode(Session session, boolean enable) {
        synchronized (session) {
            InspireFaceException.check(Native.HFSessionSetTrackLostRecoveryMode(session.handle, enable ? 1 : 0));
        }
    }

    public static void SetLightTrackConfidenceThreshold(Session session, float threshold) {
        synchronized (session) {
            InspireFaceException.check(Native.HFSessionSetLightTrackConfidenceThreshold(session.handle, threshold));
        }
    }

    public static int GetTrackPreviewSize(Session session) {
        synchronized (session) {
            int[] size = new int[1];
            InspireFaceException.check(Native.HFSessionGetTrackPreviewSize(session.handle, size));
            return size[0];
        }
    }

    /** Returns the pixel levels supported by the current detector resource. */
    public static int[] QuerySupportedPixelLevelsForFaceDetection() {
        NativeTypes.HFFaceDetectPixelList levels = new NativeTypes.HFFaceDetectPixelList();
        InspireFaceException.check(Native.HFQuerySupportedPixelLevelsForFaceDetection(levels));
        return java.util.Arrays.copyOf(levels.pixel_level, levels.size);
    }

    public static void SetLandmarkAugmentationNum(Session session, int num) {
        synchronized (session) {
            InspireFaceException.check(Native.HFSessionSetLandmarkAugmentationNum(session.handle, num));
        }
    }

    /** Disable all native SDK log output. */
    public static void LogDisable() {
        InspireFaceException.check(Native.HFLogDisable());
    }
}
