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
 * LoginActivity.java
 */
package com.deltacontrols.virtualstat.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.deltacontrols.eweb.support.api.EwebConnection;
import com.deltacontrols.eweb.support.api.FetchJSON;
import com.deltacontrols.eweb.support.interfaces.GenericCallback;
import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.LoginInfo;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.nfc.NFCHelper;

import org.apache.http.HttpStatus;

/**
 * Login activity; first intent if launched via icon. Allows user to enter in IP/name/password, and will remember these settings if desired.
 */
public class LoginActivity extends Activity {
    public final static String TAG = "VirtualStat Login";
    // Request Codes
    static final int SINGLE_STAT_REQUEST = 1;

    // Constant IDs
    public static final String SHARED_PREF_IS_DEMO_ID = "IS_DEMO_ID";

    // Connection variables
    private Context ctx;
    private boolean isConnecting = false;
    private NFCHelper mNFCHelper = null;
    private EwebConnection eweb;
    private String mNfcUuid = null;

    // Form values
    private LoginInfo mLoginInfo;

    // UI references.
    private EditText mURLView;
    private EditText mUserNameView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private ImageView mLoadingImage; // Need to animate loading spinner

    private boolean mBasicAuthentication = true;

    // --------------------------------------------------------------------------------
    // Life cycle
    // --------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eweb = App.getEwebConnection();

        setContentView(R.layout.activity_login);
        UIFactory.logDisplayMetrics(this);

        // Outlets
        mLoadingImage = (ImageView) findViewById(R.id.loading_image);
        mURLView = (EditText) findViewById(R.id.url);
        mUserNameView = (EditText) findViewById(R.id.user_name);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        // Attempt to read from NFC before loading the view since we may be launching a new intent right from here.
        mNFCHelper = new NFCHelper(getApplicationContext(), this);
        mLoginInfo = LoginInfo.getStoredLogin();

        // Start with basic authentication
        mBasicAuthentication = true;
        // Allow retry with url connect method if basic authentication not supported
        mConnectRetry = true;

        // If not launched by NFC AND we have an auto login, then attempt to login
        // Note, currently launching via NFC from the homescreen does not give us enough data to automatically open the stat; so do not login
        // automatically, instead we ask user to tap again
        if (!mNFCHelper.wasLaunchedViaNFC(getIntent()) && (mLoginInfo.autoLogin)) {
            attemptLogin(null);
        }
        else if (mNFCHelper.deviceSupportsNFC()) {
            UIFactory.deltaToast(this, getString(R.string.login_through_nfc), null);
        }

        // Setup outlets
        mURLView.setText(mLoginInfo.eWebURL);
        mUserNameView.setText(mLoginInfo.userName);
        mPasswordView.setText(mLoginInfo.password);

