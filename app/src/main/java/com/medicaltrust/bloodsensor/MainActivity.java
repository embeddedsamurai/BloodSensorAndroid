package com.medicaltrust.bloodsensor;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Intent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.GridView;

import com.medicaltrust.bloodsensor.database.DatabaseHelper;


public class MainActivity extends Activity
{
    DatabaseHelper mDbh;
    SQLiteDatabase mDb;
    boolean[] mIsDayAvailable; // updated only in setAvailableDays()

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        CalendarView cv = (CalendarView)findViewById(R.id.calendar);
        cv.setOnDayClickListener(new CalendarView.OnDayClickListener() {
            @Override
            public void onDayClick(View v, int year, int month, int day)
            {
                openDay(year, month, day);
            }
        });
        cv.setOnPageChangedListener(new CalendarView.OnPageChangedListener() {
            @Override
            public boolean onPageChanged(View v, int year, int month)
            {
                setAvailableDays(year, month);
                boolean[] meas = mIsDayAvailable;

                // カレンダーに色塗り
                GridView gv = (GridView)v.findViewById(R.id.calendar_days);
                if (gv == null) {
                    android.util.Log.d("MainActivity", "Cannot findGridView");
                    return false;
                }

                int today = (new GregorianCalendar()).get(Calendar.DATE);

                Calendar cal = new GregorianCalendar(year, month-1, 1, 0, 0, 0);
                for (int i = 0; i < meas.length; i++) {
                    if (!meas[i] || i+1 == today) continue;
                    int offset = gv.getFirstVisiblePosition();
                    int index = cal.get(Calendar.DAY_OF_WEEK) + i + 7 - 1;
                    View dv = gv.getChildAt(offset+index);
                    dv.setBackgroundColor(0xff6060b0);
                }
                return true;
            }
        });

        mDbh = new DatabaseHelper(this);
        mDb = mDbh.getReadableDatabase();
        mIsDayAvailable = null;
    }

    @Override
    public void onDestroy()
    {
        mDb.close();
        mDbh.close();

        super.onDestroy();
    }

    private void openDay(int year, int month, int day)
    {
        Calendar theday = new GregorianCalendar(year, month-1, day, 0, 0, 0);

        if (isToday(theday))
            startMeasurement(theday);
        else if (mIsDayAvailable[day-1])
            startPlayback(theday);
    }

    private void setAvailableDays(int year, int month)
    {
        // 月の情報
        Calendar c = new GregorianCalendar(year, month-1, 1, 0, 0, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        long end = c.getTimeInMillis();
        c.add(Calendar.DATE, -1);
        int mend = c.get(Calendar.DATE);

        // データのある日を調べる
        boolean[] meas = new boolean[mend];
        for (int i = 0; i < meas.length; i++) meas[i] = false;

        /*
          final String q = 
          String.format("SELECT %s,%s FROM %s WHERE %s ORDER BY date DESC",
                        "id", "date", Config.MeasurementsTableName, "date");

          Cursor cur = mDb.rawQuery(q, null);
        */

        final String q = 
            String.format("SELECT %s,%s FROM %s"+
                          " WHERE %s BETWEEN ? AND ?"+
                          " ORDER BY date DESC",
                          "id", "date", Config.MeasurementsTableName, "date");

        Cursor cur =
            mDb.rawQuery(q, new String[] { Long.toString(start),
                                           Long.toString(end) });

        if (cur.getCount() != 0)
            for (cur.moveToFirst();; cur.moveToNext()) {
                int id = cur.getInt(0);
                long date = cur.getLong(1);

                // 閏秒とか気にしない
                int day = (int)((date - start) / (86400L * 1000L));
                if (0 <= date - start && day < meas.length)
                    meas[day] = true;
                
                if (cur.isLast()) break;
            }

        mIsDayAvailable = meas;
    }

    private void startMeasurement(Calendar date)
    {
        Intent intent = new Intent(this, ModeSelectActivity.class);
        startActivity(intent);
    }
    private void startPlayback(Calendar date)
    {
        Intent intent = new Intent(this, MemoryActivity.class);
        intent.putExtra(Config.MemoryTimeStartLabel, getTime(date));
        intent.putExtra(Config.MemoryTimeEndLabel, getTime(nextDay(date)));
        startActivity(intent);
    }

    // Calendar Utilities
    private static boolean isSameDay (Calendar a, Calendar b) {
        return getYear(a) == getYear(b) &&
               getMonth(a) == getMonth(b) &&
               getDay(a) == getDay(b);
    }
    private static boolean isToday (Calendar c) {
        return isSameDay(new GregorianCalendar(), c);
    }
    private static int getYear  (Calendar c) { return c.get(Calendar.YEAR); }
    private static int getMonth (Calendar c) { return c.get(Calendar.MONTH)+1; }
    private static int getDay   (Calendar c) { return c.get(Calendar.DATE); }
    private static long getTime (Calendar c) { return c.getTimeInMillis(); }
    private static Calendar nextDay (Calendar c) {
        Calendar c2 = (Calendar)(c.clone());
        c2.add(Calendar.DATE, 1);
        return c2;
    }
}
