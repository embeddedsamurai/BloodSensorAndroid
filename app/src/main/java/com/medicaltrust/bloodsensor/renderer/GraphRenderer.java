package com.medicaltrust.bloodsensor.renderer;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;

import com.medicaltrust.GraphicUtils;
import com.medicaltrust.Viewport;
import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.MeasurementActivity.MeasurementHandler;

/** GraphRenderer Abstract Class
 */
abstract public class GraphRenderer implements GLSurfaceView.Renderer
{
    static final String TAG = "GraphRenerer";
  
    final Context mContext;
    final Handler mHandler;
    final long mRefreshRate;

    long mNextUpdate;
    long mNextDraw;
    int mWidth;
    int mHeight;

    protected boolean mIsAlive;
    protected boolean mIsPaused;

    abstract protected void initFrame (GL10 gl, int w, int h);
    abstract protected void drawFrame (GL10 gl);
    // abstract protected void nextFrame (GL10 gl);
    // returns true when drawing is over.
    // abstract protected boolean nextCleanUp (GL10 gl);

    // Events
    abstract protected boolean scroll (float ox, float oy, float dx, float dy);
    abstract protected boolean fling (float ox, float oy, float dx, float dy);
    abstract protected boolean expand (float x, float y);
    abstract protected boolean ifResize (float ox, float oy);
    abstract protected boolean resize (float ox, float oy, float factor);

    public GraphRenderer (Context context, Handler handler)
    {
        mContext = context;
        mHandler = handler;
        mRefreshRate = Config.RefreshRateInv;
        mIsPaused = true;
    }

    public void onSurfaceCreated(GL10 arg0, EGLConfig arg1)
    {
    }
  
    public void onSurfaceChanged(GL10 gl, int w, int h)
    {
        long t = System.currentTimeMillis();
        mNextUpdate = t;
        mNextDraw = t;
        mWidth = w;
        mHeight = h;
        initFrame(gl, w, h);

        mIsAlive = true;
        mIsPaused = false;
    }

    public void onDrawFrame (GL10 gl)
    {
        // draw
        long thisTime = System.currentTimeMillis();
        if (mNextDraw <= thisTime) {
            if (!mIsPaused) drawFrame(gl);
            mNextDraw = thisTime + mRefreshRate
                        - ((thisTime - mNextDraw) % mRefreshRate);
        }

        if (!mIsAlive) 
            Viewport.deleteAllTextures(gl);
    }

    public boolean onScroll (float ox, float oy, float dx, float dy)
    {
        return scroll(ox / mWidth, 1.0f - oy / mHeight,
                      dx / mWidth, dy / mHeight);
    }
    public boolean onFling (float ox, float oy, float vx, float vy)
    {
        return fling(ox / mWidth, 1.0f - oy / mHeight,
                     -vx / mWidth * Config.RefreshRateInv / 1000.0f,
                     -vy / mHeight * Config.RefreshRateInv / 1000.0f);
    }
    public boolean onExpand (float x, float y)
    {
        return expand(x / mWidth, 1.0f - y / mHeight);
    }
    public boolean onScaleBegin (float x, float y)
    {
        return ifResize(x / mWidth, 1.0f - y / mHeight);
    }
    public boolean onScale (float ox, float oy, float factor)
    {
        if (factor < Config.ResizeMinFactor)
            factor = Config.ResizeMinFactor;
        else if (Config.ResizeMaxFactor < factor)
            factor = Config.ResizeMaxFactor;
        return resize(ox / mWidth, 1.0f - oy / mHeight, factor);
    }
    
    // load texture
    protected static int loadTexture (GL10 gl, Resources res,
                                      int id, String name)
        throws IOException
    {
        int texture = GraphicUtils.loadTexture(gl, res, id);
        if (texture == 0) throw new IOException(name);
        return texture;
    }

    public void finish () {
        mIsAlive = false;
    }
    public void pause () {
        mIsPaused = true;
    }
    public void restart () {
        mIsPaused = false;
    }

    // utilities
    protected void respond (int what) {
        mHandler.obtainMessage(what).sendToTarget();
    }
}
