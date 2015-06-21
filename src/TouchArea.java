package com.akon.mytest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class TouchArea {
    public static int STATE_NORMAL = 2;
    public static int STATE_PRESSED = 0;
    public static int STATE_SELECTED = 1;
    /** 位置 */
    final RectF mRect = new RectF();
    /** 贴图 0:pressed state; 1: selected state */
    Bitmap[] areaDrawable = new Bitmap[2];
    /** 状态 */
    int state = STATE_NORMAL;
    /** id */
    int id;

    Paint paint;

    public TouchArea(int id) {
        this.id = id;
        paint = new Paint();
        paint.setColor(0x00000000);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setPressed(boolean pressed) {
        state = pressed ? STATE_PRESSED : STATE_NORMAL;
    }

    public void setSelected(boolean selected) {
        state = selected ? STATE_SELECTED : STATE_NORMAL;
    }

    public void setRect(float left, float top, float width, float height) {
        mRect.set(left, top, left + width, top + height);
    }

    /** after setRect call*/
    public void setBackImageRes(Bitmap pressed, Bitmap selected) {
        this.areaDrawable[0] = pressed;
        this.areaDrawable[1] = selected;
    }

    public void draw(Canvas canvas) {
        if(state == STATE_NORMAL){
            canvas.drawRect(mRect, paint);
        }else {
            canvas.drawBitmap(areaDrawable[state], null, mRect, null);
        }
        Log.d("draw area ", "" + id);
    }
}