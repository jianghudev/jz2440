package com.htc.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.htc.client.vr.BleDevInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 2016/8/23.
 */
public class FirmwareUpdateUtils {
    private static final String TAG = "AspenFota.client";
    private static Context mContext;

    public static final String CONNECT_MEDIA_UNKNOWN = "unknown";
    public static final String CONNECT_MEDIA_WIFI = "Wifi";
    public static final String CONNECT_MEDIA_WIMAX = "WiMax";
    public static final String CONNECT_MEDIA_RADIO = "radio";

    private static final String PREFIX_PRE = "cardboard_";
    private static final String FIRMWARE_UPDATE_KEY = PREFIX_PRE + "firmware_update_key";
    private static final String KEY_DIGEST = "key_digest";
    private static final String KEY_FIRST_CHECKIN = "key_first_checkin";
    private static final String KEY_DEVICE_INFO = "key_device_info";
    private static final String KEY_IMEI_INFO = "key_imei_info";
    private static final String KEY_FIRMWARE_IMAGEID = "key_firmware_imageID";
    private static final String KEY_CHECKIN_INTERVAL = "key_checkin_interval";
    private static final String KEY_FIRMWARE_VERSION = "key_firmware_version";
    private static final String KEY_FIRMWARE_FEATURE = "key_firmware_feature";
    private static final String KEY_CURRENT_FIRMWARE_VERSION = "key_current_firmware_version";
    private static final String KEY_CB_FOTA_SIZE = "key_cb_fota_size";
    private static final String KEY_FIRMWARE_SIZE = "key_firmware_size";
    private static final String KEY_MD5 = "key_md5";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";
    private static final String KEY_FOTA_STATUS = "key_fota_status";
    private static final String KEY_HAVE_FIRMWARE_UPDATE = "key_have_firmware_update";
    private static final String PREFERENCE_KEY_SEND_FAIL_MESSAGE = "send_fail_message";
    private static final String PREFERENCE_KEY_FIRMWARE_STATUS = "firmware_status";
    private static final String PREFERENCE_KEY_FIRMWARE_CHECK_TIME = "firmware_check_time";
    private static final String PREFERENCE_KEY_FIRMWARE_UPDATE_COMPLETE_TIME = "firmware_update_complete_time";
    private static final String PREFERENCE_KEY_START_FOTA_CHECK = "start_firmware_check";
    private static final String PREFERENCE_KEY_START_FOTA_CHECK_TIME = "start_firmware_check_time";
    private static final String PREFERENCE_KEY_GLOBAL_VERSION = "global_version";
    private static final String PREFERENCE_KEY_HMD_VERSION = "hmd_version";
    private static final String PREFERENCE_KEY_3DOF_VERSION = "3dof_version";
    private static final String PREFERENCE_KEY_DONGLE_VERSION = "dongle_version";
    private static final String PREFERENCE_KEY_CTRL_L_VERSION = "ctrl_l_version";
    private static final String PREFERENCE_KEY_CTRL_R_VERSION = "ctrl_r_version";
    private static final String PREFERENCE_KEY_CAMERA_VERSION = "camera_version";
    private static final String PREFERENCE_KEY_DEVICE_SERVER_VERSION_MESSAGE = "device_server_version";

    private static final String HTTP_POST = "POST";
    private static final String CONTENT_LEN = "Content-Length";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String FOTA_SERVER_CHECK_KEY = "x-active-g";
    private static final String FOTA_SERVER_CHECK_VALUE = "DivadGS38Omatump76";

    // Send EXTRA MSG STATUS
    // Error code from Download Manager
    public static final String DOWNLOAD_FAIL_MSG_ERROR_CODE_1 = "-1";
    // Download with security token but failed; maybe token expired or invalid token.
    public static final String DOWNLOAD_FAIL_MSG_ERROR_CODE_2 = "-2";
    // Generic error
    public static final String DOWNLOAD_FAIL_MSG_ERROR_CODE_3 = "-3";
    // Upload file error
    public static final String INSTALL_FAILED_MSG_ERROR_CODE_1 = "-1";
    // Generic error
    public static final String INSTALL_FAILED_MSG_ERROR_CODE_2 = "-2";
    // Upgrade error
    public static final String INSTALL_FAILED_MSG_ERROR_CODE_3 = "-3";

    public static final String FIRST_SUCCESS_CHECKIN_MSG = "FIRST_SUCCESS_CHECKIN";
    public static final String INSTALL_SUCCESS_MSG = "INSTALL_SUCCESS";
    public static final String VERIFY_SUCCESS_MSG = "VERIFY_SUCCESS";
    public static final String INSTALL_FAILED_MSG = "INSTALL_FAILURE";

    public static final String ACCESS_FAIL_MSG = "ACCESS_FAIL";
    public static final String DOWNLOAD_CANCEL_MSG = "Download cancel";
    public static final String DOWNLOAD_FAIL_MSG = "Download fail";

    public static final String CHECKIN_URL = "https://andchin-2.htc.com/htcfotacheckin/rest/checkin";
    public static final String EXTRA_MSG_URL = "https://fotamsg-2.htc.com/htcfotacheckin/rest/updateMsg";

    public static final int FOTA_DO_NOTHING = 0;
    public static final int FOTA_NO_UPDATE = 1;
    public static final int FOTA_UPDATE_AVAILABLE = 2;
    public static final int FOTA_INSTALLING_UPDATE = 3;
    public static final int FOTA_CHECKING_UPDATE = 4;
    public static final int FOTA_UPDATE_FAIL = 5;
    public static final int FOTA_UPDATE_COMPLETED = 6;

    private static final String DEFAULT_UPDATE_VERSION = "0.0.005.0";

    private static final boolean Htc_SECURITY_DEBUG_flag = true;

    public FirmwareUpdateUtils(Context context) {
        mContext = context;
    }

    private String mImei = "";


