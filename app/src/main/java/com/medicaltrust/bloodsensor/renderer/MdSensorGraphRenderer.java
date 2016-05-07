package com.medicaltrust.bloodsensor.renderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;

import com.medicaltrust.Viewport;
import com.medicaltrust.bloodsensor.R;
import com.medicaltrust.bloodsensor.Config;
import com.medicaltrust.bloodsensor.McbyReceivedData;
import com.medicaltrust.MyFFT;
import com.medicaltrust.bloodsensor.renderer.RuledPanel;

public class MdSensorGraphRenderer extends GraphRenderer
{
    static final String TAG = "McbyGraphRenderer";

    public interface OnDrawListener
    {
        void onDraw(int spo2, int heartrate);
    }

    List<McbyGraph> mGraphs;

    Viewport mFullView;
    RuledPanel mSpecPanel;
  
    MyFFT mFFT;

    OnDrawListener mOnDrawListener;

    // real coodinate on screen. left top are 0.0, right bottom are 1.0.
    double mPlethViewLeft = 0.0;
    double mPlethViewTop = 0.55;
    double mPlethViewRight = 1.0;
    double mPlethViewBottom = 0.95;

    double mSpecViewLeft = 0.0;
    double mSpecViewTop = 0.12;
    double mSpecViewRight = 1.0;
    double mSpecViewBottom = 0.52;

    public MdSensorGraphRenderer (Context context, Handler handler)
    {
        super(context, handler);
        mGraphs = new ArrayList<McbyGraph>();
        mFFT = new MyFFT(Config.Mcby.SpecSamples);
        mOnDrawListener = null;
    }
    public void addGraph(McbyReceivedData d)
    {
        mGraphs.add(new McbyGraph(d));
    }
  
    protected void initFrame (GL10 gl, int w, int h)
    {
        mFullView = new Viewport(gl, w, h);

        Viewport specView = 
            new Viewport(gl, w, h,
                         mSpecViewLeft, mSpecViewTop,
                         mSpecViewRight, mSpecViewBottom);
        specView.setFontSize(16);

        prepareGraphics(mContext, gl);

        final Config.Mcby c = (new Config()).new Mcby();

        {
            final double left = 0.0;
            final double top = Config.Mcby.PlethMax;
            final double right =
                Config.Mcby.PlethSamplingCycle * Config.Mcby.PlethSamples;
            final double bottom = Config.Mcby.PlethMin;

            for (McbyGraph mg: mGraphs) {
                Viewport plethView =
                    new Viewport(gl, w, h,
                                 mPlethViewLeft, mPlethViewTop,
                                 mPlethViewRight, mPlethViewBottom);
                plethView.setFontSize(16);
                RuledPanel panel =
                    new RuledPanel(plethView,
                                   left, bottom, right, top,
                                   Config.Mcby.PlethSamples,
                                   Config.Mcby.PlethSamplingCycle,
                                   c.new PanelPleth());
                mg.setPanel(panel);
            }
        }
        {
            final double left = 0.0;
            final double top = Config.Mcby.SpecMax;
            final double right = 1.0 / Config.Mcby.SpecSamplingCycle;
            final double bottom = Config.Mcby.SpecMin;
    
            mSpecPanel =
                new RuledPanel(specView,
                               left, bottom, right, top,
                               Config.Mcby.SpecSamples,
                               1.0 /
                               (Config.Mcby.SpecSamples * Config.Mcby.SpecSamplingCycle),
                               c.new PanelSpec());
        }
    }

    protected void drawFrame (GL10 gl)
    {
        //    if (mData.isLowBattery()) s +=  "LowBattery!!";
    
        mFullView.setColor(Config.Mcby.BackgroundColor);
        mFullView.clear();

        for (McbyGraph mg: mGraphs) mg.draw();

        if (mGraphs.size() > 0) {
            McbyReceivedData mrd = mGraphs.get(0).getData();

            if (mOnDrawListener != null)
                mOnDrawListener.onDraw(mrd.getSPO2(), mrd.getHeartRate());

            double[] spec = mSpecPanel.getArrayRef();
            int n = mrd.getPleth(spec); // spec as pleth
            mFFT.getASpectrum(spec); // spec as spectrum
            mSpecPanel.draw(n);
        }
    }

