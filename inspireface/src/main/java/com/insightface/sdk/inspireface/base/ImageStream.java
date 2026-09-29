package com.insightface.sdk.inspireface.base;

public class ImageStream implements AutoCloseable {
    public long handle;

    public boolean isClosed() {
        return handle == 0L;
    }

    @Override
    public synchronized void close() {
        if (handle != 0L) {
            com.insightface.sdk.inspireface.InspireFace.ReleaseImageStream(this);
        }
    }
}
