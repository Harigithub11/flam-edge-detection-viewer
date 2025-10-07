#include "ImageProcessor.h"
#include <android/log.h>

#define LOG_TAG "ImageProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool ImageProcessor::isInitialized = false;

bool ImageProcessor::initialize() {
    if (!isInitialized) {
        LOGD("Initializing ImageProcessor");
        isInitialized = true;
    }
    return isInitialized;
}

void ImageProcessor::release() {
    if (isInitialized) {
        LOGD("Releasing ImageProcessor");
        isInitialized = false;
    }
}

void ImageProcessor::processFrame(
    const cv::Mat& input,
    cv::Mat& output,
    int mode
) {
    switch (mode) {
        case MODE_RAW:
            // Pass through original (RGB color)
            input.copyTo(output);
            LOGD("Mode: RAW (pass-through, channels=%d)", input.channels());
            break;

        case MODE_EDGES:
            cannyEdgeDetection(input, output);
            break;

        case MODE_GRAYSCALE:
            grayscaleFilter(input, output);
            break;

        default:
            input.copyTo(output);
            break;
    }
}

void ImageProcessor::cannyEdgeDetection(
    const cv::Mat& input,
    cv::Mat& output
) {
    cv::Mat gray, blurred;

    // Convert to grayscale
    if (input.channels() == 4) {
        cv::cvtColor(input, gray, cv::COLOR_RGBA2GRAY);
    } else if (input.channels() == 3) {
        cv::cvtColor(input, gray, cv::COLOR_RGB2GRAY);
    } else {
        gray = input;
    }

    // Apply Gaussian blur for noise reduction
    cv::GaussianBlur(gray, blurred, cv::Size(5, 5), 1.5);

    // Canny edge detection
    // Parameters: low threshold = 50, high threshold = 150, aperture = 3
    cv::Canny(blurred, output, 50, 150, 3);

    LOGD("Canny edge detection completed");
}

void ImageProcessor::grayscaleFilter(
    const cv::Mat& input,
    cv::Mat& output
) {
    if (input.channels() == 4) {
        cv::cvtColor(input, output, cv::COLOR_RGBA2GRAY);
    } else if (input.channels() == 3) {
        cv::cvtColor(input, output, cv::COLOR_RGB2GRAY);
    } else {
        input.copyTo(output);
    }

    LOGD("Grayscale filter completed");
}
