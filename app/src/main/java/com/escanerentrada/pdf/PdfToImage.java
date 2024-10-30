package com.escanerentrada.pdf;

import android.graphics.Bitmap;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;

/**
 * Clase que se encarga de convertir un PDF a una imagen.
 */
public class PdfToImage {

    public String filePath;

    /**
     * Constructor de la clase.
     *
     * @param filePath Ruta del archivo PDF
     */
    public PdfToImage(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Método que se encarga de convertir el PDF a una imagen.
     *
     * @return Imagen del PDF
     */
    public Bitmap toImage() throws IOException {
        File file = new File(filePath);
        PDFRenderer pdfRenderer = new PDFRenderer(PDDocument.load(file));

        try {
            return pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
        } catch(IOException e) {
            return null;
        }
    }
}
