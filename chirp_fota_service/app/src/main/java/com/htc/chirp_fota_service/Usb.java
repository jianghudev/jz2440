package com.htc.chirp_fota_service;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

public class Usb {
    private static final String TAG=Const.G_TAG;

    public static int USB_STATE       = 0;//cdc:USB_STATE=1;dfu:USB_STATE=2;
    private Context mContext;

    public UsbManager mfotaUsbManager;
    private UsbDevice mfotaDevice;
    public UsbDeviceConnection mfotaConnection;
    public UsbInterface mfotaInterface;
    //private int mDeviceVersion;
    public UsbEndpoint mEpIn;
    public UsbEndpoint mEpOut;


    public final static int USB_CDC_VENDOR_ID = 0x0483;   // VID while in CDC mode 0x0BB4
    public final static int USB_CDC_PRODUCT_ID = 0x5740; // PID while in CDC mode 0x09FF
    /* USB DFU ID's (may differ by device) */
    public final static int USB_DFU_bInterfaceClass = 0xFE;   /* bInterfaceClass: Application Specific Class Code */
    public final static int USB_DFU_bInterfaceSubClass = 0x01; /* bInterfaceSubClass : Device Firmware Upgrade Code */
    public final static int USB_DFU_nInterfaceProtocol = 0x02; /* nInterfaceProtocol: DFU mode protocol */
    /* USB CDC ID's (may differ by device) */
    public final static int USB_CDC_bInterfaceClass = 0x0A;   /* bInterfaceClass: Application Specific Class Code */
    public final static int USB_CDC_bInterfaceSubClass = 0x00; /* bInterfaceSubClass : Device Firmware Upgrade Code */
    public final static int USB_CDC_nInterfaceProtocol = 0x00; /* nInterfaceProtocol: CDC mode protocol */


    public static final String HTC_ACTION_USB_PERMISSION = "com.htc.chirp.fota.USB_PERMISSION";

    private ccg4 mCCG4;
    //private FotaService fService=null;
    private FotaServiceImpl m_impl=null;
    private UsbCdcTunnel mUsbCdcTunnel = null;




    /* Callback Interface */
    public interface OnUsbChangeListener {
        void on_Connected(int device, boolean isConnected, int type);
    }

    public void setOnUsbChangeListener(OnUsbChangeListener l) {
        mOnUsbChangeListener = l;
    }

    public OnUsbChangeListener mOnUsbChangeListener;

    public UsbDevice getUsbDevice() {
        return mfotaDevice;
    }

