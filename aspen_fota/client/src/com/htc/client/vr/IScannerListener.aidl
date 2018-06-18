// IScannerListener.aidl
package com.htc.client.vr;

import com.htc.client.vr.BleDev;
import com.htc.client.vr.BleDevInfo;

interface IScannerListener {
    void onScanStarted();
    void onScanCompleted();
    void onScanResult(in BleDev device);
    void onBatchScanResults(in List<BleDev> devices);
    boolean isConnected(String mac);
    BleDevInfo getDeviceInfo(String mac);
}
