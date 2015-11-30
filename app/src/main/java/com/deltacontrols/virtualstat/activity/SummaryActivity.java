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
 * SummaryActivity.java
 */

package com.deltacontrols.virtualstat.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.LoginInfo;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.UseVirtualStat;
import com.deltacontrols.virtualstat.controls.AlertWindow;
import com.deltacontrols.virtualstat.controls.ToggleBar;
import com.deltacontrols.virtualstat.controls.ToggleBar.ToggleInteractionListener;
import com.deltacontrols.virtualstat.fragment.SingleStatSummaryFragment;
import com.deltacontrols.virtualstat.fragment.StatListFragment;
import com.deltacontrols.virtualstat.nfc.NFCHelper;

/**
 * Activity controlling the summary page; the summary page consists of both a list of virtual stats, as well as a summary 
 * view of the points and values within a selected stat.
 */
public class SummaryActivity extends FragmentActivity implements VirtualStat.ExposeVirtualStat {

    final int version = Integer.valueOf(Build.VERSION.SDK_INT);
    private VirtualStat currentStat; // Contains the data model for the currently selected stat
    private Context ctx;
    private int screenSize;
    private NFCHelper mNFCHelper;

    // Outlets
    RelativeLayout mainPageViewGroup;
    TextView statNameText;
    TextView statusText;
    AlertWindow alertWindow;
    FrameLayout fragment_content;   // Point fragment (ie. Temperature)
    FrameLayout fragment_switcher;  // Tab fragment

