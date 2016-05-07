
package com.medicaltrust.database;

public class Column
{
  String mName = null;
  String mType = null;
  boolean mPrimary = false;
  boolean mNotNull = true;
  String mDefault = null;

  public String getName () { return mName; }
  public void setName (String name) { mName = name; }

  public String getType () { return mType; }
  public void setType (String type) { mType = type; }

  public boolean isPrimary() { return mPrimary; }
  public void setPrimary (boolean primary) { mPrimary = primary; }

  public boolean isNotNull () { return mNotNull; }
  public void setNotNull (boolean notNull) { mNotNull = notNull; }

  public String getDefault () { return mDefault; }
  public void setDefault (String def) { mDefault = def; }
}
