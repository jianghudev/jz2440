#ifndef HAVE_PLD
.macro pld reg
.endm
#endif



.text
.global _start
_start :
    ldr sp,   =4096
    bl disable_watch_dog
    bl memsetup
    bl copy_2th_to_sdram
    bl create_page_table
    bl mmu_init
    ldr sp, =0xb4000000
    ldr pc, =0xb0004000
halt_loop:
    b halt_loop
