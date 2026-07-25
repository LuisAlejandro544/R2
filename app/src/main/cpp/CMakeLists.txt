cmake_minimum_required(VERSION 3.22.1)

project("oboe_audio_engine")

add_library(
    oboe_audio_engine
    SHARED
    oboe_audio_engine.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    oboe_audio_engine
    ${log-lib}
)
