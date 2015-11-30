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
 * SingleStatControlLights.java
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.controls.OnOffToggle;
import com.deltacontrols.virtualstat.controls.SeekBarWithValue;
import com.deltacontrols.virtualstat.points.LightsPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;

/**
 * Fragment containing the light(s) point; uses virtualStatDelegate (from VirtualStat.UseVirtualStat.getStatDelegate) 
 * to interact with the current VirtalStat data points.
 */
public class SingleStatControlLights extends Fragment implements VirtualStat.UseVirtualStat {
    // Outlets
    // Dynamically generated lighting inputs; could be SeekBar or Checkbox
    private View[] lightingInputs = new View[4];

    // VirtualStat (model) Interface delegate
    private VirtualStat virtualStatDelegate = null;
    private boolean isReady;
    private boolean suppressSounds = false;

    // Cache strings
    String txid_value_on;   // "On" string
    String txid_value_off;  // "Off" string

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    public SingleStatControlLights() {
        // Required empty public constructor
    }

    /**
     * Creates the view for the blinds fragment; if the point is not valid, then show an error view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_single_stat_control_lights, container, false);
        View resultView = null;
        suppressSounds = true; // Suppress sound during setup to avoid undesired "clicks"

        // Cache text
        txid_value_on = getResources().getString(R.string.value_on);
        txid_value_off = getResources().getString(R.string.value_off);

        // Set the stat delegate
        getStatDelegate();

        // Check for errors
        LightsPoint[] lights = { virtualStatDelegate.getLights(0), virtualStatDelegate.getLights(1),
                virtualStatDelegate.getLights(2), virtualStatDelegate.getLights(3) };

        // Note, have to use special function to determine if lights are valid (must take all inputs into account).
        if (!LightsPoint.isValid(lights)) {
            final View errorView = inflater.inflate(R.layout.fragment_single_stat_error, container, false);
            isReady = false;
            resultView = errorView;
        }
        else {
            // Add dynamically generated seekbars
            LinearLayout seekBarLayout = (LinearLayout) view.findViewById(R.id.seekBarsLayout);
            createInputControls(seekBarLayout);

            isReady = true;
            resultView = view;
        }

        // Set custom font
        UIFactory.setCustomFont(getActivity(), resultView);
        return resultView;
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
     * Dynamically generate the blinds input based on {@link VirtualStatPoint.Type}. 
     * Analog value -> SeekBarWithValue control 
     * Binary value -> OnOffToggle control
     * 
     * @param parentLayout Layout to add the lighting inputs to.
     */
    private void createInputControls(LinearLayout parentLayout) {

        Context ctx = getActivity();

        float mediumTextSize = UIFactory.getDPDimen(ctx, R.dimen.text_size_medium);
        float largeTextSize = UIFactory.getDPDimen(ctx, R.dimen.text_size_large);

        String ref, name = null;
        VirtualStatPoint point;

        // For each light point, create a RelativeLayout containing:
        // 1 - The name of the object (TextView)
        // 2 - The value of the object (TextView)
        // 3 - The input control for the object (SeekBar or CheckBox)
        for (int i = 0; i < lightingInputs.length; i++) {
            point = virtualStatDelegate.getLights(i);
            ref = point.getFullRef();

            // Empty refs not shown.
            if (ref.equals(""))
                continue;

            name = point.getName();
            // if name is empty, use ref
            if ((name == null) || name.isEmpty() || name.contains("null"))
            	name = ref;            
            	

            // Create parent layout and name layout - these will be the same no matter what type of object (binary or analog).
            // Create secondary layout to hold the pieces
            RelativeLayout inputLayout = new RelativeLayout(ctx);
            LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            inputLayoutParams.setMargins(0, 0, 0, 10);
            inputLayout.setLayoutParams(inputLayoutParams);

            // Create Name Label
            int inputID = VirtualStat.generateViewId();
            TextView inputName = new TextView(ctx);
            RelativeLayout.LayoutParams seekNameParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            inputName.setLayoutParams(seekNameParams);
            inputName.setPadding(5, 5, 5, 5);
            inputName.setText(name);
            inputName.setId(inputID);
            inputName.setTextAppearance(ctx, R.style.delta_tabSingleValueText);
            inputName.setTextSize(mediumTextSize);
            inputLayout.addView(inputName);

            // Create input based on type of reference
            ViewGroup innerLayout;
            if (point.getType() == VirtualStatPoint.Type.BINARY) {
                innerLayout = new OnOffToggle(ctx, createOnOffToggleListener(i), largeTextSize, txid_value_on, txid_value_off, txid_value_off);
            }
            else if (point.getType() == VirtualStatPoint.Type.ANALOG) {
                innerLayout = new SeekBarWithValue(ctx, createValueSliderListener(i), largeTextSize);
            }
            else {
                continue; // Do nothing, not supported.
            }

            // Put input directly below name
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) innerLayout.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, inputID);
            inputLayout.addView(innerLayout);

