awk '
BEGIN{
    do{
        print "enter y or n"
        getline data;
        }while(data !~ /^[ynYN]$/)
}

' $*
