package com.htc.client.vr;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class BleDevInfo implements Parcelable {
    public String mAddr;
    public String mName;

    public String mModuleName;
    public String mFwVersion;
    public String mSerialName;

    public BleDevInfo(String addr, String name, String moduleName, String fwVersion, String serialName) {
        mAddr = addr;
        mName = name;

        mModuleName = moduleName;
        mFwVersion = fwVersion;
        mSerialName = serialName;
    }

    private BleDevInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAddr);
        dest.writeString(mName);

        dest.writeString(mModuleName);
        dest.writeString(mFwVersion);
        dest.writeString(mSerialName);
    }

    private void readFromParcel(Parcel in) {
        mAddr = in.readString();
        mName = in.readString();

        mModuleName = in.readString();
        mFwVersion = in.readString();
        mSerialName = in.readString();
    }

    public static final Creator<BleDevInfo> CREATOR = new Creator<BleDevInfo>() {
        @Override
        public BleDevInfo createFromParcel(Parcel source) {
            return new BleDevInfo(source);
        }

        @Override
        public BleDevInfo[] newArray(int size) {
            return new BleDevInfo[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(mAddr, mName, mModuleName, mFwVersion, mSerialName);
    }
}
