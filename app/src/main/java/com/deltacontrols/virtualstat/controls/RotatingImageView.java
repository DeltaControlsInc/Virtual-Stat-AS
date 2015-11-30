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
 * RotatingImageView.java
 */

package com.deltacontrols.virtualstat.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

import com.deltacontrols.virtualstat.R;

/**
 * Rotates a given image based on touch inputs from the user; fashioned in the style of a dialer, but rotated image could be anything.
 */
public class RotatingImageView extends ImageView {
    // --------------------------------------------------------------------------------
    // Listener interface
    // --------------------------------------------------------------------------------
    public interface RotatingImageViewListener {
        public void onValueChange(double value); // Callback when values changes
        public void onViewReady(); // Callback when the view is considered ready
    }

    private RotatingImageViewListener listener = null;

    public void setListener(RotatingImageView.RotatingImageViewListener l) {
        listener = l;
    }

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    private static Bitmap imageOriginal, imageScaled;
    private static Matrix matrix;
    private int dialerHeight, dialerWidth;  // Height of the dialer dialer

    private int fullRotationValue;          // Value of one full rotation of the dialer
    private int direction;                  // Direction of rotation, 0 = CW, 1 = CCW
    private int minValue;                   // Min allowable value
    private int maxValue;                   // Max allowable value
    private int offsetAngle;                // Starting image offset angle

    private int totalRotationAngle;         // Once setup, full rotation 0 - X
    private boolean isViewReady;            // Indicates ready; do not fire events to listeners until ready
    private boolean isRotating;             // Indicates if the contol is currently rotating.

    private ImageView dialer;               // Pointer to self, used for size calculation

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public RotatingImageView(Context context) {
        super(context);
        initalize(context);
        fullRotationValue = 2;
        direction = 0; // 0 = CW, 1 = CCW
        minValue = Integer.MIN_VALUE;
        maxValue = Integer.MAX_VALUE;
    }

