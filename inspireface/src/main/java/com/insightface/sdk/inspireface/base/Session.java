package com.insightface.sdk.inspireface.base;

public class Session implements AutoCloseable {
    public long handle;

    public boolean isClosed() {
        return handle == 0L;
    }

    @Override
    public void close() {
        com.insightface.sdk.inspireface.InspireFace.ReleaseSession(this);
    }
}
