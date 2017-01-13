BEGIN{
    dis_count=0; 
    os_count=0; 
    ai_count=0; 
    ds_count=0; 
    graphics_count=0; 
    algorithm_count=0; 
    arch_count=0; 
}
{
    for(i=2;i<=NF; i++){
        if($i == "discrete"){
            discrete_array[$1]=  ++ dis_count ;
        }else if($i == "os"){
            os_array[$1]=  ++ os_count ;
        }else if($i == "ai"){
            ai_array[$1]=  ++ ai_count ;
        }else if($i == "ds"){
            ds_array[$1]=  ++ ds_count ;
        }else if($i == "graphics"){
            graphics_array[$1]=  ++ graphics_count ;
        }else if($i == "algorithm"){
            algorithm_array[$1]=  ++ algorithm_count ;
        }else if($i == "arch"){
            arch_array[$1]=  ++ arch_count ;
        }
    }
}


END{ 
    for(name in discrete_array) {printf("discrete_array[%s]= %d \n",name,discrete_array[name]);} 
    for(name in os_array){ printf("os_array[%s]= %d \n",name,os_array[name]); }
    for(name in ai_array){ printf("ai_array[%s]= %d \n",name,ai_array[name]); }
    for(name in ds_array){ printf("ds_array[%s]= %d \n",name,ds_array[name]); }
    for(name in graphics_array){ printf("graphics_array[%s]= %d \n",name,graphics_array[name]); }
    for(name in algorithm_array){ printf("algorithm_array[%s]= %d \n",name,algorithm_array[name]); }
    for(name in arch_array){ printf("arch_array[%s]= %d \n",name,arch_array[name]); }
        
        }
