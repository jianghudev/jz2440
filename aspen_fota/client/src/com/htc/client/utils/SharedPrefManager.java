package com.htc.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.htc.client.controllerscanner.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chihhang_chuang on 2017/10/27.
 */

/*
* Using SharedPreferences to record whether the controller need to do FOTA or not.
* Do FOTA when head device reboot.
**/

public class SharedPrefManager {
    private static final String TAG = "SharedPrefManager";
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEdit;

    private final String SHARED_PREF_CONTROLLER_UTILITY = "controllerutility";
    private final String CONTROLLER_MAC_ADDRESS = "controllerMacAddress";
    private final String DO_FOTA = "doFota";
    private final String PAIR_DEVICE = "pair_device";
    private final String PAIRING_BEFORE_OOBE = "pairingBeforeOobe";

    private final String DFU_MAC_ADDRESS = "dfu_mac_address";
    private final String FINCH_MODE_NUMBER = "finch_mode_number";
    private final String FINCH_VERSION = "finch_version";

    Context mContext;

    public SharedPrefManager(Context context) {
        mContext = context;
        mSharedPref = mContext.getSharedPreferences(SHARED_PREF_CONTROLLER_UTILITY, Context.MODE_PRIVATE);
        mSharedPrefEdit = mSharedPref.edit();
    }

    public void setMacAddress(String macAddress) {
        Logger.d(TAG, "[setMacAddress] : " + hashAddress(macAddress));
        mSharedPrefEdit.putString(PAIR_DEVICE, macAddress);
        mSharedPrefEdit.apply();
    }

    public String getPairMacAddress() {
        String address = "";
        address = mSharedPref.getString(PAIR_DEVICE, "");
        return address;
    }

    public void setControllerInfo(String macAddress, boolean doFota) {
        Logger.d(TAG, "setControllerInfo: " + hashAddress(macAddress) + " doFota: " + doFota);
        mSharedPrefEdit.putString(CONTROLLER_MAC_ADDRESS, macAddress);
        mSharedPrefEdit.putBoolean(DO_FOTA, doFota);
        mSharedPrefEdit.apply();
    }
    /*
    * key: MAC address
    * value: doFota
    * */
    public Map<String, Boolean> getControllerInfo() {
        Logger.d(TAG, "get recent bond Controller Info");
        Map<String, Boolean> controllerInfoMap = new ConcurrentHashMap<>();
        String address = mSharedPref.getString(CONTROLLER_MAC_ADDRESS, null);
        boolean doFota = mSharedPref.getBoolean(DO_FOTA, false);
        if (null == address) {
            return null;
        }
        controllerInfoMap.put(address, doFota);
        return controllerInfoMap;
    }

    public boolean isPairingBeforeOobe() {
        boolean isPairingBeforeOobe = mSharedPref.getBoolean(PAIRING_BEFORE_OOBE, true);
        if (isPairingBeforeOobe) {
            mSharedPrefEdit.putBoolean(PAIRING_BEFORE_OOBE,false);
            mSharedPrefEdit.apply();
        }
        return isPairingBeforeOobe;
    }

    public void setDFUMacAddress(String addr) {
        Logger.d(TAG, "[setDFUMacAddress] addr : " + hashAddress(addr));
        try {
            mSharedPrefEdit.putString(DFU_MAC_ADDRESS, addr);
            mSharedPrefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDFUMacAddress() {
        String address = "";
        try {
            address = mSharedPref.getString(DFU_MAC_ADDRESS, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    public void setFinchVersion(String version) {
        Logger.d(TAG, "[setFinchVersion] version : " + version);
        try {
            mSharedPrefEdit.putString(FINCH_VERSION, version);
            mSharedPrefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFinchVersion() {
        String version = "";
        try {
            version = mSharedPref.getString(FINCH_VERSION, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    public void setFinchModelNumber(String modelNumber) {
        Logger.d(TAG, "[setFinchModelNumber] modelNumber : " + modelNumber);
        try {
            mSharedPrefEdit.putString(FINCH_MODE_NUMBER, modelNumber);
            mSharedPrefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFinchModelNumber() {
        String modelNumber = "";
        try {
            modelNumber = mSharedPref.getString(FINCH_MODE_NUMBER, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modelNumber;
    }

    private String hashAddress(String address) {
        String hashString = "";
        try {
            if (!TextUtils.isEmpty(address)) {
                String list[] = address.split(":");
                hashString = list[list.length - 1] + list[list.length - 2];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashString;
    }
}
