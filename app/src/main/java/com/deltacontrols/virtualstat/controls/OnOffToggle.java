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
package com.deltacontrols.virtualstat.controls;

import android.content.Context;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;

/**
 * Custom control consisting of a "sticky" button and overlaid value text.
 */
public class OnOffToggle extends RelativeLayout {

    // --------------------------------------------------------------------------------
    // Listener interface
    // --------------------------------------------------------------------------------
    public static interface ToggleListener {
        public void onValueChange(String result);
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    private TextView mToggleText;       // Reference to text
    private CheckBox mToggleCheckbox;   // Reference to checkbox (sticky button)
    private ToggleListener mListener;   // Calling listener
    private String mOnText, mOffText, mDefaultText; // Text for states of toggle

    public OnOffToggle(Context ctx, ToggleListener listener, float textSize, String onText, String offText, String defaultText) {
        super(ctx);

        // Set properties
        mOnText = onText;
        mOffText = offText;
        mDefaultText = defaultText;
        mListener = listener;

        // Set layout default height; NOTE: could remove and pass in instead.
        int height = (int) UIFactory.getDPDimen(ctx, R.dimen.value_slider_height_estimation);
        RelativeLayout.LayoutParams innerLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, height); // Approx height of seekbar
        this.setLayoutParams(innerLayoutParams);

        // Create Value Label
        mToggleText = new TextView(ctx);
        RelativeLayout.LayoutParams inputValueParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mToggleText.setLayoutParams(inputValueParams);
        mToggleText.setTextAppearance(ctx, R.style.delta_tabSingleValueText);
        mToggleText.setTextSize(textSize);
        mToggleText.setGravity(Gravity.CENTER);
        mToggleText.setText(mDefaultText);

        // Create "checkbox" input
        mToggleCheckbox = new CheckBox(ctx);
        RelativeLayout.LayoutParams toggleParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mToggleCheckbox.setLayoutParams(toggleParams);
        mToggleCheckbox.setButtonDrawable(ctx.getResources().getDrawable(R.drawable.lights_on_off_selector));
        mToggleCheckbox.setFocusable(false);
        mToggleCheckbox.setFocusableInTouchMode(false);
        mToggleCheckbox.setBackgroundResource(R.drawable.lights_on_off_selector);
        mToggleCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Dealing with binary input
                String state, value;

                // Update value outlet
                if (isChecked) {
                    state = mOnText;
                    value = VirtualStatPoint.ActiveString;
                }
                else {
                    state = mOffText;
                    value = VirtualStatPoint.InactiveString;
                }

                mToggleText.setText(state);

                if (mListener != null) {
                    mListener.onValueChange(value);
                }
            }
        });

        // Add views
        this.addView(mToggleCheckbox);
        this.addView(mToggleText);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Puts the control into 'error', which will disable the control and show ?? as the error.
     */
    public void setError() {
        mToggleCheckbox.setEnabled(false);
        mToggleText.setText("??");
    }

    /**
     * Enables the control, sets the text and checkbox state.
     */
    public void setValue(String value) {
        mToggleCheckbox.setEnabled(true);
        boolean isActive = value.equals(VirtualStatPoint.ActiveString);
        mToggleText.setText(isActive ? mOnText : mOffText);
        mToggleCheckbox.setChecked(isActive);
    }
}
