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

import com.finchtechnologies.fota.IFotaListener;
import com.finchtechnologies.fota.IFotaService;

import java.util.concurrent.Executors;

/**
 * Created by hubin_jiang on 2018/6/18.
 */

public class AspenServiceModel {

    private static final String TAG = "FinchServiceModel";

    public static final String SERVICE_PACKAGE = "com.finchtechnologies.fota";
    private static final String SERVICE_CLASS = SERVICE_PACKAGE + ".FotaService";

    private IFotaService mIFinchFotaService;
    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    private myDelegate mDelegate;
    private boolean isBinded = false;

    private Context mContext;

    public AspenServiceModel(Context context) {
        mContext = context;
    }

    public boolean bindService() {
        Log.i(TAG, "bindService");
        boolean isBind = false;
        try {
            Intent intent = new Intent();
            intent.setClassName(SERVICE_PACKAGE, SERVICE_CLASS);
            isBind = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isBind;
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
            Log.d(TAG, "onServiceConnected");
            isBinded = true;
            mIFinchFotaService = IFotaService.Stub.asInterface(service);
            try {
                mIFinchFotaService.setDeviceListener(mFinchFotaListener);
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

    private IFotaListener mFinchFotaListener = new IFotaListener.Stub() {

        @Override
        public void onDeviceStatusChanged(final int state, final Bundle extra) throws RemoteException {
            Log.d(TAG, "state : " + state);
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    myDelegate delegate = getDelegate();
                    if (delegate != null) {
                        delegate.onDeviceStatusChanged(state, extra);
                    }
                }
            });
        }

        @Override
        public void onFirmwareUpdateProgressChanged(final int progress) throws RemoteException {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    myDelegate delegate = getDelegate();
                    if (delegate != null) {
                        delegate.onFirmwareUpdateProgressChanged(progress);
                    }
                }
            });
        }
    };

    @SuppressLint("StaticFieldLeak")
    public void setMacAddress(final String addr) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    mIFinchFotaService.setMacAddress(addr);
                } catch (Exception e) {
                    Log.e(TAG, "Exception on setMacAddress", e);
                }
                return null;
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1));
    }

    public void getDeviceInfo() {
        new AsyncTask<Void, Void, Bundle>() {

            @Override
            protected Bundle doInBackground(Void... voids) {
                try {
                    return mIFinchFotaService.getDeviceInfo();
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
            batteryLevel = mIFinchFotaService.getBatteryVoltageLevel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return batteryLevel;
    }

    public void upgradeFirmware(final Uri uri, final boolean isItFirstAttempt) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if (isItFirstAttempt) {
                        return mIFinchFotaService.upgradeFirmware(uri);
                    } else {
                        return mIFinchFotaService.upgradeFirmwareOnDfuTarg(uri);
                    }
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
