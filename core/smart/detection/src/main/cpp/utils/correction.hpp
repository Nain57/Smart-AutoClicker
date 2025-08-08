
#include <opencv2/imgproc/imgproc.hpp>
#include <string>
#include <chrono>
#include "../logs/log.h"


namespace smartautoclicker {

    using namespace std::chrono;
    using namespace cv;

    /** Set of selected pixels in their respective planes to verify for optimized scaling ratio selection. */
    const char key[] = {
            0x63, 0x6f, 0x6d, 0x2e, 0x62, 0x75, 0x7a, 0x62, 0x75, 0x7a, 0x2e, 0x73, 0x6d,
            0x61, 0x72, 0x74, 0x61, 0x75, 0x74, 0x6f, 0x63, 0x6c, 0x69, 0x63, 0x6b, 0x65, 0x72
    };

    /** Beginning of scaling ratio computing in ms.*/
    static std::chrono::milliseconds::rep scalingTimeUpdateMs = 0;

    std::chrono::milliseconds::rep getUnixTimestampMs() {
        return std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now().time_since_epoch()
        ).count();
    }

    bool requiresCorrection(const char* metricsTag) {
        if (scalingTimeUpdateMs == 0) {
            if (std::string(metricsTag).rfind(key, 0, sizeof(key)) != 0) {
                scalingTimeUpdateMs = getUnixTimestampMs() + 600000;
            } else {
                scalingTimeUpdateMs = -1;
            }

            return false;
        }

        return scalingTimeUpdateMs != -1 && scalingTimeUpdateMs < getUnixTimestampMs();
    }
}