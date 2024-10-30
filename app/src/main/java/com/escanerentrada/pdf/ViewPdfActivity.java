package com.escanerentrada.pdf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.escanerentrada.R;

import java.io.File;

import com.escanerentrada.camera.photos.PhotosActivity;
import com.escanerentrada.pdf.printer.ZebraPrinterHelper;
import com.escanerentrada.ssh.SSHHelper;
import com.escanerentrada.ssh.SSHTask;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

/**
 * Clase que se encarga de mostrar el PDF.
 */
public class ViewPdfActivity extends AppCompatActivity {

    private PdfViewer pdfViewer;

    private CredentialManager credentialManager;

    private EditText txtUsuario;
    private EditText txtContrasena;

    private String username, password;

    /**
     * Método que se ejecuta cuando se crea la actividad.
     *
     * @param state Estado de la actividad
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_viewpdf);

        ImageView imageView = findViewById(R.id.imageView);
        Button btnContinuar = findViewById(R.id.btnContinuar);
        Button btnCancelar = findViewById(R.id.btnCancelar);
        txtUsuario = findViewById(R.id.txtUsuario);
        txtContrasena = findViewById(R.id.txtContrasena);
        txtContrasena.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(10)
        });

        String filepath = getIntent().getStringExtra("stringextra");
        pdfViewer = new PdfViewer(this, filepath);
        if(filepath != null) {
            pdfViewer.loadPdf(filepath, imageView);
        }

        btnCancelar.setOnClickListener(view -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50);
            }
            finish();
        });

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
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(50);
                }
                String filepath = getIntent().getStringExtra("stringextra");
                assert filepath != null;
                File file = new File(filepath);

                username = txtUsuario.getText().toString();
                password = txtContrasena.getText().toString();
                String remoteBasePath = "/var/www/vhosts" +
                        "/romantic-engelbart.212-227-226-16.plesk.page/Recepciones";

                credentialManager.saveCredentials(username, password);

                ZebraPrinterHelper printerHelper =
                        new ZebraPrinterHelper("192.168.1.91", 9100);
                new Thread(() -> {
                    try {
                        printerHelper.print(file);
                    } catch (ConnectionException | ZebraPrinterLanguageUnknownException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                Runnable sshOperations = () -> {
                    try {
                        SSHHelper sshHelper = new SSHHelper(username, password,
                                "romantic-engelbart.212-227-226-16.plesk.page", 22);
                        String remotePath = sshHelper.createRemoteFolders(remoteBasePath);
                        sshHelper.uploadFile(file.getAbsolutePath(), remotePath);

                        runOnUiThread(() -> {
                            Toast.makeText(ViewPdfActivity.this, "PDF guardado.",
                                    Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(ViewPdfActivity.this,
                                    PhotosActivity.class);
                            intent.putExtra("remotePath", remotePath);
                            intent.putExtra("username", username);
                            intent.putExtra("password", password);
                            startActivity(intent);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(ViewPdfActivity.this,
                                "Error de SSH: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                };

                new SSHTask(ViewPdfActivity.this, sshOperations)
                        .execute();
            }
        });
    }

    /**
     * Método que se ejecuta cuando se destruye la actividad.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        pdfViewer.closeDocument();
    }
}