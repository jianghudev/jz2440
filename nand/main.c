#define gpfcon (*(volatile unsigned long *)0x56000050)
#define gpfdat (*(volatile unsigned long *)0x56000054)
#include "stdio.h"

#define  gpf4_out    (1<<8)
#define  gpf5_out    (1<<10)
#define  gpf6_out    (1<<12)


void wait (volatile unsigned long dly){
    unsigned long i=0;
    for (i = 0; i < dly; ++i) {

    }

}

int main(void){
    unsigned long i =0;

    gpfcon = gpf4_out | gpf5_out| gpf6_out;
    //fprintf(stderr,"hello!\n");
    while(1){
        wait(30000);
        gpfdat = (~(i<<4));
        if(++i ==8){
            i=0;
        }
    }
    return 0;
}
