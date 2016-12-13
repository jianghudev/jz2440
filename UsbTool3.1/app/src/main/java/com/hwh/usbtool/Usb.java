package com.hwh.usbtool;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

public class Usb {
    final static String TAG = "USB";

    /* USB DFU ID's (may differ by device) */
    public final static int USB_VENDOR_ID  = 0x0483;   // VID while in DFU mode 0x0483
    public final static int USB_PRODUCT_ID = 0xFFE1;   // PID while in DFU mode 0xDF11

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    //Endpoint type
    public final static int USB_ENDPOINT_XFER_CONTROL  = 0;// --控制传输
    public final static int USB_ENDPOINT_XFER_ISOC      =1;// --等时传输
    public final static int USB_ENDPOINT_XFER_BULK      = 2;// --块传输
    public final static int USB_ENDPOINT_XFER_INT       = 3;// --中断传输

    public final static int USB_HTC_COMP_IF_CDC_CONTROL = 5;
    public final static int USB_HTC_COMP_IF_CDC_DATA     = 1;

    public final static int USB_HTC_COMP_DATA_EP_IN      = 129;
    public final static int USB_HTC_COMP_DATA_EP_OUT     = 1;

    public final static int USB_HTC_COMP_IF_HID          = 0;
    public final static int USB_HTC_HID_DATA_EP_IN       = 129;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getDeviceAllInfo(UsbDevice device, UsbDeviceConnection connection) {
        if (device == null) {
            return "No device found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Manufacturer: " + device.getManufacturerName() + "\n");
        sb.append("ProductName: " + device.getProductName() + "\n");
        sb.append("SerialNumber: " + device.getSerialNumber() + "\n");
        sb.append("Vendor ID " + device.getVendorId() + " (0x" + Integer.toHexString(device.getVendorId()) + ")" + "\n");
        sb.append("Product ID: " + device.getProductId() + " (0x" + Integer.toHexString(device.getProductId()) + ")" + "\n");
        sb.append("----------\n");
        sb.append("Class: " + device.getDeviceClass() + "\n");
        sb.append("Subclass: " + device.getDeviceSubclass() + "\n");
        sb.append("Protocol: " + device.getDeviceProtocol() + "\n");
        sb.append("DeviceName: " + device.getDeviceName() + "\n");
        sb.append("DeviceID: " + device.getDeviceId() + " (0x" + Integer.toHexString(device.getDeviceId()) + ")" + "\n");

        sb.append("==========================\n");
        sb.append("ConfigurationCount: " + device.getConfigurationCount() + "\n");
        for (int m = 0; m < device.getConfigurationCount(); m++) {
            UsbConfiguration usbConfiguration = device.getConfiguration(m);
            //sb.append("-Configuration: " + usbConfiguration.toString() + "\n");
            sb.append("-Configuration ID: " + usbConfiguration.getId() + "\n");
            sb.append("-Configuration Name: " + usbConfiguration.getName() + "\n");
            sb.append("-Configuration MaxPower: " + usbConfiguration.getMaxPower() + "\n");
            sb.append("-Configuration InterfaceCount: " + usbConfiguration.getInterfaceCount() + "\n");
            sb.append("-Configuration isSelfPowered: " + usbConfiguration.isSelfPowered() + "\n");
            sb.append("-Configuration isRemoteWakeup: " + usbConfiguration.isRemoteWakeup() + "\n"); // suspend by HOST
            sb.append("--------------\n");
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface usbInterface = device.getInterface(i);
                //sb.append("--Interface: " + usbInterface.toString() + "\n");
                sb.append("--Interface Name: " + usbInterface.getName() + "\n");
                sb.append("--Interface ID: " + usbInterface.getId() + "\n");
                sb.append("--Interface Class: " + usbInterface.getInterfaceClass() + "\n");
                sb.append("--Interface SubClass: " + usbInterface.getInterfaceSubclass() + "\n");
                sb.append("--Interface Protocol: " + usbInterface.getInterfaceProtocol() + "\n");
                sb.append("--Interface AlternateSetting: " + usbInterface.getAlternateSetting() + "\n");
                sb.append("--Interface EndpointCount: " + usbInterface.getEndpointCount() + "\n");
                sb.append("--------------\n");
                for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                    UsbEndpoint ep = usbInterface.getEndpoint(j);
                    //sb.append("---Endpoint: " + ep.toString() + "\n");
                    sb.append("---Endpoint EndpointNumber: " + ep.getEndpointNumber() + "\n");
                    sb.append("---Endpoint Address: " + ep.getAddress() + "\n");
                    sb.append("---Endpoint Direction: " + ep.getDirection() + "\n");
                    sb.append("---Endpoint MaxPackageSize: " + ep.getMaxPacketSize() + "\n");
                    sb.append("---Endpoint Interval: " + ep.getInterval() + "\n");
                    sb.append("---Endpoint Attributes: " + ep.getAttributes() + "\n");
                    sb.append("---Endpoint Type: " + ep.getType() + "\n");
                }
            }
        }
        return sb.toString();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getDeviceInfo(UsbDevice device) {
        if (device == null) {
            return "No Device Info";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Manufacturer: " + device.getManufacturerName() + "\n");
        sb.append("ProductName: " + device.getProductName() + "\n");
        sb.append("SerialNumber: " + device.getSerialNumber() + "\n");
        sb.append("Vendor ID " + device.getVendorId() + " (0x" + Integer.toHexString(device.getVendorId()) + ")" + "\n");
        sb.append("Product ID: " + device.getProductId() + " (0x" + Integer.toHexString(device.getProductId()) + ")" + "\n");
        sb.append("Class: " + device.getDeviceClass() + "\n");
        sb.append("Subclass: " + device.getDeviceSubclass() + "\n");
        sb.append("Protocol: " + device.getDeviceProtocol() + "\n");
        sb.append("DeviceName: " + device.getDeviceName() + "\n");
        sb.append("DeviceID: " + device.getDeviceId() + " (0x" + Integer.toHexString(device.getDeviceId()) + ")" + "\n");
        sb.append("==========================\n");
        sb.append("ConfigurationCount: " + device.getConfigurationCount() + "\n");
        for (int m = 0; m < device.getConfigurationCount(); m++) {
            UsbConfiguration usbConfiguration = device.getConfiguration(m);
            //sb.append("-Configuration: " + usbConfiguration.toString() + "\n");
            sb.append("-Configuration ID: " + usbConfiguration.getId() + "\n");
            sb.append("-Configuration Name: " + usbConfiguration.getName() + "\n");
            sb.append("-Configuration MaxPower: " + usbConfiguration.getMaxPower() + "\n");
            sb.append("-Configuration InterfaceCount: " + usbConfiguration.getInterfaceCount() + "\n");
            sb.append("-Configuration isSelfPowered: " + usbConfiguration.isSelfPowered() + "\n");
            sb.append("-Configuration isRemoteWakeup: " + usbConfiguration.isRemoteWakeup() + "\n"); // suspend by HOST
            sb.append("--------------\n");
        }
        return sb.toString();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getInterfaceInfo(UsbInterface usbInterface) {
        if (usbInterface == null) {
            return "No Interface Info";
        }

        StringBuilder sb = new StringBuilder();
        //sb.append("--Interface: " + usbInterface.toString() + "\n");
        sb.append("--Interface Name: " + usbInterface.getName() + "\n");
        sb.append("--Interface ID: " + usbInterface.getId() + "\n");
        sb.append("--Interface Class: " + usbInterface.getInterfaceClass() + "\n");
        sb.append("--Interface SubClass: " + usbInterface.getInterfaceSubclass() + "\n");
        sb.append("--Interface Protocol: " + usbInterface.getInterfaceProtocol() + "\n");
        sb.append("--Interface AlternateSetting: " + usbInterface.getAlternateSetting() + "\n");
        sb.append("--Interface EndpointCount: " + usbInterface.getEndpointCount() + "\n");
        sb.append("--------------\n");
        for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
            UsbEndpoint ep = usbInterface.getEndpoint(j);
            //sb.append("---Endpoint: " + ep.toString() + "\n");
            sb.append("---Endpoint EndpointNumber: " + ep.getEndpointNumber() + "\n");
            sb.append("---Endpoint Address: " + ep.getAddress() + "\n");
            sb.append("---Endpoint Direction: " + ep.getDirection() + "\n");
            sb.append("---Endpoint MaxPackageSize: " + ep.getMaxPacketSize() + "\n");
            sb.append("---Endpoint Interval: " + ep.getInterval() + "\n");
            sb.append("---Endpoint Attributes: " + ep.getAttributes() + "\n");
            sb.append("---Endpoint Type: " + ep.getType() + "\n");
        }

        return sb.toString();
    }

    public static UsbDevice getUsbDevice(UsbManager mgr, int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceList = mgr.getDeviceList();
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

    public static UsbInterface getInterface(UsbDevice device, int IfId) {
        if (device == null) {
            return null;
        }

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (IfId == usbInterface.getId()) {
                return usbInterface;
            }
        }

        return null;
    }

    public static UsbEndpoint getEndpoint(UsbDevice device, int IfId, int addr) {
        if (device == null) {
            return null;
        }

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (IfId == usbInterface.getId()) {
                for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                    UsbEndpoint ep = usbInterface.getEndpoint(j);
                    if (addr ==  ep.getAddress()) {
                        return ep;
                    }
                }
            }
        }

        return null;
    }

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

    public static String getStringDescriptor(UsbDeviceConnection connection, int idx) {
        String desStr = new String("null");;
        if (connection == null) {
            return desStr;
        }

        byte buffer[] = new byte[100];
        byte strBuf[] = new byte[100];
        int ret = connection.controlTransfer(0x80, 0x06, 0x300 + idx, 0, buffer, 0, 100, 1000);
        StringBuilder sb = new StringBuilder();
        Log.i(TAG, "getStringDescriptor : GET_PRODUCT_STR return " + ret + "\n");
        if (ret > 2) {
            try {
                for (int i = 0; i < ret - 2; i++) {
                    strBuf[i] = buffer[i + 2];
                }
                desStr = new String(strBuf, "UnicodeLittleUnmarked");
            } catch (java.io.UnsupportedEncodingException ex) {
                Log.i(TAG, "getStringDescriptor : UnsupportedEncodingException\n");
            }
        }

        return desStr;
    }

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

    public static int sendToEndpoint(UsbDeviceConnection connection, UsbEndpoint usbEndpoint, byte buffer[]) {
        int count = -1;
        if (connection == null || usbEndpoint == null) {
            return -1;
        }

        count = connection.bulkTransfer(usbEndpoint, buffer, buffer.length, 200);

        return count;
    }

}
