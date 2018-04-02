set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake )
message(status, "cmake_module_path =" ${CMAKE_MODULE_PATH} )



find_package(HELLO REQUIRED)

if(hello_found)
    add_executable(hello main.c)
    include_directories(${hello_include_dir} )
    target_link_libraries(hello ${hello_library} )
else(hello_found)
    message(status,"hello_found failed! ")
endif(hello_found)
