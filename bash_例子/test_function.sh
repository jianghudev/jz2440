#!/bin/bash

function fun1 {
    echo "this is a example! "
}


count=1
while [ $count -le 5 ]
do
    fun1
    echo "count=$count"
    count=$[ count +1 ]
done
