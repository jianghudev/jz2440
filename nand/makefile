objs := head.o init.o nand.o main.o
nand.bin:$(objs)
	arm-linux-ld -Tnand.lds $^ -o nand_elf
	arm-linux-objcopy -O binary -S  nand_elf nand.bin
	arm-linux-objdump -D -m arm  nand_elf > nand.dis


%.o:%.c
	arm-linux-gcc -c -o $@ $^

%.o:%.S
	arm-linux-gcc -c -o $@ $^



.PHONY=clean
clean:
	-rm -rf *.o nand.bin nand_elf nand.dis
