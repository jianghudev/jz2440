


include_directories(/usr/local/include/hello)

link_directories(/usr/local/lib)



add_executable(main main.c)

###请注意 必须把 add_executable 放在 target_link_libraries 之后，不然的话 连接的时候就会找不到 libhello.so库文件 ###

#target_link_libraries(main libhello.so)
target_link_libraries(main libhello.a)
