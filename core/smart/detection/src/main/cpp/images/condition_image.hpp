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


#ifndef KLICK_R_CONDITION_IMAGE_HPP
#define KLICK_R_CONDITION_IMAGE_HPP

#include "detection_image.hpp"

namespace smartautoclicker {

    class ConditionImage : public DetectionImage {

    private:
        /** */
        cv::Scalar fullSizeColorMean;
        /** */
        std::unique_ptr<cv::Mat> scaledGrayMat = nullptr;

        void onNewImageLoaded(std::unique_ptr<cv::Mat> fullSizeColor, std::unique_ptr<cv::Mat> scaledGray) override;

    public:
        cv::Scalar getFullSizeColorMean() const;
        cv::Mat* getScaledGrayColorMat() const;
    };
}

#endif //KLICK_R_CONDITION_IMAGE_HPP
