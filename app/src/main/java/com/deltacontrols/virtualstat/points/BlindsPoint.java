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
 * BlindsPoint.java
 */
package com.deltacontrols.virtualstat.points;

/**
 * Blinds
 */
public class BlindsPoint extends VirtualStatPoint {

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public BlindsPoint() {
        super();
    }

    public BlindsPoint(String statFan) {
        super(statFan);
    }

    // --------------------------------------------------------------------------------
    // Overridden methods
    // --------------------------------------------------------------------------------
    /*
     * Returns formatted blinds value Note: Not currently reading binary state values from eWEB; we use hardcoded values 
     * "Open|active" / "Closed|inactive" 
     * @see com.deltacontrols.virtualstat.points.VirtualStatPoint#getValueFormatted()
     */
    @Override
    public String getValueFormatted() {

        String result = mValue;
        String openString = "Open";
        String closedString = "Closed";
        String percentString = "%";

        if (!mValue.startsWith("QERR")) {
            if (mType == Type.BINARY) {
                result = (mValue.equals(VirtualStatPoint.ActiveString)) ? openString : closedString;
            }
            else if (mType == Type.ANALOG) {
                try {
                    // Round value to int
                    int iVal = (int) Math.round(Double.parseDouble(mValue));
                    // If value is not within 0-100%, we cap it.
                    if (iVal > 100) {
                        iVal = 100;
                    } else {
                        if (iVal < 0) {
                            iVal = 0;
                        }
                    }

                    result = String.valueOf(iVal) + percentString;
                } catch (Exception e) {
                    result = mValue;
                }
            }
            else {
                result = "QERR_CLASS_OBJECT::QERR_CODE_UNKNOWN_OBJECT";
            }
        }

        return result;
    }

    /**
     * Blinds may be {@link VirtualStatPoint.Type.ANALOG} or {@link VirtualStatPoint.Type.BINARY}
     */
    @Override
    public boolean isValid() {
        return (mType == Type.ANALOG) || (mType == Type.BINARY);
    }
}
