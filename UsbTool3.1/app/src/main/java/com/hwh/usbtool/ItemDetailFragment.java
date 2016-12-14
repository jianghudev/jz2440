package com.hwh.usbtool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A fragment representing a single Item detail screen.
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    UsbManager mUsbManager;
    UsbDevice mUsbDevice;
    UsbDeviceConnection mUsbConnection;
    UsbInterface mUsbInterface;
    UsbEndpoint mEpIn;
    UsbEndpoint mEpOut;

    private Activity mActivity;
    private TextView mInfoTv;
    private LinearLayout mParentLayout;
    private TextView mEpInTV;
    private CheckBox mShowInHex;
    private CheckBox mInOutHex;
    private EditText mEpOutET;
    private Button mEpInStart;
    private Button mEpInStop;

    private boolean mEpInPolling = true;
    private String mEpInStr = null;

    public static final String ARG_PID = "PID";
    public static final String ARG_VID = "VID";
    public static final String ARG_FID = "FID";

    private int mVID = 0;
    private int mPID = 0;
    private int mFID = 0;

    public final static int MSG_UPDATE_EP_IN_DATA = 1;
    public final static int MSG_UPDATE_EP_OUT_DATA = 2;
    public final static int MSG_UPDATE_DELAY_CONNECT = 3;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_VID)) {
            mVID = getArguments().getInt(ARG_VID);
        }
        if (getArguments().containsKey(ARG_PID)) {
            mPID = getArguments().getInt(ARG_PID);
        }
        if (getArguments().containsKey(ARG_FID)) {
            mFID = getArguments().getInt(ARG_FID);
        }

        mActivity = this.getActivity();
        mActivity.registerReceiver(mUsbReceiver, new IntentFilter(Usb.ACTION_USB_PERMISSION));
        mUsbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) mActivity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            //appBarLayout.setTitle("VID:" + mVID + " PID:" + mPID + " FID:" + mFID);
            appBarLayout.setTitle("Interface ID:" + mFID);
        }

        mUsbDevice = Usb.getUsbDevice(mUsbManager, mVID, mPID);
        if (mUsbDevice != null) {
            requestPermission();
        }
    }

    @Override
    public void onDestroy() {
        mEpInPolling = false;
        mActivity.unregisterReceiver(mUsbReceiver);
        disconnectDevice();
        super.onDestroy();
    }

    public void requestPermission() {
        // Setup Pending Intent
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this.getActivity(), 0, new Intent(Usb.ACTION_USB_PERMISSION), 0);
        if (mUsbDevice != null && !mUsbManager.hasPermission(mUsbDevice)) {
            mUsbManager.requestPermission(mUsbDevice, permissionIntent);
        } else if (mUsbManager.hasPermission(mUsbDevice)) {
            delayConnectDevice();
        }
    }

    public void delayConnectDevice() {
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DELAY_CONNECT, 100);
    }

    public void connectDevice() {
        mUsbInterface = Usb.getInterface(mUsbDevice, mFID);
        if (mUsbConnection == null) {
            mUsbConnection = mUsbManager.openDevice(mUsbDevice);
        }
        mParentLayout = (LinearLayout) mActivity.findViewById(R.id.parent_detail);
        mInfoTv = ((TextView) mActivity.findViewById(R.id.item_detail));
        if (mUsbConnection != null && mUsbConnection.claimInterface(mUsbInterface, true)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Type:" + mUsbInterface.getInterfaceClass() + "\n");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sb.append("PRODUCT:" + mUsbDevice.getProductName() + "\n");
            }
            sb.append("EP Count:" + mUsbInterface.getEndpointCount() + "\n");
            for (int j = 0; j < mUsbInterface.getEndpointCount(); j++) {
                UsbEndpoint ep = mUsbInterface.getEndpoint(j);
                sb.append("=======================\n");
                sb.append("---EndpointNumber: " + ep.getEndpointNumber() + "\n");
                sb.append("---Address: " + ep.getAddress() + "\n");
                sb.append("---Direction: " + ep.getDirection() + "\n");
                sb.append("---MaxPackageSize: " + ep.getMaxPacketSize() + "\n");
                sb.append("---Interval: " + ep.getInterval() + "\n");
                sb.append("---Attributes: " + ep.getAttributes() + "\n");
                sb.append("---Type: " + ep.getType() + "\n");
            }
            if (mInfoTv != null) {
                mInfoTv.setText(sb.toString());
            }
        } else {
            new  AlertDialog.Builder(ItemDetailFragment.this.getActivity())
                    .setTitle("Error" )
                    .setMessage("Can't connect to USB device")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (mUsbInterface.getEndpointCount() == 0) {
            return;
        }

        mEpIn = Usb.getDirEndpoint(mUsbInterface, 128);
        mEpOut = Usb.getDirEndpoint(mUsbInterface, 0);

        if (mEpIn != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LayoutInflater inflater3 = LayoutInflater.from(mActivity);
            View view = inflater3.inflate(R.layout.endpoint_in, null);
            view.setLayoutParams(lp);
            if (mParentLayout != null) {
                mParentLayout.addView(view);
            }

            mEpInStr = "";
            mEpInTV = (TextView) mActivity.findViewById(R.id.epInTv);
            //mEpInTV.setText("");
            mEpInTV.setMaxHeight(100);
            //mEpInTV.setMovementMethod(new ScrollingMovementMethod());//会死机
            mEpInStart = (Button) mActivity.findViewById(R.id.epInStart);
            mEpInStop = (Button) mActivity.findViewById(R.id.epInStop);
            mEpInStart.setEnabled(true);
            mEpInStop.setEnabled(false);
            mEpInStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEpInPolling = true;
                    mEpInStart.setEnabled(false);
                    mEpInStop.setEnabled(true);
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_EP_IN_DATA, 200);
                }
            });
            mEpInStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEpInPolling = false;
                    mEpInStart.setEnabled(true);
                    mEpInStop.setEnabled(false);
                }
            });
            mShowInHex = (CheckBox) mActivity.findViewById(R.id.epInHex);
        }

        if (mEpOut != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LayoutInflater inflater3 = LayoutInflater.from(mActivity);
            View view = inflater3.inflate(R.layout.endpoint_out, null);
            view.setLayoutParams(lp);
            if (mParentLayout != null) {
                mParentLayout.addView(view);
            }

            mEpOutET = (EditText) mActivity.findViewById(R.id.epOutEt);
            mEpOutET.setText("");
            Button stop = (Button) mActivity.findViewById(R.id.epOutSD);
            stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_EP_OUT_DATA, 50);
                }
            });
            mInOutHex = (CheckBox) mActivity.findViewById(R.id.epOutHex);
        }
    }

    public void disconnectDevice() {
        if (mUsbConnection != null) {
            mUsbConnection.releaseInterface(mUsbInterface);
            mUsbConnection.close();
        }
        mUsbDevice = null;
        mUsbConnection = null;
        mUsbInterface = null;
        mEpIn = null;
        mEpOut = null;
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_EP_IN_DATA: {
                    if (mEpInPolling) {
                        String txt = Usb.readFromEndpoit(mUsbConnection, mEpIn, mShowInHex.isChecked());
                        Log.d(MainActivity.TAG, "__jh__ read data is= " + txt + "\n");
                        if (txt != null) {
                            //mEpInStr += txt;
                            mEpInStr = txt;
                            mEpInTV.setText(mEpInStr);
                        }
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_EP_IN_DATA, 200);
                    }
                    break;
                }
                case MSG_UPDATE_EP_OUT_DATA: {
                    String txt = mEpOutET.getText().toString();
                    if (txt.isEmpty()) {
                        Toast.makeText(mActivity.getApplicationContext(), "Please input data to send.", Toast.LENGTH_SHORT).show();
                    } else {
                        int ret = 0;
                        if (mInOutHex.isChecked()) {
                            byte buf[] = HexString2Bytes(txt);
                            ret = Usb.sendToEndpoint(mUsbConnection, mEpOut, buf);
                        } else {
                            byte sendData[]= new byte[3];
                            try {
                                byte char0 =  (byte)txt.charAt(0);
                                int char1 = Integer.parseInt(String.valueOf(txt.charAt(1)));
                                int char2 = Integer.parseInt(String.valueOf(txt.charAt(2)));

                                Log.d(MainActivity.TAG, "send str[012]= " + char0 +char1+char2+ "\n");
                                sendData[0]=char0;
                                sendData[1]=(byte)char1;
                                sendData[2]=(byte)char2;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }

                            ret = Usb.sendToEndpoint(mUsbConnection, mEpOut, sendData);
                        }

                        Toast.makeText(mActivity.getApplicationContext(), "Sent result is: " + ret, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case MSG_UPDATE_DELAY_CONNECT: {
                    connectDevice();
                    break;
                }
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        _b0 = (byte)(_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        byte ret = (byte)(_b0 ^ _b1);
        return ret;
    }

    public static byte[] HexString2Bytes(String src){
        int cnt = src.length();
        byte[] ret = new byte[cnt/2];
        byte[] tmp = src.getBytes();
        for(int i=0; i<cnt/2; i++){
            ret[i] = uniteBytes(tmp[i*2], tmp[i*2+1]);
        }
        return ret;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Usb.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        delayConnectDevice();
                    } else {
                        new  AlertDialog.Builder(ItemDetailFragment.this.getActivity())
                                .setTitle("Error" )
                                .setMessage("No USB permission")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_detail, container, false);

        // Show the dummy content as text in a TextView.
        mInfoTv = ((TextView) rootView.findViewById(R.id.item_detail));
        mInfoTv.setText("");

        mParentLayout = (LinearLayout) rootView.findViewById(R.id.parent_detail);

//        mUsbDevice = Usb.getUsbDevice(mUsbManager, mVID, mPID);
//        if (mUsbDevice != null) {
//            requestPermission();
//        }

        return rootView;
    }
}
