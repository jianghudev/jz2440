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

    final static String TAG = "3DoF_HMD.FOTA.Usb";
    public static int USB_STATE       = 0;//cdc:USB_STATE=1;dfu:USB_STATE=2;
    private Context mContext;

    public UsbManager mfotaUsbManager;
    private UsbDevice mfotaDevice;
    public UsbDeviceConnection mfotaConnection;
    public UsbInterface mfotaInterface;
    //private int mDeviceVersion;
    public UsbEndpoint mEpIn;
    public UsbEndpoint mEpOut;

    /* USB DFU ID's (may differ by device) */
    public final static int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
    public final static int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11
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


    private boolean USBPermissionDialogIsLaunch = false;

    public static final String HTC_ACTION_USB_PERMISSION = "com.htc.chirp.fota.USB_PERMISSION";

    //private FotaService fService=null;
    private FotaServiceImpl m_impl=null;

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
                            setUsbDevice(mfotaDevice);
                            if((mfotaDevice.getProductId() == USB_PRODUCT_ID && mfotaDevice.getVendorId() == USB_VENDOR_ID)){
                                tryClaimDevice(mfotaDevice);
                            }
                        }
                    } else {
                        Log.d(TAG, "usb permission fail " + mfotaDevice);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    Log.i(TAG,"receive ACTION_USB_DEVICE_ATTACHED");
                    //requestPermission(mContext, USB_VENDOR_ID, USB_PRODUCT_ID);
                    requestPermission(mContext, USB_CDC_VENDOR_ID, USB_CDC_PRODUCT_ID);
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                    Log.i(TAG,"usb device detached PID = " + device.getProductId() + "VID = " + device.getVendorId());
                    if((device.getProductId() == USB_CDC_PRODUCT_ID && device.getVendorId() == USB_CDC_VENDOR_ID) ){
                        //request permission to detach USB Device
//                        mUsbPermission = true;
                        Log.i(TAG,"detched cdc / dfu");
                        USBPermissionDialogIsLaunch = false;
                        boolean result = release();
                        mfotaDevice = null;
                        mOnUsbChangeListener.on_Connected(m_impl.curret_device, false, Usb.USB_STATE);
                        if (result){
                            Log.i(TAG,"release success");
                        }
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
        mfotaUsbManager =(UsbManager) mContext.getSystemService(Context.USB_SERVICE);



        // Handle case where USB device is connected before app launches;
        // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
        requestPermission(mContext, Usb.USB_CDC_VENDOR_ID, Usb.USB_CDC_PRODUCT_ID);
    }



    public void requestPermission(Context context, int vendorId, int productId) {
        // Setup Pending Intent
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(Usb.HTC_ACTION_USB_PERMISSION), 0);
        UsbDevice device = getUsbDevice(vendorId, productId);
//        Log.i(TAG,"device  = " + device);
        if (device != null) {
            mfotaDevice = device;
            if (!mfotaUsbManager.hasPermission(device)) {
                Log.i(TAG, "requestPermission: vid = " + vendorId + ", pid = " + productId);
                if (USBPermissionDialogIsLaunch == false) {
                    mfotaUsbManager.requestPermission(device, permissionIntent);
                    USBPermissionDialogIsLaunch = true;
                } else {
                    Log.i(TAG, "requestPermission: USB Permission dialog has been launch");
                }
            }else{
                Log.i(TAG, "requestPermission has get");
                if(mfotaConnection == null) {
                    setUsbDevice(mfotaDevice);
                    if ((mfotaDevice.getProductId() == USB_PRODUCT_ID && mfotaDevice.getVendorId() == USB_VENDOR_ID)) {
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
//            Log.i(TAG,"device vid = " + device.getVendorId() + "device pid = " + device.getProductId());
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return device;
            }
        }
        return null;
    }

    public boolean release() {
        boolean conReleased = false;
        USB_STATE = 0;
//        Log.i(TAG, "release mfotaConnection is  null");
        if (mfotaConnection != null) {
            Log.i(TAG, "release: release mfotaConnection is not null");
            if(mfotaInterface != null) {
                conReleased = mfotaConnection.releaseInterface(mfotaInterface);
                mfotaInterface = null;
            }
            mfotaConnection.close();
            mfotaConnection = null;
        }

        return conReleased;
    }

    public void setUsbDevice(UsbDevice device) {
        if (device != null) {
            UsbDeviceConnection connection = mfotaUsbManager.openDevice(device);
            if (connection != null ) {
                Log.i(TAG, "setUsbDevice: open device SUCCESS");
                mfotaConnection = connection;
            } else {
                Log.e(TAG, "setUsbDevice: open device FAIL");
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
                    if (mfotaConnection != null && mfotaConnection.claimInterface(mInterface, true)) {
                        mfotaInterface = mInterface;
                        mEpIn = Usb.getDirEndpoint(mInterface, 128);
                        mEpOut = Usb.getDirEndpoint(mInterface, 0);
                        mfotaConnection.releaseInterface(mfotaInterface);
                        if (mEpIn == null || mEpOut == null ) {
                            Log.i(TAG, "mEpIn/mEpOut  == null");
                        }
                        Log.i(TAG, "cdc connect ok");

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
            if (state == false) {
                Log.i(TAG, "Can't find usb interface.");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }




}
