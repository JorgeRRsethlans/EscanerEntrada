package com.escanerentrada;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.escanerentrada.camera.barcode.ScannerActivity;
import com.escanerentrada.pdf.PdfCreator;
import com.escanerentrada.pdf.ViewPdfActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Clase principal que da acceso a escanear un código de barras o a introducirlo manualmente.
 * También se encarga de verificar los permisos relacionados con el acceso a la camara.
 *
 * @author Jorge Rodríguez Rabanal
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final int CODIGO_PERMISOS_CAMARA = 1, CODIGO_INTENT = 2;
    private boolean permisoCamaraConcedido = false;
    private EditText codigo;

    /**
     * Método que se ejecuta al iniciar la actividad.
     *
     * @param state Bundle
     */
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnEscanear = findViewById(R.id.btnEscanear);
        Button btnSiguiente = findViewById(R.id.btnSiguiente);
        codigo = findViewById(R.id.codigo);

        btnEscanear.setOnClickListener(view -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50);
            }
            verificarYPedirPermisosDeCamara();
            if(permisoCamaraConcedido) {
                escanear();
            }
        });

        btnSiguiente.setOnClickListener(view -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50);
            }
            if(codigo.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "No se ha introducido ningún código",
                        Toast.LENGTH_SHORT).show();
            } else {
                String codigoBarras = codigo.getText().toString();
                String[] contenido = {codigoBarras};
                PdfCreator.createPdf(MainActivity.this,
                        getCacheDir().getAbsolutePath() + "/etiqueta.pdf", contenido);
                Intent i = new Intent(MainActivity.this, ViewPdfActivity.class);
                i.putExtra("stringextra", getCacheDir().getAbsolutePath() +
                        "/etiqueta.pdf");
                startActivity(i);
            }
        });
    }

    /**
     * Método que nos da acceso a un Intent para escanear el código de barras.
     */
    private void escanear() {
        Intent i = new Intent(MainActivity.this, ScannerActivity.class);
        startActivityForResult(i, CODIGO_INTENT);
    }

    /**
     * Método que nos devuelve el código de barras en ASCII.
     *
     * @param requestCode int
     * @param resultCode int
     * @param data Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CODIGO_INTENT) {
            if(resultCode == Activity.RESULT_OK) {
                if(data != null) {
                    String textoCodigo = data.getStringExtra("codigo");
                    codigo.setText(textoCodigo);
                }
            }
        }
    }

    /**
     * Método que verifica y pide permisos de acceso a la cámara.
     */
    private void verificarYPedirPermisosDeCamara() {
        int estadoPermiso = ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.CAMERA);
        if(estadoPermiso == PackageManager.PERMISSION_GRANTED) {
            permisoCamaraConcedido = true;
        } else if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA)) {
            permisoCamaraDenegado();
        } else {
            permisoCamaraDenegado();
        }
    }

    /**
     * Método que comprueba si el permiso de la camara ha sido concedido y, dependiendo del
     * resutado, ejecuta la cámara o muestra un mensaje de error.
     *
     * @param requestCode int
     * @param permissions String[]
     * @param grantResults int[]
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISOS_CAMARA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permisoCamaraConcedido = true;
                escanear();
            }
        }
    }

    /**
     * Método que muestra un mensaje por pantalla cuando el acceso a la cámara sea denegado.
     */
    private void permisoCamaraDenegado() {
        Snackbar.make(findViewById(android.R.id.content),
            "Se necesita permiso para poder escanear", Snackbar.LENGTH_INDEFINITE)
            .setAction("Otorgar", view -> ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CODIGO_PERMISOS_CAMARA)).show();
    }
}