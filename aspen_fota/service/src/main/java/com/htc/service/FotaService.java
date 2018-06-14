package com.htc.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.htc.chirp_fota.OnFirmwareUpdateListener;
import com.htc.service.dfu.faceplace_mcu;
import com.htc.service.usb.Usb;


public class FotaService extends Service implements Usb.OnUsbChangeListener{

    private FotaServiceImpl fs;
    private Usb mUsb;
    private static final String TAG=Const.G_TAG;
    private Thread ccg4_thread=null;
    private Thread facep_mcu_thread=null;

    private faceplace_mcu f_mcu =null;
    public OnFirmwareUpdateListener mUpdateListener = null;

    //public int curret_device  = -1;
    //public boolean DEVICE_STATE = false;

    @Override
    public void onCreate() {
        fs = new FotaServiceImpl(this);

        mUsb = new Usb(this,fs);
        mUsb.setOnUsbChangeListener(this);

        f_mcu = new faceplace_mcu(mUsb,this);


        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(Usb.HTC_ACTION_USB_PERMISSION));
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        setFirmwareUpdateListener(new myListener());

        ccg4_thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, "start updateFW");
                    mUsb.update_CCG4_and_show_dlg();
                } catch (Exception e) {
                    Log.d(TAG, ccg4_thread.getName()+" stop");
                    e.printStackTrace();
                }
            }
        });

        facep_mcu_thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, "faceplate sys update start");
                    f_mcu.update_sys();
                } catch (Exception e) {
                    Log.d(TAG, facep_mcu_thread.getName()+" stop");
                    e.printStackTrace();
                }
            }
        });

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
                if ( 1 == type ) { //cdc
                    ccg4_thread.start();
                }else if(2 == type ){ //dfu
                    facep_mcu_thread.start();
                }
            }else{
                boolean facep_alive =facep_mcu_thread.isAlive();
                boolean ccg_alive = ccg4_thread.isAlive();
                Log.i(TAG, "facep_alive="+facep_alive+" ccg_alive="+ccg_alive);
                if (ccg_alive) {
                    ccg4_thread.interrupt();
                }
                if (facep_alive) {
                    facep_mcu_thread.interrupt();
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void setFirmwareUpdateListener(OnFirmwareUpdateListener l) {
        mUpdateListener = l;
    }

    class myListener implements OnFirmwareUpdateListener{
        @Override
        public void onFirmwareUpdateStatusChanged(int device, int state, Bundle extra) throws RemoteException {
            Log.i(TAG, "onFirmwareUpdateStatusChanged");
        }
        @Override
        public void onFirmwareUpdateProgressChanged(int device, int progress) throws RemoteException {
            Log.i(TAG, "onFirmwareUpdateProgressChanged");
        }
        @Override
        public IBinder asBinder() {
            Log.i(TAG, "asBinder");
            return null;
        }
    }

}


