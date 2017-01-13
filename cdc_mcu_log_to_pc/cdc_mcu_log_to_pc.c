#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include <pthread.h>
#include "sys/time.h"
#include "signal.h"

#ifdef ANDROID
#include <libusb.h>
#else /* ANDROID */
#include <libusb-1.0/libusb.h>
#endif /* ANDROID */

#define HMD_USB_VENDOR_ID		0x0BB4
#define HMD_USB_PRODUCT_ID		0x0308
#define TRANSFER_TIMEOUT		1000
#define SENSOR_MAX_PACKET_SIZE		64
#define SENSOR_INTERFACE		0
#define SENSOR_ENDPOINT_IN		(1 | LIBUSB_ENDPOINT_IN)
#define PROPERTY_MAX_PACKET_SIZE	64
#define PROPERTY_INTERFACE		2
#define PROPERTY_ENDPOINT_IN		(2 | LIBUSB_ENDPOINT_IN)
#define PROPERTY_ENDPOINT_OUT		2
//#define PROPERTY_FETCH_DURING_MICROSEC	(2 * 1000 * 1000)
#define PROPERTY_FETCH_DURING_MICROSEC	(200 * 1000)

#define PTHREAD_NUM			2


#define MCU_LOG_BUF_SIZE (16*1024)
char mcu_log_buf[MCU_LOG_BUF_SIZE]={0};
static int buf_pose=0;

struct __usb_data_struct {
	pthread_t p_thread[PTHREAD_NUM];
	pthread_attr_t p_thread_attr[PTHREAD_NUM];
//	pthread_mutex_t usb_access_mutex;
	void *p_thread_retval[PTHREAD_NUM];
	unsigned short pid;
	unsigned short vid;
	unsigned char sensor_ep_in;
	unsigned char property_ep_in;
	unsigned char property_ep_out;
	libusb_context *context_data;
	libusb_device_handle *usb_handle;
	libusb_device *usb_device;
	struct sigaction sigact;
	unsigned char do_exit;
	unsigned char dump_law;
	unsigned char count_fetch_during;
	unsigned char enable_prop_fetch;
};

static struct __usb_data_struct g_usb_data;

static void sighandler(int signum)
{
	struct __usb_data_struct *usb_data = &g_usb_data;
	printf("%s: get signum: %d\n", __func__, signum);
	usb_data->do_exit = 1;
}

static void dump_buffer(unsigned char *buffer, int buffer_size)
{
	int loop_i;
	printf("\t");
	for (loop_i = 0; loop_i < buffer_size; loop_i++) {
		printf("0x%2.2X ", buffer[loop_i]);
		if (loop_i % 8 == 7)
			printf("\n\t");
	}
	printf("\n");
}

