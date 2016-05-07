package com.medicaltrust.bloodsensor;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

public abstract class BluetoothActivity extends Activity
{
    List<MyThread> mThreads;
    BluetoothHandler mHandler;

    boolean mCleaned;
    TextView mMessageView;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        this.onCreate(savedInstanceState, new BluetoothHandler());
    }
    public void onCreate (Bundle savedInstanceState, BluetoothHandler handler)
    {
        super.onCreate(savedInstanceState);

        mThreads = new ArrayList<MyThread>();
        mHandler = handler;
        mCleaned = false;
    }
  
    protected void setMessageView (TextView tv) {
        mMessageView = tv;
    }

    protected void addThread (MyThread thread) {
        mThreads.add(thread);
    }
    protected void startAll () {
        for (MyThread th: mThreads) th.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancel();
    }

    protected void cleanup ()
    {
        for (MyThread thr: mThreads) thr.kill();
        for (MyThread thr: mThreads)
            try {
                thr.join();
            } catch (InterruptedException e) { }

        mCleaned = true;
    }

    // end immediately
    protected void cancel ()
    {
        if (!mCleaned) cleanup();
    }
    protected void pause ()
    {
        for (MyThread thr: mThreads) thr.pause();
    }
    protected void restart ()
    {
        for (MyThread thr: mThreads) thr.restart();
    }

    protected void showMessage (String mes)
    {
        Log.i("BluetoothActivity.showMessage", mes);
        if (mMessageView != null)
            mMessageView.setText(mes);
    }

    // GUI controls triggered by threads are available only in this method.
    protected class BluetoothHandler extends Handler
    {
        public static final int Connected = 10;
        public static final int GotStream = 11;

        public static final int SocketIsNull = -10;
        public static final int UnableToConnect = -11;
        public static final int UnableToGetStream = -12;
        public static final int UnableToSend = -13;
        public static final int UnableToReceive = -14;

        public void handleMessage (Message m) {
            switch (m.what) {
            case Connected:
                showMessage("Connected."); break;
            case GotStream:
                showMessage("Got stream."); break;
        
            case SocketIsNull:
                showMessage("Error: Socket is null.");
                finish(); break;
            case UnableToConnect:
                showMessage("Error: Unable to connect.");
                finish(); break;
            case UnableToGetStream:
                showMessage("Error: Unable to get stream.");
                finish(); break;
            case UnableToSend:
                showMessage("Error: Unable to send data.");
                finish(); break;
        
            default:
                //        Log.e(TAG, "message unhandled."); break;
            }
        }
    }
}
