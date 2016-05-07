package com.medicaltrust.bloodsensor.eater;

import android.os.Handler;

import com.medicaltrust.bloodsensor.McbyReceivedData;
import com.medicaltrust.bloodsensor.MyThread;

public class McbyEaterThread extends MyThread
{
    McbyReceivedData mData;
    long mInterval;
    long mNextUpdate;
    int mItems;

    public McbyEaterThread(Handler h, McbyReceivedData data, long interval,
                           int n)
    {
        super(h);
        mData = data;
        mNextUpdate = System.currentTimeMillis();
        mInterval = interval;
        mItems = n;
    }

    public void run()
    {
        mIsAlive = true;

        while (mIsAlive) {
            long now = System.currentTimeMillis();
            if (now < mNextUpdate)
                try {
                    Thread.sleep(mNextUpdate - now);
                } catch (InterruptedException e) { }
            else {
                eat();
                mNextUpdate += mInterval;
            }
        }
        cancel();
    }

    public void cancel() { }
    public void kill() {
        mIsAlive = false;
    }
    public void pause() { }
    public void restart() { }

    private void eat()
    {
        mData.nextScene(mItems);
    }
}