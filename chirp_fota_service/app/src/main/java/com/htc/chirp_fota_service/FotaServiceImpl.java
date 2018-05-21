package com.htc.chirp_fota_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FotaServiceImpl extends IFotaService.Stub {
    static final String TAG = "Impl";

    public final static int DEVICE_HMD  = 0;
    public final static int DEVICE_CONTROLLER  = 1;
    public static int curret_device  = -1;
    public final static int STATE_START = 0;
    public final static int STATE_UPDATING = 1;
    public final static int STATE_ERROR = 2;
    public final static int STATE_COMPLETED = 3;

    public static int BLE_STATE = 3;//ble connect success;

    public boolean DEVICE_STATE = false;
    public boolean BLE_DEVICE_STATE = false;
    public boolean isMove = false;

    public Usb mUsb;
    public UsbDevice mUsbDevice;


    public UsbEndpoint mEpIn;
    public UsbEndpoint mEpOut;

    boolean bootloader_Image = false;
    boolean upgradeImageall = false;
    boolean mbDuringUpdating = false;
    boolean mbldfu = false;
    long startTime = 0;

    OnFirmwareUpdateListener mUpdateListener = null;
    IDeviceConnectedListener mDeviceConnectedListener = null;
    int deviceListener = 0;

    private Context mContext;
    public FotaServiceImpl(Context mContext)
    {
        this.mContext = mContext;
    }
    List<File> writeDfuFiles = new ArrayList<File>();
    private String unzip_root;
    private String unzipPath;

    @Override
    public void DownLoad() throws RemoteException {
        // do nothing
    }

    public int getVersionCode()
    {
        return 1;
    }
    public String getServiceVersionCode()
    {
        String verName = getVersion(this.mContext);
        return verName;
    }

    public String getVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return pInfo.versionName;   //get gradle app version name
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public void setVRNTF(boolean isChecked){
        // do nothing
    }
    public void reGetDeviceStatus(){
        //do nothing
    }
    public Bundle getDeviceInfo(int device /* 1 for HMD, 2 for Controller */ )
    {
        Log.i(TAG,"enter getdevice info, device = " + device);
        int retryCnt = 3;
        if(device == 1){
            Log.i(TAG, "open usb ,before get device info");
            mUsbDevice = mUsb.getUsbDevice();

            if(mUsbDevice != null){
                mUsb.setUsbDevice(mUsbDevice);
                mUsb.tryClaimDevice(mUsbDevice);
            }
            if(mUsb.USB_STATE == 1) {
                Bundle devinf = new Bundle();
                int ret = 0;
                int inMax = 0;
                int count = 0;
                retryCnt = 3;
                inMax = mEpIn.getMaxPacketSize();
                Log.i(TAG,"inMax=" + inMax);
                byte buffer[] = new byte[inMax];
                byte buffer1[] = new byte[inMax];
                byte buffer2[] = new byte[inMax];
                byte buffer3[] = new byte[inMax];
        //        String getdeviceinf_cmd = "A3";
                byte getversion_cmd[] = new byte[8];

                getversion_cmd[0] = 'p';
                getversion_cmd[1] = 0x00;
                getversion_cmd[2] = 0x0D;

                //do while ,waiting device return;
                Log.i(TAG,"enter getDeviceInfo!");
                if(mUsb.mfotaConnection == null) {
                    Log.i(TAG,"mUsbConnection is null");
                }
                if(mEpOut == null) {
                    Log.i(TAG,"mEpOut is null");
                }
                try{
                    Thread.sleep(1000);
                    ret = Usb.sendToEndpoint(mUsb.mfotaConnection, mEpOut,getversion_cmd);
//                    Log.i(TAG,"ret="+ret);

                    do {
                        count = mUsb.mfotaConnection.bulkTransfer(mEpIn, buffer, buffer.length, 200);
                        if (count > 0) {
                            Log.i(TAG,"get data success");
                            break;
                        } else {
                            Log.i(TAG,"get data fail");
                        }
                        retryCnt --;
                    } while (retryCnt > 0);
                    if(count > 0) {
                        String str1 = new String(buffer, 0, count);
//                    String str1 = new String(buffer,"UTF-8");
//                        Log.i(TAG, "DeviceInfo = " + str1);
                        devinf.putString("DeviceInfo", str1);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

                //get device is hmd or ctrl

                byte getdevice_cmd[] = new byte[8];
                getdevice_cmd[0] = 'p';
                getdevice_cmd[1] = 0x00;
                getdevice_cmd[2] = 0x11;

                ret = Usb.sendToEndpoint(mUsb.mfotaConnection, mEpOut, getdevice_cmd);
//                Log.i(TAG,"ret="+ret);

                retryCnt = 3;
                try{
                    do {
                        count = mUsb.mfotaConnection.bulkTransfer(mEpIn, buffer1, buffer1.length, 200);
        //                Thread.sleep(1000);
                        if (count > 0) {
                            Log.i(TAG,"get data success");
                            break;
                        } else {
                            Log.i(TAG,"get data fail");
                        }
                        retryCnt --;
                    } while (retryCnt > 0);
                        if(count>0) {
                            String str2 = new String(buffer1, 0, count);
//                            Log.i(TAG, "count=" + count);
//                            Log.i(TAG, "DeviceClass = " + str2);
                            devinf.putString("DeviceClass", str2);
                        }

                }catch (Exception e){
                    e.printStackTrace();
                }

                //get mode number

                byte getmode_cmd[] = new byte[8];
                getmode_cmd[0] = 'p';
                getmode_cmd[1] = 0x00;
                getmode_cmd[2] = 0x01;

                ret = Usb.sendToEndpoint(mUsb.mfotaConnection, mEpOut, getmode_cmd);
//                Log.i(TAG,"ret="+ret);

                retryCnt = 3;
                try{
                    do {
                        count = mUsb.mfotaConnection.bulkTransfer(mEpIn, buffer2, buffer2.length, 200);
        //                Thread.sleep(1000);
                        if (count > 0) {
                            Log.i(TAG,"get data success");
                            break;
                        } else {
                            Log.i(TAG,"get data fail");
                        }
                        retryCnt --;
                    } while (retryCnt > 0);
        //            String str3 = new String(buffer2,"UTF-8");
                    String str3 = new String(buffer2,0,count);
//                    Log.i(TAG,"count="+count);
//                    Log.i(TAG,"ModeNumber = " + str3);
                    devinf.putString("ModeNumber",str3);

                }catch (Exception e){
                    e.printStackTrace();
                }

                //get serial number

                byte getserial_cmd[] = new byte[8];
                getserial_cmd[0] = 'p';
                getserial_cmd[1] = 0x00;
                getserial_cmd[2] = 0x02;

                ret = Usb.sendToEndpoint(mUsb.mfotaConnection, mEpOut, getserial_cmd);
//                Log.i(TAG,"ret="+ret);

                retryCnt = 3;
                try{
                    do {
                        count = mUsb.mfotaConnection.bulkTransfer(mEpIn, buffer3, buffer3.length, 200);
        //                Thread.sleep(1000);
                        if (count > 0) {
                            Log.i(TAG,"get data success");
                            break;
                        } else {
                            Log.i(TAG,"get data fail");
                        }
                        retryCnt --;
                    } while (retryCnt > 0);
        //            String str4 = new String(buffer3,"UTF-8");
                    String str4 = new String(buffer3,0,count);
//                    Log.i(TAG,"count="+count);
//                    Log.i(TAG,"SerialNumber = " + str4);
                    devinf.putString("SerialNumber",str4);

                }catch (Exception e){
                    e.printStackTrace();
                }
                mUsb.release();
                return devinf;
            }
        }
        return null;
    }

    public int getBatteryVoltageLevel(int device)
    {
//        Log.i(TAG, "open usb ,before get Battery Voltage");
        int bat_value = 0;
        if(device == 2){
//            Log.d(TAG," get ctrl battery"); // need add  interface by ble
        }else if (device == 1) {
            mUsbDevice = mUsb.getUsbDevice();
            if(mUsbDevice != null){
                mUsb.setUsbDevice(mUsbDevice);
                mUsb.tryClaimDevice(mUsbDevice);
            }
            int ret = 0, count = 0;
            int inMax = mEpIn.getMaxPacketSize();
            byte buffer[] = new byte[inMax];
            byte getbatinfo_cmd[] = new byte[8];
            getbatinfo_cmd[0] = 'c';
            getbatinfo_cmd[1] = 0x01;
            getbatinfo_cmd[2] = 0x41;
            getbatinfo_cmd[3] = 0x00;
            getbatinfo_cmd[4] = 0x04;
//            Log.i(TAG, "enter getBatteryVoltageLevel!");
            ret = Usb.sendToEndpoint(mUsb.mfotaConnection, mEpOut, getbatinfo_cmd);

            int retryCnt = 3;
            try {
                do {
                    count = mUsb.mfotaConnection.bulkTransfer(mEpIn, buffer, buffer.length, 200);

                    if (count >= 0) {
                        Log.i(TAG, "get BatteryVoltageLevel success");
//                        Log.i(TAG, "count = " + count);
                        break;
                    } else {
                        Thread.sleep(2000);
                        Log.i(TAG, "get BatteryVoltageLevel fail");
                    }
                    retryCnt--;
                } while (retryCnt > 0);
                String str = new String(buffer, 0, count);
//                Log.i(TAG, "BatteryVoltageLevel = " + str);
                bat_value = Integer.parseInt(str);
//                Log.i(TAG, "BatteryVoltageLevel = " + bat_value);
                mUsb.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bat_value;
    }





    private final BroadcastReceiver mUpdateStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"receive status *********************0");
            String action = intent.getAction();

            if ("com.android.UpdateStatusBroadcast".equals(action)) {
                Log.i(TAG,"com.android.UpdateStatusBroadcast");
                int device = intent.getIntExtra("device", -1);
                int status = intent.getIntExtra("status", -1);
                Bundle b = intent.getBundleExtra("extra");
                if(status == STATE_ERROR) {
                    Log.d(TAG, "device = " + device + " status = STATE_ERROR ");
                }
                if(status == STATE_UPDATING) {
                    Log.d(TAG, "device = " + device + " status = STATE_UPDATING");
                }
                if(status == STATE_COMPLETED) {
                    Log.d(TAG, "device = " + device + " status = STATE_COMPLETED");
                }
                try {
                    mUpdateListener.onFirmwareUpdateStatusChanged(device, status, b);

                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }
    };

    private final BroadcastReceiver mUpdateProgressReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"receive process *********************1");
            String action = intent.getAction();

            if ("com.android.UpdateProgressBroadcast".equals(action)) {
                Log.i(TAG,"received com.android.UpdateProgressBroadcast");
                int device = intent.getIntExtra("device", -1);
                int progress = intent.getIntExtra("progress", 0);
                Log.d(TAG,"device = " + device + " progress = " + progress);
                try {
                    mUpdateListener.onFirmwareUpdateProgressChanged(device,progress);

                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }
    };

    public boolean upgradeFirmware(final int device, final Uri uri)
    {
        curret_device = device;
        startTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "start time = " + startTime);
        if(mUpdateListener == null) {
            Log.e(TAG, "Listener is not be set before upgradeFirmware() is called");
            return false;
        }
        if (curret_device == 1) {
            mUsbDevice = mUsb.getUsbDevice();
            if(mUsbDevice != null){
                mUsb.setUsbDevice(mUsbDevice);
                mUsb.tryClaimDevice(mUsbDevice);
            }
            if (true == mbDuringUpdating) {
                Log.e(TAG, "Another upgrade request is running");
                return false;
            }

            if (Usb.USB_STATE != 1) {
                Log.e(TAG, "Usb mode is not cdc before upgradeFirmware() is called");
                return false;
            }
            new Thread() {
                @Override
                public void run() {
                    InputStream inputStream = null;
                    Bundle updateInfo = new Bundle();
                    byte reboot_cmd[] = new byte[8];
                    reboot_cmd[0] = 'c';
                    reboot_cmd[1] = 0x01;
                    reboot_cmd[2] = 0x41;
                    reboot_cmd[3] = 0x00;
                    reboot_cmd[4] = 0x02;//reboot to bootloader
                    int ret;
                    mbDuringUpdating = true;
                    try {
                        //1.unzip
                        Log.i(TAG, "Hmd_Uri=" + uri.toString());
                        inputStream = mContext.getContentResolver().openInputStream(uri);
                        if (inputStream != null) {
                            // TODO : read data from InputString
                            writeDfuFiles = unZip(inputStream);
                            mUpdateListener.onFirmwareUpdateStatusChanged(device, STATE_START, updateInfo);

                            // remember to close it after use.
                            inputStream.close();
                        }
                        Log.i("Fota", "unzip done.");
                        //2. parse
                        //3. reboot device to bootloader
                        Log.i(TAG, writeDfuFiles.get(0).toString());
                        if (writeDfuFiles.size() > 0) {
                            Log.i("Fota", "unzip success.");
                            ret = Usb.sendToEndpoint(mUsb.mfotaConnection, mEpOut, reboot_cmd);
                            Log.i(TAG, " reboot to bootloader ret=" + ret);
                            if (ret != -1) {
                                upgradeImageall = true;
                                Log.i(TAG, "send reboot command success.");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        return true;
    }

    public void setFirmwareUpdateListener(OnFirmwareUpdateListener l)
    {
        mUpdateListener = l;
        //System.out.println("under construct!");
    }
    public void setDeviceConnectedListener(IDeviceConnectedListener l)
    {
        mDeviceConnectedListener = l;
        Log.i(TAG,"set connect listener");
        mUsbDevice = mUsb.getUsbDevice();
        if(mUsbDevice != null && (mUsb.mfotaUsbManager.hasPermission(mUsbDevice))){
            DEVICE_STATE = true;
            curret_device = 1;
            try {
                Log.i(TAG, "connected! ,DEVICE_STATE = " + DEVICE_STATE + " Usb.USB_STATE = " + Usb.USB_STATE);
                mDeviceConnectedListener.onConnectedStateStatusChanged(curret_device, DEVICE_STATE, Usb.USB_STATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean updateImage(int device)
    {
        return true;
    }
    private List<File> unZip(InputStream inputStream) throws Exception {
        File dfu_root,myFile;
        String myFilePath = null;
//        String myFileName = null;
        String bl_FileName = null;
        String sys_FileName = null;
        String ccg41_FileName = null;
        String ccg42_FileName = null;
        List<File> DfuFiles = new ArrayList<File>();
        String state = Environment.getExternalStorageState();
        //final String zipPath = imagePath;
        unzip_root = this.mContext.getFilesDir().toString();
        unzipPath = unzip_root + File.separator + "unzip_fota";
        Log.i(TAG,unzipPath.toString());
        if(state.equals(Environment.MEDIA_MOUNTED)){

            File unzipPathFile = new File(unzipPath);
            if(!unzipPathFile.exists()){
                unzipPathFile.mkdir();
            }else{
                unzipPathFile.delete();
                unzipPathFile.mkdir();
            }
            try {
                Log.i(TAG,"unzip start.");
                ZipUtils.UnZipFolder(inputStream, unzipPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG,"unzip done");
        dfu_root = new File(unzipPath + File.separator + "fota_dfu_package");
        if (dfu_root.exists()) {
            String[] files = dfu_root.list();
            int i=0;
            // todo support multiple dfu files in dir
            if (files.length > 0) {   // will select first dfu file found in dir
                for (String file : files) {
                    if (file.endsWith(".dfu")) {
                        myFilePath = dfu_root.toString();
                        if(file.equals("link_BL1.dfu")) {
                            bl_FileName = file;
                        }else if (file.equals("link_sys.dfu")){
                            sys_FileName = file;
                        }else if(file.equals("CCG4_FW1.dfu")){
                            ccg41_FileName = file;
                        }else {
                            ccg42_FileName = file;
                        }
                        i++;
                    }
                }
                i = 0;
                if(bl_FileName != null) {
                    myFile = new File(myFilePath + "/" + bl_FileName);
                    DfuFiles.add(i, myFile);
                    i++;
                }
                if(sys_FileName != null){
                    myFile = new File(myFilePath + "/" + sys_FileName);
                    DfuFiles.add(i,myFile);
                    i++;
                }
                if(ccg41_FileName != null){
                    myFile = new File(myFilePath + "/" + ccg41_FileName);
                    DfuFiles.add(i,myFile);
                    i++;
                }
                if(ccg42_FileName != null){
                    myFile = new File(myFilePath + "/" + ccg42_FileName);
                    DfuFiles.add(i, myFile);
                }

            }
        }
        if ((bl_FileName == null) && (sys_FileName == null) && (ccg41_FileName == null) && (ccg42_FileName == null)) {
            throw new Exception("No .dfu file found in Download Folder");
        }
        return DfuFiles;

    }
}
