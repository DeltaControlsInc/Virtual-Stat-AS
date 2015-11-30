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
 * App.java
 */

package com.deltacontrols.virtualstat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import com.deltacontrols.eweb.support.api.EwebConnection;
import com.deltacontrols.eweb.support.api.FetchJSON;
import com.deltacontrols.eweb.support.interfaces.GenericCallback;

/**
 * Global Application object: Contains static functionality that pertains to the entire application
 */
public class App extends Application {

    public final static String TAG = "VirtualStat";

    private static Context mContext;
    private static boolean mIsDemo;

    // EWeb connection (only ONE for the entire application)
    private static EwebConnection mEwebConnection;

    public static EwebConnection getEwebConnection() {
        if (mEwebConnection == null) {
            mEwebConnection = new EwebConnection();
        }
        return mEwebConnection;
    }

    // Shared Pref tags
    public static final String SHARED_PREF_ID = "APP_SHARED_PREFS";
    public static final String SHARED_PREF_CURRENT_STAT_JSON = "APP_CURRENT_STAT_JSON";

    public static int LongerReadTimeout = 9000 * 10; 
    
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mIsDemo = false;
    }

    public static Context getContext() {
        return mContext;
    }

    public static AudioManager getAudioManager() {
        return (AudioManager) App.getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    public static void saveStatIntoSharedPreferences(String JSONStr) {
        // Save statStr in shared preferences so that it can be used by other activities in the future.
        SharedPreferences.Editor editor = App.getContext().getSharedPreferences(App.SHARED_PREF_ID, Context.MODE_PRIVATE).edit();
        editor.putString(App.SHARED_PREF_CURRENT_STAT_JSON, JSONStr);
        editor.commit();
    }

    public static void removeStatFromSharedPreferences() {
        SharedPreferences.Editor editor = App.getContext().getSharedPreferences(App.SHARED_PREF_ID, Context.MODE_PRIVATE).edit();
        // editor.clear().commit();
        editor.remove(App.SHARED_PREF_CURRENT_STAT_JSON);
        editor.commit();
    }

    public static void setDemoMode(boolean set) {
        mIsDemo = set;

        if (set) {
            App.removeStatFromSharedPreferences();
        }
    }

    public static boolean getDemoMode() {
        return mIsDemo;
    }

    public static void useCustomDefaultPendingTransition(Activity activity) {
        activity.overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
    }

    /**
     * Mapping containing QERR (quattro errors) and their "human readable counterpart"
     */
    private final static Map<String, String> mErrorMap = new HashMap<String, String>();
    {
        // Temporary solution. Need to figure out how to get around this. It will go to the else case cause mContext is not initialize yet.
        if (mContext != null) {
            mErrorMap.put("QERR_CLASS_OS::QERR_CODE_DEVICE_OFFLINE", mContext.getResources().getString(R.string.network_error_device_offline));
            mErrorMap.put("QERR_CLASS_COMMUNICATION::QERR_CODE_ABORT_TSM_TIMEOUT", mContext.getResources().getString(R.string.network_error_device_offline));
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_ACCESS_DENIED", mContext.getResources().getString(R.string.network_error_unkown_object));
            mErrorMap.put("QERR_CLASS_OBJECT::QERR_CODE_UNKNOWN_OBJECT", mContext.getResources().getString(R.string.network_error_unkown_object));
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_ACCESS_DENIED", mContext.getResources().getString(R.string.network_error_invalid_read_write_permission));
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_WRITE_ACCESS_DENIED", mContext.getResources().getString(R.string.network_error_invalid_write_permission));
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_READ_ACCESS_DENIED", mContext.getResources().getString(R.string.network_error_invalid_read_permission));
            mErrorMap.put("QERR_CLASS_PROPERTY::QERR_CODE_VALUE_OUT_OF_RANGE", mContext.getResources().getString(R.string.network_error_value_out_of_range));
            mErrorMap.put("QERR_CLASS_STATUS::QERR_CODE_IN_PROGRESS", mContext.getResources().getString(R.string.network_error_invalid_setup_or_duplicate_reference));
            mErrorMap.put("QERR_Node Not Found", mContext.getResources().getString(R.string.network_error_unkown_object));
        }
        else {
            mErrorMap.put("QERR_CLASS_OS::QERR_CODE_DEVICE_OFFLINE", "Device Offline");
            mErrorMap.put("QERR_CLASS_COMMUNICATION::QERR_CODE_ABORT_TSM_TIMEOUT", "Device Offline");
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_ACCESS_DENIED", "Unknown Object");
            mErrorMap.put("QERR_CLASS_OBJECT::QERR_CODE_UNKNOWN_OBJECT", "Unknown Object");
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_ACCESS_DENIED", "Invalid permissions, cannot read or write object");
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_WRITE_ACCESS_DENIED", "Invalid permissions, cannot write to object");
            mErrorMap.put("QERR_CLASS_SECURITY::QERR_CODE_READ_ACCESS_DENIED", "Invalid permissions, cannot read from object");
            mErrorMap.put("QERR_CLASS_PROPERTY::QERR_CODE_VALUE_OUT_OF_RANGE", "Value out of range");
            mErrorMap.put("QERR_CLASS_STATUS::QERR_CODE_IN_PROGRESS", "Incorrect stat setup; duplicate object references used"); /* Note: may not be best description */
            mErrorMap.put("QERR_Node Not Found", "Unknown Object");
        }
        
    }

    /**
     * Give a status string, attempt to translate the error code.
     * 
     * @param status The string to translate
     * @return If a match found in the errorMap, then the human readable message will be returned for the status. 
     *         If no match is found, then the original string is returned.
     */
    public static String translateStatus(String status) {
        String result = mErrorMap.get(status);
        return (result == null) ? status : result;
    }

    /**
     * @brief Requests the list of Virtual Stats from eWEB; both the system name, points AND point values are returned from eWEB.
     * @details For now all stats must be prefaced with "vs".
     * @param statName Name of stat to request; if null then all stats will be requested.
     * @param fetchListener Callback to call when the getStat is complete; success or fail, this listener will be called.
     * @return void 
     * <!-- Note: Old method, but still used in Virtual Stat (does not make use of new getSystem API call) <!-- TODO Move functionality into Virtual Stat, since it is more specialized?--> 
     * <!-- Note: Added @SuppressWarnings("unchecked") since in < JDK7 varargs cannot implicitly use generics: 
     * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6227971 Until Android supports JDK7+ keep this suppression. -->
     */
    @SuppressWarnings("unchecked")
    public static void getStat(final String statName, final GenericCallback<FetchJSON.Result> fetchListener) {
        mEwebConnection.getStat(statName, fetchListener, LongerReadTimeout);
    }

    /**
     * @brief Makes the stat name "user friendly" for showing on a UI.
     * @details Handles some of the current "quirks" with systems in eWEB: 
     *          1.  Currently we cannot filter on a system name. Because of this we prepend  "vs_" to a stat system and then filter out all non-stats in the app. 
     *              Therefore we need to remove the "vs_" here to make it more user friendly. 
     *          2.  Currently we can only search for systems based on system name. Because of this, we must put all NFC identifiers in the stat system NAME. 
     *          3.  Currently we cannnot search for a system using a 'partial' name. When using NFC to get system info, the name must (therefore)
     *              contain only the NFC ID (the device's MAC Address) because it is all the information we get from NFC. 
     *          Because of this, we cannot use the system NAME to give us a good 'human readable' stat name. To get around this, we use the description field to store the
     * @param name Full name of stat, as returned from eWEB.
     * @param desc Description of stat, as returned from eWEB; as noted above, when a stat is NFC enabled the Description will contain the desired stat name.
     * @return The string containing a nicely formatted stat name.
     */
    private static String fixupStatName(String name, String desc) {
        if (name == null) {
            return "";
        }

        if (name.startsWith("vs_nfc")) {
            return desc == null ? "" : desc;
        }
        else {
            return name.replaceFirst("vs_", "").replaceAll("_", " ");
        }
    }
}
