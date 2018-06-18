package com.htc.client.controllerscanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.support.annotation.IdRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.client.BuildConfig;
import com.htc.client.R;
import com.htc.client.FotaUpdateService;
import com.htc.client.utils.FirmwareUpdateUtils;
import com.htc.client.utils.FotaServiceContract;
import com.htc.client.utils.SharedPrefManager;
import com.htc.client.utils.Utils;
import com.htc.client.utils.AnimationsContainer;
import com.htc.client.vr.BleDev;
import com.htc.client.vr.BleDevInfo;
import com.htc.client.vr.IScannerListener;
import com.htc.client.vr.IScannerServiceInterface;
import com.htc.vr.sdk.overlay.VRCustomizeOverlay;
import com.htc.vr.sdk.overlay.VROverlayParams;
import com.htc.vr.sdk.overlay.VROverlayService;
import com.htc.vr.sdk.overlay.VROverlayType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.ControllerLowBattery;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.FOTADownload;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.FOTAFailed;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.FOTAInstall;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.FOTASuccess;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.FOTAUpdateAvailable;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.Init;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.NotFound;
import static com.htc.client.controllerscanner.ScannerService.ActionTypeName.ScreenOn;

public class ScannerService extends VROverlayService {
    private final static String TAG = "[" + BuildConfig.PACKAGESIMPLENAME + "]" + ScannerService.class.getSimpleName();
    private VRCustomizeOverlay mHvrOverlay;
    private boolean mIsShowed= false;

    private BluetoothLeScanner mScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    //private Runnable mPostRunnable;
    private Runnable mPostResacanRunnable;

    private static final String[] mDevNameList = {"hTC_MVR_Controller", "Daydream controller", "Finch", "X3C01"};
    private String mSelectControllerType;
    private boolean mIsFinch6Dof = false; //in order to distinguish Finch 3Dof and Finch 6Dof, because we can't distinguish it by "Finch".
    private static final String[] mUuidList = {"0000FE59-0000-1000-8000-00805F9B34FB", "6e400001-b5a3-f393-e0a9-e50e24dcca9e", "0000fe55-0000-1000-8000-00805f9b34fb", "00001812-0000-1000-8000-00805f9b34fb", "0000f001-0000-1000-8000-00805f9b34fb"};

    private static final Map<BleDev, ScanResult> mBleDevMap = new ConcurrentHashMap<>(); //contain all scanned ble device.

    private boolean mTimerActive = false;

    private Map<String, List<BleDev>> mFinch6DofMap = new HashMap<>(); //key is the 6Dof finch type(RIGHT_STRING for right, LEFT_STRING for left), value is the BleDev list.

    private boolean isEnvReady = false;
    private boolean mIsScanning = false;
    private boolean mWaitTODoFirstScan = false;
    private boolean mIsDeviceServiceConnecting = false;

    private static final String MVR_OOBE_PACKAGENAME = "com.htc.mobilevr.setup";
    private static final String ACTION_SCAN_DEVICE = "com.htc.miac.controllerutility.action.SCAN_DEVICE";
    private static final String ACTION_DEVICE_DISCONNECT = "com.htc.miac.controllerutility.action.DEVICE_DISCONNECT";
    private static final String ACTION_HIDE_UI = "com.htc.miac.controllerutility.action.HIDE_UI";
    public static final String FINCH_6DOF_SHIFT_STRING = "Finch Shift";
    public static final String FINCH_6DOF_UARM_STRING = "Finch UArm";
    public static final String RIGHT_STRING = " R";
    public static final String LEFT_STRING  = " L";

    // Check controller per 3 seconds.
    private static final long CHECK_PERIOD = 3 * 1000;
    // Stops rescanning after 20 seconds.
    private static final long RESCAN_PERIOD = 5 * 60 * 1000;
    // wait 3 seconds to rescan device.
    private static final long RESCAN_DEVICE = 3 * 1000;
    // wait 3 seconds to find the closest device.
    private static final long WAIT_COLLECT_PERIOD= 200;

    private static final int PRIORITY_RSSI_LEVEL = -65;

    private int RSSI_H = -68;
    private int RSSI_M = -87;

    private int DELAY_H = 0;
    private int DELAY_M = 1;
    private int DELAY_L = 2;

    private int SCAN_CONTROLLER_COUNT = 1;

    private IScannerListener mListener;
    //save the select BleDev bond state
    private Map<Integer, List<BleDev>> mBondMap = new ConcurrentHashMap<>(); //key is bond state, and value is BleDev list.
    //save the scan device info
    private ConcurrentHashMap<String, BluetoothDeviceInfo> mBluetoothDeviceInfo = new ConcurrentHashMap<>();
    private ArrayList<String> mFoundDeviceList = new ArrayList<>();
    private boolean mIsCollectDeviceTimerStart = true;
    private boolean mIsStartCollectFinch = false;

    private Timer mCheckTimer; //for check valid controller.

    private BtBroadcastReceiver mBtReceiver;
    private OverlayBroadcastReceiver mOverlayReceiver;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Handler mFOTAReconnectHandler = new Handler(new FOTAReconnectHandler());
    private Handler mCollectPairDeviceHandler = new Handler(new CollectPairDevice());

    private static final String RECENT_BONDED_LIST_FILE_NAME = "recent_bonded_list.txt";
    private static final String RECENT_BONDED_LIST_SPLIT = "@";
    private final List<BleDev> mRecentBondedList = new ArrayList<>();

    private final List<BleDev> mToBeRescanBleDevs = new ArrayList<>(); //contain all these device that to be rescan.
    private final List<BleDev> mRescannedBleDevs = new ArrayList<>(); //contain all these device that have been rescanned.
    private boolean mIsRescanning = false;

    //the value for check the device is be paired
    private long DEVICE_EXIST_TIME = 200;

    private boolean mIsStartFotaUpdate = false;

    private Handler mBindServiceHandler = new Handler(new BindServiceHandler());
    private static final int MSG_SEND_BIND_CONTROLLER_SERVICE = 3024;
    private static final int MSG_SEND_BIND_FOTA_SERVICE = 3025;
    private static final int MSG_SEND_RECONNECT_DEVICE = 3026;
    private static final int MSG_SEND_PAIR_DEVICE = 3027;
    private static final int MSG_EXTEND_SEARCH_TIME = 3028;
    private Context mContext;
    private FotaUpdateService mFotaUpdateService;
    private boolean mFotaServiceBound = false;
    private BleDevInfo mFotaBleDev;

    private SharedPrefManager mSharedPrefManager;

    private int mNowStatus;
    private final int INITIAL_SCAN = 100;
    private final int SCAN = 101;
    private final int RESCAN = 102;
    private final int NO_SCAN = 103;

    private ActionTypeName mCurrentType;
    private boolean mIsFOTAing = false;
    private int mCurrentProgress = 0;

    public static boolean mIsScreenON = true;
    private boolean mIsShowScreenOn = false;
    private String mConnectedDevice;

    private static final String VOLUME_KEY_EVENT = "com.htc.vr.core.server.notification.service.receiver.2d";
    private VolumeKeyEventReceiver mVolumeKeyEventReceiver;

    private static final String ACTION_RECENTERSUCCESS = "vive.wave.intent.action.ACTION_RECENTERSUCCESS";
    private static final String ACTION_RECENTERFAIL = "vive.wave.intent.action.ACTION_RECENTERFAIL";

    //For check overlay of other class
    private static final String ACTION_OEMOVERLAYSTATE = "vive.wave.intent.action.ACTION_OEMOVERLAYSTATE";
    private static final String EXTRAS_PACKAGENAME = "vive.wave.intent.action.EXTRAS_PACKAGENAME";
    private static final String VR_CAMERA = "SeeThrough";
    private static final String VR_MTP_SERVICE = "VRMTPService";
    private static final String VR_POWER_MENU_SERVICE = "VRPowerMenuService";
    private static final String EXTRAS_IS_SHOW = "vive.wave.intent.action.EXTRAS_IS_SHOW";
    private boolean mIsOtherOverlay = false;
    private boolean mIsCameraOverlay = false;
    private boolean mIsMTPOverlay = false;
    private boolean mIsPowerMenuOverlay = false;
    private ActionTypeName mSavedViewType;
    private AnimationsContainer.FramesSequenceAnimation mControllerAnimation;

    //region VR OverlayUI
    protected void onVROverlayResume() {
        Logger.i(this.TAG, "onVROverlayResume()");
        super.onVROverlayResume();
        VRCustomizeOverlay overlay = this.mHvrOverlay;
        this.mHvrOverlay = (VRCustomizeOverlay)this.getVROverlay(new VROverlayParams(new VROverlayType(2)));
//        this.mHvrOverlay.setOverlayInputType(VRInputId.Touchpad);
//        mHvrOverlay.setOnOutOfBoundButtonEventListener(new VRCustomizeOverlay.OnOutOfBoundButtonEventListener() {
//            @Override
//            public void onOutOfBoundButtonEvent(VREventType vrEventType, VRInputId vrInputId, int i) {
//                Logger.i(TAG, "onOutOfBoundButtonEvent()");
//                if(vrInputId == VRInputId.Touchpad) ScannerService.this.hideDashboard();
//            }
//        });

        if (mIsFOTAing) {
            showActionUI(mCurrentType);
        } else {
            switch (mNowStatus) {
                case NO_SCAN:
                    if (mIsShowScreenOn || mIsShowed) {
                        ShowScreenOnUI();
                        mIsShowScreenOn = false;
                    }
                    break;
                case INITIAL_SCAN:
                    showPairUI(Init);
                    break;
                case SCAN:
                case RESCAN:
                    showPairUI(NotFound);
                    break;
            }
        }
    }

