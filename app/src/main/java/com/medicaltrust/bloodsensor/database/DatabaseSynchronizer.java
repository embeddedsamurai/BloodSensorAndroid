package com.medicaltrust.bloodsensor.database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Exception;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.provider.Settings;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.impl.client.DefaultHttpClient;

import com.medicaltrust.bloodsensor.Config;

public class DatabaseSynchronizer extends Thread
{
    public interface OnSynchronizedListener
    {
        void onItemSynchronized(int measid);
        void onAllItemsSynchronized(List<Integer> measurements);
        void onFailedToSynchronize();
    }

    HttpClient mClient;
    SQLiteDatabase mDb;
    String mAndroidId;

    OnSynchronizedListener mListener;
  
    public DatabaseSynchronizer (Context context, SQLiteDatabase db)
    {
        mClient = makeClient(context);
        mDb = db;
        mAndroidId = Settings.Secure.getString(context.getContentResolver(),
                                               Settings.Secure.ANDROID_ID);

        mListener = null;
    }
  
    @Override
    public void run ()
    {
        if (mClient == null) {
            android.util.Log.i("DatabaseSynchronizer", "Http client is not ok");
            if (mListener != null) mListener.onFailedToSynchronize();
            return;
        }
        if (mDb == null || !mDb.isOpen()) {
            android.util.Log.i("DatabaseSynchronizer", "Database is not ok");
            if (mListener != null) mListener.onFailedToSynchronize();
            return;
        }
        // get measurements id by string who is unsynchronized.
        List<Integer> ids = getUnsynchronized();
        if (ids.isEmpty()) {
            if (mListener != null) mListener.onAllItemsSynchronized(ids);
            return;
        }

        // synchronize
        for (Integer measid: ids) {
            mDb.beginTransaction();
            boolean success = false;
            try {
                copyMeasurement(measid.intValue());
                mDb.setTransactionSuccessful();
                success = true;

            } catch (IOException e) {
                android.util.Log.d("DatabaseSynchronizer",
                                   "IOException, copy measurement: "+e);
            } catch (Exception e) {
                android.util.Log.d("DatabaseSynchronizer", 
                                   "Exception, copy measurement: "+e);
            } finally {
                mDb.endTransaction();
            }
            if (!success) {
                if (mListener != null) mListener.onFailedToSynchronize();
                return;
            }
            try {
                outputMeasurement(measid.intValue());
            } catch (IOException e) { }

            if (mListener != null)
                mListener.onItemSynchronized(measid.intValue());
        }

        if (mListener != null)
            mListener.onAllItemsSynchronized(ids);

        android.util.Log.d("DatabaseSynchronizer", 
                           "All synchronizing successfully finished.");
    }

    public void setOnSynchronizedListener(OnSynchronizedListener listener)
    {
        mListener = listener;
    }
  
    private HttpClient makeClient (Context context)
    {
        ConnectivityManager cm = 
            (ConnectivityManager)
            context.getSystemService(context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            android.util.Log.e("DatabaseSynchronizer", "Network unavailable");
            return null;
        }
        int nettype = ni.getType();

        HttpClient client = null;
    
        switch (Config.SocketNetworkType.Enable) {
        case Disable: break;
        case WifiOnly:
            if (ni.isConnected()
                && nettype == ConnectivityManager.TYPE_WIFI)
                client = makeHttp();
            break;
        case Enable:
            if (ni.isConnected())
                client = makeHttp();
            break;
        }

        return client;
    }

    // prepare http client
    private HttpClient makeHttp ()
    {
        HttpClient client = new DefaultHttpClient();

        // Set parameters
        HttpParams params = client.getParams();
        // params.setParameter("http://", "Mcby");

        // Set static connection parameters
        final int t = Config.SynchronizingConnectionTimeout;
        HttpConnectionParams.setConnectionTimeout(params, t);
        HttpConnectionParams.setSoTimeout(params, 
                                          Config.SynchronizingSoTimeout);

        return client;
    }

