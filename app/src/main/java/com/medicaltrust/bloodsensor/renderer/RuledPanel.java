package com.medicaltrust.bloodsensor.renderer;

import java.util.ArrayList;
import java.util.List;

import com.medicaltrust.Viewport;
import com.medicaltrust.bloodsensor.R;
import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.Config.PanelParameters;

public class RuledPanel extends Panel
{
  double mBaseX;
  double mBaseY;

  int mTicksX;
  int mTicksY;
  int mBigTickIntervalX;
  int mBigTickIntervalY;
  double mTickSizeX;
  double mTickSizeY;
  String mFormatX;
  String mFormatY;

  int mTickColor;
  int mBigTickColor;
  int mFontColor;
  
  public RuledPanel (Viewport view,
                     double x, double y, double X, double Y,
                     int n, double unit, PanelParameters param)
  {
    super(view, x, y, X, Y, n, unit, param);
    setParameters(param);
  }

  /* paramter setter */
  public void setBase (double x, double y) {
    mBaseX = x; mBaseY = y;
  }
  public void setTicks (int x, int y, int ix, int iy) {
    mTicksX = x; mTicksY = y;
    mBigTickIntervalX = ix; mBigTickIntervalY = iy;
  }
  public void setTickSize (double x, double y) {
    mTickSizeX = x; mTickSizeY = y;
  }
  public void setFormat (String x, String y) {
    mFormatX = x; mFormatY = y;
  }
  public void setFontColor (int color) { mFontColor = color; }
  public void setTickColor (int color) { mTickColor = color; }
  public void setBigTickColor (int color) { mBigTickColor = color; }

  @Override
  public void setParameters (PanelParameters d) {
    super.setParameters(d);
    
    setBase(d.BaseX(), d.BaseY());
    setTicks(d.TicksX(), d.TicksY(),
             d.BigTickIntervalX(), d.BigTickIntervalY());
    setTickSize(d.TickSizeX(), d.TickSizeY());
    setFormat(d.FormatX(), d.FormatY());
    setFontColor(d.FontColor());
    setTickColor(d.TickColor());
    setBigTickColor(d.BigTickColor());
  }

  @Override
  public void draw (int n)
  {
    scrollFling();

    final double x = mView.getUserMinX();
    final double y = mView.getUserMinY();
    final double X = mView.getUserMaxX();
    final double Y = mView.getUserMaxY();
    
    drawBackground();
    drawRuler(x, y, X, Y);
    drawWave(n);
  }

  protected void drawRuler (double x, double y, double X, double Y)
  {
    final double w = X - x; // user 
    final double h = Y - y;

    final double tx = w / mTicksX; // scaler interval
    final double ty = h / mTicksY;

    final double tw = w * mTickSizeX; // the size of scaler
    final double th = h * mTickSizeY;

    // ground line
    mView.setColor(mTickColor);
    mView.drawLine(x, mBaseY, X, mBaseY);
    mView.drawLine(x, y, X, y);
    mView.drawLine(mBaseX, y, mBaseX, Y);

    List<Double> ticksX = new ArrayList<Double>();
    List<Double> ticksY = new ArrayList<Double>();

    double xs[];
    double ys[];

    { // scaler
      int i = 0;

      getTicks(ticksX, x, X, mTicksX);
      getTicks(ticksY, y, Y, mTicksY);

      final int n = (ticksX.size()+ticksY.size()) * 2;
      xs = new double[n];
      ys = new double[n];

      for (double a : ticksX) {
        xs[i*2] = a; xs[i*2+1] = a;
        ys[i*2] = y-th; ys[i*2+1] = y+th;
        ++i;
      }
      for (double a : ticksY) {
        xs[i*2] = x-tw; xs[i*2+1] = x+tw;
        ys[i*2] = a;    ys[i*2+1] = a;
        ++i;
      }
      mView.setColor(mTickColor);
      mView.drawLines(xs, ys, i);
    }
    { // big scaler
      int i = 0;
      
      getTicks(ticksX, x, X, mTicksX/mBigTickIntervalX);
      getTicks(ticksY, y, Y, mTicksY/mBigTickIntervalY);

      final int n = (ticksX.size()+ticksY.size()) * 2;
      xs = new double[n];
      ys = new double[n];

      for (double a : ticksX) {
        xs[i*2] = a; xs[i*2+1] = a;
        ys[i*2] = y-th; ys[i*2+1] = y+th;
        ++i;
      }

      for (double a : ticksY) {
        xs[i*2] = x-tw; xs[i*2+1] = x+tw;
        ys[i*2] = a;    ys[i*2+1] = a;
        ++i;
      }
      mView.setColor(mBigTickColor);
      mView.drawLines(xs, ys, i);
    }
    // labels
    mView.setColor(mFontColor);
    
    getTicks(ticksX, x, X, mTicksX/mBigTickIntervalX);
    getTicks(ticksY, y, Y, mTicksY/mBigTickIntervalY);

    for (double a: ticksX)
      mView.drawString(R.drawable.chars16, String.format(mFormatX, a),
                       a-tw, y+th);
    
    // ticksY.remove(0); // discard
    for (double a: ticksY)
      mView.drawString(R.drawable.chars16, String.format(mFormatY, a),
                       x+tw, a);
  }
  
  protected static void getTicks (List<Double> list,
                                  double min, double max, int n)
  {
    list.clear();
    if (max == min) return;

    final double x = (max - min) / n;
    final double y = Math.pow(10.0,
                        Math.floor(Math.log(Math.abs(x))/
                                   Math.log(10.0)));
    final double z = y * Math.rint(x/y);
    
    if (z == 0.0) return;
    for (double a = z * Math.rint(min/z); a <= max; a += z)
      list.add(a);
  }
}
