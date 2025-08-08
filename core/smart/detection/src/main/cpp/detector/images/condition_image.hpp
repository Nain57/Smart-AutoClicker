


#ifndef KLICK_R_CONDITION_IMAGE_HPP
#define KLICK_R_CONDITION_IMAGE_HPP

#include "detection_image.hpp"

namespace smartautoclicker {

    class ConditionImage : public DetectionImage {

    public:
        void processNewData(std::unique_ptr<cv::Mat> newData, int targetWidth, int targetHeight);

        [[nodiscard]] cv::Scalar getColorMean() const;
    };
}

#endif //KLICK_R_CONDITION_IMAGE_HPP
