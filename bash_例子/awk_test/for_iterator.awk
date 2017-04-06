awk '
BEGIN{
    x[1]=10; x[2]=20; x["last"]=30; 
    for(any in x){
        printf("x[%s]=%d \n",any,x[any]);
    }
}

' $*
