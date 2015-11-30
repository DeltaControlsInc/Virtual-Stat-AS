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
 * SingleStatControlActivity.java
 */

package com.deltacontrols.virtualstat.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.LoginInfo;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.VirtualStat;
import com.deltacontrols.virtualstat.VirtualStat.UseVirtualStat;
import com.deltacontrols.virtualstat.controls.AlertWindow;
import com.deltacontrols.virtualstat.fragment.SingleStatControlTabs;
import com.deltacontrols.virtualstat.nfc.NFCHelper;

/**
 * Activity controlling the view of a complete stat; it will consist of the tabs (showing relevant pieces of the stat) as well as a main content 
 * area which will contain the currently selected tab (ie.
 * Temperature)
 */
public class SingleStatControlActivity extends FragmentActivity implements VirtualStat.ExposeVirtualStat {

    final int version = Integer.valueOf(Build.VERSION.SDK_INT);
    static final int SETTINGS_REQUEST = 1;

    private VirtualStat currentStat = null;
    private NFCHelper mNFCHelper;
    private boolean isDemoMode = false;     // Demo = fixed points, no eWEB interaction
    private boolean isDestroyed = false;
    private int launchTab = 0;

    // Outlets
    RelativeLayout mainPageViewGroup = null;
    TextView statNameText = null;
    TextView statusText = null;
    SingleStatControlTabs tabFragment = null;
    ImageView statSettingsIcon = null;
    AlertWindow alertWindow = null;

    // --------------------------------------------------------------------------------
    // Life cycle
    // --------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isDemoMode = App.getDemoMode();

        setContentView(R.layout.activity_single_stat_control);
        mNFCHelper = new NFCHelper(App.getContext(), this);

        // Setup outlets
        mainPageViewGroup = (RelativeLayout) findViewById(R.id.mainPageViewGroup);
        statNameText = (TextView) findViewById(R.id.statName);
        statusText = (TextView) findViewById(R.id.statStatusText);
        alertWindow = (AlertWindow) findViewById(R.id.alertWindow);

