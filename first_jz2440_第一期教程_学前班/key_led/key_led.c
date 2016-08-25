#define GPFCON (*(volatile unsigned long*)0X56000050)
#define GPFDAT (*(volatile unsigned long*)0X56000054)

#define GPGCON (*(volatile unsigned long*)0X56000060)
#define GPGDAT (*(volatile unsigned long*)0X56000064)


//// led1 led2 led3 对应 gpf4 gpf5 gpf6
#define GPF4_OUT    (0X01 << 8)
#define GPF5_OUT    (0X01 << 10)
#define GPF6_OUT    (0X01 << 12)


#define GPF4_MASK    (0X3 << 8)
#define GPF5_MASK    (0X3 << 10)
#define GPF6_MASK    (0X3 << 12)


//// s2 s3 s4开关对应gpf0 gpf2 gpg3
#define GPF0_IN  (0x0 << 0)
#define GPF2_IN  (0x0 << 4)
#define GPG3_IN  (0x0 << 6)


#define GPF0_IN_MASK  (0x3 << 0)
#define GPF2_IN_MASK  (0x3 << 4)
#define GPG3_IN_MASK  (0x3 << 6)

int main(){

    unsigned long tData=0;
    GPFCON &= ~(GPF4_MASK| GPF5_MASK | GPF6_MASK);
    GPFCON |= GPF4_OUT | GPF5_OUT |GPF6_OUT ;


    GPFCON &= ~(GPF0_IN_MASK | GPF2_IN_MASK );
    GPFCON |= GPF0_IN | GPF2_IN  ;

    GPGCON &= ~(GPG3_IN_MASK  );
    GPGCON |= GPG3_IN ;

    while(1){
        tData=GPFDAT;
        if( tData & (1<<0)){
            GPFDAT |= (1<<4);
        }else{
            GPFDAT &= ~(1<<4);
        }
        if( tData & (1<<2)){
            GPFDAT |= (1<<5);
        }else{
            GPFDAT &= ~(1<<5);
        }

        tData=GPGDAT;
        if( tData & (1<<3)){
            GPFDAT |= (1<<6);
        }else{
            GPFDAT &= ~(1<<6);
        }

    }
    return 0;
}
