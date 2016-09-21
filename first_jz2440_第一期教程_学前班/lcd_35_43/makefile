CC = arm-linux-gcc
LD = arm-linux-ld
AR = arm-linux-ar
OBJCOPY =arm-linux-objcopy
OBJDUMP = arm-linux-objdump

INCLUDEDIR := $(shell pwd)/include
CFLAGS := -Wall -02
CPPFLAGS := -nostdinc -I$(INCLUDEDIR)

export CC LD AR OBJCOPY OBJDUMP INCLUDEDIR CFLAGS CPPFLAGS

obj := head.o init.o nand.o interrupt.o serial.o lcddrv.o framebuffer.o lcdlib.o main.o lib/libc.a

lcd.bin:$(obj)
	$(LD) -Tlcd.lds -o lcd_elf $^
	$(OBJCOPY) -O binary -S lcd_elf $@
	$(OBJDUMP) -m arm -D  lcd_elf > lcd.dis


.PHONY : lib/libc.a
lib/libc.a:
	cb lib;make; cd ..

%.o:%.c
	$(CC) $(CPPFLAGS) $(CFLAGS) -c -o $@ %<


%.o:%.S
	$(CC) $(CPPFLAGS) $(CFLAGS) -c -o $@ %<


clean:
	make clean -C lib 
	rm -f lcd.bin lcd_elf lcd.dis *.o
