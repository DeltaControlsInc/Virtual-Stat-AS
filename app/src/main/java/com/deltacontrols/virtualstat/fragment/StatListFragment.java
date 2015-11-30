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
 * StatListFragment.java
 */

package com.deltacontrols.virtualstat.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpStatus;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;

import com.deltacontrols.eweb.support.api.EwebConnection;
import com.deltacontrols.eweb.support.api.FetchJSON;
import com.deltacontrols.eweb.support.interfaces.GenericCallback;
import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.StatListItem;
import com.deltacontrols.virtualstat.StatListItemAdapter;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.ExposeVirtualStat;
import com.deltacontrols.virtualstat.activity.SummaryActivity;
import com.deltacontrols.virtualstat.controls.ToggleBar;
import com.deltacontrols.virtualstat.controls.ToggleBar.ToggleInteractionListener;

/**
 * StatListFragment
 * 
 * Controls user interaction with the stat list fragment. Responsible for loading in stat data from eWEB, caching stat data, and allowing 
 * users to load stats based on list selections.
 */
public class StatListFragment extends Fragment implements VirtualStat.UseVirtualStat {

    // --------------------------------------------------------------------------------
    // Static helper classes
    // --------------------------------------------------------------------------------
    /**
     * Caching class helper - cache Stat JSON strings to avoid extra requests. Made static so other activities can update the cached JSON to 
     * include updated values
     */
    public static class StatListCache {
        private static HashMap<String, String> cachedJSONStrs = new HashMap<String, String>();

        public static String get(String statName) {
            return cachedJSONStrs.containsKey(statName) ? cachedJSONStrs.get(statName) : null;
        }

        public static void clear() {
            cachedJSONStrs.clear();
        }

        public static void set(String statName, String json) {
            cachedJSONStrs.put(statName, json);
        }

    }

    /**
     * Demo values for stats
     */
    public static class Demo {
        /*
         * getAllStats Mimics EwebConnection.getAllStats.
         */
        public static FetchJSON.Result getAllStats() {
            // It's fake! We're ok!
            FetchJSON.Result result = new FetchJSON.Result();
            result.success = true;
            result.statusCode = HttpStatus.SC_OK;

            // Fake values
            String[][] demoArr = new String[][]{
                    new String[] { "Training Room Demo", "Training Room Demo",  
                            "TEMP_SP", "real", "16", "/Demo/100/analog-value,1", "Room Temp Setpoint Adjust",
                            "TEMP", "real", "18", "/Demo/100/analog-input,1", "Room Temp",
                            "LIGHTS1", "real", "75", "/Demo/100/analog-value,2", "Training Room Light 1",
                            "LIGHTS2", "real", "75", "/Demo/100/analog-value,3", "Training Room Light 2",
                            "LIGHTS3", "real", "Active", "/Demo/100/binary-value,1", "Training Room Light 3",
                            "LIGHTS4", "enumerated", "Active", "/Demo/100/binary-value,2", "Training Room Light 4",
                            "FAN", "unsigned", "4", "/Demo/100/multi-state-value,1", "Training Room Fan",
                            "BLINDS", "real", "50", "/Demo/100/analog-value,3", "Training Room Window Blind" },
                    new String[] { "Front Hallway Demo","Front Hallway Demo", 
                            "TEMP_SP", "real", "20", "/Demo/100/analog-value,1", "Hallway Temeperature Setpoint Adjust",
                            "TEMP", "real", "14", "/Demo/100/analog-input,1", "Hallway Temperature",
                            "LIGHTS1", "real", "100", "/Demo/100/analog-value,2","Hallway Light 2",
                            "LIGHTS2", "enumerated", "Active", "/Demo/100/binary-value,1","Hallway Light 1",
                            "LIGHTS3", "", "", "","",
                            "LIGHTS4", "", "", "","",
                            "FAN", "", "", "", "",
                            "BLINDS", "", "", "", "" },
                    new String[] { "Emma's Office Demo", "Emma's Office Demo",
                            "TEMP_SP", "real", "22", "/Demo/100/analog-value,1", "",
                            "TEMP", "real", "21", "/Demo/100/analog-input,2", "Emma's Office Temp",
                            "LIGHTS1", "enumerated", "0", "/Demo/100/binary-value,1","Emma's Office Light",
                            "LIGHTS2", "", "", "","",
                            "LIGHTS3", "", "", "","",
                            "LIGHTS4", "", "", "","",
                            "FAN", "", "", "","",
                            "BLINDS", "", "", "",""},
                    new String[] { "Liam's Office Demo", "Liam's Office Demo",
                            "TEMP_SP", "real", "19", "/Demo/100/analog-value,1","",
                            "TEMP", "real", "23", "/Demo/100/analog-input,3", "Liam's Office Temp",
                            "LIGHTS1", "real", "0", "/Demo/100/analog-value,5","Liam's Office Light 1",
                            "LIGHTS2", "enumerated", "Inactive", "/Demo/100/binary-value,6","Liam's Office Light 2",
                            "LIGHTS3", "", "", "","",
                            "LIGHTS4", "", "", "","",
                            "FAN", "", "", "","",
                            "BLINDS", "", "", "", "" }
            };

            // System format (based on result from eweb api/systems/list request
            String formatString = " \"%s\": {\"%s\": { Objects: {" + // name
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, "+ // TEMP_SP
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // TEMP
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS1
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS2
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS3
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS4
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // FAN
            		"%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" } " + // BLINDS
            		"}}},";

