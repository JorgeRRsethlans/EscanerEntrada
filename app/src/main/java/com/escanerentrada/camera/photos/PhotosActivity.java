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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.escanerentrada.R;
import com.escanerentrada.camera.BaseCameraActivity;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Clase que se encarga de hacer las fotos y subirlas al servidor.
 */
public class PhotosActivity extends BaseCameraActivity {

    private String remotePath;
    private String username, password;

    private boolean cameraReady = false;


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

        remotePath = getIntent().getStringExtra("remotePath");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");

        if(remotePath == null || remotePath.isEmpty()) {
            Toast.makeText(this, "Directorio remoto no válido", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
        restartCamera();
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
        if(cameraReady) {
            imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    executor.execute(() -> {
                        Bitmap bitmap = imageProxyToBitmap(image);
                        image.close();
                        try {
                            uploadPhoto(bitmap, remotePath, username, password);

                        } catch(Exception e) {
                            runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                                    "Error al subir la foto: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                        }
                        imageCapture = new ImageCapture.Builder().build();
                    });
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                            "Error al tomar la foto: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            Toast.makeText(PhotosActivity.this,
                    "La cámara no está lista", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método que se ejecuta para convertir la imagen a bitmap.
     *
     * @param image Imagen a convertir
     *
     * @return Imagen convertida
     */
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Método que se ejecuta para subir la foto al servidor.
     *
     * @param bitmap Imagen a subir
     * @param remoteFolderPath Ruta de la carpeta en el servidor
     * @param username Nombre de usuario
     * @param password Contraseña
     * @throws Exception Excepción en caso de error
     */
    private void uploadPhoto(Bitmap bitmap, String remoteFolderPath, String username, String password)
            throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username,
                "romantic-engelbart.212-227-226-16.plesk.page", 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        String uniqueKey = generateUniqueKey();
        String remoteFilePath = remoteFolderPath + "/" + uniqueKey + ".jpg";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] bitmapData = bos.toByteArray();

        sftpChannel.put(new ByteArrayInputStream(bitmapData), remoteFilePath);

        sftpChannel.disconnect();
        session.disconnect();
    }

    /**
     * Método que se ejecuta para generar una clave única.
     *
     * @return Clave generada
     */
    private String generateUniqueKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Método que se ejecuta para reiniciar la cámara.
     */
    private void restartCamera() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindPreview(cameraProvider);
                imageCapture = new ImageCapture.Builder().build();
                cameraProvider.bindToLifecycle(this,
                        cameraSelector, preview, imageCapture);
                cameraReady = true;
            } catch(Exception e) {
                Toast.makeText(this,
                        "Error al reiniciar la camara.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}