static void *dump_mcu_log(void *p_data)
{
	struct __usb_data_struct *usb_data = p_data;
	int ret;
	time_t raw_time;
	struct tm *tm_p;
	unsigned char send_tmp[PROPERTY_MAX_PACKET_SIZE];
	unsigned char recv_tmp[PROPERTY_MAX_PACKET_SIZE];
	int transferred;
    static int loop_i=0;
	const int act_epout_size = PROPERTY_MAX_PACKET_SIZE;
    int recv_count=0;

	time(&raw_time);
	tm_p = localtime(&raw_time);
	printf("%s: %4.4d/%2.2d/%2.2d %2.2d:%2.2d:%2.2d "
		"dump mcu log start\n", __func__,
		tm_p->tm_year + 1900, tm_p->tm_mon + 1,
		tm_p->tm_mday, tm_p->tm_hour, tm_p->tm_min,
		tm_p->tm_sec);


    memset(send_tmp, 0x0, sizeof(send_tmp));
    send_tmp[0] = 'c';
    send_tmp[1] = 0x00;
    send_tmp[2] = 0x40; // get mcu log
    send_tmp[3] = 0x00;
    //send_tmp[1] = 0x03; //clear mcu log
    ret = libusb_bulk_transfer(
            usb_data->usb_handle,
            usb_data->property_ep_out,
            send_tmp,
            act_epout_size,
            &transferred,
            TRANSFER_TIMEOUT);
    if (ret < 0) {
        printf("[prop]  ret=%d   \n",ret);
        fprintf(stderr, "CDC transfer OUT error (%d), "
            "transferred: %d\n",
            ret, transferred);
        dump_buffer((unsigned char *)&send_tmp,
                PROPERTY_MAX_PACKET_SIZE);
        usb_data->do_exit = 1;
    }

    if (transferred < act_epout_size) {
        printf("[prop]  act=%d trans=%d  \n",act_epout_size, transferred);
        fprintf(stderr, "OUT: short read: %d (%d)\n",
                transferred, ret);
        usb_data->do_exit = 1;
    }
    printf("[prop]  ret=%d trans=%d \n", ret, transferred);

    buf_pose=0;
    recv_count=0;
    memset(mcu_log_buf,0x0,sizeof(mcu_log_buf));
	do{
        /* get response */
        usleep(2 * 1000);
        memset(&recv_tmp, 0x0, sizeof(recv_tmp));
        ret = libusb_bulk_transfer( usb_data->usb_handle, usb_data->property_ep_in, recv_tmp, PROPERTY_MAX_PACKET_SIZE, &transferred, TRANSFER_TIMEOUT);
        if (ret < 0) {
            fprintf(stderr, "CDC transfer IN error (%d), " "transferred: %d\n", ret, transferred);
            usb_data->do_exit = 1;
            break;
        }
        if(transferred <0 || transferred >64 ){
            printf("[mcu log] error! ret=%d recv_len=%d  \n", ret, transferred);
            break;
        }
        if(strncmp((const char*)recv_tmp,"dump_mcu_log_done",17) ==0 ){
            printf("[mcu log] dump log done! data=\n");
            for ( loop_i = 0; loop_i < buf_pose; ++loop_i)
            {
                printf("%c", mcu_log_buf[loop_i]);
            }
            printf("\n");
            break;
        }
        if(strncmp((const char*)recv_tmp,"dump_mcu_log_failed",19) ==0 ){
            printf("[mcu log] dump log failed! count=%d \n",recv_count);
            break;
        }
//        printf("recv len=%d data=",transferred);
//        for ( loop_i = 0; loop_i < transferred; ++loop_i)
//        {
//            printf("%c", recv_tmp[loop_i]);
//        }
//        printf("\n");

        memcpy(mcu_log_buf+buf_pose,recv_tmp,transferred);
        buf_pose+=transferred;
        recv_count++;
        if(buf_pose > MCU_LOG_BUF_SIZE){
            printf("[mcu log] log buf is full! buf_pose=%d \n", buf_pose);
            break;
        }
        //printf("[mcu log] ret=%d,recv_len=%d,data=%s \n", ret, transferred, recv_tmp);
	} while (1);

	time(&raw_time);
	tm_p = localtime(&raw_time);
	printf("%s: %4.4d/%2.2d/%2.2d %2.2d:%2.2d:%2.2d "
		"dump mcu log end\n", __func__,
		tm_p->tm_year + 1900, tm_p->tm_mon + 1,
		tm_p->tm_mday, tm_p->tm_hour, tm_p->tm_min,
		tm_p->tm_sec);
	pthread_exit(usb_data->p_thread_retval[0]);
}


static int create_threads(struct __usb_data_struct *usb_data)
{
	int ret = 0;
	if (usb_data->enable_prop_fetch) {
		pthread_attr_init(&usb_data->p_thread_attr[0]);
		pthread_attr_setdetachstate(&usb_data->p_thread_attr[0],
						PTHREAD_CREATE_JOINABLE);
		if (pthread_create(&usb_data->p_thread[0],
						&usb_data->p_thread_attr[0],
						dump_mcu_log,
						(void *)usb_data) != 0) {
			printf("create p thread 0 failed\n");
			ret = -1;
		}
		pthread_attr_destroy(&usb_data->p_thread_attr[0]);
		if (ret != 0)
			return ret;
	}

	return ret;
}

static void waiting_threads_exit(struct __usb_data_struct *usb_data)
{
	if (usb_data->enable_prop_fetch)
		pthread_join(usb_data->p_thread[0],
					usb_data->p_thread_retval[0]);

}

static void usage(void)
{
	printf("get mcu log by cdc:\n"
		"\t-p: create thread \n");
}

static int parse_opt(struct __usb_data_struct *usb_data,
						int argc, char *argv[])
{
	int c;
	while ((c = getopt(argc, argv, "gc")) != -1) {
		switch (c) {
		case 'g':
			usb_data->enable_prop_fetch = 1;
			break;
		default:
			usage();
			exit(-1);
			break;
		}
	}
	return 0;
}



