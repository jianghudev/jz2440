// IFotaService.aidl
package com.htc.chirp_fota_service;

import com.htc.chirp_fota_service.OnFirmwareUpdateListener;
import com.htc.chirp_fota_service.IDeviceConnectedListener;
// Declare any non-default types here with import statements

interface IFotaService {
    /**
     * demo for excute in thread.
     */
    void DownLoad();

    /**
     * (Optional) It is use to do the interface version control if service side will extend interface in the future
     * @return Ordinal (1, 2, 3 ....)
     */
    int getVersionCode();

    /**
     * Call this to retrieve HMD/Controller's HW information.
     * @param device
     * DEVICE_HMD = 1;
       DEVICE_3DOF = 2;
       DEVICE_DONGLE = 0;
       DEVICE_CONTROLLER0 = 1;
       DEVICE_CONTROLLER1 = 2;
       DEVICE_CAMERA = 3;
     * @return null if fail, otherwise service needs to store the key value pair to carrier hw information
     * about the device (ex : modal number, firmware version, hardware version...)
     */
    Bundle getDeviceInfo(int device);


    /**
     * Call this to retrieve unit's battery level
     * @param device
     * DEVICE_HMD = 1;
       DEVICE_3DOF = 2;
       DEVICE_DONGLE = 0;
       DEVICE_CONTROLLER0 = 1;
       DEVICE_CONTROLLER1 = 2;
       DEVICE_CAMERA = 3;
     * @return voltage level (0 ~ 100)
     */
    int getBatteryVoltageLevel(int device);

    /**
     * Call this to execute firmware update.
     * @param device
     * DEVICE_HMD = 1;
       DEVICE_3DOF = 2;
       DEVICE_DONGLE = 0;
       DEVICE_CONTROLLER0 = 1;
       DEVICE_CONTROLLER1 = 2;
       DEVICE_CAMERA = 3;
     * @param imagePath fota image's path
     * @return false if something wrong, ex : UpdateListener doesn't be set
     */
    //boolean upgradeFirmware(int device, String imagePath);
    boolean upgradeFirmware(int device, in Uri uri);

    /**
     * Call this to execute firmware update.
     * @param device 0 for HMD, 1 for Controller
     * @param imagePath fota image's path
     * @return false if something wrong, ex : UpdateListener doesn't be set
     */
//    boolean upgradeCtrlFirmware(int device, in Uri uri);
    /**
     * Call this to set listener for acquiring firmware update status and progress.
     */
    void setFirmwareUpdateListener(OnFirmwareUpdateListener l);

    /**
    * Call this to set listener for acquiring device connected status.
    */
    void setDeviceConnectedListener(IDeviceConnectedListener l);

    /**
     * (Optional) It is use to do the interface version control if service side will extend interface in the future
     * @return Ordinal (1, 2, 3 ....)
     */
    String getServiceVersionCode();

    /**
    *   Call this to send the option to show high priority notification when in VR
    */
    void setVRNTF(boolean isChecked);

    /**
    *  Call this to request init Ximmerse device to get permission
    */
    void reGetDeviceStatus();
}
