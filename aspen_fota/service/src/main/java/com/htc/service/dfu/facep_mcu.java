package com.htc.service.dfu;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.htc.service.Const;
import com.htc.service.usb.Usb;
import com.htc.service.usb.UsbTunnelData;

import java.io.File;

/**
 * Created by hubin_jiang on 2018/6/12.
 */

public class Facep_mcu {

    private static  String faceplace_dfu_file_name = "aspen_faceplate_v0.6_E.dfu";
    private File mDfuFile =null;
    private HtcDfu mDfu =null;

    private static final String TAG=Const.G_TAG;
    private Usb mUsb;
    private Context mContext;


    public Facep_mcu(Usb m_usb, Context ctxt) {
        this.mUsb = m_usb;
        mContext=ctxt;

        mDfu = new HtcDfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
        mDfu.setUsb(m_usb);

        get_dfu_file();
    }


    private int get_dfu_file() {
        mDfuFile = new File(mContext.getCacheDir(), faceplace_dfu_file_name);

        String path = mDfuFile.getAbsolutePath();
        Log.d(TAG,"dfufile="+path);
        if(!mDfuFile.exists()) {
            Log.e(TAG, "file not exist");
            return -1;
        }
        return 0;
    }


    public int update_sys()
    {
        Bundle updateInfo = new Bundle();
        boolean update = true;
        boolean moveImage;
        boolean verifyImage;
        boolean eraseResult1;
        boolean eraseResult2;
        boolean downloadImage;
        boolean analyzedfu;

        int process = 0;
        int i;
        try {


            //mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_UPDATING, updateInfo);
            int retryCnt = 3;
            do {
                eraseResult1 = false;
                eraseResult2 = false;
                downloadImage = false;
                verifyImage = false;
                moveImage = false;
                update = true;
                analyzedfu = false;

                Log.i(TAG, "start flash dfu file  " + mDfuFile.getName());
                //Erase MCU sector
                analyzedfu = mDfu.m_HtcDfuFile.AnalysisDfuFile(mDfuFile);

                Log.i(TAG, "Start Address: 0x" + Integer.toHexString(mDfu.m_HtcDfuFile.FirmWareStartAddress));

                if (!analyzedfu) {
                    Log.i(TAG, "analyze dfu fail");
                    break;
                }

            }while(false);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
//                    eraseResult1 = mDfu.EraseFotaSector(mDfu.m_HtcDfuFile.FirmWareStartAddress);
//                    if(!eraseResult1){
//                        Log.i(TAG, "Erase eraseResult1 again.");
//                        eraseResult1 = mDfu.EraseFotaSector(mDfu.m_HtcDfuFile.FirmWareStartAddress);
//                    }
//                    // erase system
//                    if(mDfu.m_HtcDfuFile.FirmwareSysHeader.indexOf("systrtos")>=0){
//                        Log.i(TAG, "Erase sysrtos eraseResult2.");
//                        eraseResult2 = mDfu.EraseFotaSector(0x08060000);
//                        boolean eraseResult3 = mDfu.EraseFotaSector(0x08080000);
//                        boolean eraseResult4 = mDfu.EraseFotaSector(0x08090000);
//                        boolean eraseResult5 = mDfu.EraseFotaSector(0x080C0000);
//                        boolean eraseResult6 = mDfu.EraseFotaSector(0x080E0000);
//                        if(!(eraseResult2 & eraseResult3 & eraseResult4 & eraseResult5 & eraseResult6)){
//                            Log.i(TAG, "Erase eraseResult2 again.");
//                            eraseResult2 = mDfu.EraseFotaSector(0x08060000);
//                            eraseResult3 = mDfu.EraseFotaSector(0x08080000);
//                            eraseResult4 = mDfu.EraseFotaSector(0x08090000);
//                            eraseResult5 = mDfu.EraseFotaSector(0x080C0000);
//                            eraseResult6 = mDfu.EraseFotaSector(0x080E0000);
//                        }
//                        if(!(eraseResult2 & eraseResult3 & eraseResult4 & eraseResult5 & eraseResult6)){
//                            eraseResult1= false;
//                        }
//                    }
//                    //erase bl1
//                    if(mDfu.m_HtcDfuFile.FirmwareBlHeader.indexOf("Link BL1")>=0){
//                        Log.i(TAG, "Erase bl1 eraseResult2.");
//                        eraseResult2 = mDfu.EraseFotaSector(0x0800C000);
//                        boolean eraseResult3 = mDfu.EraseFotaSector(0x08010000);
//                        if(!(eraseResult2 & eraseResult3)){
//                            Log.i(TAG, "Erase eraseResult2 again.");
//                            eraseResult2 = mDfu.EraseFotaSector(0x0800C000);
//                            eraseResult3 = mDfu.EraseFotaSector(0x08010000);
//                        }
//                        if(!(eraseResult2 & eraseResult3)){
//                            eraseResult1= false;
//                        }
//                    }
//                    //start upgrade
//                    if (eraseResult1) {
//                        Log.i(TAG, "Erase success.");
//
//                        Log.i(TAG, "Start UpgradeFotaImage.");
//                        downloadImage = mDfu.UpgradeFotaImage(writeDfuFiles.get(i));
//                        Log.i(TAG, "Start verify.");
//                        verifyImage = mDfu.LaunchVerify(writeDfuFiles.get(i));
//
//                        //count update process time
//                        process =  ( (i+1) * 100/ writeDfuFiles.size()) ;
////                        if(!(mDfu.m_HtcDfuFile.FirmwareHeader.indexOf("fw")>=0)) {
////                            mUpdateListener.onFirmwareUpdateProgressChanged(device, process);
////                            Log.i(TAG, "fota process =" + process);
////                        }
//                        //hmd ccg4 start move
//                        if(mDfu.m_HtcDfuFile.FirmwareCCG4Header.indexOf("FW2")>=0) {
//                            isMove = true;
//                            Log.i(TAG,"isMove = " + isMove);
//                            if (downloadImage && verifyImage) {
//                                Log.i(TAG, "verify success,start send move.");
//                                if (mbldfu && i == writeDfuFiles.size() - 1) {
//                                    //update = true;
//                                    moveImage = true;
//                                    Log.i(TAG, "bl.dfu verify success ,waiting restart ");
//
//                                } else {
//                                    moveImage = mDfu.moveImage();
//                                    if (moveImage) {
//                                        Log.i(TAG, "move true.");
////                                            updateInfo.putString("Pass", "A Dfu file is write pass!");
////                                            mUpdateListener.onFirmwareUpdateProgressChanged(device, process);
////                                            mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_UPDATING, updateInfo);
//                                    } else {
//                                        Log.i(TAG, "move fail.");
////                                            updateInfo.putString("Error", "A Dfu file is write fail!");
////                                            mUpdateListener.onFirmwareUpdateProgressChanged(device, process);
////                                            mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_ERROR, updateInfo);
//                                        update = false;
//                                        break;
//                                    }
//                                }
//                            } else {
//                                Log.i(TAG, "verify fail.");
//                                update = false;
//                            }
//                        }else{
//                            moveImage = true;
//                        }
////                            mUpdateListener.onFirmwareUpdateProgressChanged(device, process);
////                            Log.i(TAG, "fota process =" + process);
//                    }else {
//                        Log.i(TAG, "Erase fail.");
//                        update = false;
//                    }
//                    if (eraseResult1&& downloadImage && verifyImage && moveImage) {
//                        retryCnt = 0;
//                    } else {
//                        update = false;
//                        retryCnt--;
//                    }
//                    Log.i(TAG, "retryCnt = " + retryCnt);
//                }while(retryCnt>0);
//                if(update == false){
//                    Log.d(TAG,"update fail, update = " + update);
//                    updateInfo.putString("Error", "A Dfu file is write fail!");
//                    mUpdateListener.onFirmwareUpdateProgressChanged(device, process);
//                    mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_ERROR, updateInfo);
//                    break;
//                }else {
//                    updateInfo.putString("Pass", "A Dfu file is write pass!");
//                    mUpdateListener.onFirmwareUpdateProgressChanged(device, process);
//                    Log.i(TAG, "fota process =" + process);
//                }
//
//
//            Log.i(TAG,"exit for.");
//            if (i == writeDfuFiles.size()&&update) {
//                Log.i(TAG, "upgrade success.");
//                updateInfo.putString("Pass", "upgrade success!");
//                mUpdateListener.onFirmwareUpdateProgressChanged(device, 100);
//                mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_COMPLETED, updateInfo);
//                //reboot device to normal
//                long endTime = SystemClock.elapsedRealtime();
//                Log.d(TAG,"upgrade time " + (endTime - startTime) + " ms");
//                Log.i(TAG,"leave dfu mode , restart to normal mode. ");
//                mDfu.leaveDfuMode(0x08000000);
//                isMove = false;
//                mbDuringUpdating = false;
//            }else{
//                Log.i(TAG, "Fota service had tried three times ,but upgrade fail.");
//                mbDuringUpdating = false;
//                updateInfo.putString("Error", "Fota service had tried three times ,but upgrade fail.");
//                mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_ERROR, updateInfo);
//            }

//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return update;



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
