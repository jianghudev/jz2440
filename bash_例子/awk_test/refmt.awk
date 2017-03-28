BEGIN {print "id number arrival time" > "today_rpt"; print "======================" > "today_rpt"}
{printf("%s  %s\n" ,$1,$2) > "today_rpt"}
