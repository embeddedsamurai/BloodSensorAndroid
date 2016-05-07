package com.medicaltrust.bloodsensor.receiver;

import java.util.Date;

import android.bluetooth.BluetoothDevice;

import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.McbyFrames;
import com.medicaltrust.bloodsensor.McbyReceivedData;
import com.medicaltrust.bloodsensor.MeasurementActivity.MeasurementHandler;
import com.medicaltrust.bloodsensor.MeasurementActivity.McbyHandler;

/** Receiver thread for Data Format #7
 *  See ReceiverThread.
 */
public class McbyReceiverThread extends ReceiverThread
{
    final static String TAG = "McbyReceiverThread";
    protected final String fTAG() { return TAG; }

    final static byte[] mBuffer = new byte[125];
    final McbyFrames mFrames = new McbyFrames(25);
    final McbyReceivedData mData;
    final StoreThread mStore;

    final long mDevId;

    protected int mTMR = -1;
    int mLastTMR = -1;
    static final int MAX_TMR = 128 * 128;

    McbyHandler mHandler2;

    public McbyReceiverThread (BluetoothDevice device,
                               McbyReceivedData rd, StoreThread st,
                               McbyHandler h, long devid)
    {
        super(device, h);
        mData = rd;
        mStore = st;
        mDevId = devid;

        mHandler2 = h;
    }

    protected boolean hello ()
    {
        try {
            android.util.Log.d("McbyReceiverThread", "TryToConnect");
            int r = tryToConnect();
            respond(r);
            if (r != mHandler.Connected) return false;
      
            android.util.Log.d("McbyReceiverThread", "TryToGetStream");
            r = tryToGetStream();
            respond(r);
            if (r != mHandler.GotStream) return false;

            /* 時間差測定開始: t1, 装置の基準時間: t1
             * 時間差測定終了: t3, 装置内の基準時間からの経過時間: t2 - t1
             */
            /*
            android.util.Log.d("McbyReceiverThread", "TryToSetDate");
            long t1 = System.currentTimeMillis();
            tryToSetDate(t1);

            android.util.Log.d("McbyReceiverThread", "TryToGetDate");
            long t2 = tryToGetDate();
            if (t2 == 0L) {
                android.util.Log.d("McbyReceiverThread.tryToGetDate", "failed");
                return false;
            }

            long t3 = System.currentTimeMillis();

            android.util.Log.d("McbyReceiverThread",
                               "Real time (includes communication time):"+(t3-t1)+
                               ", Mcby internal time:"+(t2-t1));
            */

            android.util.Log.d("McbyReceiverThread", "TryToChangeFormat");
            r = tryToChangeFormat();
            respond(r);
            if (r != mHandler.ChangedFormat) return false;

            android.util.Log.d("McbyReceiverThread", "Complete startup");

        } catch (SendException e) {
            respond(mHandler.UnableToSend);
            cancel();
            return false;

        } catch (ReceiveException e) {
            respond(mHandler.UnableToReceive);
            cancel();
            return false;
        }

        // Send actual time when measurement started.
        if (mHandler2 == null)
            android.util.Log.d("McbyReceiverThread", "McbyHandler null");
        respond(mHandler2.UpdateDate,
                mHandler2.new DeviceDate(mDevId, 
                                         java.util.Calendar
                                         .getInstance().getTimeInMillis()));

        return true;
    }

    private int tryToChangeFormat ()
        throws SendException, ReceiveException
    {
        send(Config.BytesSwitchToSPO2Format7);
        skipPackets(mBuffer);
        if (eqBytes(mBuffer, Config.BytesMcbyAccepts, 1)) {
            android.util.Log.d("McbyReceiverThread", "ChangedFormat");
            return mHandler.ChangedFormat;
        } else {
            android.util.Log.d("McbyReceiverThread",
                               "UnableToChangeFormat:"+
                               byteToString(mBuffer[0]));
            return mHandler.UnableToChangeFormat;
        }
    }

