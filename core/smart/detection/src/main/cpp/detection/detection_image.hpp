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

#ifndef KLICK_R_DETECTION_IMAGE_HPP
#define KLICK_R_DETECTION_IMAGE_HPP

#include <jni.h>
#include <android/bitmap.h>
#include <opencv2/core/types.hpp>
#include "../types/scalable_roi.hpp"

namespace smartautoclicker {

    class DetectionImage {

        private:
            void fillFullSizeColor(JNIEnv *env, jobject bitmap, AndroidBitmapInfo* bitmapInfo);
            void computeFullSizeGray() const;
            void computeScaledGray(double scaleRatio);
            static bool isRoiContains(const cv::Rect& roi, const cv::Rect& other);

        public:
            std::unique_ptr<cv::Mat> fullSizeColor = std::make_unique<cv::Mat>();
            std::unique_ptr<cv::Mat> fullSizeGray = std::make_unique<cv::Mat>();
            std::unique_ptr<cv::Mat> scaledGray = std::make_unique<cv::Mat>();

            std::unique_ptr<cv::Mat> croppedFullSizeColor = std::make_unique<cv::Mat>();
            std::unique_ptr<cv::Mat> croppedScaledGray = std::make_unique<cv::Mat>();

            cv::Rect fullSizeRoi = cv::Rect(0, 0, 0 ,0);
            cv::Rect scaledRoi = cv::Rect(0, 0, 0, 0);
            cv::Size scaledSize = cv::Size(0, 0);
            cv::Size cropScaledSize = cv::Size(0, 0);

            DetectionImage() = default;

            /** */
            static void readBitmapInfo(JNIEnv *env, jobject bitmap, AndroidBitmapInfo* result) ;

            /** */
            void processBitmap(JNIEnv *env, jobject bitmap, double scaleRatio);

            /** */
            void setCropping(const ScalableRoi& cropRoi);

            /** */
            bool isFullSizeContains(const cv::Rect& roi) const;
            /** */
            bool isScaledContains(const cv::Rect& roi) const;
            /** */
            bool isCroppedScaledContains(const cv::Size& size) const;



    };
}

#endif //KLICK_R_DETECTION_IMAGE_HPP
