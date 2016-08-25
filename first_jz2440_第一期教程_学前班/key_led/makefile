key_led.bin:head.S key_led.c
	arm-linux-gcc -g -c -o head.o head.S
	arm-linux-gcc -g -c -o key_led.o key_led.c
	arm-linux-ld -Ttext 0x00 -g head.o key_led.o -o key_led_elf
	arm-linux-objcopy -O binary -S key_led_elf key_led.bin
	arm-linux-objdump -D -m arm key_led_elf > key_led.dis
clean:
	rm -rf *.o key_led.dis key_led_elf key_led.bin
