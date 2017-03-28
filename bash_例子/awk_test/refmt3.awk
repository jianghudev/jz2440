awk '
BEGIN{
    FS = "[\t :]+";
    "date" | getline ;
    print "today is ",$2,$3 > "today_rpt3"
    print "=================" >  "today_rpt3"
    print "id number arrival time"  >  "today_rpt3"
    close( "today_rpt3");
}
{
    arrival = HM_2_M($2,$3);
    printf("%s %s:%s  %s\n", $1,$2,$3,arrival>480?"*":" "  ) | "sort -k 1 >> today_rpt3 "
    total += arrival
}
END{
    close ( "today_rpt3")
    close ( "sort -k 1 >> today_rpt3 ");
    printf("avag arrival time: %d:%d \n",total/NR/60, (total/NR)%60 ) >>  "today_rpt3"
}
function HM_2_M(hour,min){
    return hour*60+min;
}

' $*
