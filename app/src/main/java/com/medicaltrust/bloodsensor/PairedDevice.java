package com.medicaltrust.bloodsensor;

import android.os.Parcel;
import android.os.Parcelable;

public class PairedDevice implements Parcelable
{
    String mName;
    String mAddr;

    public PairedDevice (String name, String addr) {
        mName = name;
        mAddr= addr;
    }
    public String getName () { return mName; }
    public String getAddr () { return mAddr; }

    public int describeContents () {
        return 0;
    }
    public void writeToParcel (Parcel out, int flags) {
        out.writeString(mName);
        out.writeString(mAddr);
    }
    public static final Parcelable.Creator<PairedDevice> CREATOR
        = new Parcelable.Creator<PairedDevice>() {
        public PairedDevice createFromParcel(Parcel in) {
            return new PairedDevice (in.readString(), in.readString());
        }
        public PairedDevice[] newArray(int size) {
            return new PairedDevice[size];
        }
    };
}
