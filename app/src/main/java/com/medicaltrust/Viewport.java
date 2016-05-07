package com.medicaltrust;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.util.Log;

public class Viewport
{
  static final String TAG = "Viewport";

  static Hashtable<Integer, Integer> sTextures =
    new Hashtable<Integer, Integer>();
  
  static Viewport sCurrent;
  
  GL10 mGl;
  int mWidth;
  int mHeight;

  double mVx; // min 
  double mVy;
  double mVX; // max
  double mVY;
  
  double mUx; // min 
  double mUy;
  double mUX; // max
  double mUY;

  int mColor = 0xffffffff;
  int mFontSize = 12;

  // 0.0 <= view <= 1.0
  public Viewport (GL10 gl,
                   int width, int height,
                   double vx, double vy, double vX, double vY,
                   double ux, double uy, double uX, double uY)
  {
    mGl = gl;
    mWidth = width; mHeight = height;
    setView(vx, vy, vX, vY);
    setUser(ux, uy, uX, uY);

    sCurrent = this;
  }
  public Viewport (GL10 gl,
                   int width, int height,
                   double vx, double vy, double vX, double vY)
  {
    this(gl, width, height,
         vx, vy, vX, vY,
         0.0, 0.0, (float)width, (float)height);
  }
  public Viewport (GL10 gl, int width, int height)
  {
    this(gl, width, height,
         0.0, 0.0, 1.0, 1.0);
  }
  public void setView (double vx, double vy, double vX, double vY) {
    mVx = vx; mVy = vy; mVX = vX; mVY = vY;
  }
  public void setUser (double ux, double uy, double uX, double uY) {
    mUx = ux; mUy = uy; mUX = uX; mUY = uY;
  }
  public void setColor (int color) {
    mColor = color;
  }
  public void setFontSize (int size) {
    mFontSize = size;
  }

  public double getUserMinX () { return mUx; }
  public double getUserMaxX () { return mUX; }
  public double getUserMinY () { return mUy; }
  public double getUserMaxY () { return mUY; }

  // map user to real
  public double userToRealWidth (double uw) {
    return uw * mWidth * (mVX - mVx) / (mUX - mUx);
  }
  public double userToRealHeight (double uh) {
    return uh * mHeight * (mVY - mVy) / (mVY - mVy);
  }

  // map user to view
  public double userToViewX (double ux) {
    return (ux - mUx) / (mUX - mUx);
  }
  public double userToViewY (double uy) {
    return 1.0 - (uy - mUy) / (mUY - mUy);
  }

  // map real to view
  public double realToViewWidth (double rx) {
    return rx / (mWidth * (mVX - mVx));
  }
  public double realToViewHeight (double ry) {
    return ry / (mHeight * (mVY - mVy));
  }

