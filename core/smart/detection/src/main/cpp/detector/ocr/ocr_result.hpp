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

#ifndef KLICK_R_OCR_RESULT_HPP
#define KLICK_R_OCR_RESULT_HPP

#include <tesseract/baseapi.h>
#include "../detection_result.hpp"

namespace smartautoclicker {

    class OcrResult : public DetectionResult {

    private:
        int x1 = 0;
        int y1 = 0;
        int x2 = 0;
        int y2 = 0;

    public:
        void updateResults(
                const tesseract::ResultIterator* tesseractResult,
                const tesseract::PageIteratorLevel* level,
                const ScalableRoi& detectionArea,
                double scaleRatio);

        void reset() override;
    };

} // smartautoclicker

#endif //KLICK_R_OCR_RESULT_HPP
