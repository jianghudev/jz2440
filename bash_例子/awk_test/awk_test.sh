#!/bin/bash

# 打印文件中的所有数据
# awk '/./' emp.dat

# 调整薪水 
# awk -f adjust.awk emp.dat


# 2维数组的用法
# awk -f 2_direction_array_test.awk  emp.dat

# 2维数组的第2中用法
gawk -f course_jh_2.awk register.dat
