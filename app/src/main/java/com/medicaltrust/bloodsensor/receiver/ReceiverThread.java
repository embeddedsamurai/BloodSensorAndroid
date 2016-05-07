package com.medicaltrust.bloodsensor.receiver;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.MyThread;
import com.medicaltrust.bloodsensor.MeasurementActivity.MeasurementHandler;

/** Receiver thread
 *  Establish connection with Bluetooth device.
 */
public abstract class ReceiverThread extends MyThread
{
    protected boolean mIsAlive;
    MeasurementHandler mHandler;

    // abstract
    protected abstract String fTAG();

    // connection initializer
    protected abstract boolean hello();
    // talk with bluetooth device
    protected abstract boolean talk();
    // connection finish
    protected abstract boolean bye();
    // pause and restart
    protected abstract boolean helloAgain();
    protected abstract boolean byeOnce();
    // message sender
    protected abstract void respond(int what, Object obj);

    final BluetoothSocket mSocket;
  
    InputStream mInStream;
    OutputStream mOutStream;
  
    public ReceiverThread (BluetoothDevice device,
                           MeasurementHandler h)
    {
        super(h);
        mIsAlive = false;
    
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(Config.MY_UUID);
        } catch (IOException e) {
            Log.e(fTAG(), "failed to prepare client socket.");
        }
        mSocket = tmp;
    }
    
    public void run () {
        mIsAlive = true;
        
        if (!hello ()) return;

        respond(MeasurementHandler.StartMeasuring);
        while (mIsAlive) {
            if (mIsPaused)
                try {
                    Thread.sleep(1L);
                    continue;
                } catch (InterruptedException e) {}
            if (!talk()) break;
        }
        respond(MeasurementHandler.EndMeasuring);
        
        bye();
        cancel();
    }
    
    public void cancel ()
    {
        mIsAlive = false;
        
        try {
            /* BluetoothSocketがopen待ちの時だと、closeにしばらく挑戦し続けてしまう。
             * でもisConnectedはAPI Level 14なので使えない。
             */
            // if (mSocket != null && mSocket.isConnected())
            if (mSocket != null) mSocket.close();
        } catch (IOException e) { }
    
        Log.e(fTAG(), "canceled");
    
        respond(MeasurementHandler.FinishedAllTask);
    }
  
    public void kill()
    {
        Log.e(fTAG(), "killed");
        mIsAlive = false;
    }

    public void pause()
    {
        mIsPaused = true;
        if (!byeOnce()) cancel();
    }
    public void restart()
    {
        if (helloAgain())
            mIsPaused = false;
        else
            cancel();
    }
    

    protected void send (byte[] command)
        throws SendException
    {
        try {
            mOutStream.write(command);
        } catch (IOException e) {
            throw new SendException("Command: "+bytesToText(command));
        }
    }
    protected void receive(byte[] buffer, int off, int size)
        throws ReceiveException
    {
        int offset = off;
        while (offset < size) {
            try {
                int bytes = mInStream.read(buffer, offset, size-offset);
                offset += bytes;
            } catch (IOException e) {
                throw new ReceiveException("Buffer: "+bytesToText(buffer));
            }
        }
    }
    protected void receive(byte[] buffer, int size)
        throws ReceiveException
    {
        receive(buffer, 0, size);
    }
    protected void receive(byte[] buffer)
        throws ReceiveException
    {
        receive(buffer, buffer.length);
    }
    
    /* skip buffer until command is received.
       command is also discarded.
    */
    protected boolean skipFor(byte[] command, int tryLimit)
        throws ReceiveException
    {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1];
        
        int t = tryLimit;
        while (--t > 0) {
            receive(buffer, 1);
            int i;
            for (i = 0; i < command.length; i++) {
                sb.append(byteToString(buffer[0]));
                if (buffer[0] != command[i]) break;
                receive(buffer, 1);
            }
            if (i == command.length) return true;
        }
        android.util.Log.d("ReceiverThread.skipFor", sb.toString());
        return false;
    }
    
    // initialization
    protected int tryToConnect ()
    {
        if (mSocket == null) return MeasurementHandler.SocketIsNull;
        
        for (int i = 0; i < Config.TryLimit; i++) {
            if (i != 0)
                try {
                    Thread.sleep(Config.TryInterval);
                } catch (InterruptedException e) { }
            try {
                mSocket.connect();
            } catch (IOException e) {
                continue;
            }
            return MeasurementHandler.Connected;
        }
        cancel();
        return MeasurementHandler.UnableToConnect;
    }
  
    protected int tryToGetStream ()
    {
        if (mSocket == null /* || !mSocket.isConnected() */)
            return MeasurementHandler.SocketIsNull;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = mSocket.getInputStream();
            out = mSocket.getOutputStream();
        } catch (IOException e) {
            cancel();
            return MeasurementHandler.UnableToGetStream;
        }
        mInStream = in;
        mOutStream = out;
        return MeasurementHandler.GotStream;
    }

    // deserializer utilities
    /*
      protected static boolean getStatus (byte a, iLiseFrame f)
      {
      if ((a & 0x80) == 0) return false;
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
      return true;
      }
    */
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
    protected static final int byteToInt (byte b) {
        return b >= 0 ? b : 256 + b;
    }

    // debug
    protected static String byteToString (byte b) {
        return String.format("%x,", byteToInt(b));
    }
    protected static String bytesToString (byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++)
            sb.append(byteToString(b[i]));
        return sb.toString();
    }
    /*
      protected static String framesPlethToString (iLiseFrame[] x) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < x.length; i++)
      sb.append(x[i].isValid() ? String.format("%d,", x[i].getPleth()) : "#,");
      return sb.toString();
      }
    */
    protected static String byteToBinaryString (byte b) {
        return
            (b >= 0 ? "0" : "1") +
            (((b & 0x40) >> 6) == 0 ? "0" : "1") +
            (((b & 0x20) >> 5) == 0 ? "0" : "1") +
            (((b & 0x10) >> 4) == 0 ? "0" : "1") +
            (((b & 0x08) >> 3) == 0 ? "0" : "1") +
            (((b & 0x04) >> 2) == 0 ? "0" : "1") +
            (((b & 0x02) >> 1) == 0 ? "0" : "1") +
            (((b & 0x01) >> 0) == 0 ? "0" : "1");
    }
    protected static String bytesToBinaryString (byte[] b, String del) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i != 0) sb.append(del);
            sb.append(byteToBinaryString(b[i]));
        }
        return sb.toString();
    }
    protected static String bytesToBinaryString (byte[] b) {
        return bytesToBinaryString(b, ",");
    }

    protected static String bytesToText (byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++)
            sb.append(String.valueOf((char)b[i]));
        return sb.toString();
    }

    protected boolean eqBytes (byte[] a, byte[] b, int size) {
        if (a.length < size || b.length < size) return false;
        for (int i = 0; i < size; i++)
            if (a[i] != b[i]) return false;
        return true;
    }

    // Exception 
    protected class SendException extends Exception {
        SendException(String m) {
            super(m);
        }
    }
    protected class ReceiveException extends Exception {
        ReceiveException(String m) {
            super(m);
        }
    }
} // ReceiverThread
