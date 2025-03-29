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

#include <opencv2/core/types.hpp>

#include "../../scaling/scalable_roi.hpp"

namespace smartautoclicker {

    class DetectionImage {

    protected:
        /** */
        ScalableRoi roi;

        /**
         *
         * @param fullSizeColor
         * @param scaleRatio
         */
        virtual void onNewImageLoaded(std::unique_ptr<cv::Mat> fullSizeColor, std::unique_ptr<cv::Mat> scaledGray) = 0;

    public:
        void processFullSizeBitmap(cv::Mat* screenMat, double scaleRatio);
        ScalableRoi getRoi() const;
    };
}

#endif //KLICK_R_DETECTION_IMAGE_HPP