    private void tryToSetDate (long time)
        throws SendException, ReceiveException
    {
        final int offset = Config.BytesSetMcbyDate.length;

        byte[] s = new byte[offset + 6 + 1];
        for (int i = 0; i < offset; i++) s[i] = Config.BytesSetMcbyDate[i];

        Date date = new Date(time);
        s[offset+0] = (new Integer(date.getYear()-100)).byteValue();
        s[offset+1] = (new Integer(date.getMonth()+1)).byteValue();
        s[offset+2] = (new Integer(date.getDate())).byteValue();
        s[offset+3] = (new Integer(date.getHours())).byteValue();
        s[offset+4] = (new Integer(date.getMinutes())).byteValue();
        s[offset+5] = (new Integer(date.getSeconds())).byteValue();
        s[offset+6] = (byte)0x03;

        /*
          android.util.Log.d("McbyReceiverThread.SetDate",
          String.format("%04d/%02d/%02d %02d:%02d:%02d",
          date.getYear()+1900,
          date.getMonth()+1,
          date.getDate(),
          date.getHours(),
          date.getMinutes(),
          date.getSeconds()));
        */
    
        send(s);

        skipPackets(mBuffer);
        if (eqBytes(mBuffer, Config.BytesMcbyAccepts, 1))
            android.util.Log.d("McbyReceiverThread.tryToSetDate", "success");
        else
            android.util.Log.d("McbyReceiverThread.tryToSetDate",
                               "failed:"+byteToString(mBuffer[0]));
    }

    private long tryToGetDate ()
        throws SendException, ReceiveException
    {
        send(Config.BytesGetMcbyDate);

        while (skipPackets(mBuffer) != Config.BytesGotMcbyDate[0]) {}
        receive(mBuffer, 1, 10);

        android.util.Log.d("McbyReceiverThread.tryToGetDate",
                           bytesToString(mBuffer, 10));
        if (!eqBytes(mBuffer, Config.BytesGotMcbyDate, 3)) {
            return 0L;
        }

        int year   = byteToInt(mBuffer[3]);
        int month  = byteToInt(mBuffer[4]);
        int day    = byteToInt(mBuffer[5]);
        int hour   = byteToInt(mBuffer[6]);
        int minute = byteToInt(mBuffer[7]);
        int second = byteToInt(mBuffer[8]);
    
        /*
          android.util.Log.d("McbyReceiverThread",
          String.format("GotDate: %04d/%02d/%02d %02d:%02d:%02d",
          year+2000, month, day,
          hour, minute, second));
        */

        Date date = new Date(year+100, month-1, day, hour, minute, second);
        return date.getTime();
    }

    protected boolean talk ()
    {
        try {
            receive(mBuffer);

        } catch (ReceiveException e) {
            respond (mHandler.UnableToReceive);
            cancel();
            return false;
        }

        deserialize(mBuffer); // out mFrames, out mData
        if (mStore != null) store(mFrames); // out mStore

        return true;
    }

    protected boolean bye ()
    {
        return true;
    }
    protected boolean helloAgain ()
    {
        return true;
    }
    protected boolean byeOnce ()
    {
        return true;
    }

