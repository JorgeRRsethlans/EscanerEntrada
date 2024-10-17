package com.escanerentrada.pdf;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Clase que se encarga de gestionar las credenciales del usuario.
 */
public class CredentialManager {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_IS_FIRST_RUN = "isFirstRun";

    private final Context context;

    /**
     * Constructor de la clase.
     *
     * @param context Contexto de la aplicación
     */
    public CredentialManager(Context context) {
        this.context = context;
    }

    /**
     * Método que guarda las credenciales del usuario.
     *
     * @param username Nombre de usuario
     * @param password Contraseña
     */
    public void saveCredentials(String username, String password) {
        SharedPreferences sharedPreferences = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }

    /**
     * Método que carga las credenciales del usuario.
     *
     * @return Credenciales del usuario
     */
    public String[] loadCredentials() {
        SharedPreferences sharedPreferences = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        String password = sharedPreferences.getString(KEY_PASSWORD, "");
        return new String[]{username, password};
    }

    /**
     * Método que comprueba si es la primera ejecución de la aplicación.
     *
     * @return Si es la primera ejecución
     */
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
