package com.htc.chirp_fota_service;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hubin_jiang on 2018/5/23.
 */

public class ccg4 {
    private static final String TAG=Const.G_TAG;
    private Usb mUsb;

    private static final String CCG4_FM1_NAME = "htc_apn_4225_v14_1.cyacd";
    private static final String CCG4_FM2_NAME = "htc_apn_4225_v14_2.cyacd";

    ////  data format   cmd[1]  len[1] index[2] check[2]  data[56]
    ////  last line have \r\n[2]
    private static final int PKG_DATA_LEN = UsbTunnelData.USB_CDC_SEND_PACKET_MAX_SIZE -6;

    private short data_index=0;

    public ccg4(Usb usb) {
        mUsb=usb;
    }


    public short ccg4_checksum(byte[] data, int len) {
        short sum = 0;
        for (int i = 0; i < len; i++) {
            sum +=data[i];
        }
        return sum;
    }

    public boolean send_end_pkg(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = Const.CMD_FOTA_END;  //cmd
        Data.send_array[1] = 1;   //len
        Data.send_array[2] = Const.FOTA_TYPE_CCG4;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        int retryCount = 5;
        while(retryCount-- >0 ){
            if (mUsb.RequestCdcData(Data) == true) {
                if (Data.recv_array_count != 3) {
                    continue;
                }
                if(Const.CMD_FOTA_END==Data.recv_array[0]){
                    if (0== Data.recv_array[2]) {
                        return true;
                    }else{
                        Log.e(TAG, "err type="+Data.recv_array[2]);
                    }
                    break;
                }
            }
        }
        return false;
    }


    public boolean ccg4_handle_line(byte[] data, int len) {
        byte[] checksum_data = new byte[PKG_DATA_LEN];
        int checksum_len = 0;

        int retryCount = 5;
        int pkg_count=1;
        if(len > PKG_DATA_LEN ){
            pkg_count= len / PKG_DATA_LEN ;
        }
        OUTER_FOR:for (int i = 0; i < pkg_count; i++) {
            UsbTunnelData Data = new UsbTunnelData();
            Data.send_array[0] = Const.CMD_FOTA_TRANSFER;  //cmd
            if ((pkg_count-1) == i) {
                int tmp_len=(byte)(len%PKG_DATA_LEN);
                Data.send_array[1] = (byte)(tmp_len +4);
                Arrays.fill(checksum_data, (byte) 0);
                System.arraycopy(data, i*PKG_DATA_LEN, checksum_data, 0, tmp_len);
                checksum_len=tmp_len;
            }else{
                Data.send_array[1] = PKG_DATA_LEN+4;
                Arrays.fill(checksum_data, (byte) 0);
                System.arraycopy(data, i*PKG_DATA_LEN, checksum_data, 0, PKG_DATA_LEN);
                checksum_len=PKG_DATA_LEN;
            }
            Data.send_array[2] = (byte)(data_index & 0xff);
            Data.send_array[3] = (byte)(data_index>>8 & 0xff);
            data_index++;
            ////checksum
            short tmp_cs=ccg4_checksum(checksum_data,checksum_len);
            Data.send_array[4] =   (byte)(tmp_cs & 0xff);
            Data.send_array[5] =  (byte)(tmp_cs & 0xff);
            System.arraycopy(checksum_data, 0, Data.send_array, 6, checksum_len);

            Data.send_array_count = checksum_len;
            Data.recv_array_count = Data.recv_array.length;
            Data.wait_resp_ms = 2;

            boolean ackOK =false;
            while(retryCount-- >0 ){
                ackOK =false;
                if (mUsb.RequestCdcData(Data) == true) {
                    if (Data.recv_array_count != 3) {
                        continue;
                    }
                    if(Const.CMD_FOTA_TRANSFER==Data.recv_array[0]){
                        if (0== Data.recv_array[2]) {
                            ackOK=true;
                        }else{
                            Log.e(TAG, "recv ack err="+Data.recv_array[2]);
                        }
                        break;
                    }
                }
            }
            if (!ackOK) {
                Log.e(TAG, "data ack err="+Data.recv_array[2]);
                return false;
            }
        }
        return true;
    }

    public boolean init_ccg4(){
        data_index=0;
        return true;
    }

    public boolean need_update_ccg4_fw(long filesize){
        int retryCount = 5;
        boolean needUpdate=false;
        init_ccg4();
        try {
            UsbTunnelData Data = new UsbTunnelData();
            Data.send_array[0] = Const.CMD_FOTA_START;  //cmd
            Data.send_array[1] = 21;   //len
            Data.send_array[2] = Const.FOTA_TYPE_CCG4;

            String ccg_version= "ccg4 for mfg";
            byte[] tmp_ver= ccg_version.getBytes("UTF-8");
            System.arraycopy(tmp_ver, 0, Data.send_array, 3, tmp_ver.length);

            Data.send_array[19] =(byte) (filesize & 0xff);
            Data.send_array[20] =(byte) (filesize>>8 & 0xff);
            Data.send_array[21] =(byte) (filesize>>16 & 0xff);
            Data.send_array[22] =(byte) (filesize>>24 & 0xff);

            Data.send_array_count = 23;
            Data.recv_array_count = Data.recv_array.length;
            Data.wait_resp_ms = 2;
            while(retryCount-- >0 ){
                if (mUsb.RequestCdcData(Data) == true) {
                    if (Data.recv_array_count != 3) {
                        continue;
                    }
                    if(Const.CMD_FOTA_START==Data.recv_array[0]){
                        if (0== Data.recv_array[2]) {
                            needUpdate=true;
                        }else if (1== Data.recv_array[2]) {
                            Log.d(TAG, "no need update");
                        }else{
                            Log.e(TAG, "err type="+Data.recv_array[2]);
                        }
                        break;
                    }
                }
            }
            return needUpdate;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int updateFW(){
        try {
            int line_num=0;

            File dir = Environment.getDataDirectory();
            File ccgfile = new File(dir, CCG4_FM1_NAME);
            ccgfile.length();

            String path = ccgfile.getAbsolutePath();
            String name = ccgfile.getName();
            Log.d(TAG,"path="+path+" name="+name);
            if(ccgfile.exists()){
                if( !need_update_ccg4_fw(ccgfile.length()) ){
                    return -1;
                }
                InputStream is = new FileInputStream(ccgfile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    byte[] srtbyte = line.getBytes("UTF-8"); //no including \r\n  ,we must add it
                    byte[] tmp_crlf= {0X0D, 0X0A};
                    byte[] tmp_byte= new byte[UsbTunnelData.USB_CDC_SEND_PACKET_MAX_SIZE];

                    System.arraycopy(srtbyte, 0, tmp_byte, 0, srtbyte.length);
                    System.arraycopy(tmp_crlf, 0, tmp_byte, srtbyte.length, 2);
                    if( !ccg4_handle_line(tmp_byte,srtbyte.length+2) ){
                        Log.d(TAG, "line err, please check");
                        return -1;
                    }
                }
                is.close();

                send_end_pkg();


            }else{
                Log.e(TAG, "file not exist");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
