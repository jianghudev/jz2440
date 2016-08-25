#define gpfcon (*(volatile unsigned long*)0xA0000050)
#define gpfdata (*(volatile unsigned long*)0xA0000054)


#define gpf4_out (1<<8)
#define gpf5_out (1<<10)
#define gpf6_out (1<<12)



static inline void wait(volatile unsigned long dly){
    for (;dly>0; dly--);
}
int main(){
    unsigned long i=0;
    gpfcon= gpf4_out|gpf5_out|gpf6_out;

    while(1){
        wait(30000);
        gpfdata = (~(i<<4));
        if(++i ==8){
            i=0;
        }
    }

    return 0;

}
