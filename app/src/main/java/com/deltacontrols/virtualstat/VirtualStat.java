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
 * VirtualStat.java
 */
package com.deltacontrols.virtualstat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.os.Handler;
import android.util.Log;

import com.deltacontrols.eweb.support.api.EwebConnection;
import com.deltacontrols.eweb.support.interfaces.GenericCallback;
import com.deltacontrols.eweb.support.models.BACnetObjectValue;
import com.deltacontrols.eweb.support.models.BACnetObjectValueList;
import com.deltacontrols.virtualstat.points.BlindsPoint;
import com.deltacontrols.virtualstat.points.FanPoint;
import com.deltacontrols.virtualstat.points.LightsPoint;
import com.deltacontrols.virtualstat.points.OccupancyPoint;
import com.deltacontrols.virtualstat.points.TempPoint;
import com.deltacontrols.virtualstat.points.TimedOverridePoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint;
import com.deltacontrols.virtualstat.points.VirtualStatPoint.SetBy;

/**
 * Contains the overall data model for a Virtual Stat; also contains the logic for syncing the data with the enteliWEB server if a connection is valid. 
 * ExposeVirtualStat, UseVirtualStat and DataSyncListener interfaces allow for the Virtal Stat to be interacted with.
 */
public class VirtualStat {
    // --------------------------------------------------------------------------------
    // Public interfaces
    // --------------------------------------------------------------------------------
    /**
     * Used to allow communication from the fragment to the controller activity; this is done for two reasons: 
     *  1. The data in the currentStat may change at any time (from other fragments, or sync calls); fragments cannot rely on a cached copy of the data. 
     *  2. When a fragment updates a value, the controller must be notified so it can push the changes to other fragments.
     */
    public interface ExposeVirtualStat {
        public VirtualStat getCurrentStat();
    }

    /**
     * Used to allow communication from the activity to the stat fragments; this allows the activity to force fragments to update themselves with the 
     * ExposeVirtualStat delegate. The getStatDelegate is a way of enforcing that all fragments know to use a delegate.
     */
    public interface UseVirtualStat {
        public void getStatDelegate();
        public void updateWithDelegate();
    }

    /**
     * Used to inform a listener that the data within the stat has been updated, or contains an error.
     */
    public interface DataSyncListener {
        public void onDataUpdate();
        public void onStatusUpdate(String status);
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    private static String TAG = "VirtualStat";
    public String Name;

    public TempPoint Temp;
    public TempPoint TempSetpoint;
    public TempPoint OutdoorTemp;
    public LightsPoint Lights1;
    public LightsPoint Lights2;
    public LightsPoint Lights3;
    public LightsPoint Lights4;
    public FanPoint Fan;
    public BlindsPoint Blinds;
    public OccupancyPoint Occupancy;
    public TimedOverridePoint TimedOverride;

    /**
     * VirtualStat can have a single data sync listener. Initialize to empty listener so we do not have to check for existence
     */
    private DataSyncListener syncListener;

    public void setDataSyncListener(DataSyncListener listener) {
        syncListener = listener;
    }

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public VirtualStat() {
        init();
    }

    /**
     * Used mostly in simplifying unit tests; does not set Fan.override.
     */
    public VirtualStat(String statName, String statTempActual, String statTempSP,
            String statLights1, String statLights2, String statLights3, String statLights4,
            String statFan, String statBlinds) {
        init();
        Name = statName;
        Temp = new TempPoint(statTempActual);
        TempSetpoint = new TempPoint(statTempSP);
        Fan = new FanPoint(statFan);
        Lights1 = new LightsPoint(statLights1);
        Lights2 = new LightsPoint(statLights2);
        Lights3 = new LightsPoint(statLights3);
        Lights4 = new LightsPoint(statLights4);
        Blinds = new BlindsPoint(statBlinds);

    }

