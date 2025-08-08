

#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/imgproc/imgproc.hpp>
#include "template_matching_result.hpp"

using namespace smartautoclicker;


void TemplateMatchingResult::updateResults(
        const cv::Rect& detectionArea,
        const cv::Mat& condition,
        cv::Mat& matchingResults
) {
    cv::minMaxLoc(matchingResults, &minVal, &maxVal, &minLoc, &maxLoc, cv::Mat());

    area.x = detectionArea.x + maxLoc.x;
    area.y = detectionArea.y + maxLoc.y;
    area.width = condition.cols;
    area.height = condition.rows;
    centerX = area.x + ((int) (area.width / 2));
    centerY = area.y + ((int) (area.height / 2));
}

void TemplateMatchingResult::markResultAsDetected() {
    detected = true;
}

void TemplateMatchingResult::invalidateCurrentResult(const cv::Mat& condition, cv::Mat& matchingResults) const {
    cv::rectangle(
            matchingResults,
            cv::Rect(maxLoc.x, maxLoc.y,  condition.cols,condition.rows),
            cv::Scalar(0),
            CV_FILLED);
}

void TemplateMatchingResult::reset() {
    detected = false;
    centerX = 0;
    centerY = 0;
    minVal = 0;
    maxVal = 0;
    minLoc.x = 0;
    minLoc.y = 0;
    maxLoc.x = 0;
    maxLoc.y = 0;
    area.x = 0;
    area.y = 0;
    area.width = 0;
    area.height = 0;
}

bool TemplateMatchingResult::isDetected() const {
    return detected;
}

double TemplateMatchingResult::getResultConfidence() const {
    return maxVal;
}

cv::Rect TemplateMatchingResult::getResultArea() const {
    return area;
}

int TemplateMatchingResult::getResultAreaCenterX() const {
    return centerX;
}

int TemplateMatchingResult::getResultAreaCenterY() const {
    return centerY;
}
