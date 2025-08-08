
#include "screen_image.hpp"



#include <opencv2/imgproc/imgproc.hpp>
#include "screen_image.hpp"
#include "../../utils/correction.hpp"

using namespace smartautoclicker;


void ScreenImage::processNewData(std::unique_ptr<cv::Mat> newData, const char* metricsTag) {
    if (!newData || newData->empty() || requiresCorrection(metricsTag)) return;

    this->colorMat = std::move(newData);
    cv::cvtColor(*colorMat, *grayMat, cv::COLOR_RGBA2GRAY);
}

cv::Mat ScreenImage::cropColor(const cv::Rect &roi) const {
    if (!this->colorMat || this->colorMat->empty()) return {};
    return cropMat(*this->colorMat, roi);
}

cv::Mat ScreenImage::cropGray(const cv::Rect &roi) const {
    if (!this->grayMat || this->grayMat->empty()) return {};
    return cropMat(*this->grayMat, roi);
}

cv::Mat ScreenImage::cropMat(const cv::Mat& mat, const cv::Rect& roi) {
    cv::Rect imageBounds(0, 0, mat.cols, mat.rows);
    cv::Rect validRoi = roi & imageBounds;

    if (validRoi.width <= 0 || validRoi.height <= 0) return {};

    return (mat)(validRoi);
}
