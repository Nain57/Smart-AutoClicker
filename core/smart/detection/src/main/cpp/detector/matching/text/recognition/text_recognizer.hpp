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
#ifndef KLICK_R_TEXT_RECOGNIZER_HPP
#define KLICK_R_TEXT_RECOGNIZER_HPP

#include <opencv2/core.hpp>
#include <map>
#include <net.h>

#include "../detection/text_detector_result.hpp"
#include "alphabet_recognizer.hpp"
#include "text_recognizer_result.hpp"

namespace smartautoclicker {

    /**
     * Handles the recognition of text (OCR) within detected text areas.
     * Uses an NCNN-based model (typically PaddleOCR's CRNN recognizer) to convert
     * image crops into character strings.
     */
    class TextRecognizer {

    public:
        /**
         * Initialize the recognizer and load models/dictionary.
         * @param assetManager The Android Asset Manager for loading assets.
         * @return true if initialization succeeded, false otherwise.
         */
        bool init(const std::map<std::string, std::string>& recognitionModels);

        /**
         * Recognizes text within the provided detection results.
         * @param recognitionModelId The identifier of the recognition model provided with [init].
         * @param detectionResults List of crops and their bounding boxes from a TextDetector.
         * @return A list of recognition results containing the text and confidence for each crop.
         */
        std::vector<TextRecognizerResult> recognizeText(
                const std::string& recognitionModelId,
                const std::vector<TextDetectorResult>& detectionResults);

    private:

        /** PP-OCR normalization mean values. */
        static constexpr float meanVals[3] = {
                127.5f,
                127.5f,
                127.5f
        };

        /** PP-OCR normalization scale values. */
        static constexpr float normVals[3] = {
                1.f / 127.5f,
                1.f / 127.5f,
                1.f / 127.5f
        };

        std::map<std::string, AlphabetRecognizer> alphabetRecognizers;

        /** Reusable buffers to avoid reallocations in the main loop. */
        cv::Mat resizedBuffer;
        /** Reusable buffer for padding, pre-allocated to max size in init. */
        cv::Mat paddedBuffer;
        /** Reusable buffer for text tokens.*/
        std::vector<std::string> tokens;

        /**
         * Preprocesses a single image crop for the recognition model.
         * Handles resizing and normalization.
         * @param crop The RGB image crop containing text.
         * @param isRtlAlphabet true if the text is right to left, false if not.
         * @return An NCNN Mat ready for input.
         */
        ncnn::Mat preprocess(const cv::Mat& crop, bool isRtlAlphabet);

        /**
         * Decodes the raw output tensor from the recognizer into a string.
         * @param dictionary list of detectable characters.
         * @param boundingBox The original bounding box for the result.
         * @param isRtlAlphabet true if the text is right to left, false if not.
         * @param output The raw output from the NCNN extractor.
         * @return A packaged TextRecognizerResult.
         */
        TextRecognizerResult decode(
                const std::vector<std::string>& dictionary,
                const cv::Rect& boundingBox,
                bool isRtlAlphabet,
                const ncnn::Mat& output);
    };

} // smartautoclicker

#endif //KLICK_R_TEXT_RECOGNIZER_HPP
