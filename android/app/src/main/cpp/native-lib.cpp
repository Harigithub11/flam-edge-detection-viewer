#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>

// Logging macro
#define LOG_TAG "EdgeDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_flam_edgeviewer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Native library loaded successfully!";
    LOGD("JNI function called: %s", hello.c_str());
    return env->NewStringUTF(hello.c_str());
}

// Test function for OpenCV
extern "C" JNIEXPORT jstring JNICALL
Java_com_flam_edgeviewer_MainActivity_getOpenCVVersion(
        JNIEnv* env,
        jobject /* this */) {

    std::string version = "OpenCV " + std::string(CV_VERSION);

    LOGD("OpenCV version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}

// Test function for processing (placeholder)
extern "C" JNIEXPORT jint JNICALL
Java_com_flam_edgeviewer_MainActivity_testNativeProcessing(
        JNIEnv* env,
        jobject /* this */,
        jint inputValue) {

    LOGD("Test processing called with value: %d", inputValue);

    // Simple test: multiply by 2
    jint result = inputValue * 2;

    LOGD("Test processing result: %d", result);
    return result;
}
