
#include <android/log.h>
#include <android/bitmap.h>
#include <memory>
#include <opencv2/imgproc/imgproc_c.h>

#include "../logs/log.h"
#include "../utils/roi.h"
#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;


void Detector::setScreenImage(std::unique_ptr<cv::Mat> screenColorMat, const char* metricsTag) {
    screenImage->processNewData(std::move(screenColorMat), metricsTag);
}

TemplateMatchingResult* Detector::detectCondition(
        std::unique_ptr<cv::Mat> conditionMat,
        int targetConditionWidth,
        int targetConditionHeight,
        const cv::Rect& roi,
        int threshold
) {
    templateMatcher->reset();

    // Load condition and resize to requested size
    conditionImage->processNewData(
            std::move(conditionMat),
            targetConditionWidth,
            targetConditionHeight);

    // Check if the condition fits in the detection area
    if (!isRoiValidForDetection(roi)) {
        return templateMatcher->getMatchingResults();
    }

    // Apply template matching and get global results
    templateMatcher->matchTemplate(
            *screenImage,
            *conditionImage,
            roi,
            threshold);

    return templateMatcher->getMatchingResults();
}

bool Detector::isRoiValidForDetection(const cv::Rect& roi) const {
    cv::Rect screenRoi = screenImage->getRoi();
    cv::Rect conditionRoi = conditionImage->getRoi();

    if (!isRoiBiggerOrEquals(screenRoi, conditionRoi)) {
        LOGD("Detector", "Can't detectCondition, condition (w=%d, h=%d) is bigger than screen (w=%d, h=%d)",
             conditionRoi.width, conditionRoi.height, screenRoi.width, screenRoi.height);
        return false;
    }

    if (!isRoiContainsOrEquals(screenRoi, roi)) {
        LOGD("Detector", "Can't detectCondition, detection area (x=%d, y=%d, w=%d, h=%d) is not contained in screen (w=%d, h=%d)",
             roi.x, roi.y, roi.width, roi.height,
             screenRoi.width, screenRoi.height);
        return false;
    }

    if (!isRoiBiggerOrEquals(roi, conditionRoi)) {
        LOGD("Detector", "Can't detectCondition, condition (w=%d, h=%d) is bigger than detection area (x=%d, y=%d, w=%d, h=%d)",
             conditionRoi.width, conditionRoi.height, roi.x, roi.y, roi.width, roi.height);
        return false;
    }

    return true;
}
