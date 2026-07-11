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
#ifndef KLICK_R_TEXT_DETECTOR_HPP
#define KLICK_R_TEXT_DETECTOR_HPP

#include <opencv2/core/types.hpp>
#include <utility>
#include <net.h>

#include "text_detector_result.hpp"
#include "../../../images/screen_image.hpp"


namespace smartautoclicker {

    /**
     * Handles the detection of text areas within an image.
     * Uses an NCNN-based model (typically PaddleOCR's DB detector) to identify bounding boxes
     * containing text.
     */
    class TextDetector {

    public:

        bool isInitialized = false;

        /**
         * Initialize detector and load models
         * @param modelPath The path to the folder containing the detection models.
         */
        bool init(const std::string& modelPath);


        /**
         * Detect the text boxes within the provided crop.
         * @param screenCrop the cv::Mat containing the image to detect on.
         *
         * @return a list of of detected text boxes with their relevant crops
         */
        std::vector<TextDetectorResult> detectText(const cv::Mat& screenCrop);

    private:
        /**
         * Maximum size we want to process for conditions, in pixels.
         * Bigger ones get scaled down to this size as their biggest side.
         */
        static constexpr int maxSize = 960;

        /** Image normalization mean values (RGB) for PP-OCRv3/v4 Multilingual. */
        static constexpr float meanVals[3] = {
                127.5f,
                127.5f,
                127.5f
        };

        /** Image normalization scale values (1/std). */
        static constexpr float normVals[3] = {
                1.f / 127.5f,
                1.f / 127.5f,
                1.f / 127.5f
        };

        cv::Mat kernelClose  = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(5, 9));
        cv::Mat kernelDilate = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(40, 3));
        cv::Mat kernelVertical = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(1, 11));

        /** NCNN text detector.*/
        std::unique_ptr<ncnn::Net> ncnnDetector = std::make_unique<ncnn::Net>();

        /**
         * Calculates the optimal detection size while preserving aspect ratio.
         * @param rgbCondition The input image.
         * @return The resized dimensions (bounded by maxSize).
         */
        static cv::Size getDetectionSize(const cv::Mat& rgbCondition) ;

        /**
         * Calculates the padded size required by the NCNN model (multiples of 32).
         * @param detectionSize The resized image size.
         * @return The dimensions for the zero-padded input.
         */
        static cv::Size getDetectionPaddedSize(const cv::Size& detectionSize);

        /**
         * Performs the neural network inference.
         * @param paddedRgb The normalized and padded input image.
         * @param output The raw output tensor from the network.
         */
        void detectText(const cv::Mat &paddedRgb, ncnn::Mat& output) const;

        /**
         * Post-processes the network output into a binary map.
         * @param detectionOutput The raw output from the detector.
         * @return A thresholded binary cv::Mat.
         */
        cv::Mat processDetectionOutput(const cv::Mat& detectionOutput) ;

        /**
         * Finds external contours in the binary score map.
         * @param scoreMap The binary map produced by processDetectionOutput.
         * @return A vector of detected contours.
         */
        static std::vector<std::vector<cv::Point>> findContours(const cv::Mat& scoreMap) ;

        /**
         * Filters the detected contours based on detection confidence and geometry.
         *
         * @param contours The raw contours found in the binary map.
         * @param scoreMap The raw float32 score map from the detector.
         * @param resizedSize The size of the image before padding.
         *
         * @return A new vector containing only the valid contours.
         */
        static std::vector<std::vector<cv::Point>> filterContours(
                const std::vector<std::vector<cv::Point>>& contours,
                const cv::Mat& scoreMap,
                const cv::Size& resizedSize) ;

        /**
         * Calculates bounding boxes from detected contours and rescales them.
         *
         * @param contours Validated contours.
         * @param originalRoi The original input image (for coordinate reference).
         * @param resizedSize The size of the image before padding.
         * @param scaleX Horizontal scale factor.
         * @param scaleY Vertical scale factor.
         * @return A list of bounding boxes in original coordinate space.
         */
        static std::vector<cv::Rect> getBoundingBoxes(
                const std::vector<std::vector<cv::Point>>& contours,
                const cv::Mat& originalRoi,
                const cv::Size& resizedSize,
                float scaleX,
                float scaleY) ;

        /**
         * Packages the bounding boxes and image into the final result structure.
         * @param originalRoi The original input image.
         * @param boundingBoxes The list of detected text areas.
         * @return A vector of packaged results.
         */
        static std::vector<TextDetectorResult> formatResults(
                const cv::Mat& originalRoi,
                const std::vector<cv::Rect>& boundingBoxes) ;
    };
}


#endif //KLICK_R_TEXT_DETECTOR_HPP
