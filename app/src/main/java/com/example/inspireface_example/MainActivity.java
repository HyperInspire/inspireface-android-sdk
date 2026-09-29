package com.example.inspireface_example;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.insightface.sdk.inspireface.InspireFace;
import com.insightface.sdk.inspireface.base.CustomParameter;
import com.insightface.sdk.inspireface.base.FaceFeature;
import com.insightface.sdk.inspireface.base.FaceFeatureIdentity;
import com.insightface.sdk.inspireface.base.FaceQualityConfidence;
import com.insightface.sdk.inspireface.base.FeatureHubConfiguration;
import com.insightface.sdk.inspireface.base.ImageStream;
import com.insightface.sdk.inspireface.base.InspireFaceVersion;
import com.insightface.sdk.inspireface.base.MultipleFaceData;
import com.insightface.sdk.inspireface.base.Session;
import com.insightface.sdk.inspireface.utils.SDKUtils;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "InspireFace";

    void test() {
        InspireFaceVersion version = InspireFace.QueryInspireFaceVersion();
        Log.i(TAG, "InspireFace native version: " + version.major + "." + version.minor + "." + version.patch);
        if (!InspireFace.GlobalLaunch(this, InspireFace.PIKACHU)) {
            Log.e(TAG, "Failed to launch InspireFace");
            return;
        }
        Session session = null;
        ImageStream stream = null;
        Bitmap image = null;
        boolean hubEnabled = false;
        try {
            CustomParameter parameter = InspireFace.CreateCustomParameter()
                    .enableRecognition(true)
                    .enableFaceQuality(true)
                    .enableFaceAttribute(true)
                    .enableInteractionLiveness(true)
                    .enableLiveness(true)
                    .enableMaskDetect(true)
                    .enableFaceEmotion(true);
            session = InspireFace.CreateSession(parameter, InspireFace.DETECT_MODE_ALWAYS_DETECT, 10, -1, -1);
            if (session == null) throw new IllegalStateException("Failed to create session");
            InspireFace.SetTrackPreviewSize(session, 320);
            InspireFace.SetFaceDetectThreshold(session, 0.5f);
            InspireFace.SetFilterMinimumFacePixelSize(session, 0);
            image = SDKUtils.getImageFromAssetsFile(this, "inspireface/kun.jpg");
            if (image == null) throw new IllegalStateException("Failed to load sample image");
            stream = InspireFace.CreateImageStreamFromBitmap(image, InspireFace.CAMERA_ROTATION_0);
            if (stream == null) throw new IllegalStateException("Failed to create image stream");

            MultipleFaceData faces = InspireFace.ExecuteFaceTrack(session, stream);
            if (faces == null) throw new IllegalStateException("Face detection failed");
            Log.i(TAG, "Face count: " + faces.detectedNum);
            if (faces.detectedNum == 0) return;
            Log.i(TAG, "First face track count: " + faces.trackCounts[0]);

            FaceFeature feature = InspireFace.ExtractFaceFeature(session, stream, faces.tokens[0]);
            if (feature == null) throw new IllegalStateException("Feature extraction failed");
            FeatureHubConfiguration config = InspireFace.CreateFeatureHubConfiguration()
                    .setEnablePersistence(false)
                    .setPersistenceDbPath(new java.io.File(getFilesDir(), "faces.db").getAbsolutePath())
                    .setSearchThreshold(0.42f)
                    .setSearchMode(InspireFace.SEARCH_MODE_EXHAUSTIVE)
                    .setPrimaryKeyMode(InspireFace.PK_AUTO_INCREMENT);
            hubEnabled = InspireFace.FeatureHubDataEnable(config);
            if (!hubEnabled) throw new IllegalStateException("Failed to enable FeatureHub");
            FaceFeatureIdentity identity = FaceFeatureIdentity.create(-1, feature);
            if (!InspireFace.FeatureHubInsertFeature(identity)) {
                throw new IllegalStateException("Failed to insert feature");
            }
            FaceFeatureIdentity match = InspireFace.FeatureHubFaceSearch(feature);
            if (match != null && match.id != -1) {
                Log.i(TAG, "Matched ID: " + match.id + ", confidence: " + match.searchConfidence);
            }
            if (InspireFace.MultipleFacePipelineProcess(session, stream, faces, parameter)) {
                FaceQualityConfidence quality = InspireFace.GetFaceQualityConfidence(session);
                if (quality != null && quality.num > 0) Log.i(TAG, "Quality: " + quality.confidence[0]);
            }
            Bitmap crop = InspireFace.GetFaceAlignmentImage(session, stream, faces.tokens[0]);
            if (crop != null) {
                try {
                    SDKUtils.saveBitmap(getFilesDir().getAbsolutePath(), "crop", crop);
                } finally {
                    crop.recycle();
                }
            }
        } catch (RuntimeException | IOException error) {
            Log.e(TAG, "SDK example failed", error);
        } finally {
            if (stream != null) InspireFace.ReleaseImageStream(stream);
            if (session != null) InspireFace.ReleaseSession(session);
            if (hubEnabled) InspireFace.FeatureHubDataDisable();
            if (image != null) image.recycle();
            InspireFace.GlobalTerminate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //

        test();
    }
}