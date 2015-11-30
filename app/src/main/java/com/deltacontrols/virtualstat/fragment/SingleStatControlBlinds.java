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
 * SingleStatControlBlinds.java
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.controls.OnOffToggle;
import com.deltacontrols.virtualstat.controls.SlidingWindow;
import com.deltacontrols.virtualstat.controls.SlidingWindow.SlidingWindowViewListener;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint.Type;

/**
 * Fragment containing the blinds point; uses virtualStatDelegate (from VirtualStat.UseVirtualStat.getStatDelegate) to interact with the 
 * current VirtalStat data points.
 */
public class SingleStatControlBlinds extends Fragment implements VirtualStat.UseVirtualStat
{
    // Outlets
    private TextView blindsSetpoint = null;
    private SlidingWindow blindsSlider = null;
    private RelativeLayout analogInputLayout = null;
    private RelativeLayout binaryInputLayout = null;
    private OnOffToggle blindsToggle = null;

    // VirtualStat (model) Interface delegate
    private VirtualStat virtualStatDelegate = null;
    private boolean isReady;
    private boolean suppressSounds = false;

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    public SingleStatControlBlinds() {
        // Required empty public constructor
    }

    /**
     * Creates the view for the blinds fragment; if the point is not valid, then show an error view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_single_stat_control_blinds, container, false);
        View resultView = null;

        suppressSounds = true; // Suppress sound during setup to avoid undesired "clicks"

        // Set the stat delegate
        getStatDelegate();

        // Check for errors
        if (!virtualStatDelegate.Blinds.isValid()) {
            final View errorView = inflater.inflate(R.layout.fragment_single_stat_error, container, false);
            isReady = false;
            resultView = errorView;
        }
        else {
            blindsSetpoint = (TextView) view.findViewById(R.id.blindsSetpointText);
            blindsSlider = (SlidingWindow) view.findViewById(R.id.slidingWindow);
            analogInputLayout = (RelativeLayout) view.findViewById(R.id.analogInputLayout);
            binaryInputLayout = (RelativeLayout) view.findViewById(R.id.binaryInputLayout);

            // Create control, set ready and then attempt to update.
            createInputControls();
            isReady = true;
            resultView = view;
        }

        // Set custom font
        UIFactory.setCustomFont(getActivity(), resultView);
        return resultView;
    }

    @Override
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
     * Analog value -> Sliding window control (blinds image) 
     * Binary value -> OnOffToggle control
     */
    private void createInputControls() {

        Context ctx = getActivity();
        VirtualStatPoint point = virtualStatDelegate.Blinds;
        VirtualStatPoint.Type type = point.getType();
        float largeTextSize = UIFactory.getDPDimen(ctx, R.dimen.text_size_large);

        if (type == Type.ANALOG) {
            blindsSlider.setListener(new SlidingWindowViewListener() {
                @Override
                public void onValueChange(int value) {
                    String newValue = String.valueOf(value);
                    blindsSetpoint.setText(newValue + "%");

                    playClick();

                    // NOTE: The value we display may not be identical to the true delegate value.
                    // We display in increments of 10, but the true value may be any double.
                    if (convertDelegateToDisplay() != value) {
                        virtualStatDelegate.setValue(virtualStatDelegate.Blinds, newValue);
                    }
                }
            });

            analogInputLayout.setVisibility(View.VISIBLE);
        }
        else if (type == Type.BINARY) {
            analogInputLayout.setVisibility(View.GONE);
            blindsToggle = new OnOffToggle(ctx, new OnOffToggle.ToggleListener() {
                @Override
                public void onValueChange(String result) {
                    virtualStatDelegate.setValue(virtualStatDelegate.Blinds, result);
                    playClick();
                }
            }, largeTextSize, "Open", "Closed", "Closed");

            binaryInputLayout.addView(blindsToggle);
            binaryInputLayout.setVisibility(View.VISIBLE);
        }
        else {
            UIFactory.deltaToast(getActivity(), "Unsupported type: " + type, null);
        }
    }

    /**
     * Converts the blinds delegate value to be displayed in the fragment; we display in increments of 10, but the true value may be any double.
     * 
     * @return The value rounded up to the nearest 10.
     */
    private int convertDelegateToDisplay() {
        int value;

        try {
            value = (int) roundUp(Double.parseDouble(virtualStatDelegate.Blinds.getValue()), 10);
            value = Math.max(0, value);
            value = Math.min(value, 100);
        } catch (Exception e) {
            value = 0;
        }

        return value;
    }

    /**
     * Rounds up to nearest roundTo increment
     * 
     * @param value Value to round
     * @param roundTo Round precision
     * @return The value rounded up to the given precision
     */
    private double roundUp(double value, double roundTo) {
        if ((value <= 0) || (roundTo == 0)) {
            return 0f;
        }
        return Math.round(value / roundTo) * roundTo;
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
    public void updateWithDelegate()
    {
        if (!isReady || (virtualStatDelegate == null)) {
            return;
        }

        String value = virtualStatDelegate.Blinds.getValue();
        boolean isError = value.startsWith("QERR");

        // Slider
        if (blindsToggle == null) {
            if (isError) {
                blindsSlider.setEnabled(false);
                blindsSetpoint.setText("??");
            }
            else {
                blindsSlider.setEnabled(true);
                blindsSlider.setValue(convertDelegateToDisplay());
            }

        }
        // Toggle button
        else {
            if (isError) {
                blindsToggle.setEnabled(false);
                blindsToggle.setError();
            }
            else {
                blindsToggle.setEnabled(true);
                blindsToggle.setValue(value);
            }
        }

        // Disable entire fragment content
        if (isError) {
            UIFactory.disableFragment(getActivity(), this);
        }
        else {
            UIFactory.enableFragment(getActivity(), this);
        }
    }

}
