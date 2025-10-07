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

    // Apply Gaussian blur for noise reduction (5x5 kernel for better quality)
    cv::GaussianBlur(gray, blurred, cv::Size(5, 5), 1.5);

    // Canny edge detection - balanced thresholds for clean, professional output
    // Higher thresholds (100, 200) filter out noise/tiny details while keeping major edges
    // Parameters: low threshold = 100, high threshold = 200, aperture = 5, L2gradient = true
    cv::Canny(blurred, output, 100, 200, 5, true);  // L2gradient=true for better accuracy

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

void ImageProcessor::rotateFrame(
    const cv::Mat& input,
    cv::Mat& output,
    int rotationDegrees
) {
    switch (rotationDegrees) {
        case 90:
            cv::rotate(input, output, cv::ROTATE_90_CLOCKWISE);
            break;
        case 180:
            cv::rotate(input, output, cv::ROTATE_180);
            break;
        case 270:
            cv::rotate(input, output, cv::ROTATE_90_COUNTERCLOCKWISE);
            break;
        default:
            input.copyTo(output);
            break;
    }
}
