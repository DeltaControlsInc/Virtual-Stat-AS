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
 * NFCGelper.java
 */
package com.deltacontrols.virtualstat.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;

import com.deltacontrols.nfc_driver.NfcAPI;
import com.deltacontrols.nfc_driver.NfcConstant;

/**
 * Helper class for nfc-api.jar Contains wrapper functions used to enable NFC dispatches and reads.
 */
public class NFCHelper {
    private Activity mActivity = null; // NFC Launch activity; also dispatch activity; NOTE: May want to separate
    private PendingIntent mPendingIntent = null; // Intent launched when NFC dispatched
    private NfcAPI mNfcApi = null; // Reference to NfcApi library
    private boolean mDeviceSupportsNFC = false;

    // --------------------------------------------------------------------------------
    // Constructor
    // --------------------------------------------------------------------------------
    /**
     * NFCHelper Note: Use in activity onCreate()
     * 
     * @param ctx Application context; ie. Activity.getApplicationContext();
     * @param act Activity that dispatches NFC intents; also acts as the launched intent (ie. it dispatches the same activity)
     */
    public NFCHelper(Context ctx, Activity act) {
        this.mActivity = act;
        this.mNfcApi = new NfcAPI(ctx.getPackageManager(), ctx);
        this.mDeviceSupportsNFC = mNfcApi.mNFCStatus != NfcConstant.NFC_NOT_SUPPORTED;

        // Create pending intent based on passed in activity
        Intent NFCIntent = new Intent(ctx, mActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.mPendingIntent = PendingIntent.getActivity(ctx, 0, NFCIntent, 0);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Enables NFC to be dispatched from the activity Note: Use in activity onResume()
     */
    public void enableForegroundDispatch() {
        if ((mNfcApi != null) && mDeviceSupportsNFC) {
            mNfcApi.getAdapter().enableForegroundDispatch(mActivity, mPendingIntent, mNfcApi.getFilter(), mNfcApi.getTechList());
        }
    }

    /**
     * Disables NFC dispatch from the activity Note: Use in activity onPause()
     */
    public void disableForegroundDispatch() {
        if ((mNfcApi != null) && mDeviceSupportsNFC) {
            mNfcApi.getAdapter().disableForegroundDispatch(mActivity);
        }
    }

    public boolean deviceSupportsNFC() {
        return mDeviceSupportsNFC;
    }

    /**
     * getIDFromIntent Check the intent to see if it was launched via NFC; if it is, parse out the UID string from the NFC device. 
     * Note: Use in activity onNewIntent(Intent intent)
     * 
     * @param newIntent The intent we parse to see if it was launched via NFC.
     * @return String NFC device UID if found; null if no NFC or if error occurred while parsing.
     */
    public String getIDFromIntent(Intent intent) {
        // note: If an instance of activity already exists at the top of the current task and system
        // routes intent to this activity, no new instance will be created
        if (wasLaunchedViaNFC(intent)) {

            String ID;
            try {
                mNfcApi.readNFCInfo(intent);
                ID = mNfcApi.getUID();
            } catch (Exception e) {
                ID = null;
            }

            if (ID != null) {
                // Reset intent action, (essentially consume NFC tap). If we do not do this, then going 'back' in the history
                // will cause the login page to continue to use the NFC for login.
                intent.setAction(null);
                return ID;
            }
        }

        return null;
    }

    public boolean wasLaunchedViaNFC(Intent intent) {
        String action = intent.getAction();
        return (action != null) && (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED));
    }
}