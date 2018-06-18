package com.htc.miac.controllerutility.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.htc.miac.controllerutility.controllerscanner.Logger;
import com.htc.miac.controllerutility.utils.AspenServiceModel;
import com.htc.miac.controllerutility.utils.FirmwareUpdateUtils;
import com.htc.miac.controllerutility.utils.FirmwareUpdateUtils.CheckFotaUpdateListener;
import com.htc.miac.controllerutility.utils.FotaServiceContract;
import com.htc.miac.controllerutility.utils.SharedPrefManager;
import com.htc.miac.controllerutility.utils.myDelegate;
import com.htc.vr.controllerscanner.BleDevInfo;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;

/**
 * Created by hugh_chen on 2017/10/24.
 */

public class FotaUpdateService extends Service {
    private static final String TAG = "AspenFota.client";

    private FotaUpdateBinder mBinder = new FotaUpdateBinder();
    private AspenServiceModel mAspenModel;
    private FirmwareUpdateUtils mUtilsInstance;

    private Context mContext;
    private CheckFotaUpdateListener mCheckFotaUpdateListener;
    private BleDevInfo mBleDevInfo;

    private Handler mFotaUpdateMessenger = new Handler(new FotaUpdateHandler());
    private Handler mFotaTimeoutHandler = new Handler(new FotaTimeoutHandler());

    private final int MSG_ACTION_DOWNLOAD_FIRMWARE = 1;
    private final int MSG_ACTION_FOTA_UPDATE = 2;

    private final int BATTERY_LEVEL = 30;
    private static final long FOTA_UPDATE_TIME_OUT = 1000 * 60 * 5;
    private static final long SET_ADDRESS_TIME_OUT = 1000 * 60 * 1;

    private String mDownloadUrl = "";
    private String mDownloadPath = "";

    private Queue<Integer> mActionQueue = new LinkedList<Integer>();

    private static final int NO_ACTION = 0;
    private static final int ACTION_REQUEST_DEVICE_INFO = 1111;
    private static final int ACTION_START_FOTA_FOLLOW = 1112;
    private static final int ACTION_REQUEST_DEVICE_INFO_ERROR_HANDLE = 1113;
    private static final int ACTION_TIMEOUT = 1114;
    private static final int MSG_FOTA_UPDATE_TIME_OUT = 3379;
    private static final int MSG_FOTA_SET_ADDRESS_TIME_OUT = 3380;

    private static final boolean TEST_CASE = false;

    private int mErrorCount = 0;
    private int RETRY_COUNT = 1;

    private SharedPrefManager mSharedPrefManager;

