#define GPFCON (*(volatile unsigned long*)0X56000050)
#define GPFDAT (*(volatile unsigned long*)0X56000054)


#define gpf4_out (1<<8)
#define gpf5_out (1<<10)
#define gpf6_out (1<<12)


void wait(volatile unsigned long dly){
    for(;dly>0; dly--);
}

int main(void){
    unsigned long i=0;
    GPFCON = gpf4_out | gpf5_out | gpf6_out;
    while(1){
        wait(30000);
        GPFDAT = (~(i<<4));
        if(++i ==8){
            i=0;
        }
    }
    return 0;
}
