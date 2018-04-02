find_path(hello_include_dir hello.h /usr/include/hello /usr/local/include/hello/)
find_library(hello_library NAMES hello PATH /usr/lib/ /usr/local/lib/)


if(hello_include_dir AND hello_library)
    set(hello_found TRUE)
endif(hello_include_dir AND hello_library)

if(hello_found)
    if(NOT HELLO_FIND_QUIETLY)
        message(status,"found hello: ${hello_library} ")
    endif(NOT HELLO_FIND_QUIETLY)
else(hello_found)
    if(HELLO_FIND_REQUIRED)
        message(status,"could not find hello library: ${hello_library} ")
    endif(HELLO_FIND_REQUIRED)
endif(hello_found)

