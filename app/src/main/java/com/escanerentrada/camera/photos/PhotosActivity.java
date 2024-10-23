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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Clase que se encarga de hacer las fotos y subirlas al servidor.
 */
public class PhotosActivity extends BaseCameraActivity {

    private String remotePath;
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
                        uploadPhotoToMega(bitmap);
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
     * Método que se ejecuta para subir la foto a MEGA.
     *
     * @param bitmap Imagen a subir
     */
    private void uploadPhotoToMega(Bitmap bitmap) {
        String nombreArchivo = generateUniqueKey() + ".jpg";
        String rutaMega = remotePath + "/" + nombreArchivo; // Ruta completa en MEGA

        executor.execute(() -> {
            try {
                File archivoTemporal = new File(getCacheDir(), nombreArchivo);
                FileOutputStream fos = new FileOutputStream(archivoTemporal);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                executeMEGAcmd("put", "--path", rutaMega, archivoTemporal.getAbsolutePath());

                boolean deleteSuccess = archivoTemporal.delete();
                if (!deleteSuccess) {
                    runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                            "Error al eliminar el archivo temporalmente guardado en cache.",
                            Toast.LENGTH_SHORT).show());
                }

                runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                        "Imagen subida correctamente", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(PhotosActivity.this,
                        "Error al subir la imagen: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Método que se ejecuta para ejecutar el comando MEGAcmd.
     *
     * @param commands Comandos a ejecutar
     */
    private void executeMEGAcmd(String... commands) {
        try {
            String executablePath = getFilesDir() + "/MEGAcmd-Linux-aarch64";
            String[] fullCommand = new String[commands.length + 1];
            fullCommand[0] = executablePath;
            System.arraycopy(commands, 0, fullCommand, 1, commands.length);
            Process process = Runtime.getRuntime().exec(fullCommand);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process
                    .getInputStream()));
            String line;
            while((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if(exitCode != 0) {
                Toast.makeText(this,
                        "MEGAcmd exited with code: " + exitCode,
                        Toast.LENGTH_LONG).show();
            }
        } catch(IOException | InterruptedException e) {
            Toast.makeText(this,
                    "Error al ejecutar MEGAcmd: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
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