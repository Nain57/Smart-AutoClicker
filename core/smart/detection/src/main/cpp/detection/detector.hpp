/*
 * Copyright (C) 2025 Kevin Buzeau
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

    /** Detect if an image is found within another one. */
    class Detector {

    private:
        /** Tag for the Android logcat. */
        static constexpr char const* LOG_TAG = "Detector";

        /** Manages the scaling ratio for the processing depending on the scenario quality and screen size.*/
        ScaleRatioManager scaleRatioManager = ScaleRatioManager();

        /** Details of the current screen image. [conditionImage] will be search in it. */
        DetectionImage screenImage = DetectionImage();
        /** Details of the image to detect in [screenImage]. */
        DetectionImage conditionImage = DetectionImage();
        /** The region of [screenImage] in which [conditionImage] will be searched. */
        ScalableRoi detectionRoi = ScalableRoi();

        /** The results of the OpenCv template matching. */
        MatchingResults matchingResults = MatchingResults();
        /** The results of the condition detection. */
        DetectionResult detectionResult = DetectionResult();

        /**
         * Check if the provided condition is found in the current screen image.
         * The screen image should be set with [setScreenImage], and the detection roi should be up to date before
         * executing this method.
         *
         * @param env current java env.
         * @param conditionImage the image to search in the screen
         * @param threshold the detection threshold, expressed in [0..1].
         */
        void match(JNIEnv *env, jobject conditionImage, int threshold);

        /** Verify if the matching result is above the provided threshold. */
        static bool isResultAboveThreshold(const MatchingResults& results, int threshold);
        /** Get the percentage of color difference between two images. Result is expressed in [0..1]. */
        static double getColorDiff(const cv::Mat& image, const cv::Mat& condition);


    public:

        /**
         * Initialize the detector.
         *
         * @param env current java env.
         * @param results the result java object to update upon [detectCondition] calls.
         */
        void initialize(JNIEnv *env, jobject results);

        /**
         * Release the detector.
         *
         * @param env current java env.
         */
        void release(JNIEnv *env);

        /**
         * Determine the scale ratio depending on the screen size.
         * We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
         * the performance of the detection. In order to detect correctly, this should be called everytime the screen is
         * resized or rotated.
         *
         * @param env current java env.
         * @param metricsTag the debugging tag for logging.
         * @param screenBitmap the bitmap containing what's currently displayed on the screen. Used for sizing purposes.
         * @param detectionQuality the quality of the detection.
         */
        void setScreenMetrics(JNIEnv *env, jstring metricsTag, jobject screenBitmap, double detectionQuality);

        /**
         * Set the image where all following detection requests via [detectCondition] will search in.
         *
         * @param env current java env.
         * @param screenBitmap the debugging tag for logging.
         */
        void setScreenImage(JNIEnv *env, jobject screenBitmap);

        /**
         * Check if the provided image is contained in the image defined with [setScreenImage].
         * [detectionResult] structure will be updated accordingly.
         *
         * @param env current java env.
         * @param conditionImage the image to search.
         * @param threshold the minimum detection confidence to consider the detection position.
         */
        void detectCondition(JNIEnv *env, jobject conditionImage, int threshold);

        /**
         * Check if the provided image is contained in a specific area within the image defined with [setScreenImage].
         * [detectionResult] structure will be updated accordingly.
         *
         * @param env current java env.
         * @param conditionImage the image to search.
         * @param x the left position of the area to search in.
         * @param y the top position of the area to search in.
         * @param width the width of the area to search in.
         * @param height the height of the area to search in.
         * @param threshold the minimum detection confidence to consider the detection position.
         */
        void detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold);
    };
}

#endif //KLICK_R_DETECTOR_HPP
