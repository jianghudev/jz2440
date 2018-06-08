package com.htc.chirp_fota_service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by hubin_jiang on 2018/5/23.
 */

public class ccg4 {
    private static final String TAG=Const.G_TAG;
    private Usb mUsb;

    private static  String CCG4_FM1_NAME = "htc_apn_4225_v14_1.cyacd";
    private static  String CCG4_FM2_NAME = "htc_apn_4225_v14_2.cyacd";
    private boolean CCG4_FW1_UPDATE_OK=false;
    private boolean CCG4_FW2_UPDATE_OK=false;
    private byte[] ccg4_version = null;

    ////  data format   cmd[1]  len[1] index[2] check[2]  data[56]
    ////  last line have \r\n[2]
    private static final int PKG_DATA_LEN = UsbTunnelData.USB_CDC_SEND_PACKET_MAX_SIZE -6;

    public static final int CCG4_LINE_LENGTH = 528+8;

    private short data_index=0;

    private Context mContext;

    public ccg4(Usb usb, Context ctxt) {
        mUsb=usb;
        mContext=ctxt;


    }


    public short ccg4_checksum(byte[] data, int len) {
        short sum = 0;
        short tmp=0;
        for (int i = 0; i < len; i++) {
            tmp=data[i];
            sum +=tmp;
        }
        //Log.d(TAG, "checksum="+sum);
        return sum;
    }

