package com.escanerentrada.camera.barcode;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.escanerentrada.R;
import com.escanerentrada.camera.BaseCameraActivity;

import java.util.concurrent.ExecutionException;

/**
 * Clase que se encarga de escanear el código de barras.
 */
public class ScannerActivity extends BaseCameraActivity implements BarcodeListener {

    private CameraSelector cameraSelector;
    private ImageAnalysis imageAnalysis;

    /**
     * Método que se ejecuta al iniciar la actividad.
     *
     * @param state Estado de la actividad
     */
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Button btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50);
            }
            finish();
        });

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalyzer(this, this));

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
            } catch (Exception e) {
                Log.e("ScannerActivity", "Error al vincular el caso de uso", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Método que devuelve el layout de la actividad.
     *
     * @return Layout de la actividad
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_scanner;
    }

    /**
     * Método que devuelve el id del preview view.
     *
     * @return Id del preview view
     */
    @Override
    protected int getPreviewViewId() {
        return R.id.previewView;
    }

    /**
     * Método que se ejecuta cuando se detecta un código de barras.
     *
     * @param barcode Código de barras
     */
    @Override
    public void onBarcodeDetected(String barcode) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(150);
        }
        runOnUiThread(() -> {
            Intent intent = new Intent();
            intent.putExtra("codigo", barcode);
            setResult(RESULT_OK, intent);
        });

        try {
            cameraProvider = ProcessCameraProvider
                    .getInstance(getApplicationContext()).get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    "Error al cerrar la camara.", Toast.LENGTH_SHORT).show();
        } finally {
            finish();
        }
    }
}