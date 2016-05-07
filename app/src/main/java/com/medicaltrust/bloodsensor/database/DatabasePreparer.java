package com.medicaltrust.bloodsensor.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.medicaltrust.database.Column;
import com.medicaltrust.database.Row;

public class DatabasePreparer
{
    static final String TAG = "DatabasePreparer";

    enum State {
        Start, Name, Define, Data
    }
  
    ArrayList<Column> mCols = new ArrayList<Column>();
    ArrayList<Row> mRows = new ArrayList<Row>();

    String mTableName = null;

    Context mContext;
    DatabaseHelper mDbh;
    SQLiteDatabase mDb;
    // DatabaseSynchronizer mDatabaseSynchronizer;

    public DatabasePreparer (Context context, int[] xmls, boolean recreate)
    {
        mContext = context;
        try {
            mDbh = new DatabaseHelper(mContext);
            mDb = mDbh.getWritableDatabase();

            create(xmls, recreate);
        } catch (XmlPullParserException e) {
            android.util.Log.e(TAG, "Invalid XML syntax.");
        } catch (IOException e) {
            android.util.Log.e(TAG, "IO exception was occured.");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Unexpected error was occured. "+ e);
        }
    }
    public DatabasePreparer (Context context, int[] xmls)
    {
        this(context, xmls, false);
    }

    public void finish ()
    {
        /*
        try {
            mDatabaseSynchronizer.join();
        } catch (InterruptedException e) { }
        */
        mDbh.close();
        mDb.close();
    }

    /*
    public void synchronize ()
    {
        if (mDatabaseSynchronizer == null) {
            mDatabaseSynchronizer = new DatabaseSynchronizer (mContext, mDb);
        } else {
            try {
                mDatabaseSynchronizer.join();
            } catch (InterruptedException e) { }
            mDatabaseSynchronizer = new DatabaseSynchronizer (mContext, mDb);
        }
        android.util.Log.d("DatabasePreparer",
                           "Start database Synchronization");
        mDatabaseSynchronizer.start();
    }
    */

    // XML -> database
    private void create (int[] xmls, boolean recreate)
        throws XmlPullParserException, IOException
    {
        for (int xmlId: xmls) {
            // extract data from xml
            extractTableData(xmlId);
            // does table already exists?
            if (!checkTableExists()) {
                createTable();
                insertToTable();
            } else if (recreate) {
                deleteTable();
                createTable();
                insertToTable();
            }
        }
    }

    // extract data from xml
    private void extractTableData (int xmlResourceID)
        throws XmlPullParserException, IOException
    {
        Resources r = mContext.getResources();
        XmlResourceParser parser = r.getXml(xmlResourceID);
    
        State status = State.Start;
        int eventType = parser.getEventType();
        
        Column col = null; // definition of table columns
        Row data = null; // data to insert into table
        
        String tagName = null;
        String colName = null;
	
        /// Initialize
        
        // definitions of table columuns
        mCols = new ArrayList<Column>();
        // data to insert into table
        mRows = new ArrayList<Row>();
        
        mTableName = null;
        
        /// Extraction
        while (eventType != XmlPullParser.END_DOCUMENT) {
            
            if (eventType == XmlPullParser.START_TAG) {
                // tag opened
                tagName =  parser.getName();

                switch (status) {
                case Start:
                    if (tagName.equals("name")) 
                        // STATE_START -> STATE_DB_TABLE_NAME
                        status = State.Name;
                    break;
          
                case Data:
                    if (tagName.equals("value")) {
                        // STATE_DB_TABLE_DATA -> STATE_DB_TABLE_DATA
                        colName = parser.getAttributeValue(null, "column");
                        if (colName == null)
                            throw new XmlPullParserException("Invalid XML");
                    }
                    break;
          
                case Define:
                    if (tagName.equals("defaultValue")) {
                        // STATE_DB_TABLE_DEFINE -> STATE_DB_TABLE_DEFINE
                        String isNull = parser.getAttributeValue(null, "null");

                        if (isNull != null && isNull.equals("true")) {
                            if (col == null) col = new Column();
                            col.setDefault("NULL");
                        }
                    }
                    break;
          
                default:
                    if (tagName.equals("column"))
                        // * -> STATE_DB_TABLE_DEFINE
                        status = State.Define;
                    else if (tagName.equals("row"))
                        // * -> STATE_DB_TABLE_DATA
                        status = State.Data;
                    break;
                } // switch
            } // START_TAG
            else if (eventType == XmlPullParser.END_TAG) {
                // tag closed
                tagName =  parser.getName();

                switch (status) {
                case Define:
                    if (tagName.equals("column")) {
                        // STATE_DB_TABLE_DEFINE -> STATE_DB_TABLE_DEFINE
                        // end of definition of table column
                        if (col != null) mCols.add(col);
                        col = null;
                    }
                    break;
          
                case Data:
                    if (tagName.equals("row")) {
                        // STATE_DB_TABLE_DATA -> STATE_DB_TABLE_DATA
                        // end of data of table row
                        if (data != null) mRows.add(data);
                        data = null;
                    }
                    break;
                }
            } // end END_TAG
            else if (eventType == XmlPullParser.TEXT) {
                // content of tag
                String value = parser.getText().trim();

                switch (status) {
                case Name:
                    // STATE_DB_TABLE_NAME -> STATE_DB_TABLE_NAME
                    mTableName = value;
                    break;

                case Define:
                    // STATE_DB_TABLE_DEFINE -> STATE_DB_TABLE_DEFINE
                    if (col == null) col = new Column();

                    if (tagName.equals("name"))
                        col.setName(value);
                    else if (tagName.equals("type"))
                        col.setType(value);
                    else if (tagName.equals("primaryKey"))
                        col.setPrimary(value.equals("1"));
                    else if (tagName.equals("notNull"))
                        col.setNotNull(value.equals("1"));
                    else if (tagName.equals("defaultValue"))
                        col.setDefault(value);

                    break;
          
                case Data:
                    // STATE_DB_TABLE_DATA -> STATE_DB_TABLE_DATA
                    if (data == null) data = new Row();
                    data.add(colName,value);
                }
            }
        
            eventType = parser.next();
        } // end while
    } // end extractTableData 