            // Set outlets
            lightingInputs[i] = innerLayout;

            // Add everything to parent layout.
            parentLayout.addView(inputLayout);
        }
    }

    /**
     * Converts the lights delegate value to be displayed in the fragment; we display integers, but the true value may be any double.
     * 
     * @return The value rounded up to the nearest integer
     */
    private int convertValueToDisplay(String value) {

        int result;
        try {
            result = (int) Math.round(Double.parseDouble(value));
            if (result > 100) {
                result = 100;
            }else {
                if (result < 0){
                    result = 0;
                }
            }

        } catch (Exception e) {
            result = 0;
        }

        return result;
    }

    private void playClick() {
        if (!suppressSounds) {
            App.getAudioManager().playSoundEffect(SoundEffectConstants.CLICK);
        }
    }

    // --------------------------------------------------------------------------------
    // Listener Generators
    // Uses index to determine which 'lights' point the listener is hooked up to.
    // --------------------------------------------------------------------------------
    private SeekBarWithValue.ChangeListener createValueSliderListener(final int index) {

        SeekBarWithValue.ChangeListener listener = new SeekBarWithValue.ChangeListener() {
            @Override
            public void onValueChange(int value) {
                // Analog value
                String newValue = Integer.toString(value);
                int oldValue = convertValueToDisplay(virtualStatDelegate.getLights(index).getValue());

                if (oldValue != value) {
                    virtualStatDelegate.setValue(virtualStatDelegate.getLights(index), newValue);
                    playClick();
                }
            }
        };

        return listener;
    }

    private OnOffToggle.ToggleListener createOnOffToggleListener(final int index) {

        OnOffToggle.ToggleListener listener = new OnOffToggle.ToggleListener() {
            @Override
            public void onValueChange(String value) {
                virtualStatDelegate.setValue(virtualStatDelegate.getLights(index), value);
                playClick();
            }
        };

        return listener;
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
    public void updateWithDelegate()
    {
        if (!isReady || (virtualStatDelegate == null)) {
            return;
        }

        String inputClass, value;
        int iValue;
        VirtualStatPoint point;
        boolean isError, disable;
        disable = true;

        for (int i = 0; i < lightingInputs.length; i++) {

            if (lightingInputs[i] == null)
                continue; // Ignore unused lighting inputs.

            inputClass = lightingInputs[i].getClass().getSimpleName();
            point = virtualStatDelegate.getLights(i);
            value = point.getValue();
            isError = value.startsWith("QERR");

            if (!isError) {
                disable = false;
            } // If even a single input is active, then do not disable the entire frag.

            if (inputClass.equals("SeekBarWithValue")) {
                if (isError) {
                    ((SeekBarWithValue) lightingInputs[i]).setError();
                }
                else {
                    try {
                        iValue = convertValueToDisplay(value); // Value *could* be decimal, round it to the nearest decimal.
                    } catch (Exception e) {
                        iValue = 0;
                    }

                    // Setting the progress should trigger the value text to update.
                    ((SeekBarWithValue) lightingInputs[i]).setValue(iValue);
                }
            }
            else if (inputClass.equals("OnOffToggle")) {
                if (isError) {
                    ((OnOffToggle) lightingInputs[i]).setError();
                }
                else {
                    ((OnOffToggle) lightingInputs[i]).setValue(value);
                }
            }
        }

        // Disable entire fragment content
        if (disable) {
            UIFactory.disableFragment(getActivity(), this);
        }
        else {
            UIFactory.enableFragment(getActivity(), this);
        }
    }
}