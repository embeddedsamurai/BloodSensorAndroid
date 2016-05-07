package com.medicaltrust.bloodsensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.Bundle;
import android.os.Handler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.medicaltrust.bloodsensor.database.DatabaseHelper;
import com.medicaltrust.bloodsensor.database.DatabasePreparer;
import com.medicaltrust.bloodsensor.database.Measurements;

public class MemoryActivity extends Activity
{
    StoredListAdapter mStoredAdapter;

    Handler mHandler;
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memory);

        mHandler = new Handler();
        mContext = this;

        Intent intent = getIntent();
        long timeStart = intent.getLongExtra(Config.MemoryTimeStartLabel, -1L);
        long timeEnd = intent.getLongExtra(Config.MemoryTimeEndLabel, -1L);

        if (timeStart < 0 || timeEnd < 0) {
            android.util.Log.e("MemoryActivity.onCreate", "Arguments error");
            return;
        }

        // touch and start thread
        preparePlaybackScreen(getStoredData(timeStart, timeEnd));

    } // OnCreate
  
    @Override
    public void onDestroy ()
    {
        super.onDestroy();
    }

    /*
    @Override
    protected void onActivityResult (int requestCode, int resultCode,
                                     Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        }
    }
    */

    private void preparePlaybackScreen (List<StoredItem> items)
    {
        // touch and create thread
        ListView lv = (ListView)findViewById(R.id.logs);

        // start flashback mode
        lv.setOnItemClickListener (new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> a, View v,
                                     int position, long id) {
                startPlayback(a, v, position, id);
            }
        });
        // control database
        lv.setOnItemLongClickListener (new AdapterView
                                           .OnItemLongClickListener() {
            public boolean onItemLongClick (final AdapterView<?> a,
                                            final View v,
                                            final int position, final long id)
            {
                final DialogInterface.OnClickListener listener =
                    new DialogInterface.OnClickListener () {
                        @Override
                        public void onClick (DialogInterface dialog, int w) {
                            switch (w) {
                            case Config.PlayerMenuPlay:
                                startPlayback(a, v, position, id); break;
                            case Config.PlayerMenuDelete:
                                deleteStoredItem(a, v, position, id); break;
                            case Config.PlayerMenuCancel:
                                break;
                            }
                        }
                    };
                new AlertDialog.Builder(MemoryActivity.this)
                    .setTitle(Config.PlayerMenuTitle)
                    .setItems(Config.PlayerMenu, listener)
                    .create()
                    .show();
                return true;
            }
        });

        mStoredAdapter =
            new StoredListAdapter(mContext, R.layout.stored_list, items);
        lv.setAdapter(mStoredAdapter);
    }

    private void startPlayback (AdapterView<?> a, View v,
                                int position, long id)
    {
        final StoredItem i = (StoredItem)a.getItemAtPosition(position);
    
        long measid = i.getMeasId();

        Intent intent =
            new Intent(mContext, PlaybackActivity.class);

        intent.putExtra(Config.MeasurementIdLabel, measid);
        intent.putExtra(Config.StoredDateLabel, i.getDate());
        startActivity(intent);
    }

    private void deleteStoredItem (AdapterView<?> a, View v,
                                   int position, long id)
    {
        // DB操作終わるまで操作できないようにすればいいと思うよ
        final StoredItem i = (StoredItem)a.getItemAtPosition(position);
    
        mStoredAdapter.remove(i);
    
        // set value
        DatabaseHelper dbh = new DatabaseHelper(this);
        SQLiteDatabase db = dbh.getWritableDatabase();
    
        Measurements.delete(db, i.getMeasId());
    
        db.close();
        dbh.close();
    }

    private List<StoredItem> getStoredData (long start, long end)
    {
        DatabaseHelper dbh = new DatabaseHelper(this);
        SQLiteDatabase db = dbh.getReadableDatabase();

        Cursor c =
            db.rawQuery(Config.PeriodMeasurementQuery,
                        new String[] {Long.toString(start),
                                      Long.toString(end)});

        List<StoredItem> items = new ArrayList<StoredItem>();
    
        if (c.getCount() != 0)
            for (c.moveToFirst();; c.moveToNext()) {
                items.add(new StoredItem(c.getLong(0), c.getLong(1)));
                if (c.isLast()) break;
            }
        /*
        for (StoredItem i: items) {
            android.util.Log.d("MemoryActivity.getStoredData",
                               ""+i.getMeasId()+" "+i.getDate());
        }
        android.util.Log.d("MemoryActivity.getStoredData",
                           "start:"+start+", end:"+end);
        */

        db.close();
        dbh.close();

        return items;
    }


    /** Adapters */
    class StoredItem
    {
        long mMeasId;
        long mDate;

        public StoredItem (long measid, long date)
        {
            mMeasId = measid;
            mDate = date;
        }
        public long getMeasId () { return mMeasId; }
        public long getDate () { return mDate; }
    }

    class StoredListAdapter extends ArrayAdapter<StoredItem>
    {
        class ViewHolder
        {
            TextView mMeasid;
            TextView mDate;

            public ViewHolder (View v) {
                mMeasid = (TextView)v.findViewById(R.id.stored_measid);
                mDate = (TextView)v.findViewById(R.id.stored_date);
            }
            public void setView (StoredItem i) {
                mMeasid.setText(""+i.getMeasId());
                mDate.setText(Config.FormatDate.format(new Date(i.getDate())));
            }
        }

        int mLayout;
    
        public StoredListAdapter (Context context, int layout,
                                  List<StoredItem> items)
        {
            super(context, layout, items);
            mLayout = layout;
        }

        @Override
        public View getView (int position, View contentView,
                             ViewGroup viewGroup)
        {
            View view;
            ViewHolder holder; // reference of view's member

            if (contentView == null) {
                // create view and define holder as tag
                LayoutInflater inflater =
                    (LayoutInflater)
                    getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
                view = inflater.inflate(mLayout, null);
                holder = new ViewHolder(view);
                view.setTag(holder);
            } else {
                // view has been already constructed
                view = contentView;
                holder = (ViewHolder)view.getTag();
            }

            holder.setView(getItem(position));

            return view;
        }
    } // StoredListAdapter


    // user interface
    private void alert (String title, String mes, String button)
    {
        android.util.Log.d("MainActivity.alert", title+" "+mes);
        final AlertDialog a = new AlertDialog.Builder(this).create();
        a.setTitle(title);
        a.setMessage(mes);
        a.setButton(button, new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int which) {}
            });
        mHandler.post(new Runnable() {
                public void run () { a.show(); }
            });
    }

} // MemoryActivity
