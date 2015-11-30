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
 * VirtualStatPoint.java
 */
package com.deltacontrols.virtualstat.points;

import org.json.JSONObject;

import com.deltacontrols.virtualstat.VirtualStat;

/**
 * Base class for all points in the virtual stat. Contains default functionality for all points.
 */
public class VirtualStatPoint
{
    // --------------------------------------------------------------------------------
    // Static properties
    // --------------------------------------------------------------------------------
    public static enum Type {
        ANALOG, BINARY, MULTISTATE, UNKNOWN
    }; // Object reference types

    public static enum Object {
        INPUT, OUTPUT, VARIABLE, UNKNOWN
    };

    // Who is acting on the object to determine if the value needs to be updated on the 
    // server or not.
    public static enum SetBy {
        SYSTEM, USER
    }; 

    // --------------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------------
    public static final String ActiveString = "active";
    public static final String InactiveString = "inactive";
    public static final String NotInitializedString = "VIRTUALSTAT_POINT_NOT_INITIALIZED";
    public static final String DefaultProperty = "Value";
    public static final String DefaultPriority = "10";

    public String name;         // Descriptor
    public boolean isDirty;     // If isDirty is true; then value has been changed locally but not written to the device.

    protected String mFullRef;  // Object Reference (ie. //MainSite/5600.AV1)
    protected String mValue;    // Object value (not formatted, no units, raw value)
    protected String mOldValue; // Last known value set by the system; may or may not be equal to value.
    protected Type mType;       // Reference type; note: read only - set when fullRef set
    protected String mDataType; // Object data type

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------
    public VirtualStatPoint() {
        this(null);
        setFullRef("");        
        setName("");
    }

    public VirtualStatPoint(String ref) {
        setFullRef(ref);
    }

    // --------------------------------------------------------------------------------
    // Loading helpers
    // --------------------------------------------------------------------------------
    /**
     * Given a JSONObject containing a point value from a GET api/systems call, parse out the relevant parts of data and set the point properties
     * 
     * @param json JSON for point as returned from GET api/systems
     */
    public void loadFromJSON(JSONObject json) {

        try {
            // Parse ref
            this.setFullRef(json.getString("physical"));

            // Set value
            String value = json.getString("value");
            
            this.setName(json.getString("displayName"));
            
            this.setDataType(json.getString("$base"));
            
            // If value is empty assume it is uninitialized
            if (value.equals("")) {
                value = VirtualStatPoint.NotInitializedString;
            }

            if (!value.equals(VirtualStatPoint.NotInitializedString)) {
                // NOTE If ActiveString and InactiveString ever change, we may need to adapt capitalization here.
                this.setValue(value, SetBy.SYSTEM);
            }

        } 
        catch (Exception e) { 
            /* Non mapped values will get caught here, catch quietly */
        }
    }

    // --------------------------------------------------------------------------------
    // Override these methods
    // --------------------------------------------------------------------------------
    /**
     * Returns the formatted value for the point. Note: Should be overridden by classes extending VirtualStatPoint.
     * 
     * @return String Formatted point value or {@link NotInitializedString} if the point value has not yet been set.
     */
    public String getValueFormatted() {
        return mValue;
    }

    /**
     * Returns if the point is considered 'valid' Note: Value may still be {@link NotInitializedString}; this function uses the 
     * reference only to determine if the point is valid or not. Note: Should be overridden by classes extending VirtualStatPoint.
     * 
     * @return true if the reference is considered 'valid'
     */
    public boolean isValid() {
        return true;
    }

    // --------------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------------
    /**
     * Sets default values for the point
     */
    private void resetDefaultValues() {
        name = null;
        isDirty = false;

        mFullRef = "";
        mType = Type.UNKNOWN;
        mValue = NotInitializedString;
        mOldValue = NotInitializedString;
    }

    /**
     * Returns the full reference string
     * 
     * @return String The full reference; if reference not set, will return an empty string.
     */
    public String getFullRef() {
        return mFullRef;
    }

    /**
     * Sets the point to use the new full reference string. Will reset to default property values, updates fullRef and Type. Note: Values for old ref will be destroyed.
     */
    public void setFullRef(String ref) {

        if (ref == null) { // do not allow null ref
            return;
        }

        resetDefaultValues();
        mFullRef = ref.trim();
        mType = getType(mFullRef);     
    }

    /**
     * Returns the point type associated with the point's full reference.
     * 
     * @return The point type
     */
    public Type getType() {
        return mType;
    }


    /**
     * Sets the object data type
     * 
     */    
    public void setDataType(String dataType){
    	mDataType = dataType;
    }

    /**
     * Gets the object data type
     * 
     */    
    public String getDataType(){
    	return mDataType;
    }
    