    public RotatingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get custom attributes
        TypedArray rivTypes = context.obtainStyledAttributes(attrs, R.styleable.RotatingImageView);
        fullRotationValue = rivTypes.getInteger(R.styleable.RotatingImageView_fullRotationValue, 2);
        direction = rivTypes.getInteger(R.styleable.RotatingImageView_direction, 0); // 0 = CW, 1 = CCW
        minValue = rivTypes.getInt(R.styleable.RotatingImageView_minValue, Integer.MIN_VALUE);
        maxValue = rivTypes.getInt(R.styleable.RotatingImageView_maxValue, Integer.MAX_VALUE);
        offsetAngle = 0;
        rivTypes.recycle();
        initalize(context);
    }

    // --------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------
    @SuppressLint("NewApi")
    @Override
    public void onDetachedFromWindow() {
        ViewTreeObserver observer = this.getViewTreeObserver();

        try {
            observer.removeOnGlobalLayoutListener(globalLayoutListener);
        } catch (NoSuchMethodError x) {
            observer.removeGlobalOnLayoutListener(globalLayoutListener);
        }
    }

    @Override
    public void onAttachedToWindow() {
        // Listen for global layout (cannot use OnCreate etc...)
        this.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
    }

    /**
     * ImageView not fully created when constructors are called; to avoid issues with sizing, the actual bitmap setup is done after the 
     * onGlobalLayout event has been fired. At this point the layout has been complete and we know the image size.
     */
    private OnGlobalLayoutListener globalLayoutListener = new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {

            // If method called more than once, but the values only need to be initialized one time
            if (!isViewReady) {

                dialerHeight = dialer.getHeight();
                dialerWidth = dialer.getWidth();

                // Resize the image to match the desired size
                Matrix resize = new Matrix();
                resize.postScale(
                        (float) Math.min(dialerWidth, dialerHeight) / (float) imageOriginal.getWidth(),
                        (float) Math.min(dialerWidth, dialerHeight) / (float) imageOriginal.getHeight());

                imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);

                // Translate to the image view's center
                float translateX = (dialerWidth / 2 - imageScaled.getWidth() / 2);
                float translateY = (dialerHeight / 2 - imageScaled.getHeight() / 2) + 10;
                matrix.postTranslate(translateX, translateY);

                // Start with the offset; depends on direction (CW = negative, CCW = positive)
                int actualOffset = (direction == 0) ? -offsetAngle : offsetAngle;
                totalRotationAngle = actualOffset;
                matrix.postRotate(actualOffset, dialerWidth / 2, dialerHeight / 2);

                dialer.setImageBitmap(imageScaled);
                dialer.setImageMatrix(matrix);

                // Object now ready for use
                isViewReady = true;
                if (listener != null) {
                    listener.onViewReady();
                }
            }
        }
    };

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * @return The value of the current position of the dialer
     */
    public double getValue() {
        return convertAngleToValue(totalRotationAngle);
    }

    /**
     * Sets the value for the dialer; physically rotates the dialer
     * 
     * @param value The value for the dialer
     * @note 0 degrees is positive x axis, 90 degrees is positive Y axis; so positive values ==> CW rotation
     */
    public void setValue(double value) {
        if (!isViewReady) {
            return;
        }
        if (isRotating) {
            return;
        } // Do not allow value to be set elsewhere if rotation is happening.

        int newAngle = convertValueToTotalAngle(value);
        rotateDialer(newAngle);
    }

    /**
     * Common initialization; set default values, and original
     * 
     * @param context
     */
    public void initalize(Context context) {
        dialer = this;
        dialer.setScaleType(ImageView.ScaleType.MATRIX); // Must have this so we can rotate via matrix scaling

        // Setup default values
        totalRotationAngle = 0;
        isViewReady = false;
        isRotating = false;

        // Load the image only once
        if (imageOriginal == null) {

            Drawable givenImage = this.getDrawable();
            if (givenImage != null) {
                imageOriginal = drawableToBitmap(givenImage);
            }
            else {
                imageOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.input_jog_background); // Use default?
            }
        }

        // Initialize the matrix only once
        if (matrix == null) {
            matrix = new Matrix();
        } else {
            matrix.reset();
        }

        this.setOnTouchListener(new MyOnTouchListener());

        requestLayout();
        invalidate();
    }

    /**
     * Converts a drawable to a bitmap.
     * 
     * @param drawable The drawable to convert
     * @return The converted bitmap 
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Given a value, return the associated (total) angle that will correspond on the dialer.
     * 
     * @param value The value to convert
     * @return The result angle corresponding to the value; 0 degrees is positive x axis, 90 degrees is positive Y axis
     * @note If value direction is clockwise (0) then the angle will be negative; if value direction is counterclockwise (1) then the angle will be positive.
     */
    private int convertValueToTotalAngle(double value) {
        int total = (int) ((value / fullRotationValue) * 360 + (value % fullRotationValue) / 360) + offsetAngle;
        return (direction == 0) ? -total : total;
    }

    /**
     * Given an angle and a number of rotations, return the corresponding value.
     * 
     * @param angle The desired angle (0 - X); 0 degrees is positive x axis, 90 degrees is positive Y axis
     * @return The value associated with the angle and number of rotations; depends on fullRotationValue.
     */
    private double convertAngleToValue(int angle) {
        double value;

        if (direction == 0) {
            value = -((angle + offsetAngle) / 360.0) * fullRotationValue;
        }
        else {
            value = ((angle + offsetAngle) / 360.0) * fullRotationValue;
        }

        if (value == -0.0){
            value = 0.0;
        }

        return value;
    }

    /**
     * Responsible for actually rotating the dialer on screen
     * 
     * @param newAngle The (total) angle to rotate the dialer.
     */
    private void rotateDialer(int newAngle) {
        int degreeChange = totalRotationAngle - newAngle;
        double newValue = convertAngleToValue(newAngle);

        if ((newValue < minValue) || (newValue > maxValue)) {
            return;
        }

        matrix.postRotate(degreeChange, dialerWidth / 2, dialerHeight / 2);
        dialer.setImageMatrix(matrix);
        totalRotationAngle = newAngle;

        if (listener != null) {
            listener.onValueChange(newValue);
        }
    }

    /**
     * Given x, y coordinates within the image (0,0 ==> top,left), converts the coords into the cartesian coordinate system and then return the associated angle (on the unit circle).
     * 
     * @param xTouch X axis touch location
     * @param yTouch Y axis touch location
     * @return The corresponding angle on the unit circle; cartesian coordinate
     * @note http://www.mathsisfun.com/polar-cartesian-coordinates.html!!!
     */
    private int getAngle(double xTouch, double yTouch) {
        double x = xTouch - (dialerWidth / 2d);
        double y = dialerHeight - yTouch - (dialerHeight / 2d);

        double theta = Math.atan2(y, x);
        if (theta < 0) {
            theta += 2 * Math.PI;
        }
        int test3 = (int) Math.toDegrees(theta); // Should give 0 - 360

        return test3;
    }

    // --------------------------------------------------------------------------------
    // Private helper class
    // --------------------------------------------------------------------------------
    /**
     * Implementation of an {@link OnTouchListener} OnTouchListener for image; tracks movement and rotates the dialer accordingly
     */
    private class MyOnTouchListener implements OnTouchListener {
        private int startAngle;
        private final int q1Threshold = 90;     // If the point is less than 90, it is in Q1
        private final int q4Threshold = 270;    // If the point is greater than 270, it is in Q4

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                isRotating = true;
                startAngle = getAngle(event.getX(), event.getY());

                break;

            case MotionEvent.ACTION_MOVE:
                int currentAngle = getAngle(event.getX(), event.getY());
                int degreeChange = 0;

                // If the startAngle was greater than the q4 threshold (ie. started in 4th Q), and the currentAngle is less than the
                // q1 threshold (ie. is in the first Q), then we must calculate our degreeChange carefully.
                if ((startAngle >= q4Threshold) && (currentAngle <= q1Threshold)) {
                    degreeChange = -((360 - startAngle) + currentAngle); // Negative movement.
                }
                // Else, the last move has gone from Q1 to Q4, we must calculate our degreeChange carefully.
                else if ((startAngle <= q1Threshold) && (currentAngle >= q4Threshold)) {
                    degreeChange = (360 - currentAngle) + startAngle; // Positive movement
                }
                // Else do not have to worry about quadrant changes.
                else {
                    degreeChange = startAngle - currentAngle;
                }

                rotateDialer(totalRotationAngle - degreeChange);
                startAngle = currentAngle;

                break;

            case MotionEvent.ACTION_UP:
                isRotating = false;
                break;
            }

            return true;
        }
    }
}
