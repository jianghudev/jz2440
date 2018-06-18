// IFotaService.aidl
package com.finchtechnologies.fota;

import com.finchtechnologies.fota.IFotaListener;

interface IFotaService {

	/*
	* call this to set mac address
	*@param addr
    * addr is the device mac address, ex: addr="61:60:E9:CE:07:A1"
	*/
	void setMacAddress(String addr);

    /**
     * Call this to retrieve Controller's HW information.
     * @return null if fail or device disconnect, otherwise service needs to store the key value pair to carrier hw information
	 * please confirm with PM for firmware version, model name, serial name and mid.
     * the bundle value please using "String" type (ex : model number, firmware version, hardware version...)
		below are the key name:
		device firmware version : "device_version" (ex:a.b.c.d)
		device model name : "model_name"
		device serial name : "serial_name"
		device imei : "device_imei"
		device mid : "device_mid"
     */
    Bundle getDeviceInfo();


    /**
     * Call this to retrieve unit's battery level
     * addr is the device mac address, ex: addr="61:60:E9:CE:07:A1"
     * @return voltage level (-1 ~ 100) the normal range is 0~100, -1 means cannot get battery level.
     */
    int getBatteryVoltageLevel();

    /**
     * Call this to execute firmware update.
     * @param uri device firmware's path
     * @return false if something wrong, ex : UpdateListener doesn't be set
     */
    boolean upgradeFirmware(in Uri uri);

    /**
     * Call this to re-execute firmware update.
     * @param uri device firmware's path
     * @return false if something wrong, ex : UpdateListener doesn't be set
     */
    boolean upgradeFirmwareOnDfuTarg(in Uri uri);
    /**
     * Call this to set listener for acquiring firmware update status and progress.
     */
    void setDeviceListener(IFotaListener listener);

}