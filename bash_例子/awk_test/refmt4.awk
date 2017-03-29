awk '
BEGIN {
Sys_Sort = "sort -k 1 >> today_rpt4"
Result = "today_rpt4"
# 改变字段切割的方式
# 令 Shell执行"date"; getline 读取结果，并以$0记录
FS = "[ \t:]+"
"date"|getline
print " Today is " ,$2,$3  > Result
print "=========================" > Result
print " ID Number Arrival Time"   > Result
close( Result )
# 从文件按中读取迟到数据, 并用数组cnt[ ]记录. 数组cnt[ ]中以
# 员工代号为下标, 所对应的值为该员工的迟到次数.
late_file = "late.dat"$2
while( getline < late_file >0 ) 
    cnt[$1] = $2
close( late_file )
}
{
# 已更改字段切割方式, $2表小时数,$3表分钟数
    arrival = HM_to_M($2, $3)
    if( arrival > 480 ){
    mark = "*" # 若当天迟到,应再增加其迟到次数, 且令mark 为"*".
    cnt[$1]++  
}
else mark = " " 
# message 用以显示该员工的迟到累计数, 若未曾迟到message为空字符串
message = cnt[$1] ? cnt[$1] " times" : ""
printf("%s %2d:%2d %5s %s\n", $1, $2, $3, mark, message ) | Sys_Sort
total += arrival
}
END {
close( Result )
close( Sys_Sort )
printf(" Average arrival time : %d:%d\n", total/NR/60, (total/NR)%60 ) >> Result
#将数组cnt[ ]中新的迟到数据写回文件中
for( any in cnt )
    print any, cnt[any] > late_file
}
function HM_to_M( hour, min ){
    return hour*60 + min
}
' $*

