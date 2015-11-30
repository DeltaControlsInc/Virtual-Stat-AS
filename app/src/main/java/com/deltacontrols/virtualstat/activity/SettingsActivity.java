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
 * SettingsActivity.java
 */
package com.deltacontrols.virtualstat.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;

/**
 * Activity controlling the settings view of a the currently loaded stat; it will list all points in the stat and is mostly for test purposes 
 * Currently the values can be edited for the lifetime of the application, however, in the future we should make this read only.
 */
public class SettingsActivity extends Activity {

    // UI references.
    private EditText mNameView;
    private EditText mTemperature;
    private EditText mTemperatureSetpoint;
    private EditText mFanView;
    private EditText mFanOverrideView;
    private EditText mLightsView1;
    private EditText mLightsView2;
    private EditText mLightsView3;
    private EditText mLightsView4;
    private EditText mBlindsView;

    private VirtualStat settingsStat;

    // --------------------------------------------------------------------------------
    // Life cycle
    // --------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup UI View outlets
        mNameView = (EditText) findViewById(R.id.virtualStatName);
        mTemperature = (EditText) findViewById(R.id.TemperatureActual);
        mTemperatureSetpoint = (EditText) findViewById(R.id.IAT);
        mFanView = (EditText) findViewById(R.id.Fan);
        mFanOverrideView = (EditText) findViewById(R.id.FanOverride);
        mLightsView1 = (EditText) findViewById(R.id.Lights_1);
        mLightsView2 = (EditText) findViewById(R.id.Lights_2);
        mLightsView3 = (EditText) findViewById(R.id.Lights_3);
        mLightsView4 = (EditText) findViewById(R.id.Lights_4);
        mBlindsView = (EditText) findViewById(R.id.Blinds);

        // Get default loaded stat
        SharedPreferences sharedPreferences = getSharedPreferences(App.SHARED_PREF_ID, MODE_PRIVATE);
        settingsStat = new VirtualStat();

        // Load current stat from XML
        try {
            String jsonStr = sharedPreferences.getString(App.SHARED_PREF_CURRENT_STAT_JSON, "");
            if (jsonStr != "") {
                settingsStat.loadFromJSON(jsonStr);
            }
        } catch (Exception e) {

        }

        // Write to outlets; for now keep separate since in the future we may not use sharedPreferences.
        mNameView.setText(settingsStat.Name);
        mTemperature.setText(settingsStat.Temp.getFullRef());
        mTemperatureSetpoint.setText(settingsStat.TempSetpoint.getFullRef());
        mFanView.setText(settingsStat.Fan.getFullRef());
        mFanOverrideView.setText(settingsStat.Fan.override.getFullRef());
        mLightsView1.setText(settingsStat.Lights1.getName());
        mLightsView2.setText(settingsStat.Lights2.getName());
        mLightsView3.setText(settingsStat.Lights3.getName());
        mLightsView4.setText(settingsStat.Lights4.getName());
        
        mBlindsView.setText(settingsStat.Blinds.getFullRef());

        // Set custom font
        UIFactory.setCustomFont(this, findViewById(R.id.mainPageViewGroup));
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------

    public void onSaveClick(View view) {
        // Grab values
        // Store values at the time of the login attempt.
        settingsStat.Name = mNameView.getText().toString();
        settingsStat.Temp.setFullRef(mTemperature.getText().toString());
        settingsStat.TempSetpoint.setFullRef(mTemperatureSetpoint.getText().toString());
        settingsStat.Fan.setFullRef(mFanView.getText().toString());
        settingsStat.Fan.override.setFullRef(mFanOverrideView.getText().toString());
        settingsStat.Lights1.setFullRef(mLightsView1.getText().toString());
        settingsStat.Lights2.setFullRef(mLightsView2.getText().toString());
        settingsStat.Lights3.setFullRef(mLightsView3.getText().toString());
        settingsStat.Lights4.setFullRef(mLightsView4.getText().toString());
        settingsStat.Blinds.setFullRef(mBlindsView.getText().toString());

        // Save state by setting local preferences to current XML string.
        App.saveStatIntoSharedPreferences(settingsStat.createSystemJSONString());

        setResult(RESULT_OK);
        finish();
    }

    public void onCancelClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
