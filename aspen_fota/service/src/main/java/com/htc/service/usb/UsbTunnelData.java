// ++ LICENSE-HIDDEN SOURCE ++

package com.htc.service.usb;

import java.util.Arrays;

/**
 * Created by cdplayer0212 on 2017/2/3.
 */

public class UsbTunnelData {


    public static final int USB_CDC_SEND_PACKET_MAX_SIZE = 64;
    public static final int USB_CDC_RECV_PACKET_MAX_SIZE = 64 * 16;
    public byte[] send_array = new byte[USB_CDC_SEND_PACKET_MAX_SIZE];
    public int send_array_count;
    public byte[] recv_array = new byte[USB_CDC_RECV_PACKET_MAX_SIZE];
    public int recv_array_count;
    public int wait_resp_ms;

    public UsbTunnelData() {
        Arrays.fill(this.send_array, (byte) 0);
        this.send_array_count = -1;
        Arrays.fill(this.recv_array, (byte) 0);
        this.recv_array_count = -1;
        wait_resp_ms = 0;
    }

    public void McuUsbCdcTunnelDataReset() {
        send_array = new byte[USB_CDC_SEND_PACKET_MAX_SIZE];
        Arrays.fill(this.send_array, (byte) 0);
        this.send_array_count = -1;
        recv_array = new byte[USB_CDC_RECV_PACKET_MAX_SIZE];
        Arrays.fill(this.recv_array, (byte) 0);
        this.recv_array_count = -1;
        wait_resp_ms = 0;
    }
}
