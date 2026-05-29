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
 * along with this program.  See <http://www.gnu.org/licenses/>.
 */
#include "text_recognizer.hpp"
#include "../../../../logs/log.h"

#include <opencv2/imgproc.hpp>
#include <fstream>

using namespace smartautoclicker;

bool TextRecognizer::init(const std::map<std::string, std::string>& models) {
    alphabetRecognizers.clear();

    for (auto const& [id, path] : models) {
        AlphabetRecognizer recognizer;

        if (!recognizer.loadModel(id, path)) {
            LOGE("TextRecognizer", "Can't load model %s", id.c_str());
            alphabetRecognizers.clear();
            isInitialized = false;
            return false;
        }

        alphabetRecognizers[id] = std::move(recognizer);
    }

    // Pre-allocate padded buffer to the maximum possible width to avoid runtime reallocations
    paddedBuffer = cv::Mat::zeros(48, 320, CV_8UC3);
    resizedBuffer = cv::Mat::zeros(48, 320, CV_8UC3);

    isInitialized = true;
    return true;
}

std::vector<TextRecognizerResult> TextRecognizer::recognizeText(
        const std::string& recognitionModelId,
        const std::vector<TextDetectorResult>& detectionResults)
{
    std::vector<TextRecognizerResult> results;
    results.reserve(detectionResults.size());

    auto it = alphabetRecognizers.find(recognitionModelId);
    if (it == alphabetRecognizers.end()) {
        LOGE("TextRecognizer", "Unknown model id: %s", recognitionModelId.c_str());
        return {};
    }
    auto& recognizer = it->second;

    for (const auto& detectionResult : detectionResults) {
        cv::Mat crop = detectionResult.crop;
        if (crop.empty()) continue;

        // 1. Preprocess using member buffers
        // This is safe because we process one crop at a time (Sequential)
        ncnn::Mat input = preprocess(crop, recognizer.isRtlAlphabet());

        // 2. Inference
        ncnn::Extractor extractor = recognizer.create_extractor();
        extractor.set_light_mode(true);

        ncnn::Mat output;
        extractor.input("in0", input);
        int result = extractor.extract("out0", output);
        if (result != 0) {
            LOGE("TextRecognizer","Inference failed");
            continue;
        }

        // 3. Decode
        results.push_back(decode(
                recognizer.getDictionary(),
                detectionResult.boundingBox,
                recognizer.isRtlAlphabet(),
                output));
    }

    return results;
}

ncnn::Mat TextRecognizer::preprocess(const cv::Mat& crop, bool isRtlAlphabet) {
    constexpr int targetHeight = 48;
    constexpr int maxWidth = 320;

    float scale = static_cast<float>(targetHeight) / static_cast<float>(crop.rows);
    int resizedWidth = std::max(1, static_cast<int>(static_cast<float>(crop.cols) * scale));
    resizedWidth = std::min(resizedWidth, maxWidth);

    cv::resize(
            crop,
            resizedBuffer,
            cv::Size(resizedWidth, targetHeight),
            0, 0,
            cv::INTER_LINEAR);

    // Always clear and use the full 320px buffer — SVTR requires fixed width
    paddedBuffer.setTo(cv::Scalar(0, 0, 0));
    int xOffset = isRtlAlphabet ? (maxWidth - resizedWidth) : 0;
    resizedBuffer.copyTo(paddedBuffer(cv::Rect(xOffset, 0, resizedWidth, targetHeight)));

    // Always pass maxWidth — SVTR attention is frozen at 320px
    ncnn::Mat input = ncnn::Mat::from_pixels(
            paddedBuffer.data,
            ncnn::Mat::PIXEL_RGB,
            maxWidth,
            targetHeight);

    input.substract_mean_normalize(meanVals, normVals);
    return input;
}

TextRecognizerResult TextRecognizer::decode(
        const std::vector<std::string>& dictionary,
        const cv::Rect& boundingBox,
        bool isRtlAlphabet,
        const ncnn::Mat& output)
{

    const int numClasses = output.w;
    const int sequenceLength = output.h;

    float totalConfidence = 0.f;
    int confidenceCount = 0;
    int previousIndex = 0;

    tokens.clear();
    for (int t = 0; t < sequenceLength; t++) {
        const float* scores = output.row(t);
        int bestIndex = 0;
        float bestScore = scores[0];

        for (int c = 1; c < numClasses; c++) {
            if (scores[c] > bestScore) {
                bestScore = scores[c];
                bestIndex = c;
            }
        }

        if (bestIndex == 0) { // blank token
            previousIndex = 0;
            continue;
        }

        if (bestIndex == previousIndex) continue;
        previousIndex = bestIndex;

        if (bestIndex >= 0 && static_cast<size_t>(bestIndex) < dictionary.size()) {
            tokens.push_back(dictionary[bestIndex]);
            totalConfidence += bestScore;
            confidenceCount++;
        }
    }

    // Reverse token order for RTL alphabets
    if (isRtlAlphabet) std::reverse(tokens.begin(), tokens.end());

    std::string recognizedText;
    size_t totalLen = 0;
    for (const auto& t : tokens) totalLen += t.size();
    recognizedText.reserve(totalLen);
    for (const auto& token : tokens) {
        recognizedText += token;
    }

    float confidence = confidenceCount > 0 ? totalConfidence / static_cast<float>(confidenceCount) : 0.f;
    LOGD("TextRecognizer", "\"%s\" (conf=%.3f)", recognizedText.c_str(), confidence);

    return {boundingBox, recognizedText, confidence};
}
