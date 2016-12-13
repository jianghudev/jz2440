package com.hwh.usbtool;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    final static String TAG = "USB";
    //List<String> parents = null;
    //List<List> children = null;
    ExpandableListView listView = null;
    TextView textView = null;
    TextView titleView = null;

    UsbManager mUsbManager;
    List<UsbDevice> mUsbDeviceList = new ArrayList<UsbDevice>();
    List<List> mDeviceInterfaces = new ArrayList<List>();

    private static int VIEW_TAG_ID_DEVICE = 0x100;
    private static int VIEW_TAG_ID_INTERFACE = 0x200;

    private final static int MSG_DELAY_UPDATE = 1;

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DELAY_UPDATE: {
                    updateUsbList();
                    break;
                }
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void delayUpdateUsbList() {
        mHandler.sendEmptyMessageDelayed(MSG_DELAY_UPDATE, 500);
    }

    private void updateUsbList() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        mUsbDeviceList.clear();
        mDeviceInterfaces.clear();

        UsbDevice device;
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            mUsbDeviceList.add(device);
            List<UsbInterface> children = new ArrayList<UsbInterface>();
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface usbInterface = device.getInterface(i);
                children.add(usbInterface);
            }
            mDeviceInterfaces.add(children);
        }

        if (mUsbDeviceList.size() > 0) {
            titleView.setVisibility(View.VISIBLE);
            listView.collapseGroup(0);
            listView.expandGroup(0);
            textView.setText("");
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Usb.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                delayUpdateUsbList();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        registerReceiver(mUsbReceiver, new IntentFilter(Usb.ACTION_USB_PERMISSION));
        registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "USB Tool Version 1.0", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        textView = (TextView)findViewById(R.id.mText);
        textView.setMovementMethod(new ScrollingMovementMethod());

        titleView = (TextView)findViewById(R.id.listTitle);
        titleView.setVisibility(View.INVISIBLE);

        listView = (ExpandableListView)findViewById(R.id.expand);
        listView.setAdapter(new MyExpandableListAdapter());
        listView.setGroupIndicator(null);
        delayUpdateUsbList();

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, final View arg1, int arg2, long arg3) {
                int deviceIdx = (int) arg1.getTag(R.id.view_tag_device);
                int interfaceIdx = (int) arg1.getTag(R.id.view_tag_interface);

                deviceIdx = deviceIdx & 0xFF;
                if (mUsbDeviceList.size() == 0 || deviceIdx >= mUsbDeviceList.size()) {
                    return true;
                }
                UsbDevice device = (UsbDevice) mUsbDeviceList.get(deviceIdx);

                String info;
                if (interfaceIdx == 0) {
                    info = Usb.getDeviceInfo(device);
                    textView.setText(info);
                } else {
                    interfaceIdx = interfaceIdx & 0xFF;
                    UsbInterface usbInterface = (UsbInterface) mDeviceInterfaces.get(deviceIdx).get(interfaceIdx);
                    info = Usb.getInterfaceInfo(usbInterface);

                    Intent intent = new Intent(MainActivity.this, ItemDetailActivity.class);
                    intent.putExtra(ItemDetailFragment.ARG_VID, device.getVendorId());
                    intent.putExtra(ItemDetailFragment.ARG_PID, device.getProductId());
                    intent.putExtra(ItemDetailFragment.ARG_FID, usbInterface.getId());
                    startActivity(intent);
                }
                textView.setText(info);

                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                textView.setText("onItemClick:" + position + ", " + id);
            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);

        super.onDestroy();
    }

    class MyExpandableListAdapter extends BaseExpandableListAdapter {
        @Override
        public Object getChild(int arg0, int arg1) {
            return mDeviceInterfaces.get(arg0).get(arg1);
        }

        @Override
        public long getChildId(int arg0, int arg1) {
            return arg0 * 0x100 + arg1;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public View getChildView(int parentCount, int childCount, boolean arg2, View arg3, ViewGroup arg4) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View view = inflater.inflate(R.layout.list_child_layout, null);
            LinearLayout ll = (LinearLayout)view.findViewById(R.id.child);
            TextView tv = (TextView)view.findViewById(R.id.childtv);
            tv.setPadding(60, 5, 5, 5);
            ll.setPadding(5, 5, 5, 5);

            List<UsbInterface> children = null;
            if (parentCount < mDeviceInterfaces.size()) {
                children = mDeviceInterfaces.get(parentCount);
            }
            if (parentCount < mDeviceInterfaces.size() && children != null && childCount < children.size()) {
                UsbInterface usbInterface = (UsbInterface) mDeviceInterfaces.get(parentCount).get(childCount);
                StringBuilder sb = new StringBuilder();
                sb.append("--IF:" + usbInterface.getId());
                sb.append("/" + usbInterface.getInterfaceClass());
                sb.append("/EpCnt=" + usbInterface.getEndpointCount());
                tv.setText(sb.toString());
            } else {
                tv.setText("Invalid");
            }

            view.setTag(R.id.view_tag_device, VIEW_TAG_ID_DEVICE + parentCount);
            view.setTag(R.id.view_tag_interface, VIEW_TAG_ID_INTERFACE + childCount);
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mDeviceInterfaces.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mUsbDeviceList.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return mUsbDeviceList.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View view = inflater.inflate(R.layout.list_parent_layout, null);
            TextView tv = (TextView)view.findViewById(R.id.parent);
            tv.setPadding(50, 5, 5, 5);

            if (groupPosition < mUsbDeviceList.size()) {
                UsbDevice device = (UsbDevice) mUsbDeviceList.get(groupPosition);
                StringBuilder sb = new StringBuilder();
                sb.append("/VID=0x" + Integer.toHexString(device.getVendorId()));
                sb.append(",PID=0x" + Integer.toHexString(device.getProductId()));
                sb.append("\n-" + device.getProductName());
                tv.setText(sb.toString());
            } else {
                tv.setText("Invalid");
            }

            view.setTag(R.id.view_tag_device, VIEW_TAG_ID_DEVICE + groupPosition);
            view.setTag(R.id.view_tag_interface, 0);
            return view;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);

//            Intent intent = new Intent(MainActivity.this, ItemDetailActivity.class);
//            intent.putExtra(ItemDetailFragment.ARG_VID, 11);
//            intent.putExtra(ItemDetailFragment.ARG_PID, 12);
//            intent.putExtra(ItemDetailFragment.ARG_FID, 13);
//            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
