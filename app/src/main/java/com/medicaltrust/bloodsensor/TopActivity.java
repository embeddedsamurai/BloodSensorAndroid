package com.medicaltrust.bloodsensor;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;

import android.view.View;
import android.view.View.OnTouchListener;

import android.widget.Button;
import android.widget.Toast;

import com.medicaltrust.bloodsensor.database.DatabasePreparer;

public class TopActivity extends Activity
{
    DatabasePreparer mDatabasePreparer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mDatabasePreparer =
            new DatabasePreparer(this, Config.DatabaseResources);

        setContentView(R.layout.top);

        Button startBt = (Button)findViewById(R.id.start);
        Button settingBt = (Button)findViewById(R.id.setting);

        startBt.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) { startMain(); }
        });
        settingBt.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) { startSetting(); }
        });

        MyPreference pref = new MyPreference(this);
        if (!pref.isOK()) startSetting();
    }

    @Override
    public void onDestroy()
    {
        mDatabasePreparer.finish();

        super.onDestroy();
    }
  
    private void startMain()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    private void startSetting()
    {
        Intent intent = new Intent(this, MyPreferenceActivity.class);
        startActivity(intent);
    }
}
