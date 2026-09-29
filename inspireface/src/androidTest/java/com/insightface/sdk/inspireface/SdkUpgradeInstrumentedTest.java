package com.insightface.sdk.inspireface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.insightface.sdk.inspireface.base.CustomParameter;
import com.insightface.sdk.inspireface.base.FaceCaptureConfig;
import com.insightface.sdk.inspireface.base.FaceCaptureResult;
import com.insightface.sdk.inspireface.base.FaceFeature;
import com.insightface.sdk.inspireface.base.FaceFeatureIdentity;
import com.insightface.sdk.inspireface.base.FeatureHubConfiguration;
import com.insightface.sdk.inspireface.base.ImageStream;
import com.insightface.sdk.inspireface.base.MultipleFaceData;
import com.insightface.sdk.inspireface.base.Session;
import com.insightface.sdk.inspireface.jni.CPUEngine;
import com.insightface.sdk.inspireface.jni.Native;
import com.insightface.sdk.inspireface.jni.NativeTypes.*;
import com.insightface.sdk.inspireface.utils.SDKUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.insightface.sdk.inspireface.jni.NativeConstants.*;
import static org.junit.Assert.*;

/** Real JNI/inference regression tests. Requires an Android device and bundled Pikachu/kun.jpg. */
@RunWith(AndroidJUnit4.class)
public class SdkUpgradeInstrumentedTest {
    private Context context;
    private String modelPath;

