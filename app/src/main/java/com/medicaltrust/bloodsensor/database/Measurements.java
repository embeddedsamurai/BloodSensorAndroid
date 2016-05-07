package com.medicaltrust.bloodsensor.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.medicaltrust.bloodsensor.Config;

public class Measurements
{
    /* Returns the row ID of the last row inserted,
     * if this insert is successful. -1 overwise. */
    public static long insert (SQLiteDatabase db)
    {
        // (id, date)
        String q = String.format("INSERT INTO %s VALUES (NULL,?)",
                                 Config.MeasurementsTableName);
    
        SQLiteStatement st = db.compileStatement(q);

        long date = java.util.Calendar.getInstance().getTimeInMillis();
        st.bindLong(1, date);

        return st.executeInsert();
    }
    public static long insertDevice (SQLiteDatabase db, long measid,
                                     String device)
    {
        if (measid == -1L) return -1L;
        // (measid, devid, device, date)
        String q = String.format("INSERT INTO %s VALUES (?,NULL,?,NULL)",
                                 Config.DevicesTableName);

        SQLiteStatement st = db.compileStatement(q);
        st.bindLong(1, measid);
        st.bindString(2, device);

        return st.executeInsert();
    }

    public static long delete (SQLiteDatabase db, long measid)
    {
        String q = String.format(Config.DeleteMeasurementStatement,
                                 Config.MeasurementsTableName);
        SQLiteStatement st = db.compileStatement(q);
        st.bindLong(1, measid);
        long id = st.executeInsert();

        for (int i = 0; i < Config.TableNames.length; i++) {
            q = String.format(Config.DeleteMeasurementDataStatement,
                              Config.TableNames[i]);
            st = db.compileStatement(q);
            st.bindLong(1, measid);
            st.executeInsert();
        }

        return id;
    }
}