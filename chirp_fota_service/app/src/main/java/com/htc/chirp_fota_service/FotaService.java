package com.htc.chirp_fota_service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class FotaService extends Service implements Usb.OnUsbChangeListener{
    private final static String TAG = "ChirpFota";

    private FotaServiceImpl fs;
    public Usb mUsb;




    private static final String CCG4_FM1_NAME = "htc_apn_4225_v14_1.cyacd";
    private static final String CCG4_FM2_NAME = "htc_apn_4225_v14_2.cyacd";



    //public int curret_device  = -1;
    //public boolean DEVICE_STATE = false;

    @Override
    public void onCreate() {
        fs = new FotaServiceImpl(this);

        mUsb = new Usb(this,fs);
        mUsb.setOnUsbChangeListener(this);


        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(Usb.HTC_ACTION_USB_PERMISSION));
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        Log.d(TAG, "__jh__ onCreate done.");
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(mUsb.getmUsbReceiver());
        mUsb.release();
        Log.d(TAG, "Fotaservice onDestroy.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Fotaservice onBinder.");
        return fs;
    }

    public boolean onUnbind(Intent intent) {
        Log.i(TAG, " enter onunbind ");
//        fs.mUsb.release();
        return super.onUnbind(intent);
    }

    @Override
    public void on_Connected(int device, boolean isConnected, int type){
        try{
            if(fs.mDeviceConnectedListener != null) {
                fs.mDeviceConnectedListener.onConnectedStateStatusChanged(fs.curret_device, fs.DEVICE_STATE, Usb.USB_STATE);
            }
            if(isConnected){
                new Thread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "start updateFW");
                        updateCCG4();
                    }
                }).start();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int updateCCG4(){
        try {
            int line_num=0;

            File dir = Environment.getDataDirectory();
            File file = new File(dir, CCG4_FM1_NAME);

            String path = file.getAbsolutePath();
            String name = file.getName();
            Log.d(TAG,"path="+path+" name="+name);
            if(file.exists()){
                InputStream is = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    byte[] srtbyte = line.getBytes("UTF-8"); //no including \n  ,we must add it
                    ccg4_handle_line(srtbyte,srtbyte.length);
                    for (int i = 0; i < srtbyte.length; i++) {
                        String tmp=Integer.toHexString(srtbyte[i] & 0xFF);
                        Log.d(TAG, tmp +" ");

                    }
                    if(++line_num >= 2 ){
                        Log.d(TAG, " ccg4_handle_line end");
                        break;
                    }
                }

                is.close();
            }else{
                Log.e(TAG, "file not exist");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    private int ccg4_handle_line(byte[] data, int len) {

        return 0;
    }

}


