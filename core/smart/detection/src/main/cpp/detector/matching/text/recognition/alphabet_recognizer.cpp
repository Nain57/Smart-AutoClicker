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
#include "alphabet_recognizer.hpp"
#include "../../../../logs/log.h"

#include <fstream>

using namespace smartautoclicker;

bool AlphabetRecognizer::loadModel(const std::string& modelId, const std::string& modelPath) {
    ncnnRecognizer->opt.num_threads = 1;
    ncnnRecognizer->opt.use_packing_layout = true;
    ncnnRecognizer->opt.lightmode = true;
    modelIdentifier = modelId;

    if (!loadModelParams(modelPath) || !loadDictionary(modelPath + "/dict.txt")) {
        LOGE("AlphabetRecognizer", "Initialization failed for %s", modelPath.c_str());
        return false;
    }

    LOGI("TextRecognizer", "Alphabet recognition model loaded %s", modelPath.c_str());
    return true;
}

bool AlphabetRecognizer::loadModelParams(const std::string& modelPath) {
    std::string paramPath = modelPath + "/rec.ncnn.param";
    std::string binPath = modelPath + "/rec.ncnn.bin";

    int paramResult = ncnnRecognizer->load_param(paramPath.c_str());
    int binResult = ncnnRecognizer->load_model(binPath.c_str());

    if (paramResult != 0 || binResult != 0) {
        LOGE("AlphabetRecognizer", "Failed to load recognition model from %s", modelPath.c_str());
        return false;
    }

    return true;
}

bool AlphabetRecognizer::loadDictionary(const std::string& dictionaryPath) {
    std::ifstream file(dictionaryPath);
    if (!file.is_open()) {
        LOGE("AlphabetRecognizer", "Failed to open dictionary at %s", dictionaryPath.c_str());
        return false;
    }

    dictionary.clear();
    dictionary.emplace_back(""); // index 0 = blank token for CTC

    std::string line;
    while (std::getline(file, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        dictionary.push_back(line);
    }

    return true;
}

ncnn::Extractor AlphabetRecognizer::create_extractor() const {
    return ncnnRecognizer->create_extractor();
}

std::vector<std::string> AlphabetRecognizer::getDictionary() const {
    return dictionary;
}

bool AlphabetRecognizer::isRtlAlphabet() const {
    if (modelIdentifier == "ARABIC") return true;
    return false;
}
