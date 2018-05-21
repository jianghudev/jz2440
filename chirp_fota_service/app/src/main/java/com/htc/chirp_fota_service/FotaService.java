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
    private UsbCdcTunnel mUsbCdcTunnel = null;
    public Usb mUsb;


    private static final String PKG_NAME = "com.htc.vr.bledevice";
    private static final String CLS_NAME = "com.htc.vr.bledevice.BleFotaService";
    private static final ComponentName COMPONENT_NAME = new ComponentName(PKG_NAME, CLS_NAME);


    //public int curret_device  = -1;
    //public boolean DEVICE_STATE = false;

    @Override
    public void onCreate() {
        fs = new FotaServiceImpl(this);

        mUsbCdcTunnel = new UsbCdcTunnel();
        mUsb = new Usb(this,fs);
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(Usb.HTC_ACTION_USB_PERMISSION));
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        mUsb.setOnUsbChangeListener(this);
        Log.i(TAG, "Fotaservice onCreate done.");
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
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String GetSysProperty(int item)
    {
        String RetString = null;
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'p';
        Data.send_array[1] = 0;
        Data.send_array[2] = (byte)item;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 10;
        // Log.d(TAG, " get: " + Arrays.toString(Data.send_array) + "(" + Data.send_array_count + "), recv count: " + Data.recv_array_count);

        if (mUsbCdcTunnel.RequestSingleCdcData(Data) == true) {
            try {
                RetString = new String(Data.recv_array, 0, Data.recv_array_count, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(RetString == null){
                Log.w(TAG, "RetString format is error!");
                return null;
            }
            RetString = RetString.replaceAll("[^[:print:]]", "");
        }
        return RetString;
    }

}