int main(int argc, char *argv[])
{
	int ret;
	struct __usb_data_struct *usb_data = &g_usb_data;
	struct libusb_device_descriptor desc;
	unsigned char descript_string[64];
	char descript_string_length;
	int loop_i;
	memset(usb_data, 0x0, sizeof(struct __usb_data_struct));

	parse_opt(&g_usb_data, argc, argv);

	usb_data->vid = HMD_USB_VENDOR_ID;
	usb_data->pid = HMD_USB_PRODUCT_ID;
	usb_data->sensor_ep_in = SENSOR_ENDPOINT_IN;
	usb_data->property_ep_in = PROPERTY_ENDPOINT_IN;
	usb_data->property_ep_out = PROPERTY_ENDPOINT_OUT;

	if (getuid() != 0) {
		fprintf(stderr, "error, need superuser permission\n");
		return -1;
	}
	if (nice(-3) == -1) {
		fprintf(stderr, "change process priority failed\n");
		return -errno;
	}
	ret = libusb_init(&usb_data->context_data);
	if (ret < 0) {
		printf("%s: libusb initialize failed, exit(%d)\n",
						__func__, ret);
		return ret;
	}
	/*
	if (usb_data->dump_law)
		libusb_set_debug(usb_data->context_data,
					LIBUSB_LOG_LEVEL_DEBUG);
	*/
	usb_data->usb_handle = libusb_open_device_with_vid_pid(
			usb_data->context_data,
			usb_data->vid, usb_data->pid);
	if (usb_data->usb_handle == NULL) {
		printf("%s: %d libusb_open_device_with_vid_pid failed, "
			"vid: 0x%4.4X, pid: 0x%4.4X\n", __func__, __LINE__,
			usb_data->vid, usb_data->pid);
		ret = -EIO;
		goto error_libusb_exit;
	}

	usb_data->usb_device = libusb_get_device(usb_data->usb_handle);

#ifndef ANDROID
    if( libusb_kernel_driver_active(usb_data->usb_handle,PROPERTY_INTERFACE) ==1 ){
		printf("cdc kernel driver active\n " );

        ret = libusb_detach_kernel_driver(usb_data->usb_handle, PROPERTY_INTERFACE);
        if (ret != 0) {
            printf("%s: libusb_set_auto_detach_kernel_driver failed(%d)\n",
                                __func__, ret);
            ret = -EIO;
            goto error_libusb_exit_with_close;
        }

    }
#endif /* ANDROID */


	ret = libusb_claim_interface(usb_data->usb_handle, PROPERTY_INTERFACE);
	if (ret != 0) {
		printf("%s: libusb_claim_interface: PROPERTY_INTERFACE"
					" failed(%d)\n", __func__, ret);
		ret = -EIO;
		goto error_libusb_exit_with_close;
	}

	libusb_get_device_descriptor(usb_data->usb_device, &desc);
	printf("Vendor  ID: 0x%4.4X\n", desc.idVendor);
	printf("Product ID: 0x%4.4X\n", desc.idProduct);
	descript_string_length = sizeof(descript_string) - 1;
	for (loop_i = 1; loop_i < 4; loop_i++) {
		memset(&descript_string, 0x0, sizeof(descript_string));
		ret = libusb_get_string_descriptor_ascii(usb_data->usb_handle,
			loop_i, descript_string, descript_string_length);
		if (ret >= 0)
			printf("String Index %d: %s\n",
					loop_i, descript_string);
		else
			printf("String Index %d: failed(%d)\n", loop_i, ret);
	}

	usb_data->sigact.sa_handler = sighandler;
	sigemptyset(&usb_data->sigact.sa_mask);
	usb_data->sigact.sa_flags = 0;
	sigaction(SIGINT, &usb_data->sigact, NULL);
	sigaction(SIGTERM, &usb_data->sigact, NULL);
	sigaction(SIGQUIT, &usb_data->sigact, NULL);

	//pthread_mutex_init(&usb_data->usb_access_mutex, NULL);

	create_threads(usb_data);

	waiting_threads_exit(usb_data);

	//pthread_mutex_destroy(&usb_data->usb_access_mutex);

	libusb_release_interface(usb_data->usb_handle, PROPERTY_INTERFACE);
	libusb_close(usb_data->usb_handle);
	return 0;


error_libusb_exit_with_close:
	libusb_close(usb_data->usb_handle);
error_libusb_exit:
	libusb_exit(usb_data->context_data);
	printf("get error, exit\n");
	return ret;
}
