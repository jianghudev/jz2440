package com.htc.service;

import android.content.Context;
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

import com.htc.service.usb.Usb;
import com.htc.service.usb.UsbTunnelData;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FotaServiceImpl extends IFotaService.Stub {
    private static final String TAG=Const.G_TAG;

    public final static int DEVICE_HMD  = 0;
    public final static int DEVICE_CONTROLLER  = 1;
    public static int curret_device  = -1;
    public final static int STATE_START = 0;
    public final static int STATE_UPDATING = 1;
    public final static int STATE_ERROR = 2;
    public final static int STATE_COMPLETED = 3;


    public boolean DEVICE_STATE = false;

    private Upgrade_Thread my_upgradeThread=null;

    public Usb mUsb;
    public UsbDevice mUsbDevice;


    public UsbEndpoint mEpIn;
    public UsbEndpoint mEpOut;

    boolean upgradeImageall = false;
    boolean mbDuringUpdating = false;
    long startTime = 0;

    OnFirmwareUpdateListener mUpdateListener = null;
    IDeviceConnectedListener mDeviceConnectedListener = null;

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


    public String get_aspen_prop(UsbTunnelData  uData, boolean need_result){
        int retryCount = 3;
        while(retryCount-- >0 ){
            try {
                if (mUsb.RequestCdcData(uData) == true) {
                    if(need_result) {
                        if (uData.recv_array_count > 0) {
                            String ret_str = new String(uData.recv_array, 0, uData.recv_array_count, "UTF-8");
                            return ret_str;
                        }
                    }else{
                        return null; //// no need cdc result
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e ) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String get_aspen_device_info(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'p';
        Data.send_array[1] = 0x00;
        Data.send_array[2] = 0x0D;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        return get_aspen_prop(Data,true);
    }
    //get device is hmd or ctrl
    public String get_aspen_device_class(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'p';
        Data.send_array[1] = 0x00;
        Data.send_array[2] = 0x11;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        return get_aspen_prop(Data,true);
    }
    //get mode number
    public String get_aspen_model_number(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'p';
        Data.send_array[1] = 0x00;
        Data.send_array[2] = 0x01;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        return get_aspen_prop(Data,true);
    }
    //get serial number
    public String get_aspen_serial_number(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'p';
        Data.send_array[1] = 0x00;
        Data.send_array[2] = 0x02;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        return get_aspen_prop(Data,true);
    }
    public Bundle getDeviceInfo(int device){
        Bundle dev_info = new Bundle();
        int retryCnt = 3;
        if(device != 1) {
            Log.i(TAG,"err! device=" + device);
            return null;
        }
        if(mUsb.USB_STATE == 1) {
            String str1 = get_aspen_device_info();
            String str2 = get_aspen_device_class();
            String str3 = get_aspen_model_number();
            String str4 = get_aspen_serial_number();

            dev_info.putString("DeviceInfo", str1);
            dev_info.putString("DeviceClass", str2);
            dev_info.putString("ModeNumber",str3);
            dev_info.putString("SerialNumber",str4);
            return dev_info;
        }
        return null;
    }

    public String get_aspen_battery_level(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'c';
        Data.send_array[1] = 0x01;
        Data.send_array[2] = 0x41;
        Data.send_array[3] = 0x00;
        Data.send_array[4] = 0x04;
        Data.send_array_count = 5;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;
        return get_aspen_prop(Data,true);
    }
    public int getBatteryVoltageLevel(int device){
        if (device == 1) {
            String str = get_aspen_battery_level();
            int bat_value = Integer.parseInt(str);
            return bat_value;
        }
        return 0;
    }

    public void set_aspen_reboot(){
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'c';
        Data.send_array[1] = 0x01;
        Data.send_array[2] = 0x41;
        Data.send_array[3] = 0x00;
        Data.send_array[4] = 0x02;//reboot to bootloader
        Data.send_array_count = 5;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 0;
        get_aspen_prop(Data,false);
    }

    private class Upgrade_Thread extends Thread{
        private Uri mUri=null;
        private int mDevice=0;
        public Upgrade_Thread(Uri aUri, int aDevice) {
            this.mUri = aUri;
            this.mDevice = aDevice;
        }
        @Override
        public void run() {
            try {
                InputStream inputStream = null;
                Bundle updateInfo = new Bundle();
                mbDuringUpdating = true;
                Log.i(TAG, "fota_uri=" + mUri.toString());
                inputStream = mContext.getContentResolver().openInputStream(mUri);
                if (inputStream != null) {
                    writeDfuFiles = unZip(inputStream);
                    mUpdateListener.onFirmwareUpdateStatusChanged(mDevice, STATE_START, updateInfo);
                    inputStream.close();
                }
                Log.i("Fota", "unzip done.");
                Log.i(TAG, writeDfuFiles.get(0).toString());
                if (writeDfuFiles.size() > 0) {
                    Log.i("Fota", "unzip success.");
                    Log.i(TAG, " reboot to bootloader");
                    set_aspen_reboot();
                    upgradeImageall = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public boolean upgradeFirmware(final int device, final Uri uri){
        curret_device = device;
        startTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "start upgradeFirmware  device=" +device);
        if (1 != device) {
            Log.e(TAG, "err! device=" +device);
            return false;
        }
        if (Usb.USB_STATE != 1) {
            Log.e(TAG, "Usb mode is err!");
            return false;
        }
        if(null == mUpdateListener) {
            Log.e(TAG, "mUpdateListener is null");
            return false;
        }
        if (mbDuringUpdating) {
            Log.e(TAG, "Another upgrade request is running");
            return false;
        }

        my_upgradeThread = new Upgrade_Thread(uri,device);
        my_upgradeThread.start();
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
//        mUsbDevice = mUsb.getUsbDevice();
//        if(mUsbDevice != null && (mUsb.mfotaUsbManager.hasPermission(mUsbDevice))){
//            DEVICE_STATE = true;
//            curret_device = 1;
//            try {
//                Log.i(TAG, "connected! ,DEVICE_STATE = " + DEVICE_STATE + " Usb.USB_STATE = " + Usb.USB_STATE);
//                mDeviceConnectedListener.onConnectedStateStatusChanged(curret_device, DEVICE_STATE, Usb.USB_STATE);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
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
