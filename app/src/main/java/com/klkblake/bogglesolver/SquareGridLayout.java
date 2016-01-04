package com.klkblake.bogglesolver;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by kyle on 4/01/16.
 */
public class SquareGridLayout extends android.support.v7.widget.GridLayout {

    public SquareGridLayout(Context context) {
        super(context);
    }

    public SquareGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = max(getMeasuredWidth(), getMeasuredHeight());
        if (widthMode != UNSPECIFIED) {
            size = min(size, widthSize);
        }
        if (heightMode != UNSPECIFIED) {
            size = min(size, heightSize);
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(size, EXACTLY), MeasureSpec.makeMeasureSpec(size, EXACTLY));
    }
}
