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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

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
                String remoteBasePath = "/var/www/vhosts" +
                        "/romantic-engelbart.212-227-226-16.plesk.page/Recepciones";

                credentialManager.saveCredentials(username, password);

                Intent intent = new Intent(ViewPdfActivity.this, PhotosActivity.class);

                try {
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(username,
                            "romantic-engelbart.212-227-226-16.plesk.page", 22);
                    session.setPassword(password);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect();
                    String remotePath = createRemoteFolders(remoteBasePath, session);
                    uploadFile(file.getAbsolutePath(), remotePath, session);

                    intent.putExtra("remotePath", remotePath);
                    intent.putExtra("username", username);
                    intent.putExtra("password", password);

                    runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                            "PDF guardado.", Toast.LENGTH_LONG).show());

                } catch(Exception e) {
                    runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                            "Error de SSH: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());

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

    /**
     * Método que se ejecuta para crear las carpetas en el servidor.
     *
     * @param remoteBasePath Ruta base de la carpeta
     * @param session Sesión SSH
     * @return Ruta de la carpeta creada
     * @throws Exception Excepción en caso de error
     */
    private String createRemoteFolders(String remoteBasePath, Session session) throws Exception {
        String folderName = String.valueOf(findNextAvailableFolderNumber(remoteBasePath, session));
        Channel channel = session.openChannel("exec");
        String remotePath = remoteBasePath + "/" + folderName;
        ((ChannelExec) channel).setCommand("mkdir -p \"" + remotePath  + "\"");
        channel.connect();
        channel.disconnect();
        return remotePath;
    }

    /**
     * Método que se ejecuta para saber que carpeta crear en el servidor.
     *
     * @param remoteBasePath Ruta base de la carpeta
     * @param session Sesión SSH
     * @return Número de carpeta a crear
     * @throws Exception Excepción en caso de error
     */
    private int findNextAvailableFolderNumber(String remoteBasePath, Session session)
            throws Exception {
        int counter = 1;
        while(checkIfFolderExists(remoteBasePath + "/" + counter, session)) {
            counter++;
        }
        return counter;
    }

    /**
     * Método que se ejecuta para verificar si una carpeta existe en el servidor.
     *
     * @param fullFolderPath Ruta completa de la carpeta
     * @param session Sesión SSH
     * @return True si la carpeta existe, false en caso contrario
     * @throws Exception Excepción en caso de error
     */
    private boolean checkIfFolderExists(String fullFolderPath, Session session) throws Exception {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("[ -d \"" + fullFolderPath + "\" ] && echo \"exists\"");
        channel.setInputStream(null);

        InputStream in = channel.getInputStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().equals("exists")) {
                channel.disconnect();
                return true;
            }
        }

        channel.disconnect();
        return false;
    }

    private void uploadFile(String localFilePath, String remoteFolderPath, Session session)
            throws Exception {
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(localFilePath, remoteFolderPath + "/etiqueta.pdf");
        sftpChannel.disconnect();
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