    /** サーバ上の最新の測定の時間を取得し、それより新しい測定を未同期としてその測定IDを取得。
     *  sync API使用。
     *  sync(mid=[mid])
     */
    private List<Integer> getUnsynchronized ()
    {
        URI uri;
        try {
            uri = new URI(Config.UriCheckSynchronized);
        } catch (URISyntaxException e) {
            android.util.Log.e("DatabaseHelper",
                               "URI SYNTAX ERROR!! :"
                               +Config.UriCheckSynchronized);
            return null;
        }
      
        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair(Config.SetMeasAndroidIdLabel,
                                        mAndroidId));

        String datestr = post(uri, data);
        datestr = datestr.replaceAll("[\r\n]*$", "");
        android.util.Log.d("DatabaseSynchronizer",
                           "Latest on server is: ("+datestr+")");

        List<Integer> res = new ArrayList<Integer>();

        long date;
        try {
            date = Long.valueOf(datestr);
        } catch (NumberFormatException e) {
            // No items are synchronized, or Program Error.
            date = 0L;
        }
      
        Cursor c =
            mDb.rawQuery(Config.NewMeasurementsQuery, 
                         new String[] { Long.toString(date) });

        if (c.moveToFirst()) {
            while (true) {
                int id = c.getInt(0);
                android.util.Log.d("DatabaseHelper", "Synchronizing item "+id);
                res.add(id);
                if (c.isLast()) break;
                c.moveToNext();
            }
        }

        c.close();
      
