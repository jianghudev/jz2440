#include "framebuffer.h"
extern unsigned int fb_base_addr;
extern unsigned int bpp;
extern unsigned int xsize;
extern unsigned int ysize;


void putpixel(UINT32 x,UINT32 y ,UINT32 color){
    UINT8 red ,green,blue;
    switch (bpp) {
        case 16:
            {
                UINT16 *addr =(UINT16*) fb_base_addr + (y*xsize +x);
                red =(color>>19)& 0x1f;
                green =(color>>10)& 0x3f;
                blue =(color>>3)& 0x1f;
                color = (red <<11)| (green<<5)| blue;
                *addr =(UINT16)color;
                break;
            }
        case 8:
            {
                UINT8 *addr =(UINT8*) fb_base_addr + (y*xsize +x);
                *addr =(UINT8)color;
                break;
            }
        default:
            break;
    }
}

#define MOVE_AND_DRAW_LINE \
            if(dx>=dy){ \
                e=dy-dx/2; \
                while(x1<=x2){ \
                    putpixel(x1,y1,color); \
                    if(e>0){ \
                        y1+=1; \
                        e-=dx; \
                    } \
                    x1+=1; \
                    e+=dy; \
                } \
            }else{ \
                e=dx-dy/2; \
                while(y1<=y2){ \
                    putpixel(x1,y1,color); \
                    if(e>0){ \
                        x1+=1; \
                        e-=dy; \
                    } \
                    y1+=1; \
                    e+=dx; \
            }


void drawline(int x1,int y1,int x2, int y2,int color){
    int dx,dy, e;
    dx=x2-x1;
    dy=y2-y1;
    if(dx>=0){
        if(dy>=0){
            MOVE_AND_DRAW_LINE
        }else{
            dy=-dy;
            MOVE_AND_DRAW_LINE
        }

    }else{
        dx=-dx;
        if(dy>=0){
            MOVE_AND_DRAW_LINE
        }else{
            dy=-dy;
            MOVE_AND_DRAW_LINE
        }
    }
}

void mire(void){
    UINT32 x,y;
    UINT32 color;
    UINT8 red,green,blue,alpha;
    for (int y = 0; y < ysize; ++y)
    {
        for ( x = 0; x < xsize; ++x)
        {
            color = (x-xize/2)*(x-xsize/2) + (y-ysize/2)*(y-ysize/2)/64;
            red = (color/8) % 256;
            green = (color/4) % 256;
            blue = (color/2) % 256;
            alpha = (color/2) % 256;

            color |= ((UINT32) alpha << 24);
            color |= ((UINT32) red << 16);
            color |= ((UINT32) green << 8);
            color |= ((UINT32) blue << 0);

            putpixel(x,y,color);
        }
    }
}

void clearSrc(UINT32 color){
    UINT32 x,y;
    for ( y = 0; y < ysize; ++y)
    {
        for (x = 0; x < xsize; x++) {
            putpixel(x,y,color);
        }
    }

}























