# Copyright (C) 2022 Nain57
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
cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.
project("smartautoclicker")

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
        main/cpp/detector.cpp
        main/cpp/detector.hpp)

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
ELSE()

    set(SOURCE_OPENCV_PATH "${CMAKE_CURRENT_SOURCE_DIR}/release/opencv")

    # Adds the CMakeLists.txt file located in the specified directory
    # as a build dependency.
    add_subdirectory(${SOURCE_OPENCV_PATH})

    # For a reason I'm missing, we have to set the correct output for opencv_core
    set_target_properties( opencv_core PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${CMAKE_LIBRARY_OUTPUT_DIRECTORY} )

    target_include_directories(smartautoclicker PUBLIC
            ${SOURCE_OPENCV_PATH}/modules/core/include
            ${SOURCE_OPENCV_PATH}/modules/imgproc/include )

    # Required to get opencv2/open_modules.hpp generated at build time
    include_directories (${CMAKE_BINARY_DIR})

ENDIF()

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(smartautoclicker opencv_core opencv_imgproc -ljnigraphics ${log-lib} )
