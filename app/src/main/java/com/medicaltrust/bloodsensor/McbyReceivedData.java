package com.medicaltrust.bloodsensor;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class McbyReceivedData
{
    LinkedBlockingQueue<Double> mPleth;
    double mLatestMax;
    double mLatestMin;

    /* SmartPoint Algorithm:
     * High quality SmarPoint measurement.
     */
    protected boolean mSPA = false;
  
    /* Low Battery Condition:
     * Low Batteries. Replace batteries as soon as possible.
     */
    protected boolean mLOWBAT = false;

    /* Heart Rate
     */
    protected int mHR;

    /* SPO2
     */
    protected int mSPO;


    public McbyReceivedData (int first_size)
    {
        mPleth = new LinkedBlockingQueue<Double>();
        for (int i = 0; i < first_size; i++) mPleth.offer(0.0);
    }

    /*
      public final void set (boolean spa, boolean lowbat, int hr, int spo,
      int pleth)
      {
      mSPA = spa;
      mLOWBAT = lowbat;
      mHR = hr;
      mSPO = spo;
      try {
      mPleth.put((double)pleth);
      } catch (InterruptedException e) { }
      }
      public final void set (boolean spa, boolean lowbat, int hr, int spo,
      FramePleth frames[])
      {
      mSPA = spa;
      mLOWBAT = lowbat;
      mHR = hr;
      mSPO = spo;
      try {
      for (int i = 0; i < frames.length; i++) {
      if (frames[i].isValid())
      mPleth.put((double)frames[i].getPleth()); // you may want to save by int!!
      else
      mPleth.put(0.0);
      }
      } catch (InterruptedException e) { }
      }
    */
    public void set (McbyFrames frames)
    {
        /*
          mSPA = spa;
          mLOWBAT = lowbat;
        */
        mHR = frames.getHeartRate();
        mSPO = frames.getSPO2();
        try {
            for (int i = 0; i < frames.size(); i++) {
                if (frames.item(i).isValid())
                    // you may want to save by int!!
                    mPleth.put((double)frames.item(i).getPleth());
                else
                    mPleth.put(0.0);
            }
        } catch (InterruptedException e) { }
    }

    // returns the number of data obtained.
    public int getPleth (double[] out) {
        /*
          android.util.Log.d("ReceivedDataPleth",
          "[get] Pleth length is "+mPleth.size());
        */
        if (mPleth.size() != 0) {
            mLatestMax = Double.MIN_VALUE;
            mLatestMin = Double.MAX_VALUE;
        }
        int i = 0;
        for (double d: mPleth) {
            if (!(i < out.length)) break;
            out[i++] = d;
            if (mLatestMax < d) mLatestMax = d;
            else if (mLatestMin > d) mLatestMin = d;
        }
    
        return i;
    }
    public void nextScene (int thr) {
        if (mPleth.size() <= thr) return; // not smart

        try {
            /*
              android.util.Log.d("ReceivedDataPleth",
              "[take] Pleth length is "+mPleth.size());
            */
            mPleth.take();
        } catch (InterruptedException e) { }
    }
    public void nextCleanUp (int thr) {
        if (mPleth.size() <= thr) return; // not smart
    
        try {
            mPleth.put(0.0);
            mPleth.take();
        } catch (InterruptedException e) { }
    }

    public double getMaxPleth () { return mLatestMax; }
    public double getMinPleth () { return mLatestMin; }
    // public boolean isSmartPoint()  { return mSPA;    }
    // public boolean isLowBattery()  { return mLOWBAT; }
    public int getHeartRate()      { return mHR;  }
    public int getSPO2()           { return mSPO; }
}