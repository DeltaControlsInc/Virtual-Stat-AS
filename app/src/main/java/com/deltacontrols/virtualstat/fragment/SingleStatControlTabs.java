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
 * SingleStatControlTabs.java
 */
package com.deltacontrols.virtualstat.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.points.LightsPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;

/**
 * Fragment containing the tab bar that shows all valid virtual stat points and their associated display value; uses virtualStatDelegate (from VirtualStat.UseVirtualStat.getStatDelegate) to interact
 * with the current VirtalStat data points.
 */
public class SingleStatControlTabs extends Fragment implements VirtualStat.UseVirtualStat {

    // Tab states
    public static final int DEFAULT_TAB = -1; // Default = load function will use mDefaultTab at run time.
    public static final int TEMP_TAB = 1;
    public static final int LIGHTS_TAB = 2;
    public static final int FAN_TAB = 3;
    public static final int BLINDS_TAB = 4;
    public static final int SUMMARY_TAB = 5;
    public static final int LIST_VIEW = 6;

    private int mTabState = -1;
    private int mDefaultTab = TEMP_TAB; // Default use temp tab
    private boolean suppressSounds = false;

    // Outlets
    LinearLayout tempTab = null;
    LinearLayout lightsTab = null;
    LinearLayout fanTab = null;
    LinearLayout blindsTab = null;
    LinearLayout defaultTab = null;

    ImageView tempIcon = null;
    ImageView lightsIcon = null;
    ImageView fanIcon = null;
    ImageView blindsIcon = null;

    TextView tempValue = null;
    TextView lightsValue = null;
    TextView fanValue = null;
    TextView blindsValue = null;

