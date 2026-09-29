package com.insightface.sdk.inspireface.internal;

import com.insightface.sdk.inspireface.base.*;
import com.insightface.sdk.inspireface.jni.NativeTypes.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Internal adapters. Native borrowed outputs must be copied before their owner is changed. */
public final class NativeConversions {
    private NativeConversions() {}

    public static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    private static ByteBuffer view(ByteBuffer source, int count, int elementSize) {
        long bytes = (long) count * elementSize;
        if (count < 0 || bytes > Integer.MAX_VALUE || (bytes > 0 &&
                (source == null || source.remaining() < bytes))) {
            throw new IllegalArgumentException("Native buffer is shorter than its declared element count");
        }
        return source == null ? direct(0) : source.duplicate().order(ByteOrder.nativeOrder());
    }

    public static float[] floats(ByteBuffer source, int count) {
        ByteBuffer input = view(source, count, Float.BYTES);
        float[] result = new float[count];
        input.asFloatBuffer().get(result);
        return result;
    }

    public static int[] ints(ByteBuffer source, int count) {
        ByteBuffer input = view(source, count, Integer.BYTES);
        int[] result = new int[count];
        input.asIntBuffer().get(result);
        return result;
    }

    public static long[] longs(ByteBuffer source, int count) {
        ByteBuffer input = view(source, count, Long.BYTES);
        long[] result = new long[count];
        input.asLongBuffer().get(result);
        return result;
    }

    public static HFFaceBasicToken toNative(FaceBasicToken token) {
        if (token == null || token.size <= 0 || token.data == null || token.data.length < token.size) {
            throw new IllegalArgumentException("Face token must contain its declared number of bytes");
        }
        HFFaceBasicToken result = new HFFaceBasicToken();
        result.size = token.size;
        result.data = direct(token.size);
        result.data.put(token.data, 0, token.size);
        result.data.flip();
        return result;
    }

    public static FaceBasicToken copyToken(HFFaceBasicToken token) {
        if (token == null) throw new IllegalArgumentException("Face token must not be null");
        ByteBuffer data = view(token.data, token.size, 1);
        FaceBasicToken result = new FaceBasicToken();
        result.size = token.size;
        result.data = new byte[token.size];
        data.get(result.data);
        return result;
    }

    public static HFFaceFeature toNative(FaceFeature feature) {
        if (feature == null || feature.data == null || feature.data.length > Integer.MAX_VALUE / Float.BYTES) {
            throw new IllegalArgumentException("Face feature data must not be null or oversized");
        }
        HFFaceFeature result = new HFFaceFeature();
        result.size = feature.data.length;
        result.data = direct(result.size * Float.BYTES);
        result.data.asFloatBuffer().put(feature.data);
        return result;
    }

    public static FaceFeature copyFeature(HFFaceFeature feature) {
        if (feature == null) return null;
        FaceFeature result = new FaceFeature();
        result.data = floats(feature.data, feature.size);
        return result;
    }

    public static HFSessionCustomParameter toNative(CustomParameter parameter) {
        if (parameter == null) throw new IllegalArgumentException("CustomParameter must not be null");
        HFSessionCustomParameter result = new HFSessionCustomParameter();
        result.enable_recognition = parameter.enableRecognition;
        result.enable_liveness = parameter.enableLiveness;
        result.enable_ir_liveness = parameter.enableIrLiveness;
        result.enable_mask_detect = parameter.enableMaskDetect;
        result.enable_face_quality = parameter.enableFaceQuality;
        result.enable_face_attribute = parameter.enableFaceAttribute;
        result.enable_interaction_liveness = parameter.enableInteractionLiveness;
        result.enable_detect_mode_landmark = parameter.enableDetectModeLandmark;
        result.enable_face_pose = parameter.enableFacePose;
        result.enable_face_emotion = parameter.enableFaceEmotion;
        return result;
    }

    public static MultipleFaceData copyFaces(HFMultipleFaceData faces) {
        if (faces == null || faces.detectedNum < 0) throw new IllegalArgumentException("Invalid face result");
        int count = faces.detectedNum;
        if (count > 0 && (faces.rects == null || faces.rects.length < count ||
                faces.tokens == null || faces.tokens.length < count || faces.angles == null)) {
            throw new IllegalArgumentException("Face result arrays are shorter than detectedNum");
        }
        MultipleFaceData result = new MultipleFaceData();
        result.detectedNum = count;
        result.rects = new FaceRect[count];
        result.tokens = new FaceBasicToken[count];
        result.angles = new FaceEulerAngle[count];
        result.trackIds = ints(faces.trackIds, count);
        result.trackCounts = ints(faces.trackCounts, count);
        result.detConfidence = floats(faces.detConfidence, count);
        float[] roll = floats(faces.angles == null ? null : faces.angles.roll, count);
        float[] yaw = floats(faces.angles == null ? null : faces.angles.yaw, count);
        float[] pitch = floats(faces.angles == null ? null : faces.angles.pitch, count);
        for (int i = 0; i < count; i++) {
            HFaceRect source = faces.rects[i];
            FaceRect rect = new FaceRect();
            rect.x = source.x;
            rect.y = source.y;
            rect.width = source.width;
            rect.height = source.height;
            result.rects[i] = rect;
            FaceEulerAngle angle = new FaceEulerAngle();
            angle.roll = roll[i];
            angle.yaw = yaw[i];
            angle.pitch = pitch[i];
            result.angles[i] = angle;
            result.tokens[i] = copyToken(faces.tokens[i]);
        }
        return result;
    }

    /** The C pipeline consumes only tokens; populate the other required JNI descriptors with zeroes. */
    public static HFMultipleFaceData pipelineFaces(MultipleFaceData faces) {
        if (faces == null || faces.detectedNum < 0 || faces.detectedNum > Integer.MAX_VALUE / Float.BYTES ||
                (faces.detectedNum > 0 && (faces.tokens == null || faces.tokens.length < faces.detectedNum))) {
            throw new IllegalArgumentException("Face token array is shorter than detectedNum");
        }
        int count = faces.detectedNum;
        HFMultipleFaceData result = new HFMultipleFaceData();
        result.detectedNum = count;
        result.tokens = new HFFaceBasicToken[count];
        result.rects = new HFaceRect[count];
        ByteBuffer unused = direct(count * Float.BYTES);
        result.trackIds = unused;
        result.trackCounts = unused;
        result.detConfidence = unused;
        result.angles.roll = unused;
        result.angles.yaw = unused;
        result.angles.pitch = unused;
        for (int i = 0; i < count; i++) {
            result.tokens[i] = toNative(faces.tokens[i]);
            result.rects[i] = new HFaceRect();
        }
        return result;
    }
}
