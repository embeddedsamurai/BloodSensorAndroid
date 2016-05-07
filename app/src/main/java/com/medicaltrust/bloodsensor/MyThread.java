package com.medicaltrust.bloodsensor;

import android.os.Handler;

public abstract class MyThread extends Thread
{
    protected boolean mIsAlive;
    protected boolean mIsPaused;

    protected Handler mHandler;
    
    abstract public void cancel();
    abstract public void kill();
    abstract public void pause();
    abstract public void restart();

    public MyThread (Handler h)
    {
        mIsAlive = false;
        mIsPaused = false;
        mHandler = h;
    }
  
    // utilities
    protected void respond (int what) {
        mHandler.obtainMessage(what).sendToTarget();
    }
    protected void respond2 (int who, int what, Object obj) {
        mHandler.obtainMessage(who, what, 0, obj).sendToTarget();
    }
}
