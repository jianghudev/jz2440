objs := head.o init.o interrupter.o main.o



inter.bin:$(objs) s3c24xx.h
	arm-linux-ld -Ttext 0x00000000 $(objs) -o int_elf
	arm-linux-objcopy -O binary -S  int_elf inter.bin
	arm-linux-objdump -D -m arm  int_elf > int.dis

%.o:%.c
	arm-linux-gcc -c -o $@ $^

%.o:%.S
	arm-linux-gcc -c -o $@ $^

.PHONY=clean

clean:
	rm -rf *.o inter.bin int.dis int_elf
