
.equ  MEM_CTL_BASE,   0X48000000
.equ  SDRAM_BASE,     0X30000000

.text
.global  _start

_start:
    bl disable_watch_dog  @关闭watchdog
    bl memsetup
    bl copy_steppingstone_to_sdram
    ldr  pc,  =on_sdram
on_sdram:
    ldr  sp, =0x34000000
    bl main
halt_loop:
    b halt_loop

disable_watch_dog:
    mov r1,  #0x53000000
    mov r2,  #0x0
    str r2, [r1]
    mov pc, lr 


copy_steppingstone_to_sdram:
    mov r1, #0
    ldr r2, =SDRAM_BASE
    mov r3, #4*1024
3:
    ldr r4, [r1],#4 @r1对应的地址0中的内容放到r4中去  r1=r1+4
    str r4, [r2],#4 @r4中的内容放到 以r2为地址的 存储单元中 ， 就是把0地址中的数据，copy到 0x30000000中去， r2=r2+4
    cmp r1, r3
    bne 3b
    mov pc, lr


memsetup:
    mov r1,  #MEM_CTL_BASE
    adrl r2, mem_cfg_val
    add r3,  r1,#52
3:
    ldr r4, [r2],#4
    str r4, [r1],#4
    cmp r1,r3
    bne 3b
    mov pc, lr

.align 4
mem_cfg_val:
    .long 0x22011110
    .long 0x00000700
    .long 0x00000700
    .long 0x00000700
    .long 0x00000700
    .long 0x00000700
    .long 0x00000700
    .long 0x00018005
    .long 0x00018005
    .long 0x008c07a3
    .long 0x000000b1
    .long 0x00000030
    .long 0x00000030

