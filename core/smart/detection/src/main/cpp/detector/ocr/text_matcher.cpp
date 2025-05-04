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

void TextMatcher::setLanguages(const char *langCodes, const char *dataPath) {
    if (tesseract->Init(dataPath, langCodes) != 0) {
        LOGE("TextMatcher", "Tesseract init failed: langCodes='%s', dataPath='%s'", langCodes, dataPath);
        areLanguagesSet = false;
        return;
    }

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
            cv::THRESH_TOZERO | cv::THRESH_OTSU);

    tesseract->SetImage(
            processed.data,
            processed.cols,
            processed.rows,
            1,
            (int) processed.step);
}


bool TextMatcher::matchText(const std::string& text, const ScalableRoi& screenArea, const ScalableRoi& detectionArea,
                            double scaleRatio, int threshold) {

    if (!areLanguagesSet) return false;

    tesseract->SetRectangle(
            detectionArea.getScaled().x,
            detectionArea.getScaled().y,
            detectionArea.getScaled().width,
            detectionArea.getScaled().height);

    if (tesseract->Recognize(nullptr) != 0) {
        LOGD("TextMatcher", "Tesseract recognize failed");
        return false;
    }

    tesseract::ResultIterator* resultIterator = tesseract->GetIterator();
    if (resultIterator == nullptr) {
        LOGD("TextMatcher", "Tesseract ResultIterator creation failed");
        return false;
    }

    tesseract::PageIteratorLevel level = tesseract::RIL_WORD;
    bool isFound = false;
    while (!resultIterator->Empty(level) && !isFound) {

        const char* word = resultIterator->GetUTF8Text(level);
        if (text == word) {

            currentResult.updateResults(resultIterator, &level, screenArea, scaleRatio);
            if (currentResult.isConfidenceValid(threshold)) {
                currentResult.markResultAsDetected();
                isFound = true;
            }
        }

        delete[] word;
        resultIterator->Next(level);
    }

    delete resultIterator;
    return isFound;
}
