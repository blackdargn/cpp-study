package com.akon.mytest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 触摸区域面板
 * Created by Akon-Home on 15/6/21.
 */
public class TouchAreaPanel extends View implements Runnable{

    private static final String TAG = "TouchAreaPanel";
    private List<TouchArea> mAreas;
    private HashMap<Integer, TouchArea> mAreasMap;
    private OnAreaClickListener onAreaClickListener;

    final private RectF mTouchClipRect = new RectF();

    private boolean isUnSelected = false;
    private List<RectF> mSelectClipRects = new ArrayList<>(12);

    private float[] point = new float[2];
    private TouchArea touchArea;

    private int flashCount, flashTime;
    private List<Integer> flashIds = new ArrayList<>(12);

    public TouchAreaPanel(Context context) {
        super(context, null);
    }

    public TouchAreaPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mTouchClipRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // View中的相对坐标, 只纪录第一次按下的状态
                point[0] = (int) event.getX();
                point[1] = (int) event.getY();
                Log.d(TAG, "xy = " + point[0] + "," + point[1]);

                touchArea = findByXY(point);
                if (touchArea != null) {
                    touchArea.setPressed(true);
                    mTouchClipRect.set(touchArea.mRect);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (touchArea != null) {
                    touchArea.setPressed(false);
                    invalidate();
                    if (onAreaClickListener != null) {
                        onAreaClickListener.onAreaClick(touchArea.id);
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        this.onDraw1(canvas);
        if (false) {
            super.draw(canvas);
        }
    }

    protected void onDraw1(Canvas canvas) {
        drawAreas(canvas);
        if (isUnSelected) {
            mSelectClipRects.clear();
            isUnSelected = false;
        }
    }

    private void drawAreas(Canvas canvas) {
        drawAreasRect(canvas, mTouchClipRect);
        for (RectF one : mSelectClipRects) {
            drawAreasRect(canvas, one);
        }
    }

    private void drawAreasRect(Canvas canvas, RectF clipRect) {
        if (clipRect.isEmpty()) return;
        canvas.save();
        canvas.clipRect(clipRect);
        Log.d(TAG, clipRect.toString());
        if (mAreas != null) {
            for (TouchArea one : mAreas) {
                if (clipRect.contains(one.mRect)) {
                    one.draw(canvas);
                }
            }
        }
        canvas.restore();
    }

    public void setOnAreaClickListener(OnAreaClickListener listener) {
        this.onAreaClickListener = listener;
    }

    /** 初始化设置 */
    public void setData(List<TouchArea> list) {
        this.mAreas = list;
        if (mAreasMap != null) {
            mAreasMap.clear();
        } else {
            mAreasMap = new HashMap<>(list != null ? list.size() : 0);
        }
        if (mAreas != null) {
            for (TouchArea one : mAreas) {
                mAreasMap.put(one.id, one);
            }
        }
        invalidate();
    }

    public void setSelected(boolean selected, int[] ids) {
        List<Integer> list = new ArrayList<>(ids.length);
        for(int id : ids){
            list.add(id);
        }
        setSelected(selected, list);
    }
    /** 设置区域选择状态 */
    public void setSelected(boolean selected, List<Integer> ids) {
        if (mAreasMap != null && !mAreasMap.isEmpty()) {
            boolean dirty = false;
            TouchArea one;
            mSelectClipRects.clear();

            for (int id : ids) {
                one = mAreasMap.get(id);
                if (one != null) {
                    one.setSelected(selected);
                    mSelectClipRects.add(one.mRect);
                    dirty = true;
                }
            }
            if (dirty) {
                isUnSelected = !selected;
                mTouchClipRect.set(0, 0, 0, 0);
                invalidate();
            }
        }
    }

    public void flash(int time, int count, int ... ids){
        if(time <= 0 || count <= 0 || ids == null || ids.length == 0){
            return;
        }
        this.flashIds.clear();
        for(int id : ids){
            this.flashIds.add(id);
        }
        this.flashCount = count;
        this.flashTime = time;
        setSelected(flashCount % 2 == 0, flashIds);
        postDelayed(this, time);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this);
    }

    /** 由xy找到对应区域 */
    private TouchArea findByXY(float[] point) {
        if (mAreas != null) {
            for (TouchArea one : mAreas) {
                if (one.mRect.contains(point[0], point[1])) {
                    return one;
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        --flashCount;
        if(flashCount <= 0){
            setSelected(false,flashIds);
            flashIds.clear();
        }else{
            setSelected(flashCount % 2 == 0, flashIds);
            postDelayed(this, flashTime);
        }
    }

    public static interface OnAreaClickListener {
        void onAreaClick(int id);
    }
}
