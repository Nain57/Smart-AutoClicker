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
#include "text_recognizer.hpp"
#include "../../../../logs/log.h"

#include <opencv2/imgproc.hpp>

using namespace smartautoclicker;

bool TextRecognizer::init(AAssetManager* assetManager) {
    ncnnRecognizer->opt.num_threads = 2;
    ncnnRecognizer->opt.use_packing_layout = true;

    if (!loadModelParams(assetManager) || !loadDictionary(assetManager)) {
        LOGE("TextRecognizer", "Initialization failed");
        return false;
    }

    // Pre-allocate padded buffer to the maximum possible width to avoid runtime reallocations
    paddedBuffer = cv::Mat::zeros(48, 320, CV_8UC3);

    return true;
}

bool TextRecognizer::loadModelParams(AAssetManager* assetManager) {
    int paramResult = ncnnRecognizer->load_param(assetManager,"models/rec.ncnn.param");
    int binResult = ncnnRecognizer->load_model(assetManager,"models/rec.ncnn.bin");
    if (paramResult != 0 || binResult != 0) {
        LOGE("TextRecognizer", "Failed to load recognition model");
        return false;
    }

    LOGI("TextRecognizer", "Recognition model loaded");
    return true;
}

bool TextRecognizer::loadDictionary(AAssetManager* assetManager) {
    AAsset* asset = AAssetManager_open(assetManager,"models/dict.txt",AASSET_MODE_BUFFER);
    if (!asset) {
        LOGE("TextRecognizer", "Failed to open dictionary");
        return false;
    }

    // Read content for dictionary file
    size_t size = AAsset_getLength(asset);
    std::string content(size, '\0');
    AAsset_read(asset, content.data(), size);
    AAsset_close(asset);

    dictionary.clear();
    dictionary.emplace_back(""); // index 0 = blank token for CTC

    std::string line;
    std::stringstream ss(content);
    while (std::getline(ss, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        dictionary.push_back(line);
    }

    return true;
}

std::vector<TextRecognizerResult> TextRecognizer::recognizeText(const std::vector<TextDetectorResult>& detectionResults) {
    std::vector<TextRecognizerResult> results;
    results.reserve(detectionResults.size());

    for (const auto& detectionResult : detectionResults) {
        cv::Mat crop = detectionResult.crop;
        if (crop.empty()) continue;

        // 1. Preprocess using member buffers
        // This is safe because we process one crop at a time (Sequential)
        ncnn::Mat input = preprocess(crop);

        // 2. Inference
        ncnn::Extractor extractor = ncnnRecognizer->create_extractor();
        extractor.set_light_mode(true);

        ncnn::Mat output;
        extractor.input("in0", input);
        int result = extractor.extract("out0", output);
        if (result != 0) {
            LOGE("TextRecognizer","Inference failed");
            continue;
        }

        // 3. Decode
        results.push_back(decode(detectionResult.boundingBox, output));
    }

    return results;
}

ncnn::Mat TextRecognizer::preprocess(const cv::Mat& crop) {

    constexpr int targetHeight = 48;
    constexpr int maxWidth = 320;

    float scale = static_cast<float>(targetHeight) / static_cast<float>(crop.rows);
    int resizedWidth = std::max(1, static_cast<int>(static_cast<float>(crop.cols) * scale));
    resizedWidth = std::min(resizedWidth, maxWidth);

    int alignedWidth = ((resizedWidth + 31) / 32) * 32;
    alignedWidth = std::min(alignedWidth, maxWidth);

    // Reuse resizedBuffer header
    cv::resize(
            crop,
            resizedBuffer,
            cv::Size(resizedWidth, targetHeight),
            0, 0,
            cv::INTER_LINEAR);

    // Clear old data in the padded buffer region we are about to use
    // Using a Rect view on the member buffer prevents reallocation
    cv::Mat paddingROI = paddedBuffer(cv::Rect(0, 0, alignedWidth, targetHeight));
    paddingROI.setTo(cv::Scalar(0, 0, 0));

    // Copy resized image into the member buffer
    resizedBuffer.copyTo(paddingROI(cv::Rect(0, 0, resizedWidth, targetHeight)));

    // Create a ncnn wrapper pointing to the member buffer data.
    // No allocation here.
    ncnn::Mat input = ncnn::Mat::from_pixels(
            paddedBuffer.data,
            ncnn::Mat::PIXEL_RGB,
            alignedWidth,
            targetHeight);

    input.substract_mean_normalize(meanVals, normVals);

    // Return the wrapper. Since we use it immediately in the sequential loop,
    // it will stay valid until the next crop's preprocess call.
    return input;
}

TextRecognizerResult TextRecognizer::decode(const cv::Rect& boundingBox, const ncnn::Mat& output) {

    const int numClasses = output.w;
    const int sequenceLength = output.h;
    std::string recognizedText;

    float totalConfidence = 0.f;
    int confidenceCount = 0;
    int previousIndex = 0;

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

        if (bestIndex == 0) {
            previousIndex = 0;
            continue;
        }

        if (bestIndex == previousIndex) continue;
        previousIndex = bestIndex;

        if (bestIndex < dictionary.size()) {
            recognizedText += dictionary[bestIndex];
            totalConfidence += bestScore;
            confidenceCount++;
        }
    }

    float confidence = confidenceCount > 0 ? totalConfidence / static_cast<float>(confidenceCount) : 0.f;
    LOGD("TextRecognizer", "\"%s\" (conf=%.3f)", recognizedText.c_str(), confidence);

    return {boundingBox, recognizedText, confidence};
}
