package com.medicaltrust.bloodsensor.database;

import android.content.Context;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

import com.medicaltrust.bloodsensor.Config;

public class DatabaseHelper extends SQLiteOpenHelper
{
    public DatabaseHelper (Context context)
    {
        super(context, Config.DatabaseName, null, Config.DatabaseVersion);
    }
    
    @Override
    public void onCreate (SQLiteDatabase db)
    {}

    @Override
    public void onUpgrade (SQLiteDatabase db, int oldv, int newv)
    {}
  
} // end class databaseHelper
