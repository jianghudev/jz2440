package com.htc.client.vr;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Created by hugh_chen on 2017/10/24.
 */

public class BleDev implements Parcelable {
    public String mAddr;
    public String mName;

    public BleDev(String addr, String name) {
        mAddr = addr;
        mName = name;
    }

    private BleDev(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mAddr);
        parcel.writeString(mName);
    }

    private void readFromParcel(Parcel in) {
        mAddr = in.readString();
        mName = in.readString();
    }

    public static final Parcelable.Creator<BleDev> CREATOR = new Creator<BleDev>() {
        @Override
        public BleDev createFromParcel(Parcel source) {
            return new BleDev(source);
        }

        @Override
        public BleDev[] newArray(int size) {
            return new BleDev[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(mAddr, mName);
    }

    @Override
    public String toString() {
        return mName + " " + mAddr;
    }
}