  /* Map user to view and Draw different shapes. */
  public void clear () {
    mGl.glClearColor(getColorR(mColor), getColorG(mColor),
                     getColorB(mColor), getColorA(mColor));
    mGl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    mGl.glLoadIdentity();
    GLU.gluLookAt(mGl,
                  0.5f,  0.5f, -1.0f,
                  0.5f,  0.5f,  1.0f,
                  0.0f, -1.0f,  0.0f);

    mGl.glMatrixMode(mGl.GL_PROJECTION);
    mGl.glLoadIdentity();
    mGl.glOrthof(-0.5f, 0.5f, -0.5f, 0.5f, 1.0f, 10.0f);
    
    mGl.glEnable (mGl.GL_LINE_SMOOTH);
  }
  public void drawLine (double x1, double y1, double x2, double y2) {
    activate();
    GraphicUtils
      .drawLine(mGl,
                (float)userToViewX(x1), (float)userToViewY(y1),
                (float)userToViewX(x2), (float)userToViewY(y2),
                getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void drawLines (double x[], double y[], int n) {
    activate();
    final float dx[] = new float[n*2];
    final float dy[] = new float[n*2];
    for (int i = 0; i < n*2; i++) {
      dx[i] = (float)userToViewX(x[i]);
      dy[i] = (float)userToViewY(y[i]);
    }
    GraphicUtils
      .drawLines(mGl, dx, dy, n*2,
                 getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void drawRect (double x1, double y1, double x2, double y2){
    activate();
    
    final float vx1 = (float)userToViewX(x1);
    final float vy1 = (float)userToViewY(y1);
    final float vx2 = (float)userToViewX(x2);
    final float vy2 = (float)userToViewY(y2);

    final float w = Math.abs(vx1 - vx2);
    final float h = Math.abs(vy1 - vy2);

    final float x = vx1 <= vx2 ? vx1 : vx2;
    final float y = vy1 <= vy2 ? vy1 : vy2;

    GraphicUtils
      .drawRect(mGl, x, y, x+w, y+h,
                getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void fillRect (double x1, double y1, double x2, double y2) {
    activate();
    
    final float vx1 = (float)userToViewX(x1);
    final float vy1 = (float)userToViewY(y1);
    final float vx2 = (float)userToViewX(x2);
    final float vy2 = (float)userToViewY(y2);

    final float w = Math.abs(vx1 - vx2);
    final float h = Math.abs(vy1 - vy2);

    final float x = vx1 <= vx2 ? vx1 : vx2;
    final float y = vy1 <= vy2 ? vy1 : vy2;

    GraphicUtils
      .drawFilledRect(mGl, x, y, x+w, y+h,
                      getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void drawRoundRect (double x1, double y1, double x2, double y2, 
                             double arcW, double arcH) {
    activate();
    
    final float vx1 = (float)userToViewX(x1);
    final float vy1 = (float)userToViewY(y1);
    final float vx2 = (float)userToViewX(x2);
    final float vy2 = (float)userToViewY(y2);

    final float w = Math.abs(vx1 - vx2);
    final float h = Math.abs(vy1 - vy2);

    final float x = vx1 <= vx2 ? vx1 : vx2;
    final float y = vy1 <= vy2 ? vy1 : vy2;

    final float arcw = (float)userToViewX(arcW);
    final float arch = (float)userToViewY(arcH);
    
    GraphicUtils
      .drawRoundRect(mGl, x, y, x+w, y+h,
                     arcw, arch,
                     getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void fillRoundRect (double x1, double y1, double x2, double y2,
                             double arcW, double arcH) {
    activate();
    
    final float vx1 = (float)userToViewX(x1);
    final float vy1 = (float)userToViewY(y1);
    final float vx2 = (float)userToViewX(x2);
    final float vy2 = (float)userToViewY(y2);

    final float w = Math.abs(vx1 - vx2);
    final float h = Math.abs(vy1 - vy2);

    final float x = vx1 <= vx2 ? vx1 : vx2;
    final float y = vy1 <= vy2 ? vy1 : vy2;

    final float arcw = (float)userToViewX(arcW);
    final float arch = (float)userToViewY(arcH);
    
    GraphicUtils
      .drawFilledRoundRect(mGl, x, y, x+w, y+h,
                           arcw, arch,
                           getColorR(mColor), getColorG(mColor),
                           getColorB(mColor));
  }
  // floating rectangle
  /*
  public void draw3DRect(double x1, double y1, double x2, double y2,
                         boolean raised){ }
  public void fill3DRect(double x1, double y1, double x2, double y2,
                         boolean raised){ }
  */
  // ellipse
  public void drawOval (double x1, double y1, double x2, double y2) {
    activate();
    
    final float vx1 = (float)userToViewX(x1);
    final float vy1 = (float)userToViewY(y1);
    final float vx2 = (float)userToViewX(x2);
    final float vy2 = (float)userToViewY(y2);

    final float w = Math.abs(vx1 - vx2);
    final float h = Math.abs(vy1 - vy2);

    final float x = vx1 <= vx2 ? vx1 : vx2;
    final float y = vy1 <= vy2 ? vy1 : vy2;

    GraphicUtils
      .drawOval(mGl, x, y, x+w, y+h,
                getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void fillOval (double x1, double y1, double x2, double y2) {
    activate();
    
    final float vx1 = (float)userToViewX(x1);
    final float vy1 = (float)userToViewY(y1);
    final float vx2 = (float)userToViewX(x2);
    final float vy2 = (float)userToViewY(y2);

    final float w = Math.abs(vx1 - vx2);
    final float h = Math.abs(vy1 - vy2);

    final float x = vx1 <= vx2 ? vx1 : vx2;
    final float y = vy1 <= vy2 ? vy1 : vy2;

    GraphicUtils
      .drawFilledOval(mGl, x, y, x+w, y+h,
                      getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  /*
  // arc: center(x,y), radius(xr, yr)
  public void drawArc(double x, double y, double xr, double yr,
                      double startAngle, double arcAngle) { }
  // sector: center(x,y), radius(xr, yr)
  public void fillArc(double x, double y, double xr, double yr,
                      double startAngle, double arcAngle) {}
  */
  public void drawPolyline (double[] x, double[] y, int n)
  {
    activate();

    final float dx[] = new float[n];
    final float dy[] = new float[n];

    int i;
    for (i = 0; i < x.length && i < n; i++) dx[i] = (float)userToViewX(x[i]);
    while (i < n) dx[i++] = 0.0f;
    for (i = 0; i < y.length && i < n; i++) dy[i] = (float)userToViewY(y[i]);
    while (i < n) dy[i++] = 0.0f;

    GraphicUtils
      .drawPath(mGl, dx, dy, n,
                getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  public void drawPolygon (double[] x, double[] y, int n){
    activate();

    final float dx[] = new float[n+1];
    final float dy[] = new float[n+1];
    for (int i=0; i < n+1; i++) {
      dx[i] = (float)userToViewX(x[i]);
      dy[i] = (float)userToViewY(y[i]);
    }
    
    GraphicUtils
      .drawPath(mGl, dx, dy, n+1,
                getColorR(mColor), getColorG(mColor), getColorB(mColor));
  }
  /*
  public void fillPolygon(double[] x, double[] y, int numPoints){}
  */
  public void drawTexture(int resId,
                          double x1, double y1, double x2, double y2,
                          float u1, float v1, float u2, float v2) {

    mGl.glEnable(GL10.GL_BLEND);
    mGl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
    
    activate();
    GraphicUtils
      .drawTexture(mGl, getTexture(resId),
                   (float)userToViewX(x1), (float)userToViewY(y1),
                   (float)userToViewX(x2), (float)userToViewY(y2),
                   u1, v1, u2, v2,
                   getColorR(mColor), getColorG(mColor), getColorB(mColor));

    mGl.glDisable(GL10.GL_BLEND);
  }
  public void drawChar(int resId, char c,
                       double x1, double y1, double x2, double y2) {
    activate();

    mGl.glEnable(GL10.GL_BLEND);
    mGl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

    GraphicUtils
      .drawChar(mGl, getTexture(resId), c,
                (float)userToViewX(x1), (float)userToViewY(y1),
                (float)userToViewX(x2), (float)userToViewY(y2),
                getColorR(mColor), getColorG(mColor), getColorB(mColor));
    
    mGl.glDisable(GL10.GL_BLEND);
  }
  public void drawString (int resId, String str, double x, double y) {
    activate();

    mGl.glEnable(GL10.GL_BLEND);
    mGl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

    float vx = (float)userToViewX(x);
    float vw = (float)realToViewWidth(mFontSize/2.0);
    float vh = (float)realToViewHeight(mFontSize);
    
    // For compatibility with canvas.
    float vy = (float)userToViewY(y) - vh;

    GraphicUtils
      .drawText(mGl, getTexture(resId), str,
                vx, vy, vw, vh,
                getColorR(mColor), getColorG(mColor), getColorB(mColor));

    mGl.glDisable(GL10.GL_BLEND);
  }

  /* internal */
  private void activate () {
    if (equals(sCurrent)) return;

    sCurrent = this;
    
    mGl.glViewport((int)(mWidth * mVx), (int)(mHeight * mVy),
                   (int)(mWidth * (mVX - mVx)), (int)(mHeight * (mVY - mVy)));
    mGl.glMatrixMode(GL10.GL_PROJECTION);
    mGl.glLoadIdentity();
    mGl.glOrthof(-0.5f, 0.5f, -0.5f, 0.5f, 1.0f, 10.0f);
  }
  
  /* Texture */
  public static void setTexture (int id, int tex) {
    if (sTextures.containsKey(id))
      Log.w(TAG, "Texture("+id+") is already load.");
    sTextures.put(id, tex);
  }
  public static int getTexture (int id)
  {
    if (sTextures.containsKey(id)) return sTextures.get(id);
    Log.e(TAG, "Texture("+id+") has not been load.");
    return 0;
  }
  public static final void deleteAllTextures (GL10 gl)
  {
    List<Integer> keys = new ArrayList<Integer>(sTextures.keySet());
    for (Integer resId : keys) {
      int[] texId = new int[1];
      texId[0] = sTextures.get(resId);
      gl.glDeleteTextures(1, texId, 0);
    }
  }

  /* utilities */
  private static float getColorA (int c) { return ((c>>24)&0xff) / 255.0f; }
  private static float getColorR (int c) { return ((c>>16)&0xff) / 255.0f; }
  private static float getColorG (int c) { return ((c>> 8)&0xff) / 255.0f; }
  private static float getColorB (int c) { return ((c>> 0)&0xff) / 255.0f; }
}