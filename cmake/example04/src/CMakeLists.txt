


#include_directories(/usr/local/include/hello)


### 这中方式必须在shell中定义 export CMAKE_INCLUDE_PATH=/usr/local/include/hello 才能起作用###
find_path(myheader hello.h)
IF(myheader)
    include_directories(${myheader})
endif(myheader)

message(status " myheader= ${myheader} ")

link_directories(/usr/local/lib)

###exec_program(ls ARGS " ${CMAKE_SOURCE_DIR}" OUTPUT_VARIABLE ls_output  RETURN_VALUE ls_return)
exec_program(ls args " ${CMAKE_SOURCE_DIR}" OUTPUT_VARIABLE ls_output  RETURN_VALUE ls_return)
message(STATUS " ls_return: " ${ls_return})
message(STATUS " cmake_source_dir: " ${CMAKE_SOURCE_DIR})
message(STATUS " ls_output: " ${ls_output})

if(NOT ls_return)
    message(STATUS " ls result: " ${ls_output})
endif(NOT ls_return)

find_library(ret_lib X11 /usr/lib)
message(STATUS  "ret_lib=  ${ret_lib} ")
if(NOT ret_lib)
    message(ERROR " X11 not found!")
endif(NOT ret_lib)


set(CMAKE_ALLOW_LOOSE_LOOP_CONSTRUCTS ON)

message(status " home dir: $ENV{HOME}")

if(WIN32)
    message(status, "this is windows!")
else(WIN32)
    message(status, "this is not windows!")
endif(WIN32)

if(WIN32)
    message(status, "this is windows!")
elseif(UNIX)
    message(status, "this is unix!")
elseif(APPLE)
    message(status, "this is apple!")
endif(WIN32)


foreach(var RANGE 10)
    message("var=" ${var})
endforeach(var)


foreach(var2 RANGE 5 15)
    message( "var2 is =" ${var2})
endforeach(var2)


foreach(var3 RANGE 0 99 7)
    message("var3=" ${var3})
endforeach(var3)


add_executable(main main.c)

###请注意 必须把 add_executable 放在 target_link_libraries 之后，不然的话 连接的时候就会找不到 libhello.so库文件 ###

#target_link_libraries(main libhello.so)
target_link_libraries(main libhello.a)






