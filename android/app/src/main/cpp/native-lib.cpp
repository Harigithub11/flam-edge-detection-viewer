#include <jni.h>
#include <string>
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

    // Get input array
    jbyte* inputBytes = env->GetByteArrayElements(inputFrame, nullptr);
    if (inputBytes == nullptr) {
        LOGE("Failed to get input array");
        return nullptr;
    }

    jsize inputSize = env->GetArrayLength(inputFrame);

    try {
        // Create OpenCV Mat from input (YUV or RGBA)
        // Assuming RGBA for simplicity (convert YUV in production)
        cv::Mat inputMat(height, width, CV_8UC4, inputBytes);

        // Process frame
        cv::Mat outputMat;
        ImageProcessor::processFrame(inputMat, outputMat, mode);

        // Convert output to byte array
        int outputSize = outputMat.total() * outputMat.elemSize();
        jbyteArray outputArray = env->NewByteArray(outputSize);

        if (outputArray == nullptr) {
            LOGE("Failed to create output array");
            env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);
            return nullptr;
        }

        env->SetByteArrayRegion(
            outputArray,
            0,
            outputSize,
            reinterpret_cast<jbyte*>(outputMat.data)
        );

        // Release input array
        env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);

        // Log performance
        auto endTime = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
            endTime - startTime
        ).count();
        LOGD("Native processing took: %lld ms", duration);

        return outputArray;

    } catch (const cv::Exception& e) {
        LOGE("OpenCV error: %s", e.what());
        env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);
        return nullptr;
    } catch (const std::exception& e) {
        LOGE("Error: %s", e.what());
        env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);
        return nullptr;
    }
}