    /**
     * Returns the raw value for the point
     * 
     * @return String The current value or {@link NotInitializedString} if the point value has not yet been set.
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Returns the point's 'oldValue' (the last known value verified from eWEB)
     * 
     * @return String The point's 'oldValue' or {@link NotInitializedString} if the point value has not yet been set.
     */
    public String getOldValue() {
        return mOldValue;
    }

    /**
     * Sets the object's value. If the value is changed by the user, then the object is flagged as dirty and set to manual so that it can be 
     * included in the next communication with eWEB. If the value is changed by the system (ie. value coming in from eWEB), we do not update the 
     * flags, oldValue will be updated and only if the object is not dirty will the main value be updated to match.
     * 
     * @param newValue The String containing the new value
     * @param setByFlag Indicates who is calling the set: the system or the user.
     * @return True if the value was updated; false if not.
     */
    public boolean setValue(String newValue, SetBy setByFlag) {

        boolean update = true;

        // Handle change by user: set to dirty and set to manual put in manual.
        if (setByFlag == SetBy.USER) {

            // Do not update if value has not changed.
            if (newValue.equals(mValue)) {
                return false; // Did not update value.
            }

            isDirty = true;
            mValue = newValue;

            // Set oldValue only if it has not yet been initialized.
            if (mOldValue.equals(NotInitializedString)) {
                mOldValue = mValue;
            }
        }
        // Handle change by system: set old value, and only set value if not dirty.
        else {
            mOldValue = newValue;

            if (isDirty) {
                update = false; // Did not update value.
            }
            else {
                mValue = newValue;
            }
        }

        return update;
    }

    /**
     * Attempts to restore the point's value to use the 'oldValue' assuming the point is not dirty. This function is primarily used when updating 
     * point values using values from the server - we do not want to overwrite any dirty values (ie. values that have been changed by the user but 
     * have not been sent to eWEB) as that would cause confusion in the UI.
     */
    public void restoreOldValueIfNotDirty() {
        if (!(isDirty || mOldValue.equals(NotInitializedString))) {
            mValue = mOldValue;
        }
    }

    /**
     * Creates the Object XML for the point which is used in {@link VirtualStat#asXMLStr(boolean)}. The XML will contain the object reference with 
     * it's most current value
     * 
     * @return XML String for the Object
     */
    public String asXMLStr() {

        if (mFullRef == "") {
            return "";
        }

        StringBuffer xml = new StringBuffer();
        xml.append("<Object ref=\"" + mFullRef + "\">");

        // Handle null value, we will more than likely never do this in this app - putting this here for completeness.
        if (mValue == null) {
            xml.append("<Property name=\"" + DefaultProperty + "\" priority=\"" + DefaultPriority + "\" value=\"\" isNULL=\"TRUE\" />");
        }
        // All other values
        else {
            xml.append("<Property name=\"" + DefaultProperty + "\" priority=\"" + DefaultPriority + "\" value=\"" + mValue + "\" />");
        }
     
        // Close off object node
        xml.append("</Object>");

        return xml.toString();
    }

    /**
     * Returns true if the point is considered 'setup'; to be setup, a point must have a reference set.
     * 
     * @return
     */
    public boolean isSetup() {
        return mFullRef.endsWith("");
    }

    // --------------------------------------------------------------------------------
    // Public Static functions
    // --------------------------------------------------------------------------------
    /**
     * Given an object reference string, determine the object type.
     *  e.g. "/.bacnet/MainSite/5601/multi-state-value,51"
     * @param ref Reference string to parse
     * @return Type of the object; if object is unknown or incorrectly formatted, will return {@link Type#UNKNOWN}.
     */
    public static Type getType(String ref) {
        Type type = Type.UNKNOWN;
        if (ref == null) {
            return type;
        }
        if (ref.length() < 10){
        	return type;
        }

        String[] split = ref.split("/");
        String objAbbr = null;

        for (int i = 0; i < split.length; i++) {
            if (split[i].contains(",")) {
            	// Seperate the object type and object instance
            	split = split[i].split(",");
            	// Extract the object type
            	if (split[0].contains("-"))
            		objAbbr = split[0];
                break;
            }
        }

        if (objAbbr != null) {
            objAbbr = objAbbr.replaceAll("[0-9]*$", "").toLowerCase(); // remove trailing numbers

            if (objAbbr.equals("analog-output") || objAbbr.equals("analog-input") || objAbbr.equals("analog-value")) {
                type = Type.ANALOG;
            }
            else if (objAbbr.equals("binary-output") || objAbbr.equals("binary-input") || objAbbr.equals("binary-value")) {
                type = Type.BINARY;
            }
            else if (objAbbr.equals("multi-state-output") || objAbbr.equals("multi-state-input") || objAbbr.equals("multi-state-value")) {
                type = Type.MULTISTATE;
            }
        }

        return type;
    }
    
    /**
     * Returns the Object Name
     * 
     * @return String object name; if reference not set, will return an empty string.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the Object Name
     */
    public void setName(String objName) {

        name = objName;
    }    
}
