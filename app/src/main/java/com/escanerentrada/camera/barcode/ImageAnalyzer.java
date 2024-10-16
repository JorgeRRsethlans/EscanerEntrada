package com.escanerentrada.camera.barcode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

/**
 * Clase que se encarga de analizar las imágenes de la cámara.
 */
public class ImageAnalyzer  implements ImageAnalysis.Analyzer{

    private final Context context;
    private String codigo;
    private BarcodeListener listener;

    /**
     * Constructor de la clase.
     * @param context
     */
    public ImageAnalyzer(Context context, BarcodeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Método que se ejecuta para analizar una imagen.
     * @param imageProxy The image to analyze
     */
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
        if(mediaImage != null) {
            InputImage image = InputImage
                    .fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();
            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            Task<List<Barcode>> result = scanner.process(image).addOnSuccessListener(barcodes -> {
                for(Barcode barcode : barcodes) {
                    codigo = barcode.getRawValue();
                    listener.onBarcodeDetected(codigo);
                }
            }).addOnFailureListener(e -> Toast.makeText(context,
                    "Error al escanear el código de barras", Toast.LENGTH_SHORT).show())
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }
}
