package com.medicaltrust.bloodsensor;

public class McbyFrames
{
    public class Frame
    {
        int mPleth;
        boolean mValid;
    
        public Frame () {
            mPleth = 0;
            mValid = false;
        }
        public void setPleth (int p) {
            mPleth = p;
            mValid = true;
        }
        public void setInvalid () {
            mValid = false;
        }
        public boolean isValid () {
            return mValid;
        }
        public int getPleth () {
            return mPleth;
        }
    }

    Frame[] mFrames;
    int mHeartRate;
    int mSPO2;

    public McbyFrames (int size)
    {
        mFrames = new Frame[size];
        for (int i = 0; i < size; i++) mFrames[i] = new Frame();

        mHeartRate = 0;
        mSPO2 = 0;
    }

    public Frame item (int i) {
        return mFrames[i];
    }
    public int size () {
        return mFrames.length;
    }
    public int getHeartRate () {
        return mHeartRate;
    }
    public int getSPO2 () {
        return mSPO2;
    }
  
    public void set (int hr, int spo2) {
        mHeartRate = hr;
        mSPO2 = spo2;
    }

    public boolean isValid (int i) {
        return mFrames[i].isValid();
    }
    public int getPleth (int i) {
        return mFrames[i].getPleth();
    }
  
}