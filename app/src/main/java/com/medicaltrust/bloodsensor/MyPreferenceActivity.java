package com.medicaltrust.bloodsensor;

import java.util.List;

import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;

import android.os.Bundle;
import android.os.Handler;

import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import android.widget.Toast;

import com.medicaltrust.bloodsensor.database.DatabaseHelper;
import com.medicaltrust.bloodsensor.database.DatabasePreparer;
import com.medicaltrust.bloodsensor.database.DatabaseSynchronizer;

public class MyPreferenceActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        final Handler h = new Handler();

        eventSynchronize(h, (PreferenceScreen)findPreference("synchronize"));
        eventRefresh(h, (PreferenceScreen)findPreference("refresh"));
    }

    private void eventSynchronize(final Handler h, PreferenceScreen ps)
    {
        ps.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener() 
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    final ProgressDialog pd = 
                        new ProgressDialog(MyPreferenceActivity.this);
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setMessage("同期中, ちょっと待ってね。");
                    pd.setCancelable(false);
                    h.post(new Runnable() {public void run() {
                        pd.show();
                    }});

                    final DatabaseHelper dbh =
                        new DatabaseHelper(MyPreferenceActivity.this);
                    final SQLiteDatabase db = dbh.getReadableDatabase();

                    DatabaseSynchronizer dbs =
                        new DatabaseSynchronizer(MyPreferenceActivity.this, db);
                    dbs.setOnSynchronizedListener(
                        new DatabaseSynchronizer.OnSynchronizedListener()
                        {
                            @Override
                            public void onItemSynchronized(int measid) {}
                            @Override
                            public void
                            onAllItemsSynchronized(List<Integer> measurements) {
                                end(pd, dbh, db);
                            }
                            @Override
                            public void onFailedToSynchronize() {
                                end(pd, dbh, db);
                            }
                            private void end(final ProgressDialog pd,
                                             DatabaseHelper dbh,
                                             SQLiteDatabase db)
                            {
                                h.post(new Runnable() {public void run() {
                                    pd.dismiss();
                                }});
                                db.close();
                                dbh.close();
                            }
                        });
                    dbs.start();
                    return true;
                }
            });
    }

    private void eventRefresh(final Handler h, PreferenceScreen ps)
    {
        ps.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener() 
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    android.util.Log.d("MyPreferenceActivity", "click");
                    final ProgressDialog pd = 
                        new ProgressDialog(MyPreferenceActivity.this);
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setMessage("更新中, ちょっと待ってね。");
                    pd.setCancelable(false);
                    h.post(new Runnable() {public void run() {
                        android.util.Log.d("MyPreferenceActivity", "start");
                        pd.show();
                    }});

                    final DatabasePreparer dbp =
                        new DatabasePreparer(MyPreferenceActivity.this,
                                             Config.DatabaseResources, true);
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) { }

                    dbp.finish();

                    h.post(new Runnable() {public void run() {
                        android.util.Log.d("MyPreferenceActivity", "dismiss");
                        pd.dismiss();
                    }});

                    return true;
                }
            });
    }

}