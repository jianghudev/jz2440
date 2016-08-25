obj =  head.o init.o led.o

mmu.bin : $(obj)
	#arm-linux-gcc -Wall -O2 -c -o head.o head.S
	#arm-linux-gcc -c -o init.o init.c
	#arm-linux-gcc -c -o led.o  led.c
	arm-linux-ld -Tmmu.lds  -o mmu_elf
	arm-linux-objcopy -S -O binary mmu_elf mmu.bin
	arm-linux-objdump -D -m arm  mmu_elf  > mmu.dis

%.o : %.c
	arm-linux-gcc -c -o $@ $<
%.o : %.S
	arm-linux-gcc -c -o $@ $<
clean:
	rm -rf *.o mmu.bin mmu.dis mmu_elf

