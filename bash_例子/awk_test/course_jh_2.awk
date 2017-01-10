BEGIN{count=0;}



{for(i=2;i<=NF; i++)i{
    if($i == discrete){
        discrete_count++;
        } 
        number[$i,discrete_count]=$1;
    }   }
END{ for(course in number) printf("%s %d \n",course,number[course]) }
