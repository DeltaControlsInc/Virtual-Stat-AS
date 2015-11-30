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
package com.deltacontrols.virtualstat.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.deltacontrols.eweb.support.api.EwebConnection;
import com.deltacontrols.eweb.support.api.FetchJSON;
import com.deltacontrols.eweb.support.interfaces.GenericCallback;
import com.deltacontrols.virtualstat.App;
import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;
import com.deltacontrols.virtualstat.nfc.NFCHelper;

/**
 * NFCFetchActivity Shows a loading page while we make a request to the server to get the NFC stat JSON. 
 * Created a separate activity so that it could be launched both internally and externally.
 * 
 * Note, android:noHistory="true" set in Manifest - this activity will never belong to the history stack.
 */
public class NFCFetchActivity extends Activity {

    public static final String INTENT_UUID = "NFC_INTENT_UUID";

    private String mUUID;
    private EwebConnection mEweb;
    private String mFullStatName;
    private boolean mActive;
    private Activity me;
    private NFCHelper mNFCHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;
        mNFCHelper = new NFCHelper(getApplicationContext(), this);

        // UUID must be part of bundle
        // Check for tab launch
        try {
            Bundle bundle = getIntent().getExtras();
            mUUID = bundle.getString(INTENT_UUID);
        } catch (Exception e) {
            mUUID = null;
            Log.e(App.TAG, "NFCFetchActivity launched with no NFC_UUID");
            finish(); // End Activity?
        }

        mEweb = App.getEwebConnection();
        mFullStatName = String.format("vs_nfc_%s", mUUID);

        setContentView(R.layout.activity_nfcfetch);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActive = true;
        mNFCHelper.enableForegroundDispatch();

        if (mUUID == null) {
            mActive = false;
            return;
        }

        App.getStat(mFullStatName, mGetStatListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mActive = false;
        mNFCHelper.disableForegroundDispatch();
    }

    private GenericCallback<FetchJSON.Result> mGetStatListener = new GenericCallback<FetchJSON.Result>() {
        @Override
        public void onCallback(FetchJSON.Result result) {
            if (!mActive) {
                return;
            }

            try {
                // Check if eWEB had a matching NFC system
                // Currently, we get an empty JSON object back if there is no match.
                if (result.json.length() == 0) {
                    // Show feedback
                    UIFactory.deltaToast(App.getContext(), getString(R.string.no_nfc_associated_with_given_stat), null);

                    // Go 'back' (finish activity)
                    finish();
                }
                else {
                    // Save NFC data into prefs and then launch single stat
                    App.saveStatIntoSharedPreferences(result.json.toString());

                    // Launch summary
                    Intent statintent = new Intent(App.getContext(), SummaryActivity.class);
                    startActivity(statintent);
                    App.useCustomDefaultPendingTransition(me);
                }

            }
            catch (Exception e) {
                Log.e(App.TAG, "Failed to get stat data by NFC UUID: " + e.getMessage());
                finish();
            }
        }
    };
}
