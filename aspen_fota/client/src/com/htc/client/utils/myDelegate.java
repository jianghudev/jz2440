package com.htc.client.utils;

import android.os.Bundle;

/**
 * Created by hubin_jiang on 2018/6/18.
 */

public interface myDelegate {
    void onDeviceStatusChanged(int state, Bundle bundle);
    void onFirmwareUpdateProgressChanged(int progress);
    void onDeviceInfoGet(Bundle info);
    void onFotaError();
    void onServiceConnected();
}
