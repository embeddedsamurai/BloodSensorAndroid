
package com.medicaltrust.database;

import java.util.HashMap;
import java.util.Set;

public class Row
{
  HashMap<String,String> mColumn = new HashMap<String,String>();

  public void add (String name, String value)
  {
    mColumn.put(name, value);
  }
  public Set<String> keys ()
  {
    return mColumn.keySet();
  }
  public String get (String name)
  {
    return mColumn.get(name);
  }
}
