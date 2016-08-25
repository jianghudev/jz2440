#define GPFCON (*(volatile unsigned long*)0x56000050)
#define GPFDAT (*(volatile unsigned long*)0x56000054)

#define GPF4_OUT (1<<8)
#define GPF5_OUT (1<<10)
#define GPF6_OUT (1<<12)

void wait(volatile unsigned long dly){
    int i=0;
    for ( i = 0; i < dly; i++) {

    }
}
int main(){
    unsigned long i=0;
    GPFCON = GPF4_OUT|GPF5_OUT |GPF6_OUT;
    while(1){
        GPFDAT =  (~(1<<i));
        if(i++ > 6){
            i=4;
        }
        wait(90000);
    }

}
