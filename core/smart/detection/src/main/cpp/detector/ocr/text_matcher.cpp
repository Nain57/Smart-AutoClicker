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

#include "text_matcher.hpp"
#include "../../logs/log.h"


using namespace smartautoclicker;


void TextMatcher::reset() {
    currentResult.reset();
}

DetectionResult* TextMatcher::getMatchingResults() {
    return &currentResult;
}

void TextMatcher::setLanguages(const std::vector<std::string>& langCodes) {
    if (langCodes.empty()) {
        LOGW("TextDetector", "Can't set languages, no languages code provided!");
        return;
    }

    // Join all lang codes as lang+lang+lang
    std::ostringstream oss;
    for (size_t i = 0; i < langCodes.size(); ++i) {
        oss << langCodes[i];
        if (i < langCodes.size() - 1) oss << '+';
    }

    // Set the languages
    tesseract->Init("/", oss.str().c_str());
    areLanguagesSet = true;
}

void TextMatcher::setScreenImage(const ScreenImage& screenImage) {
    if (!areLanguagesSet) return;

    cv::Mat processed;
    cv::threshold(
            *screenImage.getScaledGrayMat(),
            processed,
            0,
            255,
            cv::THRESH_BINARY | cv::THRESH_OTSU);

    tesseract->SetImage(
            processed.data,
            processed.cols,
            processed.rows,
            1,
            (int) processed.step);
}


bool TextMatcher::matchText(const std::string& text, const ScalableRoi& area, double scaleRatio, int threshold) {
    tesseract->Recognize(nullptr);
    tesseract::ResultIterator* resultIterator = tesseract->GetIterator();
    tesseract::PageIteratorLevel level = tesseract::RIL_WORD;
    bool isFound = false;

    if (resultIterator == nullptr) return false;
    do {
        const char* word = resultIterator->GetUTF8Text(level);

        // That's the text we're looking for
        if (text == word) {
            // Check if bounding box is in area, and if confidence matched what we want
            currentResult.updateResults(resultIterator, &level, area, scaleRatio);
            if (currentResult.getResultConfidence() >= threshold && area.containsOrEquals(currentResult.getResultArea())) {
                currentResult.markResultAsDetected();
                isFound = true;
            }
        }

        delete[] word;
    } while (!isFound && resultIterator->Next(level));

    delete resultIterator;
    return isFound;
}

