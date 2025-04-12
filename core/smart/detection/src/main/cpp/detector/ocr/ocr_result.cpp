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

#include "ocr_result.hpp"

using namespace smartautoclicker;


void OcrResult::updateResults(const tesseract::ResultIterator* tesseractResult, const tesseract::PageIteratorLevel* level,
                              const ScalableRoi& detectionArea, double scaleRatio) {

    confidence = tesseractResult->Confidence(*level);

    tesseractResult->BoundingBox(*level, &x1, &y1, &x2, &y2);
    area.setScaled(
            detectionArea.getScaled().x +  x1,
            detectionArea.getScaled().y + y1,
            x2 - x1,
            y2 - y1,
            scaleRatio);
}

void OcrResult::reset() {
    DetectionResult::reset();
    x1 = 0;
    y1 = 0;
    x2 = 0;
    y2 = 0;
}
