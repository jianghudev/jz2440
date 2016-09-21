#include "s3c24xx.h"
#include "serial.h"

#define TXDOREADY (1<<2)
#define RXDOREADY (1)


#define  PCLK  50000000
#define UART_CLK  PCLK
#define UART_BAUD_RATE  115200
#define UART_BRD   ((UART_CLK/(UART_BAUD_RATE*16))-1)


void uart0_init(void){
    GPHCON |= 0XA0;
    GPHUP = 0X0C;

    ULCON0 =0X03;
    UCON0 =0X05;
    UFCON0=0X00;
    UMCON0= 0X00;
    UBRDIV0 = UART_BRD;
}


void putc(unsigned char c){
    while(! (UTRSTAT0& TXDOREADY));
    UTXH0 =c;
}

unsigned char getc(void){
    while(! (UTRSTAT0 & RXDOREADY));
    return URXH0;
}

int isDigit(unsigned char c){
    if( c >= '0' && c<= '9' ){
        return 1;
    }else{
        return 0;
    }
}

int isLetter(unsigned char c){
    if( c >= 'a' && c<= 'z' ){
        return 1;
    }else if( c >= 'A' && c <= 'Z')
        return 1;
    }else{
        return 0;
    }
}