  public VirtualStat(String statName, String statTempActual, String statTempSP, String statOutdoorTemp,
                     String statLights1, String statLights2, String statLights3, String statLights4,
                     String statFan, String statBlinds, String statOccupancy, String statTimedOverride) {
        init();
        Name = statName;
        Temp = new TempPoint(statTempActual);
        TempSetpoint = new TempPoint(statTempSP);
        OutdoorTemp = new TempPoint(statOutdoorTemp);
        Fan = new FanPoint(statFan);
        Lights1 = new LightsPoint(statLights1);
        Lights2 = new LightsPoint(statLights2);
        Lights3 = new LightsPoint(statLights3);
        Lights4 = new LightsPoint(statLights4);
        Blinds = new BlindsPoint(statBlinds);
        Occupancy = new OccupancyPoint(statOccupancy);
        TimedOverride = new TimedOverridePoint(statTimedOverride);
  }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Initializes properties; all points in VirtualStat will be initialized with default objects (should not be null).
     */
    public void init() {
        Name = "";

        Temp = new TempPoint();
        TempSetpoint = new TempPoint();
        OutdoorTemp = new TempPoint();
        Lights1 = new LightsPoint();
        Lights2 = new LightsPoint();
        Lights3 = new LightsPoint();
        Lights4 = new LightsPoint();
        Fan = new FanPoint();
        Blinds = new BlindsPoint();
        Occupancy = new OccupancyPoint();
        TimedOverride = new TimedOverridePoint();
    }

    /**
     * Since VirtualStat "Lights" consists of up to 4 different lights, this function allows a caller to easily get access to the points based on a pseudo index.
     */
    public LightsPoint getLights(int index) {
        LightsPoint light;

        switch (index) {
            case 0:
                light = this.Lights1;
                break;
            case 1:
                light = this.Lights2;
                break;
            case 2:
                light = this.Lights3;
                break;
            case 3:
                light = this.Lights4;
                break;
            default:
                light = null;
        }
        return light;
    }

    /**
     * Common setValue method; will apply the point value and then start the VirtualStat's sync timer which when run will sync the values with 
     * the enteliWEB if a connection exists.
     * 
     * @param point The point object to update
     * @param newValue The new value for the point
     */
    public void setValue(VirtualStatPoint point, String newValue) {
        if (point.setValue(newValue, VirtualStatPoint.SetBy.USER)) {
            startSyncTimer();
        }
    }

    /**
     * Exposes the ability to force an update to enteliWEB if a connection exists. NOTE This currently exists because the Fan point behaves differently 
     * and does not use the VirtualStat.setValue method.
     */
    public void forcePut() {
        startSyncTimer();
    }

    /**
     * Given a JSON string containing the meta data for an entire Virtual Stat system, load the stat and all of it's required points.
     * 
     * @param jsonStr JSON String as expected from the enteliweb/api/views service.
     */
    public void loadFromJSON(String jsonStr) {
        try {
        	JSONObject jsonObj = new JSONObject(jsonStr);        	
            loadFromJSON(jsonObj);
        } 
        catch (Exception e) {
            Log.e(App.TAG, "loadFromJSON: " + e.getMessage());
        }
    }

