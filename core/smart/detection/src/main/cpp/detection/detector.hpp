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

#ifndef KLICK_R_DETECTOR_HPP
#define KLICK_R_DETECTOR_HPP

#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "detection_image.hpp"
#include "matching_results.hpp"

#include "../types/detection_result.hpp"
#include "../types/scalable_roi.hpp"
#include "../utils/scaling.hpp"

namespace smartautoclicker {

    class Detector {

    private:
        /** Tag for the Android logcat. */
        static constexpr char const* LOG_TAG = "Detector";

        ScaleRatioManager scaleRatioManager = ScaleRatioManager();

        DetectionImage screenImage = DetectionImage();
        DetectionImage conditionImage = DetectionImage();
        ScalableRoi detectionRoi = ScalableRoi();

        MatchingResults matchingResults = MatchingResults();
        DetectionResult detectionResult = DetectionResult();

        void match(JNIEnv *env, jobject conditionImage, int threshold);

        bool prepareForMatching(JNIEnv *env, jobject conditionBitmap);
        bool matchTemplate(int threshold);

        static bool isResultAboveThreshold(const MatchingResults& results, int threshold);
        static double getColorDiff(const cv::Mat& image, const cv::Mat& condition);

    public:

        void initialize(JNIEnv *env, jobject results);

        void release(JNIEnv *env);

        /**
         * Select the scale ratio depending on the screen size.
         * We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
         * the performance of the detection.
         * @param env
         * @param metricsTag
         * @param screenBitmap
         * @param detectionQuality
         */
        void setScreenMetrics(JNIEnv *env, jstring metricsTag, jobject screenBitmap, double detectionQuality);

        /**
         *
         * @param env
         * @param screenBitmap
         */
        void setScreenImage(JNIEnv *env, jobject screenBitmap);

        void detectCondition(JNIEnv *env, jobject conditionImage, int threshold);

        void detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold);
    };
}

#endif //KLICK_R_DETECTOR_HPP
