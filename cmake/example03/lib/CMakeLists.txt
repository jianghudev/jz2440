add_library(hello_static STATIC hello.c)
add_library(hello SHARED hello.c)

### OUTPUT_NAME 是关键字， 它是 hello_static的1个属性###
set_target_properties(hello_static PROPERTIES OUTPUT_NAME "hello") ### hello必须是双引号，不能是单引号###

get_target_property(my_value  hello_static OUTPUT_NAME)
message(status " this is the hello_static output_name: " ${my_value})



set_target_properties(hello PROPERTIES CLEAN_DIRECT_OUTPUT 1)
set_target_properties(hello_static PROPERTIES LCEAN_DIRECT_OUTPUT 1)


set_target_properties(hello PROPERTIES VERSION 1.2 SOVERSION 1)

install(FILES hello.h DESTINATION include)

install(TARGETS hello hello_static LIBRARY DESTINATION lib ARCHIVE DESTINATION lib)


