// ++ LICENSE-HIDDEN SOURCE ++

package com.htc.chirp_fota_service;

/**
 * Created by cdplayer0212 on 2016/11/9.
 */

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

class UsbCdcTunnel {
    final static String TAG = "ChirpFota";
    private final int USB_CDC_DATA_INTERFACE = 1;
    private final Object McuUsbCdcTunnelSyncObject = new Object();
    private UsbDeviceConnection CDCConnection = null;
    private UsbDevice mUsbDevice = null;
    private UsbInterface cdcInterface = null;
    private UsbEndpoint cdcEndpointIn = null;
    private UsbEndpoint cdcEndpointOut = null;
    private final int waiting_ms = 2;

    public void SetupUsbInterface(UsbDevice Usbdevice, UsbDeviceConnection Connection) {
        mUsbDevice = Usbdevice;
        CDCConnection = Connection;
    }

    public boolean CdcTunnel_is_Initialized() {
        if ((mUsbDevice != null) && (CDCConnection != null)) {
            return true;
        }
        return false;
    }

    private boolean ClaimUsbCdcInterface() {
        boolean bOpenInterface = false;

        cdcInterface = mUsbDevice.getInterface(USB_CDC_DATA_INTERFACE);
        for (int i = 0; i < 10; i++) {
            if (CdcTunnel_is_Initialized() == false) {
                Log.e(TAG, "ClaimUsbCdcInterface: UsbDevice or CDC Connection not ready, exit");
                return false;
            }
            bOpenInterface = CDCConnection.claimInterface(cdcInterface, false);
            if (bOpenInterface) {
                break;
            }
            try {
                Thread.sleep(20);
                Log.w(TAG, "SendCdcData: claimInterface sleep retry = " + i + " count");
            } catch (InterruptedException e) {
                Thread.interrupted();
                return false;
            }
        }
        if (bOpenInterface) {
            for (int i = 0; i < cdcInterface.getEndpointCount(); i++) {
                UsbEndpoint end = cdcInterface.getEndpoint(i);
                if (end.getDirection() == UsbConstants.USB_DIR_IN) {
                    cdcEndpointIn = end;
                } else {
                    cdcEndpointOut = end;
                }
            }
        } else {
            Log.w(TAG, "SendCdcData: claimInterface error !!!");
            return false;
        }

        if (cdcEndpointIn == null || cdcEndpointOut == null) {
            Log.w(TAG, "request endpoint failed: in: " + cdcEndpointIn + ", out" + cdcEndpointOut + " !!!");
            return false;
        }
        return true;
    }

    private boolean ReleaseUsbCdcInterface() {
        boolean ret = false;
        ret = CDCConnection.releaseInterface(cdcInterface);
        cdcInterface = null;
        cdcEndpointIn = null;
        cdcEndpointOut = null;
        return ret;
    }

    private int RawSendCdcData(byte[] send_data, int send_data_count) {
        int send_count;
        //Log.d(TAG, "RawSendCdcData: send byte array [" + send_data_count + "]: " + Arrays.toString(send_data));
        send_count = CDCConnection.bulkTransfer(cdcEndpointOut, send_data, send_data_count, 1000);
        if (send_count < 0) {
            Log.w(TAG, "RawSendCdcData: bulkTransfer return failure: " + send_count);
            return send_count;
        }

        if (send_count != send_data_count) {
            Log.w(TAG, "RawSendCdcData: bulkTransfer: USB data length(" + send_count +
                    ") not equal origin data length(" + send_data_count + ")");
            return -1;
        }
        return send_count;
    }

    private int RawRecvCdcData(byte[] recv_data, int recv_data_count) {
        recv_data_count = CDCConnection.bulkTransfer(cdcEndpointIn, recv_data, recv_data_count, 1000);
        if (recv_data_count < 0) {
            Log.w(TAG, "RawRecvCdcData: bulkTransfer return failure: " + recv_data_count);
        }
        //Log.d(TAG, "RawRecvCdcData: get byte array [" + recv_data_count + "]: " + Arrays.toString(recv_data));
        return recv_data_count;
    }

