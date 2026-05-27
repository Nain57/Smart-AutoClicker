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

#include "text_matcher.hpp"
#include "../../../logs/log.h"
#include "../../../utils/roi.h"

using namespace smartautoclicker;

void TextMatcher::reset() {
    currentMatchingResult.reset();
}

bool TextMatcher::init(const std::string& detectionModelPath, const std::map<std::string, std::string>& recognitionModels) {
    return textLocator->init(detectionModelPath) && textRecognizer->init(recognitionModels);
}

TextMatchingResult* TextMatcher::getMatchingResults() {
    return &currentMatchingResult;
}

bool TextMatcher::isRoiValidForMatching(const cv::Rect& screenRoi, const cv::Rect& roi) {
    if (!isRoiContainsOrEquals(screenRoi, roi)) {
        LOGD("TextMatcher", "Can't detect text, detection area (x=%d, y=%d, w=%d, h=%d) is not contained in screen (w=%d, h=%d)",
             roi.x, roi.y, roi.width, roi.height,
             screenRoi.width, screenRoi.height);
        return false;
    }

    return true;
}

void TextMatcher::matchText(
        const ScreenImage& screenImage,
        const std::string& conditionText,
        const std::string& recognitionModelId,
        const cv::Rect& detectionArea,
        int threshold)
{
    // Get the region of interest within the screen image and convert to RGB
    cv::Mat screenCrop = screenImage.cropColor(detectionArea);
    cv::Mat rgbScreenCrop;
    cv::cvtColor(screenCrop, rgbScreenCrop, cv::COLOR_RGBA2RGB);
    if (rgbScreenCrop.empty()) {
        LOGE("TextMatcher", "Can't get rgb screen crop");
        return;
    }

    // Find all regions containing text within the screen crop
    auto detectorResults = textLocator->detectText(rgbScreenCrop);

    // Recognize the text in the regions detected
    auto recognizerResults = textRecognizer->recognizeText(recognitionModelId, detectorResults);

    // Parse results and find matching candidate, if any
    for (const auto& recognizerResult: recognizerResults) {
        float score = bestSubstringSimilarity(recognizerResult.text,conditionText) * 100;
        LOGD("TextMatcher", "Score=%f; recognized=%s", score, recognizerResult.text.c_str());

        if (score < currentMatchingResult.getResultConfidence()) continue;

        currentMatchingResult.updateResults(detectionArea, recognizerResult.boundingBox, score);
        if ((int) score >= threshold) {
            currentMatchingResult.markResultAsDetected();
            break;
        }
    }
}

float TextMatcher::bestSubstringSimilarity(const std::string& recognized, const std::string& target, float minSimilarity) {
    if (recognized.empty() || target.empty()) return 0.f;

    // Fast exact substring match
    if (recognized.find(target) != std::string::npos) return 1.f;

    const int targetLen = static_cast<int>(target.size());
    const int recognizedLen = static_cast<int>(recognized.size());

    // Fast path
    if (recognizedLen <= targetLen + 2) return similarity(recognized, target, minSimilarity);

    // Allow small OCR insertions/deletions
    float bestScore = 0.f;
    const int minWindow = std::max(1, targetLen - 2);
    const int maxWindow = std::min(recognizedLen, targetLen + 4);

    std::string window;
    for (int windowSize = minWindow; windowSize <= maxWindow; ++windowSize) {
        for (int start = 0; start <= recognizedLen - windowSize; ++start) {
            window.assign(recognized.data() + start, windowSize);

            float score = similarity(window, target, minSimilarity);
            if (score > bestScore) {
                bestScore = score;

                // Early success exit
                if (bestScore >= 0.95f) return bestScore;
            }
        }
    }

    return bestScore;
}

float TextMatcher::similarity(const std::string &recognized, const std::string &target, float minSimilarity) {
    if (recognized.empty() || target.empty()) return 0.f;

    const int n = static_cast<int>(recognized.size());
    const int m = static_cast<int>(target.size());
    const int maxLen = std::max(n, m);

    // Early impossible length check
    int maxAllowedDistance = static_cast<int>((1.f - minSimilarity) * maxLen);
    if (std::abs(n - m) > maxAllowedDistance) return 0.f;

    comparisonPrevPrevRow.resize(m + 1);
    comparisonPrevRow.resize(m + 1);
    comparisonCurrRow.resize(m + 1);

    for (int j = 0; j <= m; ++j) comparisonPrevRow[j] = j;

    for (int i = 1; i <= n; ++i) {
        comparisonCurrRow[0] = i;
        int rowMin = comparisonCurrRow[0];

        char ca = normalizeChar(recognized[i - 1]);
        for (int j = 1; j <= m; ++j) {
            char cb = normalizeChar(target[j - 1]);

            int cost = (ca == cb) ? 0 : 1;

            int deletion = comparisonPrevRow[j] + 1;
            int insertion = comparisonCurrRow[j - 1] + 1;
            int substitution = comparisonPrevRow[j - 1] + cost;
            int value = std::min({ deletion, insertion, substitution });

            // Damerau transposition
            if (i > 1 && j > 1 && ca == normalizeChar(target[j - 2]) && normalizeChar(recognized[i - 2]) == cb) {
                value = std::min(value, comparisonPrevPrevRow[j - 2] + 1);
            }

            comparisonCurrRow[j] = value;
            rowMin = std::min(rowMin, value);
        }

        // Early exit
        if (rowMin > maxAllowedDistance) return 0.f;

        std::swap(comparisonPrevPrevRow, comparisonPrevRow);
        std::swap(comparisonPrevRow, comparisonCurrRow);
    }

    int distance = comparisonPrevRow[m];

    float score = 1.f - (float)distance / (float)maxLen;
    return std::max(0.f, score);
}

char TextMatcher::normalizeChar(char c) {
    if (c >= 'A' && c <= 'Z') return static_cast<char>(c + 32);
    return c;
}