    @Before
    public void launch() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        modelPath = InspireFace.copyResourceFileToApplicationDir(context) + "/Pikachu";
        assertTrue(new File(modelPath).isFile());
        assertTrue(InspireFace.GlobalLaunch(modelPath));
        assertTrue(InspireFace.QueryLaunchStatus());
    }

    @After
    public void releaseRuntime() {
        try {
            int[] count = new int[1];
            ok(Native.HFDeBugGetUnreleasedSessionsCount(count));
            assertEquals("Leaked face sessions", 0, count[0]);
            ok(Native.HFDeBugGetUnreleasedStreamsCount(count));
            assertEquals("Leaked image streams", 0, count[0]);
        } finally {
            assertTrue(InspireFace.GlobalTerminate());
        }
    }

    @Test
    public void versionResourceValidationAndErrorMessages() {
        HFInspireFaceVersion version = new HFInspireFaceVersion();
        ok(Native.HFQueryInspireFaceVersion(version));
        assertEquals(1, version.major);
        assertEquals(2, version.minor);
        assertEquals(4, version.patch);
        assertEquals(version.patch, InspireFace.QueryInspireFaceVersion().patch);
        int[] level = new int[1];
        ok(Native.HFQueryCAPILevel(level));
        assertEquals(2, level[0]);
        HFComponentVersion mnn = new HFComponentVersion();
        ok(Native.HFQueryInspireFaceComponentVersion(HF_COMPONENT_MNN, mnn));
        assertEquals(HF_COMPONENT_VERSION_KNOWN, mnn.state);
        assertEquals(3, mnn.major);
        assertEquals(2, mnn.minor);

        HFResourcePackInfo info = new HFResourcePackInfo();
        ok(Native.HFValidateResourcePack(modelPath, info));
        assertTrue(info.modelCount > 0);
        assertTrue(info.archiveFileCount >= info.modelCount);
        assertNotEquals(0, Native.HFValidateResourcePack(modelPath + ".missing", new HFResourcePackInfo()));
        // Validation must not alter the already launched process-wide SDK.
        assertTrue(InspireFace.QueryLaunchStatus());

        int[] required = new int[1];
        ok(Native.HFGetErrorMessage(HERR_INVALID_PARAM, null, 0, required));
        assertTrue(required[0] > 1);
        ByteBuffer text = direct(required[0]);
        ok(Native.HFGetErrorMessage(HERR_INVALID_PARAM, text, text.remaining(), required));
        assertNotEquals(0, text.get(0));
        assertEquals(0, text.get(required[0] - 1));
        ok(Native.HFQueryInspireFaceDiagnosticInformation(null, 0, required));
        assertTrue(required[0] > 1);
        text = direct(required[0]);
        ok(Native.HFQueryInspireFaceDiagnosticInformation(text, text.remaining(), required));
        assertNotEquals(0, text.get(0));

        CPUEngine.PowerMode previous = CPUEngine.getGlobalPowerMode();
        try {
            CPUEngine.setGlobalPowerMode(CPUEngine.PowerMode.NORMAL);
            assertEquals(CPUEngine.PowerMode.NORMAL, CPUEngine.getGlobalPowerMode());
            CPUEngine.setGlobalPowerMode(CPUEngine.PowerMode.HIGH);
            assertEquals(CPUEngine.PowerMode.HIGH, CPUEngine.getGlobalPowerMode());
        } finally {
            CPUEngine.setGlobalPowerMode(previous);
        }
    }

    @Test
    public void legacyRecognitionPipelineAndEmptyFeatureHubSearch() {
        Bitmap image = faceBitmap();
        ImageStream stream = InspireFace.CreateImageStreamFromBitmap(image, InspireFace.CAMERA_ROTATION_0);
        assertNotNull(stream);
        Session session = null;
        boolean hubEnabled = false;
        try {
            CustomParameter options = new CustomParameter().enableRecognition(true).enableFaceQuality(true);
            session = InspireFace.CreateSession(options, InspireFace.DETECT_MODE_ALWAYS_DETECT, 4, 320, -1);
            assertNotNull(session);
            MultipleFaceData faces = InspireFace.ExecuteFaceTrack(session, stream);
            assertNotNull(faces);
            assertTrue("kun.jpg must produce a face", faces.detectedNum > 0);
            assertEquals(faces.detectedNum, faces.tokens.length);
            assertEquals(faces.detectedNum, faces.trackCounts.length);
            assertTrue(faces.tokens[0].data.length > 0);
            assertTrue(InspireFace.GetFaceDenseLandmarkFromFaceToken(faces.tokens[0]).length > 0);
            FaceFeature feature = InspireFace.ExtractFaceFeature(session, stream, faces.tokens[0]);
            assertNotNull(feature);
            assertEquals(InspireFace.GetFeatureLength(), feature.data.length);
            assertEquals(1.0f, InspireFace.FaceComparison(feature, feature), 0.001f);
            assertTrue(InspireFace.MultipleFacePipelineProcess(session, stream, faces,
                    new CustomParameter().enableFaceQuality(true)));
            assertEquals(faces.detectedNum, InspireFace.GetFaceQualityConfidence(session).num);

            FeatureHubConfiguration hub = new FeatureHubConfiguration()
                    .setPrimaryKeyMode(InspireFace.PK_MANUAL_INPUT).setSearchThreshold(0.5f);
            assertTrue(InspireFace.FeatureHubDataEnable(hub));
            hubEnabled = true;
            assertEquals(0, InspireFace.FeatureHubGetFaceCount());
            FaceFeatureIdentity missing = InspireFace.FeatureHubFaceSearch(feature);
            assertNotNull("A successful empty search returns an explicit no-match identity", missing);
            assertEquals(-1L, missing.id);
            assertNull(missing.feature);
            long id = (1L << 40) + 23;
            assertTrue(InspireFace.FeatureHubInsertFeature(FaceFeatureIdentity.create(id, feature)));
            assertEquals(1, InspireFace.FeatureHubGetFaceCount());
            FaceFeatureIdentity match = InspireFace.FeatureHubFaceSearch(feature);
            assertNotNull(match);
            assertEquals(id, match.id);
            assertEquals(1.0f, match.searchConfidence, 0.001f);
            assertEquals(id, InspireFace.FeatureHubFaceSearchTopK(feature, 1).ids[0]);
            assertTrue(InspireFace.FeatureHubFaceUpdate(FaceFeatureIdentity.create(id, feature)));
            assertEquals(id, InspireFace.FeatureHubGetFaceIdentity(id).id);
            assertTrue(InspireFace.FeatureHubFaceRemove(id));
            assertEquals(0, InspireFace.FeatureHubGetFaceCount());
        } finally {
            if (hubEnabled) assertTrue(InspireFace.FeatureHubDataDisable());
            if (session != null) InspireFace.ReleaseSession(session);
            InspireFace.ReleaseImageStream(stream);
            image.recycle();
        }
    }

    @Test
    public void nativeFeatureOwnershipAndV2ExplicitMatch() {
        HFFeatureHubConfiguration config = new HFFeatureHubConfiguration();
        config.primaryKeyMode = HF_PK_MANUAL_INPUT;
        config.persistenceDbPath = "";
        config.searchMode = HF_SEARCH_MODE_EXHAUSTIVE;
        config.searchThreshold = 0.5f;
        ok(Native.HFFeatureHubDataEnable(config));
        HFFaceFeature feature = new HFFaceFeature();
        boolean featureOwned = false;
        try {
            ok(Native.HFCreateFaceFeature(feature));
            featureOwned = true;
            int[] dimension = new int[1];
            ok(Native.HFGetFeatureLength(dimension));
            assertEquals(dimension[0], feature.size);
            assertTrue(feature.data.isDirect());
            feature.data.order(ByteOrder.nativeOrder());
            for (int i = 0; i < feature.size; i++) feature.data.putFloat(i * 4, i == 0 ? 1f : 0f);
            HFFeatureHubSearchResultV2 result = new HFFeatureHubSearchResultV2();
            ok(Native.HFFeatureHubFaceSearchV2(feature, result));
            assertEquals(0, result.found);
            HFFaceFeatureIdentity identity = new HFFaceFeatureIdentity();
            identity.id = (1L << 40) + 29;
            identity.feature = feature;
            long[] allocated = new long[1];
            ok(Native.HFFeatureHubInsertFeature(identity, allocated));
            assertEquals(identity.id, allocated[0]);
            ok(Native.HFFeatureHubFaceSearchV2(feature, result));
            assertEquals(1, result.found);
            assertEquals(identity.id, result.id);
            assertEquals(1f, result.confidence, 0.001f);
            HFFeatureHubExistingIds ids = new HFFeatureHubExistingIds();
            ok(Native.HFFeatureHubGetExistingIds(ids));
            assertEquals(1, ids.size);
            assertEquals(identity.id, ids.ids.order(ByteOrder.nativeOrder()).getLong(0));
            ok(Native.HFFeatureHubFaceRemove(identity.id));
            ok(Native.HFFeatureHubFaceSearchV2(feature, result));
            assertEquals(0, result.found);
        } finally {
            if (featureOwned) ok(Native.HFReleaseFaceFeature(feature));
            ok(Native.HFFeatureHubDataDisable());
        }
    }

    @Test
    public void nativeSnapshotOutlivesItsSessionAndStream() {
        Bitmap image = faceBitmap();
        ImageStream stream = InspireFace.CreateImageStreamFromBitmap(image, InspireFace.CAMERA_ROTATION_0);
        assertNotNull(stream);
        long[] session = new long[1];
        long[] snapshot = new long[1];
        try {
            HFSessionConfigV2 config = new HFSessionConfigV2();
            config.detectMode = HF_DETECT_MODE_ALWAYS_DETECT;
            config.maxDetectFaceNum = 4;
            config.detectPixelLevel = 320;
            config.trackByDetectModeFPS = -1;
            ok(Native.HFCreateInspireFaceSessionV2(config, session));
            ok(Native.HFExecuteFaceTrackSnapshot(session[0], stream.handle, snapshot));
            HFMultipleFaceData before = new HFMultipleFaceData();
            ok(Native.HFGetFaceResultSnapshotData(snapshot[0], before));
            assertTrue(before.detectedNum > 0);
            assertNotNull(before.trackCounts);
            byte[] token = bytes(before.tokens[0].data, before.tokens[0].size);
            ok(Native.HFExecuteFaceTrack(session[0], stream.handle, new HFMultipleFaceData()));
            ok(Native.HFReleaseInspireFaceSession(session[0]));
            session[0] = 0;
            InspireFace.ReleaseImageStream(stream);
            stream = null;
            HFMultipleFaceData after = new HFMultipleFaceData();
            ok(Native.HFGetFaceResultSnapshotData(snapshot[0], after));
            assertEquals(before.detectedNum, after.detectedNum);
            assertArrayEquals(token, bytes(after.tokens[0].data, after.tokens[0].size));
            assertEquals(before.rects[0].width, after.rects[0].width);
        } finally {
            if (snapshot[0] != 0) ok(Native.HFReleaseFaceResultSnapshot(snapshot[0]));
            if (session[0] != 0) ok(Native.HFReleaseInspireFaceSession(session[0]));
            if (stream != null) InspireFace.ReleaseImageStream(stream);
            image.recycle();
        }
    }

    @Test
    public void captureWithAndWithoutSnapshotsResetsAndCloses() {
        Bitmap image = faceBitmap();
        ImageStream stream = InspireFace.CreateImageStreamFromBitmap(image, InspireFace.CAMERA_ROTATION_0);
        assertNotNull(stream);
        Session session = InspireFace.CreateSession(new CustomParameter(), InspireFace.DETECT_MODE_LIGHT_TRACK, 4, 320, -1);
        assertNotNull(session);
        try {
            FaceCaptureConfig config = FaceCapture.defaultConfig();
            config.filterMask = FaceCapture.FILTER_NONE;
            config.minTrackCount = 0;
            config.stableDurationMs = 0;
            config.collectDurationMs = 0;
            config.minCandidateIntervalMs = 0;
            FaceCapture capture = FaceCapture.create(session, config);
            try {
                FaceDetectionSnapshot snapshot = FaceDetectionSnapshot.create(session, stream);
                try {
                    MultipleFaceData faces = snapshot.getFaces();
                    assertTrue(faces.detectedNum > 0);
                    assertEquals(faces.detectedNum, faces.trackCounts.length);
                    capture.update(stream, snapshot, 1, 1000);
                } finally {
                    snapshot.close();
                    snapshot.close();
                    assertTrue(snapshot.isClosed());
                }
                capture.finish();
                FaceCaptureResult[] results = capture.getResults();
                assertEquals(1, results.length);
                assertEquals(1L, results[0].frameId);
                assertTrue(results[0].token.length > 0);
                byte[] copiedToken = results[0].token.clone();
                capture.reset();
                assertEquals(0, capture.getResults().length);
                // Facade results own their copied token, independent of the native capture view.
                assertArrayEquals(copiedToken, results[0].token);
                capture.update(stream, 2, 2000);
                capture.finish();
                assertEquals(1, capture.getResults().length);
            } finally {
                capture.close();
                capture.close();
                assertTrue(capture.isClosed());
            }
        } finally {
            InspireFace.ReleaseSession(session);
            InspireFace.ReleaseImageStream(stream);
            image.recycle();
        }
    }

    @Test
    public void nativeCaptureDescriptorsReturnOwnedSnapshotCandidate() {
        Bitmap image = faceBitmap();
        ImageStream stream = InspireFace.CreateImageStreamFromBitmap(image, InspireFace.CAMERA_ROTATION_0);
        assertNotNull(stream);
        Session session = InspireFace.CreateSession(new CustomParameter(), InspireFace.DETECT_MODE_LIGHT_TRACK, 4, 320, -1);
        assertNotNull(session);
        long[] capture = new long[1];
        long[] snapshot = new long[1];
        try {
            HFFaceCaptureConfig config = new HFFaceCaptureConfig();
            ok(Native.HFGetDefaultFaceCaptureConfig(config));
            config.filterMask = HF_CAPTURE_FILTER_NONE;
            config.minTrackCount = 0;
            config.stableDurationMs = 0;
            config.collectDurationMs = 0;
            config.minCandidateIntervalMs = 0;
            ok(Native.HFCreateFaceCaptureSession(session.handle, config, capture));
            ok(Native.HFExecuteFaceTrackSnapshot(session.handle, stream.handle, snapshot));
            HFFaceCaptureProgress progress = new HFFaceCaptureProgress();
            ok(Native.HFUpdateFaceCaptureSessionWithSnapshot(capture[0], stream.handle,
                    snapshot[0], 10, 1000, progress));
            assertEquals(10L, progress.frameId);
            assertEquals(1000L, progress.timestampMs);
            assertEquals(HF_CAPTURE_REJECT_NONE, progress.rejectReasons);
            int[] count = new int[1];
            ok(Native.HFGetFaceCaptureResults(capture[0], null, 0, count));
            assertEquals(1, count[0]);
            HFFaceCaptureResult[] results = {new HFFaceCaptureResult()};
            ok(Native.HFGetFaceCaptureResults(capture[0], results, results.length, count));
            assertEquals(10L, results[0].frameId);
            assertTrue(results[0].rect.width > 0);
            assertTrue(results[0].token.size > 0);
            assertTrue(results[0].token.data.isDirect());
            ok(Native.HFFinishFaceCaptureSession(capture[0], progress));
            assertEquals(HF_CAPTURE_STATE_FINISHED, progress.state);
            ok(Native.HFResetFaceCaptureSession(capture[0]));
            ok(Native.HFGetFaceCaptureResults(capture[0], null, 0, count));
            assertEquals(0, count[0]);
            ok(Native.HFUpdateFaceCaptureSession(capture[0], stream.handle, 11, 2000, progress));
            assertEquals(HF_CAPTURE_REJECT_NONE, progress.rejectReasons);
            ok(Native.HFGetFaceCaptureResults(capture[0], null, 0, count));
            assertEquals(1, count[0]);
        } finally {
            if (snapshot[0] != 0) ok(Native.HFReleaseFaceResultSnapshot(snapshot[0]));
            if (capture[0] != 0) ok(Native.HFReleaseFaceCaptureSession(capture[0]));
            InspireFace.ReleaseSession(session);
            InspireFace.ReleaseImageStream(stream);
            image.recycle();
        }
    }

    @Test
    public void rejectsUnsafeNativeBuffersBeforeCreatingResources() {
        HFImageData data = new HFImageData();
        data.width = 4;
        data.height = 4;
        data.format = HF_STREAM_RGB;
        long[] handle = new long[1];
        try {
            data.data = ByteBuffer.allocate(48);
            assertThrows(IllegalArgumentException.class, () -> Native.HFCreateImageStream(data, handle));
            data.data = direct(48).asReadOnlyBuffer();
            assertThrows(IllegalArgumentException.class, () -> Native.HFCreateImageStream(data, handle));
            data.data = direct(47);
            assertThrows(IllegalArgumentException.class, () -> Native.HFCreateImageStream(data, handle));
            data.data = direct(48);
            data.format = HF_STREAM_YUV_NV21;
            data.width = 3;
            assertThrows(IllegalArgumentException.class, () -> Native.HFCreateImageStream(data, handle));
            assertEquals(0L, handle[0]);
        } finally {
            if (handle[0] != 0) ok(Native.HFReleaseImageStream(handle[0]));
        }
    }

    @Test
    public void rgbBgrAndGrayPixelsRoundTripWithoutSwappingChannels() {
        assertDecodedPixel(HF_STREAM_RGB, new byte[]{(byte) 255, 0, 0}, 0, 0, 255);
        assertDecodedPixel(HF_STREAM_BGR, new byte[]{0, 0, (byte) 255}, 0, 0, 255);
        assertDecodedPixel(HF_STREAM_RGBA, new byte[]{(byte) 255, 0, 0, (byte) 255}, 0, 0, 255);
        assertDecodedPixel(HF_STREAM_BGRA, new byte[]{0, 0, (byte) 255, (byte) 255}, 0, 0, 255);
        assertDecodedPixel(HF_STREAM_GRAY, new byte[]{73}, 73, 73, 73);

        Bitmap bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);
        ImageStream stream = InspireFace.CreateImageStreamFromBitmap(bitmap, InspireFace.CAMERA_ROTATION_0);
        assertNotNull(stream);
        long[] decoded = new long[1];
        try {
            ok(Native.HFCreateImageBitmapFromImageStreamProcess(stream.handle, decoded, 0, 1f));
            assertBgr(decoded[0], 0, 0, 255);
        } finally {
            if (decoded[0] != 0) ok(Native.HFReleaseImageBitmap(decoded[0]));
            InspireFace.ReleaseImageStream(stream);
            bitmap.recycle();
        }
    }

    private void assertDecodedPixel(int format, byte[] pixel, int b, int g, int r) {
        HFImageData data = new HFImageData();
        data.width = 4;
        data.height = 4;
        data.format = format;
        data.data = direct(16 * pixel.length + 8);
        // JNI must respect position/limit, not blindly read the direct-buffer base.
        data.data.position(8);
        for (int i = 0; i < 16; i++) data.data.put(pixel);
        data.data.position(8);
        long[] stream = new long[1];
        long[] bitmap = new long[1];
        long[] ownedStream = new long[1];
        long[] copied = new long[1];
        try {
            ok(Native.HFCreateImageStream(data, stream));
            ok(Native.HFCreateImageBitmapFromImageStreamProcess(stream[0], bitmap, 0, 1f));
            assertBgr(bitmap[0], b, g, r);
            ok(Native.HFCreateImageStreamFromImageBitmap(bitmap[0], 0, ownedStream));
            ok(Native.HFReleaseImageBitmap(bitmap[0]));
            bitmap[0] = 0;
            // The bitmap-derived stream must own pixels after its source bitmap is gone.
            ok(Native.HFCreateImageBitmapFromImageStreamProcess(ownedStream[0], copied, 0, 1f));
            assertBgr(copied[0], b, g, r);
        } finally {
            if (copied[0] != 0) ok(Native.HFReleaseImageBitmap(copied[0]));
            if (ownedStream[0] != 0) ok(Native.HFReleaseImageStream(ownedStream[0]));
            if (bitmap[0] != 0) ok(Native.HFReleaseImageBitmap(bitmap[0]));
            if (stream[0] != 0) ok(Native.HFReleaseImageStream(stream[0]));
        }
    }

    private static void assertBgr(long bitmap, int b, int g, int r) {
        HFImageBitmapData data = new HFImageBitmapData();
        ok(Native.HFImageBitmapGetData(bitmap, data));
        assertEquals(4, data.width);
        assertEquals(4, data.height);
        assertEquals(3, data.channels);
        assertEquals(b, data.data.get(0) & 255);
        assertEquals(g, data.data.get(1) & 255);
        assertEquals(r, data.data.get(2) & 255);
    }

    private Bitmap faceBitmap() {
        Bitmap bitmap = SDKUtils.getImageFromAssetsFile(context, "inspireface/kun.jpg");
        assertNotNull(bitmap);
        return bitmap;
    }

    private static byte[] bytes(ByteBuffer buffer, int count) {
        byte[] output = new byte[count];
        buffer.duplicate().get(output);
        return output;
    }

    private static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    private static void ok(long status) {
        assertEquals("Native status", HSUCCEED, status);
    }
}
