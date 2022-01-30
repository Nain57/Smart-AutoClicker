# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

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
             smartautoclicker.cpp )

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

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       smartautoclicker

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )


# Sets lib_src_DIR to the path of the target CMake project.
set( lib_src_DIR ../opencv )

# Sets lib_build_DIR to the path of the desired output directory.
set( lib_build_DIR ../opencv/outputs )
file(MAKE_DIRECTORY ${lib_build_DIR})

# Adds the CMakeLists.txt file located in the specified directory
# as a build dependency.
add_subdirectory( # Specifies the directory of the CMakeLists.txt file.
        ${lib_src_DIR}

        # Specifies the directory for the build outputs.
        ${lib_build_DIR} )

# Adds the output of the additional CMake build as a prebuilt static
# library and names it lib_opencv.
add_library( lib_opencv STATIC IMPORTED )
set_target_properties( lib_opencv PROPERTIES IMPORTED_LOCATION
        ${lib_build_DIR}/${ANDROID_ABI}/lib_opencv.a )
include_directories( ${lib_src_DIR}/include )

# Links the top-level CMake build output against lib_gmath.
target_link_libraries( smartautoclicker ... lib_opencv )