package com.htc.chirp_fota_service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;



public class FotaService extends Service implements Usb.OnUsbChangeListener{
    private final static String TAG = "ChirpFota";

    private FotaServiceImpl fs;
    public Usb mUsb;


    private static final String PKG_NAME = "com.htc.vr.bledevice";
    private static final String CLS_NAME = "com.htc.vr.bledevice.BleFotaService";
    private static final ComponentName COMPONENT_NAME = new ComponentName(PKG_NAME, CLS_NAME);


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
                        //fs.updateImage(fs.curret_device);
                        for (int i = 0; i < 10; i++) {
                            mUsb.GetSysProperty(i);
                        }
                    }
                }).start();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }



}


