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
 * SingleStatControlFan.java
 */
package com.deltacontrols.virtualstat.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.controls.SeekBarWithValue;
import com.deltacontrols.virtualstat.controls.StackedStates;
import com.deltacontrols.virtualstat.points.FanPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint.SetBy;

/**
 * Fragment containing the fan point; uses virtualStatDelegate (from VirtualStat.UseVirtualStat.getStatDelegate) to interact with the current VirtalStat data points.
 */
public class SingleStatControlFan extends Fragment implements VirtualStat.UseVirtualStat {

    // Common support
    private View activeInput; // Reference to the active input (SeekBarWithValue or StackedStates)
    private CheckBox autoManualCheckbox;

    // VirtualStat (model) Interface delegates
    private VirtualStat virtualStatDelegate = null;
    private boolean isReady;
    private boolean suppressSounds = false;
    private VirtualStatPoint.Type mPointType;
    private final String autoTextForSeek = "";

    // Input Listeners
    private SeekBarWithValue.ChangeListener valueSliderListener = new SeekBarWithValue.ChangeListener() {
        @Override
        public void onValueChange(int value) {
            // Analog value
            String newValue = Integer.toString(value);

            // Actual and display value may be different, only update if we need to.
            if (convertDelegateToDisplay() != value) {
                virtualStatDelegate.setValue(virtualStatDelegate.Fan, newValue);
                playClick();
            }
        }
    };

    private StackedStates.StatesGroupListener statesGroupListener = new StackedStates.StatesGroupListener() {
        @Override
        public void onStateSelected(String result) {
            // If non-auto (null) state selected, then we want to toggle the auto/manual checkbox if has not
            // already been toggled.
            if (result == null) {
                if (autoManualCheckbox.isChecked()) {
                    autoManualCheckbox.setChecked(false);
                }
                result = "1"; // MV states indexed on 1, so by having no states selected we are in auto mode, which corresponds to a value of 1.
            }
            else {
                if (!autoManualCheckbox.isChecked()) {
                    autoManualCheckbox.setChecked(true);
                }
            }

            virtualStatDelegate.setValue(virtualStatDelegate.Fan, result);
            playClick();
        }
    };

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    public SingleStatControlFan() {
        // Required empty public constructor
    }

    /**
     * Creates the view for the fan fragment; if the point is not valid, then show an error view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_single_stat_control_fan, container, false);
        View resultView = null;

        suppressSounds = true; // Suppress sound during setup to avoid undesired "clicks"

        // Set the stat delegate
        getStatDelegate();
        mPointType = virtualStatDelegate.Fan.getType();

        // Check for errors

        if (!virtualStatDelegate.Fan.isValid()) {
            final View errorView = inflater.inflate(R.layout.fragment_single_stat_error, container, false);
            isReady = false;
            resultView = errorView;
        }
        else {
            // Setup outlets
            RelativeLayout fanInputLayout = (RelativeLayout) view.findViewById(R.id.fanInputLayout);
            createInputControls(fanInputLayout);

            // Setup auto manual
            autoManualCheckbox = (CheckBox) view.findViewById(R.id.autoManualCheckbox);
            autoManualCheckbox.setChecked(virtualStatDelegate.Fan.getManualState()); // Set manual state before changing
            autoManualCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (activeInput == null) { 
                        return;
                    }

                    // If using an MV, State1 = auto. When switching to manual default to State2.
                    if (mPointType == VirtualStatPoint.Type.MULTISTATE) {
                        StackedStates states = (StackedStates) activeInput;
                        if (!isChecked) {
                            states.clearCheck();
                        }
                        else if (!states.hasSelection()) {
                            // If no other state has been selected (ie. the user toggled the a/m checkbox) then
                            // set a default state for them.
                            states.selectState("2");
                        }
                        // No click, state change will apply a click for us
                    }

                    else {
                        SeekBarWithValue bar = ((SeekBarWithValue) activeInput);
                        if (isChecked) {
                            bar.setValue(convertDelegateToDisplay());
                        }
                        else {
                            bar.setError(autoTextForSeek);
                        }
                        // Play sound, since not changing the value of the slider, it will not provide the click.
                        playClick();
                    }

                    // Note, we are not going through VirtualStat.setValue (because Fan does trickery for setting the manual state),
                    // We need to force a put to the server.
                    virtualStatDelegate.Fan.setManualState(isChecked, SetBy.USER);
                    virtualStatDelegate.forcePut();
                }
            });

            isReady = true;
            resultView = view;
        }

        // Set custom font
        UIFactory.setCustomFont(getActivity(), resultView);
        return resultView;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onResume() {
        // Suppress sounds during resume to avoid undesired clicks
        suppressSounds = true;
        super.onResume();
        updateWithDelegate();
        suppressSounds = false;
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Dynamically generate the fan input based on {@link VirtualStatPoint.Type}. 
     * Analog value -> SeekBarWithValue control 
     * Multistate value -> StackedStates control
     */
    private void createInputControls(RelativeLayout parentLayout) {
        Context ctx = getActivity().getApplicationContext();
        float largeTextSize = UIFactory.getDPDimen(ctx, R.dimen.text_size_large);

        // Containing layout
        RelativeLayout inputLayout = new RelativeLayout(ctx);
        RelativeLayout.LayoutParams inputLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        inputLayoutParams.setMargins(0, 0, 0, 10);
        inputLayout.setLayoutParams(inputLayoutParams);

        // Create input based on type of reference
        if (mPointType == VirtualStatPoint.Type.ANALOG) {
            activeInput = new SeekBarWithValue(ctx, valueSliderListener, largeTextSize);
            activeInput.setEnabled(false); // By default, set disabled (auto)
        }
        else if (mPointType == VirtualStatPoint.Type.MULTISTATE) {
            activeInput = new StackedStates(ctx, FanPoint.FanValueByState, largeTextSize, statesGroupListener);
        }
        else {
            // Should not get here, if unsupported we should see an error page.
            return;
        }

        // Add to parent layout
        inputLayout.addView(activeInput);
        parentLayout.addView(inputLayout);
    }