    // does table already exist?
    private boolean checkTableExists ()
    {
        Cursor c =
            mDb.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'",
                         null);
        for (c.moveToFirst();; c.moveToNext()) {
            if (c.getString(0).equals(mTableName))
                return true;
            if (c.isLast()) break;
        }
        c.close();
    
        return false;
    }

    // delete tables
    private void deleteTable ()
    {
        mDb.beginTransaction();
        try {
            ArrayList<String> clmSql = new ArrayList<String>();

            String sql = String.format("DROP TABLE %s", mTableName);
            SQLiteStatement stmt = mDb.compileStatement(sql);
            stmt.execute();
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }
    // create tables
    private void createTable ()
    {
        mDb.beginTransaction();
        try {
            ArrayList<String> qs = new ArrayList<String>();

            for (Column col: mCols) {
                StringBuilder sb = new StringBuilder();
                sb.append(col.getName());
                sb.append(" ");
                sb.append(col.getType());

                if (col.isPrimary()) sb.append(" PRIMARY KEY");
                if (col.isNotNull()) sb.append(" NOT NULL");

                String def = col.getDefault();
                if (def == null)
                    sb.append(" DEFAULT ''");
                else if (!def.equals("NULL"))
                    sb.append(" DEFAULT + '" + def + "'");

                qs.add(sb.toString());
            } // for column

            String q =
                String.format ("CREATE TABLE %s (%s);",
                               mTableName, joinList (",", qs));

            android.util.Log.d(TAG, q);
      
            SQLiteStatement stmt = mDb.compileStatement(q);
            stmt.execute();
            mDb.setTransactionSuccessful();
      
        } finally {
            mDb.endTransaction();
        }
    } // createTable

    private void insertToTable ()
    {
        mDb.beginTransaction();
        try {
            for (Row row: mRows) {

                ArrayList<String> names = new ArrayList<String>();
                ArrayList<String> values = new ArrayList<String>();

                for (String cName: row.keys()) {
                    String cValue = row.get(cName);
                    names.add(cName);
                    values.add(cValue);
                }

                long id =
                    executeInsert(mDb, 
                                  "INSERT INTO %s (%s) VALUES (%s)",
                                  mTableName, names, values);
                if (id == -1)
                    android.util.Log.d(TAG,
                                       "Database inserting error: "+mTableName);
            }
            mDb.setTransactionSuccessful();
      
        } finally {
            mDb.endTransaction();
        }
    } // insertToTable

    public static long executeInsert (SQLiteDatabase db, 
                                      String statement,
                                      String tableName,
                                      ArrayList<String> names,
                                      ArrayList<String> values)
    {
        String stStr =
            String.format(statement,
                          tableName,
                          joinList(",", names),
                          joinListAsBinder(values));

        SQLiteStatement st = db.compileStatement(stStr);

        int i = 0;
        for (String v: values) st.bindString(++i, v);

        return st.executeInsert();
    }

    public static String joinListAsBinder (ArrayList<String> lst)
    {
        ArrayList<String> qms = new ArrayList<String>();
        for (String s: lst) qms.add("?");
        return joinList(",", qms);
    }
    
    public static String joinList (String separator,
                                   Iterable<? extends Object> pColl)
    {
        Iterator<? extends Object> oIter;
        if (pColl == null || (!(oIter = pColl.iterator()).hasNext()))
            return "";
    
        StringBuilder oBuilder =
            new StringBuilder(String.valueOf(oIter.next()));
    
        while (oIter.hasNext())
            oBuilder.append(separator).append(oIter.next());
    
        return oBuilder.toString();
    }
}