    /**
     * Given a JSONObject containing the meta data for an entire Virtual Stat system, load the stat and all of it's required points.
     * 
     * @param statJSON
     *            JSONObject as expected from the enteliweb/api/views/Virtual+Stat service.
     */
    public void loadFromJSON(JSONObject statJSON) {
        /* Expected format of JSONObject
        April's Virtual Stat: {
        $base: "Collection"
            April's Virtual Stat: {
            $base: "Struct"
                nodeType: "System"
                location: {...}-
                address: {...}-
                description: {...}-
                template: {...}-
                Objects: {
                    $base: "Struct"
                    BLINDS: {
                    $base: "Enumerated"
                    value: "inactive"
                    displayName: "PAC_Office_01_VAV WS"
                    description: ""
                    physical: "/.bacnet/enteliVIZ5 Training/20101/binary-value,1"
                    }-
                    FAN: {
                    $base: "Unsigned"
                    value: "1"
                    displayName: "PAC_Office_01_VAV AirFlowControlMode"
                    description: ""
                    physical: "/.bacnet/enteliVIZ5 Training/20101/multi-state-value,1"
                    }
                }
                .....
            }
        }
        */

        // Re-init the stat as we are loading in new values.
        init();

        @SuppressWarnings("unchecked")
        Iterator<String> iter = statJSON.keys();
        String systemName = (iter.hasNext()) ? iter.next() : null;
        JSONObject json;

        if (systemName == null) {
            // Invalid JSON
            return;
        }

        // Parse out system info
        try {
            json = statJSON.getJSONObject(systemName);
            this.Name = systemName;
            //this.ID = json.getString("id");
        } catch (Exception e) {
            json = new JSONObject();
            Log.e(App.TAG, "Error parsing Stat JSON: " + e.getMessage());
        }

        // Parse out system points
        String[] eles = new String[] { "TEMP", "TEMP_SP", "OUTDOOR_TEMP", "LIGHTS1", "LIGHTS2", "LIGHTS3", "LIGHTS4", "FAN", "BLINDS", "OCCUPANCY", "TIMED_OVERRIDE" };
        VirtualStatPoint points[] = { this.Temp, this.TempSetpoint, this.OutdoorTemp, this.Lights1, this.Lights2, this.Lights3, this.Lights4, this.Fan, this.Blinds, this.Occupancy, this.TimedOverride };
        VirtualStatPoint point;
        JSONObject pointObj;
        String resultStatus = "OK";
        String value;

        for (int i = 0; i < eles.length; i++) {
            try {
                point = points[i];
                pointObj = json.getJSONObject(eles[i]);
            } 
            catch (Exception e) {
                Log.e(App.TAG, "Json point - " + e.getMessage());
                continue;
            }

            // Load each point via its JSON
            if (pointObj instanceof JSONObject) {
                point.loadFromJSON(pointObj);
                value = point.getValue();

                // Check for error: old method - value contains QERR
                if (value.startsWith("QERR")) {
                    resultStatus = value;
                }
                // New method: JSON contains errorText property
                else if (pointObj.has("errorText")) {
                    try {
                        resultStatus = "QERR_" + pointObj.getString("errorText");
                    	//resultStatus = "QERR";                        
                        point.setValue(resultStatus, SetBy.SYSTEM); // Update value so it will get displayed correctly
                        
                    } catch (Exception e) {
                        Log.e(App.TAG, "Error loading error text");
                    }

                }
            }
        }

        // Get fan override (slightly different, FanPoint contains logic for controlling it, not the Virtual Stat)
        try {
            this.Fan.override.loadFromJSON(json.getJSONObject("FAN_OVERRIDE"));
        } 
        catch (Exception e) {
            Log.e(App.TAG, "Error loading fan json");
        }

        // Call listener functions
        if (syncListener != null) {
            syncListener.onStatusUpdate(resultStatus);
            syncListener.onDataUpdate();
        }
    }

