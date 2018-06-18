// IScannerServiceInterface.aidl
package com.htc.vr.controllerscanner;

import com.htc.vr.controllerscanner.IScannerListener;
import com.htc.vr.controllerscanner.BleDev;
import com.htc.vr.controllerscanner.BleDevInfo;
import android.os.ParcelUuid;

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
