

#ifndef KLICK_R_TEMPLATE_MATCHER_HPP
#define KLICK_R_TEMPLATE_MATCHER_HPP

#include <opencv2/core/types.hpp>

#include "../images/condition_image.hpp"
#include "../images/screen_image.hpp"
#include "template_matching_result.hpp"

namespace smartautoclicker {

    class TemplateMatcher {

    private:
        TemplateMatchingResult currentMatchingResult;

        void parseMatchingResult(
                const ScreenImage& screenImage,
                const ConditionImage& condition,
                const cv::Rect& detectionArea,
                int threshold,
                cv::Mat& matchingResult);

        static bool isConfidenceValid(double confidence, int threshold);
        static double getColorDiff(const cv::Mat& image, const cv::Scalar& conditionColorMeans);

    public:
        void reset();
        void matchTemplate(
                const ScreenImage& screenImage,
                const ConditionImage& condition,
                const cv::Rect& detectionArea,
                int threshold);

        TemplateMatchingResult* getMatchingResults();

    };
}

#endif //KLICK_R_TEMPLATE_MATCHER_HPP
