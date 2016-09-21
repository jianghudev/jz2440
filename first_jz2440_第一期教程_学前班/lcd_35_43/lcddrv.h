#ifndef   __lcddrv_h__
#define __lcddrv_h__
#include  "types.h"

#define  LOWER21BITS(n)   ((n)& 0x1fffff)

#define  BPPMODE_1BPP   0X8
#define  BPPMODE_2BPP   0X9
#define  BPPMODE_4BPP   0Xa
#define  BPPMODE_8BPP   0Xb
#define  BPPMODE_16BPP   0Xc
#define  BPPMODE_24BPP   0Xd


#define  LCDTYPE_TFT    0X3

#define ENVID_DISABLE 0
#define ENVID_ENABLE  1

#define  FORMAT8BPP_5551  0
#define  FORMAT8BPP_565  1

#define  HSYNC_NORM  0
#define  HSYNC_INV  1

#define  VSYNC_NORM  0
#define  VSYNC_INV  1


#define VDEN_NORM  0
#define VDEN_INV  1

#define   BSWP  1
#define   HWSWP  1

#define  MODE_TFT_1BIT_240320    (0x4101)
#define  MODE_TFT_8BIT_240320    (0x4102)
#define  MODE_TFT_16BIT_240320    (0x4104)
#define  MODE_TFT_24BIT_240320    (0x4108)


#define  MODE_TFT_8BIT_480272   (0X410C)
#define  MODE_TFT_16BIT_480272   (0X4110)

#define  LCD_XSIZE_TFT_240320   (240)
#define  LCD_YSIZE_TFT_240320   (320)


#define  HOZVAL_TFT_240320   (LCD_XSIZE_TFT_240320-1)
#define  LINEVAL_TFT_240320   (LCD_YSIZE_TFT_240320-1)

#define   CLKVAL_TFT_240320  (4)

#define  VBPD_240320  ((6-1)&0XFF)
#define  VFPD_240320  ((4-1)&0XFF)
#define  VSPW_240320  ((2-1)&0X3F)
#define  HBPD_240320  ((11-1)&0X7F)
#define  HFPD_240320  ((2-1)&0XFF)
#define  HSPW_240320  ((1-1)&0XFF)


#define  MODE_TFT_1BIT_640480   (0X4201)
#define  MODE_TFT_8BIT_640480   (0X4202)
#define  MODE_TFT_16BIT_640480   (0X4204)
#define  MODE_TFT_24BIT_640480   (0X4208)

#define LCD_XSIZE_TFT_640480   (640)
#define LCD_YSIZE_TFT_640480   (480)

#define HOZVAL_TFT_640480  (LCD_XSIZE_TFT_640480-1)
#define LINEVAL_TFT_640480  (LCD_YSIZE_TFT_640480-1)

#define  VBPD_640480  ((33-1)&0XFF)
#define  VFPD_640480  ((10-1)&0XFF)
#define  VSPW_640480  ((2-1)&0X3F)
#define  HBPD_640480  ((48-1)&0X7F)
#define  HFPD_640480  ((16-1)&0XFF)
#define  HSPW_640480  ((96-1)&0XFF)

#define  CLKVAL_TFT_640480  (1)

#define LCDFRAMEBUFFER  0X30400000

void lcd_port_init(void);
void tft_lcd_init(int type);
void lcd_palette8bit_init(void);
void lcd_envid_onoff(int onoff);
void lcd_power_enable(int invpwren, int pwren);
void clear_src_with_tmp_plt(UINT32 color);
void disable_tmp_plt(void);
void change_palette(UINT32 color);



#endif
















































