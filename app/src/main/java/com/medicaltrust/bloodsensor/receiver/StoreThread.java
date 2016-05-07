/*
 * If you hope better performance,
 * multiple insertion will be a good solution. 
 * However sqlite does not support following popular syntax
 *   INSERT INTO 'tablename' ('column1', 'column2')
 *     VALUES ('data1', 'data2'),
 *            ('data3', 'data4'),
 *            ('data5', 'data6'),
 *            ('data7', 'data8'), ... ,
 * following code is valid.
 *   INSERT INTO 'tablename'
 *           SELECT 'data1' AS 'column1', 'data2' AS 'column2'
 *     UNION SELECT 'data3', 'data4'
 *     UNION SELECT 'data5', 'data6'
 *     UNION SELECT 'data7', 'data8'...
 */

package com.medicaltrust.bloodsensor.receiver;

import java.util.concurrent.LinkedBlockingQueue;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;

import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.MyThread;
import com.medicaltrust.bloodsensor.McbyFrames;

public class StoreThread extends MyThread
{
    // insert待ちコマンド
    abstract class ToInsert {
        protected abstract void execute();
        protected SQLiteStatement mStatement;
        public ToInsert (SQLiteStatement statement) {
            mStatement = statement;
        }
    }
    class McbyToInsert extends ToInsert {
        long mDevId;
        long mTime;
        int mFrameNo;
        int mData;
        public McbyToInsert (SQLiteStatement statement,
                             long devid, long time, int frame, int data) {
            super(statement);
            mDevId = devid;
            mTime = time;
            mFrameNo = frame;
            mData = data;
        }
        public void execute () {
            mStatement.bindLong(1, mMeasId);
            mStatement.bindLong(2, mDevId);
            mStatement.bindLong(3, mTime);
            mStatement.bindLong(4, mFrameNo);
            mStatement.bindLong(5, mData);
            mStatement.executeInsert();
        }
    }
    class PlethToInsert extends McbyToInsert {
        public PlethToInsert (long devid, long time, int frame, int pleth) {
            super(mPlethSt, devid, time, frame, pleth);
        }
    }
    class HeartRateToInsert extends McbyToInsert {
        public HeartRateToInsert (long devid, long time, int hr) {
            super(mHeartRateSt, devid, time, 0, hr);
        }
    }
    class SPO2ToInsert extends McbyToInsert {
        public SPO2ToInsert (long devid, long time, int spo2) {
            super(mSPO2St, devid, time, 0, spo2);
        }
    }

    // コンパイル済みコマンド
    SQLiteStatement mPlethSt;
    SQLiteStatement mSPO2St;
    SQLiteStatement mHeartRateSt;

    LinkedBlockingQueue<ToInsert> mWait;

    SQLiteDatabase mDb;
    long mMeasId;

    public StoreThread (SQLiteDatabase db, long measid, Handler handler)
    {
        super(handler);

        mDb = db;
        mMeasId = measid;

        mPlethSt = db.compileStatement(Config.InsertPlethStatement);
        mSPO2St = db.compileStatement(Config.InsertSPO2Statement);
        mHeartRateSt = db.compileStatement(Config.InsertHeartRateStatement);

        mWait = new LinkedBlockingQueue<ToInsert>();
    }

    public void run ()
    {
        mIsAlive = true;

        try {
            mDb.beginTransaction();
            while (mIsAlive) {
                try {
                    if (!mWait.isEmpty()) mWait.take().execute();
                    Thread.sleep(1);
                } catch (InterruptedException e) { }
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    
        cancel();
    }
    public void cancel ()
    {
        mIsAlive = false;
        for (ToInsert ti: mWait) ti.execute();
    }
    public void kill ()
    {
        mIsAlive = false;
    }
    public void pause ()
    {
    }
    public void restart ()
    {
    }

    // Mcbyの受信データをinsert待ちキューへ追加
    public void addMcby (long devid, long time, McbyFrames frames)
    {
        try {
            for (int i = 0; i < frames.size(); i++)
                mWait.put(new PlethToInsert(devid, time, i,
                                            frames.item(i).getPleth()));
            mWait.put(new HeartRateToInsert(devid, time,
                                             frames.getHeartRate()));
            mWait.put(new SPO2ToInsert(devid, time, frames.getSPO2()));
        } catch (InterruptedException e) { }
    }
}