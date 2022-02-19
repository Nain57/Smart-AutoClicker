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

    class Detector {

    private:
        constexpr static const double SCALED_IMAGE_MIN_SIZE_PIXEL = 400;

        double scaleRatio = 1;

        std::unique_ptr<cv::Mat> currentImage = nullptr;
        std::unique_ptr<cv::Mat> currentImageScaled = nullptr;

        static bool matchCondition(cv::Mat& image, cv::Mat& condition, double threshold);

        static std::unique_ptr<cv::Mat> bitmapRGBA888ToMat(JNIEnv *env, jobject bitmap);

        static std::unique_ptr<cv::Mat> scale(const cv::Mat& mat, const double& ratio);

    public:

        Detector() = default;

        void setScreenImage(JNIEnv *env, jobject screenImage);

        bool detectCondition(JNIEnv *env, jobject conditionImage, double threshold);

        bool detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, double threshold);
    };
}


