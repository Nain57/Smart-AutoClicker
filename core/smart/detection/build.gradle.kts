@file:Suppress("UnstableApiUsage")
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidLocalTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.sourceDownload)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.detection"
    compileSdk = 35

    val openCvDir = File(project.projectDir, "src/release/opencv/OpenCV-android-sdk/sdk/native/jni")

    defaultConfig {
        minSdk = 21
        externalNativeBuild {
            cmake {
                // Point CMake to your local OpenCV package
                arguments += listOf(
                    "-DOpenCV_DIR=${openCvDir.absolutePath}")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(project(":core:common:base"))
}