    /**
     * Converts the fan delegate value to be displayed in the fragment; we display integers, but the true value may be any double.
     * 
     * @return The value rounded up to the nearest integer
     */
    private int convertDelegateToDisplay() {
        int value;

        try {
            value = (int) Math.round(Double.parseDouble(virtualStatDelegate.Fan.getValue()));
        } catch (Exception e) {
            value = 0;
        }

        return value;
    }

    private void playClick() {
        if (!suppressSounds) {
            App.getAudioManager().playSoundEffect(SoundEffectConstants.CLICK);
        }
    }

    // --------------------------------------------------------------------------------
    // VirtualStat.UseVirtualStat interface
    // --------------------------------------------------------------------------------
    /**
     * Get the VirtualStat object from the parent activity; use ExposeVirtualStat interface to ensure that the parent correctly exposes the object.
     */
    @Override
    public void getStatDelegate() {
        Activity activity = getActivity();

        try {
            virtualStatDelegate = ((ExposeVirtualStat) activity).getCurrentStat();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ExposeVirtualStat");
        }
    }

    /**
     * Update the fragment with the desired values from the VirtualStat delegate; allows the parent activity to 'push' updates to each fragment.
     */
    @Override
    public void updateWithDelegate() {
        if (!isReady || (virtualStatDelegate == null)) {
            return;
        }

        // When updating with delegate, always use virtualStatDelegate.Fan as the reference may have changed since last call.
        FanPoint point = virtualStatDelegate.Fan;
        boolean isError = point.getValue().startsWith("QERR");
        boolean isInManual = point.getManualState();

        // Update UI according to object type
        if (mPointType == VirtualStatPoint.Type.ANALOG) {
            if (isError) {
                ((SeekBarWithValue) activeInput).setError();
            } else if (isInManual) {
                // Setting the progress should trigger the value text to update.
                ((SeekBarWithValue) activeInput).setValue(convertDelegateToDisplay());
            }
            else {
                ((SeekBarWithValue) activeInput).setError(autoTextForSeek);
            }
        }
        else if (mPointType == VirtualStatPoint.Type.MULTISTATE) {
            // Note: Cannot use setEnabled/Disabled here since it conflicts with using enabled/disabled to show auto/manual.
            // Note: Being inError will also disable the entire fragment, so not being able to disable the input directly may not be a huge issue.
            if (isError) {
                // activeInput.setEnabled(false);
            }
            else {
                ((StackedStates) activeInput).selectState(point.getValue());
            }
        }
        else {
            return;
        }

        // Update auto/manual checkbox
        autoManualCheckbox.setChecked(point.getManualState());

        // Disable entire fragment
        if (isError) {
            UIFactory.disableFragment(getActivity(), this);
        }
        else {
            UIFactory.enableFragment(getActivity(), this);
        }
    }
}
