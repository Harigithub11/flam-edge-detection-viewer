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

// Helper function: Convert YUV to RGB manually (like OpenCV-NDK reference)
static inline void YUV2RGB_Manual(uint8_t Y, uint8_t U, uint8_t V, uint8_t& R, uint8_t& G, uint8_t& B) {
    int y = Y - 16;
    int u = U - 128;
    int v = V - 128;
    if (y < 0) y = 0;

    // Integer math for performance (no floating point)
    int r = (1192 * y + 1634 * v) >> 10;
    int g = (1192 * y - 833 * v - 400 * u) >> 10;
    int b = (1192 * y + 2066 * u) >> 10;

    R = std::min(255, std::max(0, r));
    G = std::min(255, std::max(0, g));
    B = std::min(255, std::max(0, b));
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_processFrameNative(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray inputFrame,
        jint width,
        jint height,
        jint mode,
        jint rotationDegrees) {

    // Performance timing
    auto startTime = std::chrono::high_resolution_clock::now();

    // BEST PRACTICE: Get array size BEFORE critical section (no JNI calls allowed inside)
    jsize inputSize = env->GetArrayLength(inputFrame);

    // Log input parameters
    LOGD("Processing frame: size=%d, width=%d, height=%d, mode=%d", inputSize, width, height, mode);

    // Validate input size
    if (inputSize < 100) {
        LOGE("Invalid input size: %d (too small)", inputSize);
        return nullptr;
    }

    // CRITICAL SECTION START
    jboolean isCopy = JNI_FALSE;
    jbyte* inputBytes = reinterpret_cast<jbyte*>(
        env->GetPrimitiveArrayCritical(inputFrame, &isCopy)
    );

    if (inputBytes == nullptr) {
        LOGE("Failed to get input array");
        return nullptr;
    }

    cv::Mat inputMat;

    if (mode == 0) {  // MODE_RAW - YUV to RGB conversion
        // BALANCED APPROACH: Try OpenCV conversion first, fall back to manual if needed
        cv::Mat yuvMat(height + height / 2, width, CV_8UC1, inputBytes);
        cv::Mat yuvCopy = yuvMat.clone();

        // CRITICAL SECTION END: Release immediately
        env->ReleasePrimitiveArrayCritical(inputFrame, inputBytes, JNI_ABORT);

        LOGD("MODE_RAW: Converting YUV to RGB...");

        cv::Mat rgbMat(height, width, CV_8UC3);

        // Try YV12 first (Y + V + U)
        try {
            cv::cvtColor(yuvCopy, rgbMat, cv::COLOR_YUV2RGB_YV12);
            LOGD("YV12 conversion successful");
        } catch (...) {
            // Try I420 (Y + U + V)
            try {
                cv::cvtColor(yuvCopy, rgbMat, cv::COLOR_YUV2RGB_I420);
                LOGD("I420 conversion successful");
            } catch (...) {
                // Try NV21 (Y + interleaved VU)
                try {
                    cv::cvtColor(yuvCopy, rgbMat, cv::COLOR_YUV2RGB_NV21);
                    LOGD("NV21 conversion successful");
                } catch (...) {
                    LOGE("All OpenCV conversions failed, using manual conversion");
                    // Manual conversion as last resort
                    uint8_t* yPtr = yuvCopy.data;
                    uint8_t* uPtr = yuvCopy.data + (width * height);
                    uint8_t* vPtr = uPtr + (width * height / 4);

                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int yIdx = y * width + x;
                            int uvIdx = (y / 2) * (width / 2) + (x / 2);

                            uint8_t Y = yPtr[yIdx];
                            uint8_t U = uPtr[uvIdx];
                            uint8_t V = vPtr[uvIdx];

                            uint8_t R, G, B;
                            YUV2RGB_Manual(Y, U, V, R, G, B);

                            rgbMat.at<cv::Vec3b>(y, x) = cv::Vec3b(R, G, B);
                        }
                    }
                    LOGD("Manual conversion complete");
                }
            }
        }

        // Fix vertical flip
        cv::flip(rgbMat, inputMat, 0);

        LOGD("MODE_RAW: RGB ready - channels=%d, size=%dx%d",
             inputMat.channels(), inputMat.cols, inputMat.rows);
    } else {
        // For EDGES and GRAYSCALE: Use Y plane only
        cv::Mat yPlane(height, width, CV_8UC1, inputBytes);
        cv::Mat yCopy = yPlane.clone();

        // CRITICAL SECTION END: Release immediately
        env->ReleasePrimitiveArrayCritical(inputFrame, inputBytes, JNI_ABORT);

        // Fix vertical flip
        cv::flip(yCopy, inputMat, 0);

        LOGD("MODE_GRAY/EDGES: Grayscale ready, channels=%d", inputMat.channels());
    }

    // OUTSIDE CRITICAL SECTION: Now safe to do processing with JNI calls possible
    cv::Mat outputMat;

    try {
        LOGD("Processing frame with mode=%d", mode);

        // Process frame
        ImageProcessor::processFrame(inputMat, outputMat, mode);

        LOGD("Frame processed successfully, output channels=%d", outputMat.channels());

        // Apply rotation if needed
        if (rotationDegrees != 0) {
            cv::Mat rotatedMat;
            ImageProcessor::rotateFrame(outputMat, rotatedMat, rotationDegrees);
            outputMat = rotatedMat;
        }
    } catch (const cv::Exception& e) {
        LOGE("OpenCV error in mode %d: %s", mode, e.what());
        return nullptr;
    } catch (const std::exception& e) {
        LOGE("Error in mode %d: %s", mode, e.what());
        return nullptr;
    } catch (...) {
        LOGE("Unknown error in mode %d", mode);
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