    protected void deserialize (byte[] x)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mFrames.size(); i++) { // (i-1)th packet
            McbyFrames.Frame f = mFrames.item(i);
            if (getStatus(x[i*5], f)
                && checkCheck(x[i*5], x[i*5+1], x[i*5+2], x[i*5+3], x[i*5+4]))
                f.setPleth(256 * byteToInt(x[i*5+1]) + byteToInt(x[i*5+2]) - 32768);
            else
                f.setInvalid();
            sb.append(byteToString(x[i*5]));
        }
        android.util.Log.d(TAG, "("+sb.toString()+")");
        lookFloat(mFrames, x, 5, 4); // mFrames.set will be called.

        mData.set(mFrames);
    }

    protected void store (McbyFrames frames)
    {
        long t = mTMR * Config.Mcby.MsecPerPacket;

        mStore.addMcby(mDevId, t, frames);
    }

    // returns the first byte which is not head of packet
    protected byte skipPackets (byte[] buffer) throws ReceiveException
    {
        // StringBuilder sb = new StringBuilder();
        receive(buffer, 1);
        while (isPacketHead(buffer[0])) {
            receive(buffer, 1, 5);
            // sb.append(bytesToString(buffer, 5));
            receive(buffer, 1);
        }
        // sb.append(bytesToString(buffer, 1));
        // android.util.Log.d("McbyReceiverThread.skipPackets", "("+sb.toString()+")");
        return buffer[0];
    }

    protected static boolean isPacketHead (byte a)
    {
        return (a & 0x80) != 0;
    }
    protected static boolean getStatus (byte a, McbyFrames.Frame f)
    {
        if ((a & 0x80) == 0) return false;
        /*
          switch ((a & 0x60) >> 1) {
          case 0:
          case 1: f.setPerfusion((byte)1); break;
          case 2: f.setPerfusion((byte)3); break;
          case 3: f.setPerfusion((byte)2); break;
          default: Log.e("ReceiverThread", "FIXME in getStatus!");
          }
          f.setArtifact((a & 0x20) != 0);
          f.setOutOfTrack((a & 0x10) != 0);
          f.setSensorAlarm((a & 0x08) != 0);
          f.setSync((a & 0x01) != 0);
        */
        return true;
    }
    protected void lookFloat (McbyFrames frames, byte[] x, int n, int i)
    {
        final int j = i-1; // i th
        int hr = calcInt7(x[j], x[n+j]);
        int spo2 = calcInt7(x[2*n+j]);
        android.util.Log.d("McbyReceiverThread", "HeartRate:"+hr+", SPO2:"+spo2);
        // mSREV  = calcInt7(x[3*n+j]);

        // Frame5 reserved
        int tmr = calcInt7(x[5*n+j], x[6*n+j], 0x7f, 0x7f);
        if (mLastTMR == -1) {
            mTMR = 0;
        } else {
            mTMR += tmr > mLastTMR ? tmr - mLastTMR : MAX_TMR - mLastTMR + tmr;
        }
        mLastTMR = tmr;

        /*
          mSTAT  = calcInt7(x[7*n+j]);
          mSPOD  = calcInt7(x[8*n+j]);
          mSPOF  = calcInt7(x[9*n+j]);
          mSPOB  = calcInt7(x[10*n+j]);
          // Frame12, 13 reserved
          mEHR   = calcInt7(x[13*n+j], x[14*n+j]);
          mESPO  = calcInt7(x[15*n+j]);
          mESPOD = calcInt7(x[16*n+j]);
          // Frame 18, 19 reserved
          mHRD   = calcInt7(x[19*n+j], x[20*n+j]);
          mEHRD  = calcInt7(x[21*n+j], x[22*n+j]);
          // Frame 24,25 reserved
          */

        frames.set(hr, spo2);
    } // lookFloat

    // deserializer utilities
    protected static boolean checkStart (byte a) {
        return a == 1;
    }
    protected static boolean checkCheck (byte a, byte b, byte c, byte d, byte e)
    {
        return
            (byteToInt(a) + byteToInt(b) + byteToInt(c) + byteToInt(d)) % 256
            == byteToInt(e);
    }
    protected static int calcInt7 (byte lsb) {
        return lsb & 0x7f;
    }
    protected static int calcInt7 (byte msb, byte lsb) {
        return 128 * (msb & 0x03) + (lsb & 0x7f);
    }
    protected static int calcInt7 (byte msb, byte lsb, int msbm, int lsbm) {
        return 128 * (msb & msbm) + (lsb & lsbm);
    }
    /*
      protected static final int byteToInt (byte b) {
      return b >= 0 ? b : 256 + b;
      }
    */

    protected void respond (int what, Object obj) {
        respond2(what, 0, obj);
    }
    protected void respond (int what) {
        respond(what, null);
    }

    // debug
    protected static String byteToString (byte b) {
        return String.format("%02x,", byteToInt(b));
    }
    protected static String bytesToString (byte[] b) {
        return bytesToString(b, b.length);
    }
    protected static String bytesToString (byte[] b, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(byteToString(b[i]));
        return sb.toString();
    }
    /*
      protected static String framesPlethToString (FramePleth[] x) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < x.length; i++)
      sb.append(x[i].isValid() ? String.format("%d,", x[i].getPleth()) : "#,");
      return sb.toString();
      }
    */
}
