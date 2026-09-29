package com.insightface.sdk.inspireface.base;


public class CustomParameter {
    public int enableRecognition = 0;               // Enable face recognition
    public int enableLiveness = 0;                  // Enable liveness detection
    public int enableIrLiveness = 0;                // Enable IR liveness detection
    public int enableMaskDetect = 0;                // Enable mask detection
    public int enableFaceQuality = 0;               // Enable face quality detection
    public int enableFaceAttribute = 0;             // Enable face attribute detection
    public int enableInteractionLiveness = 0;       // Enable interaction liveness detection
    /** Legacy C option, retained independently from pose; current engines may ignore this flag. */
    public int enableDetectModeLandmark = 0;
    public int enableFacePose = 0;                  // Enable face pose detection
    public int enableFaceEmotion = 0;                   // Enable face emotion prediction

    public CustomParameter() {}

    /** Set the legacy detection-mode landmark flag independently from face pose. */
    public CustomParameter enableDetectModeLandmark(boolean enable) {
        this.enableDetectModeLandmark = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable face attribute detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */
    public CustomParameter enableFaceAttribute(boolean enable) {
        this.enableFaceAttribute = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable interaction liveness detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */
    public CustomParameter enableInteractionLiveness(boolean enable) {
        this.enableInteractionLiveness = enable ? 1 : 0;
        return this;
    }   
    
    /**
     * Enable mask detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */
    public CustomParameter enableMaskDetect(boolean enable) {
        this.enableMaskDetect = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable face quality detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */ 
    public CustomParameter enableFaceQuality(boolean enable) {
        this.enableFaceQuality = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable liveness detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */ 
    public CustomParameter enableLiveness(boolean enable) {
        this.enableLiveness = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable face recognition
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */  
    public CustomParameter enableRecognition(boolean enable) {
        this.enableRecognition = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable IR liveness detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */  
    public CustomParameter enableIrLiveness(boolean enable) {
        this.enableIrLiveness = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable face pose detection
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */  
    public CustomParameter enableFacePose(boolean enable) {
        this.enableFacePose = enable ? 1 : 0;
        return this;
    }

    /**
     * Enable face emotion prediction
     * @param enable true if enable, false otherwise
     * @return CustomParameter object
     */
    public CustomParameter enableFaceEmotion(boolean enable) {
        this.enableFaceEmotion = enable ? 1 : 0;
        return this;
    }


}
