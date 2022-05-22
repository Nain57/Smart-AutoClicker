/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>

namespace smartautoclicker {

    class DetectionResult {
    public:
        bool isDetected;
        double centerX;
        double centerY;

        double minVal;
        double maxVal;
        cv::Point minLoc;
        cv::Point maxLoc;

        void reset() {
            isDetected = false;
            centerX = 0;
            centerY = 0;
            minVal = 0;
            maxVal = 0;
            minLoc.x = 0;
            minLoc.y = 0;
            maxLoc.x = 0;
            maxLoc.y = 0;
        }
    };

    class Detector {

    private:
        constexpr static const double SCALED_IMAGE_MIN_SIZE_PIXEL = 400;

        double scaleRatio = 1;

        std::unique_ptr<cv::Mat> currentImage = nullptr;
        std::unique_ptr<cv::Mat> currentImageScaled = nullptr;

        DetectionResult detectionResult;

        static std::unique_ptr<cv::Mat> bitmapRGBA888ToMat(JNIEnv *env, jobject bitmap);

        static std::unique_ptr<cv::Mat> scale(const cv::Mat& mat, const double& ratio);

        static std::unique_ptr<cv::Mat> matchTemplate(const cv::Mat& image, const cv::Mat& condition);

        static void locateMinMax(const cv::Mat& matchingResult, DetectionResult& results);

        static bool isValidMatching(const DetectionResult& results, const int threshold);

        double getColorDiff(const cv::Mat& image, const cv::Mat& condition);

    public:

        Detector() = default;

        void setScreenImage(JNIEnv *env, jobject screenImage, double detectionQuality);

        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, int threshold);

        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold);
    };
}


