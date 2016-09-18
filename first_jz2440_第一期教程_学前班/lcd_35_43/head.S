.extern main
.text
.global _start
_start:
    b reset
handleundef:
    b handleundef
handleswi:
    b handleswi
handleprefetchabort:
    b handleprefetchabort
handledataabort:
    b handledataabort
handlenotused:
    b handlenotused
@ 0x18  中断向量地址
    b handleirq
handlefiq:
    b handlefiq

reset:
    ldr sp, =4096
    bl disable_watch_dog
    bl clock_init
    bl mem_setup
    bl nand_init

    ldr r0, =0x30000000
    mov r1, #4096
    mov r2, #16*1024
    bl copycode2sdram

    bl clean_bss

    msr cpsr_c, #0xd2
    ldr sp, =0x31000000

    ldr lr, =ret_initirq
    ldr pc, =init_irq
ret_initirq:
    msr cpsr_c, #0x5f

    ldr lr, =halt_loop
    ldr pc, =main
halt_loop:
    b halt_loop

handleirq:
    sub lr, lr, #4
    stmdb sp!, {r0-r12,lr}

    ldr lr,  =int_return
    ldr pc, =irq_handle
int_return:
    ldmia sp!,  {r0-r12,pc}^

