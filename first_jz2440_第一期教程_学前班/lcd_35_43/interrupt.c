#include "s3c24xx.h"

void (*isr_handle_array[50])(void);

void dummy_isr(void){
    while(1);
}


void init_irq(void){
    int i=0;
    for ( i = 0; i < sizeof(isr_handle_array)/sizeof(isr_handle_array[0]); ++i)
    {
        isr_handle_array[i]= dummy_isr;
    }
    INTMOD =0X0;
    INTMSK = BIT_ALLMSK;
}

void irq_handle(void){
    unsigned long  oft = INTOFFSET;
    if(oft == 4){
        EINTPEND = 1<<7;
    }
    SRCPND = 1<<oft ;
    INTPND = INTPND;

    isr_handle_array[oft]();

}
