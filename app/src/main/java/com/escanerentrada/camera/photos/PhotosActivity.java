package com.escanerentrada.camera.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.escanerentrada.R;
import com.escanerentrada.camera.BaseCameraActivity;

import java.util.UUID;

public class PhotosActivity extends BaseCameraActivity {

    private Button btnAtras;
    private Button btnFoto;
    private Button btnAceptar;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        btnAtras = findViewById(R.id.btnAtras);
        btnFoto = findViewById(R.id.btnFoto);
        btnAceptar = findViewById(R.id.btnAceptar);

        btnAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhotoAndCache();
            }
        });

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_photos;
    }

    @Override
    protected int getPreviewViewId() {
        return R.id.previewView;
    }

    private void takePhotoAndCache() {
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
                cacheImageWithGlide(bitmap, generateUniqueKey());
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(PhotosActivity.this,
                        "Error al tomar la foto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        byte[] bytes = image.getPlanes()[0].getBuffer().array();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void cacheImageWithGlide(Bitmap bitmap, String key) {
        Glide.with(this)
                .asBitmap()
                .load(bitmap)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .submit();
    }

    private String generateUniqueKey() {
        return UUID.randomUUID().toString();
    }

}