package com.medicaltrust.bloodsensor;

import java.util.Date;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.medicaltrust.bloodsensor.receiver.McbyReceiverThread;
import com.medicaltrust.bloodsensor.receiver.StoreThread;
import com.medicaltrust.bloodsensor.renderer.MdSensorGraphRenderer;

public class MdSensorActivity extends BluetoothActivity
{
    MdSensorGraphRenderer mRenderer;

    protected MdSensorHandler mHandler;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, new MdSensorHandler());
        throw new RuntimeException("Use onCreate(Bundle, MdSensorReceivedData, MdSensorHandler, MyPreference)");
    }
    public void onCreate(Bundle savedInstanceState,
                         MdSensorGraphRenderer renderer,
                         final MdSensorHandler h,
                         MyPreference pref)
    {
        mHandler = h;

        super.onCreate(savedInstanceState, h);

        mRenderer = renderer;

        setContentView(R.layout.measurement);

        BluetoothGraphSurfaceView glView =
            (BluetoothGraphSurfaceView)findViewById(R.id.gl);
        glView.setRenderer(renderer);

        setMessageView((TextView)findViewById(R.id.panel_message));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mRenderer != null) mRenderer.finish();
    }

    @Override
    protected void cleanup ()
    {
        super.cleanup();
    }

    public class MdSensorHandler extends BluetoothHandler
    {
        @Override
        public void handleMessage (Message m) {
            super.handleMessage(m);
        }
    }
}
