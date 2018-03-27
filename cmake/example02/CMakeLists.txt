project(hello)

add_subdirectory(src bin)

install(FILES copyright readme DESTINATION share/doc/cmake/example02/)
install(PROGRAMS runhello.sh DESTINATION bin)
install(DIRECTORY doc/ DESTINATION share/doc/cmake/example02/)

