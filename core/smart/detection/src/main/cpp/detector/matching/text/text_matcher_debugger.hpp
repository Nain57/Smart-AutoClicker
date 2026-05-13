/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef KLICK_R_TEXT_MATCHER_DEBUGGER_HPP
#define KLICK_R_TEXT_MATCHER_DEBUGGER_HPP


#include <opencv2/core.hpp>
#include "detection/text_detector_result.hpp"

#ifdef NDEBUG // Skip debugging methods in release builds

inline void saveScoreMap(const cv::Mat& scoreMap) {}
inline void saveBinaryMap(const cv::Mat& binary) {}
inline void saveVisualDebug(const cv::Mat& roi, const std::vector<std::vector<cv::Point>>& contours, float scaleX, float scaleY) {}
inline void saveCrops(const std::vector<smartautoclicker::TextDetectorResult>& results) {}

#else

#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include "../../../logs/log.h"

inline void saveScoreMap(const cv::Mat& scoreMap) {
    double minVal;
    double maxVal;
    cv::minMaxLoc(scoreMap, &minVal, &maxVal);
    LOGI("TextDetector","Score range: %f -> %f", minVal, maxVal);

    cv::Mat debugScore;
    scoreMap.convertTo(debugScore, CV_8UC1, 255.0);
    cv::imwrite("/sdcard/Download/ocr_score.png", debugScore);
}

inline void saveBinaryMap(const cv::Mat& binary) {
    cv::imwrite("/sdcard/Download/ocr_binary.png", binary);
}

inline void saveVisualDebug(
        const cv::Mat& roi,
        const std::vector<std::vector<cv::Point>>& contours,
        float scaleX,
        float scaleY)
{
    cv::Mat debugImage = roi.clone();
    for (const auto& contour : contours) {
        cv::RotatedRect rect = cv::minAreaRect(contour);
        cv::Point2f pts[4];
        rect.points(pts);
        for (int j = 0; j < 4; j++) {
            cv::Point2f p1(pts[j].x * scaleX, pts[j].y * scaleY);
            cv::Point2f p2(pts[(j + 1) % 4].x * scaleX, pts[(j + 1) % 4].y * scaleY);
            cv::line(debugImage, p1, p2, cv::Scalar(0, 255, 0), 2);
        }
    }
    cv::imwrite("/sdcard/Download/ocr_boxes.png", debugImage);
}

inline void saveCrops(const std::vector<smartautoclicker::TextDetectorResult>& results) {
    for (size_t i = 0; i < results.size(); i++) {
        std::string cropPath ="/sdcard/Download/crop_" +std::to_string(i) +".png";
        cv::imwrite(cropPath, results[i].crop);
    }
}
#endif // NDEBUG

#endif //KLICK_R_TEXT_MATCHER_DEBUGGER_HPP
