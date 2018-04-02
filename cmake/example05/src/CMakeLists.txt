
#########方法1###############
#include_directories(/home/jh/software/cmake-3.2.2/Utilities/cmcurl/include/)
#include_directories(/home/jh/software/cmake-3.2.2/Utilities/)
#link_directories(/usr/lib/x86_64-linux-gnu)

        #link_directories(/home/jh/htc_work/htc_miac/out/target/product/htc_miac/obj/lib/)
#add_executable(curltest main.c)
#target_link_libraries(curltest  libcurl.so)


######## 方法1 end ############


find_package(CURL)
if(CURL_FOUND)
    include_directories( ${CURL_INCLUDE_DIR} )
    add_executable(curltest main.c)
    target_link_libraries(curltest ${CURL_LIBRARY} )
else(CURL_FOUND)
    message(status, "curl library not found! ")
endif(CURL_FOUND)






