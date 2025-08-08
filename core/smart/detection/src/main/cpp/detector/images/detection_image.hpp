

#ifndef KLICK_R_DETECTION_IMAGE_HPP
#define KLICK_R_DETECTION_IMAGE_HPP

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class DetectionImage {

    protected:
        std::unique_ptr<cv::Mat> colorMat = std::make_unique<cv::Mat>();
        std::unique_ptr<cv::Mat> grayMat = std::make_unique<cv::Mat>();

    public:
        [[nodiscard]] const cv::Mat* getColorMat() const;
        [[nodiscard]] const cv::Mat* getGrayMat() const;
        [[nodiscard]] cv::Rect getRoi() const;
    };
}

#endif //KLICK_R_DETECTION_IMAGE_HPP
