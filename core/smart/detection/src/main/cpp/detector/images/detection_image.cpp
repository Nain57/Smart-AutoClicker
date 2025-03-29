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

#include <opencv2/imgproc/imgproc.hpp>
#include "detection_image.hpp"

using namespace smartautoclicker;


void DetectionImage::processFullSizeBitmap(cv::Mat* screenMat, double scaleRatio) {
    // Read bitmap to create full size color Mat
    std::unique_ptr<cv::Mat> fullSizeMat = std::make_unique<cv::Mat>(*screenMat);

    // Compute full and scaled sizes
    roi.setFullSize(fullSizeMat.get(), scaleRatio);

    // Convert to gray scale
    cv::Mat fullSizeGray;
    cv::cvtColor(*fullSizeMat, fullSizeGray, cv::COLOR_RGBA2GRAY);

    // Resize and store result in scaledGray
    std::unique_ptr<cv::Mat> scaledGrayMat = std::make_unique<cv::Mat>(cv::Size(0, 0), CV_8UC1);
    cv::resize(fullSizeGray, *scaledGrayMat, roi.getScaled().size(), 0, 0, cv::INTER_AREA);

    // Let implementation handles cache memory for the output
    onNewImageLoaded(std::move(fullSizeMat), std::move(scaledGrayMat));
}

ScalableRoi DetectionImage::getRoi() const {
    return roi;
}