    /* event handler */
    protected boolean scroll (float ox, float oy, float dx, float dy)
    {
        boolean consumed = false;
        // when scroll occured in pleth panel,
        if (isOnPlethPanel(ox, oy)) {
            for (McbyGraph mg: mGraphs) mg.scroll(dx, dy);
            consumed = true;
        }
        if (isOnSpecPanel(ox, oy)) {
            mSpecPanel.scroll(dx, dy);
            consumed = true;
        }
        return consumed;
    }
    protected boolean fling (float ox, float oy, float vx, float vy)
    {
        boolean consumed = false;
        if (isOnPlethPanel(ox, oy)) {
            for (McbyGraph mg: mGraphs) mg.fling(vx, vy);
            consumed = true;
        }
        if (isOnSpecPanel(ox, oy)) {
            mSpecPanel.fling(vx, vy);
            consumed = true;
        }
        return consumed;
    }
    protected boolean expand (float x, float y)
    {
        if (isOnPlethPanel(x, y)) {
            for (McbyGraph mg: mGraphs) mg.fitFull();
            return true;
        }
        return false;
    }
  
    protected boolean ifResize (float ox, float oy)
    {
        return isOnPlethPanel(ox, oy) || isOnSpecPanel(ox, oy);
    }
    protected boolean resize (float ox, float oy, float factor)
    {
        boolean consumed = false;
        if (isOnPlethPanel(ox, oy)) {
            for (McbyGraph mg: mGraphs)
                mg.resize(getViewReal(ox, mPlethViewLeft, mPlethViewRight),
                          getViewReal(oy, mPlethViewTop, mPlethViewBottom),
                          factor);
            consumed = true;
        }
        if (isOnSpecPanel(ox, oy)) {
            mSpecPanel.resize(getViewReal(ox, mSpecViewLeft, mSpecViewRight),
                              getViewReal(oy, mSpecViewTop, mSpecViewBottom),
                              factor);
            consumed = true;
        }
        return consumed;
    }
    private boolean isOnPlethPanel (float x, float y)
    {
        return mPlethViewLeft < x && x < mPlethViewRight
            && mPlethViewTop < y && y < mPlethViewBottom;
    }
    private boolean isOnSpecPanel (float x, float y)
    {
        return mSpecViewLeft < x && x < mSpecViewRight
            && mSpecViewTop < y && y < mSpecViewBottom;
    }
    private double getViewReal (float x, double min, double max)
    {
        return (x - min) / (max - min);
    }

    /* static */
    private static void prepareGraphics (Context context, GL10 gl) 
    {
        // texture settings
        Resources res = context.getResources();
        try {
            Viewport
                .setTexture(R.drawable.chars16,
                            loadTexture(gl, res, R.drawable.chars16, "chars"));
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to load texture: "+e);
        }
    }

    /** Pair of McbyReceivedData and RuledPanel
     *  RuledPanelの継承クラスになるような、ならないような・・・。
     */
    class McbyGraph
    {
        McbyReceivedData mData;
        RuledPanel mPanel;

        public McbyGraph(McbyReceivedData data) {
            mData = data;
        }
        public McbyReceivedData getData() {
            return mData;
        }
        public void setPanel(RuledPanel panel) {
            mPanel = panel;
        }
        /* Note that Panel.getArrayRef returns a reference of y-array's output.
         * And Panel.draw refers the y-array. */
        public void draw() {
            mPanel.draw(mData.getPleth(mPanel.getArrayRef()));
        }
        public void scroll(float dx, float dy) {
            mPanel.scroll(dx, dy);
        }
        public void fling(float vx, float vy) {
            mPanel.fling(vx, vy);
        }
        public void fitFull() {
            mPanel.fit(mData.getMinPleth(), mData.getMaxPleth());
        }
        public void resize(double ox, double oy, double factor) {
            mPanel.resize(ox, oy, factor);
        }
    } // McbyGraph
}
