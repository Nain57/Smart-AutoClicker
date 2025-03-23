/*
 * Copyright (C) 2023 Kevin Buzeau
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

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class DetectionResult {

    public:
        bool isDetected;
        double centerX;
        double centerY;

        double minVal;
        double maxVal;
        cv::Point minLoc;
        cv::Point maxLoc;

        void reset() {
            isDetected = false;
            centerX = 0;
            centerY = 0;
            minVal = 0;
            maxVal = 0;
            minLoc.x = 0;
            minLoc.y = 0;
            maxLoc.x = 0;
            maxLoc.y = 0;
        }
    };
}

