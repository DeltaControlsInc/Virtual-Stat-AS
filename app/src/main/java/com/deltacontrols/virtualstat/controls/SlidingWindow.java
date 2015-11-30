/* Copyright (c) 2014, Delta Controls Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this 
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this 
list of conditions and the following disclaimer in the documentation and/or other 
materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may 
be used to endorse or promote products derived from this software without specific 
prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
*/
/**
 * SlidingWindow.java
 */

package com.deltacontrols.virtualstat.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.deltacontrols.virtualstat.R;

/**
 * Custom control that slides one image over a background color via user gestures. Currently used for the blinds control, but could be extended in the future.
 */
public class SlidingWindow extends View {

    // --------------------------------------------------------------------------------
    // Listener interfaces
    // --------------------------------------------------------------------------------
    public interface SlidingWindowViewListener {
        public void onValueChange(int value); // Callback when values changes
    }

    private SlidingWindowViewListener listener = null;

    public void setListener(SlidingWindowViewListener l) {
        listener = l;
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    private Paint mPaint;           // For drawing background
    private Context mCtx;           // Current context
    private int mHeight, mWidth;    // Current size of control
    private int mIncrement = 10;    // Slide value increment. Note, must be in increments of 10 for the background image to match up with the sliding image

    private Bitmap blindsBitmap;    // Sliding image (blinds)
    private Bitmap topBitmap;       // Top stationary image (valance)
    private int mTopHeight;         // The sliding image height 'offset' (to account for the topBitmap)

    private int mValue;             // 0 - 100 (%); will determine where the sliding blinds bitmap will be located when onDraw is called.

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public SlidingWindow(Context context) {
        super(context);
        init(context);
    }

    public SlidingWindow(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get custom attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SlidingWindow, 0, 0);
        try {
            mValue = a.getInt(R.styleable.SlidingWindow_value, 0);
        } finally {
            a.recycle();
        }

        init(context);
    }

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    /**
     * When size changed, recalculate offsets and then reset the bitmap sizes to match.
     */
    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        mWidth = xNew;
        mHeight = yNew;
        mTopHeight = (int) (mHeight * 0.1); // Start 10% 'down'
        initBlindsBitmap();
    }

    /**
     * Draw the background and then translate the sliding image to match the current control value (between 0 and 100). 
     * At 0 the sliding image has been translated completely up (such that it is not seen) At 100 the sliding image has 
     * been translated completely down (such that it is fully seen)
     */
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Find Split height
        float newSplit = (mValue * (mHeight - mTopHeight) / 100);
        newSplit = newSplit + mTopHeight;

        // Background
        mPaint.setColor(getResources().getColor(R.color.DeltaBrightRed));
        canvas.drawRect(0, 0, mWidth, mHeight, mPaint);

        // Background semi-transparent indicator
        mPaint.setAlpha(70);
        canvas.drawBitmap(blindsBitmap, 0, mTopHeight, mPaint);

        // Blinds slider
        mPaint.setAlpha(255);
        canvas.drawBitmap(blindsBitmap, 0, -mHeight + mTopHeight + newSplit, mPaint);

        // Top bar
        canvas.drawBitmap(topBitmap, 0, 0, mPaint);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Sets property defaults
     * 
     * @param context
     */
    public void init(Context context) {
        mCtx = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * Scales bitmaps according to the current control width/height
     */
    private void initBlindsBitmap() {
        Bitmap originalBitmap = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.blinds_only);
        Bitmap originalTopBitmap = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.blinds_top);
        topBitmap = Bitmap.createScaledBitmap(originalTopBitmap, (int) mWidth, (int) mTopHeight, true);
        blindsBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) mWidth, (int) (mHeight - mTopHeight), true);
    }

    /**
     * Returns the current value / position of the bottom of the image.
     * 
     * @return Value between 0 and 100
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Sets the value (forces value between 0 and 100); fires onValueChange if values has changed.
     * 
     * @param newValue
     */
    public void setValue(int newValue) {
        // Set newValue
        newValue = Math.max(0, newValue);
        newValue = Math.min(newValue, 100);

        // Adhere to increments
        newValue = (int) (Math.floor(newValue / mIncrement) * mIncrement);

        if (newValue != mValue) {
            mValue = newValue;

            // Redraw
            invalidate();
            requestLayout();

            if (listener != null) {
                listener.onValueChange(mValue);
            }
        }
    }

    // --------------------------------------------------------------------------------
    // Overriden methods
    // --------------------------------------------------------------------------------
    /**
     * Listen for touch events, translate the touch location and set the value to the corresponding value (0 to 100). Note: Does not restrict increment here (done in setValue)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float y = event.getY();

        // Translate point into value
        int newValue = (int) (((y - mTopHeight) / (mHeight - mTopHeight)) * 100);
        newValue = Math.max(newValue, 0);

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                setValue(newValue);
                break;
            case MotionEvent.ACTION_DOWN:
                setValue(newValue);
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }

        return true;
    }
}
