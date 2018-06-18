package com.htc.client.utils;

public class FotaServiceContract {
    public static final String DEVICE_MODEL_NAME = "model_name";
    public static final String DEVICE_VERSION = "device_version";
    public static final String DEVICE_SERIAL_NAME = "serial_name";

    public static final int STATE_FOTA_START = 0;
    public static final int STATE_FOTA_UPDATING = 1;
    public static final int STATE_FOTA_ERROR = 2;
    public static final int STATE_FOTA_COMPLETED = 3;
    public static final int STATE_DEVICE_LOW_BATTERY = 4;
    public static final int STATE_DEVICE_NOT_FOUND = 5;
    public static final int STATE_DEVICE_CONNECTED = 6;
    public static final int STATE_DEVICE_DISCONNECTED = 7;
    public static final int STATE_FOTA_DOWNLOAD_ERROR = 8;
    public static final int STATE_FOTA_BATTERY_ERROR = 9;
    public static final int STATE_FOTA_DOWNLOAD_START = 10;
    public static final int STATE_NO_FOTA_UPDATE = 11;

    public final static String ADDRESS = "ADDRESS";
    public final static String ERROR_TYPE = "ERROR_TYPE";
    public final static String ERROR = "ERROR";
    public final static String ERROR_MESSAGE = "ERROR_MESSAGE";

    public final static String MANUFACTURER_NAME = "MANUFACTURER_NAME";
    public final static String MODEL_NUMBER = "MODEL_NUMBER";
    public final static String SERIAL_NUMBER = "SERIAL_NUMBER";
    public final static String HARDWARE_REVISION = "HARDWARE_REVISION";
    public final static String FIRMWARE_REVISION = "FIRMWARE_REVISION";
    public final static String SOFTWARE_REVISION = "SOFTWARE_REVISION";

    public static final String FILE_FOLDER_NAME = "firmware";
    public static final String AUTO_TEST_FOTA = "auto_test_fota.txt";

    public static final String FILE_NAME = "firmware.zip";
    public static final String CTRL_FILE_NAME = "fota_dfu_package_ctrl.zip";
    public static final String HMD_FILE_NAME = "fota_dfu_package_hmd.zip";

    public static final String FILE_PROVIDER_PACKAGE_NAME = "com.htc.miac.controllerutility.fileprovider";

    public static final boolean DEBUG_CHECK_UPDATE = true;



    public static final int TYPE_DEVICE_HMD = 1;
    public static final int TYPE_DEVICE_3DOF = 2;
    public static final int TYPE_DEVICE_DONGLE = 0;
    public static final int TYPE_DEVICE_CONTROLLER0 = 1;
    public static final int TYPE_DEVICE_CONTROLLER1 = 2;
    public static final int TYPE_DEVICE_CAMERA = 3;

}