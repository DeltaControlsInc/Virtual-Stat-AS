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
 * LightsPoint.java
 */
package com.deltacontrols.virtualstat.points;

import android.content.Context;

import com.deltacontrols.virtualstat.R;

/**
 * Lights
 * 
 * Note: The virtual stat can contain up to 4 lights; the LightsPoint class contains some static functionality that provides the logic 
 * for determining values based on arrays of lights.
 * 
 * Note: Currently we hardcode the values ON / OFF for the binary value, in the future the state list could be acquired from eWEB and used. 
 * ON / OFF can be found in the strings resource file for the application.
 */
public class LightsPoint extends VirtualStatPoint {

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public LightsPoint() {
        super();
    }

    public LightsPoint(String statLights) {
        super(statLights);
    }

    // --------------------------------------------------------------------------------
    // Overridden methods
    // --------------------------------------------------------------------------------
    /**
     * Lights may be {@link VirtualStatPoint.Type.ANALOG} or {@link VirtualStatPoint.Type.BINARY}
     */
    @Override
    public boolean isValid() {
        return (mType == Type.ANALOG) || (mType == Type.BINARY);
    }

    // --------------------------------------------------------------------------------
    // Static functions
    // --------------------------------------------------------------------------------
    /**
     * Note, not the same as isValid(), but serves the same purpose; takes an array of LightPoints to determine if ANY ONE is valid or not. 
     * Lights may be {@link VirtualStatPoint.Type.ANALOG} or {@link VirtualStatPoint.Type.BINARY}
     */
    public static boolean isValid(LightsPoint[] lights) {
        boolean atLeastOneValid = false;

        for (LightsPoint point : lights) {
            if (point.isValid()) {
                atLeastOneValid = true;
                break;
            }
        }
        return atLeastOneValid;
    }

    /**
     * Given an array of LightPoints, determine what the summary value should be.
     * 
     * @param ctx Calling activity context
     * @param lights Array of LightPoints
     * @return onString if any of the LightPoints are considered 'On' (ie. > 0 or 'Active'), else offString.
     */
    public static String getSummaryValueFormatted(Context ctx, LightsPoint[] lights) {

        String result, value;
        String offString = ctx.getString(R.string.value_off);
        String onString = ctx.getString(R.string.value_on);
        int numErrors = 0;

        result = offString;
        for (int i = 0; i < lights.length; i++) {
            value = lights[i].getValue();

            // If not initialized or QERR, then count as error and skip.
            if (value.equals(VirtualStatPoint.NotInitializedString) || value.startsWith("QERR")) {
                numErrors++;
                continue;
            }

            try {
                // Analog + value greater than 0 ==> ON
                float fValue = Float.parseFloat(value);

                if (fValue > 0) {
                    result = onString;
                    break;
                }
            } catch (Exception e) {
                // Binary + value "active" ==> ON
                if (value.equals(VirtualStatPoint.ActiveString)) {
                    result = onString;
                    break;
                }
            }
        }

        if (numErrors == lights.length) {
            result = lights[0].getValue(); // For now take the first error.
        }

        return result;
    }

    /**
     * Given an array of LightPoints, determine if the lights should be considered disabled.
     * 
     * @param ctx Calling activity context
     * @param lights Array of LightPoints
     * @return true if any of the LightPoints contains a reference (ie. is set), else false
     */
    public static boolean lightsDisabled(Context ctx, LightsPoint[] lights) {
        return lights[0].getFullRef().equals("")
                && lights[1].getFullRef().equals("")
                && lights[2].getFullRef().equals("")
                && lights[3].getFullRef().equals("");
    }
}