    // VirtualStat (model) Interface delegate
    private VirtualStat virtualStatDelegate = null;

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_single_stat_control_tabs, container, false);

        suppressSounds = true; // Suppress sound during setup to avoid undesired "clicks"

        // Setup activity delegate
        getStatDelegate();

        // Hookup tab 'clicks'
        tempIcon = (ImageView) view.findViewById(R.id.tempTabIcon);
        tempValue = (TextView) view.findViewById(R.id.tempTabValue);
        tempTab = (LinearLayout) view.findViewById(R.id.tempTabLayout);
        tempTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadView(TEMP_TAB);
            }
        });

        lightsIcon = (ImageView) view.findViewById(R.id.lightsTabIcon);
        lightsValue = (TextView) view.findViewById(R.id.lightsTabsValue);
        lightsTab = (LinearLayout) view.findViewById(R.id.lightsTabLayout);
        lightsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadView(LIGHTS_TAB);
            }
        });

        fanIcon = (ImageView) view.findViewById(R.id.fanTabIcon);
        fanValue = (TextView) view.findViewById(R.id.fanTabValue);
        fanTab = (LinearLayout) view.findViewById(R.id.fanTabLayout);
        fanTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadView(FAN_TAB);
            }
        });

        blindsIcon = (ImageView) view.findViewById(R.id.blindsTabIcon);
        blindsValue = (TextView) view.findViewById(R.id.blindsTabValue);
        blindsTab = (LinearLayout) view.findViewById(R.id.blindsTabLayout);
        blindsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadView(BLINDS_TAB);
            }
        });

        // Set custom font
        UIFactory.setCustomFont(getActivity(), view);

        return view;
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
     * Loads the last known tab state
     */
    public void reloadLastTab() {
        loadView(mTabState);
    }

    //
    /**
     * Generic function to load tab fragment content given the 'tab' id.
     * 
     * @param tabID The tab to load (see top of page for constants)
     */
    public void loadView(int tabID) {
        playClick();
        hideUnusedTabs();

        // Set default.
        if (tabID == DEFAULT_TAB) {
            tabID = mDefaultTab;
        }

        UIFactory.cancelToast(); // Cancel any toasts that may be currently showing

        if (mTabState != tabID) {
            mTabState = tabID;

            int tabUnselected = this.getResources().getColor(R.color.DeltaBrightRed);
            int tabSelected = this.getResources().getColor(R.color.DeltaDarkRed);
            int textUnselected = this.getResources().getColor(R.color.DeltaDarkRed);
            int textSelected = this.getResources().getColor(R.color.DeltaTabText);

            // Reset tab colors
            tempTab.setBackgroundColor(tabUnselected);
            lightsTab.setBackgroundColor(tabUnselected);
            fanTab.setBackgroundColor(tabUnselected);
            blindsTab.setBackgroundColor(tabUnselected);

            // Reset icons
            tempIcon.setImageResource(R.drawable.icon_temp_red);
            lightsIcon.setImageResource(R.drawable.icon_lights_red);
            fanIcon.setImageResource(R.drawable.icon_fan_red);
            blindsIcon.setImageResource(R.drawable.icon_blinds_red);

            // Reset text color
            tempValue.setTextColor(textUnselected);
            lightsValue.setTextColor(textUnselected);
            fanValue.setTextColor(textUnselected);
            blindsValue.setTextColor(textUnselected);

            // Fragments have access to their parent Activity's FragmentManager. You can
            // obtain the FragmentManager like this.
            FragmentManager fm = getFragmentManager();

            if (fm != null) {
                FragmentTransaction ft = fm.beginTransaction();
                Fragment tabFrag = null;

                switch (tabID) {
                case LIGHTS_TAB:
                    lightsTab.setBackgroundColor(tabSelected);
                    lightsIcon.setImageResource(R.drawable.icon_lights_white);
                    lightsValue.setTextColor(textSelected);
                    tabFrag = new SingleStatControlLights();
                    break;

                case FAN_TAB:
                    fanTab.setBackgroundColor(tabSelected);
                    fanIcon.setImageResource(R.drawable.icon_fan_white);
                    fanValue.setTextColor(textSelected);
                    tabFrag = new SingleStatControlFan();
                    break;

                case BLINDS_TAB:
                    blindsTab.setBackgroundColor(tabSelected);
                    blindsIcon.setImageResource(R.drawable.icon_blinds_white);
                    blindsValue.setTextColor(textSelected);
                    tabFrag = new SingleStatControlBlinds();
                    break;

                default:
                    tempTab.setBackgroundColor(tabSelected);
                    tempIcon.setImageResource(R.drawable.icon_temp_white);
                    tempValue.setTextColor(textSelected);
                    tabFrag = new SingleStatControlTemperature();
                    break;
                }

                ft.replace(R.id.fragment_content, tabFrag);
                ft.commit();
            }
        }
    }

    /**
     * Show/hide required tabs and reset summary data; used to reset the state of the tabs after settings have been changed.
     */
    public void reset() {
        hideUnusedTabs();
        tempValue.setText("");
        lightsValue.setText("");
        fanValue.setText("");
        blindsValue.setText("");
    }

    /**
     * Show/hide tabs based on how the control delegate is setup.
     */
    private void hideUnusedTabs() {

        // Using delegate, determine which tabs are to be shown - if a reference is null, then do not show the tab
        // Only show tabs if one or more tabs are active. (ie. hide if only one is active).
        LinearLayout allTabs[] = new LinearLayout[] { tempTab, lightsTab, fanTab, blindsTab }; // NOTE Order same as "Tab states"

        boolean hideLights = virtualStatDelegate.getLights(0).getFullRef().equals("")
                && virtualStatDelegate.getLights(1).getFullRef().equals("")
                && virtualStatDelegate.getLights(2).getFullRef().equals("")
                && virtualStatDelegate.getLights(3).getFullRef().equals("");

        boolean tabVisibility[] = new boolean[] {
                !virtualStatDelegate.TempSetpoint.getFullRef().equals(""),
                !hideLights,
                !virtualStatDelegate.Fan.getFullRef().equals(""),
                !virtualStatDelegate.Blinds.getFullRef().equals("") };

        mDefaultTab = -1;

        // Iterate through tabs "backwards" - done so we can correctly determine our default tab state.
        for (int i = (allTabs.length - 1); i >= 0; i--) {
            if (tabVisibility[i]) {
                allTabs[i].setVisibility(View.VISIBLE);
                mDefaultTab = i + 1;
            }
            else {
                allTabs[i].setVisibility(View.GONE);
            }
        }
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

        if (virtualStatDelegate == null) {
            return;
        }
        // When updating with delegate, always get the point again, in case the pointer to the object has changed.

        String errorString = "??";
        String formattedValue = "";

        // TempSetpoint
        // If Temp is set, then show it; if not, then show the temperature (setpoint)
        formattedValue = virtualStatDelegate.Temp.getValueFormatted();
        if (formattedValue.equals(VirtualStatPoint.NotInitializedString)) {
            formattedValue = virtualStatDelegate.TempSetpoint.getValueFormatted();
        }
        if (formattedValue.startsWith("QERR")) {
            formattedValue = errorString;
        }
        tempValue.setText(formattedValue);

        // Lights
        // May have up to 4 inputs, if any one of these are ON, then the text should read On.
        LightsPoint[] lights = new LightsPoint[] { virtualStatDelegate.getLights(0), virtualStatDelegate.getLights(1),
                virtualStatDelegate.getLights(2), virtualStatDelegate.getLights(3) };
        formattedValue = LightsPoint.getSummaryValueFormatted(getActivity(), lights);
        if (formattedValue.startsWith("QERR")) {
            formattedValue = errorString;
        }
        lightsValue.setText(formattedValue);

        // Fan
        formattedValue = virtualStatDelegate.Fan.getValueFormatted();
        if (formattedValue.startsWith("QERR")) {
            formattedValue = errorString;
        }
        fanValue.setText(formattedValue);

        // Blinds
        formattedValue = virtualStatDelegate.Blinds.getValueFormatted();
        if (formattedValue.startsWith("QERR")) {
            formattedValue = errorString;
        }
        blindsValue.setText(formattedValue);
    }
}
