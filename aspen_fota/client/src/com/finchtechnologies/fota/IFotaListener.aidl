// IFotaListener.aidl
package com.finchtechnologies.fota;

interface IFotaListener {
    /**
     * Callback it while status change.
     * @param state the device state
        STATE_FOTA_START = 0;
        STATE_FOTA_UPDATING = 1;
        STATE_FOTA_ERROR = 2;
        STATE_FOTA_COMPLETED = 3;
        STATE_DEVICE_LOW_BATTERY = 4;
        STATE_DEVICE_NOT_FOUND = 5;
		STATE_DEVICE_CONNECTED = 6;
		STATE_DEVICE_DISCONNECTED = 7;
     * @param extra use it to carry more information, ex : error code. the error code value is "Integer"
	    below is the error code key name:
		error code : "error_code"
     */
    void onDeviceStatusChanged(int state,in Bundle extra);

    /**
     * Callback while progress change during updating, so end-user can know how long they have to wait
     * @param progress (0 ~ 100)
     */
    void onFirmwareUpdateProgressChanged(int progress);
}