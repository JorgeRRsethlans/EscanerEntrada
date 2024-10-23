package com.escanerentrada.pdf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.escanerentrada.R;
import com.escanerentrada.camera.photos.PhotosActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


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
                new InputFilter.LengthFilter(25)
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

        credentialManager = new CredentialManager(ViewPdfActivity.this);
        if(!credentialManager.isFirstRun()) {
            String[] credentials = credentialManager.loadCredentials();
            txtUsuario.setText(credentials[0]);
            txtContrasena.setText(credentials[1]);
        }

        btnContinuar.setOnClickListener(view -> {
            String filepath1 = getIntent().getStringExtra("stringextra");
            assert filepath1 != null;
            File file = new File(filepath1);

            String username = txtUsuario.getText().toString();
            String password = txtContrasena.getText().toString();
            String remoteBasePath = "/Root/Incoming Shares/SETHLANS/ENTRADAS";

            credentialManager.saveCredentials(username, password);

            Intent intent = new Intent(ViewPdfActivity.this, PhotosActivity.class);

            try {
                executeMEGAcmd("login", username, password);
                String remotePath = createRemoteFolders(remoteBasePath);
                executeMEGAcmd("put", "--path", remotePath, file.getAbsolutePath());

                intent.putExtra("remotePath", remotePath);

                runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                        "PDF guardado.", Toast.LENGTH_LONG).show());

                startActivity(intent);

            } catch(Exception e) {
                Toast.makeText(ViewPdfActivity.this,
                        "Error al subir el PDF. " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
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
     * Método que se ejecuta para crear las carpetas en el servidor MEGA.
     *
     * @param remoteBasePath Ruta base de las carpetas a crear
     * @return Ruta de las carpetas creadas
     */
    private String createRemoteFolders(String remoteBasePath) {
        String remotePath = remoteBasePath;
        try {
            int folderNumber = 1;
            while (true) {
                @SuppressLint("DefaultLocale")
                String folderPath = remotePath + "/" + String.format("%04d", folderNumber);

                executeMEGAcmd("ls", folderPath);

                Process process = Runtime.getRuntime().exec(new String[]{getFilesDir()
                        + "/MEGAcmd-Linux-aarch64", "ls", folderPath});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process
                        .getInputStream()));
                String line;
                boolean folderExists = false;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("No such file or directory")) {
                        folderExists = true;
                        break;
                    }
                }

                if (!folderExists) {
                    executeMEGAcmd("mkdir", folderPath);
                    remotePath = folderPath;
                    break;
                }

                folderNumber++;
            }
            return remotePath;

        } catch (Exception e) {
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