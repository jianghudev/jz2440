// IDeviceConnectedListener.aidl
package com.htc.chirp_fota;

// Declare any non-default types here with import statements
interface IDeviceConnectedListener {
    /**
    * Callback it while status change
    * @param device echo to device
    * DEVICE_HMD = 1;
      DEVICE_CONTROLLER = 2;
      DEVICE_LINK = 3;
      DEVICE_CAMERA = 4;
      DEVICE_LED_MARKER = 5;
    * @param isConnected  true or false
    * @param usb_state
      NO use = 0;
      USB_STATE_CDC_MODE = 1;
      USB_STATE_DFU_MODE = 2;
    */
    void onConnectedStateStatusChanged(int device, boolean isConnected,int usb_state);
}