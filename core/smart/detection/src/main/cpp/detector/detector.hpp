

#ifndef KLICK_R_DETECTOR_HPP
#define KLICK_R_DETECTOR_HPP

#include <opencv2/imgproc/imgproc.hpp>

#include "matching/template_matcher.hpp"
#include "matching/template_matching_result.hpp"
#include "images/condition_image.hpp"
#include "images/screen_image.hpp"

namespace smartautoclicker {

    class Detector {

    private:
        std::unique_ptr<ScreenImage> screenImage = std::make_unique<ScreenImage>();
        std::unique_ptr<ConditionImage> conditionImage = std::make_unique<ConditionImage>();
        std::unique_ptr<TemplateMatcher> templateMatcher = std::make_unique<TemplateMatcher>();

        [[nodiscard]] bool isRoiValidForDetection(const cv::Rect& roi) const;

    public:

        Detector() = default;

        void setScreenImage(std::unique_ptr<cv::Mat> screenColorMat, const char* metricsTag);

        TemplateMatchingResult* detectCondition(
                std::unique_ptr<cv::Mat> conditionMat,
                int targetConditionWidth,
                int targetConditionHeight,
                const cv::Rect& roi,
                int threshold);
    };
}

#endif //KLICK_R_DETECTOR_HPP