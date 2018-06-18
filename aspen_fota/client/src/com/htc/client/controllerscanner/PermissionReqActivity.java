package com.htc.client.controllerscanner;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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

import com.htc.client.BuildConfig;

public class PermissionReqActivity extends Activity {

    private String TAG = BuildConfig.PACKAGESIMPLENAME + PermissionReqActivity.class.getSimpleName();

    public static final String PERM_TYPE = "Permission_Type";
    public static final int PERM_TYPE_BT_ENABLE = 1;
    public static final int PERM_TYPE_LOCATION = 2;
    public static final int PERM_TYPE_LOCATION_ENABLE = 3;

    private static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_LOCATION_PERMISSION = 2;
    public static final int REQUEST_ENABLE_LOCATION = 3;
    private int mPermType = -1;

    private static final String PKG_NAME = "com.htc.miac.controllerutility";
    private static final String CLS_NAME = "com.htc.miac.controllerutility.controllerscanner.ScannerService";
    private static final ComponentName COMPONENT_NAME = new ComponentName(PKG_NAME, CLS_NAME);

    private ScannerService mScannerService;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.v(TAG, "onServiceConnected");
            mScannerService = ((ScannerService.IScannerService) iBinder).getLocalService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mScannerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.v(TAG, "onCreate");

        mPermType = getIntent().getIntExtra(PERM_TYPE, -1);
        Logger.v(TAG, "mPermType " + mPermType);

        Intent intent = new Intent();
        intent.setComponent(COMPONENT_NAME);
        if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Logger.w(TAG, "failed to bind to scanner service!");
            finish();
        }

        if (PERM_TYPE_BT_ENABLE == mPermType) {
            Logger.i(TAG, "BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (PERM_TYPE_LOCATION == mPermType) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Logger.d(TAG, "ACCESS_COARSE_LOCATION permission request.");
                    this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                }
            }
        }

        if (PERM_TYPE_LOCATION_ENABLE == mPermType) {
            Intent enableIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableIntent, REQUEST_ENABLE_LOCATION);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.v(TAG, "onResume");
    }

    private void closeSelf() {
        if (mScannerService != null) {
            unbindService(mConnection);
        }
        finish();
    }

    private void checkLocationEnable() {
        if (ScannerService.isLocationOpen(this)) {
            handler.post(new onPermResRunnable(PERM_TYPE_LOCATION_ENABLE, true));
            closeSelf();
        } else {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_ENABLE_LOCATION);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean res = false;

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    handler.post(new onPermResRunnable(PERM_TYPE_BT_ENABLE, true));
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Logger.d(TAG, "ACCESS_COARSE_LOCATION permission ungranted.");
                            this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                        }
                        else {
                            handler.post(new onPermResRunnable(PERM_TYPE_LOCATION, true));
                            checkLocationEnable();
                        }
                    }
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Logger.d(TAG, "BT not enabled");
                    handler.post(new onPermResRunnable(PERM_TYPE_BT_ENABLE, false));
                    closeSelf();
                }
                break;

            case REQUEST_ENABLE_LOCATION:
                if (ScannerService.isLocationOpen(this)) {
                    Logger.d(TAG, "location enabled");
                    res = true;
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Logger.d(TAG, "location enable fail");
                    res = false;
                }
                handler.post(new onPermResRunnable(PERM_TYPE_LOCATION_ENABLE, res));
                closeSelf();
                break;

            default:
                Logger.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION && permissions.length > 0) {
            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Logger.d(TAG, "location permission allowed");
                    handler.post(new onPermResRunnable(PERM_TYPE_LOCATION, true));
                    checkLocationEnable();
                } else {
                    Logger.d(TAG, "location permission denied");
                    handler.post(new onPermResRunnable(PERM_TYPE_LOCATION, false));
                    closeSelf();
                }
            }
        }
    }

    private final class onPermResRunnable implements Runnable {

        private int mType;
        private boolean mRes;
        public onPermResRunnable(int type, boolean res) {
            mType = type;
            mRes = res;
        }
        @Override
        public void run() {
            if (mScannerService != null) {
                mScannerService.onPermRequestResult(mType, mRes);
            }
        }
    }
}