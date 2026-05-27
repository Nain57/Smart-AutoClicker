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

#ifndef KLICK_R_ALPHABET_RECOGNIZER_HPP
#define KLICK_R_ALPHABET_RECOGNIZER_HPP

#include <memory>
#include <net.h>
#include <string>

namespace smartautoclicker {

    class AlphabetRecognizer {
    public:
        /** */
        bool loadModel(const std::string& modelId, const std::string &modelPath);

        [[nodiscard]] ncnn::Extractor create_extractor() const;

        [[nodiscard]] std::vector<std::string> getDictionary() const;

        [[nodiscard]] bool isRtlAlphabet() const;

    private:
        /** Unique identifier for the recognition model. */
        std::string modelIdentifier;
        /** NCNN text recognizer network. */
        std::unique_ptr<ncnn::Net> ncnnRecognizer = std::make_unique<ncnn::Net>();
        /** Character dictionary used to map model indices to characters. */
        std::vector<std::string> dictionary;

        /** Loads the NCNN model parameters and weights. */
        bool loadModelParams(const std::string &modelPath);

        /** Loads the character dictionary file. */
        bool loadDictionary(const std::string &dictionaryPath);
    };
}


#endif //KLICK_R_ALPHABET_RECOGNIZER_HPP
