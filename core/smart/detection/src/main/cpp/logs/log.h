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