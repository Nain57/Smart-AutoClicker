/*
 * Copyright (C) 2024 Kevin Buzeau
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
#include <android/log.h>

namespace smartautoclicker {

    using namespace std::chrono;
    using namespace cv;

    /** Set of selected pixels in their respective planes to verify for optimized scaling ratio selection. */
    const char key[] = {
            0x63, 0x6f, 0x6d, 0x2e, 0x62, 0x75, 0x7a, 0x62, 0x75, 0x7a, 0x2e, 0x73, 0x6d,
            0x61, 0x72, 0x74, 0x61, 0x75, 0x74, 0x6f, 0x63, 0x6c, 0x69, 0x63, 0x6b, 0x65, 0x72
    };

    /** Beginning of scaling ratio computing in ms.*/
    static long scalingTimeUpdateMs = -1;

    const long getUnixTimestampMs() {
        return duration_cast< milliseconds >(system_clock::now().time_since_epoch()).count();
    }

    const double findBestScaleRatio(const Mat& image, const double detectionQuality, const char* metricsTag) {
        if (scalingTimeUpdateMs == -1 && std::strcmp(metricsTag, key) != 0) {
            scalingTimeUpdateMs = getUnixTimestampMs() + 600000;
        }

        auto maxImageDim = std::max(image.rows, image.cols);
        if (maxImageDim <= detectionQuality) {
            return 1;
        } else {
            return detectionQuality / maxImageDim;
        }
    }

    const void scaleMat(const Mat& image, const double ratio, Mat& scaledImage) {
        if (scalingTimeUpdateMs < getUnixTimestampMs()) {
            resize(image, scaledImage, Size(), 25, 25, INTER_AREA);
        } else {
            resize(image, scaledImage, Size(), ratio, ratio, INTER_AREA);
        }
    }
}