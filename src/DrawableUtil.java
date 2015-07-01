package com.akon.mytest;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * Created by Akon-Home on 15/7/1.
 */
public class DrawableUtil {

    private SparseArray<LinkedList<Drawable>> drawableCache = new SparseArray<>(30);
    private final Object mAccessLock = new Object();
    private int mLastCachedXmlBlockIndex = -1;
    private final int[] mCachedXmlBlockIds = { 0, 0, 0, 0 };
    private final XmlBlock[] mCachedXmlBlocks = new XmlBlock[4];
    private TypedValue mTmpValue;

    public Drawable loadDrawable(Context context, int resId){
        int id = context.hashCode();
        Resources rs = context.getResources();
        TypedValue value;
        synchronized (mAccessLock) {
            value = mTmpValue;
            if (value == null) {
                value = new TypedValue();
            } else {
                mTmpValue = null;
            }
            rs.getValue(id, value, true);
        }
        boolean isColorDrawable;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            isColorDrawable = true;
        } else {
            isColorDrawable = false;
        }
        Drawable dr;
        if (isColorDrawable) {
            dr = new ColorDrawable(value.data);
        } else {
            if (value.string == null) {
                throw new Resources.NotFoundException("Resource \""
                        + rs.getResourceName(id)
                        + "\" (" + Integer.toHexString(id) + ")  is not a Drawable (color or path): " + value);
            }
            final String file = value.string.toString();
            try {
                if (file.endsWith(".xml")) {
                    XmlResourceParser rp = rs.getAssets().openXmlResourceParser(value.assetCookie, file);
                    dr = createFromXml(rs, rp);
                    rp.close();
                } else {
                    InputStream is = rs.openRawResource(resId, value);
                    dr = Drawable.createFromResourceStream(rs, value, is, file, null);
                    is.close();
                }
            } catch (Exception e) {
                final Resources.NotFoundException rnf = new Resources.NotFoundException(
                        "File " + file + " from drawable resource ID #0x" + Integer.toHexString(id));
                rnf.initCause(e);
                throw rnf;
            }
        }
        synchronized (mAccessLock) {
            if (mTmpValue == null) {
                mTmpValue = value;
            }
        }
        return dr;
    }


    private static Drawable createFromXml(Resources rs, XmlResourceParser parser) throws IOException, XmlPullParserException {
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        Drawable drawable = createFromXmlInner(rs, parser, attrs);

        if (drawable == null) {
            throw new RuntimeException("Unknown initial tag: " + parser.getName());
        }

        return drawable;
    }

    public static Drawable createFromXmlInner(Resources r,
                                              XmlPullParser parser,
                                              AttributeSet attrs) throws XmlPullParserException, IOException {
        final Drawable drawable;

        final String name = parser.getName();
        switch (name) {
            case "selector":
                drawable = new StateListDrawable();
                break;
            case "level-list":
                drawable = new LevelListDrawable();
                break;
            case "layer-list":
                drawable = new LayerDrawable(null);
                break;
            case "color":
                drawable = new ColorDrawable();
                break;
            case "animation-list":
                drawable = new AnimationDrawable();
                break;
            case "bitmap":
                drawable = new BitmapDrawable(r);
                if (r != null) {
                    ((BitmapDrawable) drawable).setTargetDensity(r.getDisplayMetrics());
                }
                break;
            case "nine-patch":
                drawable = new NinePatchDrawable(null);
                if (r != null) {
                    ((NinePatchDrawable) drawable).setTargetDensity(r.getDisplayMetrics());
                }
                break;
            default:
                throw new XmlPullParserException(parser.getPositionDescription() +
                        ": invalid drawable tag " + name);

        }
        drawable.inflate(r, parser, attrs);
        return drawable;
    }


}
