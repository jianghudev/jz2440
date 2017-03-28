BEGIN{
   #"date" |getline;  print"today is ", $2,$3 > "today_rpt2";
    date = system("date");
     print"today is ", date  > "today_rpt2";
   print"============================" > "today_rpt2";
   print "id number arriaval time"  > "today_rpt2";
   close("today_rpt2")
    }
    {printf("%s %s\n",$1,$2)  | "sort -k 1 >> today_rpt2"}
