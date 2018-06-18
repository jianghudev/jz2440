package com.htc.miac.controllerutility.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.util.Log
import com.finchtechnologies.fota.IFotaListener
import com.finchtechnologies.fota.IFotaService
import java.util.concurrent.Executors

/**
 * Created by hubin_jiang on 2018/6/18.
 */
public class AspenServiceModel(val mContext: Context) {
    interface Delegate {
        fun onDeviceStatusChanged(state: Int, bundle: Bundle)
        fun onFirmwareUpdateProgressChanged(progress: Int)
        fun onDeviceInfoGet(info: Bundle?)
        fun onFotaError()
        fun onServiceConnected()
    }

    private val TAG = "AspenFota.client"

    val SERVICE_PACKAGE = "com.finchtechnologies.fota"
    private val SERVICE_CLASS = SERVICE_PACKAGE + ".FotaService"

    private var mIFinchFotaService: IFotaService? = null
    private val mUIHandler = Handler(Looper.getMainLooper())
    private var mDelegate: Delegate? = null
    private var mMacAddress = ""
    private var isBinded = false

    //private var mContext: Context;

//    fun AspenServiceModel(context: Context): ??? {
//        mContext = context
//    }

    fun bindService(): Boolean {
        Log.i(TAG, "bindService")
        var isBind = false
        try {
            val intent = Intent()
            intent.setClassName(SERVICE_PACKAGE, SERVICE_CLASS)
            isBind = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return isBind
    }

    fun unbindService() {
        Log.d(TAG, "unbindService")
        mMacAddress = ""
        mContext.unbindService(mConnection)
        isBinded = false
    }

    fun setDelegate(delegate: Delegate) {
        mDelegate = delegate
    }

    fun getDelegate(): Delegate? {
        return mDelegate
    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected")
            isBinded = true
            mIFinchFotaService = IFotaService.Stub.asInterface(service)
            try {
                mIFinchFotaService!!.setDeviceListener(mFinchFotaListener)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            val delegate = getDelegate()
            delegate?.onServiceConnected() ?: Log.w(TAG, "[onServiceConnected] delegate is null !")
        }

        override fun onServiceDisconnected(name: ComponentName) {

        }
    }

    private val mFinchFotaListener = object : IFotaListener.Stub() {

        @Throws(RemoteException::class)
        override fun onDeviceStatusChanged(state: Int, extra: Bundle) {
            Log.d(TAG, "state : " + state)
            mUIHandler.post {
                val delegate = getDelegate()
                delegate?.onDeviceStatusChanged(state, extra)
            }
        }

        @Throws(RemoteException::class)
        override fun onFirmwareUpdateProgressChanged(progress: Int) {
            mUIHandler.post {
                val delegate = getDelegate()
                delegate?.onFirmwareUpdateProgressChanged(progress)
            }
        }
    }

    fun setMacAddress(addr: String) {
         object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg voids: Void): Void? {
                try {
                    mIFinchFotaService!!.setMacAddress(addr)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on setMacAddress", e)
                }

                return null
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1))
    }

    fun getDeviceInfo() {
        object : AsyncTask<Void, Void, Bundle>() {

            override fun doInBackground(vararg voids: Void): Bundle? {
                try {
                    return mIFinchFotaService!!.deviceInfo
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on getDeviceInfo", e)
                }

                return null
            }

            override fun onPostExecute(info: Bundle?) {
                val delegate = getDelegate()
                if (delegate != null) {
                    if (info != null) {
                        delegate.onDeviceInfoGet(info)
                    } else {
                        delegate.onFotaError()
                    }
                } else {
                    Log.e(TAG, "delegate is null")
                }
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1))
    }

    fun getBatteryVoltageLevel(): Int {
        var batteryLevel = -1
        try {
            batteryLevel = mIFinchFotaService!!.batteryVoltageLevel
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return batteryLevel
    }

    fun upgradeFirmware(uri: Uri, isItFirstAttempt: Boolean) {
        object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg voids: Void): Boolean? {
                try {
                    return if (isItFirstAttempt) {
                        mIFinchFotaService!!.upgradeFirmware(uri)
                    } else {
                        mIFinchFotaService!!.upgradeFirmwareOnDfuTarg(uri)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on upgradeFirmware", e)
                }

                return false
            }

            override fun onPostExecute(result: Boolean) {
                if ( !result ) {
                    val delegate = getDelegate()
                    delegate?.onFotaError()
                }
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1))
    }

    fun isBinded(): Boolean {
        return isBinded
    }

    companion object{
        fun get_static_name(): String {
            return "static test"
        }
    }
}