    public String createCheckinJSON(boolean isAuto, Bundle info) {
        if (mContext == null || info == null) {
            Log.w(TAG, "createCheckinJSON context is null!");
            return "";
        }
        String deviceVesrion = info.getString(FotaServiceContract.DEVICE_VERSION);
        String SN =  info.getString(FotaServiceContract.DEVICE_SERIAL_NAME);
        SN = SN.toLowerCase();
        String deviceModeNumber = info.getString(FotaServiceContract.DEVICE_MODEL_NAME);
        JSONObject request = new JSONObject();
        JSONObject checkin = new JSONObject();
        String deviceInfo = "";
        if (FotaServiceContract.DEBUG_CHECK_UPDATE) {

            deviceVesrion = "0.0.001.0";
            //SN = "fa74raj00030";
            SN = "123456789012345";
            deviceModeNumber = "2Q25100";
        }
        setCurrentFirmwareVersion(deviceVesrion);
        try {
            //String imei = getIMEI();
            /*String imei = "";
            if (device == FotaServiceContract.DEVICE_HMD) {
                imei = "ff:ff:ff:ff:ff:ff";
            } else {
                imei = "ff:ff:ff:ff:ff:22";
            }

            imei = imei.replace(":", "");
            imei = imei.toLowerCase();*/

            String imei = SN;
            mImei = SN;
            if (Htc_SECURITY_DEBUG_flag) {
                Log.d(TAG, "SN : " + imei);
            } else {
                try {
                    if (!TextUtils.isEmpty(imei)) {
                        Log.d(TAG, "SN size : " + imei.substring(imei.length() - 4, imei.length()) + " : " + imei.length());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            request.put("checkin", checkin);
            JSONObject build = new JSONObject();
            checkin.put("build", build);

            String firmwareVersion = deviceVesrion;
            /*if (device == FotaServiceContract.DEVICE_HMD) {
                firmwareVersion = "1.17.999.5";
            } else {
                firmwareVersion = "1.4.999.5";
            }*/

            Log.d(TAG, "WW Firmware version : " + firmwareVersion);
            if (!TextUtils.isEmpty(firmwareVersion)) {
                build.put("firmware_version", firmwareVersion);
                deviceInfo += firmwareVersion + ":";
            } else {
                build.put("firmware_version", "");
                deviceInfo += ":";
            }

            // for checkin.buld
            build.put("bootloader", "");

            String serialno = SN;
            if (!TextUtils.isEmpty(serialno)) {
                build.put("serialno", serialno);
                deviceInfo += serialno + ":";
            } else {
                build.put("serialno", "");
                deviceInfo += ":";
            }

            String buildType = "eng";
            if (!TextUtils.isEmpty(buildType)) {
                build.put("build_type", buildType);
                deviceInfo += buildType + ":";
            } else {
                build.put("build_type", "");
                deviceInfo += ":";
            }

            build.put("product", "MIAC");
            // for checkin
            String cid = "";
            if (!TextUtils.isEmpty(cid)) {
                checkin.put("cid", cid);
                deviceInfo += cid + ":";
            } else {
                checkin.put("cid", "");
                deviceInfo += ":";
            }

            checkin.put("client_version", "A1.0.1(VR)");

            String connMedia = getConnectMedia();
            checkin.put("connection_media", connMedia);

            String mid = deviceModeNumber;
            if (!TextUtils.isEmpty(mid)) {
                checkin.put("mid", mid);
                deviceInfo += mid + ":";
            } else {
                checkin.put("mid", "");
                deviceInfo += ":";
            }

            String mcc_mnc = getMNC_MCC();
            checkin.put("mcc_mnc", mcc_mnc);
            if (isAuto) {
                checkin.put("checkin_type", "Auto");
            } else {
                checkin.put("checkin_type", "Manual");
            }

            String sim_mcc_mnc = getSIM_MCC_MNC((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE));
            checkin.put("sim_mcc_mnc", sim_mcc_mnc);
            // for root
            String digest = getDigest();
            request.put("digest", digest);
            request.put("imei", imei);
            String locale = Locale.getDefault().toString();
            request.put("locale", locale);

            String moderNumber = deviceModeNumber;
            /*if (device == FotaServiceContract.DEVICE_HMD) {
                moderNumber = "card_board_test";
            } else {
                moderNumber = "card_board_ctrl_test";
            }*/

            if (!TextUtils.isEmpty(moderNumber)) {
                request.put("model_number", moderNumber);
                deviceInfo += moderNumber + ":";
            } else {
                request.put("model_number", "");
                deviceInfo += ":";
            }

            request.put("last_checkin_msec", System.currentTimeMillis()); // need store last checin time

            String timeZone = getTimeZoneText();
            Log.d(TAG, "timeZone : " + timeZone);
            if (timeZone != null) {
                request.put("timeZone", timeZone);
            } else {
                request.put("timeZone", "");
            }
            long timeStamp = System.currentTimeMillis();
            request.put("timeStamp", timeStamp);
            String flag = "0";
            if (!TextUtils.isEmpty(flag)) {
                request.put("mFlag", flag);
                deviceInfo += flag + ":";
            } else {
                request.put("mFlag", "");
                deviceInfo += ":";
            }

            if (!TextUtils.isEmpty(buildType)) {
                request.put("aaReport", buildType);
                deviceInfo += buildType;
            } else {
                request.put("aaReport", "");
                deviceInfo += "";
            }
            String client = "aos_ww";
            request.put("client", client);

            String x1 = getX1(client, imei, timeStamp);
            Log.d(TAG, "x1 : " + x1);

            Build bd = new Build();
            String model = bd.MODEL;
            String brand = bd.BRAND;

            if (!TextUtils.isEmpty(brand)) {
                request.put("productBrand", brand);
            } else {
                request.put("productBrand", "unknow");
            }

            if (!TextUtils.isEmpty(model)) {
                request.put("productModel", model);
            } else {
                request.put("productModel", "unknow");
            }

            request.put("x1", x1);
            if (Htc_SECURITY_DEBUG_flag) {
                Log.d(TAG, "=request= : " + request.toString());
                Log.d(TAG, "deviceInfo : " + deviceInfo);
            } else {
                try {
                    if (!TextUtils.isEmpty(mImei)) {
                        String temp = request.toString().replace(SN, SN.substring(SN.length() - 4, SN.length()));
                        Log.d(TAG, "=request= : " + temp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setDeviceInfo(deviceInfo);
            return request.toString();
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return "";
    }

    public String getConnectMedia() {
        if (mContext == null) {
            Log.w(TAG, "context is null !");
            return CONNECT_MEDIA_UNKNOWN;
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIMAX) {
                return CONNECT_MEDIA_WIMAX;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return CONNECT_MEDIA_WIFI;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return CONNECT_MEDIA_RADIO;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when get connected media :" + e);
        }
        return CONNECT_MEDIA_UNKNOWN;
    }

    /* form LINK_OOBE */
    /** check if the network is connected */
    public static boolean isNetworkConnected(final String TAG, Context context) {
        if (context == null) {
            Log.w(TAG, "context is null");
            return false;
        }

        boolean isConnected = false;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info != null) {
            isConnected = info.isConnected();
            Log.i(TAG, "isNetworkConnected: " + isConnected + ", type = " + info.getTypeName());
        }
        return isConnected;
    }
    /* form LINK_OOBE */
    /** check if the specified network type is connected */
    public static Boolean isNetworkConnected(final String TAG, Context context, int ConnectType) {
        if (context == null) {
            Log.w(TAG, "context is null");
            return false;
        }

        boolean isConnected = false;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info != null) {
            isConnected = (ConnectType == info.getType()) && info.isConnected();
            Log.i(TAG, info.getTypeName() + ", isNetworkConnected: " + isConnected);
        }
        return isConnected;
    }

    private String getMNC_MCC() {
        String mnc_mcc = "";
        if (mContext == null) {
            Log.w(TAG, "context is null");
            return mnc_mcc;
        }
        try {
            mnc_mcc = ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperator();
            if (mnc_mcc == null) {
                mnc_mcc = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mnc_mcc;
    }

    private String getSIM_MCC_MNC(TelephonyManager teleManager) {
        String sim_mcc_mnc = "";
        try {
            if (teleManager != null) {
                sim_mcc_mnc = teleManager.getSimOperator();
            }
        } catch (Exception e) {
            Log.e(TAG, "getSIM_MCC_MNC() Error:", e);
        }

        if (sim_mcc_mnc == null || sim_mcc_mnc.isEmpty()) {
            sim_mcc_mnc = "";
        }

        return sim_mcc_mnc;
    }

    public void setDigest(String digest) {
        if (mContext == null) {
            Log.d(TAG, "setDigest context is null.");
            return;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_DIGEST, digest).apply();
        } catch (Exception e) {
            Log.w(TAG, "setDigest fail!");
            Log.e(TAG, "", e);
        }
    }

    public String getDigest() {
        if (mContext == null) {
            Log.d(TAG, "getDigest context is null.");
            return "";
        }
        String digest = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            digest = prefs.getString(KEY_DIGEST, "");
        } catch (Exception e) {
            Log.w(TAG, "getDigest fail!");
            Log.e(TAG, "", e);
        }
        return digest;
    }

    private String getTimeZoneText() {
        TimeZone tz = Calendar.getInstance().getTimeZone();
        boolean daylight = tz.inDaylightTime(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append(formatOffset(tz.getRawOffset() + (daylight ? tz.getDSTSavings() : 0)));

        return sb.toString();
    }

    private char[] formatOffset(int off) {
        off = off / 1000 / 60;

        char[] buf = new char[9];
        buf[0] = 'G';
        buf[1] = 'M';
        buf[2] = 'T';

        if (off < 0) {
            buf[3] = '-';
            off = -off;
        } else {
            buf[3] = '+';
        }

        int hours = off / 60;
        int minutes = off % 60;

        buf[4] = (char) ('0' + hours / 10);
        buf[5] = (char) ('0' + hours % 10);

        buf[6] = ':';

        buf[7] = (char) ('0' + minutes / 10);
        buf[8] = (char) ('0' + minutes % 10);

        return buf;
    }

    private String getX1(String clientType, String imei, long timestamp) {
        String time_str = Long.toString(timestamp);

        if (TextUtils.isEmpty(clientType)) {
            clientType = "";
        }
        if (TextUtils.isEmpty(imei)) {
            imei = "";
        }
        if (TextUtils.isEmpty(time_str)) {
            time_str = "";
        }

        /*
         * MessageDigest md = null; try { md = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException ex) { ex.printStackTrace(); } catch
         * (Exception e){ Log.e(TAG, "Can't get message digest:" + e); }
         */

        // Get shift value
        int SKIP_COUNT = 2, shift_value = 0;
        for (int i = SKIP_COUNT; i <= time_str.length(); i++) {

            if (time_str.charAt(time_str.length() - i) != '0') {
                // shift_value = time_str.charAt(time_str.length() - i);
                shift_value = Character.getNumericValue(time_str.charAt(time_str.length() - i));

                break;
            }
        }

        // Shift the timestamp to make signature
        String time_head = "", time_tail = "";
        time_head = time_str.substring(0, time_str.length() - shift_value);
        time_tail = time_str.substring(time_str.length() - shift_value, time_str.length());

        // TIME(tail) + SN + IMEI + TIME(head)
        StringBuilder sb = new StringBuilder();
        sb.append(time_tail).append(clientType).append(imei).append(time_head);

        /*
         * md.update(sb.toString().getBytes()); BigInteger number = new BigInteger(1, md.digest()); sb.delete(0, sb.length()); sb.append(number.toString(16));
         * while (sb.length() < 32) { sb.insert(0, "0"); }
         */
        String x1 = sha256(sb.toString()).toUpperCase();
        // if(Const.LOG) Log.d(TAG, "The hashed signature is " + sb.toString().toUpperCase());


        return x1;
    }

    private String sha256(String base) {
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes());
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String doRequest(String url, String postMsg) throws Exception {
        URL urltest = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urltest.openConnection();

        conn.setRequestMethod(HTTP_POST);
        conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
        conn.setRequestProperty(FOTA_SERVER_CHECK_KEY, FOTA_SERVER_CHECK_VALUE);

        byte[] postDataBytes = postMsg.toString().getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty(CONTENT_LEN, String.valueOf(postDataBytes.length));
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);
        conn.connect();
        int responseCode = conn.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        Log.d(TAG, "status : " + responseCode);
        in.close();
        conn.disconnect();
        return response.toString();
    }

    public Pair<Boolean, String> handleReply(JSONObject reply) {
        String downloadUri = "";
        if (reply != null) {
            try {
                boolean status = isReplyOk(reply);
                if (status) {
                    String reason = "";
                    if (reply.has("reason")) {
                        reason = reply.getString("reason");
                        Log.d(TAG, "reason : " + reason);
                    }
                    boolean isFirst = getIsFirstFirmwareUpdate();
                    Log.d(TAG, "isFirst : " + isFirst);
                    if (isFirst) {
                        setIsFirstFirmwareUpdate(false);
                        try {
                            sendMessage(mContext, FIRST_SUCCESS_CHECKIN_MSG, "");
                        } catch (Exception e) {
                            Log.w(TAG, "startService to check first success checkin fail !");
                            Log.e(TAG, "", e);
                        }
                    }
                    downloadUri = getDownloadUri(reply);
                    return Pair.create(status, downloadUri);

                } else {
                    Log.w(TAG, "The stats_ok is false !");
                    return Pair.create(status, "");
                }
            } catch (JSONException e) {
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "replay is null !");
        }
        return Pair.create(false, "");
    }

    private boolean isReplyOk(JSONObject reply) throws JSONException {
        return reply.has("stats_ok") && reply.getBoolean("stats_ok");
    }

    public void setIsFirstFirmwareUpdate(boolean isFirst) {
        if (mContext == null) {
            Log.d(TAG, "setIsFirstFirmwareUpdate context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_FIRST_CHECKIN, isFirst).apply();
        } catch (Exception e) {
            Log.w(TAG, "setIsFirstFirmwareUpdate fail!");
            Log.e(TAG, "", e);
        }
    }

    public boolean getIsFirstFirmwareUpdate() {
        boolean isFirst = true;
        if (mContext == null) {
            Log.d(TAG, "getIsFirstFirmwareUpdate context is null.");
            return isFirst;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            isFirst = prefs.getBoolean(KEY_FIRST_CHECKIN, true);
        } catch (Exception e) {
            Log.w(TAG, "getIsFirstFirmwareUpdate fail!");
            Log.e(TAG, "", e);
        }
        return isFirst;
    }

    public void sendMessage(Context context, String status, String tag1) {
        JSONObject request = new JSONObject();
        try {
            String[] deviceInfo = getDeviceInfo().split(":");

            String cid = deviceInfo[3];
            request.put("cid", cid);
            String mcc_mnc = getMNC_MCC();
            request.put("mcc_mnc", mcc_mnc);
            String connMedia = getConnectMedia();
            request.put("connection_media", connMedia);
            String mid = deviceInfo[4];
            request.put("mid", mid);
            String serialNo = deviceInfo[1];
            request.put("serialNo", serialNo);
            String locale = Locale.getDefault().toString();
            request.put("locale", locale);
            String timeZone = getTimeZoneText();
            request.put("timeZone", timeZone);
            String sim_mcc_mnc = getSIM_MCC_MNC((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            request.put("sim_mcc_mnc", sim_mcc_mnc);
            //String imei = getIMEI();
            /*String imei = "90:e7:c4:39:2c:db";
            imei = imei.replace(":", "");
            imei = imei.toLowerCase();*/
            request.put("imei", serialNo);
            String firmware_version = "";
            if (INSTALL_SUCCESS_MSG.equals(status)) {
                firmware_version = getFirmwareVersion();
            } else {
                firmware_version = deviceInfo[0];
            }
            request.put("version", firmware_version);
            String model_number = deviceInfo[5];
            request.put("model", model_number);
            Long timestamp = System.currentTimeMillis();
            request.put("time", timestamp);

            request.put("status", status);
            String client = "aos_ww";
            request.put("client", client);
            String x1 = getX1(client, serialNo, timestamp);
            request.put("x1", x1);


            if (!TextUtils.isEmpty(tag1)) {
                request.put("tag1", tag1);
            }

            try {
                String mFlag = deviceInfo[6];
                request.put("mFlag", mFlag);

                String aaReport = deviceInfo[7];
                request.put("aaReport", aaReport);

                String imageId = getFirmwareImageID();

                Log.d(TAG, "send message imageID : " + imageId);

                request.put("imageID", imageId);

                String buildType = deviceInfo[7];
                request.put("buildType", buildType);

                Build bd = new Build();
                String model = bd.MODEL;
                String brand = bd.BRAND;

                if (!TextUtils.isEmpty(brand)) {
                    request.put("productBrand", brand);
                } else {
                    request.put("productBrand", "unknow");
                }

                if (!TextUtils.isEmpty(model)) {
                    request.put("productModel", model);
                } else {
                    request.put("productModel", "unknow");
                }

            } catch (Exception e) {
                Log.e(TAG, "", e);
            }

            if (Htc_SECURITY_DEBUG_flag) {
                Log.d(TAG, "data : " + request.toString());
            } else {
                try {
                    if (!TextUtils.isEmpty(mImei)) {
                        String temp = request.toString().replace(serialNo, serialNo.substring(serialNo.length() - 4, serialNo.length()));
                        Log.d(TAG, "data : " + request.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            SendMessageTask task = new SendMessageTask(EXTRA_MSG_URL, request.toString(), timestamp);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    public void setDeviceInfo(String info) {
        if (mContext == null) {
            Log.d(TAG, "setDeviceInfo context is null.");
            return;
        }
        if (!TextUtils.isEmpty(info)) {
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_DEVICE_INFO, info).apply();
            } catch (Exception e) {
                Log.w(TAG, "setDeviceInfo fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "info is null or empty !!");
        }

    }

    public String getDeviceInfo() {
        if (mContext == null) {
            Log.d(TAG, "getDeviceInfo context is null.");
            return "";
        }
        String info = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            info = prefs.getString(KEY_DEVICE_INFO, "");
        } catch (Exception e) {
            Log.w(TAG, "getDeviceInfo fail!");
        }
        return info;
    }

    public String getIMEI() {
        String imei = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            imei = prefs.getString(KEY_IMEI_INFO, "");
        } catch (Exception e) {
            Log.e(TAG, "get imei failed", e);
        }

        return imei;
    }

    public void setIMEI(String imei) {
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_IMEI_INFO, imei).apply();
        } catch (Exception e) {
            Log.e(TAG, "set imei failed", e);
        }
    }

    public void setFirmwareImageID(String imageID) {
        if (mContext == null) {
            Log.d(TAG, "setFirmwareImageID context is null.");
            return;
        }
        if (!TextUtils.isEmpty(imageID)) {
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_FIRMWARE_IMAGEID, imageID).apply();
            } catch (Exception e) {
                Log.w(TAG, "setFirmwareImageID fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "imageID is null or empty !!");
        }

    }

    public String getFirmwareImageID() {
        if (mContext == null) {
            Log.d(TAG, "getFirmwareImageID context is null.");
            return "";
        }
        String imageID = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            imageID = prefs.getString(KEY_FIRMWARE_IMAGEID, "");
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareImageID fail!");
            Log.e(TAG, "", e);
        }
        return imageID;
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {

        private String mUrl;
        private String mPostMsg;
        private long mTime;
        private HashMap<Long, String> tempMessageList;

        private SendMessageTask(String url, String postMSG, long time) {
            mUrl = url;
            mPostMsg = postMSG;
            mTime = time;
        }

        @Override
        protected Void doInBackground(Void... params) {
            HashMap<Long, String> messageList = getSendingFailMessage();
            messageList.put(mTime, mPostMsg);
            tempMessageList = new HashMap<>(messageList);
            for (Long time : messageList.keySet()) {
                String postMsg = messageList.get(time);
                Log.d(TAG, "time : " + time);
                try {
                    if (Htc_SECURITY_DEBUG_flag) {
                        Log.d(TAG, "postMSG : " + postMsg);
                    } else {
                        if (!TextUtils.isEmpty(mImei)) {
                            String temp = postMsg.toString().replace(mImei, mImei.substring(mImei.length() - 4, mImei.length()));
                            Log.d(TAG, "postMSG : " + temp);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sendMessage(postMsg, time);
            }
            setSendingFailMessage(tempMessageList);
            return null;
        }

        private void sendMessage(String postMsg, long time) {
            BufferedReader in = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(mUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(HTTP_POST);
                conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
                conn.setRequestProperty(FOTA_SERVER_CHECK_KEY, FOTA_SERVER_CHECK_VALUE);

                byte[] postDataBytes = postMsg.toString().getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty(CONTENT_LEN, String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
                int responseCode = conn.getResponseCode();

                in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                Log.d(TAG, "[SendMessageTask] status : " + responseCode);
                Log.d(TAG, "[SendMessageTask] response : " + response.toString());
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    tempMessageList.remove(time);
                }
            } catch (Exception e) {
                Log.e(TAG, "", e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "", e);
                }
                if (conn != null) {
                    conn.disconnect();
                }

            }
        }
    }

    public HashMap<String, String> getDeviceServerVersion() {
        HashMap<String, String> data = new HashMap<String, String>();
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            data = (HashMap<String, String>) ObjectSerializer.deserialize(prefs.getString(PREFERENCE_KEY_DEVICE_SERVER_VERSION_MESSAGE,
                    ObjectSerializer.serialize(new HashMap<String, String>())));
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        if (data == null) {
            data = new HashMap<String, String>();
        }

        return data;
    }

    public void setDeviceServerVersion(HashMap<String, String> data) {
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            String resultString = ObjectSerializer.serialize(data);
            prefs.edit().putString(PREFERENCE_KEY_DEVICE_SERVER_VERSION_MESSAGE, resultString).apply();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    public HashMap<Long, String> getSendingFailMessage() {
        HashMap<Long, String> data = new HashMap<Long, String>();
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            data = (HashMap<Long, String>) ObjectSerializer.deserialize(prefs.getString(PREFERENCE_KEY_SEND_FAIL_MESSAGE,
                    ObjectSerializer.serialize(new HashMap<Long, String>())));
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        if (data == null) {
            data = new HashMap<Long, String>();
        }

        return data;
    }

    public void setSendingFailMessage(HashMap<Long, String> data) {
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            String resultString = ObjectSerializer.serialize(data);
            prefs.edit().putString(PREFERENCE_KEY_SEND_FAIL_MESSAGE, resultString).apply();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    private String getDownloadUri(JSONObject reply) throws JSONException {
        if (!reply.has("intent")) {
            return "";
        }

        // Check digest is same with server or not. if not the same, save the checkin_interval value in local.
        if (reply.has("digest")) {
            String digestFromServer = reply.getString("digest");

            Log.d(TAG, "digestFromServer : " + digestFromServer);

            String digest = getDigest();
            if (!digest.equals(digestFromServer)) {
                setDigest(digestFromServer);

                if (reply.has("setting")) {
                    JSONArray settingsArray = reply.getJSONArray("setting");
                    int length = settingsArray.length();
                    String keyName = "";
                    String valueName = "";
                    for (int i = 0; i < length; i++) {
                        JSONObject obj = settingsArray.getJSONObject(i);
                        if (obj.has("name")) {
                            keyName = obj.getString("name");
                            if ("checkin_interval".equals(keyName)) {
                                valueName = obj.getString("value");
                                Log.d(TAG, "checkin inteval in server : " + valueName);
                                setCheckinInterval(valueName);
                            }
                        }
                    }
                }
            }
        }

        JSONArray arr = reply.getJSONArray("intent");
        int length = arr.length();

        String dataUri = "";
        String pkgFileName = "";
        String keyName = "";
        String imageId = "";
        String extraMessage = "";
        String extraFeature = "";
        String extraVersion = "";
        String extraSize = "";
        for (int i = 0; i < length; i++) {
            JSONObject obj = arr.getJSONObject(i);
            if (true) {
                Log.d(TAG, "[" + i + "] : " + obj.toString());
            }

            if (obj.has("data_uri")) {
                dataUri = obj.getString("data_uri");
            }

            if (obj.has("pkgFileName")) {
                pkgFileName = obj.getString("pkgFileName");
            }

            if (obj.has("imageId")) {
                imageId = obj.getString("imageId");
                if (true) {
                    Log.d(TAG, "imageId : " + imageId);
                }
                setFirmwareImageID(imageId);
            }

            if (obj.has("extra")) {
                JSONArray extra = obj.getJSONArray("extra");
                int extraLength = extra.length();

                for (int j = 0; j < extraLength; j++) {
                    JSONObject extraObj = extra.getJSONObject(j);
                    // Log.d("hugh", "[" + j + "] extraObj : " + extraObj.toString());
                    if (extraObj.has("name")) {
                        keyName = extraObj.getString("name");
                        if ("promptMessage".equals(keyName)) {
                            extraMessage = extraObj.getString("value");
                        } else if ("promptFeature".equals(keyName)) {
                            extraFeature = extraObj.getString("value");
                        } else if ("promptVersion".equals(keyName)) {
                            extraVersion = extraObj.getString("value");
                        } else if ("promptSize".equals(keyName)) {
                            extraSize = extraObj.getString("value");
                        }
                    }
                }
                if (true) {
                    Log.d(TAG, "extraMessage : " + extraMessage);
                }

                setFirmwareVersion(extraVersion);
                if (true) {
                    Log.d(TAG, "extraFeature : " + extraFeature);
                }
                setFirmwareFeature(extraFeature);
                if (true) {
                    Log.d(TAG, "extraVersion : " + extraVersion);
                    Log.d(TAG, "extraSize : " + extraSize);
                }
                setFOTASize(extraSize);
                setFirmwareSize(extraSize);
            }
        }

        if (true) {
            Log.d(TAG, "dataUri : " + dataUri);
        }
        setDownloadUrl(dataUri);
        if (true) {
            Log.d(TAG, "pkgFileName : " + pkgFileName);
        }
        return dataUri;
    }

    public void setCheckinInterval(String checkinInterval) {
        if (mContext == null) {
            Log.d(TAG, "setCheckinInterval context is null.");
            return;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CHECKIN_INTERVAL, checkinInterval).apply();
        } catch (Exception e) {
            Log.w(TAG, "setDigest fail!");
            Log.e(TAG, "", e);
        }
    }

    public String getCheckinInterval() {
        if (mContext == null) {
            Log.d(TAG, "getCheckinInterval context is null.");
            return "";
        }
        String checkinInterval = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            checkinInterval = prefs.getString(KEY_CHECKIN_INTERVAL, "");
        } catch (Exception e) {
            Log.w(TAG, "getCheckinInterval fail!");
            Log.e(TAG, "", e);
        }
        return checkinInterval;
    }

    public static String getFirmwareVersion() {
        if (mContext == null) {
            Log.d(TAG, "getFirmwareVersion context is null.");
            return "";
        }
        String firmwareVersion = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            firmwareVersion = prefs.getString(KEY_FIRMWARE_VERSION, "");
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareVersion fail!");
            Log.e(TAG, "", e);
        }
        return firmwareVersion;
    }

    public void setFirmwareVersion(String version) {
        if (mContext == null) {
            Log.d(TAG, "setFirmwareVersion context is null.");
            return;
        }
        if (!TextUtils.isEmpty(version)) {
            //Yvan modify
            //ex:"Software update: 0.28.999.99 (0.61 MB)";
            //The versionTemp will be "0.28.999.99 "
            String versionTemp = "";
            String regularRule = "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+";

            try {
                Pattern pattern = Pattern.compile(regularRule);
                Matcher matcher = pattern.matcher(version);
                boolean matchFound = matcher.find();
                if (matchFound) {
                    versionTemp = matcher.group();
                } else {
                    versionTemp = DEFAULT_UPDATE_VERSION;
                }
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
            Log.d(TAG, "versionTemp : " + versionTemp);
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_FIRMWARE_VERSION, versionTemp).apply();
            } catch (Exception e) {
                Log.w(TAG, "setFirmwareVersion fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "version is null or empty !!");
        }
    }

    public void setFirmwareFeature(String feature) {
        if (mContext == null) {
            Log.d(TAG, "setFirmwareFeature context is null.");
            return;
        }
        if (!TextUtils.isEmpty(feature)) {
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_FIRMWARE_FEATURE, feature).apply();
            } catch (Exception e) {
                Log.w(TAG, "setFirmwareFeature fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "version is null or empty !!");
        }

    }

    public String getFirmwareFeature() {
        if (mContext == null) {
            Log.d(TAG, "getFirmwareFeature context is null.");
            return "";
        }
        String firmwareFeature = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            firmwareFeature = prefs.getString(KEY_FIRMWARE_FEATURE, "");
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareVersion fail!");
            Log.e(TAG, "", e);
        }
        return firmwareFeature;
    }

    public void setFOTASize(String size) {
        if (mContext == null) {
            Log.d(TAG, "setFOTASize context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CB_FOTA_SIZE, size).apply();
        } catch (Exception e) {
            Log.w(TAG, "setFOTASize fail!");
            Log.e(TAG, "", e);
        }
    }

    public String getFOTASize() {
        String size = "";
        if (mContext == null) {
            Log.d(TAG, "getFOTASize context is null.");
            return size;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            size = prefs.getString(KEY_CB_FOTA_SIZE, "");
        } catch (Exception e) {
            Log.w(TAG, "getFOTASize fail!");
            Log.e(TAG, "", e);
        }
        return size;
    }

    public void setFirmwareSize(String size) {
        if (mContext == null) {
            Log.d(TAG, "setFirmwareSize context is null.");
            return;
        }
        if (!TextUtils.isEmpty(size)) {
            // Example : size = 5.68 MB, after filter size = 5
            size = size.toLowerCase();
            float fileSize = 0;
            if (size.contains("mb")) {
                size = size.replace("mb", "");
                size = size.trim();
                fileSize = Float.parseFloat(size);
            }
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putFloat(KEY_FIRMWARE_SIZE, fileSize).apply();
            } catch (Exception e) {
                Log.w(TAG, "setFirmwareSize fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "size is null or empty !!");
        }

    }

    public float getFirmwareSize() {
        if (mContext == null) {
            Log.d(TAG, "getFirmwareSize context is null.");
            return 0;
        }
        float firmwareSize = 0;
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            firmwareSize = prefs.getFloat(KEY_FIRMWARE_SIZE, 0);
        } catch (Exception e) {
            Log.w(TAG, "getFirmwareSize fail!");
            Log.e(TAG, "", e);
        }
        return firmwareSize;
    }

    public void setMD5(String md5) {
        if (mContext == null) {
            Log.d(TAG, "setMD5 context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_MD5, md5).apply();
        } catch (Exception e) {
            Log.w(TAG, "setMD5 fail!");
            Log.e(TAG, "", e);
        }
    }

    public String getMD5() {
        String md5 = "";
        if (mContext == null) {
            Log.d(TAG, "getMD5 context is null.");
            return md5;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            md5 = prefs.getString(KEY_MD5, "");
        } catch (Exception e) {
            Log.w(TAG, "setMD5 fail!");
            Log.e(TAG, "", e);
        }
        return md5;
    }

    public void setDownloadUrl(String url) {
        if (mContext == null) {
            Log.d(TAG, "setDownloadUrl context is null.");
            return;
        }
        if (!TextUtils.isEmpty(url)) {
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_DOWNLOAD_URL, url).apply();
            } catch (Exception e) {
                Log.w(TAG, "setDownloadUrl fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "url is null or empty !!");
        }
    }

    public String getDownloadUrl() {
        if (mContext == null) {
            Log.d(TAG, "getDownloadUrl context is null.");
            return "";
        }
        String downloadUrl = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            downloadUrl = prefs.getString(KEY_DOWNLOAD_URL, "");
        } catch (Exception e) {
            Log.w(TAG, "getDownloadUrl fail!");
            Log.e(TAG, "", e);
        }
        return downloadUrl;
    }

    public boolean setFirmwareStatus(boolean haveNewFirmware) {
        boolean result = true;
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();

            edit.putBoolean(PREFERENCE_KEY_FIRMWARE_STATUS, haveNewFirmware);

            edit.putLong(PREFERENCE_KEY_FIRMWARE_CHECK_TIME, Calendar.getInstance().getTimeInMillis());

            edit.apply();
        } catch (Exception e) {
            Log.e(TAG, "set firmware status failed", e);
            result = false;
        }
        return result;
    }

    public void setCurrentFirmwareVersion(String version) {
        if (mContext == null) {
            Log.d(TAG, "setCurrentFirmwareVersion context is null.");
            return;
        }
        if (!TextUtils.isEmpty(version)) {
            try {
                SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_CURRENT_FIRMWARE_VERSION, version).apply();
            } catch (Exception e) {
                Log.w(TAG, "setCurrentFirmwareVersion fail!");
                Log.e(TAG, "", e);
            }
        } else {
            Log.w(TAG, "version is null or empty !!");
        }

    }

    public String getCurrentFirmwareVersion() {
        if (mContext == null) {
            Log.d(TAG, "getCurrentFirmwareVersion context is null.");
            return "";
        }
        String version = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            version = prefs.getString(KEY_CURRENT_FIRMWARE_VERSION, "");
        } catch (Exception e) {
            Log.w(TAG, "getCurrentFirmwareVersion fail!");
            Log.e(TAG, "", e);
        }
        return version;
    }

    public Pair<String, Boolean> getFirmwareStatus(String addr) {
        String checkTime = null;
        boolean haveNewFirmware = false;
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY,
                    Context.MODE_PRIVATE);
            long time = prefs.getLong(PREFERENCE_KEY_FIRMWARE_CHECK_TIME, -1);
            String updateCompletetime = getFirmwareUpdateCompleteTime(addr);
            if (!TextUtils.isEmpty(updateCompletetime) || time != -1) {
                haveNewFirmware = prefs.getBoolean(PREFERENCE_KEY_FIRMWARE_STATUS, false);
                if(haveNewFirmware)
                    checkTime = getCurrentTime(time);
                else
                    checkTime = updateCompletetime;
            }
        } catch (Exception e) {
            Log.e(TAG, "get firmware status failed", e);
        }

        return Pair.create(checkTime, haveNewFirmware);
    }

    public boolean setFirmwareUpdateCompleteTime(String addr, String time) {
        if (TextUtils.isEmpty(addr)) {
            Log.w(TAG, "set firmware update complete time failed, addr is null.");
            return false;
        }
        boolean result = true;
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();

            edit.putString(PREFERENCE_KEY_FIRMWARE_UPDATE_COMPLETE_TIME, time);
            edit.apply();
        } catch (Exception e) {
            Log.e(TAG, "set firmware update complete time failed", e);
            result = false;
        }
        return result;
    }

    public String getFirmwareUpdateCompleteTime(String addr)
    {
        if (TextUtils.isEmpty(addr)) {
            Log.w(TAG, "get firmware update complete time failed, addr is null.");
            return "";
        }
        String time = "";
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY,
                    Context.MODE_PRIVATE);
            time = prefs.getString(PREFERENCE_KEY_FIRMWARE_UPDATE_COMPLETE_TIME, "");
        } catch (Exception e) {
            Log.e(TAG, "get firmware update complete time failed", e);
        }
        return time;
    }

    public void setStartFotaCheck(boolean isStarted) {
        setStartFotaCheckTime(System.currentTimeMillis());
        if (mContext == null) {
            Log.d(TAG, "setStartFotaCheck context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PREFERENCE_KEY_START_FOTA_CHECK, isStarted).apply();
        } catch (Exception e) {
            Log.w(TAG, "setStartFotaCheck fail!");
            Log.e(TAG, "", e);
        }
    }

    public boolean isStartedFotaCheck() {
        boolean isStarted = false;
        if (mContext == null) {
            Log.d(TAG, "isStartedFotaCheck context is null.");
            return isStarted;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            isStarted = prefs.getBoolean(PREFERENCE_KEY_START_FOTA_CHECK, false);
        } catch (Exception e) {
            Log.w(TAG, "isStartedFotaCheck fail!");
            Log.e(TAG, "", e);
        }
        return isStarted;
    }

    public void setStartFotaCheckTime (long time) {
        if (mContext == null) {
            Log.d(TAG, "setStartFotaCheckTime context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putLong(PREFERENCE_KEY_START_FOTA_CHECK_TIME, time).apply();
        } catch (Exception e) {
            Log.w(TAG, "setStartFotaCheckTime fail!");
            Log.e(TAG, "", e);
        }
    }

    public long getStartFotaCheckTime() {
        long time = 0;
        if (mContext == null) {
            Log.d(TAG, "getStartFotaCheckTime context is null.");
            return time;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            time = prefs.getLong(PREFERENCE_KEY_START_FOTA_CHECK_TIME, 0);
        } catch (Exception e) {
            Log.w(TAG, "getStartFotaCheckTime fail!");
            Log.e(TAG, "", e);
        }
        return time;
    }


    public String getCurrentTime(long time) {
        Date date = new Date();
        date.setTime(time);
        return java.text.DateFormat.getDateTimeInstance().format(date);
    }

    public void setHaveFirmwareUpdate(boolean isUpdate) {
        if (mContext == null) {
            Log.d(TAG, "setHaveFirmwareUpdate context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_HAVE_FIRMWARE_UPDATE, isUpdate).apply();
        } catch (Exception e) {
            Log.w(TAG, "setHaveFirmwareUpdate fail!");
            Log.e(TAG, "", e);
        }
    }

    public boolean haveFirmwareUpdate() {
        boolean isUpdate = false;
        if (mContext == null) {
            Log.d(TAG, "haveFirmwareUpdate context is null.");
            return isUpdate;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            isUpdate = prefs.getBoolean(KEY_HAVE_FIRMWARE_UPDATE, false);
        } catch (Exception e) {
            Log.w(TAG, "haveFirmwareUpdate fail!");
            Log.e(TAG, "", e);
        }
        return isUpdate;
    }

    public void setFOTAStatus(int status) {
        if (mContext == null) {
            Log.d(TAG, "setFOTAStatus context is null.");
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_FOTA_STATUS, status).apply();
        } catch (Exception e) {
            Log.w(TAG, "setFOTAStatus fail!");
            Log.e(TAG, "", e);
        }
    }

    public int getFOTAStatus() {
        int status = FOTA_NO_UPDATE;
        if (mContext == null) {
            Log.d(TAG, "getFOTAStatus context is null.");
            return status;
        }
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(FIRMWARE_UPDATE_KEY, Context.MODE_PRIVATE);
            status = prefs.getInt(KEY_FOTA_STATUS, FOTA_NO_UPDATE);
        } catch (Exception e) {
            Log.w(TAG, "getFOTAStatus fail!");
            Log.e(TAG, "", e);
        }
        return status;
    }

    public void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message =
                    "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    public void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    public void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + file);
            }
            if (!file.delete()) {
                String message =
                        "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    public interface FirmwareUpdateServiceListener {
        void onServiceConnected();
        void onServiceDisconnected();
        void showCheckDialog();
        void showDownloadError();
        void showFirstCheckDialog();
        void dismissConnectedDialog();
        void dismissFirstLaunchDialog();
        void checkFOTACompleted(int status);
        void updateDownloadProgress(int number);
        void downloadCompleted();
        void updateCompletedUI();
        void updateFailUI();
        void updateUploadProgress(int number);
        void showWrongDialog(Bundle bundle);
        void showNotFoundDeviceDialog(Bundle bundle);
        void showHMDLowBatteryDialog(Bundle bundle);
        void setScreenOn(Boolean isOn);
        void dismissDownloadDialog();
    }

    public interface CheckFotaUpdateListener {
        void onCheckFotaUpdateResult(boolean haveUpdate, BleDevInfo deviceInfo);
        void onFotaUpdateCompleted(BleDevInfo deviceInfo);
        void onStatusChanged(int status, BleDevInfo deviceInfo);
        void onProgressChanged(int progress);
    }
}
