SECTIONS {
    first  0x00000000  : {head.o init.o}
    second 0xb0004000 : AT(2048)  {led.o}
    }
