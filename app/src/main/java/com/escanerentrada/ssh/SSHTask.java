package com.escanerentrada.ssh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Clase que se encarga de ejecutar una tarea en segundo plano y mostrar
 * un mensaje de error si ocurre uno.
 */
public class SSHTask extends AsyncTask<Void, Void, Void> {

    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private final Runnable task;
    private Exception exception;

    /**
     * Constructor de la clase.
     *
     * @param context Contexto de la actividad
     * @param task Tarea a ejecutar
     */
    public SSHTask(Context context, Runnable task) {
        this.context = context;
        this.task = task;
    }

    /**
     * Método que se ejecuta en segundo plano para ejecutar la tarea.
     *
     * @param voids Parámetros de la tarea
     * @return Resultado de la tarea
     */
    @Override
    protected Void doInBackground(Void... voids) {
        try {
            task.run();
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    /**
     * Método que se ejecuta cuando se finaliza la tarea.
     *
     * @param aVoid Resultado de la tarea
     */
    @Override
    protected void onPostExecute(Void aVoid) {
        if (exception != null) {
            Toast.makeText(context,
                    "Error de SSH: " + exception.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}