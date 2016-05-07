package com.medicaltrust.bloodsensor;

import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;

public class MyPreference
{
    boolean mIsOK;

    public MyPreference(Context context)
    {
        SharedPreferences pref =
            PreferenceManager.getDefaultSharedPreferences(context);

        try {
            mIsOK = true;

        } catch (NumberFormatException e) {
            mIsOK = false;
        }
    }

    public boolean isOK() { return mIsOK; }
}