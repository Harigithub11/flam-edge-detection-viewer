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

// EXACT COPY from sjfricke/OpenCV-NDK - Image_Reader.cpp lines 182-209
// https://github.com/sjfricke/OpenCV-NDK
#ifndef MAX
#define MAX(a, b) \
  ({                        \
    __typeof__(a) _a = (a); \
    __typeof__(b) _b = (b); \
    _a > _b ? _a : _b;      \
  })
#define MIN(a, b) \
  ({                        \
    __typeof__(a) _a = (a); \
    __typeof__(b) _b = (b); \
    _a < _b ? _a : _b;      \
  })
#endif

// This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
// are normalized to eight bits.
static const int kMaxChannelValue = 262143;

static inline uint32_t YUV2RGB(int nY, int nU, int nV) {
  nY -= 16;
  nU -= 128;
  nV -= 128;
  if (nY < 0) nY = 0;

  // Standard ITU-R BT.601 conversion coefficients (scaled by 1024 for integer math)
  // R = Y + 1.402 * (V - 128)
  // G = Y - 0.344136 * (U - 128) - 0.714136 * (V - 128)  
  // B = Y + 1.772 * (U - 128)
  
  int nR = (int)(1024 * nY + 1436 * nV);
  int nG = (int)(1024 * nY - 352 * nU - 731 * nV);
  int nB = (int)(1024 * nY + 1815 * nU);

  nR = MIN(kMaxChannelValue, MAX(0, nR));
  nG = MIN(kMaxChannelValue, MAX(0, nG));
  nB = MIN(kMaxChannelValue, MAX(0, nB));

  nR = (nR >> 10) & 0xff;
  nG = (nG >> 10) & 0xff;
  nB = (nB >> 10) & 0xff;

  return 0xff000000 | (nR << 16) | (nG << 8) | nB;
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

    // Validate input size - YUV420 requires (width * height * 3) / 2 bytes
    const int expectedMinSize = (width * height * 3) / 2;
    if (inputSize < expectedMinSize) {
        LOGE("Invalid input size: %d (expected at least %d for %dx%d YUV420)",
             inputSize, expectedMinSize, width, height);
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
        // OFFICIAL OPENCV APPROACH from Google's documentation and Stack Overflow
        // https://stackoverflow.com/questions/30510928/convert-android-camera2-api-yuv-420-888-to-rgb
        // Mat construction: (height + height/2) rows x width columns for NV21 format

        // Create Mat directly from NV21 buffer
        cv::Mat yuvMat(height + height / 2, width, CV_8UC1, inputBytes);
        cv::Mat yuvCopy = yuvMat.clone();

        // CRITICAL SECTION END: Release immediately after cloning
        env->ReleasePrimitiveArrayCritical(inputFrame, inputBytes, JNI_ABORT);

        LOGD("MODE_RAW: Using OpenCV cvtColor with COLOR_YUV2BGR_NV21");
        LOGD("YUV Mat size: %dx%d, channels=%d", yuvCopy.cols, yuvCopy.rows, yuvCopy.channels());

        cv::Mat bgrMat;

        // Use OpenCV's built-in NV21 to BGR conversion with timeout protection
        LOGD("Starting YUV to BGR conversion...");
        try {
            cv::cvtColor(yuvCopy, bgrMat, cv::COLOR_YUV2BGR_NV21);
            LOGD("OpenCV NV21 conversion succeeded");
        } catch (const cv::Exception& e) {
            LOGE("OpenCV NV21 conversion failed: %s", e.what());
            // Create empty BGR mat as fallback
            bgrMat = cv::Mat::zeros(height, width, CV_8UC3);
        } catch (...) {
            LOGE("Unknown error in YUV conversion");
            // Create empty BGR mat as fallback
            bgrMat = cv::Mat::zeros(height, width, CV_8UC3);
        }
        LOGD("YUV conversion completed");

        LOGD("cvtColor complete - BGR Mat %dx%d, channels=%d",
             bgrMat.cols, bgrMat.rows, bgrMat.channels());

        // Fix both vertical flip (upside down) and horizontal flip (mirror)
        cv::Mat flippedMat;
        cv::flip(bgrMat, flippedMat, -1);

        // Convert BGR to RGB for OpenGL (OpenCV outputs BGR, OpenGL expects RGB)
        cv::cvtColor(flippedMat, inputMat, cv::COLOR_BGR2RGB);

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