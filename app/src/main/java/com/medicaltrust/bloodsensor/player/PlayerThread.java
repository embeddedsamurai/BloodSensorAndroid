package com.medicaltrust.bloodsensor.player;

import android.os.Handler;
import android.database.Cursor;

import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.PlaybackActivity.PlaybackHandler;
import com.medicaltrust.bloodsensor.MyThread;

public abstract class PlayerThread extends MyThread
{
    Cursor mCursor;
    protected long mNextSend;

    // put data into shared memory.
    protected abstract boolean hello (Cursor c);
    protected abstract boolean talk (Cursor c);
    protected abstract void bye ();
  
    public PlayerThread (Handler h, Cursor c)
    {
        super(h);
        mCursor = c;
        mNextSend = 0;
    }

    public void run ()
    {
        respond(PlaybackHandler.EndSelecting);
    
        if (!mCursor.moveToFirst()) {
            respond(PlaybackHandler.NoData);
            return;
        }
        mNextSend = System.currentTimeMillis();

        if (!hello(mCursor)) return;

        respond(PlaybackHandler.StartPlaying);

        mIsAlive = true;

        while (mIsAlive) {
            long now = System.currentTimeMillis();
            if (now < mNextSend)
                try {
                    Thread.sleep(mNextSend - now);
                } catch (InterruptedException e) { }
            else if (!talk(mCursor) || mCursor.isLast()) break;
        }
    
        respond(PlaybackHandler.EndPlaying);
        bye();
        cancel();
    }

    public void runAtOnce ()
    {
        if (!mCursor.moveToFirst()) {
            respond(PlaybackHandler.NoData);
            return;
        }
        if (!hello(mCursor)) return;

        respond(PlaybackHandler.StartPlaying);

        while (true)
            if (!talk(mCursor) || mCursor.isLast()) break;
    
        respond(PlaybackHandler.EndPlaying);
        bye();
        cancel();
    }

    public void cancel () {
        respond(PlaybackHandler.FinishedAllTask);
    }
    public void kill () {
        mIsAlive = false;
    }
    public void pause () {
    }
    public void restart () {
    }

}