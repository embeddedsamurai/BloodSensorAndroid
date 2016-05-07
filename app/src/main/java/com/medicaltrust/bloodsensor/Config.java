package com.medicaltrust.bloodsensor;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import android.graphics.Color;

public class Config {

    public class PanelParameters {
        public double BaseX () { return 0.0; }
        public double BaseY () { return 0.0; }
        public int TicksX () { return 30; }
        public int TicksY () { return 50; }
        public int BigTickIntervalX () { return 5; }
        public int BigTickIntervalY () { return 5; }
        public double TickSizeX () { return 0.02; }
        public double TickSizeY () { return 0.01; }
        public String FormatX () { return "%.0f"; }
        public String FormatY () { return"%.0f"; }
        public int BackgroundColor () { return Color.BLACK; }
        public int ForegroundColor () { return Color.LTGRAY; }
        public int FontColor () { return Color.WHITE; }
        public int TickColor () { return Color.DKGRAY; }
        public int BigTickColor () { return Color.WHITE; }
    }
    public class AnnPanelParameters extends PanelParameters {
        public int AnnColor () { return Color.GREEN; }
    }

    /* ui setting */
    public static final float ResizeMaxFactor = 1.05f;
    public static final float ResizeMinFactor = 0.95f;
    public static final double ResizeHorizontallyBand = 0.15;

    public static final float ScrollSpeedDown = 0.005f;
  
    public static final SimpleDateFormat FormatDate =
        new SimpleDateFormat(" yyyy/MM/dd (ccc) a hh:mm:ss", Locale.US);
    public static final java.util.TimeZone TimeZone =
        java.util.TimeZone.getTimeZone("GMT+09:00");

    public static final String PlayerMenuTitle = "What do you do?";
    public static final String[] PlayerMenu =
        new String[] {"Play", "Delete", "Cancel"};
    public static final int PlayerMenuPlay = 0;
    public static final int PlayerMenuDelete = 1;
    public static final int PlayerMenuCancel = 2;

    public static final String PairedListName = "name";
    public static final String PairedListAddr = "addr";
    public static final String[] PairedList =
        new String[] { PairedListName, PairedListAddr };

    /* rendering setting */
    public static final long RefreshRateInv = 33L;

    /* database setting */
    public static final String DatabaseName = "mcby.db";

    public static final int DatabaseVersion = 1;
    public static final int[] DatabaseResources =
        new int[] { R.xml.measurements, R.xml.devices,
                    R.xml.pleths, R.xml.spo2s, R.xml.heartrates };

    public static final String MeasurementsTableName = "measurements";
    public static final String DevicesTableName = "devices";
    public static final String PlethsTableName = "pleths";
    public static final String SPO2sTableName = "spo2s";
    public static final String HeartRatesTableName = "heartrates";
    public static final String[] TableNames =
        new String[] { DevicesTableName,
                       PlethsTableName, SPO2sTableName, HeartRatesTableName
                     };

    /* commands */
    public static final String DeleteMeasurementStatement = 
        "DELETE FROM %s WHERE id=?";

    // for all tables which have measid
    public static final String DeleteMeasurementDataStatement = 
        "DELETE FROM %s WHERE measid=?";

    public static final String UpdateDateStatement = 
        String.format("UPDATE %s SET date=? WHERE measid=? AND devid=?",
                      Config.DevicesTableName);

    public static final String InsertPlethStatement =
        String.format("INSERT INTO %s VALUES (?,?,?,?,?)",
                      Config.PlethsTableName);
    public static final String InsertSPO2Statement =
        String.format("INSERT INTO %s VALUES (?,?,?,?,?)",
                      Config.SPO2sTableName);
    public static final String InsertHeartRateStatement =
        String.format("INSERT INTO %s VALUES (?,?,?,?,?)",
                      Config.HeartRatesTableName);

    /* query */
    public static final String MeasurementQuery =
        String.format("SELECT date"+
                      " FROM %s WHERE id = ?",
                      Config.MeasurementsTableName);

    public static final String NewMeasurementsQuery =
        String.format("SELECT id FROM %s WHERE date > ?",
                      Config.MeasurementsTableName);
    public static final String PeriodMeasurementQuery =
        String.format("SELECT id, date FROM %s"+
                      " WHERE date BETWEEN ? AND ? ORDER BY date DESC",
                      Config.MeasurementsTableName);

    public static final String DevicesQuery =
        String.format("SELECT devid, device, date FROM %s"+
                      " WHERE measid = ?",
                      Config.DevicesTableName);

