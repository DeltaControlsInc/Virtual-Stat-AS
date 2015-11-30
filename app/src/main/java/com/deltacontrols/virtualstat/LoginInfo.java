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
package com.deltacontrols.virtualstat;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginInfo {

    private static final String SHARED_PREF_SERVER_ID = "SERVER";
    private static final String SHARED_PREF_USERNAME_ID = "USERNAME";
    private static final String SHARED_PREF_PASSWORD_ID = "PASSWORD";
    private static final String SHARED_PREF_AUTOLOGIN_ID = "AUTOLOGIN";

    public String eWebURL;
    public String userName;
    public String password;
    public boolean autoLogin;

    public LoginInfo() {
        init();
    }

    public LoginInfo(String eWebURL, String userName, String password, boolean autoLogin) {
        this.eWebURL = eWebURL;
        this.userName = userName;
        this.password = password;
        this.autoLogin = autoLogin;

    }

    private void init() {
        this.eWebURL = "";
        this.userName = "";
        this.password = "";
        this.autoLogin = false;
    }

    // --------------------------------------------------------------------------------
    // Class Methods
    // --------------------------------------------------------------------------------
    public boolean isValid() {
        return LoginInfo.isValid(this);
    }

    public void setStoredLogin() {
        setStoredLogin(this);
    }

    // --------------------------------------------------------------------------------
    // Overridden Methods
    // --------------------------------------------------------------------------------
    /**
     * Auto Generated equals override
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LoginInfo other = (LoginInfo) obj;
        if (eWebURL == null) {
            if (other.eWebURL != null)
                return false;
        } else if (!eWebURL.equals(other.eWebURL))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (userName == null) {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        return true;
    }

    /**
     * Auto Generated hashCode override
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eWebURL == null) ? 0 : eWebURL.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    // --------------------------------------------------------------------------------
    // Static Methods
    // --------------------------------------------------------------------------------
    /**
     * Helper function to get
     * 
     * @return
     */
    public static LoginInfo getStoredLogin() {
        Context ctx = App.getContext();
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(App.SHARED_PREF_ID, 0);
        return new LoginInfo(sharedPreferences.getString(LoginInfo.SHARED_PREF_SERVER_ID, ""),
                sharedPreferences.getString(LoginInfo.SHARED_PREF_USERNAME_ID, ""),
                sharedPreferences.getString(LoginInfo.SHARED_PREF_PASSWORD_ID, ""),
                sharedPreferences.getBoolean(LoginInfo.SHARED_PREF_AUTOLOGIN_ID, false));
    }

    public static void setStoredLogin(LoginInfo login) {
        Context ctx = App.getContext();
        SharedPreferences.Editor editor = ctx.getSharedPreferences(App.SHARED_PREF_ID, Context.MODE_PRIVATE).edit();
        editor.putString(LoginInfo.SHARED_PREF_SERVER_ID, login.eWebURL);
        editor.putString(LoginInfo.SHARED_PREF_USERNAME_ID, login.userName);
        editor.putString(LoginInfo.SHARED_PREF_PASSWORD_ID, login.password);
        editor.putBoolean(LoginInfo.SHARED_PREF_AUTOLOGIN_ID, login.autoLogin);
        editor.commit();
    }

    public static void logoutCurrentUser() {
        Context ctx = App.getContext();
        SharedPreferences.Editor editor = ctx.getSharedPreferences(App.SHARED_PREF_ID, Context.MODE_PRIVATE).edit();
        editor.putBoolean(LoginInfo.SHARED_PREF_AUTOLOGIN_ID, false);
        editor.commit();
    }

    public static boolean isValid(LoginInfo login) {
        // Currently only checks for all login values being non-empty
        return (!login.eWebURL.equals("") && !login.userName.equals("") && !login.password.equals(""));
    }
}
