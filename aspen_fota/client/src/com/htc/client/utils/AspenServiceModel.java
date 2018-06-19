package com.htc.client.utils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.htc.service.IDeviceConnectedListener;
import com.htc.service.OnFirmwareUpdateListener;
import com.htc.service.IFotaService;

import java.util.concurrent.Executors;


/**
 * Created by hubin_jiang on 2018/6/18.
 */

public class AspenServiceModel {

    private static final String TAG = "AspenFota.client";

    public static final String SERVICE_PACKAGE = "com.htc.service";
    private static final String SERVICE_CLASS = SERVICE_PACKAGE + ".FotaService";

    private IFotaService mAspenService;
    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    private myDelegate mDelegate;
    private boolean isBinded = false;

    private Context mContext;

    public AspenServiceModel(Context context) {
        mContext = context;
    }

    public boolean bind_FotaService() {
        try {
            Log.i(TAG, "__jh__ aidl bind_FotaService");
            Intent intent = new Intent();
            intent.setClassName(SERVICE_PACKAGE, SERVICE_CLASS);
            boolean isBind = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "__jh__ isBind="+isBind);
            return isBind;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void unbindService() {
        Log.d(TAG, "unbindService");
        mContext.unbindService(mConnection);
        isBinded = false;
    }

    public void setDelegate(myDelegate delegate) {
        mDelegate = delegate;
    }

    public myDelegate getDelegate() {
        return mDelegate;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "__jh__ onServiceConnected");
            isBinded = true;
            mAspenService = IFotaService.Stub.asInterface(service);
            try {
                mAspenService.setFirmwareUpdateListener(mFinchFotaListener);

                mAspenService.setDeviceConnectedListener(mAspenConnectLister);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            myDelegate delegate = getDelegate();
            if (delegate != null) {
                delegate.onServiceConnected();
            } else {
                Log.w(TAG, "[onServiceConnected] delegate is null !");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    private IDeviceConnectedListener mAspenConnectLister =new IDeviceConnectedListener.Stub(){
        @Override
        public void onConnectedStateStatusChanged(int device, boolean isConnected, int usb_state) throws RemoteException {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    myDelegate delegate = getDelegate();
                    if (delegate != null) {
                        //delegate.onServiceConnected();   //// todo
                    }
                }
            });
        }
    };

    private OnFirmwareUpdateListener mFinchFotaListener = new OnFirmwareUpdateListener.Stub() {
        @Override
        public void onFirmwareUpdateStatusChanged(int device, int state, Bundle extra) throws RemoteException {
            Log.d(TAG, "state : " + state);
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    myDelegate delegate = getDelegate();
                    if (delegate != null) {
                        //delegate.onDeviceStatusChanged(state, extra);   //// todo
                    }
                }
            });
        }
        @Override
        public void onFirmwareUpdateProgressChanged(int device, final int progress) throws RemoteException {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    myDelegate delegate = getDelegate();
                    if (delegate != null) {
                        delegate.onFirmwareUpdateProgressChanged(progress);  //// todo
                    }
                }
            });
        }
    };


    @SuppressLint("StaticFieldLeak")
    public void getDeviceInfo() {
        new AsyncTask<Void, Void, Bundle>() {

            @Override
            protected Bundle doInBackground(Void... voids) {
                try {
                    return mAspenService.getDeviceInfo(FotaServiceContract.TYPE_DEVICE_HMD); //TODO
                } catch (Exception e) {
                    Log.e(TAG, "Exception on getDeviceInfo", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bundle info) {
                myDelegate delegate = getDelegate();
                if (delegate != null) {
                    if (info != null) {
                        delegate.onDeviceInfoGet(info);
                    } else {
                        delegate.onFotaError();
                    }
                } else {
                    Log.e(TAG, "delegate is null");
                }
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1));
    }

    public int getBatteryVoltageLevel() {
        int batteryLevel = -1;
        try {
            batteryLevel = mAspenService.getBatteryVoltageLevel(FotaServiceContract.TYPE_DEVICE_HMD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return batteryLevel;
    }



    @SuppressLint("StaticFieldLeak")
    public void upgradeFirmware(final Uri uri, final boolean isItFirstAttempt) {
         new  AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    Log.e(TAG, "start AsyncTask upgradeFirmware");
                    return mAspenService.upgradeFirmware(FotaServiceContract.TYPE_DEVICE_HMD  , uri);
                } catch (Exception e) {
                    Log.e(TAG, "Exception on upgradeFirmware", e);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) {
                    myDelegate delegate = getDelegate();
                    if (delegate != null) {
                        delegate.onFotaError();
                    }
                }
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1));
    }

    public boolean isBinded() {
        return isBinded;
    }
}
