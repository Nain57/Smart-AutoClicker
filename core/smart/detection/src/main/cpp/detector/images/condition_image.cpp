

#include <opencv2/imgproc/imgproc.hpp>
#include "condition_image.hpp"

using namespace smartautoclicker;

void ConditionImage::processNewData(std::unique_ptr<cv::Mat> newData, int targetWidth, int targetHeight) {
    if (!newData || newData->empty()) return;

    // Java BitmapFactory allow to read and scale down at the same time, saving memory on the java heap.
    // But it can only scale down to a power of 2, so it usually won't have the exact required size.
    // This second exact scaling by OpenCv is a trade-off between memory and execution time.
    if (newData->cols == targetWidth && newData->rows == targetHeight) {
        this->colorMat = std::move(newData);
    } else {
        cv::resize(
                *newData,
                *(this->colorMat),
                cv::Size(targetWidth, targetHeight),
                0, 0,
                cv::INTER_AREA);
    }

    cv::cvtColor(*colorMat, *grayMat, cv::COLOR_RGBA2GRAY);
}

cv::Scalar ConditionImage::getColorMean() const {
    return cv::mean(*this->colorMat);
}
