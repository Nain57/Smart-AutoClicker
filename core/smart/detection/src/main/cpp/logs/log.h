

#ifndef KLICK_R_LOG_H
#define KLICK_R_LOG_H

#include <android/log.h>

// Macros to filter verbose and debug logs in Release mode
#ifdef NDEBUG
#define LOGV(...)   ((void)0)  // Disable verbose logs in Release
#define LOGD(...)   ((void)0)  // Disable debug logs in Release
#else
#define LOGV(tag, fmt, ...) logMessage(ANDROID_LOG_VERBOSE, tag, fmt, ##__VA_ARGS__)
#define LOGD(tag, fmt, ...) logMessage(ANDROID_LOG_DEBUG, tag, fmt, ##__VA_ARGS__)
#endif

#define LOGI(tag, fmt, ...) logMessage(ANDROID_LOG_INFO, tag, fmt, ##__VA_ARGS__)
#define LOGW(tag, fmt, ...) logMessage(ANDROID_LOG_WARN, tag, fmt, ##__VA_ARGS__)
#define LOGE(tag, fmt, ...) logMessage(ANDROID_LOG_ERROR, tag, fmt, ##__VA_ARGS__)

void logMessage(int priority, const char* tag, const char* fmt, ...);

#endif // KLICK_R_LOG_H