package com.medicaltrust.bloodsensor.renderer;

import com.medicaltrust.Viewport;
import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.Config.PanelParameters;

public class Panel
{
  protected final Viewport mView;
  protected final int mN;
  
  final double mUnit;

  final double [] mXs;
  final double [] mYs;

  final double mInitLeft;
  final double mInitTop;
  final double mInitRight;
  final double mInitBottom;

  protected double mLeft;
  protected double mTop;
  protected double mRight; // > mLeft
  protected double mBottom; // < mTop

  float mVx;
  float mVy;
  
  /* paramters */
  protected int mBackgroundColor;
  protected int mForegroundColor;

  public Panel (Viewport view,
                double x, double y, double X, double Y,
                int n, double unit, PanelParameters param)
  {
    mView = view;
    mN = n;
    mUnit = unit;

    mInitLeft = mLeft = x;
    mInitTop = mTop = Y;
    mInitRight = mRight = X;
    mInitBottom = mBottom = y;

    mView.setUser(x, y, X, Y);

    mXs = new double[mN];
    mYs = new double[mN];

    for (int i = 0; i < mN; i++) {
      mXs[i] = i * mUnit;
      mYs[i] = 0.0;
    }
  }
  public double[] getArrayRef () { return mYs; }

  /* paramter setter */
  public void setBackgroundColor (int color) { mBackgroundColor = color; }
  public void setForegroundColor (int color) { mForegroundColor = color; }
  public void setParameters (PanelParameters d) {
    setBackgroundColor(d.BackgroundColor());
    setForegroundColor(d.ForegroundColor());
  }

  /* interactiveness */
  public void scroll (float dx, float dy)
  {
    {
      final double l = mLeft - mInitLeft;
      final double r = mInitRight - mRight;
      if (0.0 <= l && 0.0 <= r) {
        double d = (mRight - mLeft) * -dx;
        if (l < d) d = l;
        else if (r < -d) d = -r;
        mLeft -= d;
        mRight -= d;
      }
    }
    {
      final double b = mBottom - mInitBottom;
      final double t = mInitTop - mTop;
      if (0.0 <= b && 0.0 <= t) {
        double d = (mTop - mBottom) * dy;
        if (b < d) d = b;
        else if (t < -d) d = -t;
        mTop -= d;
        mBottom -= d;
      }
    }
    mView.setUser(mLeft, mBottom, mRight, mTop);
  }

  public void fling (float vx, float vy)
  {
    mVx = vx; mVy = vy;
  }

  public void fit (double min, double max)
  {
    mBottom = min;
    mTop = max;
    mView.setUser(mLeft, min, mRight, max);
  }

  public void resize (double ox, double oy, double factor)
  {
    if (oy > Config.ResizeHorizontallyBand) {
      final double span1 = (mTop - mBottom);
      final double span2 = span1 / factor;
      mTop -= (1.0 - oy) * (span1 - span2);
      mBottom += oy * (span1 - span2);
      if (mInitTop < mTop) mTop = mInitTop;
      if (mBottom < mInitBottom) mBottom = mInitBottom;
    } else {
      final double span1 = (mRight - mLeft);
      final double span2 = span1 / factor;
      mRight -= (1.0 - ox) * (span1 - span2);
      mLeft += ox * (span1 - span2);
      if (mInitRight < mRight) mRight = mInitRight;
      if (mLeft < mLeft) mLeft = mInitLeft;
    }
    mView.setUser(mLeft, mBottom, mRight, mTop);
  }

  protected void scrollFling ()
  {
    final double sd = Config.ScrollSpeedDown;
    if (mVx != 0.0f) {
      mVx += mVx < 0.0f ? sd : -sd;
      if (-sd < mVx && mVx < sd) mVx = 0.0f;
    }
    if (mVy != 0.0f) {
      mVy += mVy < 0.0f ? sd : -sd;
      if (-sd < mVy && mVy < sd) mVy = 0.0f;
    }

    scroll(mVx, mVy);
  }

  /* drawer */
  public void draw (int n)
  {
    scrollFling();
    
    drawBackground();
    drawWave(n);
  }

  protected void drawBackground ()
  {
    /*
    mView.setColor(mBackgroundColor);
    mView.fillRect(x, y, X, Y);
    */
  }

  protected void drawWave (int n)
  {
    mView.setColor(mForegroundColor);
    mView.drawPolyline(mXs, mYs, n);
  }

}