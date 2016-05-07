package com.medicaltrust.bloodsensor.player;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;

import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.PlaybackActivity.PlaybackHandler;
import com.medicaltrust.bloodsensor.McbyFrames;
import com.medicaltrust.bloodsensor.McbyReceivedData;

public class McbyPlayerThread extends PlayerThread
{
    McbyReceivedData mData;
    McbyFrames mFrames;
    int mLastTime;

    public McbyPlayerThread (SQLiteDatabase db, McbyReceivedData rd,
                             PlaybackHandler h,
                             long measid, long devid)
    {
        super(h, db.rawQuery(Config.McbyDataQuery,
                             new String[] { Long.toString(measid),
                                            Long.toString(devid) }));
        mData = rd;
        mFrames = new McbyFrames(1);
        mLastTime = 0;
    }

    /* Player Thread */
    protected boolean hello (Cursor c)
    {
        mLastTime = c.getInt(0);
        return true;
    }

    protected boolean talk (Cursor c)
    {
        int pleth = c.getInt(1);
        int spo = c.getInt(2);
        int hr = c.getInt(3);

        mFrames.item(0).setPleth(pleth);
        mFrames.set(hr, spo);

        /*
          android.util.Log.d("McbyPlayerThread",
                             "hr="+hr+" spo="+spo+" pleth="+pleth);
        */
    
        mData.set(mFrames);

        mCursor.moveToNext();

        int t = mCursor.getInt(0);
        mNextSend += t - mLastTime;
        mLastTime = t;

        return true;
    }

    protected void bye ()
    {
    }
}