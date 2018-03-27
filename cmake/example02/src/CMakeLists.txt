

add_executable(world main.c)


set(EXECUTABLE_OUTPUT_PATH ${PROJECT_BINARY_DIR}/jh_bin/)
set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/jh_lib/)


INSTALL(TARGETS world RUNTIME DESTINATION bin)   ####这个语句必须放在 add_executable之后才能编译通过

