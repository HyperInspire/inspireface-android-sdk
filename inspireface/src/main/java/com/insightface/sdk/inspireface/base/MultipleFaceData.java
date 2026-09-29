package com.insightface.sdk.inspireface.base;

public class MultipleFaceData {
    public int detectedNum;
    public FaceRect[] rects;
    public int[] trackIds;
    /** Number of frames tracked for each face (added in SDK 1.2.4). */
    public int[] trackCounts;
    public float[] detConfidence;
    public FaceEulerAngle[] angles;
    public FaceBasicToken[] tokens;

    public MultipleFaceData() {}
}
