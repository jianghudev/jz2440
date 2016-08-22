#define  BUSY  1


#define nand_sector_size_lp  2048
#define nand_block_mask_lp    (nand_sector_size_lp-1)



typedef unsigned int  s3c24x0_reg32 ;

typedef struct {
  s3c24x0_reg32 nfconf;
  s3c24x0_reg32 nfcont;
  s3c24x0_reg32 nfcmd;
  s3c24x0_reg32 nfaddr;
  s3c24x0_reg32 nfdata;
  s3c24x0_reg32 nfmeccd0;
  s3c24x0_reg32 nfmeccd1;
  s3c24x0_reg32 nfseccd;
  s3c24x0_reg32 nfstat;
  s3c24x0_reg32 nfestat0;
  s3c24x0_reg32 nfestat1;
  s3c24x0_reg32 nfmecc0;
  s3c24x0_reg32 nfmecc1;
  s3c24x0_reg32 nfsecc;
  s3c24x0_reg32 nfsblk;
  s3c24x0_reg32 nfeblk;
}s3c2440_nand;

typedef struct {
    void (*nand_reset)(void);
    void (*nand_idle)(void);
    void (*nand_select_chip)(void);
    void (*nand_deselect_chip)(void);
    void (*write_cmd)(int cmd);
    void (*write_addr)(unsigned int addr);
    unsigned char (*read_data)(void);
}t_nand_chip;


static s3c2440_nand*  s3c2440Nand =(s3c2440_nand*)0x4e000000;

static t_nand_chip nand_chip;


void nand_init(void);
void nand_read(unsigned char*buf, unsigned long start_addr, int size );


static    void nand_reset(void);
static    void nand_idle(void);
static    void nand_select_chip(void);
static    void nand_deselect_chip(void);
static    void write_cmd(int cmd);
static    void write_addr(unsigned int addr);
static    unsigned char read_data(void);


static    void nand_reset(void){
    nand_select_chip();
    write_cmd(0xff);
    nand_idle();
    nand_deselect_chip();

}
static    void nand_idle(void){
    int i=0;
    volatile unsigned char * p= (volatile unsigned char *)&s3c2440Nand->nfstat;
    while(*p & BUSY){
        for (i = 0; i < 10; ++i) {

        }
    }
}
static    void nand_select_chip(void){
    int i=0;
    s3c2440Nand->nfcont &= ~(1<<1);
    for (i = 0; i < 10; ++i) {

    }
}
static    void nand_deselect_chip(void){

    int i=0;
    s3c2440Nand->nfcont |= (1<<1);
}
static    void write_cmd(int cmd){

    volatile unsigned char * p= (volatile unsigned char *)&s3c2440Nand->nfcmd;
    *p =cmd;
}
static    void write_addr(unsigned int addr){
	int i;
	volatile unsigned char *p = (volatile unsigned char *)&s3c2440Nand->nfaddr;
	int col, page;

	col = addr & nand_block_mask_lp;
	page = addr / nand_sector_size_lp;

	*p = col & 0xff;			/* Column Address A0~A7 */
	for(i=0; i<10; i++);
	*p = (col >> 8) & 0x0f; 	/* Column Address A8~A11 */
	for(i=0; i<10; i++);
	*p = page & 0xff;			/* Row Address A12~A19 */
	for(i=0; i<10; i++);
	*p = (page >> 8) & 0xff;	/* Row Address A20~A27 */
	for(i=0; i<10; i++);
	*p = (page >> 16) & 0x03;	/* Row Address A28~A29 */
	for(i=0; i<10; i++);

}
static    unsigned char read_data(void){

	volatile unsigned char *p = (volatile unsigned char *)&s3c2440Nand->nfdata;
    return *p;
}

void nand_init(void){
#define TACLS   0
#define TWRPH0  3
#define TWRPH1  0
    nand_chip.nand_reset = nand_reset;
    nand_chip.nand_idle = nand_idle;
    nand_chip.nand_select_chip = nand_select_chip;
    nand_chip.nand_deselect_chip = nand_deselect_chip;
    nand_chip.write_cmd = write_cmd;
    nand_chip.write_addr = write_addr;
    nand_chip.read_data = read_data;

		/* 设置时序 */
        s3c2440Nand->nfconf = (TACLS<<12)|(TWRPH0<<8)|(TWRPH1<<4);
        /* 使能NAND Flash控制器, 初始化ECC, 禁止片选 */
        s3c2440Nand->nfcont = (1<<4)|(1<<1)|(1<<0);

    /* 复位NAND Flash */
    nand_reset();

}

void nand_read(unsigned char*buf, unsigned long start_addr,int size){
    int i =0, j=0;

    if((start_addr & nand_block_mask_lp) || (size & nand_block_mask_lp)){
        return;
    }
    nand_select_chip();

    for (i = start_addr; i < (start_addr+size); ++i) {
        write_cmd(0);
        write_addr(i);

        write_cmd(0x30);
        nand_idle();

        for (j = 0; j < nand_sector_size_lp; j++, i++) {
            *buf = read_data();
            buf ++;
        }

    }
    nand_deselect_chip();
    return ;

}


















































