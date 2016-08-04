led_c_on.bin:led_c_on.c head.S
	arm-linux-gcc -g -c -o head.o head.S 
	arm-linux-gcc  -g -c -o led_c_on.o led_c_on.c
	arm-linux-ld -Ttext 0x0000000 -g head.o led_c_on.o -o led_c_elf
	arm-linux-objcopy -O binary -S  led_c_elf led_c_on.bin 
	arm-linux-objdump -D -m arm led_c_elf > led_c_on.dis
clean:
	rm -rf *.o led_c_on.bin led_c_on.dis led_c_elf 
