// IScannerListener.aidl
package com.htc.vr.controllerscanner;

import com.htc.vr.controllerscanner.BleDev;
import com.htc.vr.controllerscanner.BleDevInfo;

interface IScannerListener {
    void onScanStarted();
    void onScanCompleted();
    void onScanResult(in BleDev device);
    void onBatchScanResults(in List<BleDev> devices);
    boolean isConnected(String mac);
    BleDevInfo getDeviceInfo(String mac);
}
