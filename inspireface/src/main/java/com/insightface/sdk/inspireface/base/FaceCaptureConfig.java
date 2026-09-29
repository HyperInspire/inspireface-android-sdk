package com.insightface.sdk.inspireface.base;

/** Mutable configuration for the synchronous face-capture policy. */
public final class FaceCaptureConfig {
    public long filterMask = 31L | (1L << 9);
    public int outputCount = 1;
    public int minTrackCount = 5;
    public long stableDurationMs = 300L;
    public long collectDurationMs = 800L;
    public long maxCollectDurationMs = 3000L;
    public long trackLostGraceMs = 300L;
    public long minCandidateIntervalMs = 150L;
    public float minFaceWidthRatio = 0.12F;
    public float maxFaceWidthRatio = 0.75F;
    public float maxCenterOffsetX = 0.25F;
    public float maxCenterOffsetY = 0.25F;
    public float boundaryMarginRatio = 0.02F;
    public float maxCenterMotionRatio = 0.025F;
    public float maxSizeChangeRatio = 0.08F;
    public float maxAbsYaw = 25.0F;
    public float maxAbsPitch = 25.0F;
    public float maxAbsRoll = 20.0F;
    public float minQualityScore = 0.60F;
    public float minSharpnessScore = 0.03F;
    public float minBrightnessScore = 0.15F;
    public float maxBrightnessScore = 0.90F;

    public FaceCaptureConfig() {}

    public void validate() {
        if ((filterMask & ~1023L) != 0L || outputCount <= 0 || outputCount > 8
                || minTrackCount < 0
                || ((filterMask & (1L << 9)) != 0L && minTrackCount == 0)
                || stableDurationMs < 0L || collectDurationMs < 0L
                || maxCollectDurationMs < 0L || trackLostGraceMs < 0L
                || minCandidateIntervalMs < 0L || maxCollectDurationMs < collectDurationMs) {
            throw new IllegalArgumentException("Invalid face capture integer configuration");
        }
        if (!unitRange(minFaceWidthRatio) || !unitRange(maxFaceWidthRatio)
                || minFaceWidthRatio > maxFaceWidthRatio
                || !unitRange(maxCenterOffsetX) || !unitRange(maxCenterOffsetY)
                || !unitRange(boundaryMarginRatio) || boundaryMarginRatio >= 0.5F
                || !unitRange(maxCenterMotionRatio) || !unitRange(maxSizeChangeRatio)
                || !nonNegative(maxAbsYaw) || !nonNegative(maxAbsPitch) || !nonNegative(maxAbsRoll)
                || !unitRange(minQualityScore) || !unitRange(minSharpnessScore)
                || !unitRange(minBrightnessScore) || !unitRange(maxBrightnessScore)
                || minBrightnessScore > maxBrightnessScore) {
            throw new IllegalArgumentException("Invalid face capture floating-point configuration");
        }
    }

    private static boolean unitRange(float value) {
        return nonNegative(value) && value <= 1.0F;
    }

    private static boolean nonNegative(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value) && value >= 0.0F;
    }
}
