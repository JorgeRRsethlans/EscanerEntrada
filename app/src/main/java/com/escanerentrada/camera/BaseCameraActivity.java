package com.escanerentrada.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Clase base para las actividades de la cámara.
 */
public abstract class BaseCameraActivity extends AppCompatActivity {

    protected ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    protected ExecutorService cameraExecutor;
    protected PreviewView previewView;
    protected Preview preview;

    protected ImageCapture imageCapture ;
    protected Executor executor = Executors.newSingleThreadExecutor();

    private static final int PERMISSIONS_REQUEST_CODE = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    /**
     * Método que se ejecuta cuando se crea la actividad.
     *
     * @param state Estado de la actividad
     */
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(getLayoutId());

        previewView = findViewById(getPreviewViewId());
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Método que se ejecuta para iniciar la cámara.
     */
    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation()).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                bindPreview(cameraProvider);

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e("BaseCameraActivity", "Error al vincular el caso de uso", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Método que se ejecuta para vincular el caso de uso.
     *
     * @param cameraProvider Proveedor de la cámara
     */
    protected void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception exc) {
            Log.e("BaseCameraActivity", "Error al vincular el caso de uso", exc);
        }
    }

    /**
     * Metodo que se ejecuta para otorgar los permisos.
     *
     * @param requestCode codigo de solicitud
     * @param permissions permisos a otorgar
     * @param grantResults resultados de la solicitud
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Log.e("BaseCameraActivity", "Permisos no otorgados");
                finish();
            }
        }
    }

    /**
     * Metodo que se ejecuta para verificar si se otorgaron los permisos.
     *
     * @return true si se otorgaron los permisos, false en caso contrario
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Método que se ejecuta cuando se destruye la actividad.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    /**
     * Método que se ejecuta para obtener el layout.
     *
     * @return Id del layout
     */
    protected abstract int getLayoutId();

    /**
     * Método que se ejecuta para obtener el id del preview view.
     *
     * @return Id del preview view
     */
    protected abstract int getPreviewViewId();
}
