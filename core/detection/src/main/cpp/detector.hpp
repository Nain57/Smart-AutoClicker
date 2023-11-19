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

#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "types/detectionResult.hpp"

namespace smartautoclicker {

    class Detector {

    private:
        double scaleRatio = 1;

        std::unique_ptr<cv::Mat> fullSizeColorCurrentImage = nullptr;
        std::unique_ptr<cv::Mat> scaledGrayCurrentImage = std::make_unique<cv::Mat>();

        DetectionResult detectionResult;

        std::unique_ptr<cv::Mat> scaleAndChangeToGray(const cv::Mat &fullSizeColored) const;

        static std::unique_ptr<cv::Mat> matchTemplate(const cv::Mat& image, const cv::Mat& condition);
        static void locateMinMax(const cv::Mat& matchingResult, DetectionResult& results);
        static bool isValidMatching(const DetectionResult& results, const int threshold);
        static double getColorDiff(const cv::Mat& image, const cv::Mat& condition);

        cv::Rect getDetectionResultFullSizeRoi(const cv::Rect& detectionRoi, int fullSizeWidth, int fullSizeHeight) const;

        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, cv::Rect fullSizeDetectionRoi, int threshold);

    public:

        Detector() = default;

        void setScreenMetrics(JNIEnv *env, jobject screenImage, double detectionQuality);

        void setScreenImage(JNIEnv *env, jobject screenImage);

        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, int threshold);
        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold);
    };
}


