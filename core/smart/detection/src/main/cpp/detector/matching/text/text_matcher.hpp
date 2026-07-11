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
#ifndef KLICK_R_TEXT_MATCHER_HPP
#define KLICK_R_TEXT_MATCHER_HPP

#include <opencv2/core/types.hpp>
#include <map>
#include <net.h>
#include <limits>

#include "text_matching_result.hpp"
#include "detection/text_detector.hpp"
#include "recognition/text_recognizer.hpp"
#include "../../images/screen_image.hpp"

namespace smartautoclicker {

    /** How to interpret the decimal and thousands separators in a detected number string. */
    enum class NumberFormat {
        AUTO        = 0,
        DOT_DECIMAL = 1,
        COMMA_DECIMAL = 2,
    };

    /**
     * Orchestrates the text matching process by combining text detection and recognition.
     * It locates text areas on the screen, converts them to strings using OCR, and compares
     * the results against a target condition using fuzzy string matching.
     */
    class TextMatcher {

    private:
        /** Handles the localization of text bounding boxes. */
        std::unique_ptr<TextDetector> textLocator = std::make_unique<TextDetector>();
        /** Handles the conversion of image crops to text. */
        std::unique_ptr<TextRecognizer> textRecognizer = std::make_unique<TextRecognizer>();

        /** Buffer for Levenshtein distance calculations (previous row). */
        std::vector<int> comparisonPrevRow;
        /** Buffer for Levenshtein distance calculations (current row). */
        std::vector<int> comparisonCurrRow;
        /** Buffer for Levenshtein distance calculations (two rows back). */
        std::vector<int> comparisonPrevPrevRow;

        /** Stores the result of the most recent match operation. */
        TextMatchingResult currentMatchingResult;

        /** Identifier of the first loaded recognition model, used for generic matching (like numbers). */
        std::string defaultRecognitionModelId;

        /**
         * Checks if the provided text represents a numeric value.
         * @param text The text to check.
         * @return true if it is a number, false otherwise.
         */
        static bool isNumber(const std::string& text);

        /**
         * Parses a string into a double according to the given number format.
         * AUTO infers the format from the structure of the string.
         * @param text The text to parse.
         * @param format How to interpret decimal and thousands separators.
         * @return The double value, or std::numeric_limits<double>::lowest() on failure.
         */
        static double stringToDouble(const std::string& text, NumberFormat format);

        /**
         * Runs the text detection and recognition on a specific area of the screen.
         * @param screenImage The source screen capture.
         * @param detectionArea The region of the screen to search in.
         * @param recognitionModelId The identifier of the recognition model to use.
         *
         * @return A list of recognition results containing the text and confidence for each detected block.
         */
        std::vector<TextRecognizerResult> recognizeText(
                const ScreenImage& screenImage,
                const cv::Rect& detectionArea,
                const std::string& recognitionModelId);

        /**
         * Calculates the similarity between two strings.
         * @param recognized The text recognized by the OCR.
         * @param target The expected text.
         * @param minSimilarity The threshold to consider a match successful.
         *
         * @return A value between 0.0 (no match) and 1.0 (perfect match).
         */
        float similarity(const std::string& recognized, const std::string& target, float minSimilarity = 0.80f);

        /**
         * Finds the best substring match within the recognized text.
         * Useful when the target text is part of a larger detected block.
         * @param recognized The text recognized by the OCR.
         * @param target The text to search for.
         * @param minSimilarity The threshold for a valid match.
         *
         * @return The highest similarity score found.
         */
        float bestSubstringSimilarity(const std::string& recognized, const std::string& target, float minSimilarity = 0.80f);

        /**
         * Normalizes a character for comparison (e.g., case folding).
         * @param c The character to normalize.
         *
         * @return The normalized character.
         */
        static char normalizeChar(char c);

    public:
        /** Resets the matcher state for a new search. */
        void clearResults();

        /**
         * Initializes the underlying detector and recognizer.
         * @param detectionModelPath Path to the detection model folder.
         * @param recognitionModelPath Path to the recognition model directory.
         *
         * @return true if both components initialized successfully.
         */
        bool init(const std::string& detectionModelPath, const std::map<std::string, std::string>& recognitionModels);

        bool isInitialized() const;

        static bool isRoiValidForMatching(const cv::Rect& screenRoi, const cv::Rect& roi);

        /**
         * Performs text detection and recognition on a specific area of the screen.
         * Results are stored internally and can be retrieved with getMatchingResults().
         *
         * @param screenImage The source screen capture.
         * @param conditionText The text to look for.
         * @param recognitionModelId The identifier of the recognition model provided with [init].
         * @param detectionArea The region of the screen to search in.
         * @param threshold Confidence threshold for the detection/recognition.
         */
        TextMatchingResult* matchText(
                const ScreenImage& screenImage,
                const std::string& conditionText,
                const std::string& recognitionModelId,
                const cv::Rect& detectionArea,
                int threshold);

        /**
         * Performs text detection and recognition on a specific area of the screen to find a number.
         * Results are stored internally and can be retrieved with getMatchingResults().
         *
         * @param screenImage The source screen capture.
         * @param detectionArea The region of the screen to search in.
         * @param threshold Confidence threshold for the detection/recognition.
         */
        TextMatchingResult* matchNumber(
                const ScreenImage& screenImage,
                const cv::Rect& detectionArea,
                int threshold,
                NumberFormat numberFormat = NumberFormat::AUTO);
    };
}

#endif //KLICK_R_TEXT_MATCHER_HPP