    /**
     * Creates a JSON String comparable to the JSON returned by enteliweb/api/views; some attributes, including $base, are omitted.
     * 
     * @return The string containing the system JSON representation of the Virtual Stat
     */
    public String createSystemJSONString() {
        /*  Current format as returned from the eWEB web service. 
        {   Vstat - e0 02 58 72 69 8a 63 30: 
            {   $base: "Struct",
                id: "3b2fc372-da99-11e2-ba9b-0026b978baa8",
                location: "",
                address: "",
                template: "vstat.xml",
                
                TEMP_SP: {
                    $base: "Real",
                    id: "69244441-1fd5-11e3-9932-0026b978baa8",
                    type: "Object",
                    ref: "//MainSite/5600.AO591",
                    value: "95"
                },
                TEMP: {
                    $base: "Real",
                    id: "6924444d-1fd5-11e3-9932-0026b978baa8",
                    type: "Object",
                    ref: "//MainSite/5600.AO591",
                    value: "95"
                },
                ...
            }
        }
        */

        String[] args = new String[] { this.Name,
                "TEMP", this.Temp.getDataType(),  this.Temp.getValue(), this.Temp.getFullRef(), this.Temp.getName(),
                "TEMP_SP", this.TempSetpoint.getDataType(), this.TempSetpoint.getValue(), this.TempSetpoint.getFullRef(), this.TempSetpoint.getName(),
                "OUTDOOR_TEMP", this.OutdoorTemp.getDataType(), this.OutdoorTemp.getValue(), this.OutdoorTemp.getFullRef(), this.OutdoorTemp.getName(),
                "LIGHTS1", this.Lights1.getDataType(), this.Lights1.getValue(), this.Lights1.getFullRef(), this.Lights1.getName(),
                "LIGHTS2", this.Lights2.getDataType(), this.Lights2.getValue(), this.Lights2.getFullRef(), this.Lights2.getName(),
                "LIGHTS3", this.Lights3.getDataType(), this.Lights3.getValue(), this.Lights3.getFullRef(), this.Lights3.getName(),
                "LIGHTS4", this.Lights4.getDataType(), this.Lights4.getValue(), this.Lights4.getFullRef(), this.Lights4.getName(),
                "FAN", this.Fan.getDataType(), this.Fan.getValue(), this.Fan.getFullRef(), this.Fan.getName(),
                "FAN_OVERRIDE", this.Fan.override.getDataType(), this.Fan.override.getValue(), this.Fan.override.getFullRef(), this.Fan.override.getName(),
                "BLINDS", this.Blinds.getDataType(), this.Blinds.getValue(), this.Blinds.getFullRef(), this.Blinds.getName(),
                "OCCUPANCY", this.Occupancy.getDataType(), this.Occupancy.getValue(), this.Occupancy.getFullRef(), this.Occupancy.getName(),
                "TIMED_OVERRIDE", this.Occupancy.getDataType(), this.Occupancy.getValue(), this.Occupancy.getFullRef(), this.Occupancy.getName()
        };

        // Note, only format the parts we care about parsing back out.
        String jsonStr = String.format("{ \"%s\": { " + // name
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // TEMP
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // TEMP_SP
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // OUTDOOR_TEMP
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS1
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS2
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS3
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // LIGHTS4
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // FAN
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }, " + // FAN_OVERRIDE
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" },  " + // BLINDS
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" },  " + // OCCUPANCY
                "%s: { $base: \"%s\", value: \"%s\", physical: \"%s\", displayName: \"%s\" }  " + // TIMED_OVERRIDE
                "}}", (Object[]) args);

        //Log.e(App.TAG, " JSon: " + jsonStr);
        return jsonStr;
    }

    /**
     * Returns an ArrayList<VirtualStatPoint> of non-empty points in the stat.
     * 
     * @param onlyDirty If true then only points marked as dirty will be returned; if false, all points returned. Empty point references not returned.
     * @return ArrayList of points depending on the onlyDirty flag.
     */
    public ArrayList<VirtualStatPoint> getPoints(boolean onlyDirty) {
        String tempRef;
        ArrayList<VirtualStatPoint> points = new ArrayList<VirtualStatPoint>();
        ArrayList<VirtualStatPoint> all = new ArrayList<VirtualStatPoint>(Arrays.asList(Temp, TempSetpoint, OutdoorTemp,
                Lights1, Lights2, Lights3, Lights4, Fan, Fan.override, Blinds));

        for (VirtualStatPoint point : all) {
            tempRef = point.getFullRef();

            // If ref not empty AND we want a dirty value (and value is dirty) or we want all values then add to list.
            if (!tempRef.isEmpty() && ((onlyDirty && point.isDirty) || (!onlyDirty))) {
                points.add(point);
            }
        }

        return points;
    }

    /**
     * toBACnetObjectValueList
     */
    public BACnetObjectValueList toBACnetObjectValueList(boolean onlyDirty) {
        BACnetObjectValueList result = new BACnetObjectValueList();

        String tempRef;

        ArrayList<VirtualStatPoint> all = new ArrayList<VirtualStatPoint>(Arrays.asList(Temp, TempSetpoint, OutdoorTemp,
                Lights1, Lights2, Lights3, Lights4, Fan, Fan.override, Blinds));

        for (VirtualStatPoint point : all) {
            tempRef = point.getFullRef();

            // If ref not empty AND we want a dirty value (and value is dirty) or we want all values then add to list.
            if (!tempRef.isEmpty() && ((onlyDirty && point.isDirty) || (!onlyDirty))) {
                BACnetObjectValue obj = new BACnetObjectValue(tempRef);
                obj.via.property = "present-value"; // For now ALWAYS set to present-value of the object
                obj.value = point.getValue();
                obj.dataType = point.getDataType();
                result.put(tempRef, obj);
            }
        }

        return result;
    }

