#include "vsprintf.h"
#include "printf.h"
#include "string.h"

extern void putc(unsigned char c);
extern unsigned char getc(void);

#define OUTPUTSIZE 1024
#define INPUTSIZE 1024

static unsigned char g_output_buf[OUTPUTSIZE];
static unsigned char g_input_buf[INPUTSIZE];


int print(const char*fmt, ... ){
    int i;
    int len;
    va_list args;

    va_start(args,fmt);
    len= vspriintf(g_output_buf,fmt,args);
    va_end(args);

    for (int i = 0; i < strlen(g_output_buf); ++i) {
        putc(g_output_buf[i]);
    }
    return len;
}

int scanf(const char* fmt,...){
    int i=0;
    unsigned char c;
    va_list args;
    while(1){
        c=getc();
        if( (c==0x0d) || (c==0x0a) ){
            g_input_buf[i]='\0';
            break;
        }else{
            g_input_buf[i++]=c;
        }
    }
    va_start(args,fmt);
    i= vsscanf(g_input_buf,fmt,args);
    va_end(args);

    return i;

}

