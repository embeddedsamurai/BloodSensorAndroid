package com.medicaltrust.bloodsensor;

import java.util.ArrayList;
import java.util.List;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.egoclean.android.widget.flinger.ViewFlinger;

public class CalendarView extends ViewFlinger
{
    Month mPrevMonth;
    Month mCurrMonth;
    Month mNextMonth;

    Page mPrevPage;
    Page mCurrPage;
    Page mNextPage;

    Calendar mCurrentCal; // 要らないかも

    final int cTodayForeground;
    final int cTodayBackground;

    public interface OnDayClickListener 
    {
        public void onDayClick(View view, int year, int month, int day);
    }
    public interface OnPageChangedListener
    {
        public boolean onPageChanged(View view, int year, int month);
    }
    OnPageChangedListener mPageChangedListener;

    public CalendarView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // layout attributes
        TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.CalendarView);

        cTodayForeground = a.getColor(R.styleable.CalendarView_today_foreground,
                                      0xffffffff);
        cTodayBackground = a.getColor(R.styleable.CalendarView_today_background,
                                      0x00000000);

        initCalendar(context);
        setCurrentScreenNow(1, false);

        setOnScreenChangeListener(new ViewFlinger.OnScreenChangeListener() {
            @Override
            public void onScreenChanged(View newScreen, int newScreenIndex) {
                /* 起動時、今月の頁に移動させたときにもOnScreenChangeListenerが
                 * 発動して後と整合がとりにくいので、一回目は捨てるようにしている。
                 * 今月の頁へ移動したことが確認できたら改めてイベントを設定する。
                 */
                if (newScreenIndex != 1) return; // hack
                onPrepared(); // hack

                setOnScreenChangeListener(
                    new ViewFlinger.OnScreenChangeListener() {
                        @Override
                        public void onScreenChanged(View newScreen,
                                                    int newScreenIndex) {
                            paging(newScreen, newScreenIndex);
                        }
                        @Override
                        public void onScreenChanging(View newScreen, 
                                                     int newScreenIndex) {}
                    });
            }
            @Override
            public void onScreenChanging(View newScreen, int newScreenIndex) {}
        });
    }

    // setting
    public void setTextColor(TextView tv, int color) {
        tv.setTextColor(color);
    }
    public void setBackgroundColor(View v, int color) {
        v.setBackgroundColor(color);
    }
    public void setTodayForeground(int color) {
        View today = getTodayView();
        setTextColor((TextView)today.findViewById(R.id.day), color);
    }
    public void setTodayBackground(int color) {
        View today = getTodayView();
        setBackgroundColor(today, color);
    }

    /*
    @Override
    protected void onLayout(boolean changed, int l, int u, int r, int d) 
    {        
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        for (int i = 0; i < getChildCount(); i++) {
            View page = getScreenAt(i).findViewById(R.id.calendar_days);
            View child = page.getChildAt(0);
            child.measure(widthMeasureSpec, widthMeasureSpec);
            int width = resolveSize(child.getMeasuredWidth(), widthMeasureSpec);
            child.measure(width, width); // 2nd pass with the correct size
            setMeasuredDimension(width, width);
        }
    }
    */

    /* 本当はコンストラクト時に行いたい操作だが、
     * 画面が最後まで作られないとGridViewの子要素がとれない
     * (GridView.getChildCount() returns zero)ので、
     * 仕方なく適当なイベントを作った。
     */
    private void onPrepared()
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            View v = getScreenAt(i);
            GridView gv = (GridView)v.findViewById(R.id.calendar_days);

            final int cells = gv.getChildCount();

            int width = getNthView(i, 0).getWidth();
            for (int n = 0; n < cells; n++) {
                View cell = getNthView(i, n);
                cell.setMinimumHeight(width);
                setBackgroundColor(cell, 0x00000000); // redraw
            }

            if (cells == 0) continue;

            for (int n = 0; n < (cells - 1) / 7 + 1; n++) { // Sunday
                View dv = getNthView(i, n*7);
                setBackgroundColor(dv, 0xff700000);
            }
            for (int n = 0; n < cells / 7; n++) { // Saturday
                View dv = getNthView(i, n*7+6);
                setBackgroundColor(dv, 0xff000060);
            }
        }

        View today = getTodayView();
        setBackgroundColor(today, cTodayBackground);
        setTextColor((TextView)today.findViewById(R.id.day), cTodayForeground);
    }

    public View getTodayView() {
        return getDayView((new GregorianCalendar()).get(Calendar.DATE));
    }
    public View getDayView(int d) {
        // SUNDAY = 1, SATURDAY = 7
        return getNthView(mCurrentCal.get(Calendar.DAY_OF_WEEK) + d + 7 - 2);
    }
    public View getNthView(int n)
    {
        return getNthView(1, n);
    }
    public View getNthView(int i, int n)
    {
        View v = getScreenAt(i);
        GridView gv = (GridView)v.findViewById(R.id.calendar_days);
        int offset = gv.getFirstVisiblePosition();
        return gv.getChildAt(offset+n);
    }

    public void setOnDayClickListener(final OnDayClickListener listener)
    {
        GridView gv =
            (GridView)getScreenAt(1).findViewById(R.id.calendar_days);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView parent, View v,
                                    int pos, long id)
            {
                Calendar c = mCurrMonth.getCalendar();
                int y = c.get(Calendar.YEAR);
                int m = c.get(Calendar.MONTH) + 1;
                int d = pos - (c.get(Calendar.DAY_OF_WEEK) - 1 + 7) + 1;
                if (d > 0) listener.onDayClick(v, y, m, d);
            }
        });
    }
    public void setOnPageChangedListener(OnPageChangedListener listener)
    {
        mPageChangedListener = listener;
    }

    


    private void initCalendar(Context context)
    {
        mCurrentCal = goMonths(getHeadOfMonth(), -1); // N-1月
        mPrevMonth = new Month(mCurrentCal);

        goNextMonth(mCurrentCal); // N月
        mCurrMonth = new Month(mCurrentCal);

        goNextMonth(mCurrentCal); // N+1月
        mNextMonth = new Month(mCurrentCal);

        goMonths(mCurrentCal, -1); // N月
        
        mPrevPage = new Page(context, mPrevMonth);
        mCurrPage = new Page(context, mCurrMonth);
        mNextPage = new Page(context, mNextMonth);
    }
    private void paging(View newScreen, int newScreenIndex) {
        /* 全体を先月、今月、来月の三頁で構成。
         * 右に移動したら先月を今月で上書き、今月を来月で上書き、来月は新しく作る。
         * 左もまた然り。
         * その後中心のページに戻ることによって、無限頁送りを実現。
         * …のつもりだったが、
         * なぜか戻ったときに頁が表示されなくて困った。
         * 
         * 他の問題点
         * 各頁には色とかつけたいが、
         * Adapterをclearしてaddしたとき、それらが残るかどうかよくわかってない。
         * 今日の日付だけは限定的に色をつけるのだけど、いつ処理するのがよいか。
         *
         * とりあえずコメントアウトして今月と隣り合う月限定にしている。
         */
        /*
        android.util.Log.d("newScreenIndex", "index:"+newScreenIndex);

        if (newScreenIndex == 0) {
            mNextMonth.set(mCurrMonth);
            mCurrMonth.set(mPrevMonth);
            goMonths(mCurrentCal, -2);
            mPrevMonth = new Month(mCurrentCal);
            goMonths(mCurrentCal, 1);

        } else if (newScreenIndex == 2) {
            mPrevMonth.set(mCurrMonth);
            mCurrMonth.set(mNextMonth);
            goMonths(mCurrentCal, 2);
            mNextMonth = new Month(mCurrentCal);
            goMonths(mCurrentCal, -1);
        }

        mPrevPage.set(mPrevMonth);
        mCurrPage.set(mCurrMonth);
        mNextPage.set(mNextMonth);

        setCurrentScreenNow(1, false);
        */
        

        Calendar c = null;
        switch (newScreenIndex) {
        case 0: c = mPrevMonth.getCalendar(); break;
        case 1: c = mCurrMonth.getCalendar(); break;
        case 2: c = mNextMonth.getCalendar(); break;
        }
        if (c == null) return;
        if (mPageChangedListener == null) return;
        if (mPageChangedListener.onPageChanged(getChildAt(newScreenIndex), 
                                               c.get(Calendar.YEAR),
                                               c.get(Calendar.MONTH)+1))
            return; // 頁送り
        else
            return; // 頁送りなし
    }

    class Month
    {
        Calendar mCalendar;
        List<String> mItems;

        public Month(Calendar c) {
            mCalendar = (Calendar)c.clone();
            mItems = new ArrayList<String>();
            setPageItems();
        }
        public void set(Calendar c) {
            mCalendar = (Calendar)c.clone();
            setPageItems();
        }
        public void set(Month m) {
            mCalendar = m.getCalendar();
            mItems = m.getItems();
        }
        public Calendar getCalendar() { return mCalendar; }
        public List<String> getItems() { return mItems; }

        private void setPageItems() {
            Calendar c = (Calendar)mCalendar.clone();

            mItems.clear();

            mItems.add("日"); mItems.add("月"); mItems.add("火");
            mItems.add("水"); mItems.add("木"); mItems.add("金");
            mItems.add("土");

            // skip weekdays
            for (int i = 0; i < c.get(Calendar.DAY_OF_WEEK)-1; i++)
                mItems.add("");

            // make calendar
            mItems.add(toDayString(c.get(Calendar.DATE)));

            for (int d = goNextDay(c); d != 1; d = goNextDay(c))
                mItems.add(toDayString(d));
        }
    }

    class Page
    {
        final ArrayAdapter<String> mAdapter;
        final TextView mTitle;

        public Page(Context context, Month m) {
            View v = View.inflate(context, R.layout.calendar_page, null);
            mTitle = (TextView)v.findViewById(R.id.calendar_title);
            mTitle.setText(toTitleString(m.getCalendar()));

            mAdapter =
                new ArrayAdapter<String>(context, R.layout.calendar_day,
                                         m.getItems());
            GridView gv = (GridView)v.findViewById(R.id.calendar_days);
            gv.setAdapter(mAdapter);

            addView(v);
        }
        public void set(Month m) {
            mTitle.setText(toTitleString(m.getCalendar()));
            /*
            mAdapter.clear();
            mAdapter.addAll(m.getItems());
            */
        }
    }

    private String toTitleString(Calendar c) {
        return "" + c.get(Calendar.YEAR) + "年"
                  + (c.get(Calendar.MONTH) + 1) + "月";
    }
    private String toDayString(int d) {
        return ""+d;
    }
        
    // Calendar Utilities
    private Calendar copyCalendar(Calendar c) {
        return (Calendar)(c.clone());
    }

    private Calendar goDays(Calendar c, int n) {
        c.roll(Calendar.DATE, n);
        return c;
    }
    private int goNextDay(Calendar c) {
        goDays(c, 1);
        return c.get(Calendar.DATE);
    }

    private Calendar goMonths(Calendar c, int n) {
        c.roll(Calendar.MONTH, n);
        return c;
    }
    private int goNextMonth(Calendar c) {
        goMonths(c, 1);
        return c.get(Calendar.MONTH)+1;
    }

    private Calendar getHeadOfMonth() {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.DATE, 1);
        return c;
    }
    private Calendar getHeadOfMonth(int y, int m) {
        return new GregorianCalendar(y, m-1, 1);
    }
}

