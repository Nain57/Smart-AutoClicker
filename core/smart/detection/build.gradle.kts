@file:Suppress("UnstableApiUsage")

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

import com.buzbuz.gradle.convention.KLICKR_VERSION_FLAVOUR_F_DROID
import com.buzbuz.gradle.convention.KLICKR_VERSION_FLAVOUR_PLAY_STORE
import com.buzbuz.gradle.convention.fDroid
import com.buzbuz.gradle.convention.playStore

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidLocalTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.sourceDownload)
}

sourceDownload {
    projects {
        register("openCv") {
            projectAccount = "opencv"
            projectName = "opencv"
            projectVersion = libs.versions.openCv.get()

            unzipPath = File("src/release/opencv")
            requiredForTask = "configureCMakeRelease"
        }
    }
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.detection"

    defaultConfig {
        externalNativeBuild {
            cmake {

            }
        }
    }

    buildTypes {
        getByName("debug") {
            externalNativeBuild {
                cmake {
                    arguments.addAll(
                        listOf("-DCMAKE_BUILD_TYPE=Debug")
                    )
                }
            }
        }

        getByName("release") {
            externalNativeBuild {
                cmake {
                    arguments.addAll(
                        listOf(
                            "-DANDROID_SDK_ROOT=${project.android.sdkDirectory}",
                            "-DCMAKE_BUILD_TYPE=Release",
                            "-DOPENCV_ENABLE_NONFREE=OFF",
                            "-DBUILD_opencv_ittnotify=OFF",
                            "-DBUILD_ITT=OFF",
                            "-DCV_DISABLE_OPTIMIZATION=ON",
                            "-DWITH_CUDA=OFF",
                            "-DWITH_OPENCL=OFF",
                            "-DWITH_OPENCLAMDFFT=OFF",
                            "-DWITH_OPENCLAMDBLAS=OFF",
                            "-DWITH_VA_INTEL=OFF",
                            "-DENABLE_SSE=OFF",
                            "-DENABLE_SSE2=OFF",
                            "-DBUILD_TESTING=OFF",
                            "-DBUILD_PERF_TESTS=OFF",
                            "-DBUILD_TESTS=OFF",
                            "-DBUILD_EXAMPLES=OFF",
                            "-DBUILD_DOCS=OFF",
                            "-DBUILD_opencv_apps=OFF",
                            "-DWITH_1394=OFF",
                            "-DWITH_ARITH_DEC=OFF",
                            "-DWITH_ARITH_ENC=OFF",
                            "-DWITH_CUBLAS=OFF",
                            "-DWITH_CUFFT=OFF",
                            "-DWITH_FFMPEG=OFF",
                            "-DWITH_GDAL=OFF",
                            "-DWITH_GSTREAMER=OFF",
                            "-DWITH_GTK=OFF",
                            "-DWITH_HALIDE=OFF",
                            "-DWITH_JASPER=OFF",
                            "-DWITH_NVCUVID=OFF",
                            "-DWITH_OPENEXR=OFF",
                            "-DWITH_PROTOBUF=OFF",
                            "-DWITH_PTHREADS_PF=OFF",
                            "-DWITH_QUIRC=OFF",
                            "-DWITH_V4L=OFF",
                            "-DWITH_WEBP=OFF",
                            "-DBUILD_LIST=core,imgproc",
                            "-DBUILD_JAVA=OFF",
                            "-DBUILD_ANDROID_EXAMPLES=OFF",
                            "-DBUILD_ANDROID_PROJECTS=OFF",
                            "-DBUILD_SHARED_LIBS=ON"
                        )
                    )
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    productFlavors {
        fDroid {
            externalNativeBuild.cmake.arguments.addAll(
                listOf("-DWITH_BUILD_ID=OFF")
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(project(":core:common:base"))
}
