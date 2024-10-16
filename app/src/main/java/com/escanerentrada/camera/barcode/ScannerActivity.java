package com.escanerentrada.camera.barcode;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.escanerentrada.R;
import com.escanerentrada.camera.BaseCameraActivity;

public class ScannerActivity extends BaseCameraActivity implements BarcodeListener {

    private Button btnCancelar;
    private CameraSelector cameraSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(v -> finish());

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
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

    @Override
    protected int getLayoutId() {
        return R.layout.activity_scanner;
    }

    @Override
    protected int getPreviewViewId() {
        return R.id.previewView;
    }

    @Override
    public void onBarcodeDetected(String barcode) {
        runOnUiThread(() -> {
            Intent intent = new Intent();
            intent.putExtra("codigo", barcode);
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}