    /**
     * Returns an ArrayList<String> of non-empty references in the stat.
     * 
     * @param onlyDirty If true then only points marked as dirty will be returned; if false, all points returned. Empty point references not returned.
     * @return ArrayList of points depending on the onlyDirty flag.
     */
    public ArrayList<String> getFullRefs(boolean onlyDirty) {

        ArrayList<String> refs = new ArrayList<String>();
        ArrayList<VirtualStatPoint> points = getPoints(onlyDirty);
        String fullRef;

        for (VirtualStatPoint point : points) {
            fullRef = point.getFullRef();

            if (!refs.contains(fullRef)) {
                refs.add(fullRef);
            }
        }

        return refs;
    }

    /**
     * Sets all stat object's dirty flags to false
     */
    public void markAllAsNonDirty() {
        Temp.isDirty = false;
        TempSetpoint.isDirty = false;
        OutdoorTemp.isDirty = false;
        Lights1.isDirty = false;
        Lights2.isDirty = false;
        Lights3.isDirty = false;
        Lights4.isDirty = false;
        Fan.isDirty = false;
        Blinds.isDirty = false;
        Occupancy.isDirty = false;
        TimedOverride.isDirty = false;
    }

    /**
     * Sets all stat object's dirty flags to true
     */
    public void markAllAsDirty() {
        Temp.isDirty = true;
        TempSetpoint.isDirty = true;
        OutdoorTemp.isDirty = true;
        Lights1.isDirty = true;
        Lights2.isDirty = true;
        Lights3.isDirty = true;
        Lights4.isDirty = true;
        Fan.isDirty = true;
        Blinds.isDirty = true;
        Occupancy.isDirty = true;
        TimedOverride.isDirty = true;
    }

    /**
     * Sets all stat object's dirty flags to false, and then restores all values to their old value.
     */
    public void restoreAllToLastKnownValue() {
        markAllAsNonDirty();
        Temp.restoreOldValueIfNotDirty(); // Shouldn't be able to change, but set just in case.
        TempSetpoint.restoreOldValueIfNotDirty();
        OutdoorTemp.restoreOldValueIfNotDirty();
        Lights1.restoreOldValueIfNotDirty();
        Lights2.restoreOldValueIfNotDirty();
        Lights3.restoreOldValueIfNotDirty();
        Lights4.restoreOldValueIfNotDirty();
        Fan.restoreOldValueIfNotDirty();
        Blinds.restoreOldValueIfNotDirty();
        Occupancy.restoreOldValueIfNotDirty();
        TimedOverride.restoreOldValueIfNotDirty();
    }

    /**
     * Creates the PropertyList XML for the stat which is used in {@link EwebConnection#putProperty(String, com.deltacontrols.virtualstat.api.FetchXML.Listener)}. 
     * The XML will contain each object
     * reference with it's most current value.
     * 
     * @param onlyDirty If true, the XML will contain only objects that have been changed by the user and not sent to eWEB (ie. are dirty).
     * @return XML String for the PropertyList
     */
    public String asXMLStr(boolean onlyDirty) {
        StringBuffer xml = new StringBuffer();

        // NOTE: If the same reference is used for multiple virtualStatPoints, the XML will
        //  contain multiple requests for the same properties; this results in a QERR result that
        //  will cause the update to fail. This is likely to be extremely uncommon, but check is
        //  put here for saftey. No guarantees made on which object's value will be 'put'.
        // NOTE: May want to simply remove this since the case is so uncommon.

        // NOTE: Do not return Temp - it is intended to be read only.
        VirtualStatPoint[] points = new VirtualStatPoint[] { TempSetpoint, Lights1, Lights2, Lights3, Lights4, Fan, Fan.override, Blinds, Occupancy, TimedOverride };
        VirtualStatPoint point;

        // Do not need XML tag? "<?xml version=\"1.0\" encoding\"UTF-8\"?>";
        xml.append("<PropertyList>");
        for (int i = 0; i < points.length; i++) {
            point = points[i];
            // Only include points that are setup
            if ((!point.getFullRef().equals(""))
                    && (!point.getValue().startsWith("QERR"))
                    && (!point.getValue().equals(VirtualStatPoint.NotInitializedString))) {
                xml.append(point.asXMLStr());
            }
        }
        xml.append("</PropertyList>");

        return xml.toString();
    }

