package com.insightface.sdk.inspireface;

import com.insightface.sdk.inspireface.base.FaceCaptureConfig;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

/** Java rejects malformed capture policies before passing them across JNI. */
public class FaceCaptureConfigTest {
    @Test
    public void defaultsAndDisabledTrackFilterAreValid() {
        new FaceCaptureConfig().validate();
        FaceCaptureConfig config = new FaceCaptureConfig();
        config.filterMask = 0;
        config.minTrackCount = 0;
        config.stableDurationMs = 0;
        config.collectDurationMs = 0;
        config.validate();
    }

    @Test
    public void rejectsUnknownFilterAndEnabledZeroTrackCount() {
        FaceCaptureConfig config = new FaceCaptureConfig();
        config.filterMask = 1L << 10;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.filterMask = 1L << 9;
        config.minTrackCount = 0;
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    public void rejectsOutputCountsOutsideNativeCapacity() {
        FaceCaptureConfig config = new FaceCaptureConfig();
        config.outputCount = 0;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.outputCount = 9;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.outputCount = 8;
        config.validate();
    }

    @Test
    public void rejectsInvalidDurationsAndCollectionOrder() {
        FaceCaptureConfig config = new FaceCaptureConfig();
        config.stableDurationMs = -1;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.stableDurationMs = 0;
        config.collectDurationMs = config.maxCollectDurationMs + 1;
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    public void rejectsNanAndInfinityEvenWhenFilterIsDisabled() {
        FaceCaptureConfig config = new FaceCaptureConfig();
        config.filterMask = 0;
        config.minQualityScore = Float.NaN;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.minQualityScore = 0.5f;
        config.maxAbsYaw = Float.POSITIVE_INFINITY;
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    public void rejectsInvertedFaceSizeAndBrightnessRanges() {
        FaceCaptureConfig config = new FaceCaptureConfig();
        config.minFaceWidthRatio = 0.8f;
        config.maxFaceWidthRatio = 0.2f;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.minFaceWidthRatio = 0.1f;
        config.maxFaceWidthRatio = 0.8f;
        config.minBrightnessScore = 0.9f;
        config.maxBrightnessScore = 0.2f;
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
