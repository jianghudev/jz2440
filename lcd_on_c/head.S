.text
.global _start
_start:
    ldr r0 , =0x53000000  @watchdog 寄存器address
    mov r1, #0x0
    str r1, [r0]  @写入0 ，禁止watchdog重启

   ldr sp, =1024*4  @设置栈指针
   bl main
halt_loop:
    b halt_loop