        // Attach click listener to login button.
        findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if ((!mURLView.getText().toString().contains("http")) && (mURLView.getText().toString().length() != 0)) {
                            mLoginInfo.eWebURL = String.format("http://%s", mURLView.getText().toString());
                        } else {
                            mLoginInfo.eWebURL = mURLView.getText().toString();
                        }
                        mLoginInfo.userName = mUserNameView.getText().toString();
                        mLoginInfo.password = mPasswordView.getText().toString();
                        // Try basic authentication first, if fail callback will switch back to url authentication
                        mBasicAuthentication = true;
                        mConnectRetry = true;
                        attemptLogin(null);
                    }
                });

        ctx = this;
        
        // Set custom font
        UIFactory.setCustomFont(ctx, findViewById(R.id.mainPageViewGroup));
    }

    @Override
    public void onPause() {
        super.onPause();
        mNFCHelper.disableForegroundDispatch();
        UIFactory.cancelToast(); // Kill any toasts currently showing
    }

    @Override
    public void onResume() {
        super.onResume();
        mNFCHelper.enableForegroundDispatch(); // Setup NFC dispatch
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        UIFactory.setCustomFont(this, findViewById(R.id.mainPageViewGroup));
    }

    /*
     * (non-Javadoc) This function is called when the intent has been launched via a new intent; this could be the case if the login 
     * screen already active and then another NFC 'tap' was discovered.
     * The second tap will call this onNewIntent - so we want to check the intent to make sure this is the case, and
     *  then pull out any NFC data we may discover.
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
            if (!eweb.isConnected()) {
                attemptLogin(nfcID);
            }
            else {
                launchNFCMode(nfcID);
            }
        }
    }

    // --------------------------------------------------------------------------------
    // Launch Modes
    // --------------------------------------------------------------------------------
    private void launchDemoMode() {
        App.setDemoMode(true);

        // Clear shared prefs so that next login does not use demo info
        SharedPreferences.Editor editor = getSharedPreferences(App.SHARED_PREF_ID, MODE_PRIVATE).edit();
        editor.clear().commit();

        // Launch single stat activity without logging in to eweb
        Intent intent = new Intent(ctx, SummaryActivity.class);
        startActivity(intent);
        App.useCustomDefaultPendingTransition(this);
    }

    private void launchNFCMode(String nfc_uuid) {
        App.setDemoMode(false);

        // Launch single stat activity without logging in to eweb
        Intent intent = new Intent(ctx, NFCFetchActivity.class);
        intent.putExtra(NFCFetchActivity.INTENT_UUID, nfc_uuid);
        startActivity(intent);
        App.useCustomDefaultPendingTransition(this);
    }

    private void launchLiveMode() {
        App.setDemoMode(false);

        // Launch main intent (pass along id; could be null if not launched via NFC)
        Intent intent = new Intent(ctx, SummaryActivity.class);
        startActivity(intent);
        App.useCustomDefaultPendingTransition(this);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    private String convertConnectMsgToString(EwebConnection.CONNECTION_STATUS message) {
        String result;

        switch (message) {
        case OK:
            result = getString(R.string.network_connect_ok);
            break;
        case ERROR_NETWORK:
            result = getString(R.string.eweb_connect_error_connection);
            break;
        case ERROR_UNKNOWN_URL:
            result = getString(R.string.eweb_connect_error_url);
            break;
        case ERROR_LOGIN_PASSWORD:
            result = getString(R.string.network_connect_login_failure);
            break;
        case ERROR_NO_CONNECTION_AVAILABLE:
            result = getString(R.string.eweb_connect_error_offline);
            break;
        case ERROR_UNKNOWN_EXCEPTION:
        default:
            result = getString(R.string.network_connect_unknown_error);
            break;
        }

        return result;
    }

    /**
     * Reports that a login has failed by using the built in Toast functionality.
     * 
     * @param message the message string shown in the Toast
     */
    private void reportFailedLogin(String message) {
        UIFactory.deltaToast(App.getContext(), message, null);
    }

    /**
     * Attempts to sign in using the values currently stored in mLoginInfo If there are form errors, the errors are presented and no actual login attempt is made.
     * 
     * @return Nothing
     */
    private void attemptLogin(final String nfc_uuid) {

        if (isConnecting) {
            return; // Task already running
        }

        mNfcUuid = nfc_uuid;

        // If nothing filled in, go to demo mode
        if (mLoginInfo.eWebURL.equals("") && mLoginInfo.userName.equals("") && mLoginInfo.password.equals("")) {
            if (mNfcUuid == null) {
                launchDemoMode();
            }
            else {
                UIFactory.deltaToast(App.getContext(), getString(R.string.network_connect_retry_message), null);
            }

            return;
        }

        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        isConnecting = true;
        mLoginStatusMessageView.setText(R.string.login_activity_progress_signing_in);
        showProgress(true);

        try {
            eweb.disconnect();
            eweb.connect(mLoginInfo.eWebURL, mLoginInfo.userName, mLoginInfo.password, connectListener, mBasicAuthentication);
        }
        catch (Exception e) {
            isConnecting = false;
            reportFailedLogin(getString(R.string.network_connect_fail_message));
        }
    }

    /**
     * Shows the progress UI and hides the login form, or vice versa
     * 
     * @param show If true, then we want to show the progress view, if false then we hide the progress view.
     * @return Nothing
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        if (show) {
            mLoadingImage.startAnimation(AnimationUtils.loadAnimation(this, R.animator.login_image_rotate));
        }
        else {
            mLoadingImage.clearAnimation();
        }

        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    // This is handler is used to call the old authentication when the new authentication fail.
    Handler connectHandler = new Handler(); // Handler for the refresh functionality
    private final long mConnetTimeout = 10; // Refresh timeout in milliseconds
    private boolean mConnectRetry = true;
    private Runnable connectTask = new Runnable() { // Runnable task executed by the handler via timeout
        @Override
        public void run() {
            if (eweb.getConnectionStatus() != EwebConnection.CONNECTION_STATUS.OK){
                eweb.disconnect();
                eweb.connect(mLoginInfo.eWebURL, mLoginInfo.userName, mLoginInfo.password, connectListener);
                // Only does once
                stopReconnect();
            }

        }
    };

    private void startReconnect(long delayMs) {
        connectHandler.removeCallbacks(connectTask);
        connectHandler.postDelayed(connectTask, delayMs);
    }

    private void stopReconnect() {
        connectHandler.removeCallbacks(connectTask);
    }

    private GenericCallback<FetchJSON.Result> connectListener = new GenericCallback<FetchJSON.Result>() {
        @Override
        public void onCallback(FetchJSON.Result result) {
            isConnecting = false;
            try {
                showProgress(false);

                Log.e(TAG, "Connect Status. " + result.statusCode);
                // Problem logging in, display the connection message.
                if (!eweb.isConnected()) {
                    if ((result.statusCode == HttpStatus.SC_BAD_REQUEST) || (result.statusCode == HttpStatus.SC_UNAUTHORIZED)) {
                        Log.e(TAG, "Fail basic authentication.");
                        if (mConnectRetry) {
                            mBasicAuthentication = false;
                            mConnectRetry = false;
                            // Reconnect using url in 10ms
                            startReconnect(mConnetTimeout);
                            showProgress(true);
                        }
                        else {
                            reportFailedLogin(convertConnectMsgToString(eweb.getConnectionStatus()));
                        }

                    }
                    else {
                        reportFailedLogin(convertConnectMsgToString(eweb.getConnectionStatus()));
                    }
                }
                else {
                    LoginInfo storedLogin = LoginInfo.getStoredLogin();

                    // If current login != stored login, then we want to nuke the cached "last" stat and save this
                    // login as the new stored login
                    if (!storedLogin.equals(mLoginInfo)) {
                        App.removeStatFromSharedPreferences();
                    }
                    // reset the runnable to stop calling connect
                    stopReconnect();

                    // Set user to auto login from now on
                    mLoginInfo.autoLogin = true;
                    mLoginInfo.setStoredLogin();

                    if (mNfcUuid == null) {
                        launchLiveMode(); // Go straight to summary
                    } else {
                        launchNFCMode(mNfcUuid); // Need to lookup NFC info before showing summary
                    }
                }
            }
            catch (Exception e) {
                reportFailedLogin(getString(R.string.eweb_connect_unknown_error));
            }
        }
    };

}
