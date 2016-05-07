package com.medicaltrust.bloodsensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.Intent;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import android.graphics.drawable.Drawable;

import android.location.LocationManager;

import android.os.Bundle;
import android.os.Message;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.medicaltrust.bloodsensor.database.DatabaseHelper;
import com.medicaltrust.bloodsensor.database.Measurements;

import com.medicaltrust.bloodsensor.eater.McbyEaterThread;
import com.medicaltrust.bloodsensor.receiver.McbyReceiverThread;
import com.medicaltrust.bloodsensor.receiver.StoreThread;

import com.medicaltrust.bloodsensor.renderer.MdSensorGraphRenderer;

public class MeasurementActivity extends MdSensorActivity
{
    DatabaseHelper mDbh;
    SQLiteDatabase mDb;
    long mMeasId;
    List<DateReminder> mDates;

    boolean mSuccess;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        MyPreference pref = new MyPreference(this);
        if (!pref.isOK()) {
            android.util.Log.d("MeasurementActivity",
                               "preference is not correct");
            finish();
        }

        Intent intent = getIntent();

        List<PairedDevice> devices =
            intent.getParcelableArrayListExtra(Config.DevicesNameLabel);
        
        final McbyHandler h = new McbyHandler();
        final MdSensorGraphRenderer renderer =
            new MdSensorGraphRenderer(this, h);

        super.onCreate(savedInstanceState, renderer, h, pref);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.e("MoekoActivity.onCreate", "bluetooth adapter not available.");
            return;
        }

        mDbh = new DatabaseHelper((Context)this);
        mDb = mDbh.getWritableDatabase();

        // mDb.beginTransactionNonExclusive(); // from API level 11
        // mDb.beginTransaction();

        mMeasId = Measurements.insert(mDb);
        if (mMeasId == -1) {
            android.util.Log.e(
                "MeasurementActivity.OnCreate",
                "failed to insert new measurement into database.");
            return;
        }

        final StoreThread store = new StoreThread(mDb, mMeasId, mHandler);
        addThread(store);

        mDates = new ArrayList<DateReminder>();

        for (PairedDevice pd: devices) {
            final McbyReceivedData mrd =
                new McbyReceivedData(Config.Mcby.PlethSamples);
            renderer.addGraph(mrd);

            // Device ID
            long devId = Measurements.insertDevice(mDb, mMeasId, pd.getName());

            // Role adding data
            McbyReceiverThread mrt =
                new McbyReceiverThread(adapter.getRemoteDevice(pd.getAddr()),
                                       mrd, store, h, devId);

            // Role proceeding data
            McbyEaterThread met = 
                new McbyEaterThread(mHandler, mrd, Config.Mcby.MsecPerFrame,
                                    Config.Mcby.PlethSamples);

            addThread(mrt);
            addThread(met);

            mDates.add(new DateReminder(devId));
        }

        mSuccess = true;
        startAll();

        /*
        ((TextView)findViewById(R.id.panel_title)).setText(Mcby1DevName);
        */
    }

    @Override
    public void onDestroy ()
    {
        super.onDestroy();
        
        if (mSuccess)
            for (DateReminder dr: mDates)
                dr.updateDatabase(mDb, mMeasId);
            // mDb.setTransactionSuccessful();
        else
            // database transaction would rollback implicitly.
            Measurements.delete(mDb, mMeasId); 
        // mDb.endTransaction();

        mDb.close();
        mDbh.close();
    }

    private void updateDate (long devid, long time)
    {
        for (DateReminder dr: mDates)
            if (devid == dr.getId()) dr.setDate(time);
    }

    public class MeasurementHandler extends MdSensorHandler
    {
        public static final int ChangedFormat = 202;
        public static final int StartMeasuring = 203;
        public static final int EndMeasuring = 204;
        public static final int FinishedAllTask = 205;

        public static final int UnableToChangeFormat = -202;

        @Override
        public void handleMessage (Message m) {
            switch (m.what) {
            case ChangedFormat:
                showMessage("changed format."); break;
            case StartMeasuring:
                showMessage("Start measuring."); break;
            case EndMeasuring:
                showMessage("Finished measuring."); break;
            case FinishedAllTask:
                showMessage("Finished task.");
                cleanup(); break;

            case UnableToChangeFormat:
                showMessage("unable to change format.");
                finish(); break;
            }

            switch (m.what) {
            case SocketIsNull:
            case UnableToConnect:
            case UnableToGetStream:
            case UnableToSend:
            case UnableToReceive:
            case UnableToChangeFormat:
                mSuccess = false;
                setResult(RESULT_CANCELED);
                break;
            default:
                setResult(RESULT_OK);
            }
            super.handleMessage(m);
        }
    } // MeasurementHandler

    public class McbyHandler extends MeasurementHandler
    {
        public static final int ChangedFormat = 300;
        public static final int UnableToChangeFormat = -300;
        public static final int UpdateDate = 301;

        public class DeviceDate {
            public long devid;
            public long date;
            public DeviceDate (long di, long dt) {
                date = di; date = dt;
            }
        }

        @Override
        public void handleMessage (Message m) {
            switch (m.what) {
            case UpdateDate:
                DeviceDate dt = (DeviceDate)m.obj;
                updateDate(dt.devid, dt.date); break;
            }
            super.handleMessage(m);
        }
    }

    class DateReminder
    {
        long mId;
        long mDate;
        public DateReminder(long id) {
            mId = id;
            mDate = 0L;
        }
        public void setDate(long date) {
            mDate = date;
        }
        public long getId() {
            return mId;
        }
        public boolean updateDatabase(SQLiteDatabase db, long measId) {
            if (mDate == 0L) return false;

            SQLiteStatement st =
                mDb.compileStatement(Config.UpdateDateStatement);
            st.bindLong(1, mDate);
            st.bindLong(2, measId);
            st.bindLong(3, mId);
            st.execute();

            return true;
        }
    } // DateReminder
}