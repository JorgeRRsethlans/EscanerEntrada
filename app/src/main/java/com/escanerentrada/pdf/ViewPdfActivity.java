package com.escanerentrada.pdf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
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

import io.github.eliux.mega.Mega;
import io.github.eliux.mega.MegaSession;
import io.github.eliux.mega.auth.MegaAuthCredentials;
import io.github.eliux.mega.error.MegaException;
import io.github.eliux.mega.error.MegaLoginException;


/**
 * Clase que se encarga de mostrar el PDF.
 */
public class ViewPdfActivity extends AppCompatActivity {

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;

    private CredentialManager credentialManager;

    private ImageView imageView;
    private EditText txtUsuario;
    private EditText txtContrasena;

    /**
     * Método que se ejecuta cuando se crea la actividad.
     *
     * @param state Estado de la actividad
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_viewpdf);

        imageView = findViewById(R.id.imageView);
        Button btnContinuar = findViewById(R.id.btnContinuar);
        Button btnCancelar = findViewById(R.id.btnCancelar);
        txtUsuario = findViewById(R.id.txtUsuario);
        txtContrasena = findViewById(R.id.txtContrasena);
        txtContrasena.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(10)
        });

        String filepath = getIntent().getStringExtra("stringextra");

        try {
            assert filepath != null;
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

        btnCancelar.setOnClickListener(view -> finish());

        credentialManager = new CredentialManager(this);
        if(!credentialManager.isFirstRun()) {
            String[] credentials = credentialManager.loadCredentials();
            txtUsuario.setText(credentials[0]);
            txtContrasena.setText(credentials[1]);
        }

        btnContinuar.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View view) {
                String filepath = getIntent().getStringExtra("stringextra");
                assert filepath != null;
                File file = new File(filepath);

                String username = txtUsuario.getText().toString();
                String password = txtContrasena.getText().toString();
                String remoteBasePath = "/SETHLANS/ENTRADAS";

                credentialManager.saveCredentials(username, password);

                Intent intent = new Intent(ViewPdfActivity.this, PhotosActivity.class);

                try {
                    MegaSession session = Mega.login(new MegaAuthCredentials(username, password));
                    String remotePath = createRemoteFolders(remoteBasePath, session);
                    session.uploadFile(file.getAbsolutePath(), remotePath)
                            .createRemotePathIfNotPresent().run();

                    intent.putExtra("remotePath", remotePath);
                    intent.putExtra("username", username);
                    intent.putExtra("password", password);

                    runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                            "PDF guardado.", Toast.LENGTH_LONG).show());

                } catch(MegaLoginException e) {
                    System.err.println("Error de login: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                            "Error de login: " + e.getMessage(), Toast.LENGTH_LONG).show());

                } catch (MegaException e) {
                    System.err.println("Error MEGA: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                            "Error MEGA: " + e.getMessage(), Toast.LENGTH_LONG).show());

                }
                startActivity(intent);
            }
        });
    }

    /**
     * Método que se ejecuta para mostrar una página del PDF.
     *
     * @param index Índice de la página a mostrar
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

    private String createRemoteFolders(String remoteBasePath, MegaSession session) {
        String remotePath = remoteBasePath;
        try {
            int folderNumber = 1;
            while(true) {
                @SuppressLint("DefaultLocale")
                String folderPath = remotePath + "/" + String.format("%04d", folderNumber);
                String lsOutput = session.ls(folderPath).toString();
                if(lsOutput.contains("No such file or directory")) {
                    session.makeDirectory(folderPath);
                    remotePath = folderPath;
                    break;
                }
                folderNumber++;
            }
            return remotePath;

        } catch(MegaException e) {
            Toast.makeText(ViewPdfActivity.this,
                    "Error al crear las carpetas. " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return remoteBasePath;
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