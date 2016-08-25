#define wtcon  (*(volatile unsigned long*)0x53000000)
#define  mem_ctl_base    0x48000000

void disable_watch_dog();
void memsetup();



void disable_watch_dog(){
    wtcon = 0;

}

void memsetup(){
    int i=0;
    unsigned long *p = (unsigned long*)mem_ctl_base;

    unsigned long const mem_cfg_val []={
        0x22011110,
        0x00000700,
        0x00000700,
        0x00000700,
        0x00000700,
        0x00000700,
        0x00000700,
        0x00018005,
        0x00018005,
        0x008c07a3,
        0x000000b1,
        0x00000030,
        0x00000030,
    };
    for (i = 0; i < 13; i++) {
        p[i]= mem_cfg_val[i];
    }
}
