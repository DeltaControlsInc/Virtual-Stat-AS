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
 * StackedStates.java
 */

package com.deltacontrols.virtualstat.controls;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;

/**
 * Custom control for selecting one of many given 'states', similar to a RadioGroup; states are stacked vertically and the caller 
 * can listen for the onStateSelected event to indicate when a change has been made.
 */
public class StackedStates extends RadioGroup {

    // --------------------------------------------------------------------------------
    // Listener interfaces
    // --------------------------------------------------------------------------------
    public static interface StatesGroupListener {
        public void onStateSelected(String result);
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    private Context mCtx;
    private StatesGroupListener mListener;          // Calling listener
    private LinkedHashMap<String, String> mStates;  // Expecting: [Key: state string][Value: state value] (ValueByState)
    private LinkedHashMap<String, RadioButton> mButtons; // Mapping of state VALUE to the given button.
    private float mTextSize;                        // Button text size (could make attribute instead)

    private boolean mIsEnabled;                     // Flag for if the control is enabled or not.
    private Integer mCheckedState;                  // Stores the currently selected/checked state

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public StackedStates(Context context) {
        super(context);
        mCtx = context;
        init();
    }

    public StackedStates(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCtx = context;
        init();
    }

    public StackedStates(Context context,
            LinkedHashMap<String, String> states,
            float textSize,
            StatesGroupListener listener) {
        super(context);
        mCtx = context;
        mStates = states;
        mTextSize = textSize;
        mListener = listener;
        init();
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Attaches a listener to the RadioGroup, and creates a RadioButton for each state in mStates. 
     * Note: Because the states are stacked in a linear group vertically, the first state will appear 'on top'; therefore, 
     * the order the states are put in the given array matter.
     */
    private void init() {
        mIsEnabled = true;
        mCheckedState = null;

        mButtons = new LinkedHashMap<String, RadioButton>();
        LinearLayout.LayoutParams innerLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(innerLayoutParams);
        this.setOrientation(RadioGroup.VERTICAL);
        this.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String result;

                // Clearing group
                if (checkedId == -1) {
                    result = null;
                    mCheckedState = null;
                }
                else {
                    RadioButton btn = (RadioButton) group.findViewById(checkedId);

                    // Android bug: RadioGroup.OnCheckedChangeListener is called twice when the selection is cleared
                    // https://code.google.com/p/android/issues/detail?id=4785
                    // http://stackoverflow.com/questions/4519103/error-in-androids-clearcheck-for-radiogroup
                    // If the button is currently not checked, then ignore this event.
                    if (!btn.isChecked()) {
                        return;
                    }

                    String mapKey = btn.getText().toString(); // Get state STRING (ie. III not 2).
                    result = mStates.get(mapKey); // Get the VALUE based on the state string.
                    mCheckedState = Integer.parseInt(result); // Store the checked state VALUE for later (reload state etc)
                }

                updateButtons();

                if (mListener != null) {
                    mListener.onStateSelected(result);
                }
            }
        });

        RadioButton button;
        RadioGroup.LayoutParams buttonLayout;

        for (Map.Entry<String, String> entry : mStates.entrySet()) {
            buttonLayout = new RadioGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
            buttonLayout.setMargins(0, 0, 0, 10);

            // Create custom RadioButtons
            button = new RadioButton(mCtx);
            button.setLayoutParams(buttonLayout);
            button.setButtonDrawable(new StateListDrawable()); // null does not work, use empty statelist instead
            button.setBackgroundResource(R.drawable.shape_state_off);
            button.setTextAppearance(mCtx, R.style.delta_tabSingleValueText);
            button.setTextSize(mTextSize);
            button.setTextColor(getResources().getColor(R.color.DeltaMidGrey));
            button.setGravity(Gravity.CENTER);
            button.setText(entry.getKey()); // State STRING (not value)
            button.setPadding(10, 10, 10, 10);
            mButtons.put(entry.getValue(), button); // Map state VALUE (not string) to the radio button
            this.addView(button);
        }
    }

    /**
     * Update the visual look of the buttons based on if they are selected and if the control is enabled or disabled. 
     * If enabled, the selected button should appear highlighted red; if disabled, the highlighted button should appear highlighted in grey.
     */
    private void updateButtons() {

        Integer buttonState;
        int buttonResourceSelected, buttonResourceUnselected;
        int buttonTextColorSelected, buttonTextColorUnselected;

        if (mIsEnabled) {
            buttonResourceSelected = R.drawable.shape_state_on;
            buttonResourceUnselected = R.drawable.shape_state_off;
            buttonTextColorSelected = R.color.DeltaTabText;
            buttonTextColorUnselected = R.color.DeltaMidGrey;
        }
        else {
            buttonResourceSelected = R.drawable.lights_off_button_shape;
            buttonResourceUnselected = R.drawable.shape_state_off;
            buttonTextColorSelected = R.color.DeltaMidGrey;
            buttonTextColorUnselected = R.color.DeltaMidGrey;
        }

        for (Map.Entry<String, RadioButton> button : mButtons.entrySet()) {

            buttonState = Integer.parseInt(button.getKey());

            if (buttonState == mCheckedState) {
                button.getValue().setBackgroundResource(buttonResourceSelected);
                button.getValue().setTextColor(getResources().getColor(buttonTextColorSelected));
            }
            else {
                button.getValue().setBackgroundResource(buttonResourceUnselected);
                button.getValue().setTextColor(getResources().getColor(buttonTextColorUnselected));
            }
        }
    }

    /**
     * Given a state VALUE (not state string), select that state in the group.
     * 
     * @param state Value to select
     */
    public void selectState(String state) {
        try {
            mButtons.get(state).setChecked(true);
        } 
        catch (Exception e) {
            Log.e(App.TAG, "Error getting state: " + e.getMessage());
        }
    }

    public boolean hasSelection() {
        return mCheckedState != null;
    }

    /**
     * Not implemented yet, implement to bring in line with other custom controls.
     */
    public void setError() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not implemented yet, implement to bring in line with other custom controls. Could be similar to selectState?
     * 
     * @param value
     */
    public void setValue(String value) {
        throw new UnsupportedOperationException();
    }

    // --------------------------------------------------------------------------------
    // Overridden functionality
    // --------------------------------------------------------------------------------
    /**
     * Enables / Disables the control (both visually and by interaction)
     */
    @Override
    public void setEnabled(boolean set) {

        if (set == mIsEnabled) {
            return;
        }
        mIsEnabled = set;
        updateButtons();

        // For each button in the group, call its parent setEnabled method
        for (Map.Entry<String, RadioButton> button : mButtons.entrySet()) {
            (button.getValue()).setEnabled(set);
        }
    }
}