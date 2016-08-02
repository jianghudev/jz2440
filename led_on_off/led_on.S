.text
.global _start
_start:
    LDR R0,=0X56000050  @R0为 GPFCON寄存器

    MOV R1,#0x00000100
    STR R1,[R0]         @设置gpf4为输出引脚， 位[8,7]=0b01

    LDR R0,=0X56000054  @R0为 GPFDATA寄存器
    MOV R1,#0x00000000
    STR R1,[R0]         @GPF4输出0，LED1点亮
MAIN_LOOP:
    B   MAIN_LOOP