    /* Broadcast Receiver*/
    private final BroadcastReceiver mFotaUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,"receive action="+action);
            if (HTC_ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    mfotaDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (mfotaDevice != null) {
                            openDevice(mfotaDevice);
                            if((mfotaDevice.getProductId() == USB_CDC_PRODUCT_ID && mfotaDevice.getVendorId() == USB_CDC_VENDOR_ID)){
                                tryClaimDevice(mfotaDevice);
                            }
                        }
                    } else {
                        Log.d(TAG, "usb permission fail " + mfotaDevice);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                request_Permission(mContext, USB_CDC_VENDOR_ID, USB_CDC_PRODUCT_ID);
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if((device.getProductId() == USB_CDC_PRODUCT_ID && device.getVendorId() == USB_CDC_VENDOR_ID) ){
                        Log.i(TAG,"detached cdc ");
                        release();
                        mfotaDevice = null;
                        mOnUsbChangeListener.on_Connected(m_impl.curret_device, false, 1);
                    }
                }
            }
        }
    };


    public BroadcastReceiver getmUsbReceiver() {
        return mFotaUsbReceiver;
    }

    public Usb(Context context, FotaServiceImpl fImpl) {
        mContext = context;
        //fService = (FotaService)context;
        m_impl=fImpl;
        mUsbCdcTunnel = new UsbCdcTunnel();
        mfotaUsbManager =(UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        mCCG4 =new ccg4(this,context);

        // Handle case where USB device is connected before app launches;
        // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
        request_Permission(mContext, Usb.USB_CDC_VENDOR_ID, Usb.USB_CDC_PRODUCT_ID);
    }



    public void request_Permission(Context context, int vendorId, int productId) {
        // Setup Pending Intent
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(Usb.HTC_ACTION_USB_PERMISSION), 0);
        UsbDevice device = getUsbDevice(vendorId, productId);
        Log.i(TAG,"device  = " + device);
        if (device != null) {
            mfotaDevice = device;
            if (!mfotaUsbManager.hasPermission(mfotaDevice)) {
                Log.i(TAG, "requestPermission: vid = " + vendorId + ", pid = " + productId);
                mfotaUsbManager.requestPermission(device, permissionIntent);
            }else{
                Log.i(TAG, "requestPermission has get");
                if(mfotaConnection == null) {
                    openDevice(mfotaDevice);
                    if ((mfotaDevice.getProductId() == USB_CDC_PRODUCT_ID && mfotaDevice.getVendorId() == USB_CDC_VENDOR_ID)) {
                        tryClaimDevice(mfotaDevice);
                    }
                }else{
                    Log.d(TAG,"device has opened ");
                }
            }
        }
    }

    private UsbDevice getUsbDevice(int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceList = mfotaUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        UsbDevice device;
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return device;
            }
        }
        return null;
    }

    public boolean release() {
        boolean ret = false;
        USB_STATE = 0;
        if (mfotaConnection != null) {
            Log.i(TAG, "release: release mfotaConnection is not null");
            if(mfotaInterface != null) {
                ret = mfotaConnection.releaseInterface(mfotaInterface);
                mfotaInterface = null;
            }
            mfotaConnection.close();
            mfotaConnection = null;
        }

        Log.i(TAG,"release success");
        return ret;
    }

    public void openDevice(UsbDevice device) {
        if (device != null) {
            UsbDeviceConnection connection = mfotaUsbManager.openDevice(device);
            if (connection != null ) {
                Log.i(TAG, "openDevice: open device SUCCESS");
                mfotaConnection = connection;
            } else {
                Log.e(TAG, "openDevice: open device FAIL");
                mfotaConnection = null;
            }
        }
        m_impl.curret_device = 1;
    }

    //make sure usb if is connected
    public boolean UsbIsConnected() {
        return (mfotaConnection != null);
    }

    // set up Usb Endpoint connection
    public static UsbEndpoint getDirEndpoint(UsbInterface usbInterface, int dir) {
        if (usbInterface == null) {
            return null;
        }

        for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
            UsbEndpoint ep = usbInterface.getEndpoint(j);
            if (dir ==  ep.getDirection()) {
                return ep;
            }
        }

        return null;
    }

    // Set up input endpoint
    public static String readFromEndpoit(UsbDeviceConnection connection, UsbEndpoint usbEndpoint, boolean inHex) {
        if (connection == null || usbEndpoint == null) {
            return null;
        }

        int inMax = usbEndpoint.getMaxPacketSize();
        byte buffer[] = new byte[inMax];
        int count = connection.bulkTransfer(usbEndpoint, buffer, buffer.length, 200);
        StringBuilder sb = new StringBuilder();
        if (count > 0) {
            if (inHex) {
                for (int i = 0; i < count; i++) {
                    sb.append(" 0x" + Integer.toHexString(buffer[i]));
                }
            } else {
                try {
                    sb.append(new String(buffer, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }

        return null;
    }

    // Set up output endpoint
    public static int sendToEndpoint(UsbDeviceConnection connection, UsbEndpoint usbEndpoint, byte buffer[]) {
        int count = -1;
        if (connection == null || usbEndpoint == null) {
            return -1;
        }

        count = connection.bulkTransfer(usbEndpoint, buffer, buffer.length, 200);

        return count;
    }

    //Set up Usb Control Transfer
    public int UsbControlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout) {
        synchronized (this) {
            if(mfotaConnection != null) {
                return mfotaConnection.controlTransfer(requestType, request, value, index, buffer, length, timeout);
            }
            else
                return  0;
        }
    }




    public void tryClaimDevice(UsbDevice device) {
        UsbInterface mInterface;
        try {
            boolean state = false;
            m_impl.curret_device = 1;
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                mInterface = device.getInterface(i);
                if (mInterface.getInterfaceClass() == Usb.USB_CDC_bInterfaceClass && mInterface.getInterfaceSubclass() == Usb.USB_CDC_bInterfaceSubClass && mInterface.getInterfaceProtocol() == Usb.USB_CDC_nInterfaceProtocol) {
//                    Log.i(TAG, "find cdc usb interface.");
                    if (mfotaConnection != null && mfotaConnection.claimInterface(mInterface, false)) {
                        mfotaInterface = mInterface;
                        mEpIn = Usb.getDirEndpoint(mInterface, 128);
                        mEpOut = Usb.getDirEndpoint(mInterface, 0);
                        mfotaConnection.releaseInterface(mfotaInterface);
                        mUsbCdcTunnel.SetupUsbInterface(mfotaDevice,mfotaConnection);
                        if (mEpIn == null || mEpOut == null ) {
                            Log.i(TAG, "mEpIn/mEpOut  == null");
                        }
                        Log.i(TAG, "cdc connect ok id=" +i);

                    }
                    Usb.USB_STATE = 1;//cdc mode
                    m_impl.DEVICE_STATE = true;
                    break;
                } else if (mInterface.getInterfaceClass() == Usb.USB_DFU_bInterfaceClass && mInterface.getInterfaceSubclass() == Usb.USB_DFU_bInterfaceSubClass && mInterface.getInterfaceProtocol() == Usb.USB_DFU_nInterfaceProtocol) {
                    Usb.USB_STATE = 2;//dfu mode
                    break;
                } else {
                    Usb.USB_STATE = 3;//other mode
                }
            }

            mOnUsbChangeListener.on_Connected(m_impl.curret_device, m_impl.DEVICE_STATE, Usb.USB_STATE);
            if (Usb.USB_STATE == 0) {
                Log.i(TAG, "Can't find usb interface.");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int update_CCG4_and_show_dlg() throws InterruptedException{
        int ret =updateCCG4();
        if(13 == ret){
            return 13; ////mcu reboot , don't show dialog
        }
        mCCG4.show_status_dlg(ret);
        return ret;
    }


    public int updateCCG4() throws InterruptedException{
        boolean update_fw1_fw2=false;
        int retryCount = 10;
        while(retryCount-- >0 ){
            int ret= mCCG4.updateFW();
            if (ret == 0) {
                update_fw1_fw2=true;
                break;
            }else if(11==ret ||  12==ret ){
                Log.i(TAG, "ret="+ret);
            }else if (13 == ret){
                return 13;
            }
            //// todo  timeout
        }
        if (!update_fw1_fw2) {
            Log.i(TAG, "update CCG4 fw1/fw2 fail!");
            return -1;
        }
        if(! mCCG4.send_query_pkg() ){
            Log.i(TAG, "query pkg fail!");
            return -1;
        }
        Log.i(TAG, "update CCG4 all ok");
        return 0;
    }

    public boolean RequestCdcData(UsbTunnelData Data) {
        return mUsbCdcTunnel.RequestSingleCdcData(Data);
    }


    public String GetSysProperty(int item)
    {
        String RetString = null;
        UsbTunnelData Data = new UsbTunnelData();
        Data.send_array[0] = 'd';
        Data.send_array[1] = 0;
        Data.send_array[2] = (byte)item;
        Data.send_array_count = 3;
        Data.recv_array_count = Data.recv_array.length;
        Data.wait_resp_ms = 2;

        if (mUsbCdcTunnel.RequestSingleCdcData(Data) == true) {
            try {
                RetString = new String(Data.recv_array, 0, Data.recv_array_count, "UTF-8");
                //Log.d(TAG, "recv __jh__1 str="+RetString);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(RetString == null){
                Log.w(TAG, "RetString format is error!");
                return null;
            }
            RetString = RetString.replaceAll("[^[:print:]]", "");
        }
        Log.d(TAG, "recv __jh__2 str="+RetString);
        return RetString;
    }

}
