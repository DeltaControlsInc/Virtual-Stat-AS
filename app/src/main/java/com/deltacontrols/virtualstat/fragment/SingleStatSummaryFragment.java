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
 * SingleStatSummaryFragment.java
 */
package com.deltacontrols.virtualstat.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.activity.SingleStatControlActivity;
import com.deltacontrols.virtualstat.points.LightsPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;

/**
 * Fragment containing the summary of all points for a stat; uses virtualStatDelegate (from VirtualStat.UseVirtualStat.getStatDelegate) 
 * to interact with the current VirtalStat data points.
 */
public class SingleStatSummaryFragment extends Fragment implements VirtualStat.UseVirtualStat {
    // --------------------------------------------------------------------------------
    // Helper classes
    // --------------------------------------------------------------------------------
    /**
     * Groups references to all the outlets for each summary item so they can be enabled / disabled accordingly.
     */
    private class SummaryItem {
        LinearLayout layout;
        TextView text;
        ImageView icon;
        int tab;
        String ref;
        String formattedValue;

        public SummaryItem(LinearLayout l, TextView t, ImageView i, int ta) {
            layout = l;
            text = t;
            icon = i;
            tab = ta;

            text.setText(getString(R.string.loading_value));
        }

        public void updateTextViews(String r, String v) {
            ref = r;
            formattedValue = v;

            // Cases:
            // 1. The point is not setup in the stat: Hide icon and value
            // 2. The point is setup, but the ref/value is in error: Show icon, but show ?? for value
            if (ref.equals("")) {
                text.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                layout.setOnClickListener(null);
            }
            else {
                // If null, then no value returned
                // NOTE eWEB api/systems appears to return NULL for some values, if this changes, we may need to remove this clause.
                if ((formattedValue == null) || (formattedValue.equals("NULL"))) {
                    formattedValue = "";
                    layout.setOnClickListener(null);
                }
                // If not initialized, indicate loading
                else if (formattedValue.equals(VirtualStatPoint.NotInitializedString)) {
                    formattedValue = getString(R.string.loading_value);
                    layout.setOnClickListener(null);
                }
                // If error, show error string
                else if (formattedValue.startsWith("QERR") || formattedValue.equals("Unknown object")) {
                    formattedValue = "??";
                    layout.setOnClickListener(null);
                }
                // Add click listener only if no error.
                else {
                    layout.setOnClickListener(createItemListener(getActivity(), tab));
                }

                text.setVisibility(View.VISIBLE);
                icon.setVisibility(View.VISIBLE);
                text.setText(formattedValue);
                text.invalidate();
            }
        }
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    // Outlets (icons/text)
    SummaryItem tempSummary;
    SummaryItem lightsSummary;
    SummaryItem fanSummary;
    SummaryItem blindsSummary;

    // VirtualStat (model) Interface delegate
    private VirtualStat virtualStatDelegate = null;
    private boolean isReady = false;

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Creates the view for the summary fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_single_stat_summary, container, false);
        View singleSummary = view;

        getStatDelegate();

        // Setup summary items with corresponding outlets / icons / tab data
        tempSummary = new SummaryItem((LinearLayout) singleSummary.findViewById(R.id.summary_layout_temp),
                (TextView) singleSummary.findViewById(R.id.summary_value_temp),
                (ImageView) singleSummary.findViewById(R.id.summary_icon_temp),
                SingleStatControlTabs.TEMP_TAB);

        lightsSummary = new SummaryItem((LinearLayout) singleSummary.findViewById(R.id.summary_layout_lights),
                (TextView) singleSummary.findViewById(R.id.summary_value_lights),
                (ImageView) singleSummary.findViewById(R.id.summary_icon_lights),
                SingleStatControlTabs.LIGHTS_TAB);

        fanSummary = new SummaryItem((LinearLayout) singleSummary.findViewById(R.id.summary_layout_fan),
                (TextView) singleSummary.findViewById(R.id.summary_value_fan),
                (ImageView) singleSummary.findViewById(R.id.summary_icon_fan),
                SingleStatControlTabs.FAN_TAB);

        blindsSummary = new SummaryItem((LinearLayout) singleSummary.findViewById(R.id.summary_layout_blinds),
                (TextView) singleSummary.findViewById(R.id.summary_value_blinds),
                (ImageView) singleSummary.findViewById(R.id.summary_icon_blinds),
                SingleStatControlTabs.BLINDS_TAB);

        // Update font
        UIFactory.setCustomFont(getActivity(), view);
        isReady = true;

        return view;
    }

    public void onResume() {
        super.onResume();
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * createItemListener Generates an OnClickListener closed on given parameters regarding the tab to load.
     * 
     * @param ctx Context
     * @param tabToLoad Tab enum that the click will attempt to load
     * @return An OnClickListener which will load the desierd tab
     */
    private OnClickListener createItemListener(final Context ctx, final int tabToLoad) {

        return new OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                // Lock against the delegate to prevent multiple clicks from invoking the activity multiple times.
                synchronized (virtualStatDelegate) {
                    // Save statStr in shared preferences so that it can be used by other activities in the future.
                    App.saveStatIntoSharedPreferences(virtualStatDelegate.createSystemJSONString());

                    // Launch single stat activity
                    Intent intent = new Intent(ctx, SingleStatControlActivity.class);
                    Bundle params = new Bundle();
                    params.putInt("loadTab", tabToLoad);
                    intent.putExtras(params);
                    startActivity(intent);
                }
            }
        };
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

        Context ctx = getActivity();
        LightsPoint[] lights = new LightsPoint[] { virtualStatDelegate.getLights(0), virtualStatDelegate.getLights(1),
                virtualStatDelegate.getLights(2), virtualStatDelegate.getLights(3) };
        String lightsValue = LightsPoint.getSummaryValueFormatted(ctx, lights);
        String lightsRef = (LightsPoint.lightsDisabled(ctx, lights)) ? "" : "ref"; // Any string will indicate that we want to show the icon, use 'ref'.

        tempSummary.updateTextViews(virtualStatDelegate.Temp.getFullRef(), virtualStatDelegate.Temp.getValueFormatted());         
        fanSummary.updateTextViews(virtualStatDelegate.Fan.getFullRef(), virtualStatDelegate.Fan.getValueFormatted());
        blindsSummary.updateTextViews(virtualStatDelegate.Blinds.getFullRef(), virtualStatDelegate.Blinds.getValueFormatted());
        lightsSummary.updateTextViews(lightsRef, lightsValue);
    }
}
