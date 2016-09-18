#include "s3c24xx.h"

void disable_watch_dog(void);
void clock_init(void);
void mem_setup(void);
void copy_steppingstone_to_sdram(void);
void clean_bss(void);




void disable_watch_dog(void){
    WTCON = 0;
}

#define FCLK  200000000
#define HCLK  100000000
#define PCLK  50000000
#define s3c2410_mpll_200mhz   ((0x5c<<12)|(0x04<<4)|(0x00))
#define s3c2440_mpll_200mhz   ((0x5c<<12)|(0x01<<4)|(0x02))
void clock_init(void){
    CLKDIVN = 0X03;

    __asm__(
            "mrc p15, 0, r1,c1,c0,0\n"
            "orr r1, r1, #0xc0000000\n"
            "mcr p15,0,r1, c1,c0,0\n"
            );

    if((GSTATUS1 == 0x32410000) || (GSTATUS1 == 0X32410002) ){
        MPLLCON =  s3c2410_mpll_200mhz;
    }else{
        MPLLCON =  s3c2440_mpll_200mhz;
    }
}
void mem_setup(void){
    volatile unsigned long *p = (volatile unsigned long *)MEM_CTL_BASE;
    p[0]= 0x22011110;
    p[1]= 0x00000700;
    p[2]= 0x00000700;
    p[3]= 0x00000700;
    p[4]= 0x00000700;
    p[5]= 0x00000700;
    p[6]= 0x00000700;

    p[7]= 0x00018005;
    p[8]= 0x00018005;

    p[9]= 0x008c04f4;
    p[10]= 0x000000b1;
    p[11]= 0x00000030;
    p[12]= 0x00000030;
}
void copy_steppingstone_to_sdram(void){
    unsigned int * psrc =(unsigned int *)0;
    unsigned int * pdest =(unsigned int *)0x30000000;
    while(psrc < (unsigned int *)4096){
        *pdest = *psrc;
        pdest ++;
        psrc ++;
    }
}
void clean_bss(void){
    extern int __bss_start, __bss_end;
    int *p = &__bss_start;
    for ( ; p < &__bss_end; ++p)
    {
        *p =0;
    }
}


int copycode2sdram(void){unsigned char*buf, unsigned long start_addr,int size}{
    extern void nand_read(unsigned char*buf,unsigned long start_addr,int size);
    nand_read(buf,start_arrd,size);
    return 0;
}