    // --------------------------------------------------------------------------------
    // Data Syncing Functionality
    // Was originally in SingleStatControlActivity, but moved here so syncing can be done on different activities.
    // --------------------------------------------------------------------------------
    private static enum SyncState {
        IDLE, PENDING_REQUEST, REQUEST_SENT, REQUEST_RECIEVED
    };

    private static SyncState getState = SyncState.IDLE;
    private static SyncState putState = SyncState.IDLE;

    // -------------------------------------------------------------------------------------
    // Variables for authentication retry functionality
    // -------------------------------------------------------------------------------------
    private int backgroundFetchRetry = MaxBackgroundFetchRetry; // Keep track on how many retry on authentication
    private static int MaxBackgroundFetchRetry = 3;

    // -------------------------------------------------------------------------------------
    // Variables for 'put' functionality
    // -------------------------------------------------------------------------------------
    private Timer syncTimer; // Timer controlling the put sync with eWEB
    private long syncTimerDelay = 1000; // Delay before sending the put (in milliseconds)

    // -------------------------------------------------------------------------------------
    // Variables for 'get' functionality
    // -------------------------------------------------------------------------------------
    // private boolean isGettingData = false; // Flags when a get is in process
    private boolean ignoreNextGet = false; // Flags when we know the get response may be stale

    Handler refreshHandler = new Handler(); // Handler for the refresh functionality
    private final long refreshTimeout = 10000; // Refresh timeout in milliseconds
    private Runnable refreshDataTask = new Runnable() { // Runnable task executed by the handler via timeout
        @Override
        public void run() {
            EwebConnection eweb = App.getEwebConnection();

            BACnetObjectValueList refs = toBACnetObjectValueList(false); // Get all
            if (refs.size() > 0) {
                putState = SyncState.REQUEST_SENT;
                eweb.getMulti(refs, getPropertyListener);
            }
        }
    };

    // -------------------------------------------------------------------------------------
    // Get (Refresh) functionality
    // -------------------------------------------------------------------------------------
    public void startDataRefresh(long delayMs) {
        // If demo, do not do data refresh
        if (App.getDemoMode()) {
            return;
        }

        // If 1) no one currently wants data, or 2) we are already making a request, then do not allow request to be made.
        if ((syncListener == null) || (getState == SyncState.REQUEST_SENT)) {
            return;
        }

        getState = SyncState.PENDING_REQUEST;
        refreshHandler.removeCallbacks(refreshDataTask);
        refreshHandler.postDelayed(refreshDataTask, delayMs);
    }

    public void stopDataRefresh() {
        getState = SyncState.IDLE;
        refreshHandler.removeCallbacks(refreshDataTask);
    }

