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
 * FanPoint.java
 */
package com.deltacontrols.virtualstat.points;

import java.util.LinkedHashMap;

/**
 * Fan
 */
public class FanPoint extends VirtualStatPoint {

    public FanOverridePoint override;

    // --------------------------------------------------------------------------------
    // State mappings
    // --------------------------------------------------------------------------------
    // Store MV states for now.
    public static final String AutoString = "Auto";
    public static final LinkedHashMap<String, String> FanStateByValue = new LinkedHashMap<String, String>();
    public static final LinkedHashMap<String, String> FanValueByState = new LinkedHashMap<String, String>();
    static {
        // Note: State1 corresponds to "auto".

        FanStateByValue.put("5", "III");
        FanStateByValue.put("4", "II");
        FanStateByValue.put("3", "I");
        FanStateByValue.put("2", "0");

        FanValueByState.put("III", "5");
        FanValueByState.put("II", "4");
        FanValueByState.put("I", "3");
        FanValueByState.put("0", "2");
    }

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public FanPoint() {
        super();
        init();
    }

    public FanPoint(String statFan) {
        super(statFan);
        init();
    }

    public FanPoint(String fan, String fanOverride) {
        super(fan);
        override = new FanOverridePoint(fanOverride);
    }

    public void init() {
        override = new FanOverridePoint();
    }

    // --------------------------------------------------------------------------------
    // Method
    // --------------------------------------------------------------------------------
    /**
     * QERRs for either value or override should produce false (not manual, is auto)
     */
    public boolean getManualState() {

        boolean result = false;

        if (mType == Type.MULTISTATE) {
            // If not state 1 and not in error, then manual
            result = !(mValue.equals("1") || mValue.startsWith("QERR"));
        }
        else if (mType == Type.ANALOG) {
            // If override not set (TODO error?) or override value is acitve, then manual.
            result = (override == null) || (override.getValueFormatted().equals(VirtualStatPoint.ActiveString));
        }

        return result;
    }

    public void setManualState(boolean set, VirtualStatPoint.SetBy setBy) {

        String newValue;

        if (mType == Type.MULTISTATE) {
            if (set) {
                // If setting manual and we do not yet have a manual state, set it to 2.
                newValue = (mValue.equals("1")) ? "2" : mValue;
            }
            else {
                // Auto = state1
                newValue = "1";
            }

            this.setValue(newValue, setBy);
        }
        else if (mType == Type.ANALOG) {
            // Set override
            newValue = (set) ? VirtualStatPoint.ActiveString : VirtualStatPoint.InactiveString;
            override.setValue(newValue, setBy);
            override.isDirty = true;
        }
    }

    // --------------------------------------------------------------------------------
    // Overridden methods
    // --------------------------------------------------------------------------------
    /*
     * Returns formatted fan value Note: Not currently reading multistate values from eWEB; we use hardcoded values from 
     * {@link FanPoint.FanStateByValue} and {@link FanPoint.FanValueByState}
     * @see com.deltacontrols.virtualstat.points.VirtualStatPoint#getValueFormatted()
     */
    @Override
    public String getValueFormatted() {

        String result = mValue;
        String percentString = "%";

        if (!mValue.startsWith("QERR")) {
            if (mType == Type.MULTISTATE) {
                result = mValue.equals("1") ? AutoString : FanStateByValue.get(mValue); // State1 = auto
                if (result == null) {
                    result = mValue;
                }
            }
            else if (mType == Type.ANALOG) {
                // Determine if in auto by using override point
                if (override.getValueFormatted().equals(VirtualStatPoint.InactiveString)) {
                    result = AutoString;
                }
                else {
                    try {
                        // Round value to int
                        int iVal = (int) Math.round(Double.parseDouble(mValue));
                        result = String.valueOf(iVal) + percentString;
                    } catch (Exception e) {
                        result = mValue;
                    }
                }
            }
            else {
                result = "QERR_CLASS_OBJECT::QERR_CODE_UNKNOWN_OBJECT";
            }
        }

        return result;
    }

    /**
     * Blinds may be {@link VirtualStatPoint.Type.MULTISTATE} or {@link VirtualStatPoint.Type.ANALOG}
     */
    @Override
    public boolean isValid() {
        boolean isAnalogOk = ((mType == Type.ANALOG) && (override.isValid()));
        return (mType == Type.MULTISTATE) || isAnalogOk;
    }
}
