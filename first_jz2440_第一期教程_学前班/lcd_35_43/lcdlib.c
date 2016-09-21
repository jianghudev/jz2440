#include "stdio.h"
#include "lcddrv.h"
#include "framebuffer.h"

void test_lcd_tft_8bit_240320(void){
    lcd_port_init();
    tft_lcd_int(MODE_TFT_8BIT_240320);
    lcd_powerenable(0,1);
    lcd_envidonoff(1);

    lcd_palette8bit_init();
    clearSrc(0x0);
    printf("[tft 64k color(16bpp) lcd test ]\n");

    printf("1. press any key to draw line\n");
    getc();

    drawline(0,0,239,0,0);
    drawline(0,0,0,319,1);
    drawline(239,0,239,319,0);
    drawline(0,319,239,319,0);

    drawline(0,0,239,319,8);
    drawline(239,0,0,319,16);
    drawline(120,0,120,319,32);
    drawline(0,160,239,160,64);

    printf("2 press any key to draw circles\n");
    getc();
    mire();


    printf("3 press any key to fill the screen with one color\n");
    getc();
    clearSrc(128);

    printf("4 press any key to fill the screen by temporary palette \n");
    getc();
    clear_src_with_tmp_plt(0x0000ff);

    printf("5 press any key to fill the screen by palette \n");
    getc();
    disabletmpplt();
    changepalette(0xffff00);


    printf("6 press any key to stop the testing \n");
    getc();
    lcd_envidonoff(0);

}


/*
 * 以240x320,16bpp的显示模式测试TFT LCD
 */
void Test_Lcd_Tft_16Bit_240320(void)
{
    Lcd_Port_Init();                     // 设置LCD引脚
    Tft_Lcd_Init(MODE_TFT_16BIT_240320); // 初始化LCD控制器
    Lcd_PowerEnable(0, 1);               // 设置LCD_PWREN有效，它用于打开LCD的电源
    Lcd_EnvidOnOff(1);                   // 使能LCD控制器输出信号

    ClearScr(0x0);  // 清屏，黑色
    printf("[TFT 64K COLOR(16bpp) LCD TEST]\n");

    printf("1. Press any key to draw line\n");
    getc();
    DrawLine(0  , 0  , 239, 0  , 0xff0000);     // 红色
    DrawLine(0  , 0  , 0  , 319, 0x00ff00);     // 绿色
    DrawLine(239, 0  , 239, 319, 0x0000ff);     // 蓝色
    DrawLine(0  , 319, 239, 319, 0xffffff);     // 白色
    DrawLine(0  , 0  , 239, 319, 0xffff00);     // 黄色
    DrawLine(239, 0  , 0  , 319, 0x8000ff);     // 紫色
    DrawLine(120, 0  , 120, 319, 0xe6e8fa);     // 银色
    DrawLine(0  , 160, 239, 160, 0xcd7f32);     // 金色

    printf("2. Press any key to draw circles\n");
    getc();
    Mire();

    printf("3. Press any key to fill the screem with one color\n");
    getc();
    ClearScr(0xff0000);             // 红色

    printf("4. Press any key to fill the screem by temporary palette\n");
    getc();
    ClearScrWithTmpPlt(0x0000ff);   // 蓝色

    printf("5. Press any key stop the testing\n");
    getc();
    Lcd_EnvidOnOff(0);
}


/*
 * 以480x272,8bpp的显示模式测试TFT LCD
 */
void Test_Lcd_Tft_8Bit_480272(void)
{
    Lcd_Port_Init();                     // 设置LCD引脚
    Tft_Lcd_Init(MODE_TFT_8BIT_480272);  // 初始化LCD控制器
    Lcd_PowerEnable(0, 1);               // 设置LCD_PWREN有效，它用于打开LCD的电源
    Lcd_EnvidOnOff(1);                   // 使能LCD控制器输出信号

    Lcd_Palette8Bit_Init();     // 初始化调色板
    ClearScr(0x0);              // 清屏
    printf("[TFT 64K COLOR(16bpp) LCD TEST]\n");

    printf("1. Press any key to draw line\n");
    getc();
    DrawLine(0  , 0  , 479, 0  , 0);    // 颜色为DEMO256pal[0]
    DrawLine(0  , 0  , 0  , 271, 1);    // 颜色为DEMO256pal[1]
    DrawLine(479, 0  , 479, 271, 2);    // ……
    DrawLine(0  , 271, 479, 271, 4);
    DrawLine(0  , 0  , 479, 271, 8);
    DrawLine(479, 0  , 0  , 271, 16);
    DrawLine(240, 0  , 240, 271, 32);
    DrawLine(0  , 136, 479, 136, 64);

    printf("2. Press any key to draw circles\n");
    getc();
    Mire();

    printf("3. Press any key to fill the screem with one color\n");
    getc();
    ClearScr(128);  //  输出单色图像，颜色为DEMO256pal[128]

    printf("4. Press any key to fill the screem by temporary palette\n");
    getc();
    ClearScrWithTmpPlt(0x0000ff);       //  输出单色图像，颜色为蓝色

    printf("5. Press any key to fill the screem by palette\n");
    getc();
    DisableTmpPlt();            // 关闭临时调色板寄存器
    ChangePalette(0xffff00);    // 改变整个调色板为黄色，输出单色图像

    printf("6. Press any key stop the testing\n");
    getc();
    Lcd_EnvidOnOff(0);
}

/*
 * 以480x272,16bpp的显示模式测试TFT LCD
 */