    private byte[] get_ccg4_ver(File cfile){
        byte[] raw_version= new byte[24];
        Arrays.fill(raw_version, (byte) 0);
        try {
            InputStream is = new FileInputStream(cfile);
            BufferedInputStream bstream = new BufferedInputStream(is);
            long skip_len= bstream.skip(0x1d9);
            int len=bstream.read(raw_version,0,16);  //
            Log.d(TAG,"len="+len+ " raw_version="+Arrays.toString(raw_version) );
            is.close();

            return raw_version;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return raw_version;
    }

    boolean send_query_pkg(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = Const.CMD_FOTA_QUERY;  //cmd
        Data.send_array[1] = 17;   //len
        Data.send_array[2] = Const.FOTA_TYPE_CCG4;
        if(null !=ccg4_version){
            System.arraycopy(ccg4_version, 0, Data.send_array, 3, 16);
        }
        Data.send_array_count = 19;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        int retryCount = 10;
        while(retryCount-- >0 ){
            if (mUsb.RequestCdcData(Data) == true) {
                if (3 ==Data.recv_array_count &&  Const.CMD_FOTA_QUERY==Data.recv_array[0] && 0== Data.recv_array[2] ) {
                    CCG4_FW2_UPDATE_OK=false;
                    CCG4_FW2_UPDATE_OK=false;
                    return true;
                }
            }
            try {
                if(9 ==retryCount ){
                    Thread.sleep(100);
                }else{
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
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

        int retryCount = 0;
        int pkg_count=1;
        if(len > PKG_DATA_LEN ){
            pkg_count= (len / PKG_DATA_LEN )+1;
            //Log.e(TAG, "pkg_count="+pkg_count );
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
                //Log.e(TAG, "tmp_len="+tmp_len+" i="+i );
            }else{
                Data.send_array[1] = PKG_DATA_LEN+4;
                Arrays.fill(checksum_data, (byte) 0);
                System.arraycopy(data, i*PKG_DATA_LEN, checksum_data, 0, PKG_DATA_LEN);
                checksum_len=PKG_DATA_LEN;
            }
            Data.send_array[2] = (byte)(data_index & 0xff);
            Data.send_array[3] = (byte)(data_index>>8 & 0xff);

            ////checksum
            short tmp_cs=ccg4_checksum(checksum_data,checksum_len);
            Data.send_array[4] =   (byte)(tmp_cs & 0xff);
            Data.send_array[5] =  (byte)(tmp_cs>>8 & 0xff);
            System.arraycopy(checksum_data, 0, Data.send_array, 6, checksum_len);

            Data.send_array_count = checksum_len+6;
            Data.recv_array_count = Data.recv_array.length;
            Data.wait_resp_ms = 2;

            boolean ackOK =false;
            retryCount = 5;
            while(retryCount >0 ){
                if (mUsb.RequestCdcData(Data) == true) {
                    if(3==Data.recv_array_count && Const.CMD_FOTA_TRANSFER==Data.recv_array[0]  && 0==Data.recv_array[2]){
                        data_index++;
                        ackOK=true;
                        break;
                    }
                }
                retryCount--;
                Log.e(TAG, "request fail, retryCount="+retryCount );
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

    private int get_ccg4_file_from_phone() {
        //File dir = Environment.getDataDirectory();
        File dir = mContext.getCacheDir();
        File[] tmp_files = dir.listFiles();// must have permission
        if (tmp_files != null) {// 先判断目录是否为空，否则会报空指针
            for (File file : tmp_files) {
                if (file.isFile()) {
                    String fileName = file.getAbsolutePath();
                    if (fileName.endsWith("1.cyacd")) {
                        CCG4_FM1_NAME= fileName;
                    }else if (fileName.endsWith("2.cyacd")) {
                        CCG4_FM2_NAME= fileName;
                    }
                }
            }
        }else{
            Log.d(TAG,"dir is null");
            return -1;
        }
        return 0;
    }


    void show_status_dlg(int status){
        Intent d_intent = new Intent(mContext,ActivityDialog.class);
        d_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (status == 0) {
            d_intent.putExtra("status", "update ok!");
        }else{
            d_intent.putExtra("status", "update fail!");
        }
        mContext.startActivity(d_intent);
    }

    public int need_update_ccg4_fw(){
        int retryCount = 5;
        init_ccg4();
        try {
            if( 0 != get_ccg4_file_from_phone() ){
                return -1;
            }

            File ccgfile = new File(CCG4_FM1_NAME);
            String path = ccgfile.getAbsolutePath();
            Log.d(TAG,"file1="+path);
            if(!ccgfile.exists()) {
                Log.e(TAG, "file not exist");
                return -1;
            }
            long file_size=ccgfile.length();
            ccg4_version =get_ccg4_ver(ccgfile);

            UsbTunnelData Data = new UsbTunnelData();
            Data.send_array[0] = Const.CMD_FOTA_START;  //cmd
            Data.send_array[1] = 21;   //len
            Data.send_array[2] = Const.FOTA_TYPE_CCG4;

            String ccg_version= "ccg4 for mfg";
            byte[] tmp_ver= ccg_version.getBytes("UTF-8");
            System.arraycopy(tmp_ver, 0, Data.send_array, 3, tmp_ver.length);

            Data.send_array[19] =(byte) (file_size & 0xff);
            Data.send_array[20] =(byte) (file_size>>8 & 0xff);
            Data.send_array[21] =(byte) (file_size>>16 & 0xff);
            Data.send_array[22] =(byte) (file_size>>24 & 0xff);

            Data.send_array_count = 23;
            Data.recv_array_count = Data.recv_array.length;
            Data.wait_resp_ms = 2;
            while(retryCount-- >0 ){
                if (mUsb.RequestCdcData(Data) == true) {
                    if (Data.recv_array_count != 3) {
                        continue;
                    }
                    if(Const.CMD_FOTA_START==Data.recv_array[0]){
                        if (11== Data.recv_array[2]) {
                            return 11;
                        }else if (12== Data.recv_array[2]) {
                            return 12;
                        }else if (0== Data.recv_array[2]) {
                            Log.d(TAG, "ccg4 start ack can't be 0");
                        }else{
                            Log.e(TAG, "err type="+Data.recv_array[2]);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }



    public int updateFW() throws InterruptedException{
        int ack_start= need_update_ccg4_fw();
        try {
            int line_num=0;
            File ccgfile=null;
            if( 11 ==  ack_start){
                ccgfile = new File(CCG4_FM1_NAME);
            }else if( 12 == ack_start ){
                ccgfile = new File(CCG4_FM2_NAME);
            }else{
                Log.e(TAG,"err! ack_start="+ack_start);
                throw new InterruptedException();
                //Thread.currentThread().stop();
                //return 13;   //// MCU will reboot, usb reconnect
                //Thread.currentThread().interrupt();
            }
            String path = ccgfile.getAbsolutePath();
            Log.d(TAG,"file="+path);
            if(!ccgfile.exists()) {
                Log.e(TAG, "file not exist");
                return -1;
            }
            data_index=1; //first pkg index is 1

            InputStream is = new FileInputStream(ccgfile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str_line = null;
            byte[] line_data= new byte[CCG4_LINE_LENGTH];
            while ((str_line = reader.readLine()) != null) {
                line_num++;
                Log.d(TAG, "read line="+line_num);
                byte[] srtbyte = str_line.getBytes("UTF-8"); //no including \r\n  ,we must add it
                byte[] tmp_crlf= {0X0D, 0X0A};
                Arrays.fill(line_data, (byte) 0);
                System.arraycopy(srtbyte, 0, line_data, 0, srtbyte.length);
                System.arraycopy(tmp_crlf, 0, line_data, srtbyte.length, 2);
                if( !ccg4_handle_line(line_data,srtbyte.length+2) ){
                    Log.d(TAG, "handle line err, please check");
                    return -1;
                }
            }
            is.close();
            send_end_pkg();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if( 11 ==  ack_start){
            CCG4_FW1_UPDATE_OK=true;
            Log.i(TAG, "ccg4 file1 xfer ok  fw2="+CCG4_FW2_UPDATE_OK);
        }else if( 12 == ack_start ){
            CCG4_FW2_UPDATE_OK=true;
            Log.i(TAG, "ccg4 file2 xfer ok  fw1="+CCG4_FW1_UPDATE_OK);
        }
        if( CCG4_FW1_UPDATE_OK && CCG4_FW2_UPDATE_OK ){
            return 0;
        }else if(CCG4_FW1_UPDATE_OK){
            return 11;
        }else if(CCG4_FW2_UPDATE_OK){
            return 12;
        }

        return -1;
    }
}
