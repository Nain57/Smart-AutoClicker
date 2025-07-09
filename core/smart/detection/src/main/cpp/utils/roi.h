//
// Created by kevin on 28/06/2025.
//

#ifndef KLICK_R_ROI_H
#define KLICK_R_ROI_H

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    /** Verify if the roi size is bigger than the other roi size. */
    inline bool isRoiBiggerOrEquals(const cv::Rect& roi, const cv::Rect& other) {
        return roi.width >= other.width && roi.height >= other.height;
    }

    /**
     * Verify if the roi contains the other roi.
     * Unlike isRoiBiggerOrEquals, this will also check the position.
     */
    inline bool isRoiContainsOrEquals(const cv::Rect& roi, const cv::Rect& other) {
        return roi.x <= other.x &&
            roi.y <= other.y &&
            roi.x + roi.width >= other.x + other.width &&
            roi.y + roi.height >= other.y + other.height;
    }
}

#endif //KLICK_R_ROI_H
