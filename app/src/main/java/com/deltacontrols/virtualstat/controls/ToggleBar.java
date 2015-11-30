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
 * ToggleBar.java 
 */

package com.deltacontrols.virtualstat.controls;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.deltacontrols.virtualstat.R;

/**
 * Custom control that creates the Toggle bar that will show/hide the summary list.
 */
public class ToggleBar extends RelativeLayout {

    // --------------------------------------------------------------------------------
    // Event / Listeners
    // --------------------------------------------------------------------------------
    public interface ToggleInteractionListener {
        public void onTap();

        public void onSwipeUp();

        public void onSwipeDown();
    }

    private ToggleInteractionListener mToggleListener;

    public void setToggleInteractionListener(ToggleInteractionListener listener) {
        mToggleListener = listener;
    }

    // --------------------------------------------------------------------------------
    // Variables
    // --------------------------------------------------------------------------------
    private GestureDetector mGestureDetector;

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public ToggleBar(Context context) {
        super(context);
        init(context);
    }

    public ToggleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ToggleBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Inflates the ToggleBar layout and sets up gesture listeners.
     * 
     * @param context Current context
     */
    private void init(Context context) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        inflater.inflate(R.layout.view_stat_list_toggle, this, true);

        // Gesture detection
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mToggleListener != null) {
                    mToggleListener.onTap();
                }
                return true;
            }

            /**
             * Currently we only check for swipe up and swipe down gestures.
             */
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float sensitivity = 50;

                // Swipe Up Check
                if (e1.getY() - e2.getY() > sensitivity) {
                    if (mToggleListener != null) {
                        mToggleListener.onSwipeUp();
                    }
                }
                // Swipe Down Check
                else if (e2.getY() - e1.getY() > sensitivity) {
                    if (mToggleListener != null) {
                        mToggleListener.onSwipeDown();
                    }
                }
                // Swipe Left Check
                else if (e1.getX() - e2.getX() > sensitivity) {
                    // N/A
                }
                // Swipe Right Check
                else if (e2.getX() - e1.getX() > sensitivity) {
                    // N/A
                }
                else {
                    // N/A
                }

                return true;
            }
        });

        OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };

        this.setOnTouchListener(gestureListener);
    }
}