            StringBuffer fakeData = new StringBuffer();
            String fakeDataString;
            JSONObject fakeJSON;
            try {
                fakeData.append("{ ");

                for (int i = 0; i < demoArr.length; i++) {
                    fakeData.append(String.format(formatString, (Object[]) demoArr[i]));
                }

                fakeData.deleteCharAt(fakeData.length() - 1); // remove last ","                
                fakeData.append("}");
                fakeDataString = fakeData.toString();
                fakeJSON = new JSONObject(fakeDataString);
            } catch (Exception e) {
                fakeJSON = new JSONObject();
            }

            result.json = fakeJSON;

            // Pass fake data back
            return result;
        }
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    Context ctx;
    InputMethodManager inputManager;

    // List
    ListView statList;
    View statListHeader;
    Filter statFilter;
    StatListItemAdapter listAdapter;
    ArrayList<StatListItem> listItemsArray = new ArrayList<StatListItem>();

    // Outlets
    EditText searchText;
    ToggleBar switcher_toggle_onlist;
    ImageView icon_refresh;

    // VirtualStat (model) Interface delegate
    private VirtualStat virtualStatDelegate = null;

    /**
     * Listener used to handle clicks on the stat list. When an item in the stat list is selected, load the stat's points so that the summary 
     * fragment can show the data. Also, depending on screen size, hide the soft keyboard automatically.
     */
    private OnItemClickListener selectListItemListener = new OnItemClickListener() {
        /*
         * (non-Javadoc) When item is clicked in the stat list, attempt to load the stat that corresponds to the selection.
         * 
         * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
         */
        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            Object o = statList.getItemAtPosition(position);

            if (o instanceof StatListItem) {
                // Get ID from the stat list item clicked and use it to load the stat.
            	String statName = ((StatListItem)o).name;
            	loadStatJSON(statName);
            	
                // Auto hide the soft keyboard
                inputManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0);

                // If switcher_toggle_onlist exists then we are on a 4" device, so auto hide the stat list as well.
                if (switcher_toggle_onlist != null) {
                    hideStatList();
                }
            }
        }
    };

    // --------------------------------------------------------------------------------
    // Life cycle
    // --------------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getStatDelegate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ctx = getActivity();
        inputManager = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);

        View view = inflater.inflate(R.layout.fragment_stat_list, container, false);
        icon_refresh = (ImageView) view.findViewById(R.id.icon_refresh);

        // Setup list
        statList = (ListView) view.findViewById(R.id.statListView);
        statList.setOnItemClickListener(selectListItemListener);

        // switcher_toggle_onlist exists on < large screens (where the list takes up the full layout)
        // If this toggler exists then hookup gesture listeners.
        // Note: Currently, toggler only visible if list is being shown; this means we only have to listen for a tap/swipe down
        // and implement the hide list functionality.
        switcher_toggle_onlist = (ToggleBar) view.findViewById(R.id.switcher_toggle_onlist);
        if (switcher_toggle_onlist != null) {
            switcher_toggle_onlist.setToggleInteractionListener(new ToggleInteractionListener() {
                @Override
                public void onTap() {
                    hideStatList();
                }

                @Override
                public void onSwipeUp() {
                	updateList();
                }

                @Override
                public void onSwipeDown() {
                    hideStatList();
                }
            });
        }

        // Search text will apply a real time filter on the stat list.
        searchText = ((EditText) view.findViewById(R.id.statListFilterText));
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilterToList(s.toString());
            }
        });

        // Setup clear icon
        ((ImageView) view.findViewById(R.id.icon_clear)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getView().playSoundEffect(SoundEffectConstants.CLICK);
                searchText.setText(null);
            }
        });

        icon_refresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getView().playSoundEffect(SoundEffectConstants.CLICK);
                updateList();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (listItemsArray.isEmpty()) {
            updateList(); // Do an initial update of the list; all subsequent updates must be triggered through the refresh button
        }
        else {
            setListAdapter(); // If coming back from a long period away, reset the list.
        }
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Listener used to handle the result of a request for the stat list. Loads the stat list with a given JSONObject containing a list of 
     * systems (from either eWEB or with demo data)
     */
    private GenericCallback<FetchJSON.Result> getListListener = new GenericCallback<FetchJSON.Result>() {
        @SuppressWarnings("unchecked")
        @Override
        public void onCallback(FetchJSON.Result result) {
            try {
                String name,jsonStr, itemName;
                Object value, itemValue;
                StatListItem singleListItem;
                JSONObject valueObj, itemObj;
                
                String statSys;
                Object statValue;
                JSONObject statSysObj;
                Iterator<String> statIter, iter, item;
                
                showLoadingIcon(false);
                listItemsArray.clear();


                /*
                {                	
                	Room 200: {...}-
                	Room100: {...}-
                }
                */

            	StatListCache.clear(); // Clear cache so list is always up to date with current request.          	            
                
                // Iterate over views result to get each stat's objects and values.                
                iter = result.json.keys();
                // Check if Stat list is empty
                if (iter.hasNext() == false){                	
                	App.removeStatFromSharedPreferences();
                	virtualStatDelegate.init();
                	jsonStr = virtualStatDelegate.createSystemJSONString();
                	virtualStatDelegate.loadFromJSON(jsonStr);
                	listAdapter = null;                	
                }
                else {
                    while(iter.hasNext()){
                    	//Top level key
                        name = iter.next();
                        value = result.json.get(name);
                        valueObj = (JSONObject) value;
                        
                        /*
                         * Room100: {
    					 *	$base: "Collection"
    					 *	Room100: {...}-
    					 *	Default System Dashboard: {...}-
    					 *  }
                         */
                        item = valueObj.keys();
                        while(item.hasNext()) {
                        	itemName = item.next();
                            // Only interested in the individual view
                            if (!itemName.contains("$base") && !itemName.contains("Default System Dashboard")){
                            	
                                itemValue = valueObj.get(itemName);
                                itemObj = (JSONObject)itemValue;
                                statIter = itemObj.keys();
                                while(statIter.hasNext()){
                                	statSys = statIter.next();
                                	if (statSys.contains("Objects")){
                                		statValue = itemObj.getString(statSys);
                                    	singleListItem = new StatListItem(name);
                                    	listItemsArray.add(singleListItem);
                                        // Since current web response contains full views info, add to cache to avoid a second request.
                                        // Cache json string, including name so that we can easily parse it out later.                        
                                        jsonStr = "{ \"" + name + "\" :" + statValue.toString() + "}";
    									StatListCache.cachedJSONStrs.put(name, jsonStr);
                                        break;
                                	}
                                }                    	
                            }
                        	
                        }
                                                                                  
                    } // while(iter.hasNext()){                
                } // List not empty
                	
                	                	
                // Sort list alphabetically by name
                Collections.sort(listItemsArray, new Comparator<StatListItem>() {
                    public int compare(StatListItem o1, StatListItem o2) {
                        return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
                    }
                });

                setListAdapter();                
                
                // If nothing selected, default to show first.
                if (virtualStatDelegate.Name.isEmpty()) {
                    loadFirstItemInList();
                }
                else{                	
                	String currentStat = StatListCache.get(virtualStatDelegate.Name);
                	// Check if currentStat still exist in stat list
                	if (currentStat == null){
                		// load first stat on the list
                		loadFirstItemInList();
                	}
                	else {
                		loadStatJSON(virtualStatDelegate.Name);
                	}
                		
                }
                               

            }
            catch (Exception e) {
                Log.e(App.TAG, "Error in getListListener: " + e.getMessage());
            }
        }
    };

    /**
     * Update the stat list with live or demo data depending on the connection to eWEB.
     */
    private void updateList() {
        // Get data from eWEB
        EwebConnection eweb = App.getEwebConnection();

        if (App.getDemoMode()) {
            // Get demo values
            getListListener.onCallback(Demo.getAllStats());
        }
        else if (eweb.isConnected()) {
            // Attempt to talk to eWEB
            showLoadingIcon(true);
            App.getStat(null, getListListener);
        }
    }

    /**
     * (Re)sets the list adapter with the items in listItemsArray
     */
    @SuppressWarnings("unchecked")
    private void setListAdapter() {
        // Note, make a clone of the full data list; because the adapter will keep a reference to to data, and that data may become filtered, we want
        // to preserve the original list for a case when we may want to reset it.
        ArrayList<StatListItem> observableData = (ArrayList<StatListItem>) listItemsArray.clone();

        if (listAdapter == null) {
            listAdapter = new StatListItemAdapter(ctx, R.layout.view_summary_list_item_layout, observableData);
            statList.setAdapter(listAdapter);
            statFilter = ((StatListItemAdapter) listAdapter).getFilter();
            listAdapter.updateData(observableData);
        }
        else {
            listAdapter.updateData(observableData);
        }

        applyFilterToList(searchText.getText().toString());
    }

    /**
     * Applies the string as a filter to the items in the list.
     */
    private void applyFilterToList(String s) {
        if (statFilter != null) {
            statFilter.filter(s);
        }
    }

    private void showLoadingIcon(boolean show) {
        if (show) {
            icon_refresh.setImageResource(R.drawable.icon_refresh_loading);
            icon_refresh.startAnimation(AnimationUtils.loadAnimation(ctx, R.animator.login_image_rotate));
        }
        else {
            icon_refresh.setImageResource(R.drawable.icon_refresh);
            icon_refresh.clearAnimation();
        }
    }

    private void loadFirstItemInList() {
        Object o = statList.getItemAtPosition(0);

        if (o instanceof StatListItem) {
            // Get name from the stat list item clicked and use it to load the stat.            
        	String statName = ((StatListItem) o).name;
            loadStatJSON(statName);
        }

    }

    // --------------------------------------------------------------------------------
    // Stat loading functionality
    // --------------------------------------------------------------------------------
    /**
     * Call into the parent activity to hide the stat list (since the parent owns the display of the stat list, it must know how to hide it).
     */
    private void hideStatList() {
        ((SummaryActivity) getActivity()).hideStatList();
    }
    
    /**
     * loadStatJSON Given an id, lookup the stat's JSON String in cache, and load it into the virtual stat delegate.
     * 
     * @param id The ID of the stat to load
     */
    private void loadStatJSON(String name) {
        try {
            String statStr = StatListCache.get(name);

            
            if (statStr == null) {
                loadFirstItemInList();
                return;
             // If no cache, return, cannot load.
            }
                        
            // Save statStr in shared preferences so that it can be used by other activities in the future.
            App.saveStatIntoSharedPreferences(statStr);

            // Load stat with new statStr and start the data refresh.
            virtualStatDelegate.loadFromJSON(statStr);
            virtualStatDelegate.startDataRefresh(0); // get data.
        } 
        catch (Exception e) {
            UIFactory.deltaToast(getActivity(), getString(R.string.error_loading_json), null);
        }
    	
    }

    // --------------------------------------------------------------------------------
    // VirtualStat.UseVirtualStat Interface
    // --------------------------------------------------------------------------------
    @Override
    public void getStatDelegate() {
        Activity activity = getActivity();

        try {
            virtualStatDelegate = ((ExposeVirtualStat) activity).getCurrentStat();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ExposeVirtualStat");
        }
    }

    @Override
    public void updateWithDelegate() {
        // Stat list does not use delegate.
    }
}
