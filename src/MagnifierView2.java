package com.akon.mytest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by Akon-Home on 15/7/15.
 */
public class MagnifierView2 extends RelativeLayout{

    private float lastX, lastY;

    private int zWidth = 300, zHeight = 180;

    private Canvas cacheCanvas;
    private Bitmap cacheBitmap;
    private Paint paint = new Paint();

    private WindowManager mWindowManager;
    private ImageView zoomFloatView;
    private OnZoomUpdatedListener onZoomUpdatedListener;

    private boolean isShow = false;
    private WindowManager.LayoutParams smallWindowParams;
    private int[] location = new int[2];

    public MagnifierView2(Context context) {
        this(context, null);
    }

    public MagnifierView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setOnZoomUpdatedListener(new OnZoomUpdatedListener() {
            @Override
            public void onZoomUpdated(int x, int y, Bitmap bmp) {
                addFloatView();
                zoomFloatView.setImageBitmap(bmp);
                Rect dest = getZoomRect(1);
                updateFloatView(dest.left, dest.top);
            }
        });
    }

    private void addFloatView(){
        if (zoomFloatView == null) {
            zoomFloatView = new ImageView(getContext());
            mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

            if (smallWindowParams == null) {
                smallWindowParams = new WindowManager.LayoutParams();
                smallWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                smallWindowParams.format = PixelFormat.RGBA_8888;
                smallWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                smallWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
                smallWindowParams.width = zWidth;
                smallWindowParams.height = zHeight;
                smallWindowParams.x = 0;
                smallWindowParams.y = 0;
            }
        }
        if(!isShow) {
            mWindowManager.addView(zoomFloatView, smallWindowParams);
            isShow = true;
        }
    }

    private void updateFloatView(int x, int y){
        smallWindowParams.x = location[0] + x;
        smallWindowParams.y = location[1] + y;
        mWindowManager.updateViewLayout(zoomFloatView, smallWindowParams);
    }

    private void removeFloatView(){
        if(isShow) {
            mWindowManager.removeView(zoomFloatView);
            isShow = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (cacheBitmap != null) {
            cacheBitmap.recycle();
            cacheBitmap = null;
        }
        removeFloatView();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        initShaderDrawable();
        lastX = event.getX();
        lastY = event.getY();
        if(event.getAction() == MotionEvent.ACTION_CANCEL){
            removeFloatView();
        }else{
            makeZoom();
        }
        return super.dispatchTouchEvent(event);
    }

    private void makeZoom(){
        setDrawingCacheEnabled(true);
        Bitmap bmp = getDrawingCache();
        if (bmp != null) {
            cacheCanvas.drawBitmap(bmp, getZoomRect(2), new Rect(0,0,zWidth,zHeight), null);
            if (onZoomUpdatedListener != null) {
                onZoomUpdatedListener.onZoomUpdated((int) lastX, (int) lastY, cacheBitmap);
            }
        }
        setDrawingCacheEnabled(false);
    }

    private Rect getZoomRect(int dx){
        Rect src = new Rect();

        int margin = (int)lastX - zWidth / (dx*2);
        src.left = margin;
        if (margin <= 0) {
            src.left = 0;
        } else if (margin >= getWidth() - zWidth/dx) {
            src.left = getWidth() - zWidth/dx;
        }

        margin = (int)lastY - zHeight / (dx*2);
        src.top = margin;
        if (margin <= 0) {
            src.top = 0;
        } else if (margin >= getHeight() - zHeight/dx) {
            src.top = getHeight() - zHeight/dx;
        }
        src.right = src.left + zWidth/dx;
        src.bottom = src.top + zHeight/dx;

        return src;
    }

    public void setOnZoomUpdatedListener(OnZoomUpdatedListener listener) {
        this.onZoomUpdatedListener = listener;
    }

    private void initShaderDrawable() {
        if (cacheBitmap == null && onZoomUpdatedListener != null) {
            cacheBitmap = Bitmap.createBitmap(zWidth, zHeight, Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas();
            cacheCanvas.setBitmap(cacheBitmap);
            paint.setColor(0xFFFFFFFF);
            getLocationInWindow(location);
        }
    }

    public static interface OnZoomUpdatedListener {
        void onZoomUpdated(int x, int y, Bitmap drawable);
    }
}
