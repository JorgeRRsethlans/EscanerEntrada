package com.escanerentrada.camera.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import com.escanerentrada.R;
import com.escanerentrada.camera.BaseCameraActivity;

import java.nio.ByteBuffer;
import java.util.UUID;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * Clase que se encarga de hacer las fotos y subirlas al servidor.
 */
public class PhotosActivity extends BaseCameraActivity {

    private String directorio;

    /**
     * Método que se ejecuta cuando se crea la actividad.
     *
     * @param state Estado de la actividad
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        Button btnAtras = findViewById(R.id.btnAtras);
        Button btnFoto = findViewById(R.id.btnFoto);
        Button btnAceptar = findViewById(R.id.btnAceptar);

        btnAtras.setOnClickListener(view -> finish());

        btnFoto.setOnClickListener(view -> takePhotoAndUpload());

        btnAceptar.setOnClickListener(view -> finish());

        directorio = getIntent().getStringExtra("directorio");
        if(directorio == null || directorio.isEmpty()) {
            Toast.makeText(this, "Directorio no válido", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Método que se ejecuta para obtener el layout.
     *
     * @return Id del layout
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_photos;
    }

    /**
     * Método que se ejecuta para obtener el id del preview view.
     *
     * @return Id del preview view
     */
    @Override
    protected int getPreviewViewId() {
        return R.id.previewView;
    }

    /**
     * Método que se ejecuta para tomar la foto y subirla al servidor.
     */
    private void takePhotoAndUpload() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shitter_sound);
        mediaPlayer.start();
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(100);
        }
        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                runOnUiThread(() -> uploadPhoto(bitmap));
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                        "Error al tomar la foto: " + exception.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Método que se ejecuta para convertir la imagen a bitmap.
     *
     * @param image Imagen a convertir
     *
     * @return Imagen convertida
     */
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] bytes = image.getPlanes()[0].getBuffer().array();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Método que se ejecuta para subir la foto al servidor.
     *
     * @param bitmap Imagen a subir
     */
    private void uploadPhoto(Bitmap bitmap) {
        String nombreArchivo = generateUniqueKey() + ".jpg";
        String rutaArchivo = directorio + nombreArchivo;
        try {
            SmbFile archivoSmb = new SmbFile(rutaArchivo);
            SmbFileOutputStream out = new SmbFileOutputStream(archivoSmb);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            runOnUiThread(() -> Toast.makeText(PhotosActivity.this, "Imagen subida correctamente",
                    Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(PhotosActivity.this,
                    "Error al subir la imagen: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método que se ejecuta para generar una clave única.
     *
     * @return Clave generada
     */
    private String generateUniqueKey() {
        return UUID.randomUUID().toString();
    }
}