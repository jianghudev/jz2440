obj = head.o init.o serial.o main.o

uart.bin:$(obj) serial.h s3c24xx.h
	arm-linux-ld -Tuart.lds $(obj) -o uart_elf
	arm-linux-objcopy -O binary -S uart_elf $@
	arm-linux-objdump -D -m arm uart_elf > uart.dis

%.o:%.c
	arm-linux-gcc -c -o $@ $^
%.o:%.S
	arm-linux-gcc -c -o $@ $^


.PHONY = clean
clean:
	-rm -rf *.o uart_elf uart.bin uart.dis
