cmake_minimum_required(VERSION 3.22.1)

project("androidcamera")

#声明头文件路径
set(INCLUDE_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/include)
#声明库文件路径
set(LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/lib)
#libYuv
set(YUV_LIB yuv)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -L${LIB_DIR}/${ANDROID_ABI} -std=gnu++11")

#设置头文件
include_directories(${INCLUDE_DIR})

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
        YuvUtil.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        #链接yuv库
        ${YUV_LIB}
        android
        log)