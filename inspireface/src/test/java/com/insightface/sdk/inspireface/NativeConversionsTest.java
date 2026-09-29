package com.insightface.sdk.inspireface;

import com.insightface.sdk.inspireface.base.CustomParameter;
import com.insightface.sdk.inspireface.base.FaceBasicToken;
import com.insightface.sdk.inspireface.base.MultipleFaceData;
import com.insightface.sdk.inspireface.internal.NativeConversions;
import com.insightface.sdk.inspireface.jni.NativeTypes;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

/** Host-side regressions for the Java/native ownership boundary; no native library is loaded. */
public class NativeConversionsTest {
    @Test
    public void borrowedFaceResultIsDeepCopiedIncludingTrackCountsAndToken() {
        NativeTypes.HFMultipleFaceData nativeFaces = new NativeTypes.HFMultipleFaceData();
        nativeFaces.detectedNum = 1;
        NativeTypes.HFaceRect rect = new NativeTypes.HFaceRect();
        rect.x = 9;
        rect.width = 40;
        nativeFaces.rects = new NativeTypes.HFaceRect[] { rect };
        nativeFaces.trackIds = NativeConversions.direct(4).putInt(0, 27);
        nativeFaces.trackCounts = NativeConversions.direct(4).putInt(0, 81);
        nativeFaces.detConfidence = NativeConversions.direct(4).putFloat(0, 0.9f);
        nativeFaces.angles.roll = NativeConversions.direct(4).putFloat(0, 3.0f);
        nativeFaces.angles.yaw = NativeConversions.direct(4).putFloat(0, 4.0f);
        nativeFaces.angles.pitch = NativeConversions.direct(4).putFloat(0, 5.0f);
        NativeTypes.HFFaceBasicToken token = new NativeTypes.HFFaceBasicToken();
        token.size = 3;
        token.data = NativeConversions.direct(3);
        token.data.put(new byte[] { 4, 5, 6 }).flip();
        nativeFaces.tokens = new NativeTypes.HFFaceBasicToken[] { token };

        MultipleFaceData copy = NativeConversions.copyFaces(nativeFaces);
        rect.x = 0;
        nativeFaces.trackCounts.putInt(0, -1);
        nativeFaces.angles.yaw.putFloat(0, -1.0f);
        token.data.put(0, (byte) 0);

        assertEquals(9, copy.rects[0].x);
        assertEquals(40, copy.rects[0].width);
        assertArrayEquals(new int[] { 27 }, copy.trackIds);
        assertArrayEquals(new int[] { 81 }, copy.trackCounts);
        assertEquals(0.9f, copy.detConfidence[0], 0.0f);
        assertEquals(3.0f, copy.angles[0].roll, 0.0f);
        assertEquals(4.0f, copy.angles[0].yaw, 0.0f);
        assertEquals(5.0f, copy.angles[0].pitch, 0.0f);
        assertArrayEquals(new byte[] { 4, 5, 6 }, copy.tokens[0].data);
        assertEquals(0, token.data.position());
    }

    @Test
    public void borrowedBuffersHonorPositionLimitAndNativeOrderWithoutChangingSource() {
        ByteBuffer data = NativeConversions.direct(16);
        data.putLong(0, 100L);
        data.putLong(8, 0x1122334455667788L);
        data.position(8);
        data.order(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        assertArrayEquals(new long[] { 0x1122334455667788L }, NativeConversions.longs(data, 1));
        assertEquals(8, data.position());
        data.limit(12);
        assertThrows(IllegalArgumentException.class, () -> NativeConversions.longs(data, 1));
    }

    @Test
    public void noFacesProducesUsableEmptyArrays() {
        MultipleFaceData faces = NativeConversions.copyFaces(new NativeTypes.HFMultipleFaceData());
        assertEquals(0, faces.detectedNum);
        assertEquals(0, faces.tokens.length);
        assertEquals(0, faces.trackCounts.length);
        assertEquals(0, faces.angles.length);
        assertEquals(0, NativeConversions.pipelineFaces(faces).detectedNum);
    }

    @Test
    public void tokenInputsAreOwnedDirectCopiesAndRejectTruncation() {
        FaceBasicToken token = new FaceBasicToken();
        token.size = 2;
        token.data = new byte[] { 7, 8, 9 };
        NativeTypes.HFFaceBasicToken nativeToken = NativeConversions.toNative(token);
        token.data[0] = 0;
        assertTrue(nativeToken.data.isDirect());
        assertEquals(2, nativeToken.data.remaining());
        assertEquals(7, nativeToken.data.get(0));
        token.size = 4;
        assertThrows(IllegalArgumentException.class, () -> NativeConversions.toNative(token));
    }

    @Test
    public void pipelineAcceptsLegacyTokenOnlyFaceLists() {
        MultipleFaceData faces = new MultipleFaceData();
        faces.detectedNum = 1;
        FaceBasicToken token = new FaceBasicToken();
        token.size = 2;
        token.data = new byte[] { 1, 2 };
        faces.tokens = new FaceBasicToken[] { token };
        NativeTypes.HFMultipleFaceData nativeFaces = NativeConversions.pipelineFaces(faces);
        assertEquals(1, nativeFaces.detectedNum);
        assertNotNull(nativeFaces.rects[0]);
        assertTrue(nativeFaces.trackCounts.isDirect());
        assertEquals(4, nativeFaces.angles.yaw.remaining());
        assertArrayEquals(new byte[] { 1, 2 }, NativeConversions.copyToken(nativeFaces.tokens[0]).data);
    }

    @Test
    public void allCustomParameterFieldsAreMappedIndependently() {
        CustomParameter parameter = new CustomParameter();
        parameter.enableRecognition = 1;
        parameter.enableLiveness = 2;
        parameter.enableIrLiveness = 3;
        parameter.enableMaskDetect = 4;
        parameter.enableFaceQuality = 5;
        parameter.enableFaceAttribute = 6;
        parameter.enableInteractionLiveness = 7;
        parameter.enableDetectModeLandmark = 8;
        parameter.enableFacePose = 9;
        parameter.enableFaceEmotion = 10;
        NativeTypes.HFSessionCustomParameter output = NativeConversions.toNative(parameter);
        assertEquals(1, output.enable_recognition);
        assertEquals(2, output.enable_liveness);
        assertEquals(3, output.enable_ir_liveness);
        assertEquals(4, output.enable_mask_detect);
        assertEquals(5, output.enable_face_quality);
        assertEquals(6, output.enable_face_attribute);
        assertEquals(7, output.enable_interaction_liveness);
        assertEquals(8, output.enable_detect_mode_landmark);
        assertEquals(9, output.enable_face_pose);
        assertEquals(10, output.enable_face_emotion);
    }
}
