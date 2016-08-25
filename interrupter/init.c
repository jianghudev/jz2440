#include "s3c24xx.h"

#define gpf4_out  (1<<8)
#define gpf5_out  (1<<10)
#define gpf6_out  (1<<12)


#define gpf4_mask  (3<<8)
#define gpf5_mask  (3<<10)
#define gpf6_mask  (3<<12)



#define gpf0_int  (0x2<<0)
#define gpf2_int   (0x2<<4)
#define gpg3_int  (0x2<<6)


#define gpf0_mask  (3<<0)
#define gpf2_mask  (3<<4)
#define gpg3_mask  (3<<6)


void disable_watch_dog(){  WTCON=0; }


void init_led() {
    GPFCON &= ~(gpf4_mask |gpf5_mask |gpf6_mask );

    GPFCON  |=gpf4_out | gpf5_out |gpf6_out ;
}

void init_irq(){


    GPFCON  &= ~(gpf0_mask | gpf2_mask);
    GPFCON |=  gpf0_int | gpf2_int;

    GPGCON &= ~gpg3_mask;
    GPGCON |= gpg3_int;


    EINTMASK &= ~(1<<11);

    PRIORITY = ( PRIORITY & ( (~0X01)|(0X03<<7 ))) | (0x0<<7);
    INTMSK &= (~(1<<0)) & (~(1<<2)) & (~(1<<5));
}