    private GenericCallback<BACnetObjectValueList> getPropertyListener = new GenericCallback<BACnetObjectValueList>() {
        @Override
        public void onCallback(BACnetObjectValueList result) {

            getState = SyncState.REQUEST_RECIEVED;
            Long nextRefresh = refreshTimeout; // Set refresh to the default timeout, may change depending on state of response
            String resultStatus = "OK";

            try {            	
            	Log.i(TAG, "Get Multi StatusCode: " + result.statusCode);
                // Handle errors in network response
                if (result.statusCode != 200) {
                    switch(result.statusCode) {
                        case 407: {
                            resultStatus = "Authentication issue"; // login issue
                            break;
                        }

                        case 401: {
                            resultStatus = "Unauthorized"; // User authentication failure
                            break;
                        }

                        case 403: {
                            resultStatus = "ForbiddenCall";  // Invalid request
                            break;
                        }

                        default: {
                            resultStatus = "HttpHostConnectException"; // network connection issue
                            break;
                        }
                    }// switch(result.statusCode)

                    if (backgroundFetchRetry > 0) {
                        backgroundFetchRetry--;
                        // Call listener functions
                        if (syncListener != null) {
                            syncListener.onStatusUpdate(resultStatus);
                        }
                    } else {
                        // Stop retry and initialize back to 3 for next time
                        backgroundFetchRetry = MaxBackgroundFetchRetry;
                        // Call listener functions
                        if (syncListener != null) {
                            syncListener.onStatusUpdate(resultStatus);
                        }
                        // Stop background data fetch
                        return;
                    }

                } // if (result.statusCode != 200)
                // Handle stale response (ie. if put request has gone out before this response has come back)
                else if (ignoreNextGet) {
                    ignoreNextGet = false;
                    nextRefresh = 0l; // Start new refresh immediately
                    backgroundFetchRetry = MaxBackgroundFetchRetry;
                }
                // Handle valid response, parse through point values to determine errors (if any)
                // Should only get here if result contains actual values (although may be invalid points, so may contain QERRS)
                else {
                    ArrayList<VirtualStatPoint> points = getPoints(false);
                    String value;
                    String errorText = null;
                    backgroundFetchRetry = MaxBackgroundFetchRetry;

                    for (VirtualStatPoint point : points) {
                        // Set value

                        String ref = point.getFullRef();
                        ref = ref.toLowerCase();
                        if (result.containsKey(ref)) {
                            value = result.get(ref).value;
                            errorText = result.get(ref).errorText;
                        }
                        else {
                            value = null;
                        }

                        if (errorText != null){
                            Log.i(TAG, "errorText: " + errorText.toString());
                        }

                        if (value == null) {
                            continue;
                        } // If value null, then the response did not contain a value for the point.

                        // Handle value error
                        if (value.startsWith("QERR")) {
                            resultStatus = value;
                        }

                        // Update even when value in error
                        point.setValue(value, SetBy.SYSTEM);
                    }

                    // Call listener functions
                    if (syncListener != null) {
                        syncListener.onStatusUpdate(resultStatus);
                        syncListener.onDataUpdate(); // Always called? Even on network error?
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "Exception: " + e.getMessage());
            }

            // Start next data refresh if desired
            if (nextRefresh != null) {
                startDataRefresh(nextRefresh);
            }
        }
    };

    // -------------------------------------------------------------------------------------
    // Put Property Functionality
    // -------------------------------------------------------------------------------------
    private void createSyncTimer() {
        // Do not sync with eweb
        putState = SyncState.PENDING_REQUEST;

        syncTimer = new Timer();
        syncTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onSyncTimeout();
            }
        }, syncTimerDelay);
    }

    private void startSyncTimer() {
        stopDataRefresh(); // Stop any calls to refresh data (since the put should override the results long term)

        if (syncTimer != null) {
            syncTimer.cancel(); // Cancel old timer
        }
        createSyncTimer();
    }

    private void onSyncTimeout() {
    	BACnetObjectValueList putObjectValueList;
    	
        EwebConnection eweb = App.getEwebConnection();

        if (syncListener != null) {
            syncListener.onDataUpdate();
        }

        // If demo, do not do data refresh
        if (App.getDemoMode()) {
            putState = SyncState.IDLE; // Jump right to idle, we did not actually have to send.
            return;
        }

        putState = SyncState.REQUEST_SENT;

        try {
            // Put data(
            GenericCallback<BACnetObjectValueList> fetchListener = new GenericCallback<BACnetObjectValueList>() {
                @Override
                public void onCallback(BACnetObjectValueList result) {
                	String resultStatus = "OK";
                	
                	Log.e(App.TAG,  "Put Multi");
                    putState = SyncState.REQUEST_RECIEVED;                    
                    
                    // Handle errors in network response
                    try {
                    	Log.i("Tag", "Put Multi Status: " + result.statusCode);
	                    if (result.statusCode != 200) {
                            switch(result.statusCode){
                                case 407: {
                                    resultStatus = "Authentication issue"; // login issue
                                    break;
                                }

                                case 401: {
                                    resultStatus = "Unauthorized";  // User authentication failure
                                    break;
                                }

                                case 403: {
                                    resultStatus = "ForbiddenCall";  // Invalid request
                                    break;
                                }

                                default: {
                                    resultStatus = "HttpHostConnectException"; // network connection issue
                                    break;
                                }
                            } // switch(result.statusCode)
                            if (backgroundFetchRetry > 0) {
                                backgroundFetchRetry--;
                                // Call listener functions
                                if (syncListener != null) {
                                    syncListener.onStatusUpdate(resultStatus);
                                }
                            }
                            else {
                                // Stop retry and initialize back to 3 for next time
                                backgroundFetchRetry = MaxBackgroundFetchRetry;
                                // Call listener functions
                                if (syncListener != null) {
                                    syncListener.onStatusUpdate(resultStatus);
                                }
                                // Stop background data fetch
                                return;
                            }

	                    } // if (result.statusCode != 200)
	                    else {
	                        showResponseFeedback(result);
	                    }
                    }
                    catch (Exception e) {
                    	Log.i(TAG, "Exception: " + e.getMessage());
                    }
                                        
                    // Restart the data refresh.
                    startDataRefresh(10000); // Need to give time for the value to update (especially if dealing with change to auto).
                    putState = SyncState.IDLE;
                }
            };

            // We are in the middle of getting data from eWEB, but we are about to send in a putProperty. This means we want to ignore
            // the next get result as it will contain stale data.
            if (getState == SyncState.REQUEST_SENT) {
                ignoreNextGet = true;
            }

            // Send changes (dirty) to eWEB and then mark as non-dirty
            putObjectValueList = this.toBACnetObjectValueList(true);
            if (!putObjectValueList.isEmpty())
            {
	            eweb.putMulti(this.toBACnetObjectValueList(true), fetchListener);
	            this.markAllAsNonDirty();
            }

        } catch (Exception e) {
            Log.v(TAG, e.getMessage());
        }

    }

    /**
     * Helper function which takes an text node and returns its text value.
     * 
     * @param node Document text node.
     * @return The text string parsed out of the text node.
     */
    private String getXMLTextValue(Node node) {
        NodeList nl = node.getChildNodes(); // Text node is actually the first child node.
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < nl.getLength(); i++) {
            Node textChild = nl.item(i);
            if (textChild.getNodeType() != Node.TEXT_NODE) {
                buf.append("");
                continue;
            }
            buf.append(textChild.getNodeValue());
        }
        return buf.toString();
    }

    /**
     * Based on old function updateWithXMLResponse, but only parses XML response looking for status errors
     * 
     * @param result The result XML from {@link EwebConnection#putProperty(String, com.deltacontrols.virtualstat.api.FetchXML.Listener)} 
     *  Note: We cannot rely on the values returned in the put request as they are (?) simply a copy of the value we attempted to put.
     *  This means when going into auto, the value returned is always empty so we do not know the true auto value at that moment.
     *  Note: If we can change the eWEB API such that a put will always return the current value, then we could grab that value here instead of forcing a second get.
     */
    private void showResponseFeedback(BACnetObjectValueList result) {
        String responseStatus = "OK";
        boolean returnData = true;
        String ref;

        // Points should index into the BACnetObjectValueList map.
        ArrayList<VirtualStatPoint> points = this.getPoints(false);

        if (result.errorText != null) {
            // Network error may have occurred.
            responseStatus = result.errorText;
            returnData = false;
        }
        else {
            // Handle valid response
            for (int i = 0; i < points.size(); i++) {
                ref = points.get(i).getFullRef();

                if (result.containsKey(ref)) {
                    // If error found with point, then restore to the old value assuming the user
                    // has not made another (unsynched) change.
                    if (result.get(ref).errorText != null) {
                        responseStatus = result.get(ref).errorText;
                        points.get(i).restoreOldValueIfNotDirty();
                    }
                }
            }
        }

        if (syncListener != null) {
            // Call listener functions
            syncListener.onStatusUpdate(responseStatus);

            if (returnData) {
                syncListener.onDataUpdate();
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // Static methods
    // -------------------------------------------------------------------------------------
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate a value suitable for use in {@link #setId(int)}. This value will not collide with ID values generated at build time by aapt for R.id.
     * 
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF)
                newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static String getRefValueFromJSON(String ref, JSONObject jObj, String property) {
        String result = null;

        try {
            JSONObject refInfo = jObj.getJSONObject(ref);
            result = refInfo.getString(property);
        } 
        catch (Exception e) {
            Log.e(App.TAG, "getRefValueFromJSON parse error: " + e.getMessage());
        }

        return result;
    }
}