package com.escanerentrada.pdf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.escanerentrada.R;
import com.escanerentrada.camera.photos.PhotosActivity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * Clase que se encarga de mostrar el PDF.
 */
public class ViewPdfActivity extends AppCompatActivity {

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;

    private CredentialManager credentialManager;

    private ImageView imageView;
    private Button btnCamara;
    private EditText txtUsuario;
    private EditText txtContrasena;
    private Button btnGuardar;
    private Button btnCancelar;

    /**
     * Método que se ejecuta cuando se crea la actividad.
     *
     * @param state
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_viewpdf);

        imageView = findViewById(R.id.imageView);
        btnCamara = findViewById(R.id.btnCamara);
        txtUsuario = findViewById(R.id.txtUsuario);
        txtContrasena = findViewById(R.id.txtContrasena);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        String filepath = getIntent().getStringExtra("stringextra");

        try {
            File file = new File(filepath);
            if(!file.exists()) {
                Toast.makeText(this, "Error, el PDF no se ha creado",
                        Toast.LENGTH_SHORT).show();
            }
            ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor
                    .open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            showPage(currentPageIndex);
        } catch(IOException e) {
            Toast.makeText(this, "Error al cargar el PDF", Toast.LENGTH_SHORT).show();
        }

        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ViewPdfActivity.this, PhotosActivity.class);
                startActivity(i);
            }
        });

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        credentialManager = new CredentialManager(this);
        if(!credentialManager.isFirstRun()) {
            String[] credentials = credentialManager.loadCredentials();
            txtUsuario.setText(credentials[0]);
            txtContrasena.setText(credentials[1]);
        }

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View view) {
                String filepath = getIntent().getStringExtra("stringextra");
                File file = new File(filepath);

                String username = txtUsuario.getText().toString();
                String password = txtContrasena.getText().toString();
                credentialManager.saveCredentials(txtUsuario.getText().toString(),
                        txtContrasena.getText().toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords",
                                    "false");
                            jcifs.Config.setProperty("jcifs.smb.client.connectTimeout", "10000");
                            jcifs.Config.setProperty("jcifs.smb.client.responseTimeout", "10000");
                            jcifs.Config.setProperty("jcifs.smb.client.soTimeout", "10000");

                            String baseDir = "smb://" + username + ":" + password +
                                    "@192.168.1.133" +
                                    "/sethlans_administracion/R - CLOUD/DRIVE/IT/Recepciones/";
                            SmbFile smbFile = new SmbFile(baseDir);

                            if (!smbFile.exists()) {
                                smbFile.mkdirs();
                            }

                            int folderNumber = 1;
                            while (true) {
                                SmbFile folder = new SmbFile(baseDir +
                                        String.format("%04d", folderNumber) + "/");
                                if (!folder.exists()) {
                                    folder.mkdirs();
                                    break;
                                }
                                folderNumber++;
                            }

                            String destPath = baseDir + String.format("%04d", folderNumber) + "/" +
                                    file.getName();

                            SmbFileOutputStream out = new SmbFileOutputStream(new SmbFile(destPath));
                            Files.copy(file.toPath(), out);
                            out.close();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ViewPdfActivity.this,
                                            "PDF guardado.", Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ViewPdfActivity.this,
                                            "Error al guardar el PDF: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
                finish();
            }
        });
    }

    /**
     * Método que se ejecuta para mostrar una página del PDF.
     *
     * @param index
     */
    private void showPage(int index) {
        if(pdfRenderer != null && index < pdfRenderer.getPageCount()) {
            if(currentPage != null) {
                currentPage.close();
            }
            currentPage = pdfRenderer.openPage(index);
            Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(),
                    Bitmap.Config.ARGB_8888);
            currentPage.render(bitmap, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            imageView.setImageBitmap(bitmap);
            currentPageIndex = index;
        }
    }

    /**
     * Método que se ejecuta cuando se destruye la actividad.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(currentPage != null) {
            currentPage.close();
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
    }
}