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
 * TempPoint.java
 */
package com.deltacontrols.virtualstat.points;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Temperature
 */
public class TempPoint extends VirtualStatPoint {

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public TempPoint() {
        super();
    }

    public TempPoint(String statFan) {
        super(statFan);
    }

    // --------------------------------------------------------------------------------
    // Overridden methods
    // --------------------------------------------------------------------------------
    /**
     * Returns formatted temp value If the value is not an error, this function will format the temperature to a single point of 
     * precision and add a degree symbol.
     * @see com.deltacontrols.virtualstat.points.VirtualStatPoint#getValueFormatted()
     */
    @Override
    public String getValueFormatted() {

        String result = mValue;
        char degreeSymbol = 0x00B0;        

        if (!mValue.startsWith("QERR")) {
            // Parse into desired format and display
            try {
                double valued = Double.parseDouble(mValue);
                DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
                decimalFormatSymbols.setDecimalSeparator('.');                
                DecimalFormat decimalFormat = new DecimalFormat("###0.0", decimalFormatSymbols);
                result = decimalFormat.format(valued) + degreeSymbol;
            } catch (Exception e) {
                result = mValue;
            }
        }
        
        return result;
    }

    /**
     * Temp may only be {@link VirtualStatPoint.Type.ANALOG}
     */
    @Override
    public boolean isValid() {
        return (mType == Type.ANALOG);
    }
}
