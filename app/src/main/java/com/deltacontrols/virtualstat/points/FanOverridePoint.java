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
 * FanOverridePoint.java
 */
package com.deltacontrols.virtualstat.points;

/**
 * Fan Override
 */
public class FanOverridePoint extends VirtualStatPoint {
    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public FanOverridePoint() {
        super();
    }

    public FanOverridePoint(String statFanOverride) {
        super(statFanOverride);
    }

    // --------------------------------------------------------------------------------
    // Overridden methods
    // --------------------------------------------------------------------------------
    /*
     * Returns formatted fan override value
     */
    @Override
    public String getValueFormatted() {

        String result = mValue;

        if (!mValue.startsWith("QERR")) {
            if (mType == Type.MULTISTATE) {
                result = mValue.equals("1") ? VirtualStatPoint.InactiveString : VirtualStatPoint.ActiveString; // State1 = Off = Auto
            }
            else if (mType == Type.BINARY) {
                // Do nothing, use raw value which should be VirtualStatPoint.InactiveString or VirtualStatPoint.ActiveString
            }
            else {
                result = "QERR_CLASS_OBJECT::QERR_CODE_UNKNOWN_OBJECT";
            }
        }

        return (result);
    }

    /**
     * Override getValue so that we can shelter the UI from having to know if we are an MV or a BV. newValue should always be 
     * VirtualStatPoint.InactiveString or VirtualStatPoint.ActiveString; need to convert if we are MV
     */
    @Override
    public String getValue() {
        return mValue;
    }

    /**
     * Override setValue so that we can shelter the UI from having to know if we are an MV or a BV. newValue should always be 
     * VirtualStatPoint.InactiveString or VirtualStatPoint.ActiveString; need to convert if we are MV
     */
    @Override
    public boolean setValue(String newValue, SetBy setByFlag) {

        if (mType == Type.MULTISTATE) {
            // Only format if active/inactive strings found
            if (newValue.equals(VirtualStatPoint.InactiveString)) {
                newValue = "1";
            }
            else if (newValue.equals(VirtualStatPoint.ActiveString)) {
                newValue = "2";
            }
        }
        return super.setValue(newValue, setByFlag);
    }

    /**
     * Blinds may be {@link VirtualStatPoint.Type.MULTISTATE} or {@link VirtualStatPoint.Type.BINARY}
     */
    @Override
    public boolean isValid() {
        return (mType == Type.MULTISTATE) || (mType == Type.BINARY);
    }
}
