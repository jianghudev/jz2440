#include "s3c24xx.h"
#include "serial.h"


#define TXD0READY (1<<2)
#define RXD0READY (1)


#define PCLK  50000000
#define UART_CLK  PCLK
#define uart_baud_rate   115200
#define uart_brd      ( (UART_CLK/(uart_baud_rate*16) )  -1    )


void uart0_init(){
    GPHCON  |= 0XA0;
    GPHUP =0X0C;
    ULCON0  =0X03;
    UCON0 =0X05;
    UFCON0=0X00;
    UMCON0 =0X00;
    UBRDIV0 =uart_brd;

}

void putc(unsigned char c){
    while(!(UTRSTAT0 & TXD0READY) );
    UTXH0 =  c ;
}

unsigned char getc(void){
    while(!(UTRSTAT0 & RXD0READY) );
    return URXH0  ;
}

int isDigit(unsigned char c){
    if(c>= '0' && c<= '9')
        return 1;
    else
        return 0;
}

int isLetter(unsigned char c){
    if(c>= 'a' && c<= 'z')
        return 1;
    if(c>= 'A' && c<= 'Z')
        return 1;
    else
        return 0;
}


