project(hello)

add_subdirectory(src bin)

install(FILES copyright readme DESTINATION share/doc/cmake/example02/)
install(PROGRAMS runhello.sh DESTINATION bin)
install(DIRECTORY doc/ DESTINATION share/doc/cmake/example02/)



####先进入到 build目录下面，然后执行如下的命令就可以了#######
######################################################
# cd build/
# cmake -DCMAKE_INSTALL_PREFIX=/tmp/jh_test3/usr/  ..
# make
# make install
#

