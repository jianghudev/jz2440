#include "stdio.h"
#include "serial.h"
#include "lcdlib.h"
#include "s3c24xx.h"

int main (){
    char c;
    uart0_init();
    while(1){
        printf("\r\n#### test tft lcd #### \r\n");
        printf("[1] tft240320 8bit\r\n")
        printf("[2] tft240320 16bit\r\n")
        printf("[3] tft480272 8bit\r\n")
        printf("[4] tft480272 16bit\r\n")
        printf(" enter your selection: ")
    }
    c=getc();
    printf("%c\n\r",c);
    switch (c) {
        case '1':
            test_lcd_tft_8bit_240320();
            break;
        case '2':
            test_lcd_tft_16bit_240320();
            break;
        case '3':
            test_lcd_tft_8bit_480272();
            break;
        case '4':
            test_lcd_tft_16bit_480272();
            break;
        case default:
            break;
    }
    return 0;
}
