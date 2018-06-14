package com.htc.service.dfu;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.htc.service.Const;
import com.htc.service.FotaService;
import com.htc.service.usb.Usb;
import com.htc.service.usb.UsbTunnelData;

import java.io.File;

/**
 * Created by hubin_jiang on 2018/6/12.
 */

public class faceplace_mcu {

    private static  String faceplace_dfu_file_name = "aspen_faceplate_v0.6_E.dfu";
    private File mDfuFile =null;
    private HtcDfu mDfu =null;
    private FotaService mService =null;

    private static final String TAG=Const.G_TAG;
    private Usb mUsb;


    public final static int DEVICE_HMD  = 0;
    public final static int DEVICE_CONTROLLER  = 1;

    public final static int STATE_START = 0;
    public final static int STATE_UPDATING = 1;
    public final static int STATE_ERROR = 2;
    public final static int STATE_COMPLETED = 3;


    //private int[] addr_sys_test = {0x8000000};

    private int[] addr_sys = {
            0x8000000, 0x8000800, 0x8001000, 0x8001800, 0x8002000, 0x8002800, 0x8003000, 0x8003800,
            0x8004000, 0x8004800, 0x8005000, 0x8005800, 0x8006000, 0x8006800, 0x8007000, 0x8007800,
            0x8008000, 0x8008800, 0x8009000, 0x8009800, 0x800A000, 0x800A800, 0x800B000, 0x800B800,
            0x800C000, 0x800C800, 0x800D000, 0x800D800, 0x800E000, 0x800E800, 0x800F000, 0x800F800,

            0x8010000, 0x8010800, 0x8011000, 0x8011800, 0x8012000, 0x8012800, 0x8013000, 0x8013800,
            0x8014000, 0x8014800, 0x8015000, 0x8015800, 0x8016000, 0x8016800, 0x8017000, 0x8017800,
            0x8018000, 0x8018800, 0x8019000, 0x8019800, 0x801A000, 0x801A800, 0x801B000, 0x801B800,
            0x801C000, 0x801C800, 0x801D000, 0x801D800, 0x801E000, 0x801E800, 0x801F000, 0x801F800,
    };

    public faceplace_mcu(Usb m_usb , FotaService svr) {
        this.mUsb = m_usb;
        mService=svr;

        mDfu = new HtcDfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
        mDfu.setUsb(m_usb);

        get_dfu_file();
    }


    private int get_dfu_file() {
        mDfuFile = new File(mService.getCacheDir(), faceplace_dfu_file_name);

        String path = mDfuFile.getAbsolutePath();
        Log.d(TAG,"dfufile="+path);
        if(!mDfuFile.exists()) {
            Log.e(TAG, "file not exist");
            return -1;
        }
        return 0;
    }

    public int erase_facep_sys() {
        long startEraseTime = System.currentTimeMillis();
        for (int i = 0; i < addr_sys.length; i++) {
            boolean ret = mDfu.EraseFotaSector(addr_sys[i]);
            if(!ret){
                Log.i(TAG, "erease facep sys err!");
                return -1;
            }
        }
        long total_time = System.currentTimeMillis() -startEraseTime;
        Log.i(TAG,"=========> Erase all sector success.time="+total_time+"ms <=========");
        return 1;
    }



    public boolean update_sys(){

        Bundle updateInfo = new Bundle();
        boolean updateOK = false;
        boolean verifyImage=false;
        int eraseResult=0;
        boolean flushImage =false;
        boolean analyzedfu;
        long startTime = SystemClock.elapsedRealtime();
        int process = 0;
        try {
            mService.mUpdateListener.onFirmwareUpdateStatusChanged(DEVICE_HMD, STATE_UPDATING, updateInfo);
            int retryCnt = 4;
            while(retryCnt-- >0){
                Log.i(TAG, "start flash dfu file  " + mDfuFile.getName());
                //Erase MCU sector
                analyzedfu = mDfu.m_HtcDfuFile.AnalysisDfuFile(mDfuFile);
                if (!analyzedfu) {
                    Log.i(TAG, "analyze dfu fail");
                    break;
                }
                eraseResult = erase_facep_sys();
                //start upgrade
                if ( 1 == eraseResult) {
                    Log.i(TAG, "Start UpgradeFotaImage.");
                    flushImage = mDfu.UpgradeFotaImage(mDfuFile);
                    Log.i(TAG, "Start verify.");
                    verifyImage = mDfu.LaunchVerify(mDfuFile);
                    if ( flushImage && verifyImage ) {
                        updateOK = true;
                        break;
                    }
                }
                Log.i(TAG, "retryCnt = " + retryCnt);
            }

            if (updateOK) {
                Log.i(TAG, "upgrade success.");
                updateInfo.putString("Pass", "upgrade success!");
                mService.mUpdateListener.onFirmwareUpdateProgressChanged(DEVICE_HMD, 100);
                mService.mUpdateListener.onFirmwareUpdateStatusChanged(DEVICE_HMD, STATE_COMPLETED, updateInfo);
                //reboot device to normal
                long endTime = SystemClock.elapsedRealtime();
                Log.d(TAG,"upgrade time " + (endTime - startTime) + " ms");
                Log.i(TAG,"leave dfu mode , restart to normal mode. ");
                mDfu.leaveDfuMode(0x08000000);
            }else{
                Log.i(TAG, "Fota service had tried three times ,but upgrade fail.");
                updateInfo.putString("Error", "Fota service had tried three times ,but upgrade fail.");
                mService.mUpdateListener.onFirmwareUpdateStatusChanged(DEVICE_HMD, STATE_ERROR, updateInfo);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return updateOK;

    }


    public int enter_dfu_mode(){
        int retryCount = 5;
        try {
            long file_size=mDfuFile.length();
            //ccg4_version =get_ccg4_ver(ccgfile);

            UsbTunnelData Data = new UsbTunnelData();
            Data.send_array[0] = Const.CMD_FOTA_START;  //cmd
            Data.send_array[1] = 21;   //len
            Data.send_array[2] = Const.FOTA_TYPE_FACEP_SYS;

            String dfu_version= "faceplace_dfu";
            byte[] tmp_ver= dfu_version.getBytes("UTF-8");
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
}
