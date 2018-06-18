// IScannerServiceInterface.aidl
package com.htc.client.vr;

import android.os.ParcelUuid;

import com.htc.client.vr.IScannerListener;
import com.htc.client.vr.BleDev;
import com.htc.client.vr.BleDevInfo;

interface IScannerServiceInterface {
    void start();
    void stop();
    void registerListener(in ParcelUuid appId, in IScannerListener listener);
    void unregister(in ParcelUuid appId);
    List<BleDev> getControllers();
    void setControllerCount(int cnt);
    void onConnected(String mac);
    void onDisconnected(String mac);
    void onDeviceInfo(String mac, in BleDevInfo info);
}
