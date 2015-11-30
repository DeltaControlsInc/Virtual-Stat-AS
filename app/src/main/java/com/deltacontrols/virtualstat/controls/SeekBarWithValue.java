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
 * SeekBarWithValue.java
 */
package com.deltacontrols.virtualstat.controls;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;

/**
 * SeekBarWithValue is a custom control that contains both a SeekBar and a display for the current seek value.
 */
public class SeekBarWithValue extends LinearLayout {

    // --------------------------------------------------------------------------------
    // Listener interface
    // --------------------------------------------------------------------------------
    public static interface ChangeListener {
        public void onValueChange(int value);
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    private Context mCtx;               // Control context
    private TextView mValueText;        // Outlet for text
    private SeekBar mSeekBar;           // Outlet for seekbar
    private ChangeListener mListener;   // Calling listener
    private boolean mIsEnabled;         // Current state of control
    private boolean mDoingUpdateHack;   // State of progress update hack (see setEnabled for details)
    private final int mActiveTextColor = Color.parseColor("#666666");
    private final int mDiabledTextColor = Color.parseColor("#a3a3a3");

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public SeekBarWithValue(Context ctx, ChangeListener listener, float textSize) {
        super(ctx);

        mListener = listener;
        mCtx = ctx;
        mIsEnabled = true;
        mDoingUpdateHack = false;

        // Main linear layout; will contain the slider and and value
        RelativeLayout.LayoutParams innerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(innerLayoutParams);
        this.setOrientation(LinearLayout.HORIZONTAL);
        this.setBackgroundResource(R.drawable.lights_off_button_shape);

        // Value textview
        mValueText = new TextView(mCtx);
        LinearLayout.LayoutParams inputValueParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 2.5f);
        inputValueParams.setMargins(0, 0, 0, 0);
        mValueText.setLayoutParams(inputValueParams);
        mValueText.setTextColor(mActiveTextColor);
        mValueText.setTextSize(textSize);
        mValueText.setGravity(Gravity.CENTER);
        mValueText.setText("0%");

        // "SeekBar" slider input
        int padding = (int) UIFactory.getDPDimen(mCtx, R.dimen.value_slider_padding);

        mSeekBar = new SeekBar(mCtx);
        mSeekBar.setMax(100);
        mSeekBar.setSecondaryProgress(0);
        mSeekBar.setProgressDrawable(mCtx.getResources().getDrawable(R.drawable.lighting_seek_bar));

        mSeekBar.setThumb(mCtx.getResources().getDrawable(R.drawable.selector_seek_thumb));
        mSeekBar.setPadding(55, padding, 30, padding);
        LinearLayout.LayoutParams seekBarParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
        mSeekBar.setLayoutParams(seekBarParams);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                if ((mListener != null) && mIsEnabled && !mDoingUpdateHack) {
                    LinearLayout parentLayout = (LinearLayout) seekBar.getParent();

                    // Update background
                    if (progress == 0) {
                        parentLayout.setBackgroundResource(R.drawable.lights_off_button_shape);
                    } 
                    else {
                        parentLayout.setBackgroundResource(R.drawable.lights_on_button_shape);
                    }

                    // Update the value text
                    String newValue = Integer.toString(progress);
                    mValueText.setText(newValue + "%");

                    // Call client listener
                    mListener.onValueChange(progress);
                }
            }
        });

        mSeekBar.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return !mIsEnabled; // Consume event (return true) if not enabled.
            }
        });

        // Add value and seekbar to main layout
        this.addView(mSeekBar);
        this.addView(mValueText);
    }

    // --------------------------------------------------------------------------------
    // Overridden functionality
    // --------------------------------------------------------------------------------
    /**
     * Enables / Disables the control (both visually and by interaction)
     */
    @Override
    public void setEnabled(boolean set) {
        // Note, does not call parent setEnabled; problems occurred when doing so.

        if (mIsEnabled == set) {
            return;
        }
        mIsEnabled = set;

        // Update text
        int textColor = (mIsEnabled) ? mActiveTextColor : mDiabledTextColor;
        mValueText.setTextColor(textColor);

        // Update seekbar
        int drawable = (mIsEnabled) ? R.drawable.lighting_seek_bar : R.drawable.lighting_seek_bar_disabled;
        Rect bounds = mSeekBar.getProgressDrawable().getBounds();
        mSeekBar.setProgressDrawable(mCtx.getResources().getDrawable(drawable));
        mSeekBar.getProgressDrawable().setBounds(bounds); // Must reset bounds or progress bar will have a size of 0.

        // Note, changing the progress bar drawable does NOT force the progress to be redrawn (so the changed bar color is not
        // painted automatically). All attempts to call setIndeterminate, refreshDrawableState, postInvalidate or forceLayout did not force the redraw.
        // To get around this we need to manually change the progress VALUE (to force the progress to draw), however, we do not want the
        // value to show this. Use mDoingUpdateHack in onProgressChanged to indicate that we do NOT want to update the visible value.
        mDoingUpdateHack = true;
        int progress = mSeekBar.getProgress();
        mSeekBar.setProgress(0);
        mSeekBar.setProgress(progress);
        mDoingUpdateHack = false;
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Puts the control into 'error', which will disable the control and show ?? as the error.
     */
    public void setError() {
        this.setError("??");
    }

    public void setError(String text) {
        this.setEnabled(false);
        mSeekBar.setEnabled(false);
        mSeekBar.setProgress(0);
        mValueText.setText(text);
        ((LinearLayout) mSeekBar.getParent()).setBackgroundResource(R.drawable.lights_off_button_shape);
    }

    /**
     * Enables the control and sets the progress for the control
     */
    public void setValue(int value) {
        this.setEnabled(true);
        mSeekBar.setEnabled(true);
        mSeekBar.setProgress(value);
        mValueText.setText(value + "%");
    }
}
