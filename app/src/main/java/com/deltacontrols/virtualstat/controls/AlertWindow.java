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
 * AlertWindow.java
 */

package com.deltacontrols.virtualstat.controls;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deltacontrols.virtualstat.R;
import com.deltacontrols.virtualstat.UIFactory;

/**
 * Custom control which allows an alert window to be overlaid at the top of an activity. If desired, the alert can be displayed with a "full" 
 * semi-transparent layer that disables interaction with content "underneath" it.
 */
public class AlertWindow extends RelativeLayout {

    private final int version = Integer.valueOf(Build.VERSION.SDK_INT); // Device OS version

    private LinearLayout alertMessageLayout = null; // Main layout
    private TextView alertMessageText = null;       // Message text
    private RelativeLayout disableLayout = null;    // Layout to disable interaction

    private boolean isShowingAlert;                 // Flag indicating if alert is showing
    private boolean isDisabled;                     // Flag indicating if disableLayout is active

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public AlertWindow(Context context) {
        super(context);
        init(context);
    }

    public AlertWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlertWindow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Inflates view and sets outlets
     */
    private void init(Context context) {
        this.isShowingAlert = false;

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        inflater.inflate(R.layout.view_alertwindow_layout, this, true);

        alertMessageLayout = (LinearLayout) findViewById(R.id.alertMessageLayout);
        alertMessageText = (TextView) findViewById(R.id.alertMessageText);
        disableLayout = (RelativeLayout) findViewById(R.id.deviceOfflineLayout);
    }

    /**
     * Will display the alert with the given message, disabling the underlying view if desired. If the alert is already showing, then the text will be changed.
     * 
     * @param message Message to show
     * @param disable If true, then also activate the disableLayout view.
     */
    @SuppressLint("NewApi")
    public synchronized void showAlert(final String message, boolean disable) {
        // If we are currently not showing an alert or if the alertMessageLayout is not visible (note: this fixes
        // a race condition where the stat still think that it is showing an alert, but the alert is no longer present. If in the
        // future this condition can be tracked down, or solved via a mutex, then we can remove the second condition).
        if ((!isShowingAlert) || (alertMessageLayout.getVisibility() == View.GONE)) {

            isShowingAlert = true;
            alertMessageText.setText(message);

            if (version <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                // No animation
                alertMessageLayout.setVisibility(View.VISIBLE);
                alertMessageLayout.setClickable(true);
                UIFactory.setLayoutAlpha(alertMessageLayout, 1f);
            }
            else {
                // Use animation
                alertMessageLayout.setAlpha(0f);
                alertMessageLayout.setVisibility(View.VISIBLE);
                alertMessageLayout.animate()
                        .setDuration(1000)
                        .alpha(1f)
                        .setListener(null);
            }
        }
        else {
            alertMessageText.setText(message); // Update text only.
        }

        if (disable) {
            this.disable();
        }
    }

    /**
     * Hides the alert box (fades out) 
     * Note: currentAlertAnimation.setFillAfter(true); // Do not set this, for some reason it makes the view.gone still clickable
     */
    @SuppressLint("NewApi")
    public synchronized void hideAlert(boolean enable) {
        if (!isShowingAlert) {
            return;
        }
        isShowingAlert = false;

        if (version <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // No animation
            alertMessageLayout.setClickable(false);
            alertMessageText.setText("");
            UIFactory.setLayoutAlpha(alertMessageLayout, 0f);
            alertMessageLayout.setVisibility(View.GONE);
        }
        else {
            // Use animation
            alertMessageLayout.animate()
                    .setDuration(1000)
                    .alpha(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            alertMessageText.setText("");
                            alertMessageLayout.setVisibility(View.GONE);
                        }
                    });
        }

        if (enable) {
            this.enable();
        }
    }

    /**
     * Disables underlying content by putting a semi-transparent layer over the top of the UI.
     */
    @SuppressLint("NewApi")
    public synchronized void disable() {
        if (isDisabled) {
            return;
        }
        isDisabled = true;

        if (version <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // No animation
            disableLayout.setClickable(true);
            disableLayout.setVisibility(View.VISIBLE);
            UIFactory.setLayoutAlpha(disableLayout, 0.6f);
        }
        else {
            disableLayout.setAlpha(0f);
            disableLayout.setVisibility(View.VISIBLE);

            // Use animation
            disableLayout.animate()
                    .setDuration(1000)
                    .alpha(0.6f)
                    .setListener(null);
        }
    }

    /**
     * Enables underlying content by hiding the semi-transparent layer.
     */
    @SuppressLint("NewApi")
    public synchronized void enable() {

        if ((isDisabled) || (disableLayout.getVisibility() == View.GONE)) {
            isDisabled = false;

            if (version <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                // No animation
                disableLayout.setClickable(false);
                disableLayout.setVisibility(View.GONE);
                UIFactory.setLayoutAlpha(disableLayout, 0f);

            }
            else {
                // Use animation
                disableLayout.animate()
                        .setDuration(1000)
                        .alpha(0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                disableLayout.setVisibility(View.GONE);
                            }
                        });
            }
        }
    }

}
