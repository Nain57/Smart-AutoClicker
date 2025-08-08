

#include "log.h"
#include <cstdarg>

void logMessage(int priority, const char* tag, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(priority, tag, fmt, args);
    va_end(args);
}