void Test_Lcd_Tft_16Bit_480272(void)
{
    Lcd_Port_Init();                     // 设置LCD引脚
    Tft_Lcd_Init(MODE_TFT_16BIT_480272); // 初始化LCD控制器
    Lcd_PowerEnable(0, 1);               // 设置LCD_PWREN有效，它用于打开LCD的电源
    Lcd_EnvidOnOff(1);                   // 使能LCD控制器输出信号

    ClearScr(0x0);  // 清屏，黑色
    printf("[TFT 64K COLOR(16bpp) LCD TEST]\n");

    printf("1. Press any key to draw line\n");
    getc();
    DrawLine(0  , 0  , 479, 0  , 0xff0000);    // 红色
    DrawLine(0  , 0  , 0  , 271, 0x00ff00);    // 绿色
    DrawLine(479, 0  , 479, 271, 0x0000ff);    // 蓝色
    DrawLine(0  , 271, 479, 271, 0xffffff);    // 白色
    DrawLine(0  , 0  , 479, 271, 0xffff00);    // 黄色
    DrawLine(479, 0  , 0  , 271, 0x8000ff);    // 紫色
    DrawLine(240, 0  , 240, 271, 0xe6e8fa);    // 银色
    DrawLine(0  , 136, 479, 136, 0xcd7f32);    // 金色

    printf("2. Press any key to draw circles\n");
    getc();
    Mire();

    printf("3. Press any key to fill the screem with one color\n");
    getc();
    ClearScr(0xff0000);             // 红色

    printf("4. Press any key to fill the screem by temporary palette\n");
    getc();
    ClearScrWithTmpPlt(0x0000ff);   // 蓝色

    printf("5. Press any key stop the testing\n");
    getc();
    Lcd_EnvidOnOff(0);
}


/*
 * 以640x480,8bpp的显示模式测试TFT LCD
 */
void Test_Lcd_Tft_8Bit_640480(void)
{
    Lcd_Port_Init();                     // 设置LCD引脚
    Tft_Lcd_Init(MODE_TFT_8BIT_640480);  // 初始化LCD控制器
    Lcd_PowerEnable(0, 1);               // 设置LCD_PWREN有效，它用于打开LCD的电源
    Lcd_EnvidOnOff(1);                   // 使能LCD控制器输出信号

    Lcd_Palette8Bit_Init();     // 初始化调色板
    ClearScr(0x0);              // 清屏，黑色
    printf("[TFT 64K COLOR(16bpp) LCD TEST]\n");

    printf("1. Press any key to draw line\n");
    getc();
    DrawLine(0  , 0  , 639, 0  , 0);    // 颜色为DEMO256pal[0]
    DrawLine(0  , 0  , 0  , 479, 1);    // 颜色为DEMO256pal[1]
    DrawLine(639, 0  , 639, 479, 2);    // ……
    DrawLine(0  , 479, 639, 479, 4);
    DrawLine(0  , 0  , 639, 479, 8);
    DrawLine(639, 0  , 0  , 479, 16);
    DrawLine(320, 0  , 320, 479, 32);
    DrawLine(0  , 240, 639, 240, 64);

    printf("2. Press any key to draw circles\n");
    getc();
    Mire();

    printf("3. Press any key to fill the screem with one color\n");
    getc();
    ClearScr(128);  //  输出单色图像，颜色为DEMO256pal[128]

    printf("4. Press any key to fill the screem by temporary palette\n");
    getc();
    ClearScrWithTmpPlt(0x0000ff);       //  输出单色图像，颜色为蓝色

    printf("5. Press any key to fill the screem by palette\n");
    getc();
    DisableTmpPlt();            // 关闭临时调色板寄存器
    ChangePalette(0xffff00);    // 改变整个调色板为黄色，输出单色图像

    printf("6. Press any key stop the testing\n");
    getc();
    Lcd_EnvidOnOff(0);
}

/*
 * 以640x480,16bpp的显示模式测试TFT LCD
 */
void Test_Lcd_Tft_16Bit_640480(void)
{
    Lcd_Port_Init();                     // 设置LCD引脚
    Tft_Lcd_Init(MODE_TFT_16BIT_640480); // 初始化LCD控制器
    Lcd_PowerEnable(0, 1);               // 设置LCD_PWREN有效，它用于打开LCD的电源
    Lcd_EnvidOnOff(1);                   // 使能LCD控制器输出信号

    ClearScr(0x0);  // 清屏，黑色
    printf("[TFT 64K COLOR(16bpp) LCD TEST]\n");

    printf("1. Press any key to draw line\n");
    getc();
    DrawLine(0  , 0  , 639, 0  , 0xff0000);     // 红色
    DrawLine(0  , 0  , 0  , 479, 0x00ff00);     // 绿色
    DrawLine(639, 0  , 639, 479, 0x0000ff);     // 蓝色
    DrawLine(0  , 479, 639, 479, 0xffffff);     // 白色
    DrawLine(0  , 0  , 639, 479, 0xffff00);     // 黄色
    DrawLine(639, 0  , 0  , 479, 0x8000ff);     // 紫色
    DrawLine(320, 0  , 320, 479, 0xe6e8fa);     // 银色
    DrawLine(0  , 240, 639, 240, 0xcd7f32);     // 金色

    printf("2. Press any key to draw circles\n");
    getc();
    Mire();

    printf("3. Press any key to fill the screem with one color\n");
    getc();
    ClearScr(0xff0000);             // 红色

    printf("4. Press any key to fill the screem by temporary palette\n");
    getc();
    ClearScrWithTmpPlt(0x0000ff);   // 蓝色

    printf("5. Press any key stop the testing\n");
    getc();
    Lcd_EnvidOnOff(0);
}





