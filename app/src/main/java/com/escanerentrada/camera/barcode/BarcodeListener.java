package com.escanerentrada.camera.barcode;

/**
 * Interfaz que se encarga de detectar el código de barras.
 */
public interface BarcodeListener {
    void onBarcodeDetected(String barcode);
}
