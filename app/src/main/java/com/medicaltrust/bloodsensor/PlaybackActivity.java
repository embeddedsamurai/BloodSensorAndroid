package com.medicaltrust.bloodsensor;

import java.util.Date;

import android.content.Intent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.graphics.drawable.Drawable;

import android.os.Bundle;
import android.os.Message;

import android.view.View;
import android.widget.TextView;

import com.medicaltrust.bloodsensor.database.DatabaseHelper;
import com.medicaltrust.bloodsensor.eater.McbyEaterThread;
import com.medicaltrust.bloodsensor.player.McbyPlayerThread;
import com.medicaltrust.bloodsensor.renderer.MdSensorGraphRenderer;

public class PlaybackActivity extends MdSensorActivity
{
    DatabaseHelper mDbh;
    SQLiteDatabase mDb;

    TextView mHeartRateView;
    TextView mSPO2View;

    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        MyPreference pref = new MyPreference(this);
        if (!pref.isOK()) {
            android.util.Log.d("PlaybackActivity",
                               "preference is not correct");
            finish();
        }

        Intent intent = getIntent();
        long measid = intent.getLongExtra(Config.MeasurementIdLabel, -1L);
        long date = intent.getLongExtra(Config.StoredDateLabel, -1L);
        if (measid == -1L || date == -1L) {
            android.util.Log.e("PlaybackActivity.onCreate",
                               "Invalid arguments.");
            return;
        }

        final PlaybackHandler h = new PlaybackHandler();
        final MdSensorGraphRenderer renderer =
            new MdSensorGraphRenderer(this, h);

        mDbh = new DatabaseHelper(this);
        mDb = mDbh.getReadableDatabase();

        super.onCreate(savedInstanceState, renderer, h, pref);

        Cursor c = 
            mDb.rawQuery(Config.DevicesQuery,
                         new String[] {Long.toString(measid)});
        if (c.getCount() != 0)
            for (c.moveToFirst();; c.moveToNext()) {
                final McbyReceivedData mrd =
                    new McbyReceivedData(Config.Mcby.PlethSamples);
                renderer.addGraph(mrd);

                // Role adding data
                McbyPlayerThread mpt =
                    new McbyPlayerThread(mDb, mrd, h, measid, c.getLong(0));
                // Role proceeding data
                McbyEaterThread met =
                    new McbyEaterThread(h, mrd, Config.Mcby.MsecPerFrame,
                                        Config.Mcby.PlethSamples);
                
                addThread(mpt);
                addThread(met);

                if (c.isLast()) break;
            }

        startAll();

        TextView title = (TextView)findViewById(R.id.panel_title);
        String dateStr = Config.FormatDate.format(new Date(date));
        title.setText(dateStr);
    }

    @Override
    public void onDestroy ()
    {
        mDb.close();
        mDbh.close();
        super.onDestroy();
    }

    public class PlaybackHandler extends MdSensorHandler
    {
        // iLisePlayerThread
        public static final int StartSelecting = 300;
        public static final int EndSelecting = 301;
        public static final int StartPlaying = 302;
        public static final int EndPlaying = 303;
        public static final int FinishedAllTask = 304;

        public static final int NoData = -300;

        @Override
        public void handleMessage (Message m) {
            switch (m.what) {
            case StartSelecting:
                showMessage("Quering to database."); break;
            case EndSelecting:
                showMessage("Got data from database."); break;
            case StartPlaying:
                showMessage("Start playing back."); break;
            case EndPlaying:
                showMessage("Finished playing back."); break;
            case FinishedAllTask:
                showMessage("Finished task."); cleanup(); break;
            case NoData:
                showMessage("Error: There is no data."); finish(); break;
            }
            super.handleMessage(m);
        }
    }
}