    private boolean mIsRetry = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "[FotaUpdateService] onBind");
        mContext = getApplicationContext();
        mSharedPrefManager = new SharedPrefManager(mContext);
        initAspenModel();
        mAspenModel.bindService();
        mUtilsInstance = new FirmwareUpdateUtils(mContext);
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        try {
            if (mAspenModel != null) {
                mAspenModel.setDelegate(null);
                mAspenModel.unbindService();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (!mActionQueue.isEmpty()) {
                mActionQueue.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mFotaTimeoutHandler.removeMessages(MSG_FOTA_UPDATE_TIME_OUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAspenModel() {
        mAspenModel = new AspenServiceModel(mContext);
        AspenDelegate delegate = new AspenDelegate();
        mAspenModel.setDelegate(delegate);
    }

    public class FotaUpdateBinder extends Binder {
        public FotaUpdateService getService() {
            return FotaUpdateService.this;
        }
    }

    public void StartFotaUpdate(CheckFotaUpdateListener listener, BleDevInfo deviceInfo, boolean isRetry) {
        if (TEST_CASE) {
            mCheckFotaUpdateListener = listener;
            mBleDevInfo = deviceInfo;
            testCase();
        } else {
            mIsRetry = isRetry;
            checkFotaUpdate(listener, deviceInfo, true);
        }
    }


    public void checkFotaUpdate(CheckFotaUpdateListener listener, BleDevInfo deviceInfo) {
        checkFotaUpdate(listener, deviceInfo, true);
    }

    private void checkFotaUpdate(CheckFotaUpdateListener listener, BleDevInfo deviceInfo, boolean isCheckFota) {
        Log.d(TAG, "[checkFotaUpdate] : " + isCheckFota);
        mCheckFotaUpdateListener = listener;
        mBleDevInfo = deviceInfo;
        if (isCheckFota) {
            checkFotaUpdate(deviceInfo, isCheckFota);
        } else {
            if (deviceInfo != null && !TextUtils.isEmpty(deviceInfo.mAddr)) {
                synchronized (mActionQueue) {
                    if (mActionQueue.isEmpty()) {
                        if (mAspenModel.isBinded()) {
                            setMacAddress(deviceInfo.mAddr);
                        } else {
                            mActionQueue.add(ACTION_REQUEST_DEVICE_INFO_ERROR_HANDLE);
                        }
                    } else {
                        Log.d(TAG, "Action queue is not empty, is doing FOTA !");
                    }
                }
            } else {
                Log.d(TAG, "[checkFotaUpdate] address is null !");
                sendError(FotaServiceContract.STATE_FOTA_ERROR);
            }

        }
    }

    private void checkFotaUpdate(BleDevInfo deviceInfo, boolean isCheckFota) {
        Bundle info = new Bundle();
        CheckFirmwareUpdateTask checkUpdateTask;

        String mac = "";
        if (deviceInfo.mAddr.contains(":")) {
            mac = deviceInfo.mAddr.replace(":", "");
        }

        info.putString(FotaServiceContract.DEVICE_VERSION, deviceInfo.mFwVersion);
        info.putString(FotaServiceContract.DEVICE_SERIAL_NAME, mac);
        info.putString(FotaServiceContract.DEVICE_MODEL_NAME, deviceInfo.mModuleName);

        checkUpdateTask = new CheckFirmwareUpdateTask(info, isCheckFota);
        checkUpdateTask.executeOnExecutor(Executors.newFixedThreadPool(1));

        if (mSharedPrefManager != null) {
            mSharedPrefManager.setFinchModelNumber(deviceInfo.mModuleName);
            mSharedPrefManager.setFinchVersion(deviceInfo.mFwVersion);
        } else {
            Logger.w(TAG, "[checkFotaUpdate] mSharedPrefManager is null !");
        }
    }

    private class CheckFirmwareUpdateTask extends AsyncTask<Void, Void, Pair<Boolean, String>> {

        private Bundle mInfo;
        private FirmwareUpdateUtils mInstance;
        private boolean mIsCheckFota = true;

        public CheckFirmwareUpdateTask(Bundle info, boolean isCheckFota) {
            mInfo = info;
            mInstance = new FirmwareUpdateUtils(mContext);
            mIsCheckFota = isCheckFota;
        }

        @Override
        protected Pair<Boolean, String> doInBackground(Void... params) {
            Pair<Boolean, String> pair = Pair.create(false, "");
            try {
                String request = mInstance.createCheckinJSON(true, mInfo);
                String resp = "";
                String checkinUrl = FirmwareUpdateUtils.CHECKIN_URL;
                resp = mInstance.doRequest(checkinUrl, request);
                pair = mInstance.handleReply(new JSONObject(resp));
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
            return pair;
        }

        @Override
        protected void onPostExecute(Pair<Boolean, String> result) {
            super.onPostExecute(result);
            boolean hasFOTA = false;
            if (result.first) {
                String downloadUrl = result.second;
                if (!TextUtils.isEmpty(downloadUrl)) {
                    hasFOTA = true;
                    mDownloadUrl = downloadUrl;
                    mUtilsInstance.setFirmwareStatus(true);
                    mUtilsInstance.setHaveFirmwareUpdate(true);
                } else {
                    Log.w(TAG, "download url is null or empty !");
                    mUtilsInstance.setFirmwareStatus(false);
                    mUtilsInstance.setHaveFirmwareUpdate(false);
                }
            } else {
                mUtilsInstance.setFirmwareStatus(false);
                mUtilsInstance.setHaveFirmwareUpdate(false);
            }
            if (mCheckFotaUpdateListener != null) {
                mCheckFotaUpdateListener.onCheckFotaUpdateResult(hasFOTA, mBleDevInfo);
            } else {
                Log.w(TAG, "mCheckFotaUpdateListener is null !");
            }
            Log.d(TAG, "__jh__ mIsCheckFota="+mIsCheckFota+" hasFOTA="+hasFOTA);
            if (hasFOTA) {
                mFotaUpdateMessenger.sendEmptyMessage(MSG_ACTION_DOWNLOAD_FIRMWARE);
            } else {
                removeActionQueue();
                if (mCheckFotaUpdateListener != null) {
                    mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_NO_FOTA_UPDATE, mBleDevInfo);
                } else {
                    Log.d(TAG, "[CheckFirmwareUpdateTask] mCheckFotaUpdateListener is null");
                }
            }
        }
    }

    class FotaUpdateHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "FotaUpdateHandler MSG = " + msg.what);
            switch (msg.what) {
                case MSG_ACTION_DOWNLOAD_FIRMWARE:
                    Log.d(TAG, "MSG_ACTION_DOWNLOAD_FIRMWARE");
                    startDownload();
                    break;
                case MSG_ACTION_FOTA_UPDATE:
                    Log.d(TAG, "MSG_ACTION_FOTA_UPDATE");
                    startFotaUpdate();
                    break;
            }
            return false;
        }
    }

    private void startDownload() {
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.executeOnExecutor(Executors.newFixedThreadPool(1));
    }

    private class DownloadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (!TextUtils.isEmpty(mDownloadUrl)) {
                downloadFOTAUpdate(mDownloadUrl);
            } else {
                sendDownloadError(FirmwareUpdateUtils.DOWNLOAD_FAIL_MSG_ERROR_CODE_3);
                Log.e(TAG, "[DownloadTask] Download url is null !");
            }
            return null;
        }

        private void downloadFOTAUpdate(String downloadUrl) {
            int TIME_OUT = 30000;

            File firmwarePath = new File(mContext.getFilesDir(), FotaServiceContract.FILE_FOLDER_NAME);
            //Log.d(TAG, "[downloadFOTAUpdate] firmwarePath name : " + firmwarePath);
            File updateFile = new File(firmwarePath, FotaServiceContract.FILE_NAME);
            //Log.d(TAG, "[downloadFOTAUpdate] updateFile name : " + updateFile.getPath());
            //Log.d(TAG, "[downloadFOTAUpdate] download url : " + downloadUrl);

            InputStream is = null;
            FileOutputStream out = null;

            try {
                URLConnection conn = null;
                URL fotaUpdateUrl = new URI(downloadUrl).toURL();

                if (!firmwarePath.exists()) {
                    firmwarePath.mkdir();
                }

                if (!updateFile.exists()) {
                    if (!updateFile.createNewFile()) {
                        Log.d(TAG, "can't create new file");
                        //delete download file.
                        sendDownloadError(FirmwareUpdateUtils.DOWNLOAD_FAIL_MSG_ERROR_CODE_2);
                        deleteDownloadFile(updateFile);
                        return;
                    }
                }

                conn = fotaUpdateUrl.openConnection();
                conn.setConnectTimeout(TIME_OUT);
                conn.setReadTimeout(TIME_OUT);
                conn.setUseCaches(true);
                conn.connect();

                if (mCheckFotaUpdateListener != null) {
                    //Show UI
                    mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_FOTA_DOWNLOAD_START, mBleDevInfo);
                } else {
                    Log.w(TAG, "[downloadFOTAUpdate] mCheckFotaUpdateListener is null");
                }

                float fileSize = 0;

                try {
                    float tempSize = mUtilsInstance.getFirmwareSize();
                    fileSize = tempSize * 1024 * 1024;
                    Log.d(TAG, "__jh__ download fileSize1="+fileSize);
                } catch (Exception e) {
                    fileSize = conn.getContentLength();
                    Log.d(TAG, "__jh__ download fileSize2="+fileSize);
                    e.printStackTrace();
                }

                Log.d(TAG, "fileSize : " + fileSize);

                is = conn.getInputStream();
                out = new FileOutputStream(updateFile);

                final byte[] buf = new byte[8192];

                int read_size = -1;
                int size = 0;
                float count = 0;
                int number = 0;

                while ((read_size = is.read(buf)) >= 0) {
                    size = size + read_size;
                    out.write(buf, 0, read_size);

                    count = (float)size / fileSize;
                    number = Math.round(count * 100);

                    if (mCheckFotaUpdateListener != null) {
                        if (number > 100) {
                            number = 100;
                        }
                        mCheckFotaUpdateListener.onProgressChanged(number);
                    } else {
                        Log.w(TAG, "[downloadFOTAUpdate] mCheckFotaUpdateListener is null !");
                    }
                }
                out.flush();

                sendMSG(FirmwareUpdateUtils.VERIFY_SUCCESS_MSG, "");
            } catch (Exception e) {
                e.printStackTrace();
                sendDownloadError(FirmwareUpdateUtils.DOWNLOAD_FAIL_MSG_ERROR_CODE_1);
                deleteDownloadFile(updateFile);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Close FileOutputStream and InputStream fail !");
                    Log.e(TAG, "", e);
                }
            }
            if (updateFile.exists()) {
                int file_size = Integer.parseInt(String.valueOf(updateFile.length()) );
                mDownloadPath = updateFile.getPath();
                Log.d(TAG, "__jh__ download file size=" + file_size);
                mFotaUpdateMessenger.sendEmptyMessage(MSG_ACTION_FOTA_UPDATE);
            } else {
                sendDownloadError(FirmwareUpdateUtils.DOWNLOAD_FAIL_MSG_ERROR_CODE_3);
            }
        }

        private void deleteDownloadFile(File updateFile) {
            if (updateFile.exists()) {
                try {
                    boolean isSuccess = updateFile.delete();
                    Log.d(TAG, "Delete the file : " + isSuccess);
                } catch (Exception deleteException) {
                    Log.e(TAG, "", deleteException);
                }
            }
        }
    }

    private void startFotaUpdate() {
        int level = mAspenModel.getBatteryVoltageLevel();
        Log.d(TAG, "[startFotaUpdate] battery level : " + level);
        if (level == -1) {
            sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_2);
        } else {
            if (level > BATTERY_LEVEL) {
                mErrorCount = 0;
                if (mIsRetry) {
                    mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_FOTA_START, mBleDevInfo);
                    sendFirmware(false);
                } else {
                    sendFirmware(true);
                }
            } else {
                sendLowBatteryError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_2);
            }
        }
    }

    private void sendFirmware(boolean isItFirstAttempt) {
        mFotaTimeoutHandler.sendEmptyMessageDelayed(MSG_FOTA_UPDATE_TIME_OUT, FOTA_UPDATE_TIME_OUT);
        try {
            Uri uri = getFileUri();
            Log.d(TAG, "[sendFirmware] uri : " + uri.toString() + ", is retry : " + mIsRetry + ", isItFirstAttempt : " + isItFirstAttempt);
            grantUriPermission(AspenServiceModel.SERVICE_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setDFUMacAddress(mBleDevInfo.mAddr);
            mAspenModel.upgradeFirmware(uri, isItFirstAttempt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri getFileUri() throws IOException {
        String path = mDownloadPath;
        //Log.d(TAG, "[getFileUri] path : " + path);
        File firmwareFile = new File(path);
        Uri uri = Uri.parse(firmwareFile.toString());
        //Log.d(TAG, "[getFileUri] uri : " + uri.getPath());

        if (!firmwareFile.exists()) {
            Log.d(TAG, "file not exist !");

            FileOutputStream outputStream = new FileOutputStream(firmwareFile);
            outputStream.write(new byte[1024]);
            outputStream.close();
        } else {
            int file_size = Integer.parseInt(String.valueOf(firmwareFile.length() / 1024));
            Log.d(TAG, "file exist ! : " + file_size);
        }

        return FileProvider.getUriForFile(mContext, FotaServiceContract.FILE_PROVIDER_PACKAGE_NAME, firmwareFile);
    }

    private class AspenDelegate implements myDelegate {

        @Override
        public void onDeviceStatusChanged(int state, Bundle extra) {
            try {
                int action = NO_ACTION;
                if (!mActionQueue.isEmpty()) {
                    synchronized (mActionQueue) {
                        action = mActionQueue.poll();
                    }
                }
                switch (state) {
                    case FotaServiceContract.STATE_DEVICE_CONNECTED:
                        if (action != NO_ACTION) {
                            switch (action) {
                                case ACTION_REQUEST_DEVICE_INFO:
                                    mFotaTimeoutHandler.removeMessages(MSG_FOTA_SET_ADDRESS_TIME_OUT);
                                    mActionQueue.add(ACTION_START_FOTA_FOLLOW);
                                    mAspenModel.getDeviceInfo();
                                    break;
                            }
                        } else {
                            Log.d(TAG, "Device connected !");
                            mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_DEVICE_CONNECTED, mBleDevInfo);
                        }
                        break;
                    case FotaServiceContract.STATE_FOTA_START:
                        Log.d(TAG, "FOTA started.");
                        mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_FOTA_START, mBleDevInfo);
                        break;
                    case FotaServiceContract.STATE_FOTA_UPDATING:
                        Log.d(TAG, "updating...");
                        break;
                    case FotaServiceContract.STATE_FOTA_ERROR:
                        mErrorCount ++;
                        Log.d(TAG, "[STATE_FOTA_ERROR] Error count : " + mErrorCount);
                        if (extra != null) {
                            Log.d(TAG, "FOTA error. Error: " + extra.getInt(FotaServiceContract.ERROR) + "; error type: "
                                    + extra.getInt(FotaServiceContract.ERROR_TYPE) + "; error message: " + extra.getString(FotaServiceContract.ERROR_MESSAGE) + ".");
                        }
                        if (mErrorCount > RETRY_COUNT) {
                            sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_1);
                        } else {
                            sendFirmware(false);
                        }
                        break;
                    case FotaServiceContract.STATE_FOTA_COMPLETED:
                        Log.d(TAG, "Fota completed.");
                        setDFUMacAddress("");
                        mFotaTimeoutHandler.removeMessages(MSG_FOTA_UPDATE_TIME_OUT);
                        removeActionQueue();
                        mCheckFotaUpdateListener.onFotaUpdateCompleted(mBleDevInfo);
                        break;
                    case FotaServiceContract.STATE_DEVICE_LOW_BATTERY:
                        Log.d(TAG, "Device is low battery.");
                        sendLowBatteryError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_2);
                        break;
                    case FotaServiceContract.STATE_DEVICE_NOT_FOUND:
                        Log.d(TAG, "Device not found.");
                        mFotaTimeoutHandler.removeMessages(MSG_FOTA_SET_ADDRESS_TIME_OUT);
                        sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_1);
                        break;
                    case FotaServiceContract.STATE_DEVICE_DISCONNECTED:
                        if (extra != null) {
                            Log.d(TAG, "Device disconnected: " + extra.getString(FotaServiceContract.ADDRESS) + ".");
                        }
                        mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_DEVICE_DISCONNECTED, mBleDevInfo);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_1);
            }
        }

        @Override
        public void onFirmwareUpdateProgressChanged(int progress) {
            Log.d(TAG, "[onFirmwareUpdateProgressChanged] progress : " + progress);
        }

        @Override
        public void onDeviceInfoGet(Bundle devInfo) {
            String iemi = devInfo.getString(FotaServiceContract.SERIAL_NUMBER);
            String version = devInfo.getString(FotaServiceContract.FIRMWARE_REVISION);
            String modelName = devInfo.getString(FotaServiceContract.MODEL_NUMBER) ;
            Log.d(TAG,
                    "[onDeviceInfoGet] firmware rev: " + devInfo.getString(FotaServiceContract.FIRMWARE_REVISION) +
                            "\nhardware rev: " + devInfo.getString(FotaServiceContract.HARDWARE_REVISION) +
                            "\nmanufacturer: " + devInfo.getString(FotaServiceContract.MANUFACTURER_NAME) +
                            "\nmodel number: " + devInfo.getString(FotaServiceContract.MODEL_NUMBER) +
                            "\nserial number: " + devInfo.getString(FotaServiceContract.SERIAL_NUMBER) +
                            "\nsoftware rev: " + devInfo.getString(FotaServiceContract.SOFTWARE_REVISION));

            mBleDevInfo.mFwVersion = version;
            mBleDevInfo.mModuleName = modelName;
            mBleDevInfo.mSerialName = iemi;
            checkFotaUpdate(mBleDevInfo, false);
        }

        @Override
        public void onFotaError() {
            Log.d(TAG, "[onFirmwareUpdateProgressChanged] onFotaError");
            sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_1);
        }

        @Override
        public void onServiceConnected() {
            if (!mActionQueue.isEmpty()) {
                synchronized (mActionQueue) {
                    int action = mActionQueue.poll();
                    Log.d(TAG, "[onServiceConnected] action : " + action);
                    switch (action) {
                        case ACTION_REQUEST_DEVICE_INFO_ERROR_HANDLE:
                            setMacAddress(mBleDevInfo.mAddr);
                            break;
                    }
                }
            }
        }
    }

    private void sendError(int status) {
        Log.d(TAG, "[sendError] status : " + status);
        if (mCheckFotaUpdateListener == null) {
            Log.w(TAG, "[sendError] listener is null !");
        }
        removeActionQueue();
        switch (status) {
            case FotaServiceContract.STATE_FOTA_DOWNLOAD_ERROR:
                mCheckFotaUpdateListener.onStatusChanged(status, mBleDevInfo);
                break;
            case FotaServiceContract.STATE_DEVICE_LOW_BATTERY:
                mFotaTimeoutHandler.removeMessages(MSG_FOTA_UPDATE_TIME_OUT);
                mCheckFotaUpdateListener.onStatusChanged(status, mBleDevInfo);
                break;
            case FotaServiceContract.STATE_FOTA_ERROR:
                mFotaTimeoutHandler.removeMessages(MSG_FOTA_UPDATE_TIME_OUT);
                mCheckFotaUpdateListener.onStatusChanged(status, mBleDevInfo);
                break;
        }
    }

    private void sendLowBatteryError(String errorCode) {
        sendError(FotaServiceContract.STATE_DEVICE_LOW_BATTERY);
        sendMSG(FirmwareUpdateUtils.INSTALL_FAILED_MSG, errorCode);
    }

    private void sendDownloadError(String errorCode) {
        sendError(FotaServiceContract.STATE_FOTA_ERROR);
        sendMSG(FirmwareUpdateUtils.DOWNLOAD_FAIL_MSG, errorCode);
    }

    private void sendUpdateError(String errorCode) {
        sendError(FotaServiceContract.STATE_FOTA_ERROR);
        sendMSG(FirmwareUpdateUtils.INSTALL_FAILED_MSG, errorCode);
    }

    private void sendMSG(String status, String errorCode) {
        SendMSGTask task = new SendMSGTask(status, errorCode);
        task.executeOnExecutor(Executors.newFixedThreadPool(1));
    }

    private class SendMSGTask extends AsyncTask<Void, Void, Void> {
        private String mStatus;
        private String mErrorCode;

        private SendMSGTask(String status, String errorCode) {
            mStatus = status;
            mErrorCode = errorCode;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mUtilsInstance != null ) {
                mUtilsInstance.sendMessage(mContext, mStatus, mErrorCode);
            } else {
                Log.d(TAG, "[SendMSGTask] mFirmwareUpdateUtils is null !");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void testCase() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_FOTA_DOWNLOAD_START, mBleDevInfo);
                    Thread.sleep(4000);
                    mCheckFotaUpdateListener.onProgressChanged(10);
                    Thread.sleep(2000);
                    mCheckFotaUpdateListener.onProgressChanged(20);
                    Thread.sleep(2000);
                    mCheckFotaUpdateListener.onProgressChanged(40);
                    Thread.sleep(2000);
                    mCheckFotaUpdateListener.onProgressChanged(60);
                    Thread.sleep(2000);
                    mCheckFotaUpdateListener.onProgressChanged(80);
                    Thread.sleep(2000);
                    mCheckFotaUpdateListener.onProgressChanged(100);
                    Thread.sleep(5000);
                    mCheckFotaUpdateListener.onStatusChanged(FotaServiceContract.STATE_FOTA_START, mBleDevInfo);
                    Thread.sleep(8000);
                    mCheckFotaUpdateListener.onFotaUpdateCompleted(mBleDevInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void removeActionQueue() {
        synchronized (mActionQueue) {
            if (!mActionQueue.isEmpty()) {
                mActionQueue.poll();
            }
        }
    }

    private class FotaTimeoutHandler implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FOTA_UPDATE_TIME_OUT:
                    Log.d(TAG, "[MSG_FOTA_UPDATE_TIME_OUT] error happened, change to FOTA fail!");
                    sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_3);
                    break;
                case MSG_FOTA_SET_ADDRESS_TIME_OUT:
                    Log.d(TAG, "[MSG_FOTA_SET_ADDRESS_TIME_OUT] error happened, change to FOTA fail!");
                    removeActionQueue();
                    sendUpdateError(FirmwareUpdateUtils.INSTALL_FAILED_MSG_ERROR_CODE_2);
                    break;
            }
            return false;
        }
    }

    private void setMacAddress(String mac) {
        mFotaTimeoutHandler.sendEmptyMessageDelayed(MSG_FOTA_SET_ADDRESS_TIME_OUT, SET_ADDRESS_TIME_OUT);
        if (mIsRetry) {
            String dfuAddress = mSharedPrefManager.getDFUMacAddress();
            Log.d(TAG, "[setMacAddress] retry case : " + hashAddress(dfuAddress));
            mAspenModel.setMacAddress(dfuAddress);
            mFotaTimeoutHandler.removeMessages(MSG_FOTA_SET_ADDRESS_TIME_OUT);
            mActionQueue.add(ACTION_START_FOTA_FOLLOW);
            checkFotaUpdate(mBleDevInfo, false);
        } else {
            mActionQueue.add(ACTION_REQUEST_DEVICE_INFO);
            Log.d(TAG, "[setMacAddress] normal case : " + hashAddress(mac));
            mAspenModel.setMacAddress(mac);
        }

    }

    private void setDFUMacAddress(String address) {
        Logger.w(TAG, "[setDFUMacAddress] address : " + hashAddress(address));
        String dfuAddress = "";
        try {
            /**
             *  The rule for normal mac address change to DFU mac address is add 1
             *  ex: normal - 00:00:00:00:00:FF > DFU - 00:00:00:00:00:00
             */
            if (!TextUtils.isEmpty(address)) {
                String last2Address = address.substring(address.lastIndexOf(":")+1, address.length());
                BigInteger value = new BigInteger(last2Address, 16);
                value = value.add(new BigInteger("1"));
                String newValue = value.toString(16);
                if (newValue.length() > 2) {
                    newValue = newValue.substring(1, newValue.length());
                }
                dfuAddress = address.replace(last2Address, newValue).toUpperCase();
            }
            Log.d(TAG, "[setDFUMacAddress] dfuAddress : " + hashAddress(dfuAddress));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mSharedPrefManager != null) {
            mSharedPrefManager.setDFUMacAddress(dfuAddress);
        } else {
            Logger.d(TAG, "[setDFUMacAddress] mSharedPrefManager is null !");
        }
    }

    private String hashAddress(String address) {
        String hashString = "";
        try {
            if (!TextUtils.isEmpty(address)) {
                String list[] = address.split(":");
                hashString = list[list.length - 1] + list[list.length - 2];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashString;
    }
}
