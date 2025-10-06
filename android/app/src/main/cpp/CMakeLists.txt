cmake_minimum_required(VERSION 3.22.1)

project("edgeviewer")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenCV configuration
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/opencv/sdk/native/jni")
find_package(OpenCV REQUIRED)

# Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# Source files
set(SOURCES
    native-lib.cpp
    ImageProcessor.cpp
)

# Create native library
add_library(
    ${CMAKE_PROJECT_NAME}
    SHARED
    ${SOURCES}
)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Link libraries
target_link_libraries(
    ${CMAKE_PROJECT_NAME}
    ${OpenCV_LIBS}
    ${log-lib}
    ${android-lib}
)
