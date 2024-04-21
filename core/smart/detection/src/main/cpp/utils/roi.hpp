/*
 * Copyright (C) 2023 Kevin Buzeau
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
#include <opencv2/imgproc/imgproc.hpp>

namespace smartautoclicker {

    bool isRoiContainedInImage(const cv::Rect& roi, const cv::Mat& image) {
        return 0 > roi.x || 0 > roi.width || roi.x + roi.width > image.cols
                || 0 > roi.y || 0 > roi.height || roi.y + roi.height > image.rows;
    }

    bool isRoiContainsImage(const cv::Rect& roi, const cv::Mat& image) {
        return roi.x >= 0 && roi.width > 0 && roi.x + roi.width <= image.cols
                && roi.y >= 0 && roi.height > 0 && roi.y + roi.height <= image.rows;
    }

    cv::Rect getScaledRoi(const cv::Rect& roi, const double scaleRatio) {
        return {
                cvRound(roi.x * scaleRatio),
                cvRound(roi.y * scaleRatio),
                cvRound(roi.width * scaleRatio),
                cvRound(roi.height * scaleRatio)
        };
    }

    cv::Rect getRoiForResult(const cv::Point& resultLoc, const cv::Mat& expectedImage) {
        return {
                resultLoc.x,
                resultLoc.y,
                expectedImage.cols,
                expectedImage.rows
        };
    }

    void markRoiAsInvalidInResults(const cv::Rect& roi, const cv::Mat& results) {
        cv::rectangle(results, roi, cv::Scalar(0), CV_FILLED);
    }

    void logInvalidRoiInImage(const cv::Rect& roi, const cv::Mat& image) {
        __android_log_print(
                ANDROID_LOG_ERROR, "Detector",
                "ROI is invalid, %1d/%2d %3d/%4d in %5d/%6d",
                roi.x, roi.y, roi.width, roi.height,
                image.cols, image.rows
        );
    }
}