        findViewById(R.id.alertMessageLayout).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertWindow.showAlert(getString(R.string.network_reconnect_status), false);
                        currentStat.startDataRefresh(0);
                    }
                });

        // Load stat values BEFORE we load the fragments.
        loadStat();

        // Grab the instance of TabFragment that was included with the layout and have it launch the initial tab.
        // Note, use FrameLayout so that the fragment loads AFTER we have the stat setup (if Fragment set
        // directly in XML, then its OnCreate is called before we get here and the virtualStatDelegate will be null.
        FragmentManager fm = getSupportFragmentManager();
        tabFragment = new SingleStatControlTabs();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_switcher, tabFragment);
        ft.commit();

        // Check for tab launch
        try {
            Bundle bundle = getIntent().getExtras();
            launchTab = bundle.getInt("loadTab");
        } catch (Exception e) {
            launchTab = 0;
        }

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

    /*
     * (non-Javadoc) This function is called when the intent has been launched via a new intent; this could be the case if the login screen already 
     * active and then another NFC 'tap' was discovered. The second tap will call this onNewIntent - so we want to check the intent to make sure this is 
     * the case, and then pull out any NFC data we may discover.
     * 
     * @note For this to work correctly, the activity must be set to singleInstance in the manifest file.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        // Called on 'back' or on NFC launch
        super.onNewIntent(intent);
        checkNFCLaunch(intent);
        loadStat();
    }

    private void checkNFCLaunch(Intent intent) {
        String nfcID = mNFCHelper.getIDFromIntent(intent);
        if (nfcID != null) {
            // Launch single stat activity without logging in to eweb
            Intent startIntent = new Intent(App.getContext(), NFCFetchActivity.class);
            startIntent.putExtra(NFCFetchActivity.INTENT_UUID, nfcID);
            startActivity(startIntent);
            finish();
        }
    }

    /**
     * When resuming the activity, enable NFC, load the tab that was last viewed, and start refreshing data.
     */
    public void onResume() {
        super.onResume();
        overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_right);

        mNFCHelper.enableForegroundDispatch();

        // Start data refresh if required
        if (!isDemoMode) {
            currentStat.startDataRefresh(0);
        }

        // Load the desired tab
        if (launchTab == 0) {
            tabFragment.reloadLastTab();
        }
        else {
            tabFragment.loadView(launchTab);
            launchTab = 0; // Only load on first launch; after that reload last.
        }

        // Update data in fragments
        updateFragments();
    }

    public void onPause() {
        super.onPause();
        currentStat.stopDataRefresh();
        mNFCHelper.disableForegroundDispatch();

        // Save statStr in shared preferences so that it can be used by other activities in the future.
        App.saveStatIntoSharedPreferences(currentStat.createSystemJSONString());
    }

    public void onDestroy() {
        super.onDestroy();
        this.isDestroyed = true;
        this.currentStat.setDataSyncListener(null);
        this.currentStat = null;
    }

    /**
     * Call Back method to get the Message form other Activity Note: You can't use startActivityForResult() if your activity is being launched as a 
     * singleInstance or singleTop; originally to avoid multiple NFC launches we are setting the launchMode of the original Activity as singleInstance - 
     * now we are attempting to use singleTask which should hopefully work
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_REQUEST) {
            // Settings does not return data, currently it sets the shared prefs
            if (resultCode == RESULT_OK) {
                showStatusMessage("");
                // We may have new stat values, restart the activity so the overall stat will be re-created.
                finish();
                startActivity(new Intent(this, SingleStatControlActivity.class));
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_left);
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

    // --------------------------------------------------------------------------------
    // Data Sync Listener
    // --------------------------------------------------------------------------------
    VirtualStat.DataSyncListener dataListener = new VirtualStat.DataSyncListener() {
        /**
         * If error occurs, parse and display error.
         */
        @Override
        public void onStatusUpdate(String msg) {
            // If network error, we want to stop data refresh, disable content and show an alert message.
            if (msg.equals("HttpHostConnectException")) {
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

        /**
         * When new data received, update all fragments
         */
        @Override
        public void onDataUpdate() {
            if (isDestroyed) {
                return;
            } // Async data update; if the activity has been destroyed, do not update UI.
            try {
                alertWindow.hideAlert(true);
            }
            catch (Exception e) {
                Log.e(App.TAG, "onDataUpdate: " + e.getMessage());
            }

            updateFragments();
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
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Uses shared preferences to load in the current stat; if any errors occur, load into demo mode.
     */
    private void loadStat() {
        if (currentStat == null) {
            currentStat = new VirtualStat();
        }

        // Use shared preferences to get 'current' selected stat.
        SharedPreferences sharedPreferences = getSharedPreferences(App.SHARED_PREF_ID, MODE_PRIVATE);
        String jsonStr = sharedPreferences.getString(App.SHARED_PREF_CURRENT_STAT_JSON, "");

        currentStat.setDataSyncListener(dataListener);
        currentStat.loadFromJSON(jsonStr);
        statNameText.setText(currentStat.Name);
    }

    /**
     * Launch the settings page
     * 
     * @param view
     */
    private void launchSettingsActivity(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST);
    }

    // --------------------------------------------------------------------------------
    // Fragment interaction
    // --------------------------------------------------------------------------------
    private void updateFragments() {
        // Always update fragments on the ui thread
        runOnUiThread(new Runnable() {
            public void run() {
                FragmentManager fm = getSupportFragmentManager();

                // Update header
                statNameText.setText(currentStat.Name);

                // Update tabs fragment
                if (tabFragment != null) {
                    tabFragment.updateWithDelegate();
                }

                // Update visible fragment using the UseVirtualStat interface
                Fragment contentFragment = (Fragment) fm.findFragmentById(R.id.fragment_content);
                UseVirtualStat contentInterface = (UseVirtualStat) contentFragment;
                if (contentInterface != null) {
                    contentInterface.updateWithDelegate();
                }
            }
        });
    }

    // --------------------------------------------------------------------------------
    // VirtualStat.ExposeVirtualStat interface
    // --------------------------------------------------------------------------------
    @Override
    public VirtualStat getCurrentStat() {
        return this.currentStat;
    }
}
