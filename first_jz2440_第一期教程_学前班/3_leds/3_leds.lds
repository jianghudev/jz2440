SECTIONS{
    . = 0X00;
    .text   : {*(.text)}
    .radata ALIGN(4)   : {*(.radata)}
    .data ALIGN(4)   : {*(.data)}
    .bss ALIGN(4)   : {*(.bss)  *(.COMMON) }
    
    }