    public boolean RequestSingleCdcData(UsbTunnelData Data) {
        if (this.CdcTunnel_is_Initialized() == false) {
            Log.e(TAG, "SendCommonCdcData: CDC Tunnel not ready !!!");
            return false;
        }
        synchronized (McuUsbCdcTunnelSyncObject) {
            try {
                Thread.sleep(waiting_ms);
            } catch (InterruptedException e) {
                Log.d(TAG, "RequestSingleCdcData: sleep error at entry stage");
            }
            if (this.ClaimUsbCdcInterface() != true) {
                return false;
            }
            if (this.RawSendCdcData(Data.send_array, Data.send_array_count) < 0) {
                this.ReleaseUsbCdcInterface();
                return false;
            }

            if (Data.wait_resp_ms > 0) {
                try {
                    Thread.sleep(Data.wait_resp_ms);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }

                if (this.RawRecvCdcData(Data.recv_array, Data.recv_array_count) < 0) {
                    this.ReleaseUsbCdcInterface();
                    return false;
                }
                // Log.d(TAG, "SendCommonCdcData get: " + Arrays.toString(Data.recv_array) + "(" + Data.recv_array_count + ")");
            }

            this.ReleaseUsbCdcInterface();
            try {
                Thread.sleep(waiting_ms);
            } catch (InterruptedException e) {
                Log.d(TAG, "RequestSingleCdcData: sleep error at leave stage");
            }

            return true;
        }
    }

    public boolean RequestBlockCdcDataForHLog(UsbTunnelData Data) {
        if (this.CdcTunnel_is_Initialized() == false) {
            Log.e(TAG, "SendCommonCdcData: CDC Tunnel not ready !!!");
            return false;
        }
        synchronized (McuUsbCdcTunnelSyncObject) {
            try {
                Thread.sleep(waiting_ms);
            } catch (InterruptedException e) {
                Log.d(TAG, "RequestBlockCdcDataForHLog: sleep error at entry stage");
            }
            boolean capture_recv_log = true;
            //byte[] temp_data = new byte[64];
            int recv_count_ret;
            int recv_count_offset;
            int recvBufferSize;
            int recvBufferSizeOffset;
            final String log_done = "dump_mcu_log_done";
            final String log_fail = "dump_mcu_log_failed";
            String RetStr = null;

            if (this.ClaimUsbCdcInterface() != true) {
                return false;
            }

            if (this.RawSendCdcData(Data.send_array, Data.send_array_count) < 0) {
                this.ReleaseUsbCdcInterface();
                return false;
            }

            //Data.recv_array_count = 0;
            recv_count_offset = 0;
            recvBufferSize = Data.recv_array_count;
            recvBufferSizeOffset = 0;
            long intervalS = System.currentTimeMillis();
            do {
                /*
                try {
                    Thread.sleep(Data.wait_resp_ms);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                */

                //Arrays.fill(temp_data, (byte) 0);
                //recv_count_ret = temp_data.length;
                //Log.d(TAG, "Data.recv_array_count: " + Data.recv_array_count);
                recv_count_ret = RawRecvCdcData(Data.recv_array, recvBufferSize);
                if (recv_count_ret < 0) {
                    this.ReleaseUsbCdcInterface();
                    return false;
                }
                recvBufferSizeOffset += recv_count_ret;
                //Log.d(TAG, "recv size="+recv_count_ret+" offset="+recvBufferSizeOffset);
                
                // Log.d(TAG, "copy array count: " + Data.recv_array_count + ", count: " + recv_count_ret + ", offset: " + recv_count_offset);
                //System.arraycopy(temp_data, 0, Data.recv_array, recv_count_offset, recv_count_ret);
                //Data.recv_array_count += recv_count_ret;
                //recv_count_offset += (recv_count_ret);
                //Log.d(TAG, "SendCommonCdcData get: " + Arrays.toString(temp_data) + "(" + recv_count_ret + ")");
                /*
                try {
                    RetStr = new String(temp_data, 0, recv_count_ret, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if(RetStr == null){
                    Log.w(TAG, "parse recv data error!");
                    return false;
                }
                RetStr = RetStr.replaceAll("[^[:print:]]", "");
                if (RetStr.equals(log_done) || RetStr.equals(log_fail)) {
                    Log.e(TAG, "SendCommonCdcData: Done, exit");
                    capture_recv_log = false;
                }
                */
                if (recv_count_ret < 64){
                    capture_recv_log = false;
                }
                if (recvBufferSizeOffset >= recvBufferSize){
                    capture_recv_log = false;
                }
            } while(capture_recv_log == true);
            long intervalE = System.currentTimeMillis();
            Log.d(TAG, "RequestBlockCdcDataForHLog time: " + (intervalE - intervalS) +" mSec, data length: " + recvBufferSizeOffset + " >_<");


            this.ReleaseUsbCdcInterface();
            try {
                Thread.sleep(waiting_ms);
            } catch (InterruptedException e) {
                Log.d(TAG, "RequestBlockCdcDataForHLog: sleep error at leave stage");
            }
            return true;
        }
    }
}
