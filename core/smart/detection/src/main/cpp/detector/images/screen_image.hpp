


#ifndef KLICK_R_SCREEN_IMAGE_HPP
#define KLICK_R_SCREEN_IMAGE_HPP

#include "detection_image.hpp"

namespace smartautoclicker {

    class ScreenImage : public DetectionImage {

    private:
        static cv::Mat cropMat(const cv::Mat& mat, const cv::Rect& roi);

    public:
        void processNewData(std::unique_ptr<cv::Mat> newData, const char* metricsTag);

        [[nodiscard]] cv::Mat cropColor(const cv::Rect& roi) const;
        [[nodiscard]] cv::Mat cropGray(const cv::Rect& roi) const;
    };
}

#endif //KLICK_R_SCREEN_IMAGE_HPP
