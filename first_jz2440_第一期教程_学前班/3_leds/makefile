CFLAGS := -Wall -Wstrict-prototypes -O2 -fomit-frame-pointer -ffreestanding
3_leds.bin:3_leds.c head.S
	arm-linux-gcc $(CFLAGS) -c -g -o 3_leds.o 3_leds.c
	arm-linux-gcc $(CFLAGS) -c -g -o head.o head.S
	#arm-linux-ld -Ttext 0x00000000 head.o 3_leds.o -o 3_leds_elf
	arm-linux-ld -T3_leds.lds head.o 3_leds.o -o 3_leds_elf
	arm-linux-objcopy -O binary -S 3_leds_elf  3_leds.bin
	#arm-linux-objdump -b binary -D -m arm  3_leds.bin > 3_leds.dis
	arm-linux-objdump -D -m arm  3_leds_elf > 3_leds.dis

clean:
	rm -rf *.o 3_leds_elf 3_leds.dis 3_leds.bin

