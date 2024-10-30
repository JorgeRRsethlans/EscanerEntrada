package com.escanerentrada.pdf;

import android.content.Context;
import android.widget.ImageView;
import android.widget.Toast;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;

/**
 * Clase que se encarga de mostrar el PDF.
 */
public class PdfViewer {

    private int currentPageIndex = 0;
    private PDDocument document;
    private final Context context;
    private final String filePath;

    /**
     * Constructor de la clase usado para visualizar Pdfs.
     *
     * @param context Contexto de la actividad
     */
    public PdfViewer(Context context, String filePath) {
        this.context = context;
        this.filePath = filePath;
    }

    /**
     * Constructor de la clase usado para extraer el Bitmap de un PDF.
     */
    public PdfViewer(String filePath) {
        this.context = null;
        this.filePath = filePath;
    }

    /**
     * Método que se encarga de cargar el PDF.
     *
     * @param filePath Ruta del archivo PDF
     * @param imageView Vista para mostrar el PDF
     */
    public void loadPdf(String filePath, ImageView imageView) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(context,
                        "Error, el PDF no se ha creado",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            document = PDDocument.load(file);
            new PDFRenderer(document);
            showPage(currentPageIndex, imageView);
        } catch (IOException e) {
            Toast.makeText(context,
                    "Error al cargar el PDF",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método que se encarga de mostrar la página del PDF.
     *
     * @param index Índice de la página a mostrar
     * @param imageView Vista para mostrar la página
     */
    private void showPage(int index, ImageView imageView) throws IOException {
        if (document != null && index < document.getNumberOfPages()) {
            PdfToImage pdfToImage = new PdfToImage(filePath);
            imageView.setImageBitmap(pdfToImage.toImage());
            currentPageIndex = index;
        }
    }

    /**
     * Método que se encarga de cerrar el PDF.
     */
    public void closeDocument() {
        try {
            if (document != null) {
                document.close();
            }
        } catch (IOException e) {
            Toast.makeText(context,
                    "Error al cerrar el PDF",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
