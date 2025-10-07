#ifndef EDGE_DETECTION_VIEWER_IMAGEPROCESSOR_H
#define EDGE_DETECTION_VIEWER_IMAGEPROCESSOR_H

#include <opencv2/opencv.hpp>

class ImageProcessor {
public:
    enum ProcessingMode {
        MODE_RAW = 0,
        MODE_EDGES = 1,
        MODE_GRAYSCALE = 2
    };

    static bool initialize();
    static void release();

    static void processFrame(
        const cv::Mat& input,
        cv::Mat& output,
        int mode
    );

    static void rotateFrame(
        const cv::Mat& input,
        cv::Mat& output,
        int rotationDegrees
    );

private:
    static void cannyEdgeDetection(
        const cv::Mat& input,
        cv::Mat& output
    );

    static void grayscaleFilter(
        const cv::Mat& input,
        cv::Mat& output
    );

    static bool isInitialized;
};

#endif
