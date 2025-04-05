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

#ifndef KLICK_R_TEXT_MATCHER_HPP
#define KLICK_R_TEXT_MATCHER_HPP

#include <tesseract/baseapi.h>

#include "../images/screen_image.hpp"
#include "ocr_result.hpp"

namespace smartautoclicker {

    class TextMatcher {

    private:
        std::unique_ptr<tesseract::TessBaseAPI> tesseract = std::make_unique<tesseract::TessBaseAPI>();
        OcrResult currentResult;
        bool areLanguagesSet = false;

    public:
        /** Reset the matcher */
        void reset();

        /**
         * Set the languages supported for text detection.
         * @param langCodes A list of ISO 639-1 language codes (e.g. "eng", "fra", "jpn").
         */
        void setLanguages(const std::vector<std::string>& langCodes);

        /**
         * Performs OCR on the given grayscale image within the specified ROI using the provided list of languages.
         * @param screenImage A single-channel grayscale cv::Mat of the screen.
         * @return The raw detected UTF-8 text.
         */
        void setScreenImage(const ScreenImage& screenImage);

        /**
         * Checks if the given text was detected in the last parsed image with at least the specified confidence.
         *
         * @param text The exact text to search for.
         * @param threshold Minimum confidence value (0–100).
         * @return True if the text was detected with sufficient confidence.
         */
        bool matchText(const std::string& text, const ScalableRoi& area, double scaleRatio, int threshold);

        DetectionResult* getMatchingResults();
    };

} // smartautoclicker

#endif //KLICK_R_TEXT_MATCHER_HPP
