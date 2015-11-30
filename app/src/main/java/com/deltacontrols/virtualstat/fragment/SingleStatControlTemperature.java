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
 * SingleStatControlTemperature.java
 */
package com.deltacontrols.virtualstat.fragment;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.controls.RotatingImageView;
import com.deltacontrols.virtualstat.controls.RotatingImageView.RotatingImageViewListener;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint.SetBy;

/**
 * Fragment containing the blinds point; uses virtualStatDelegate (from VirtualStat.UseVirtualStat.getStatDelegate) to 
 * interact with the current VirtalStat data points.
 */
public class SingleStatControlTemperature extends Fragment implements VirtualStat.UseVirtualStat {

    // Outlets
    private TextView tempSetpoint = null;
    private RotatingImageView jogWheel = null;
    private TextView degreeSymbol = null;
    private CharSequence units = "\u00B0";

    // VirtualStat (model) Interface delegate
    private VirtualStat virtualStatDelegate = null;
    private boolean isReady = false;
    private boolean suppressSounds = false;

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Creates the view for the temp fragment; if the point is not valid, then show an error view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_single_stat_control_temperature, container, false);
        View resultView = null;

        suppressSounds = true; // Suppress sound during setup to avoid undesired "clicks"

        // Set the stat delegate
        getStatDelegate();

        // Check for errors (temp setpoint MUST exist)
        if (!virtualStatDelegate.TempSetpoint.isValid()) {
            final View errorView = inflater.inflate(R.layout.fragment_single_stat_error, container, false);
            isReady = false;
            resultView = errorView;
        }
        else {
            // Outlets
            tempSetpoint = (TextView) view.findViewById(R.id.tempSetpointText);
            if (tempSetpoint == null) {
                Toast.makeText(getActivity().getApplicationContext(), getActivity().getApplicationContext().getString(R.string.temp_setpoint_null), Toast.LENGTH_SHORT).show();
            }
            ;
            degreeSymbol = (TextView) view.findViewById(R.id.degreeSymbol);

            jogWheel = (RotatingImageView) view.findViewById(R.id.jogRotatingImageView);
            jogWheel.setListener(new RotatingImageViewListener() {
                @Override
                public void onViewReady() {
                    updateWheel();
                }

                @Override
                public void onValueChange(double value) {
                    if (value != Double.NaN) {
                        // Attempt to change the temp value
                        String convertValue = convertDoubleToFormattedString(value);
                        virtualStatDelegate.setValue(virtualStatDelegate.TempSetpoint, convertValue);
                        updateWithDelegate(); // Needed?
                    }
                }
            });

            isReady = true;
            resultView = view;
        }

        // Update font
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
    // Returns delegate value truncated to one decimal place
    private String formatValue() {
        if (virtualStatDelegate == null) {
            return null;
        }

        try {
            String value = virtualStatDelegate.TempSetpoint.getValue();            
                        
            // Convert to 1 decimal place            
            Double dValue = Double.parseDouble(value);            
            	
            return convertDoubleToFormattedString(dValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Updates the text outlet for the temperature
     */
    private void updateText() {
        String value = formatValue();
        if (value == null) {
            degreeSymbol.setText("");
            return;
        }
        else {
            degreeSymbol.setText(units);
        }

        // Parse into desired format and display
        if (!value.equals(tempSetpoint.getText())) {
            tempSetpoint.setText(value);
            playClick();
        }
    }

    /**
     * Updates the jog wheel outlet for the temperature
     */
    private void updateWheel() {
        String value = formatValue(); // Forces decimal truncation
        if (value == null)
            return;

        // Compare values as strings to avoid double rounding issues.
        String jogValueStr = convertDoubleToFormattedString(jogWheel.getValue());
        if (!value.equals(jogValueStr)) {
            VirtualStatPoint point = virtualStatDelegate.TempSetpoint;
            point.setValue(String.valueOf(value), SetBy.SYSTEM); // Update model to use expected format.
            jogWheel.setValue(Double.parseDouble(value));
            // No click here, sound applied when updating the text
        }
    }

    /**
     * If the value is not an error, this function will format the temperature to a single point of precision.
     * 
     * @param value
     *            The value to convert
     * @return A string containing the value, formatted to one decimal place
     */
    private String convertDoubleToFormattedString(double value) {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("###0.0", decimalFormatSymbols);
        String formattedValue = decimalFormat.format(value);
        return formattedValue;
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
        if (isReady) {

            String value = virtualStatDelegate.TempSetpoint.getValue();
            if (value.startsWith("QERR")) {
                UIFactory.disableFragment(getActivity(), this);
                tempSetpoint.setText("??");
            }
            else {
                UIFactory.enableFragment(getActivity(), this);
            }

            updateText();
            updateWheel();
        }
    }
}
