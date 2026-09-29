package com.insightface.sdk.inspireface;

import com.insightface.sdk.inspireface.base.ImageStream;
import com.insightface.sdk.inspireface.base.Session;
import com.insightface.sdk.inspireface.base.MultipleFaceData;
import com.insightface.sdk.inspireface.internal.NativeConversions;
import com.insightface.sdk.inspireface.jni.Native;
import com.insightface.sdk.inspireface.jni.NativeTypes.HFMultipleFaceData;
import com.insightface.sdk.inspireface.jni.InspireFaceException;

/** Owned detection snapshot for reusing one tracking result in capture assessment. */
public final class FaceDetectionSnapshot implements AutoCloseable {
    static {
        System.loadLibrary("InspireFace");
    }

    private long nativeHandle;

    private FaceDetectionSnapshot() {}

    public static FaceDetectionSnapshot create(Session session, ImageStream stream) {
        if (session == null || session.handle == 0L || stream == null || stream.handle == 0L) {
            throw new IllegalArgumentException("Session and ImageStream must be open");
        }
        FaceDetectionSnapshot snapshot = new FaceDetectionSnapshot();
        int status = nativeCreate(session.handle, stream.handle, snapshot);
        FaceCapture.throwIfError(status, "Create face detection snapshot");
        return snapshot;
    }

    synchronized long requireHandle() {
        if (nativeHandle == 0L) {
            throw new IllegalStateException("FaceDetectionSnapshot is closed");
        }
        return nativeHandle;
    }

    public synchronized boolean isClosed() {
        return nativeHandle == 0L;
    }

    /** Copies the snapshot, including tokens and track counts, into independently owned arrays. */
    public synchronized MultipleFaceData getFaces() {
        HFMultipleFaceData data = new HFMultipleFaceData();
        InspireFaceException.check(Native.HFGetFaceResultSnapshotData(requireHandle(), data));
        return NativeConversions.copyFaces(data);
    }

    @Override
    public synchronized void close() {
        if (nativeHandle == 0L) {
            return;
        }
        int status = nativeRelease(nativeHandle);
        FaceCapture.throwIfError(status, "Release face detection snapshot");
        nativeHandle = 0L;
    }

    private static native int nativeCreate(long sessionHandle, long streamHandle,
                                           FaceDetectionSnapshot output);
    private static native int nativeRelease(long snapshotHandle);
}
