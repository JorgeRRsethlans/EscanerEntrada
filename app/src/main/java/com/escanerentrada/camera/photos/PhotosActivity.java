package com.escanerentrada.camera.photos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import com.escanerentrada.ssh.SSHTask;
import com.escanerentrada.ssh.SSHHelper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase que se encarga de tomar fotos y subirlas al servidor.
 */
public class PhotosActivity extends BaseCameraActivity {

    private String remotePath;
    private String username, password;

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
        btnAceptar.setOnClickListener(view -> {
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
            assert i != null;
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });

        remotePath = getIntent().getStringExtra("remotePath");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");

        if (remotePath == null || remotePath.isEmpty()) {
            Toast.makeText(this, "Directorio remoto no válido", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Método que se ejecuta para obtener el ID del layout.
     *
     * @return ID del layout
     */
    @Override
    protected int getLayoutId() { return R.layout.activity_photos; }

    /**
     * Método que se ejecuta para obtener el ID del layout.
     *
     * @return ID del layout
     */
    @Override
    protected int getPreviewViewId() { return R.id.previewView; }

    /**
     * Método que se ejecuta para tomar una foto y subirla al servidor.
     */
    private void takePhotoAndUpload() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shitter_sound);
        mediaPlayer.start();
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(150);
        }
        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                executor.execute(() -> {
                    Bitmap bitmap = imageProxyToBitmap(image);
                    image.close();

                    Runnable uploadTask = () -> {
                        try {
                            SSHHelper sshHelper = new SSHHelper(username, password,
                                    "romantic-engelbart.212-227-226-16.plesk.page", 22);

                            String uniqueKey = namePhoto();
                            String remoteFilePath = remotePath + "/" + uniqueKey + ".jpg";

                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            byte[] bitmapData = bos.toByteArray();

                            sshHelper.uploadImage(bitmapData, remoteFilePath);
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                                    "Error al subir la foto: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                        }
                    };

                    new SSHTask(PhotosActivity.this, uploadTask).execute();
                });
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
     * Método que se ejecuta para convertir una imagen a bitmap.
     *
     * @param image Imagen a convertir
     * @return Bitmap de la imagen
     */
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Método que se ejecuta para generar una clave única.
     *
     * @return Clave única
     */
    private String namePhoto() {
        Date ahora = new Date();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formato = new SimpleDateFormat("ddMMyyyy_HHmmss");
        return "foto-" + formato.format(ahora);
    }
}