    protected void onVROverlayPause() {
        Logger.i(TAG, "onVROverlayPause() start...");
    }

    private void hideDashboard() {
        Logger.i(TAG, "hideDashboard()");

        hideRecenterAnimation();

        if(mHvrOverlay == null) {
            Logger.i(TAG, "mHvrOverlay is null");
            return;
        }
        mIsShowScreenOn = false;
        mHvrOverlay.hideOverlay();

        mIsShowed = false;
    }
    //endregion

    // JUnit test used
    public void scanSpecificLeDevice (boolean enable, String mac) {
        Logger.i(TAG, "start scanSpecificLeDevice");
        mRecentBondedList.clear();
        mRecentBondedList.add(new BleDev(mac, "Finch_JUnit_test"));
        scanLeDevice(enable);
    }
    // JUnit test used
    public boolean isSpecificDeviceConnected (String mac) {
        Logger.i(TAG, "isSpecificDeviceConnected");
        return isControllerConnected(mac);
    }
    // JUnit test used
    public void unPairSpecificDevice (String mac) {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (null != devices && !devices.isEmpty()) {
            for (BluetoothDevice device : devices) {
                String addr = device.getAddress();
                if (null == addr) {
                    continue;
                }
                if (addr.equals(mac)) {
                    Logger.d(TAG, "unpair device: " + hashAddress(mac));
                    unpairDevice(device);
                    break;
                }
            }
        }
    }

