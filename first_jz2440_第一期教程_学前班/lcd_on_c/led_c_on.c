#define GPFCON  (*(volatile unsigned long *)0x56000050)
#define GPFDAT  (*(volatile unsigned long *)0x56000054)


int main(){

    GPFCON =0X100;
    GPFDAT =0X00;
    return 0;
}
