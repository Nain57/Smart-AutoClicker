

#ifndef KLICK_R_TEMPLATE_MATCHING_RESULT_HPP
#define KLICK_R_TEMPLATE_MATCHING_RESULT_HPP

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class TemplateMatchingResult {
    private:
        bool detected;
        double minVal;
        double maxVal;
        cv::Point minLoc;
        cv::Point maxLoc;
        int centerX;
        int centerY;
        cv::Rect area;

    public:
        void updateResults(
                const cv::Rect& detectionArea,
                const cv::Mat& condition,
                cv::Mat& matchingResults);
        void markResultAsDetected();
        void reset();

        [[nodiscard]] bool isDetected() const;
        [[nodiscard]] double getResultConfidence() const;
        [[nodiscard]] cv::Rect getResultArea() const;
        [[nodiscard]] int getResultAreaCenterX() const;
        [[nodiscard]] int getResultAreaCenterY() const;

        void invalidateCurrentResult(const cv::Mat& condition, cv::Mat& results) const;
    };
} // smartautoclicker

#endif //KLICK_R_TEMPLATE_MATCHING_RESULT_HPP
