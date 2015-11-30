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
 * UIFactory.java
 */
package com.deltacontrols.virtualstat;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Contains helper methods to auto generate custom UI controls and other UI related items
 */
public class UIFactory {

    // Suppress default instructor to insure non-instantiation
    private UIFactory() {
        throw new AssertionError();
    }

    /**
     * setCustomFont
     * 
     * @param ctx Current app context
     * @param v View or ViewGroup to apply font to.
     */
    public static void setCustomFont(Context ctx, View v) {
        try {
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    UIFactory.setCustomFont(ctx, child);
                }
            } else if (v instanceof TextView) {
                ((TextView) v).setTypeface(Typeface.createFromAsset(ctx.getAssets(), "fonts/Muli.ttf"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Disables the given fragment by injecting a relativelayout over the top of the layout. RelativeLayout is given the R.id.disabledFragment id.
     * 
     * @param ctx
     * @param frag
     */
    public static void disableFragment(final Context ctx, final Fragment frag) {

        // Check to see if already disabled
        ViewGroup mainView = (ViewGroup) frag.getView();
        RelativeLayout disabledOverlay = (RelativeLayout) mainView.findViewById(R.id.disabledFragment);

        if (disabledOverlay == null) {
            // Create overlay.
            disabledOverlay = new RelativeLayout(ctx);
            RelativeLayout.LayoutParams disabledOverlayParmas = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            disabledOverlay.setLayoutParams(disabledOverlayParmas);
            disabledOverlay.setId(R.id.disabledFragment);
            disabledOverlay.setClickable(true);
            disabledOverlay.setGravity(Gravity.CENTER);
            disabledOverlay.setBackgroundColor(ctx.getResources().getColor(R.color.DeltaDisabledBackground));

            // Add overlay to fragment layout
            mainView.addView(disabledOverlay, -1);
        }
        else {
           
        }
    }

    /**
     * Disables the given fragment by removing the relativelayout over the top of the layout by disableFragment.
     * 
     * @param ctx
     * @param frag
     */
    public static void enableFragment(Context ctx, Fragment frag) {
        // Check to see if already disabled
        ViewGroup mainView = (ViewGroup) frag.getView();
        RelativeLayout disabledOverlay = (RelativeLayout) mainView.findViewById(R.id.disabledFragment);

        if (disabledOverlay == null) {
           
        }
        else {
            mainView.removeView(disabledOverlay);
        }
    }

    public static void setLayoutAlpha(View v, float to) {
        AlphaAnimation alpha = new AlphaAnimation(to, to);
        alpha.setDuration(0); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        v.startAnimation(alpha);
    }

    /**
     * Logs screen size and density metrics to the log for the attached device.
     * 
     * @param ctx
     */
    public static void logDisplayMetrics(final Context ctx) {
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        Configuration config = ctx.getResources().getConfiguration();
        StringBuffer dmStr = new StringBuffer();

        // Screen size
        double x = Math.pow(metrics.widthPixels / metrics.xdpi, 2);
        double y = Math.pow(metrics.heightPixels / metrics.ydpi, 2);
        double screenInches = Math.sqrt(x + y);

        // Screen density = sqrt(pixel_width^2 + pixel_height^2)/diagonal_in_inches
        // Phyiscal size of a dp pixel is the same across devices (density INDEPENDANT!)
        // http://www.slideshare.net/Maksim_Golivkin/fast-track-to-android-design

        // Determine screen size
        String sizeBucket = "??";
        if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            sizeBucket = "xlarge";
        } 
        else if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            sizeBucket = "large";
        } 
        else if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            sizeBucket = "normal";
        } 
        else if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {
            sizeBucket = "small";
        }

        int density = metrics.densityDpi;
        String densityBucket = "??";
        if (density == DisplayMetrics.DENSITY_XHIGH) {
            densityBucket = "xhdpi";
        } else if (density == DisplayMetrics.DENSITY_HIGH) {
            densityBucket = "hdpi";
        } else if (density == DisplayMetrics.DENSITY_TV) {
            densityBucket = "tvdpi";
        } else if (density == DisplayMetrics.DENSITY_MEDIUM) {
            densityBucket = "mdpi";
        } else if (density == DisplayMetrics.DENSITY_LOW) {
            densityBucket = "ldpi";
        }

        dmStr.append("> DisplayMetrics: \n");
        dmStr.append(">   SCREEN_INCHES: " + screenInches + "\n");
        dmStr.append(">   SCREEN_WIDTH_PXS: " + metrics.widthPixels + "\n");
        dmStr.append(">   SCREEN_HEIGHT_PXS: " + metrics.heightPixels + "\n");
        dmStr.append(">   SCREEN_SIZE_BUCKET: " + sizeBucket + "\n");
        dmStr.append(">   DENSITY_DPI: " + metrics.densityDpi + "\n");
        dmStr.append(">   DENSITY_BUCKET: " + densityBucket + "\n");

        Log.i("DeltaMetrics", dmStr.toString());
    }

    /**
     * Given a dimension resource id, get the actual numerical DP value.
     */
    public static float getDPDimen(final Context ctx, int id) {
        return (ctx.getResources().getDimension(id) / ctx.getResources()
                .getDisplayMetrics().density);
    }

    // Helper function to convert a Document to an XML string.
    public static String docToXMLStr(Document doc) {

        String result = null;
        try {
            // Normalize to reduce whitespace noise:
            // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            result = writer.getBuffer().toString().replaceAll("\n|\r", "");
        } 
        catch (Exception e) {
            // Build error node
        }

        return result;
    }

    /**
     * Custom toast message.
     */
    private static Toast errorToast;
    public static final int deltaGravityMaskMiddle = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL | Gravity.FILL_HORIZONTAL;

    @SuppressWarnings("deprecation")
    public static void deltaToast(final Context ctx, String message,
            Integer gravityMask) {
        if (gravityMask == null) {
            gravityMask = Gravity.TOP | Gravity.CENTER_HORIZONTAL | Gravity.FILL_HORIZONTAL;
        }

        if (errorToast != null) {
            errorToast.cancel();
        }

        errorToast = Toast.makeText(ctx, message, Toast.LENGTH_LONG);
        View view = errorToast.getView();
        view.setBackgroundDrawable(App.getContext().getResources().getDrawable(R.drawable.shape_toast_layout));
        // view.setBackgroundColor(color.DeltaDarkRed);
        errorToast.setGravity(gravityMask, 0, 12);
        errorToast.show();
    }

    public static void cancelToast() {
        if (errorToast != null) {
            errorToast.cancel();
        }
    }
}