    private void doFirstScan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                readBondedList();
                for (BleDev bleDev : mRecentBondedList) {
                    Logger.d(TAG, "check controller connected");
                    if (isControllerConnected(bleDev.mAddr)) {
                        addToBondMap(BluetoothDevice.BOND_BONDED, bleDev);
                        mConnectedDevice = bleDev.mAddr;
                        mNowStatus = NO_SCAN;
                        Logger.d(TAG, "A controller is connected, do not scan");
                        return;
                    }
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logger.i(TAG, "initial scan");
                        showPairUI(Init);
                        scanLeDevice(true);
                        mNowStatus = INITIAL_SCAN;
                    }
                });
            }
        }).start();
    }


    public final class IScannerService extends IScannerServiceInterface.Stub {
        @Override
        public void start() throws RemoteException {
            Logger.v(TAG, "start");
            if (!isEnvReady){
                Logger.w(TAG, "system env is not ready.");
                if (!mBluetoothAdapter.isEnabled()) {
                    Logger.w(TAG, "wait BT ON to do first Scan");
                    mWaitTODoFirstScan = true;
                }
                return;
            }
            doFirstScan();
        }

        @Override
        public void stop() throws RemoteException {
            Logger.v(TAG, "stop");
            scanLeDevice(false);
            mNowStatus = NO_SCAN;
        }

        @Override
        public void registerListener(ParcelUuid appId, IScannerListener listener) throws RemoteException {
            //current, only one listener is support!
            mListener = listener;
        }

        @Override
        public void unregister(ParcelUuid appId) throws RemoteException {
            mListener = null;
        }

        @Override
        public List<BleDev> getControllers() throws RemoteException {
            //return  mBondMap.get(BluetoothDevice.BOND_BONDED);
            return null;
        }

        @Override
        public void setControllerCount(int cnt) throws RemoteException {
            Logger.d(TAG, "[setControllerCount] : " + cnt);
        }

        @Override
        public void onConnected(String mac) throws RemoteException {
            Logger.d(TAG, "[onConnected] : " + hashAddress(mac));
            String addr = mSharedPrefManager.getPairMacAddress();
            if (TextUtils.isEmpty(addr)) {
                mSharedPrefManager.setMacAddress(mac);
            }
            mIsDeviceServiceConnecting = false;
            if (mNowStatus != NO_SCAN) {
                stopControllerScan();
                mNowStatus = NO_SCAN;
            }
            showPairUI(ScreenOn);
        }

        @Override
        public void onDisconnected(String mac) throws RemoteException {
            Logger.d(TAG, "[onDisconnected] : " + hashAddress(mac));

            BleDev device = getBondedDev(mac);
            if (device != null) {
                Logger.d(TAG, "getBondedDev(): " + device.mName + " @ " + hashAddress(device.mAddr));
                if (mIsScreenON) {
                    // do rescan immediately
                    if (add2ToBeRescanBleDevs(device)) {
                        restartScanner();
                        mNowStatus = RESCAN;
                    }
                    showActionUI(NotFound);
                } else {
                    // do scan after screen on
                    Logger.d(TAG, "do scan after screen on");
                    mNowStatus = SCAN;
                }
            }
        }

        @Override
        public void onDeviceInfo(String mac, BleDevInfo info) throws RemoteException {
            boolean isOOBEFinished = isOOBEFakeFinished();
            Logger.d(TAG, "[onDeviceInfo] info : " +  info.mName + " @ " + hashAddress(info.mAddr) +
                    " @ version : " + info.mFwVersion + " @ model name : " + info.mModuleName  + ", is OOBE finished : " + isOOBEFinished);
            if (isOOBEFinished) {
                checkFotaUpdate(info);
            }
        }

        public ScannerService getLocalService() {
            return ScannerService.this;
        }
    }

    public final IScannerService mBinder = new IScannerService();

    private boolean isController(String devName) {
        for (String name : mDevNameList) {
            if (devName.startsWith(name)) {
                return true;
            }
        }

        return false;
    }

    private BleDev getBleDevFromBleDevMap(String addr) {
        for (BleDev dev : mBleDevMap.keySet()) {
            if (dev.mAddr.equals(addr)) {
                return dev;
            }
        }

        return null;
    }

    private boolean isFinch6DofBleDev(String devName) {
        if (devName.startsWith(FINCH_6DOF_SHIFT_STRING) || devName.startsWith(FINCH_6DOF_UARM_STRING)) {
            return true;
        }

        return false;
    }

    private boolean addToBondMap(int bondState, BleDev dev) {
        List<BleDev> devs = mBondMap.get(bondState);

        if (null == devs) {
            devs = new ArrayList<>();
            devs.add(dev);
            mBondMap.put(bondState, devs);
            return true;
        } else {
            for (BleDev bleDev : devs) {
                if (bleDev.mAddr.equals(dev.mAddr)) {
                    return false;
                }
            }

            devs.add(dev);
            return true;
        }
    }

    private boolean addToBondMap(BleDev dev) {
        BluetoothDevice bluetoothDevice = mBleDevMap.get(dev).getDevice();
        if (null == bluetoothDevice) {
            Logger.e(TAG, "can't be null!");
            return false;
        }

        return addToBondMap(bluetoothDevice.getBondState(), dev);
    }

    private boolean addToFinch6DofMap(String type, BleDev dev) {
        List<BleDev> devs = mFinch6DofMap.get(type);
        if (null == devs) {
            devs = new ArrayList<>();
            devs.add(dev);
            mFinch6DofMap.put(type, devs);
            return true;
        } else {
            if (devs.size() >= 2) {
                //Logger.d(TAG, "got one 6dof controller " + devs.get(0) + " " + devs.get(1));
                return false;
            }

            if (devs.isEmpty()) {
                devs.add(dev);
                return true;
            }

            BleDev bleDev = devs.get(0);
            if (bleDev.mAddr.equals(dev.mAddr)) {
                //Logger.d(TAG, "same address");
                return false;
            }
            if (bleDev.mName.startsWith(FINCH_6DOF_SHIFT_STRING) && dev.mName.startsWith(FINCH_6DOF_SHIFT_STRING)) {
                //Logger.d(TAG, "same Shift dev");
                return false;
            }
            if (bleDev.mName.startsWith(FINCH_6DOF_UARM_STRING) && dev.mName.startsWith(FINCH_6DOF_UARM_STRING)) {
                //Logger.d(TAG, "same Uarm dev");
                return false;
            }
            devs.add(dev);
            return true;
        }
    }

    private int getFinch6DofControllerCnt() {
        int count = 0;

        for (String type : mFinch6DofMap.keySet()) {
            List<BleDev> devs = mFinch6DofMap.get(type);
            if (devs != null && devs.size() == 2) {
                count++;
            }
        }

        return count;
    }

    private BleDev getAndRemoveBondingBleDev(String addr) {
        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDING);

        if (null == devs) {
            return null;
        }

        for (BleDev bleDev : devs) {
            if (bleDev.mAddr.equals(addr)) {
                devs.remove(bleDev);
                return bleDev;
            }
        }

        return null;
    }

    private List<BleDev> getSortedBleDevs() {
        List<BleDev> sortedDevs = new ArrayList<>();
        if (mBleDevMap != null && !mBleDevMap.isEmpty()) {
            List<Map.Entry<BleDev, ScanResult>> entryList = new ArrayList<Map.Entry<BleDev, ScanResult>>(mBleDevMap.entrySet());
            Collections.sort(entryList,
                    new Comparator<Map.Entry<BleDev, ScanResult>>() {
                        public int compare(Map.Entry<BleDev, ScanResult> entry1,
                                           Map.Entry<BleDev, ScanResult> entry2) {
                            int value1 = entry1.getValue().getRssi();
                            int value2 = entry2.getValue().getRssi();
                            return value2 - value1;
                        }
                    });
            Iterator<Map.Entry<BleDev, ScanResult>> iter = entryList.iterator();
            Map.Entry<BleDev, ScanResult> tmpEntry = null;
            while (iter.hasNext()) {
                tmpEntry = iter.next();
                sortedDevs.add(tmpEntry.getKey());
            }
        }
        return sortedDevs;
    }

    private List<BleDev> getSortedAndRecentBonedBleDevs() {
        List<BleDev> sortedDevs = new ArrayList<>();
        if (mBleDevMap != null && !mBleDevMap.isEmpty()) {
            List<Map.Entry<BleDev, ScanResult>> entryList = new ArrayList<Map.Entry<BleDev, ScanResult>>(mBleDevMap.entrySet());
            Collections.sort(entryList,
                    new Comparator<Map.Entry<BleDev, ScanResult>>() {
                        public int compare(Map.Entry<BleDev, ScanResult> entry1,
                                           Map.Entry<BleDev, ScanResult> entry2) {
                            int value1 = entry1.getValue().getRssi();
                            int value2 = entry2.getValue().getRssi();
                            return value2 - value1;
                        }
                    });
            Iterator<Map.Entry<BleDev, ScanResult>> iter = entryList.iterator();
            Map.Entry<BleDev, ScanResult> tmpEntry = null;
            while (iter.hasNext()) {
                tmpEntry = iter.next();
                for (BleDev dev : mRecentBondedList) {
                    if (dev.mAddr.equals(tmpEntry.getKey().mAddr)) {
                        sortedDevs.add(tmpEntry.getKey());
                        break;
                    }
                }
            }
            //TODO for Debug***************************************
            iter = entryList.iterator();
            Map.Entry<BleDev, ScanResult> tmpEntryLog = null;
            while (iter.hasNext()) {
                tmpEntryLog = iter.next();
                String name = tmpEntryLog.getKey().mName;
                String addr = tmpEntryLog.getKey().mAddr;
                int rssiValue = tmpEntryLog.getValue().getRssi();
                Logger.d(TAG, "[sort] devName: " + name + " address: " + hashAddress(addr) + " rssi: " + rssiValue);
            }
            //***************************************
            iter = entryList.iterator();
            while (iter.hasNext()) {
                tmpEntry = iter.next();

                boolean bAdded = false;
                for (BleDev dev : sortedDevs) {
                    if (dev.mAddr.equals(tmpEntry.getKey().mAddr)) {
                        bAdded = true;
                        break;
                    }
                }

                if (bAdded) {
                    continue;
                }
                sortedDevs.add(tmpEntry.getKey());
            }
        }

        return sortedDevs;
    }

    private void addBleDev(ScanResult result) {
        BluetoothDevice dev = result.getDevice();
        String devName = dev.getName();
        if (devName != null && isController(devName)) {
            BleDev device = getBleDevFromBleDevMap(dev.getAddress());
            if (null == device) {
                Logger.v(TAG, "found controller: " + devName);
                device = new BleDev(dev.getAddress(), dev.getName());
            }
            mBleDevMap.put(device, result);
        }
        bondTheDevice();
    }

    private void bondTheDevice () {
        Logger.v(TAG, "check to find controller!");
        if (findControllers() >= SCAN_CONTROLLER_COUNT) {
            stopControllerScan();
            mNowStatus = NO_SCAN;
            checkBondState();
        }
    }

    private void updateScanDeviceInfo(ScanResult result) {
        if (mBluetoothDeviceInfo.containsKey(result.getDevice().getAddress())) {
            // remove when commit
            Log.d(TAG, "[updateScanDeviceInfo] addr : " + hashAddress(result.getDevice().getAddress()) + ", rssi : "
                    + result.getRssi() + ", timestamp : " + System.currentTimeMillis());
            BluetoothDeviceInfo info = mBluetoothDeviceInfo.get(result.getDevice().getAddress());
            info.timestamp = System.currentTimeMillis();
            info.result = result;
            mBluetoothDeviceInfo.put(result.getDevice().getAddress(), info);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Logger.d(TAG, "onScanResult " + "callbackType:" + callbackType + " result:" + result);
            if (result.getDevice().getName() != null && (result.getDevice().getName().startsWith("Finch") || result.getDevice().getName().startsWith("DFU"))) {
                String dfuAddress = mSharedPrefManager.getDFUMacAddress();
                if (result.getDevice().getAddress().equals(dfuAddress) && result.getDevice().getName().startsWith("DFU")) {
                    Logger.d(TAG, "[onScanResult] found dfu mode device, start FOTA : " + hashAddress(dfuAddress) + ", name : " + result.getDevice().getName());
                    if (stopControllerScan()) {
                        mFotaBleDev = new BleDevInfo(revertDFUMacAddress(result.getDevice().getAddress()), result.getDevice().getName(), mSharedPrefManager.getFinchModelNumber(), mSharedPrefManager.getFinchVersion(), "");
                        startFotaUpdate(true);
                    }
                } else {
                    //updateScanDeviceInfo(result);
                    // home key event
                    byte[] advManufacturerSpecificData = result.getScanRecord().getManufacturerSpecificData(0);
                    if (advManufacturerSpecificData != null) {
                        //StringBuilder sb = new StringBuilder();
                        //for (byte b : advManufacturerSpecificData)
                        //    sb.append(String.format("%02x ", b & 0xff));
                        //Logger.d(TAG, "onScanResult advManufacturerSpecificData : " + sb.toString());
                        if (2 == advManufacturerSpecificData.length && (byte) 0x80 == advManufacturerSpecificData[0] && 0x00 == advManufacturerSpecificData[1]) {
                            Logger.d(TAG, "onScanResult finch Controller broadcast attach key [home] or touch");
                            String addr = mSharedPrefManager.getPairMacAddress();
                            String scanAddr = result.getDevice().getAddress();
                            int scanRssi = result.getRssi();
                            String hashScanAddr = hashAddress(scanAddr);

                            if (TextUtils.isEmpty(addr)) {
                                Logger.d(TAG, "[onScanResult] device : " + hashScanAddr + ", rssi: " + scanRssi);
                                if(scanRssi >= PRIORITY_RSSI_LEVEL && mIsCollectDeviceTimerStart){
                                    mIsCollectDeviceTimerStart = false;
                                    Logger.d(TAG, "[onScanResult] device : " + hashScanAddr + ", try to connect");
                                    addBleDev(result);
                                }

                                //if (mIsCollectDeviceTimerStart) {
                                //    mIsCollectDeviceTimerStart = false;
                                //    mCollectPairDeviceHandler.sendEmptyMessageDelayed(MSG_SEND_PAIR_DEVICE, WAIT_COLLECT_PERIOD);
                                //    mIsStartCollectFinch = true;
                                //}
                                //if (mIsStartCollectFinch) {
                                //    if (!mBluetoothDeviceInfo.containsKey(result.getDevice().getAddress())) {
                                //        BluetoothDeviceInfo info = new BluetoothDeviceInfo(result, System.currentTimeMillis());
                                //        mBluetoothDeviceInfo.put(result.getDevice().getAddress(), info);
                                //    }
                                //}
                            } else {
                                Logger.d(TAG, "[onScanResult] The last pair device : " + hashAddress(addr));
                                if (addr.equals(scanAddr)) {
                                    Logger.d(TAG,"[onScanResult] Result device fit to SharedPreference");
                                    addBleDev(result);
                                }else{
                                    Logger.d(TAG, "[onScanResult] The last pair device : " + fullHashAddress(addr) );
                                    Logger.d(TAG, "[onScanResult] The result device : " + fullHashAddress(result.getDevice().getAddress()));
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Logger.d(TAG, "onBatchScanResults:" + results);
            for (ScanResult result : results) {
                addBleDev(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Logger.w(TAG, "onScanFailed:" + errorCode);
        }
    };


    public ScannerService() {
        mHandler = new Handler();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }
        mContext = ScannerService.this;
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        return intentFilter;
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void unpairBonedDevices() {
        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDED);
        if (devs != null && !devs.isEmpty()) {
            for (BleDev bleDev : devs) {
                BluetoothDevice device = mBleDevMap.get(bleDev).getDevice();
                if (null == device) {
                    continue;
                }
                unpairDevice(device);
            }
        }
    }

    private void writeBondedList() {
        BufferedWriter bufferdWriter = null;
        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDED);
        if (null == devs || devs.isEmpty()) {
            return;
        }

        try {
            FileOutputStream outputStream = this.openFileOutput(RECENT_BONDED_LIST_FILE_NAME, Context.MODE_PRIVATE);
            bufferdWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            for (BleDev bleDev : devs) {
                Logger.d(TAG, "write recent bonded ble device " + hashAddress(bleDev.mAddr));
                bufferdWriter.write(bleDev.mAddr + RECENT_BONDED_LIST_SPLIT + bleDev.mName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferdWriter != null) {
                    bufferdWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readBondedList() {
        BufferedReader bufferedReader = null;
        try {
            File file = new File(this.getFilesDir(), RECENT_BONDED_LIST_FILE_NAME);
            if (!file.exists()) {
                Log.w(TAG, "file:" + RECENT_BONDED_LIST_FILE_NAME + " not exist.");
                return;
            }

            FileInputStream inputStream = this.openFileInput(RECENT_BONDED_LIST_FILE_NAME);

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                String[] pair = line.split(RECENT_BONDED_LIST_SPLIT);
                if (pair.length == 2) {
                    BleDev dev = new BleDev(pair[0], pair[1]);
                    Logger.d(TAG, "read recent bonded ble device " + hashAddress(dev.mAddr));
                    mRecentBondedList.add(dev);
                } else {
                    Logger.e(TAG, "unknow text format " + line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean isControllerConnected(String mac) {
        if (null != mListener && null != mac) {
            try {
                boolean isConnected = mListener.isConnected(mac);
                Logger.d(TAG,"isControllerConnected(mac):" + isConnected);
                return isConnected;
            } catch (RemoteException e) {
                Logger.d(TAG, e.toString());
            }
        } else {
            Logger.d(TAG, "mListener: " + mListener + ", mac is null: " + (mac == null));
        }
        return false;
    }

    @Override
    public void onCreate() {
        Logger.v(TAG, "onCreate");
        Logger.v(TAG, "Ver: " + BuildConfig.VERSION_NAME + " TimeStamp:" + BuildConfig.BUILD_TIMESTAMP);
        super.onCreate();
        mBtReceiver = new BtBroadcastReceiver();
        mOverlayReceiver = new OverlayBroadcastReceiver();
        registerReceiver(mBtReceiver, makeIntentFilter());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_OEMOVERLAYSTATE);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mOverlayReceiver,intentFilter);

        /* BIND SERVICE
        * 1. FotaService
        * */
        mBindServiceHandler.sendEmptyMessage(MSG_SEND_BIND_FOTA_SERVICE);
        mSharedPrefManager = new SharedPrefManager(mContext);

        if (!mBluetoothAdapter.isEnabled()) {
            Logger.w(TAG, "BT is not enabled. wait BT turn on");
//            Intent permIntent = new Intent(this, PermissionReqActivity.class);
//            permIntent.putExtra(PermissionReqActivity.PERM_TYPE, PermissionReqActivity.PERM_TYPE_BT_ENABLE);
//            startActivity(permIntent);
            return;
        }

        if (!checkLocationPermission()) {
            Logger.w(TAG, "No Location permission. Request permission!");
            Intent permIntent = new Intent(this, PermissionReqActivity.class);
            permIntent.putExtra(PermissionReqActivity.PERM_TYPE, PermissionReqActivity.PERM_TYPE_LOCATION);
            startActivity(permIntent);
            return;
        }

        if (!isLocationOpen(this)) {
            Logger.w(TAG, "Location is not enabled. Request user to turn ON!");
            Intent permIntent = new Intent(this, PermissionReqActivity.class);
            permIntent.putExtra(PermissionReqActivity.PERM_TYPE, PermissionReqActivity.PERM_TYPE_LOCATION_ENABLE);
            startActivity(permIntent);
            return;
        }
        isEnvReady = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_KEY_EVENT);
        filter.addAction(ACTION_RECENTERSUCCESS);
        filter.addAction(ACTION_RECENTERFAIL);
        mVolumeKeyEventReceiver = new VolumeKeyEventReceiver();
        registerReceiver(mVolumeKeyEventReceiver, filter);
    }

    @Override
    public void onDestroy() {
        Logger.v(TAG, "onDestroy");
        stopControllerScan();
        mNowStatus = NO_SCAN;
        unregisterReceiver(mBtReceiver);
        unregisterReceiver(mOverlayReceiver);
//        unpairBonedDevices();

        if (null != mFotaUpdateService && mFotaServiceBound) {
            mContext.unbindService(mFotaUpdateConnection);
            mFotaServiceBound = false;
        }

        mFOTAReconnectHandler.removeMessages(MSG_SEND_RECONNECT_DEVICE);

        super.onDestroy();
    }

    @TargetApi(23)
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    public static boolean isLocationOpen(final Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        List<String> providers = manager.getAllProviders();
        for (String prov : providers) {
            if (manager.isProviderEnabled(prov)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    private int findControllers() {
        if (mBleDevMap.isEmpty()) {
            Logger.v(TAG, "No controller found.");
            return 0;
        }

        List<BleDev> devs = getSortedAndRecentBonedBleDevs();

        int count = 0;

        //the first BleDev is which kind controller we want to find.
        String ctrlName = null;
        for (String name : mDevNameList) {
            if (devs.get(0).mName.startsWith(name)) {
                ctrlName = name;
                break;
            }
        }

        if (null == ctrlName) {
            Logger.e(TAG, "can't reach here!");
            return 0;
        }

        if (null == mSelectControllerType) {
            mSelectControllerType = ctrlName;

            //for temp solution
            if (isFinch6DofBleDev(devs.get(0).mName)) {
                mIsFinch6Dof = true;
                SCAN_CONTROLLER_COUNT = 2;
            } else {
                mIsFinch6Dof = false;
                SCAN_CONTROLLER_COUNT = 1;
            }
            Logger.v(TAG, "try to find " + mSelectControllerType + " controller " + "mIsFinch6Dof " + mIsFinch6Dof);
        }

        if (mIsFinch6Dof) {
            for (BleDev bleDev : devs) {
                if (!isFinch6DofBleDev(bleDev.mName)) {
                    continue;
                }

                if (bleDev.mName.contains(RIGHT_STRING)) {
                    if (addToFinch6DofMap(RIGHT_STRING, bleDev)) {
                        addToBondMap(bleDev);
                    }
                }
                if (bleDev.mName.contains(LEFT_STRING)) {
                    if (addToFinch6DofMap(LEFT_STRING, bleDev)) {
                        addToBondMap(bleDev);
                    }
                }
            }

            count = getFinch6DofControllerCnt();
        } else {
            for (BleDev device : devs) {
                Logger.d(TAG, "findControllers :" + hashAddress(device.mAddr));
                if (!device.mName.startsWith(mSelectControllerType)) {
                    continue;
                }

                addToBondMap(device);

                count++;
                Logger.d(TAG, "got 3dof controller : " + hashAddress(device.mAddr) + " " + count);
                if (count >= SCAN_CONTROLLER_COUNT) {
                    break;
                }
            }
        }

        if (0 == count) {
            Logger.v(TAG, "No valible controller found.");
        }

        return count;
    }

    //device need bond serially, only one device can be bond one time.
    private void startBondInMainThread() {
        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDING);
        if (devs != null && !devs.isEmpty()) {
            Logger.d(TAG, "there is a device bonding. can't start another bond.");
            return;
        }

        devs = mBondMap.get(BluetoothDevice.BOND_NONE);
        if (null == devs || devs.isEmpty()) {
            Logger.v(TAG, "no unbond device");
            return;
        }

        BleDev bleDev = devs.get(0);

        //move to bonding list.
        devs.remove(0);
        devs = mBondMap.get(BluetoothDevice.BOND_BONDING);
        if (null == devs) {
            devs = new ArrayList<>();
            devs.add(bleDev);
            mBondMap.put(BluetoothDevice.BOND_BONDING, devs);
        } else {
            devs.add(bleDev);
        }

        BluetoothDevice bluetoothDevice = mBleDevMap.get(bleDev).getDevice();
        if (null == bluetoothDevice) {
            Logger.e(TAG, "can't be null!");
            return;
        }

        Logger.d(TAG, "create bond for " + hashAddress(bleDev.mAddr));
        bluetoothDevice.createBond();
    }

    private void startBond() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                startBondInMainThread();
            }
        });
    }

    private boolean stopControllerScan() {
        if (stopRescan()) {
            Logger.d(TAG, "stopRescan()");
            return true;
        }

        /*if (mTimerActive) {
            mCheckTimer.cancel();
            mTimerActive = false;
        }*/

        if (!mIsScanning) {
            Logger.d(TAG, "already be stopped");
            return false;
        }
        mIsScanning = false;
        Logger.v(TAG, "stop scanning");
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mScanner.flushPendingScanResults(mScanCallback);
            mScanner.stopScan(mScanCallback);
        } else {
            Logger.w(TAG, "bluetooth is not enabled");
        }

        if (mListener != null) {
            try {
                mListener.onScanCompleted();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private boolean haveUnbondDevs() {
        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_NONE);
        if (null == devs || devs.isEmpty()) {
            Logger.v(TAG, "no unbond device");
            return false;
        }

        return true;
    }

    private boolean haveBondingDevs() {
        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDING);
        if (null == devs || devs.isEmpty()) {
            Logger.v(TAG, "no bonding device");
            return false;
        }

        return true;
    }

    private BleDev getBondedDev(String addr) {
        if (mBondMap != null && !mBondMap.isEmpty()) {
            for (BleDev bleDev : mBondMap.get(BluetoothDevice.BOND_BONDED)) {
                if (bleDev.mAddr.equals(addr)) {
                    return bleDev;
                }
            }
        }

        return null;
    }

    private void sendScanResult() {
        if (null == mListener) {
            return;
        }
        List<String> addrList = new ArrayList<>();
        if (mIsFinch6Dof) {
            for (String type : mFinch6DofMap.keySet()) {
                List<BleDev> devs = mFinch6DofMap.get(type);
                if (devs != null && devs.size() == 2) {
                    try {
                        Logger.d(TAG,"+++mListener.onScanResult()");
                        mListener.onBatchScanResults(devs);
                        mIsDeviceServiceConnecting = true;
                        Logger.d(TAG,"---mListener.onScanResult()");
                        for (BleDev dev : devs) {
                            addrList.add(dev.mAddr);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDED);
            if (null == devs) {
                return;
            }

            for (BleDev bleDev : devs) {
                try {
                    Logger.d(TAG,"+++mListener.onScanResult(): " + bleDev.mName + " @ " + hashAddress(bleDev.mAddr));
                    mListener.onScanResult(bleDev);
                    mIsDeviceServiceConnecting = true;
                    mConnectedDevice = bleDev.mAddr;
                    Logger.d(TAG,"---mListener.onScanResult()");
                    addrList.add(bleDev.mAddr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                writeBondedList();
            }
        }).start();
    }

    void checkBondState() {
        if (needFotaUpdate()) {
            startFotaUpdate(false);
        } else {
            if (haveUnbondDevs()) {
                startBond();
            } else if (!haveBondingDevs()){
                sendScanResult();
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (isControllerConnected(mConnectedDevice)){
            return;
        }

        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Logger.w(TAG, "bluetooth is not enabled");
            return;
        }

        if (!isEnvReady) {
            Logger.v(TAG, "system env is not ready.");
            return;
        }

        if (null == mScanner) {
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (null == mScanner) {
                Logger.e(TAG, "mScanner null");
                return;
            }
        }

        if (enable) {
            if (mIsScanning) {
                Logger.d(TAG, "already enabled.");
                return;
            }

            /*mCheckTimer = new Timer();
            mCheckTimer.schedule(new TimerTask(){
                public void run(){
                    Logger.v(TAG, "check to find controller!");
                    if (findControllers() >= SCAN_CONTROLLER_COUNT) {
                        stopControllerScan();
                        mNowStatus = NO_SCAN;
                        checkBondState();
                    }
                }
            }, CHECK_PERIOD, CHECK_PERIOD);*/

            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            //filters.add(new ScanFilter.Builder().setDeviceName("hTC_MVR_Controller").build());
            for (String uuid : mUuidList) {
                filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuid)).build());
            }

            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            ScanSettings settings = builder.build();

            Logger.v(TAG, "start scan ");
            mBleDevMap.clear();
            mBondMap.clear();
            mFinch6DofMap.clear();
            mSelectControllerType = null;
            mIsScanning = true;
            mTimerActive = true;

            //init the scan hashmap before scan
            initBluetoothDeviceInfoMap();
            mScanner.startScan(filters, settings, mScanCallback);

            if (mListener != null) {
                try {
                    mListener.onScanStarted();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (stopControllerScan()) {
                mNowStatus = NO_SCAN;
                if (findControllers() > SCAN_CONTROLLER_COUNT) {
                    checkBondState();
                }
            }
        }
    }

    private void initBluetoothDeviceInfoMap(){
        mBluetoothDeviceInfo.clear();
        mIsCollectDeviceTimerStart = true;
        mIsStartCollectFinch = false;
    }

    private boolean add2RescannedBleDevs(String addr) {
        BleDev dev = getBondedDev(addr);

        if (null == dev) {
            return false;
        }

        for (BleDev bleDev : mRescannedBleDevs) {
            if (bleDev.mAddr.equals(dev.mAddr)) {
                return false;
            }
        }

        mRescannedBleDevs.add(dev);
        return true;
    }

    private boolean stopRescan() {
        if (!mIsRescanning) {
            Logger.d(TAG, "Rescanning already be stopped");
            return false;
        }
        mIsRescanning = false;
        Logger.v(TAG, "stop rescanning");
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mScanner.flushPendingScanResults(mRescanCallback);
            mScanner.stopScan(mRescanCallback);
        } else {
            Logger.w(TAG, "bluetooth is not enabled");
        }
        mHandler.removeCallbacks(mPostResacanRunnable);
        mRescannedBleDevs.clear();
        mToBeRescanBleDevs.clear();

        return true;
    }

    private boolean checkRescanResult(ScanResult result) {
        if(add2RescannedBleDevs(result.getDevice().getAddress())) {
            if (mRescannedBleDevs.size() == mToBeRescanBleDevs.size()) {
                stopControllerScan();
                mNowStatus = NO_SCAN;
                sendScanResult();
                return true;
            }
        }

        return false;
    }

    private ScanCallback mRescanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Logger.d(TAG, "onScanResult " + "callbackType:" + callbackType + " result:" + result);
            Logger.d(TAG, "onReScanResult: " + result.getDevice().getName() + " @ " + hashAddress(result.getDevice().getAddress()));
            if (!mIsRescanning) {
                return;
            }
            checkRescanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Logger.d(TAG, "onBatchScanResults:" + results);
            if (!mIsRescanning) {
                return;
            }
            for (ScanResult result : results) {
                if (checkRescanResult(result) == true) {
                    break;
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Logger.w(TAG, "onScanFailed:" + errorCode);
        }
    };

    private void restartScanner() {
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Logger.w(TAG, "bluetooth is not enabled");
            return;
        }

        if (!isEnvReady) {
            Logger.v(TAG, "system env is not ready.");
            return;
        }

        if (mIsScanning || mIsRescanning) {
            Logger.w(TAG, "already in scanning. " + mIsRescanning);
            return;
        }

        if (null == mScanner) {
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (null == mScanner) {
                Logger.e(TAG, "mScanner null");
                return;
            }
        }

        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDED);
        if (null == devs) {
            Logger.w(TAG, "not any bonded device is found");
            return;
        }

        // Stops scanning after a pre-defined scan period.
        if (null == mPostResacanRunnable) {
            mPostResacanRunnable = new Runnable() {
                @Override
                public void run() {
                    if (stopControllerScan()) {
                        mNowStatus = NO_SCAN;
//                        unpairBonedDevices();
                        mRescannedBleDevs.clear();
                        mToBeRescanBleDevs.clear();
                        Logger.d(TAG,"call NotFound");
                        showPairUI(NotFound);
                        scanLeDevice(true);
                        mNowStatus = SCAN;
                    }
                }
            };
        }
        mHandler.postDelayed(mPostResacanRunnable, RESCAN_PERIOD);
        showPairUI(NotFound);

        mIsRescanning = true;

        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        for (BleDev bleDev : devs) {
            filters.add(new ScanFilter.Builder().setDeviceAddress(bleDev.mAddr).build());
        }

        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        ScanSettings settings = builder.build();
        Logger.v(TAG, "start Rescan");
        mScanner.startScan(filters, settings, mRescanCallback);
    }

    void onPermRequestResult(int type, boolean success) {
        if (PermissionReqActivity.PERM_TYPE_BT_ENABLE == type) {
            if (success) {
                Logger.d(TAG, "bt enable success.");
            } else {
                if (mListener != null) {
                    //mListener.onErrorState();
                }
            }
        }

        if (PermissionReqActivity.PERM_TYPE_LOCATION == type) {
            if (success) {
                Logger.d(TAG, "location permission allowed.");
            } else {
                if (mListener != null) {
                    //mListener.onErrorState();
                }
            }
        }

        if (PermissionReqActivity.PERM_TYPE_LOCATION_ENABLE == type) {
            if (success) {
                Logger.d(TAG, "location enable success.");
                isEnvReady = true;
            } else {
                if (mListener != null) {
                    //mListener.onErrorState();
                }
            }
        }
    }

    private boolean add2ToBeRescanBleDevs(BleDev dev) {
        for (BleDev bleDev : mToBeRescanBleDevs) {
            if (bleDev.mAddr.equals(dev.mAddr)) {
                return false;
            }
        }
        mToBeRescanBleDevs.add(dev);

        return true;
    }
    private class OverlayBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_OEMOVERLAYSTATE.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRAS_PACKAGENAME);
                boolean showStatus = intent.getBooleanExtra(EXTRAS_IS_SHOW,false);
                Logger.d(TAG, "ACTION_OEMOVERLAYSTATE: ["+pkgName+"]"+showStatus);
                if (pkgName.equals(VR_CAMERA)) {
                    mIsCameraOverlay = showStatus;
                } else if (pkgName.equals(VR_MTP_SERVICE)) {
                    mIsMTPOverlay = showStatus;
                } else if (pkgName.equals(VR_POWER_MENU_SERVICE)) {
                    mIsPowerMenuOverlay = showStatus;
                }

                if (showStatus) {
                    mIsOtherOverlay = true;
                    if (mIsShowed) {
                        hideRecenterAnimation();
                        mHvrOverlay.hideOverlay();
                    }
                } else if (mIsOtherOverlay) {
                    if (!(mIsCameraOverlay||mIsMTPOverlay||mIsPowerMenuOverlay)) {
                        mIsOtherOverlay = false;
                        if (mIsShowed) {
                            showActionUI(mSavedViewType);
                        }
                    }
                }
                return;
            }
        }
    }

    private class BtBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Logger.d(TAG, "screen on");
                Logger.d(TAG, "status: " + mNowStatus);
                mIsScreenON = true;

                // to prevent not receiving Intent
                if (mNowStatus == NO_SCAN && !mIsDeviceServiceConnecting &&
                        mConnectedDevice != null && !isControllerConnected(mConnectedDevice)) {
                    mNowStatus = SCAN;
                }

                if (mNowStatus == INITIAL_SCAN) {
                    showPairUI(Init);
                    scanLeDevice(true);
                    mNowStatus = INITIAL_SCAN;
                } else if (mNowStatus == SCAN || mNowStatus == RESCAN) {
                    showPairUI(NotFound);
                    scanLeDevice(true);
                    mNowStatus = SCAN;
                } else if (mNowStatus == NO_SCAN){
                    //todo
                    //show "press home key"
                    mIsShowScreenOn = true;
                    ShowScreenOnUI();
                }
                return;
            }
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Logger.d(TAG, "screen off");
                Logger.d(TAG, "status: " + mNowStatus);
                mIsScreenON = false;

                if (mNowStatus == INITIAL_SCAN || mNowStatus == SCAN) {
                    stopControllerScan();
                    hideDashboard();
                } else if (mNowStatus == RESCAN) {
                    stopControllerScan();
//                    unpairBonedDevices();
                    mRescannedBleDevs.clear();
                    mToBeRescanBleDevs.clear();
                    hideDashboard();
                }
                return;
            }

            int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (BluetoothAdapter.STATE_ON == btState) {
                Logger.d(TAG, "[EXTRA_STATE][STATE_ON] BT is enabled");
                int serviceStatus = mNowStatus;
                // stop Controller Scan, and keep the status.
                if (mNowStatus != NO_SCAN) {
                    Logger.d(TAG, "after BT restart, stop scanning");
                    stopControllerScan();
                    mNowStatus = serviceStatus;
                }
                // update BT instance;
                isEnvReady = false;
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (null == mBluetoothAdapter) {
                    Logger.d(TAG, "after BT restart, get null BT DefaultAdapter");
                    return;
                }
                mScanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (null == mScanner) {
                    Logger.e(TAG, "after BT restart, mScanner null");
                    return;
                }
                isEnvReady = true;
                // after waiting BT start, do First scan
                if (mWaitTODoFirstScan) {
                    mWaitTODoFirstScan = false;
                    Logger.d(TAG, "Bluetooth TURN ON, finish waiting and do First scan");
                    doFirstScan();
                    return;
                }
                // if screen is OFF, do scan when screen turn ON.
                if (mIsScreenON) {
                    if (mNowStatus != NO_SCAN) {
                        switch (mNowStatus) {
                            case INITIAL_SCAN:
                                showPairUI(Init);
                                break;
                            case SCAN:
                            case RESCAN:
                                showPairUI(NotFound);
                                break;
                        }
                        Logger.e(TAG, "after BT restart, restart scan");
                        scanLeDevice(true);
                    }
                } else {
                    Logger.d(TAG, "after BT restart, wait screen ON to scan");
                }
                return;
            } else if (BluetoothAdapter.STATE_OFF == btState){
                Logger.d(TAG, "[EXTRA_STATE][STATE_OFF] BT is not enabled");
                isEnvReady = false;
                if (mNowStatus != NO_SCAN) {
                    stopControllerScan();
                }
                return;
            }

            BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (dev != null) {
                String devName = dev.getName();
                String addr = dev.getAddress();

                Logger.d(TAG, "on Receive : " + devName);
                Logger.d(TAG, "action is  : " + action);

                if (null == devName) {
                    return;
                }

                if (!isController(devName)) {
                    Logger.d(TAG, "I'm not care about other bt device.");
                    return;
                }

                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    Logger.d(TAG, "Device State " + dev.getBondState());
                    if (dev.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Logger.d(TAG, "Device bonded with state" + dev.getBondState());
                        BleDev bleDev = getAndRemoveBondingBleDev(dev.getAddress());
                        if (bleDev != null) {
                            addToBondMap(BluetoothDevice.BOND_BONDED, bleDev);
                            checkBondState();
                        }
                    }

                    if (dev.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Logger.d(TAG, "Device:" + hashAddress(dev.getAddress()) + " bonding");
                    }

                    if (dev.getBondState() == BluetoothDevice.BOND_NONE) {
                        BleDev bleDev = getAndRemoveBondingBleDev(dev.getAddress());
                        if (bleDev != null) {//fail to bind device
                            //addToBondMap(BluetoothDevice.BOND_NONE, bleDev);
                            //startBond();
                            Logger.d(TAG, "bond controller fail, do scan");
                            showPairUI(NotFound);
                            scanLeDevice(true);
                            mNowStatus = SCAN;
                        }
                        Logger.d(TAG, "Device:" + hashAddress(dev.getAddress()) + " unbonded");
                    }

                    return;
                }

                if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                            BluetoothAdapter.ERROR);
                    Logger.d(TAG, BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED + " state:" + state);

                    return;
                }

                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    //filter if connected BT device is target device, if yes, do service connection
                    Logger.d(TAG, hashAddress(addr) + " Device Connected.");
                    if (dev.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Logger.d(TAG, hashAddress(addr) + " Device is bonded.");
                    }

                    return;
                }
            }
        }
    }

    private class BindServiceHandler implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEND_BIND_FOTA_SERVICE:
                    Logger.d(TAG, "[MSG_SEND_BIND_FOTA_SERVICE]");
                    Intent intent = new Intent(mContext, FotaUpdateService.class);
                    if (!mContext.bindService(intent, mFotaUpdateConnection, Context.BIND_AUTO_CREATE)) {
                        Logger.d(TAG, "bind FotaUpdate Service fail");
                        mContext.unbindService(mFotaUpdateConnection);
                        mFotaServiceBound = false;
                    }
                    break;
            }
            return false;
        }
    }

    private ServiceConnection mFotaUpdateConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.d(TAG, "FotaUpdateService Connected");
            mFotaUpdateService = ((FotaUpdateService.FotaUpdateBinder) iBinder).getService();
            mFotaServiceBound = true;
            if (mIsStartFotaUpdate) {
                startFotaUpdate(false);
                mIsStartFotaUpdate = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(TAG, "FotaUpdateService Disconnected");
            mFotaUpdateService = null;
            mFotaServiceBound = false;
        }
    };

    private FirmwareUpdateUtils.CheckFotaUpdateListener mFotaUpdateListener = new FirmwareUpdateUtils.CheckFotaUpdateListener() {

        @Override
        public void onCheckFotaUpdateResult(boolean haveUpdate, BleDevInfo deviceInfo) {
            if (null != mSharedPrefManager) {
                Logger.d(TAG, "onCheckFotaUpdateResult: save to SharedPreferences: " + hashAddress(deviceInfo.mAddr) + " haveUpdate: " + haveUpdate);
                mSharedPrefManager.setControllerInfo(deviceInfo.mAddr, haveUpdate);
            }
        }

        @Override
        public void onFotaUpdateCompleted(BleDevInfo deviceInfo) {
            Logger.d(TAG, "onFotaUpdateCompleted: deviceInfo: " + hashAddress(deviceInfo.mAddr));
            registerReceiver(mBtReceiver, makeIntentFilter());
            mCurrentType = FOTASuccess;
            mIsFOTAing = false;
            showActionUI(mCurrentType);
            mSharedPrefManager.setControllerInfo(deviceInfo.mAddr, false);
            mFOTAReconnectHandler.sendMessageDelayed(getReconnectDeviceMessage(deviceInfo.mAddr), RESCAN_DEVICE);
        }

        @Override
        public void onStatusChanged(int status, BleDevInfo deviceInfo) {
            Logger.d(TAG, "onStatusChanged: deviceInfo: " + hashAddress(deviceInfo.mAddr) + " status: " + status);
            switch (status) {
                case FotaServiceContract.STATE_FOTA_DOWNLOAD_START:
                    mCurrentType = FOTADownload;
                    showActionUI(mCurrentType);
                    break;
                case FotaServiceContract.STATE_FOTA_START:
                    mCurrentType = FOTAInstall;
                    showActionUI(mCurrentType);
                    break;
                case FotaServiceContract.STATE_FOTA_ERROR:
                    mCurrentType = FOTAFailed;
                    mIsFOTAing = false;
                    registerReceiver(mBtReceiver, makeIntentFilter());
                    mSharedPrefManager.setControllerInfo(deviceInfo.mAddr, false);
                    mFOTAReconnectHandler.sendMessageDelayed(getReconnectDeviceMessage(deviceInfo.mAddr), RESCAN_DEVICE);
                    showActionUI(mCurrentType);
                    break;
                case FotaServiceContract.STATE_DEVICE_LOW_BATTERY:
                    mCurrentType = ControllerLowBattery;
                    mIsFOTAing = false;
                    registerReceiver(mBtReceiver, makeIntentFilter());
                    mSharedPrefManager.setControllerInfo(deviceInfo.mAddr, false);
                    mFOTAReconnectHandler.sendMessageDelayed(getReconnectDeviceMessage(deviceInfo.mAddr), RESCAN_DEVICE);
                    showActionUI(mCurrentType);
                    break;
                case FotaServiceContract.STATE_NO_FOTA_UPDATE:
                    mIsFOTAing = false;
                    registerReceiver(mBtReceiver, makeIntentFilter());
                    mFOTAReconnectHandler.sendMessageDelayed(getReconnectDeviceMessage(deviceInfo.mAddr), RESCAN_DEVICE);
                    break;
            }
        }

        @Override
        public void onProgressChanged(final int progress) {
            Logger.d(TAG, "onProgressChanged:" + progress);
            mCurrentProgress = progress;
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(progress);
                }
            });

            //Hugh mark
            //mUnityPlayerMessageSender.updateDownloadProgress(progress);
        }
    };

    private void checkFotaUpdate(BleDevInfo bleDevInfo) {
        if (!Utils.isNetworkConnected(TAG, mContext)) {
            Logger.d(TAG, "check FotaUpdate fail: NO Network Connected");
            return;
        }

        if (null == mFotaUpdateService && !mFotaServiceBound) {
            Logger.d(TAG, "FotaUpdateService is not bound");
            return;
        }
        Logger.d(TAG, "check FotaUpdate");
        mFotaUpdateService.checkFotaUpdate(mFotaUpdateListener, bleDevInfo);
    }

    private boolean needFotaUpdate() {
        if (!Utils.isNetworkConnected(TAG, mContext)) {
            Logger.d(TAG, "No need to do Fota Update: NO Network Connected");
            return false;
        }

        List<BleDev> devs = mBondMap.get(BluetoothDevice.BOND_BONDED);
        if (null == devs || devs.isEmpty()) {
            Logger.v(TAG, "needFotaUpdate: no bonded device");
            return false;
        }
        BleDev bleDev = devs.get(0);

        Map<String, Boolean> controllerInfoMap = mSharedPrefManager.getControllerInfo();
        if (null == controllerInfoMap || controllerInfoMap.isEmpty()) {
            Logger.v(TAG, "needFotaUpdate: no controller SharedPref Info");
            return false;
        }
        // Only support 3dof now
        for (String savedAddress : controllerInfoMap.keySet()) {
            boolean doFota = controllerInfoMap.get(savedAddress);
            Logger.d(TAG, "mSharedPrefManager: " + hashAddress(savedAddress) + " , doFota: " + doFota);
            if (bleDev.mAddr.equals(savedAddress) && doFota) {
                Logger.d(TAG, "find controller do FOTA update: " + bleDev.mName);
                mFotaBleDev = new BleDevInfo(bleDev.mAddr, bleDev.mName,"","","");
                return true;
            }
        }
        return false;
    }

    private void startFotaUpdate(boolean isRetry) {
        if (mFotaUpdateService == null || !mFotaServiceBound || mFotaBleDev == null || !isOOBEFakeFinished()) {
            Logger.d(TAG, "start FotaUpdate fail, is OOBE finished : " + isOOBEFakeFinished());
            mIsStartFotaUpdate = true;
            return;
        }
        mCurrentType = FOTAUpdateAvailable;
        mIsFOTAing = true;
        showActionUI(mCurrentType);
        Logger.d(TAG, "start FotaUpdate");
        try {
            unregisterReceiver(mBtReceiver);
        }catch (Exception e){
            Logger.e(TAG, "unregister bt receiver failed: " + e.getMessage());
        }
        mFotaUpdateService.StartFotaUpdate(mFotaUpdateListener, mFotaBleDev, isRetry);
    }

    private Message getReconnectDeviceMessage(String mac) {
        Message message = Message.obtain();
        message.what = MSG_SEND_RECONNECT_DEVICE;
        message.obj = mac;
        return message;
    }

    private class FOTAReconnectHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEND_RECONNECT_DEVICE:
                    String mac = (String) msg.obj;
                    Logger.d(TAG, "[MSG_SEND_RECONNECT_DEVICE] : " + hashAddress(mac));
                    hideAllPages();
                    try {
                        doFirstScan();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
            return false;
        }
    }

    private boolean isOOBEFakeFinished(){
        return true;
    }

    private boolean isOOBEFinished() {
        boolean isFinished = true;

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(MVR_OOBE_PACKAGENAME);
            ResolveInfo info = mContext.getPackageManager().resolveActivity(intent, 0);
            if (info != null) {
                isFinished = false;
            } else {
                isFinished = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isFinished;
    }

    private void showPairUI(ActionTypeName type) {
        boolean isOOBEFinished = isOOBEFakeFinished();
        if (isOOBEFinished) {
            showActionUI(type);
        } else {
            switch (type) {
                case Init:
                    Log.d(TAG, "[showPairUI] Init isOOBEFinished : " + isOOBEFinished);
                    sendActionToOOBE(ACTION_SCAN_DEVICE);
                    break;
                case NotFound:
                    Log.d(TAG, "[showPairUI] NotFound isOOBEFinished : " + isOOBEFinished);
                    sendActionToOOBE(ACTION_DEVICE_DISCONNECT);
                    break;
            }
        }
    }

    private void hideAllPages() {
        boolean isOOBEFinished = isOOBEFakeFinished();
        Log.d(TAG, "[hideAllPages] isOOBEFinished : " + isOOBEFinished);
        if (!isOOBEFinished) {
            sendActionToOOBE(ACTION_HIDE_UI);
        }
        hideDashboard();
    }

    private void sendActionToOOBE(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(MVR_OOBE_PACKAGENAME);
        sendBroadcast(intent);
    }

    private void showActionUI(ScannerService.ActionTypeName type) {
        boolean isOOBEFinished = isOOBEFinished();
        Log.d(TAG, "[showActionUI] type : " + type + ", isOOBEFinished: " + isOOBEFinished);
        if(mHvrOverlay == null) {
            Log.d(TAG, "[showActionUI] mHvrOverlay is null !");
            return;
        }
        int spanTextResID = 0;
        int animationImageViewResID = 0;
        int resource;
        String addr = mSharedPrefManager.getPairMacAddress();
        switch (type) {
            case ScreenOn:
                if(isOOBEFinished) {
                    resource = R.layout.controller_welcome_new_layout;
                    spanTextResID = R.id.welcome_description;
                    animationImageViewResID = R.id.welcome_imageview;
                }else{
                    resource = R.layout.controller_oobe_layout;
                    animationImageViewResID = R.id.oobe_welcome_imageview;
                }
                break;
            case Init:
                if(isOOBEFinished) {
                    if (!TextUtils.isEmpty(addr)) {
                        resource = R.layout.controller_searching_layout;
                        spanTextResID = R.id.warning_text;
                        animationImageViewResID = R.id.disconnect_imageview;
                    } else {
                        resource = R.layout.controller_welcome_new_layout;
                        spanTextResID = R.id.welcome_description;
                        animationImageViewResID = R.id.welcome_imageview;
                    }
                }else{
                    resource = R.layout.controller_oobe_layout;
                    animationImageViewResID = R.id.oobe_welcome_imageview;
                }
                break;
            case NotFound:
                if(isOOBEFinished) {
                    if (!TextUtils.isEmpty(addr)) {
                        resource = R.layout.controller_searching_layout;
                        spanTextResID = R.id.warning_text;
                        animationImageViewResID = R.id.disconnect_imageview;
                    } else {
                        resource = R.layout.controller_welcome_new_layout;
                        spanTextResID = R.id.welcome_description;
                        animationImageViewResID = R.id.welcome_imageview;
                    }
                }else{
                    resource = R.layout.controller_oobe_layout;
                    animationImageViewResID = R.id.oobe_welcome_imageview;
                }
                break;
            case FOTAUpdateAvailable:
                hideAllPages();
                resource = R.layout.controller_update_available_layout;
                break;
            case FOTADownload:
                showDownloadPage();
                return;
            case FOTAInstall:
                resource = R.layout.controller_updating_layout;
                break;
            case FOTASuccess:
                resource = R.layout.controller_update_success_layout;
                break;
            case FOTAFailed:
                resource = R.layout.controller_update_fail_layout;
                break;
            case ControllerLowBattery:
                resource = R.layout.controller_low_battery_layout;
                break;
            default:
                Log.d(TAG, "[showActionUI] default");
                resource = R.layout.controller_disconnect_layout;
                break;
        }
        RelativeLayout launcherLayout=(RelativeLayout) ((LayoutInflater)
                this.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(resource, null);

        if(spanTextResID != 0) {
            Utils.renderPressHomeBtnUIText(this, launcherLayout, spanTextResID);
        }

        if(animationImageViewResID != 0){
            showRecenterAnimation(launcherLayout, animationImageViewResID);
        }

        mSavedViewType = type;
        showHVrOverlay(launcherLayout);
    }

    private synchronized void showRecenterAnimation(RelativeLayout layout, @IdRes int imageViewResID){
        try {
            Log.d(TAG, "[showRecenterAnimation] isScreenON: " + mIsScreenON);

            if(mControllerAnimation != null){
                mControllerAnimation.stop();
                mControllerAnimation = null;
            }

            mControllerAnimation = Utils.renderPressHomeBtnAnimation(this, layout, imageViewResID);

            if (mControllerAnimation != null && mIsScreenON) {
                mControllerAnimation.start();
            }
        }catch (Exception e){
            Log.e(TAG, "[showRecenterAnimation] start press home btn animation failed: " + e.getMessage(), e);
        }
    }

    private synchronized void hideRecenterAnimation(){
        try {
            Logger.d(TAG, "[hideRecenterAnimation] isScreenON: " + mIsScreenON);

            if(mControllerAnimation != null){
                mControllerAnimation.stop();
                mControllerAnimation = null;
            }

        }catch (Exception e){
            Log.e(TAG, "[hideRecenterAnimation] hide press home btn animation failed: " + e.getMessage(), e);
        }
    }

    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private RelativeLayout mDownloadPage;
    private void showDownloadPage() {
        mDownloadPage = (RelativeLayout) ((LayoutInflater)
                this.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.controller_updating_layout,null);
        mProgressBar = (ProgressBar) mDownloadPage.findViewById(R.id.progressBar);
        mProgressBar.setIndeterminate(false);
        mProgressBar.setMax(100);
        mProgressBar.setProgress(mCurrentProgress);
        TextView title = (TextView) mDownloadPage.findViewById(R.id.updating_status_title);
        title.setText(getString(R.string.fw_download_progress_description));
        mProgressText = (TextView) mDownloadPage.findViewById(R.id.updating_percentage);
        mProgressText.setVisibility(View.VISIBLE);
        mProgressText.setText(mCurrentProgress + "%");
        showHVrOverlay(mDownloadPage);
    }

    private void setProgress(int progress) {
        if (mProgressBar == null || mProgressText == null || mDownloadPage == null) {
            Log.w(TAG, "[setProgress] something null !!");
            return;
        }
        mProgressBar.setProgress(progress);
        mProgressText.setText(progress + "%");
        showHVrOverlay(mDownloadPage);
    }

    private void showHVrOverlay(View view){
        mIsShowed = true;
        Logger.d(TAG,"showHVrOverlay:"+mIsOtherOverlay);
        if (!mIsOtherOverlay) {
            mHvrOverlay.showOverlay(view);
            double[] matrix = new double[] { 0.0f, 0.0f, -1.013f};
            mHvrOverlay.setOverlayFixedPosition(matrix);
        }
    }

    public enum ActionTypeName {
        ScreenOn,
        Init,
        NotFound,
        FOTAUpdateAvailable,
        FOTADownload,
        FOTAInstall,
        FOTASuccess,
        FOTAFailed,
        ControllerLowBattery
    }

    class BluetoothDeviceInfo {
        public long timestamp;
        public ScanResult result;

        public BluetoothDeviceInfo(ScanResult result, long timestamp) {
            this.timestamp = timestamp;
            this.result = result;
        }
    }

    private class CollectPairDevice implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEND_PAIR_DEVICE:
                    mIsStartCollectFinch = false;
                    foundPairDevice(false);
                    break;
                case MSG_EXTEND_SEARCH_TIME:
                    foundPairDevice(true);
                    break;
            }
            return false;
        }
    }

    private void foundPairDevice(boolean isExtendCase) {
        boolean isDevicePaired = false;
        long currentTime = System.currentTimeMillis();
        for (BluetoothDeviceInfo info: getSortedDevs()) {
            long existTime = currentTime - info.timestamp;
            Log.d(TAG, "[sortedDevs] addr : " + hashAddress(info.result.getDevice().getAddress()) + ", rssi : " +
                    info.result.getRssi() + ", timestamp : " + info.timestamp + ", exist time : " + existTime);
            if (existTime < DEVICE_EXIST_TIME) {
                if (!isExtendCase) {
                    int delayTime = getDelayTime(info.result.getRssi());
                    if (delayTime > 0) {
                        mCollectPairDeviceHandler.sendEmptyMessageDelayed(MSG_EXTEND_SEARCH_TIME, delayTime);
                    } else {
                        addBleDev(info.result);
                    }
                } else {
                    addBleDev(info.result);
                }
                isDevicePaired = true;
                break;
            }
        }
        if (!isDevicePaired) {
            resetSearchDeviceRule();
        }
    }

    private List<BluetoothDeviceInfo> getSortedDevs() {
        ConcurrentHashMap<String, BluetoothDeviceInfo> deviceInfo = new ConcurrentHashMap<>(mBluetoothDeviceInfo);
        for(String addr : deviceInfo.keySet()) {
            Log.d(TAG, "[MSG_SEND_PAIR_DEVICE] addr : " + hashAddress(addr) + ", rssi : " +
                    deviceInfo.get(addr).result.getRssi() + ", timestamp : " + deviceInfo.get(addr).timestamp);
        }
        List<BluetoothDeviceInfo> sortedDevs = new ArrayList<>();
        List<Map.Entry<String, BluetoothDeviceInfo>> entryList = new ArrayList<Map.Entry<String, BluetoothDeviceInfo>>(deviceInfo.entrySet());
        Collections.sort(entryList,
                new Comparator<Map.Entry<String, BluetoothDeviceInfo>>() {
                    public int compare(Map.Entry<String, BluetoothDeviceInfo> entry1,
                                       Map.Entry<String, BluetoothDeviceInfo> entry2) {
                        int value1 = entry1.getValue().result.getRssi();
                        int value2 = entry2.getValue().result.getRssi();
                        return value2 - value1;
                    }
                });
        Iterator<Map.Entry<String, BluetoothDeviceInfo>> iter = entryList.iterator();
        Map.Entry<String, BluetoothDeviceInfo> tmpEntry = null;
        while (iter.hasNext()) {
            tmpEntry = iter.next();
            sortedDevs.add(tmpEntry.getValue());
        }
        return sortedDevs;
    }

    private int getDelayTime(int rssi) {
        int delayTime = 0;
        if (rssi > RSSI_H) {
            delayTime = DELAY_H;
        } else if (rssi <= RSSI_H && rssi > RSSI_M) {
            delayTime = DELAY_M;
        } else {
            delayTime = DELAY_L;
        }
        return delayTime;
    }

    private void resetSearchDeviceRule() {
        mBluetoothDeviceInfo.clear();
        mIsCollectDeviceTimerStart = true;
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

    private String revertDFUMacAddress(String address) {
        Logger.w(TAG, "[revertDFUMacAddress] address : " + hashAddress(address));
        String dfuAddress = address;
        try {
            /**
             *  The rule for DFU mac address change to normal mac address is reduce 1
             *  ex: DFU - 00:00:00:00:00:00 > normal - 00:00:00:00:00:FF
             */
            if (!TextUtils.isEmpty(address)) {
                String last2Address = address.substring(address.lastIndexOf(":") + 1, address.length());
                BigInteger value = new BigInteger(last2Address, 16);
                value = value.add(new BigInteger("-1"));
                String newValue = value.toString(16);
                if (newValue.length() > 2) {
                    newValue = newValue.substring(1, newValue.length());
                }
                dfuAddress = address.replace(last2Address, newValue).toUpperCase();
            }
            Log.d(TAG, "[revertDFUMacAddress] dfuAddress : " + hashAddress(dfuAddress));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dfuAddress;
    }

    public class VolumeKeyEventReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "[VolumeKeyEventReceiver] action : " + intent.getAction());
            switch (intent.getAction()) {
                case VOLUME_KEY_EVENT:
                    int volumeType = intent.getIntExtra("volume_type", -99);
                    Logger.d(TAG, "[VOLUME_KEY_EVENT] action : " + intent.getAction() + ", volume type : " + intent.getIntExtra("volume_type", -99)
                            + ", volume value : " + intent.getIntExtra("volume_value", -99) + ", is scanning : " + mIsScanning + ", is re scanning : " + mIsRescanning);
                    if ((mIsScanning || mIsRescanning) && volumeType == 3) {
                        /*Logger.d(TAG, "[VOLUME_KEY_EVENT] action : " + intent.getAction() + ", volume type : " + intent.getIntExtra("volume_type", -99)
                                  + ", volume value : " + intent.getIntExtra("volume_value", -99) + ", is scanning : " + mIsScanning);*/
                        mSharedPrefManager.setMacAddress("");
                        if (mIsScanning) {
                            showPairUI(Init);
                            mNowStatus = INITIAL_SCAN;
                        } else {
                            if (stopControllerScan()) {
                                mNowStatus = NO_SCAN;
                                mRescannedBleDevs.clear();
                                mToBeRescanBleDevs.clear();
                                Logger.d(TAG,"call NotFound");
                                showPairUI(NotFound);
                                scanLeDevice(true);
                                mNowStatus = SCAN;
                            }
                        }
                    }
                    break;
                case ACTION_RECENTERSUCCESS:
                    Logger.d(TAG, "[onButton: EVENT] : Recenter Success");
                    hideAllPages();
                    break;
                case ACTION_RECENTERFAIL:
                    Logger.d(TAG, "[onButton: EVENT] : Recenter Failed");
                    hideAllPages();
                    break;
            }
        }
    }

    private void ShowScreenOnUI() {
        String addr = mSharedPrefManager.getPairMacAddress();
        Log.d(TAG, "[ShowScreenOnUI] addr : " + hashAddress(addr));
        if (!TextUtils.isEmpty(addr)) {
            try {
                Log.d(TAG, "[ShowScreenOnUI] isConnected : " + mListener.isConnected(addr));
                if (mListener.isConnected(addr)) {
                    showPairUI(ScreenOn);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String fullHashAddress(String str)
    {
        try
        { // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(str.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for(int i=0; i<messageDigest.length; i++)
            {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "";
    }
}