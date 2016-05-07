package com.medicaltrust.bloodsensor;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnTouchListener;

import android.widget.Button;
import android.widget.Toast;

public class ModeSelectActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mode_select);

        Button normalBt = (Button)findViewById(R.id.normal);
        Button memoryBt = (Button)findViewById(R.id.memory);

        normalBt.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) { startNormal(); }
        });
        memoryBt.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) { startMemory(); }
        });

    } // OnCreate

    private void startNormal()
    {
        Intent intent = new Intent(this, DevicesActivity.class);
        startActivity(intent);
    }
    private void startMemory()
    {
        Calendar today = new GregorianCalendar();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar tommorow = (Calendar)(today.clone());
        tommorow.add(Calendar.DATE, 1);

        Intent intent = new Intent(this, MemoryActivity.class);
        intent.putExtra(Config.MemoryTimeStartLabel, today.getTimeInMillis());
        intent.putExtra(Config.MemoryTimeEndLabel, tommorow.getTimeInMillis());
        startActivity(intent);
    }
}
