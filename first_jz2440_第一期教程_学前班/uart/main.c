#include "serial.h"

int main(){
    unsigned char c=0xff;
    uart0_init();

    while(1){
        c =getc();
        if(isDigit(c) || isLetter(c) ){
            putc(c+1);
        }
    }
    return 0;

}
