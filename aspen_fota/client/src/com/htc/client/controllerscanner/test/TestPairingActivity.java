package com.htc.client.controllerscanner.test;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.htc.client.R;
import com.htc.client.controllerscanner.Logger;
import com.htc.client.controllerscanner.ScannerService;
import com.htc.client.service.FotaUpdateService;
import com.htc.client.utils.FirmwareUpdateUtils;
import com.htc.client.vr.BleDevInfo;
import com.htc.client.vr.BleDev;
import com.htc.client.vr.IScannerListener;

import java.util.List;

/**
 * Created by chihhang_chuang on 2017/10/24.
 */

public class TestPairingActivity extends Activity {
    private String TAG = "AspenFota.client";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScannerService mScannerService;
    private ScannerService.IScannerService mIScannerService;
    private TextView mTextView;
    private boolean mBound = false;

    private FotaUpdateService mFotaUpdateService;

    private static final int REQUEST_PERMISSION = 2909;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_pairing_activity);
        Logger.d(TAG, "onCreate");

        mTextView = (TextView) findViewById(R.id.textView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                //bindFotaUpdateService();
            }
        } else {
            //bindFotaUpdateService();
        }

        findViewById(R.id.bind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBound) {
                    Logger.d(TAG, "call bindService");
                    bindService(new Intent(TestPairingActivity.this, ScannerService.class), mConnection, Context.BIND_AUTO_CREATE);
                }
            }
        });

        findViewById(R.id.startScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound) {
                    Logger.d(TAG, "startScan Clicked");
                    try {
                        mIScannerService.start();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        findViewById(R.id.stopScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound) {
                    Logger.d(TAG, "stopScan Clicked");
                    try {
                        mIScannerService.stop();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        findViewById(R.id.bind_fota_update_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindFotaUpdateService();
            }
        });

    }

    private void bindFotaUpdateService() {
        Intent intent = new Intent(this, FotaUpdateService.class);
        bindService(intent, mFotaUpdateConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission Granted");
                    //bindFotaUpdateService()
                } else {
                    Log.i(TAG, "Permission Denied");
                }
                return;
            }
        }
    }

    private ServiceConnection mFotaUpdateConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(TAG, "FotaUpdateService Connected");
            mFotaUpdateService = ((FotaUpdateService.FotaUpdateBinder) iBinder).getService();
            final BleDevInfo deviceInfo = new BleDevInfo("CF:6E:24:8B:FB:E1", "Finch Dash E1FB", "", "", "");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        mFotaUpdateService.StartFotaUpdate(mFotaUpdateListener, deviceInfo, false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(TAG, "FotaUpdateService Disconnected");
            mFotaUpdateService = null;
        }
    };

    private FirmwareUpdateUtils.CheckFotaUpdateListener mFotaUpdateListener = new FirmwareUpdateUtils.CheckFotaUpdateListener() {

        @Override
        public void onCheckFotaUpdateResult(boolean haveUpdate, BleDevInfo deviceInfo) {

        }

        @Override
        public void onFotaUpdateCompleted(BleDevInfo deviceInfo) {

        }

        @Override
        public void onStatusChanged(int status, BleDevInfo deviceInfo) {

        }

        @Override
        public void onProgressChanged(int progress) {

        }
    };

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "onDestroy");
        if (mBound) {
            Logger.d(TAG, "call unbindService");
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(TAG, "onServiceConnected");
            mScannerService = ((ScannerService.IScannerService) iBinder).getLocalService();
            mIScannerService = (ScannerService.IScannerService) iBinder;
            try {
                mIScannerService.registerListener(null, mIScannerListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(TAG, "onServiceDisconnected");
            mScannerService = null;
            mBound = false;
        }
    };

    private IScannerListener mIScannerListener = new IScannerListener() {
        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void onScanStarted() throws RemoteException {
            Logger.d(TAG, "onScanStarted()");
        }

        @Override
        public void onScanCompleted() throws RemoteException {
            Logger.d(TAG, "onScanCompleted()");
        }

        @Override
        public void onScanResult(BleDev device) throws RemoteException {
            Logger.d(TAG, "onScanResult()");
            Logger.d(TAG, "BleDev: " + device.mName + "@" + device.mAddr);
            setText("BleDev: " + device.mName + "@" + device.mAddr);
        }

        @Override
        public void onBatchScanResults(List<BleDev> devices) throws RemoteException {
            Logger.d(TAG, "onBatchScanResults()");
        }

        @Override
        public boolean isConnected(String mac) throws RemoteException {
            return false;
        }

        @Override
        public BleDevInfo getDeviceInfo(String mac) throws RemoteException {
            return null;
        }
    };

    private void setText (final String string) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(string);
            }
        });
    }
}
