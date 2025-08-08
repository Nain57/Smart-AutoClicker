

#include <opencv2/imgproc/imgproc.hpp>
#include "detection_image.hpp"

using namespace smartautoclicker;


const cv::Mat* DetectionImage::getColorMat() const {
    return colorMat.get();
}

const cv::Mat* DetectionImage::getGrayMat() const {
    return grayMat.get();
}

cv::Rect DetectionImage::getRoi() const {
    return {0, 0, colorMat->cols, colorMat->rows};
}