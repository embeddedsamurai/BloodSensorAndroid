package com.medicaltrust.bloodsensor;

import android.util.AttributeSet;

import android.content.Context;
import android.opengl.GLSurfaceView;

import android.util.AttributeSet;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.medicaltrust.bloodsensor.renderer.GraphRenderer;

public class BluetoothGraphSurfaceView
    extends GLSurfaceView
    implements GestureDetector.OnGestureListener,
               GestureDetector.OnDoubleTapListener,
               ScaleGestureDetector.OnScaleGestureListener
{

    GraphRenderer mRenderer;
    GestureDetector mGesture;
    ScaleGestureDetector mScaleGesture;

    public BluetoothGraphSurfaceView(Context context)
    {
        this(context, null);
    }
    public BluetoothGraphSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRenderer = null;
        mGesture = new GestureDetector(context, this);
        mGesture.setOnDoubleTapListener(this);
        mScaleGesture = new ScaleGestureDetector(context, this);
    }
  
    public void setRenderer (GraphRenderer renderer)
    {
        super.setRenderer(renderer);
        mRenderer = renderer;
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        final boolean ip = mScaleGesture.isInProgress();
        mScaleGesture.onTouchEvent(event);
        return ip
            || mScaleGesture.isInProgress() 
            || mGesture.onTouchEvent(event)
            || super.onTouchEvent(event);
    }
    
    /* GestureDetector.OnGestureListener */
    @Override
    public boolean onDown (MotionEvent e)
    {
    // android.util.Log.d("McbyGraphSurfaceView", "onDown");
        return true;
    }
    @Override
    public boolean onScroll (MotionEvent e1, MotionEvent e2,
                                 float dx, float dy)
    {
        // android.util.Log.d("McbyGraphSurfaceView", "onScroll");
        return mRenderer.onScroll(e1.getX(), e1.getY(), dx, dy);
    }
    @Override
    public boolean onFling (MotionEvent e1, MotionEvent e2,
                                float vx, float vy)
    {
        return mRenderer.onFling(e1.getX(), e1.getY(), vx, vy);
    }
    @Override
    public boolean onSingleTapUp (MotionEvent e)
    {
        // android.util.Log.d("McbyGraphSurfaceView", "onSingleTapUp");
        return false;
    }
  
    @Override
    public void onShowPress (MotionEvent e)
    {}
    @Override
    public void onLongPress (MotionEvent e)
    {}

    /* GestureDetector.OnDoubleTapListener */
    @Override
    public boolean onDoubleTap (MotionEvent e)
    {
        // android.util.Log.d("McbyGraphSurfaceView", "onDoubleTap");
        return mRenderer.onExpand(e.getX(), e.getY());
    }
    @Override
    public boolean onDoubleTapEvent (MotionEvent e)
    {
        // android.util.Log.d("McbyGraphSurfaceView", "onDoubleTapEvent");
        return false;
    }
    @Override
    public boolean onSingleTapConfirmed (MotionEvent e)
    {
        // android.util.Log.d("McbyGraphSurfaceView", "onSingleTapConfirmed");
        return false;
    }

    /* ScaleGestureDetector.OnScaleGestureListener */
    @Override
    public boolean onScale (ScaleGestureDetector detector)
    {
        return mRenderer.onScale(detector.getFocusX(), detector.getFocusY(),
                                 detector.getScaleFactor());
    }
    @Override
    public boolean onScaleBegin (ScaleGestureDetector detector)
    {
        return mRenderer.onScaleBegin(detector.getFocusX(),
                                      detector.getFocusY());
    }
    @Override
    public void onScaleEnd (ScaleGestureDetector detector)
    {
    }
}