        return res;
    }

    /** サーバへデータを送信。
     *  一度に全てを送ろうとするとOutOfMemoryExceptionが発生する場合があるので、分割する。
     *  測定IDとAndroid IDによって分割したデータの整合性はサーバ側で調整する。
     *  set API使用。
     *  set(aid=[aid], mid=[mid],
     *      [meas=[meas] | device=[device]
            | pleth=[pleth] | spo=[spo2] | hr=[hr]])
     **/
    private void copyMeasurement(int measid)
        throws Exception, IOException
    {
        URI uri;
        try {
            uri = new URI(Config.UriSetMeasurement);
        } catch (URISyntaxException e) {
            android.util.Log.e("DatabaseHelper",
                               "URI SYNTAX ERROR!! :"+Config.UriSetMeasurement);
            return;
        }

        
        {
            List<NameValuePair> param = getParameter(measid);
            param.add(new BasicNameValuePair(Config.SetMeasMeasLabel,
                                             makeMeasurementString(measid)));
            param.add(new BasicNameValuePair(Config.SetMeasDeviceLabel,
                                             makeDeviceString(measid)));

            String res = copyPartialMeasurement(uri, param);
            android.util.Log.d("DatabaseSynchronizer",
                               "Copy meas, device: "+res);
            if (res == "")
                throw new IOException("Failed to copy meas, device: id="+
                                      measid);
        }
        {
            List<NameValuePair> param = getParameter(measid);
            param.add(new BasicNameValuePair(Config.SetMeasHeartRateLabel,
                                             makeHeartRateString(measid)));
            param.add(new BasicNameValuePair(Config.SetMeasSPO2Label,
                                             makeSPO2String(measid)));

            String res = copyPartialMeasurement(uri, param);
            android.util.Log.d("DatabaseSynchronizer",
                               "Copy hr, spo2: "+res);
            if (res == "")
                throw new IOException("Failed to copy hr, spo2: id="+measid);
        }
        for (long devid: getDeviceIds(measid)) {

            List<NameValuePair> param = getParameter(measid);
            param.add(new BasicNameValuePair(Config.SetMeasDeviceIdLabel,
                                             Long.toString(devid)));
            param.add(new BasicNameValuePair(Config.SetMeasPlethLabel,
                                             makePlethString(measid, devid)));
            String res = copyPartialMeasurement(uri, param);
            android.util.Log.d("DatabaseSynchronizer", "Copy pleth: "+res);
            if (res == "")
                throw new IOException("Failed to copy pleth:"+
                                      " id="+measid+
                                      " device_id="+devid);
        }
    }

    private List<NameValuePair> getParameter(int measid)
    {
        List<NameValuePair> param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair(Config.SetMeasAndroidIdLabel,
                                         mAndroidId));
        param.add(new BasicNameValuePair(Config.SetMeasMeasIdLabel,
                                         Integer.toString(measid)));
        return param;
    }

    private String copyPartialMeasurement (URI uri, List<NameValuePair> param)
    {
        String res = post(uri, param);
        return res;
    }

    private String makeMeasurementString (int measid)
        throws Exception
    {
        String[] arg = new String[] { Integer.toString(measid) };

        Cursor c = mDb.rawQuery(Config.MeasurementQuery, arg);
        if (!c.moveToFirst())
            throw new Exception("Invalid meas id: "+measid);
        long date = c.getLong(0);
        
        c.close();

        StringBuilder sb = new StringBuilder();
        sb.append(date);
        return sb.toString();
    }

    private String makeDeviceString (int measid)
        throws Exception
    {
        List<MeasurementData> mds = new ArrayList<MeasurementData>();

        String[] arg = new String[] { Integer.toString(measid) };

        Cursor c = mDb.rawQuery(Config.DevicesQuery, arg);
        if (!c.moveToFirst())
            return "";
        while (true) {
            mds.add(new Device(c.getLong(0), c.getString(1), c.getLong(2)));
            if (c.isLast()) break;
            c.moveToNext();
        }
        c.close();

        StringBuilder sb = new StringBuilder();
        for (MeasurementData md: mds) {
            sb.append(md.toString());
            sb.append(Config.SetMeasDivideChar);
        }
        return sb.toString();
    }
    private List<Long> getDeviceIds (int measid)
        throws Exception
    {
        List<Long> ids = new ArrayList<Long>();

        String[] arg = new String[] { Integer.toString(measid) };

        Cursor c = mDb.rawQuery(Config.DevicesQuery, arg);
        if (!c.moveToFirst()) return ids;
        while (true) {
            ids.add(new Long(c.getLong(0)));
            if (c.isLast()) break;
            c.moveToNext();
        }
        c.close();

        return ids;
    }

    // これだけは測定IDとデバイスIDごとに送信する。
    private String makePlethString (int measid, long device_id)
        throws Exception
    {
        List<MeasurementData> mds = new ArrayList<MeasurementData>();

        String[] arg = new String[] { Integer.toString(measid),
                                      Long.toString(device_id) };

        Cursor c = mDb.rawQuery(Config.PlethsQuery, arg);
        if (!c.moveToFirst())
            return "";
        // throw new Exception("Check pleths table: measid="+measid);
        while (true) {
            mds.add(new Pleth(c.getInt(0), c.getInt(1), c.getInt(2)));
            if (c.isLast()) break;
            c.moveToNext();
        }
        c.close();

        StringBuilder sb = new StringBuilder();
        for (MeasurementData md: mds) {
            sb.append(md.toString());
            sb.append(Config.SetMeasDivideChar);
        }
        return sb.toString();
    }

    private String makeSPO2String (int measid)
        throws Exception
    {
        List<MeasurementData> mds = new ArrayList<MeasurementData>();

        String[] arg = new String[] { Integer.toString(measid) };

        Cursor c = mDb.rawQuery(Config.SPO2sQuery, arg);
        if (!c.moveToFirst())
            return "";
        // throw new Exception("Check spo2s table: measid="+measid);
        while (true) {
            Spo2 s = new Spo2(c.getLong(0),
                              c.getInt(1), c.getInt(2), c.getInt(3));
            mds.add(s);
            if (c.isLast()) break;
            c.moveToNext();
        }
        c.close();

        StringBuilder sb = new StringBuilder();
        for (MeasurementData md: mds) {
            sb.append(md.toString());
            sb.append(Config.SetMeasDivideChar);
        }
        return sb.toString();
    }

    private String makeHeartRateString (int measid)
        throws Exception
    {
        List<MeasurementData> mds = new ArrayList<MeasurementData>();

        String[] arg = new String[] { Integer.toString(measid) };

        Cursor c = mDb.rawQuery(Config.HeartRatesQuery, arg);
        if (!c.moveToFirst())
            return "";
        // throw new Exception("Check heartrates table: measid="+measid);
        while (true) {
            HeartRate h = new HeartRate(c.getLong(0),
                                        c.getInt(1), c.getInt(2), c.getInt(3));
            mds.add(h);
            if (c.isLast()) break;
            c.moveToNext();
        }
        c.close();
    
        StringBuilder sb = new StringBuilder();
        for (MeasurementData md: mds) {
            sb.append(md.toString());
            sb.append(Config.SetMeasDivideChar);
        }
        return sb.toString();
    }


    /** サーバーへファイル書き出しを指令する。
     *  output API使用。
     *  output(aid=[aid],mid=[mid])
     */
    private void outputMeasurement(int measid)
        throws IOException
    {
        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair(Config.OutputMeasAndroidIdLabel,
                                        mAndroidId));
        data.add(new BasicNameValuePair(Config.OutputMeasMeasIdLabel,
                                        Integer.toString(measid)));

        URI uri;
        try {
            uri = new URI(Config.UriOutputMeasurement);
        } catch (URISyntaxException e) {
            android.util.Log.e("DatabaseHelper",
                               "URI SYNTAX ERROR!!! :"+
                               Config.UriOutputMeasurement);
            return;
        }
    
        String res = post(uri, data);
    
        android.util.Log.d("DatabaseSynchronizer", "Output measurement: "+res);
    
        if (res == "")
            throw new IOException("Failed to output measurement: id="+measid);
    }

    // send data and get reponse
    private String post (URI uri, List<NameValuePair> data)
    {
        // POST
        HttpPost post = new HttpPost(uri);

        if (data != null)
            try {
                UrlEncodedFormEntity ent = new UrlEncodedFormEntity(data);
                post.setEntity(ent);
          
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "";
            }

        String res;

        try {
            // Send
            HttpResponse response = mClient.execute(post);
            StatusLine stln = response.getStatusLine();
            HttpEntity ent = response.getEntity();
        
            // Response
            if (stln.getStatusCode() == HttpStatus.SC_OK) {
          
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ent.writeTo(output);

                res = output.toString();
                // android.util.Log.d("post", "Response "+res);
            } else {
                res = "";
                android.util.Log.d("post", "Bad Status Code.");
            }
        } catch (ClientProtocolException e) {
            android.util.Log.d("post", "ClientProtocolException");
            e.printStackTrace();
            res = "";
        } catch (IOException e) {
            android.util.Log.d("post", "IOException");
            e.printStackTrace();
            res = "";
        }

        return res;
    } // end post

    // for set api
    private interface MeasurementData
    {
        abstract String toString();
    }
    public class Device implements MeasurementData
    {
        long mDevId; String mDevice; long mDate;
        
        public Device (long devid, String device, long date) {
            mDevId = devid; mDevice = device; mDate = date;
        }
        public String toString () {
            return
                Long.toString(mDevId) + "," +
                mDevice + "," +
                Long.toString(mDate);
        }
    }
    public class Pleth implements MeasurementData
    {
        int mTime; int mFrame; int mPleth;
  
        public Pleth (int time, int frame, int pleth) {
            mTime = time; mFrame = frame; mPleth = pleth;
        }
        public String toString () {
            return
                Integer.toString(mTime) + "," +
                Integer.toString(mFrame) + "," +
                Integer.toString(mPleth);
        }
    } // class Pleth
    private class HeartRate implements MeasurementData
    {
        long mDevId; int mTime; int mFrame; int mHeartRate;
    
        public HeartRate (long devid, int time, int frame, int heartrate) {
            mDevId = devid; 
            mTime = time; mFrame = frame; mHeartRate = heartrate;
        }
        public String toString () {
            return
                Long.toString(mDevId) + "," +
                Integer.toString(mTime) + "," +
                Integer.toString(mFrame) + "," +
                Integer.toString(mHeartRate);
        }
    } // class HeartRate
    public class Spo2 implements MeasurementData
    {
        long mDevId; int mTime; int mFrame; int mSPO2;
  
        public Spo2 (long devid, int time, int frame, int spo2) {
            mDevId = devid; mTime = time; mFrame = frame; mSPO2 = spo2;
        }
        public String toString () {
            return
                Long.toString(mDevId) + "," +
                Integer.toString(mTime) + "," +
                Integer.toString(mFrame) + "," +
                Integer.toString(mSPO2);
        }
    } // class Spo2
}
