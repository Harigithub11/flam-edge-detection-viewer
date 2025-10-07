#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <chrono>
#include "ImageProcessor.h"

#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Original test functions
extern "C" JNIEXPORT jstring JNICALL
Java_com_flam_edgeviewer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Native library loaded successfully!";
    LOGD("JNI function called: %s", hello.c_str());
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_flam_edgeviewer_MainActivity_getOpenCVVersion(
        JNIEnv* env,
        jobject /* this */) {

    std::string version = "OpenCV " + std::string(CV_VERSION);
    LOGD("OpenCV version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_flam_edgeviewer_MainActivity_testNativeProcessing(
        JNIEnv* env,
        jobject /* this */,
        jint inputValue) {

    LOGD("Test processing called with value: %d", inputValue);
    jint result = inputValue * 2;
    LOGD("Test processing result: %d", result);
    return result;
}

// FrameProcessor JNI functions
extern "C" JNIEXPORT jboolean JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_initializeProcessor(
        JNIEnv* env,
        jobject /* this */) {

    LOGD("Initializing native processor");
    bool success = ImageProcessor::initialize();
    return static_cast<jboolean>(success);
}

extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_releaseProcessor(
        JNIEnv* env,
        jobject /* this */) {

    LOGD("Releasing native processor");
    ImageProcessor::release();
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_processFrameNative(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray inputFrame,
        jint width,
        jint height,
        jint mode) {

    // Performance timing
    auto startTime = std::chrono::high_resolution_clock::now();

    // BEST PRACTICE: Get array size BEFORE critical section (no JNI calls allowed inside)
    jsize inputSize = env->GetArrayLength(inputFrame);

    // Validate input size
    if (inputSize < width * height) {
        LOGE("Invalid input size: %d, expected at least: %d", inputSize, width * height);
        return nullptr;
    }

    // CRITICAL SECTION START: Get input array using GetPrimitiveArrayCritical
    // Rules: NO JNI calls, NO GC, minimal operations only
    jboolean isCopy = JNI_FALSE;
    jbyte* inputBytes = reinterpret_cast<jbyte*>(
        env->GetPrimitiveArrayCritical(inputFrame, &isCopy)
    );

    if (inputBytes == nullptr) {
        LOGE("Failed to get input array");
        return nullptr;
    }

    // CRITICAL SECTION: Only wrap data, NO processing
    // For RAW mode, we need full YUV data for color conversion
    // For other modes, Y plane (grayscale) is sufficient
    cv::Mat inputMat;

    if (mode == 0) {  // MODE_RAW - need color
        // YUV_420_888 format: Y plane (width*height) + VU planes (width*height/2)
        int ySize = width * height;
        int uvSize = ySize / 2;

        // Create YUV image and clone the data
        cv::Mat yuvImage(height + height / 2, width, CV_8UC1, inputBytes);
        cv::Mat yuvCopy = yuvImage.clone();

        // CRITICAL SECTION END: Release immediately after cloning
        env->ReleasePrimitiveArrayCritical(inputFrame, inputBytes, JNI_ABORT);

        // OUTSIDE CRITICAL SECTION: Convert YUV to RGB (not BGR to fix color swap)
        cv::Mat bgrMat;
        cv::cvtColor(yuvCopy, bgrMat, cv::COLOR_YUV2BGR_NV21);
        cv::cvtColor(bgrMat, inputMat, cv::COLOR_BGR2RGB);
    } else {
        // For EDGES and GRAYSCALE modes, Y plane only (grayscale)
        cv::Mat yPlane(height, width, CV_8UC1, inputBytes);
        inputMat = yPlane.clone();

        // CRITICAL SECTION END: Release immediately after cloning
        env->ReleasePrimitiveArrayCritical(inputFrame, inputBytes, JNI_ABORT);
    }

    // OUTSIDE CRITICAL SECTION: Now safe to do processing with JNI calls possible
    cv::Mat outputMat;

    try {
        ImageProcessor::processFrame(inputMat, outputMat, mode);
    } catch (const cv::Exception& e) {
        LOGE("OpenCV error: %s", e.what());
        return nullptr;
    } catch (const std::exception& e) {
        LOGE("Error: %s", e.what());
        return nullptr;
    }

    // Convert output to byte array (outside critical section)
    int outputSize = outputMat.total() * outputMat.elemSize();
    jbyteArray outputArray = env->NewByteArray(outputSize);

    if (outputArray == nullptr) {
        LOGE("Failed to create output array");
        return nullptr;
    }

    // Use GetPrimitiveArrayCritical for output as well for optimization
    jbyte* outputBytes = reinterpret_cast<jbyte*>(
        env->GetPrimitiveArrayCritical(outputArray, nullptr)
    );

    if (outputBytes != nullptr) {
        // Direct memory copy
        std::memcpy(outputBytes, outputMat.data, outputSize);
        env->ReleasePrimitiveArrayCritical(outputArray, outputBytes, 0);
    } else {
        // Fallback to SetByteArrayRegion if critical access fails
        env->SetByteArrayRegion(
            outputArray,
            0,
            outputSize,
            reinterpret_cast<jbyte*>(outputMat.data)
        );
    }

    // Log performance
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        endTime - startTime
    ).count();
    LOGD("Native processing took: %lld ms (zero-copy: %s)",
         duration,
         isCopy ? "false" : "true");

    return outputArray;
}
