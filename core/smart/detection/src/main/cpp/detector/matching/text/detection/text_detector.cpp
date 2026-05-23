/*
 * Copyright (C) 2026 Kevin Buzeau
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
#include <opencv2/imgproc/imgproc_c.h>

#include "text_detector.hpp"
#include "../text_matcher_debugger.hpp"
#include "../../../../logs/log.h"

using namespace smartautoclicker;

bool TextDetector::init(const std::string& modelPath) {

    std::string paramPath = modelPath + "/det.ncnn.param";
    std::string binPath = modelPath + "/det.ncnn.bin";

    int paramResult = ncnnDetector->load_param(paramPath.c_str());
    int binResult = ncnnDetector->load_model(binPath.c_str());

    if (paramResult != 0 || binResult != 0) {
        LOGE("TextDetector", "Can't load detection model from %s", modelPath.c_str());
        return false;
    }

    // Dummy extraction to warmup the lib
    ncnn::Mat dummy(32, 32, 3);
    ncnn::Extractor ex = ncnnDetector->create_extractor();
    ex.input("in0", dummy);
    ncnn::Mat out;
    ex.extract("out0", out);

    return true;
}

std::vector<TextDetectorResult> TextDetector::detectText(const cv::Mat& rgbScreenCrop) {
    // Resize screen image for optimal detection
    cv::Size resizedSize = getDetectionSize(rgbScreenCrop);
    cv::Mat resized;
    cv::resize(rgbScreenCrop, resized,cv::Size(resizedSize.width, resizedSize.height));

    // Pad the resized image, multiple of 32 required by PaddleOCR detector
    cv::Size paddedSize = getDetectionPaddedSize(resizedSize);
    cv::Mat padded = cv::Mat::zeros(paddedSize.height,paddedSize.width,CV_8UC3);
    resized.copyTo(padded(cv::Rect(0,0,resizedSize.width, resizedSize.height)));

    // Run text detection
    ncnn::Mat detectionOutput;
    detectText(padded, detectionOutput);
    LOGD("TextDetector", "Output shape: w=%d h=%d c=%d", detectionOutput.w, detectionOutput.h, detectionOutput.c);

    // Process results and get the textboxes
    cv::Mat scoreMap(detectionOutput.h, detectionOutput.w, CV_32FC1, (void*)detectionOutput.data);
    cv::Mat binaryResults = processDetectionOutput(scoreMap);
    auto contours = findContours(binaryResults);
    LOGD("TextDetector", "Contours found: %zu", contours.size());

    // Scaling factor for detection -> original.
    float scaleX = static_cast<float>(rgbScreenCrop.cols) / static_cast<float>(resizedSize.width);
    float scaleY = static_cast<float>(rgbScreenCrop.rows) / static_cast<float>(resizedSize.height);

    // Remove irrelevant results
    auto filteredContours = filterContours(contours, scoreMap, resizedSize);
    // Get bounding boxes in original coordinates
    auto boundingBoxes = getBoundingBoxes(filteredContours, rgbScreenCrop, resizedSize, scaleX, scaleY);
    // Format output results
    auto results = formatResults(rgbScreenCrop, boundingBoxes);

    // Debugging (does nothing in release builds)
    saveScoreMap(scoreMap);
    saveBinaryMap(binaryResults);
    saveVisualDebug(rgbScreenCrop, contours, scaleX, scaleY);
    saveCrops(results);

    return results;
}

cv::Size TextDetector::getDetectionSize(const cv::Mat& rgbCondition) {
    int width = rgbCondition.cols;
    int height = rgbCondition.rows;
    int maxSide = std::max(width, height);

    // Scale down if needed
    if (maxSide > maxSize) {
        float scale = static_cast<float>(maxSize) / static_cast<float>(maxSide);
        width = static_cast<int>(static_cast<float>(width) * scale);
        height = static_cast<int>(static_cast<float>(height) * scale);
    }

    return { width, height };
}

cv::Size TextDetector::getDetectionPaddedSize(const cv::Size& detectionSize) {
    // Multiple of 32 required by PaddleOCR detector
    return {
            (detectionSize.width + 31) / 32 * 32,
            (detectionSize.height + 31) / 32 * 32
    };
}

void TextDetector::detectText(const cv::Mat &paddedRgb, ncnn::Mat& output) const {
    // Create ncnn input mat
    ncnn::Mat input = ncnn::Mat::from_pixels(
            paddedRgb.data,
            ncnn::Mat::PIXEL_RGB,
            paddedRgb.cols,
            paddedRgb.rows);

    // Normalize
    input.substract_mean_normalize(meanVals, normVals);

    // Inference
    ncnn::Extractor extractor = ncnnDetector->create_extractor();
    extractor.input("in0", input);

    int result = extractor.extract("out0", output);
    if (result != 0) {
        LOGE("TextDetector", "Inference failed");
        return;
    }
}

cv::Mat TextDetector::processDetectionOutput(const cv::Mat& scoreMap) {
    // Convert to OpenCv format
    cv::Mat binary;

    // Thresholding - lowered slightly to help join character fragments
    cv::threshold(scoreMap, binary, 0.3f, 1.0f, cv::THRESH_BINARY);
    binary.convertTo(binary, CV_8UC1, 255);

    // Morphology Close - joins disconnected parts of the same word/character
    cv::Mat kernelClose = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(5, 9));
    cv::morphologyEx(binary, binary, cv::MORPH_CLOSE, kernelClose);

    cv::Mat kernelVertical = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(1, 11));
    cv::dilate(binary, binary, kernelVertical);

    // Dilation - Smear horizontally to merge words into full sentences/lines
    // We use a wider kernel horizontally (30) than vertically (3) to avoid merging separate lines.
    cv::Mat kernelDilate = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(30, 3));
    cv::dilate(binary, binary, kernelDilate);

    return binary;
}

std::vector<std::vector<cv::Point>> TextDetector::findContours(const cv::Mat& binaryResults) {
    std::vector<std::vector<cv::Point>> contours;
    cv::findContours(binaryResults, contours,cv::RETR_EXTERNAL,cv::CHAIN_APPROX_SIMPLE);

    return contours;
}

std::vector<std::vector<cv::Point>> TextDetector::filterContours(
        const std::vector<std::vector<cv::Point>>& contours,
        const cv::Mat& scoreMap,
        const cv::Size& resizedSize)
{

    std::vector<std::vector<cv::Point>> filtered;
    filtered.reserve(contours.size());
    cv::Mat mask = cv::Mat::zeros(scoreMap.size(), CV_8UC1);

    for (const auto& contour : contours) {
        cv::Rect box = cv::boundingRect(contour);

        // Geometry Check
        auto w = static_cast<float>(box.width);
        auto h = static_cast<float>(box.height);
        float aspect = std::max(w, h) / std::max(1.f, std::min(w, h));
        if (aspect > 50.f || std::min(w, h) < 6.f) continue;

        // Boundary Check (reject boxes entirely in padding)
        if (box.x >= resizedSize.width || box.y >= resizedSize.height) continue;

        // Expand slightly (important for game UI text edges)
        const int marginX = 2;
        const int marginY = 5;
        box.x = std::max(0, box.x - marginX);
        box.y = std::max(0, box.y - marginY);
        box.width  = std::min(scoreMap.cols - box.x, box.width  + 2 * marginX);
        box.height = std::min(scoreMap.rows - box.y, box.height + 2 * marginY);

        // ROI views
        cv::Mat scoreROI = scoreMap(box);
        cv::Mat maskROI = cv::Mat::zeros(scoreROI.size(), CV_8UC1);

        // Shift contour into ROI space
        std::vector<cv::Point> shifted;
        shifted.reserve(contour.size());
        for (const auto& p : contour) shifted.emplace_back(p.x - box.x, p.y - box.y);

        std::vector<std::vector<cv::Point>> poly = { shifted };
        cv::fillPoly(maskROI, poly, cv::Scalar(255));

        double maxScore;
        cv::minMaxLoc(scoreROI, nullptr, &maxScore);
        if (maxScore  < 0.5) continue;

        filtered.push_back(contour);
    }

    return filtered;
}

std::vector<cv::Rect> TextDetector::getBoundingBoxes(
        const std::vector<std::vector<cv::Point>>& contours,
        const cv::Mat& originalRoi,
        const cv::Size& resizedSize,
        const float scaleX,
        const float scaleY)
{

    std::vector<cv::Rect> boundingBoxes;
    boundingBoxes.reserve(contours.size());

    for (const auto& contour : contours) {
        // Bounding box in detector space
        cv::Rect boundingBox = cv::boundingRect(contour);

        // Clamp bounding box inside resized content
        boundingBox &= cv::Rect(0, 0, resizedSize.width, resizedSize.height);

        // Expand box slightly
        const int marginX = 4;
        const int marginY = 8;   // increased: v3 multilingual has tighter vertical bounds
        boundingBox.x -= marginX;
        boundingBox.y -= marginY;
        boundingBox.width  += marginX * 2;
        boundingBox.height += marginY * 2;

        // Clamp again after expansion
        boundingBox &= cv::Rect(0, 0, resizedSize.width, resizedSize.height);

        // Convert to original ROI coordinates
        cv::Rect originalBox;
        originalBox.x = static_cast<int>(static_cast<float>(boundingBox.x) * scaleX);
        originalBox.y = static_cast<int>(static_cast<float>(boundingBox.y) * scaleY);
        originalBox.width = static_cast<int>(static_cast<float>(boundingBox.width) * scaleX);
        originalBox.height = static_cast<int>(static_cast<float>(boundingBox.height) * scaleY);

        // Clamp inside ROI
        originalBox &= cv::Rect(0, 0, originalRoi.cols, originalRoi.rows);

        // Final validation
        if (originalBox.width <= 0 || originalBox.height <= 0) continue;

        boundingBoxes.push_back(originalBox);
    }

    return boundingBoxes;
}

std::vector<TextDetectorResult> TextDetector::formatResults(
        const cv::Mat &originalRoi,
        const std::vector<cv::Rect> &boundingBoxes)
{
    std::vector<TextDetectorResult> results;
    results.reserve(boundingBoxes.size());

    for (const cv::Rect& boundingBox : boundingBoxes) {
        // We don't need to clone, as we keep the crop for the whole matching process
        cv::Mat crop = originalRoi(boundingBox);

        // Rotate vertical crops to horizontal for better recognition compatibility.
        if (crop.rows > crop.cols) {
            cv::rotate(crop, crop, cv::ROTATE_90_CLOCKWISE);
            float scaledWidth = static_cast<float>(crop.cols) * (48.f / static_cast<float>(crop.rows));
            if (scaledWidth > 320.f) {
                // Crop is too wide for the recognizer — skip or split
                LOGW("TextDetector", "Skipping crop too wide for recognizer: %dx%d", crop.cols, crop.rows);
                continue;
            }
        }

        results.emplace_back(boundingBox, crop);
    }

    return results;
}