    // --------------------------------------------------------------------------------
    // Life cycle
    // --------------------------------------------------------------------------------
    /**
     * Creates the view for the summary page; loads any saved stat and listens for changes to the stat.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Attempt to read from NFC before loading the view since we may be launching a new intent right from here.
        mNFCHelper = new NFCHelper(getApplicationContext(), this);

        // Create new VirtualStat
        ctx = App.getContext();
        currentStat = new VirtualStat();
        currentStat.setDataSyncListener(dataListener);

        // Setup outlets
        mainPageViewGroup = (RelativeLayout) findViewById(R.id.mainPageViewGroup);
        statNameText = (TextView) findViewById(R.id.statName);
        statusText = (TextView) findViewById(R.id.statStatusText);
        alertWindow = (AlertWindow) findViewById(R.id.alertWindow);
        fragment_content = (FrameLayout) findViewById(R.id.fragment_content);
        fragment_switcher = (FrameLayout) findViewById(R.id.fragment_switcher);

        findViewById(R.id.alertMessageLayout).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertWindow.showAlert(getString(R.string.network_reconnect_status), false);
                        currentStat.startDataRefresh(0);
                    }
                });

        // Grab the instance of TabFragment that was included with the layout and have it launch the initial tab.
        // Note, use FrameLayout so that the fragment loads AFTER we have the stat setup (if Fragment set
        // directly in XML, then its OnCreate is called before we get here and the virtualStatDelegate will be null.
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_content, new SingleStatSummaryFragment());
        ft.replace(R.id.fragment_switcher, new StatListFragment());
        ft.commit();

        // Setup toggler logic (to 'open' and 'close' the stat list fragment)
        ToggleBar switcher_toggle = (ToggleBar) findViewById(R.id.switcher_toggle);
        if (switcher_toggle != null) {
            switcher_toggle.setToggleInteractionListener(new ToggleInteractionListener() {
                @Override
                public void onTap() {
                    if (fragment_switcher.getVisibility() == View.GONE) {
                        showStatList();
                    }
                    else {
                        hideStatList();
                    }
                }

                @Override
                public void onSwipeUp() {
                    showStatList();
                }

                @Override
                public void onSwipeDown() {
                    hideStatList();
                }
            });
        }

        // Get screen size bucket; used to determine how to animate the stat list.
        // May not be the most ideal way to do this, but summary page requires different layouts based on screen sizes.
        Configuration config = ctx.getResources().getConfiguration();
        screenSize = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);

        // Set custom font
        UIFactory.setCustomFont(this, findViewById(R.id.mainPageViewGroup));

        // Setup context menu
        final ImageView overflow_icon = (ImageView) findViewById(R.id.overflow_icon);
        if (overflow_icon != null) {
            overflow_icon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup(v);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNFCHelper.enableForegroundDispatch(); // Setup NFC dispatch

        // Load data from shared prefs
        loadStat();

        if (!currentStat.Name.isEmpty()) {
            hideStatList();
            updateFragments();
            currentStat.startDataRefresh(0);
        }
    }

    public void onPause() {
        super.onPause();
        mNFCHelper.disableForegroundDispatch();

        if (!currentStat.Name.isEmpty()) {
            App.saveStatIntoSharedPreferences(currentStat.createSystemJSONString());
            currentStat.stopDataRefresh();
        }

    }

    public void onStop() {
        super.onStop();
    }

    public void onDestroy() {
        super.onDestroy();
        this.currentStat.setDataSyncListener(null);
        this.currentStat = null;
    }

    /*
     * (non-Javadoc) This function is called when the intent has been launched via a new intent; this could be the case if the login screen already 
     * active and then another NFC 'tap' was discovered. The second tap will call this onNewIntent - so we want to check the intent to make sure this 
     * is the case, and then pull out any NFC data we may discover.
     * 
     * @note For this to work correctly, the activity must be set to singleInstance in the manifest file.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        // Called on 'back' or on NFC launch
        super.onNewIntent(intent);
        checkNFCLaunch(intent);
    }

    private void checkNFCLaunch(Intent intent) {
        String nfcID = mNFCHelper.getIDFromIntent(intent);
        if (nfcID != null) {
            // Launch single stat activity without logging in to eweb
            Intent startIntent = new Intent(ctx, NFCFetchActivity.class);
            startIntent.putExtra(NFCFetchActivity.INTENT_UUID, nfcID);
            startActivity(startIntent);
            App.useCustomDefaultPendingTransition(this);
        }
    }

    // --------------------------------------------------------------------------------
    // Menu tricks
    // --------------------------------------------------------------------------------
    @SuppressLint("NewApi")
    private void showPopup(View v) {
        try {
            PopupMenu popup = new PopupMenu(this, v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.context_menu_summary, popup.getMenu());
            popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return handleMenuSelect(item);
                }
            });
            popup.show();
        }
        // Note: Usually bad practice to catch NoClassDefFoundError, however, I am using this as a safety net to guard against
        // old APIs that do not support PopupMenu.
        // Option: The android-support-v7-appcompat library is an option, which requires a more complex project build setup due to resources:
        // * http://developer.android.com/tools/support-library/setup.html#add-library
        // * http://stackoverflow.com/questions/17975002/android-v7-support-library-popup-menu
        // Currently: Catch the no class error, and simply pop up the menu as a floating context menu instead. This does not require the v7 support lib.
        // since this is technically an example application I did not want to clutter it with the external library dependency.
        catch (NoClassDefFoundError e) {
            registerForContextMenu(v);
            openContextMenu(v);
        }
    }

    private boolean handleMenuSelect(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.logout:
            logoutAction();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void logoutAction() {
        App.getEwebConnection().disconnect();
        LoginInfo.logoutCurrentUser();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    // --------------------------------------------------------------------------------
    // Overridden methods
    // --------------------------------------------------------------------------------
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_summary, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return handleMenuSelect(item);
    }

    /**
     * Override onKeyDown to listen for the back button event; override back button to act like home button.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * 'Shows' the list of stats Animate show if viewing on < Large screen sizes. 
     * Note: On larger devices the stat list is put in a weighted layout, animating this causes 'jumps' in the UI.
     */
    public void showStatList() {
        if (fragment_switcher.getVisibility() == View.GONE) {
            if (screenSize < Configuration.SCREENLAYOUT_SIZE_LARGE) {
                Animation bottomUp = AnimationUtils.loadAnimation(ctx, R.animator.slide_bottom_up);
                fragment_switcher.startAnimation(bottomUp);
            }

            fragment_switcher.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 'Hides' the list of stats. Note: The UI 'jump' does not have a large impact when hiding the list, so animate hide for all screen sizes,
     */
    public void hideStatList() {
        if (fragment_switcher.getVisibility() == View.VISIBLE) {
            Animation bottomDown = AnimationUtils.loadAnimation(ctx, R.animator.slide_bottom_down);
            fragment_switcher.startAnimation(bottomDown);
            fragment_switcher.setVisibility(View.GONE);
        }
    }

    /**
     * Attempt to load the stat by checking 1) for an NFC launch and then 2) from any JSON stored in shared preferences (cached from previous app usage).
     */
    private void loadStat() {

        hideStatusMessage();

        SharedPreferences sharedPreferences = getSharedPreferences(App.SHARED_PREF_ID, MODE_PRIVATE);
        String jsonStr = sharedPreferences.getString(App.SHARED_PREF_CURRENT_STAT_JSON, "");

        try {
            if (!jsonStr.isEmpty()) {
                currentStat.loadFromJSON(jsonStr);
                currentStat.startDataRefresh(0);
            }
        } 
        catch (Exception e) {
            Log.e(App.TAG, "loadStat error: " + e.getMessage());
        }

        int size = statNameText.length();
        ScrollView.LayoutParams x = (ScrollView.LayoutParams)statNameText.getLayoutParams();
        if (size < 30){
            x.gravity = Gravity.CENTER;
        }
        else {
            x.gravity = Gravity.START;
        }
        statNameText.setLayoutParams(x);
        statNameText.setText(currentStat.Name);
    }

    // --------------------------------------------------------------------------------
    // Data Sync Listener
    // --------------------------------------------------------------------------------
    VirtualStat.DataSyncListener dataListener = new VirtualStat.DataSyncListener() {
        @Override
        public void onStatusUpdate(String msg) {
            // If network error, we want to stop data refresh, disable content and show an alert message.
            if (msg.equals("HttpHostConnectException")) {
                Log.e(App.TAG, "HttpHostConnectException");
                alertWindow.showAlert(getString(R.string.network_status_other), true);
                currentStat.stopDataRefresh();
                currentStat.restoreAllToLastKnownValue();
                updateFragments();
            }
            else if (msg.equals("Authentication issue")) {
                Log.e(App.TAG, "Authentication issue");
                alertWindow.showAlert(getString(R.string.network_status_authentication_issue), true);
                currentStat.stopDataRefresh();
                currentStat.restoreAllToLastKnownValue();
                updateFragments();
            	
            }
            else if (msg.equals("Unauthorized")){
                Log.e(App.TAG, "Unauthorized");
                alertWindow.showAlert(getString(R.string.network_status_authorization_fail), true);
                currentStat.stopDataRefresh();
                currentStat.restoreAllToLastKnownValue();
                updateFragments();
            }
            else if (msg.equals("ForbiddenCall")){
                Log.e(App.TAG, "ForbiddenCall");
                alertWindow.showAlert(getString(R.string.network_status_forbidden_call), true);
                currentStat.stopDataRefresh();
                currentStat.restoreAllToLastKnownValue();
                updateFragments();
            }
            // Else, attempt to format error and show it.
            else if (msg.startsWith("QERR")) {
                showStatusMessage(App.translateStatus(msg));
            }
            else {
                if (!msg.equals("OK")) {
                    // Log error, but do not show it
                    Log.e(App.TAG, "Status error: " + msg);
                }
                hideStatusMessage();
            }
        }

        @Override
        public void onDataUpdate() {
            alertWindow.hideAlert(true);
            updateFragments();

            String json = currentStat.createSystemJSONString();
            StatListFragment.StatListCache.set(currentStat.Name, json);
        }
    };

    private void hideStatusMessage() {
        // Force run on main UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.statStatusText);
                tv.setText("");
            }
        });
    }

    /**
     * Updates the statStatusText outlet to show the message
     * 
     * @param message
     */
    private void showStatusMessage(final String message) {
        // Force run on main UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.statStatusText);
                if (!tv.getText().equals(message)) {
                    tv.setText(message);
                }
            }
        });
    }

    // --------------------------------------------------------------------------------
    // Fragment interaction
    // --------------------------------------------------------------------------------
    private void updateFragments() {
        FragmentManager fm = getSupportFragmentManager();

        // Update header
        int size = currentStat.Name.length();
        ScrollView.LayoutParams x = (ScrollView.LayoutParams)statNameText.getLayoutParams();
        if (size < 15){
            x.gravity = Gravity.CENTER | Gravity.CENTER_VERTICAL;
        }
        else {
            x.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        }
        statNameText.setLayoutParams(x);
        statNameText.setText(currentStat.Name);

        // Note, stat list fragment does not have to be updated.

        // Update visible fragment using the UseVirtualStat interface
        Fragment contentFragment = (Fragment) fm.findFragmentById(R.id.fragment_content);
        UseVirtualStat contentInterface = (UseVirtualStat) contentFragment;
        if (contentInterface != null) {
            contentInterface.updateWithDelegate();
        }
    }

    // --------------------------------------------------------------------------------
    // VirtualStat.ExposeVirtualStat interface
    // --------------------------------------------------------------------------------
    @Override
    public VirtualStat getCurrentStat() {
        return this.currentStat;
    }

}
