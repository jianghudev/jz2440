// OnFirmwareUpdateListener.aidl
package com.htc.chirp_fota_service;

// Declare any non-default types here with import statements

interface OnFirmwareUpdateListener {
    /**
     * Callback it while status change, the normal flow is START -> UPDATING -> COMPLETED
     * @param device echo to device (first parameter of upgradeFirmware)
        DEVICE_HMD = 1;
        DEVICE_CONTROLLER = 2;
        DEVICE_LINK = 3;
        DEVICE_CAMERA = 4;
        DEVICE_LED_MARKER = 5;
     * @param state the device state
        STATE_FOTA_START = 0;
        STATE_FOTA_UPDATING = 1;
        STATE_FOTA_ERROR = 2;
        STATE_FOTA_COMPLETED = 3;
        STATE_DEVICE_LOW_BATTERY = 4;
        STATE_DEVICE_NOT_FOUND = 5;
     * @param extra use it to carry more information, ex : error code.
     */
    void onFirmwareUpdateStatusChanged(int device, int state,out Bundle extra);

    /**
     * Callback while progress change during updating, so end-user can know how long they have to wait
     * @param device echo to device (first parameter of upgradeFirmware)
         DEVICE_HMD = 1;
         DEVICE_CONTROLLER = 2;
         DEVICE_LINK = 3;
         DEVICE_CAMERA = 4;
         DEVICE_LED_MARKER = 5;
     * @param progress (0 ~ 100)
     */
    void onFirmwareUpdateProgressChanged(int device, int progress);
}
