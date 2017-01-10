{for(i=2;i<=NF; i++) number[$i]++  }
END{ for(course in number) printf("%s %d \n",course,number[course]) }
