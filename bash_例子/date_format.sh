#!/bin/bash

string=`date +%Y%m%d`

ls /usr/local/ > log_${string}.log
