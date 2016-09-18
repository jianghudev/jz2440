#ifdef __FRAME_BUFFER_H__
#define __FRAME_BUFFER_H__
#include "types.h"

void putpixel(UINT32 x, UINT32 y, UINT32 color);
void drawline(int x1, int y1,int x2, int y2 ,int color);
void mire(void);
void clearSrc(UINT32 color);


#endif