    public static final String PlethsQuery =
        String.format("SELECT time, frame, pleth FROM %s"+
                      " WHERE measid = ? AND devid = ?",
                      Config.PlethsTableName);
    public static final String SPO2sQuery =
        String.format("SELECT devid, time, frame, spo2 FROM %s"+
                      " WHERE measid = ?",
                      Config.SPO2sTableName);
    public static final String HeartRatesQuery =
        String.format("SELECT devid, time, frame, heartrate FROM %s"+
                      " WHERE measid = ?",
                      Config.HeartRatesTableName);

    public static final String McbyDataQuery =
        String.format("SELECT x.time, x.pleth, x.spo2, h.heartrate FROM ("+
                      "  %s AS p INNER JOIN %s AS s"+
                      "   ON p.measid = s.measid"+
                      "  AND p.devid = s.devid"+
                      "  AND p.time = s.time"+
                      ") AS x INNER JOIN %s AS h"+
                      "   ON x.measid = h.measid"+
                      "  AND x.devid = h.devid"+
                      "  AND x.time = h.time"+
                      " WHERE x.measid = ? AND x.devid = ?"+
                      " ORDER BY x.time, x.frame",
                      Config.PlethsTableName,
                      Config.SPO2sTableName,
                      Config.HeartRatesTableName);

    /* activity communication */
    public static final String DevicesNameLabel = "devices_name";
    public static final String StoredDateLabel = "stored_date";

    public static final String MemoryTimeStartLabel = "memory_time_start";
    public static final String MemoryTimeEndLabel = "memory_time_end";  

    public static final String MeasurementIdLabel = "measid";

    /* bluetooth settings */
    public static final UUID MY_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int TryLimit = 10;
    public static final long TryInterval = 4000L;
  
    public static final int SkipTryLimit = 1000;

    /* network settings */
    public enum SocketNetworkType {
        Enable, WifiOnly, Disable
    };
    // timeout until connection is established.
    public static final int SynchronizingConnectionTimeout = 5000;
    // timeout for waiting for data.
    public static final int SynchronizingSoTimeout = 60000;

    public static final int ConnectionTimeout = 5000;
    // data fetching time out
    public static final int SoTimeout = 10000;

    // set api settings
    public static final String SetMeasAndroidIdLabel = "aid";
    public static final String SetMeasMeasIdLabel = "mid";
    public static final String SetMeasDeviceIdLabel = "did";
    public static final String SetMeasMeasLabel = "meas";
    public static final String SetMeasDeviceLabel = "device";
    public static final String SetMeasPlethLabel = "pleth";
    public static final String SetMeasSPO2Label = "spo2";
    public static final String SetMeasHeartRateLabel = "hr";

    public static final String SetMeasDivideChar = ";";

    // output api settings
    public static final String OutputMeasAndroidIdLabel = "aid";
    public static final String OutputMeasMeasIdLabel = "mid";

    public static final String UriSetMeasurement =
        "http://www.densikairo.com/BloodSensor/set";
    public static final String UriCheckSynchronized =
        "http://www.densikairo.com/BloodSensor/sync";
    public static final String UriOutputMeasurement = 
        "http://www.densikairo.com/BloodSensor/out";

    /* hardware setting */
    public class Mcby
    {
        /* refresh rate */
        public static final long MsecPerPacket = 333L;
        public static final long MsecPerFrame = 13L;

        /* graph settings */
        public static final double PlethMax = 32767.0;
        public static final double PlethMin = -32768.0;
        public static final double PlethSamplingCycle = 0.013;
        public static final int PlethSamples = 512;
  
        public static final double SpecMax = 1.0;
        public static final double SpecMin = 0.0;
        public static final double SpecSamplingCycle = 0.013;
        public static final int SpecSamples = 512;
  
        public static final int BackgroundColor = Color.BLACK;

        public class PanelPleth extends PanelParameters {
            @Override public String FormatX () { return "%.1f"; }
            @Override public String FormatY () { return "%.0f"; }
            @Override public int ForegroundColor () { return 0xa00000ff; }
        }
        public class PanelSpec extends PanelParameters {
            @Override public String FormatX () { return "%.0f"; }
            @Override public String FormatY () { return "%.2f"; }
            @Override public int ForegroundColor () { return 0xa000ff00; }
        }
    }

    /* mcby command */
    /* request */
    public static byte[] BytesSwitchToSPO2Format7 =
        new byte[] { (byte)0x44, (byte)0x37 };
    public static byte[] BytesSetMcbyDate =
        new byte[] { (byte)0x02, (byte)0x72, (byte)0x06 };
    public static byte[] BytesGetMcbyDate =
        new byte[] { (byte)0x02, (byte)0x72, (byte)0x00, (byte)0x03 };

    /* response */
    public static byte[] BytesMcbyAccepts =
        new byte[] { (byte)0x06 };
    public static byte[] BytesGotMcbyDate =
        new byte[] { (byte)0x02, (byte)0xf2, (byte)0x06 };


    {
        Config.FormatDate.setTimeZone(Config.TimeZone);
    }
}