BEGIN{
    arr["0","0"]= 1;
    arr["0","1"]= 2;
    arr["1","0"]= 3;
    arr["1","1"]= 4;
    for(element in arr){
        split(element,a,SUBSEP);
        printf("arr[%s,%s]=%s \n",a[1],a[2],arr[a[1],a[2]]);
        }

    }
