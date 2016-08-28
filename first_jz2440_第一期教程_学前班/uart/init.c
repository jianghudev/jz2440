#include "s3c24xx.h"

void disable_watch_dog();
void clock_init();
void mem_setup();
void copy_steppingstone_to_sdram();

#define s3c2440_MPLL_200Mhz ((0x5c<<12) | (0x01<<4 )| (0x02))

void disable_watch_dog(){
    WTCON= 0;
}
void clock_init(){
    LOCKTIME = 0X00FFFFFF;
    CLKDIVN = 0X03;

__asm__(
    "mrc    p15, 0, r1, c1, c0, 0\n"        /* 读出控制寄存器 */
    "orr    r1, r1, #0xc0000000\n"          /* 设置为“asynchronous bus mode” */
    "mcr    p15, 0, r1, c1, c0, 0\n"        /* 写入控制寄存器 */
    );

    MPLLCON = s3c2440_MPLL_200Mhz;
}
void mem_setup(){
    volatile unsigned long *p = (volatile unsigned long*)MEM_CTL_BASE;

    /* 这个函数之所以这样赋值，而不是像前面的实验(比如mmu实验)那样将配置值
     * 写在数组中，是因为要生成”位置无关的代码”，使得这个函数可以在被复制到
     * SDRAM之前就可以在steppingstone中运行
     */
    /* 存储控制器13个寄存器的值 */
    p[0] = 0x22011110;     //BWSCON
    p[1] = 0x00000700;     //BANKCON0
    p[2] = 0x00000700;     //BANKCON1
    p[3] = 0x00000700;     //BANKCON2
    p[4] = 0x00000700;     //BANKCON3
    p[5] = 0x00000700;     //BANKCON4
    p[6] = 0x00000700;     //BANKCON5
    p[7] = 0x00018005;     //BANKCON6
    p[8] = 0x00018005;     //BANKCON7

                                            /* REFRESH,
                                             * HCLK=12MHz:  0x008C07A3,
                                             * HCLK=100MHz: 0x008C04F4
                                             */
    p[9]  = 0x008C04F4;
    p[10] = 0x000000B1;     //BANKSIZE
    p[11] = 0x00000030;     //MRSRB6
    p[12] = 0x00000030;     //MRSRB7



}
void copy_steppingstone_to_sdram(){
    unsigned int * psrc = (unsigned int *)0x0;
    unsigned int * pdst = (unsigned int *)0x30000000;

    while ( psrc < (unsigned int*)4096 ){
        *pdst = *psrc;
        pdst ++;
        psrc ++;
    }

}


