package com.escanerentrada.pdf;

import android.content.Context;
import android.content.SharedPreferences;

public class CredentialManager {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_IS_FIRST_RUN = "isFirstRun";

    private Context context;

    public CredentialManager(Context context) {
        this.context = context;
    }

    public void saveCredentials(String username, String password) {
        SharedPreferences sharedPreferences = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }

    public String[] loadCredentials() {
        SharedPreferences sharedPreferences = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        String password = sharedPreferences.getString(KEY_PASSWORD, "");
        return new String[]{username, password};
    }

    public boolean isFirstRun() {
        SharedPreferences sharedPreferences = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean(KEY_IS_FIRST_RUN, true);

        if(isFirstRun) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_FIRST_RUN, false);
            editor.apply();
        }

        return isFirstRun;
    }
}
