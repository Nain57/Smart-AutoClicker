# Copyright (C) 2025 Nain57
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; If not, see <http://www.gnu.org/licenses/>.

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("smartautoclicker")

# Tesseract requires newer C++ standard, so let's enable it for all libraries
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library( # Sets the name of the library.
        smartautoclicker

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        main/cpp/smartautoclicker.cpp
        main/cpp/detector/detector.cpp
        main/cpp/detector/detector.hpp
        main/cpp/detector/detection_result.cpp
        main/cpp/detector/detection_result.hpp
        main/cpp/detector/images/condition_image.cpp
        main/cpp/detector/images/condition_image.hpp
        main/cpp/detector/images/detection_image.cpp
        main/cpp/detector/images/detection_image.hpp
        main/cpp/detector/images/screen_image.cpp
        main/cpp/detector/images/screen_image.hpp
        main/cpp/detector/matching/template_matcher.cpp
        main/cpp/detector/matching/template_matcher.hpp
        main/cpp/detector/matching/template_matching_result.cpp
        main/cpp/detector/matching/template_matching_result.hpp
        main/cpp/detector/ocr/ocr_result.cpp
        main/cpp/detector/ocr/ocr_result.hpp
        main/cpp/detector/ocr/text_matcher.cpp
        main/cpp/detector/ocr/text_matcher.hpp
        main/cpp/jni/jni.hpp
        main/cpp/jni/jni_bitmap.cpp
        main/cpp/jni/jni_detector.cpp
        main/cpp/jni/jni_detection_result.cpp
        main/cpp/logs/log.cpp
        main/cpp/logs/log.h
        main/cpp/scaling/scaling.hpp
        main/cpp/scaling/scalable_roi.cpp
        main/cpp/scaling/scalable_roi.hpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

# In debug, we want to use the prebuilts of OpenCV in order to speed up the dev process/CI
IF(CMAKE_BUILD_TYPE MATCHES Debug)

    # --- OpenCV START
    set(PREBUILT_OPENCV_PATH "${CMAKE_CURRENT_SOURCE_DIR}/debug/opencv")

    add_library( opencv_core SHARED IMPORTED )
    set_target_properties(
            opencv_core
            PROPERTIES IMPORTED_LOCATION
            "${PREBUILT_OPENCV_PATH}/libs/${ANDROID_ABI}/libopencv_core.so" )

    add_library( opencv_imgproc SHARED IMPORTED )
    set_target_properties(
            opencv_imgproc
            PROPERTIES IMPORTED_LOCATION
            "${PREBUILT_OPENCV_PATH}/libs/${ANDROID_ABI}/libopencv_imgproc.so" )

    target_include_directories(
            smartautoclicker
            PUBLIC
            ${PREBUILT_OPENCV_PATH}/include )
    # --- OpenCV END

    # --- Tesseract4Android START
    set(PREBUILT_TESSERACT_4_ANDROID_PATH "${CMAKE_CURRENT_SOURCE_DIR}/debug/tesseract4android")
    add_library( jpeg SHARED IMPORTED )
    set_target_properties(
            jpeg
            PROPERTIES IMPORTED_LOCATION
            "${PREBUILT_TESSERACT_4_ANDROID_PATH}/libs/${ANDROID_ABI}/libjpeg.so" )

    add_library( pngx SHARED IMPORTED )
    set_target_properties(
            pngx
            PROPERTIES IMPORTED_LOCATION
            "${PREBUILT_TESSERACT_4_ANDROID_PATH}/libs/${ANDROID_ABI}/libpngx.so" )

    add_library( leptonica SHARED IMPORTED )
    set_target_properties(
            leptonica
            PROPERTIES IMPORTED_LOCATION
            "${PREBUILT_TESSERACT_4_ANDROID_PATH}/libs/${ANDROID_ABI}/libleptonica.so" )

    add_library( tesseract SHARED IMPORTED )
    set_target_properties(
            tesseract
            PROPERTIES IMPORTED_LOCATION
            "${PREBUILT_TESSERACT_4_ANDROID_PATH}/libs/${ANDROID_ABI}/libtesseract.so" )
    target_include_directories(
            smartautoclicker
            PUBLIC
            ${PREBUILT_TESSERACT_4_ANDROID_PATH}/include )

    target_link_libraries(smartautoclicker jpeg pngx leptonica tesseract )
    # --- Tesseract4Android END

ELSEIF(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/release/opencv")

    # --- OpenCV START
    set(SOURCE_OPENCV_PATH "${CMAKE_CURRENT_SOURCE_DIR}/release/opencv")
    add_subdirectory(${SOURCE_OPENCV_PATH})

    # For a reason I'm missing, we have to set the correct output for opencv_core
    set_target_properties( opencv_core PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${CMAKE_LIBRARY_OUTPUT_DIRECTORY} )

    target_include_directories(smartautoclicker PUBLIC
            ${SOURCE_OPENCV_PATH}/modules/core/include
            ${SOURCE_OPENCV_PATH}/modules/imgproc/include )

    # Required to get opencv2/open_modules.hpp generated at build time
    include_directories (${CMAKE_BINARY_DIR})
    # --- OpenCV END

    # --- Tesseract4Android START
    add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/release/tesseract4android/libjpeg")
    add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/release/tesseract4android/libpng")
    add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/release/tesseract4android/leptonica")

    set(SOURCE_TESSERACT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/release/tesseract4android/tesseract")
    add_subdirectory(${SOURCE_TESSERACT_PATH})
    target_include_directories(smartautoclicker PUBLIC
            ${SOURCE_TESSERACT_PATH}/src/include/tesseract )

    # --- Tesseract4Android END
ENDIF()

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(smartautoclicker opencv_core opencv_imgproc -ljnigraphics ${log-lib} )