package com.escanerentrada.pdf;

import android.content.Context;
import android.widget.Toast;

import com.tom_roush.harmony.awt.AWTColor;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Clase que se encarga de crear un PDF.
 */
public class PdfCreator {

    /**
     * Método que se encarga de crear un PDF.
     *
     * @param context Contexto de la aplicación
     * @param filePath Ruta del archivo PDF
     * @param content Contenido del PDF
     */
    public static void createPdf(Context context, String filePath, String[] content) {
        try {
            PDFBoxResourceLoader.init(context);
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(new PDRectangle(75f * 2.54f, 49.6f * 2.54f));
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.setNonStrokingColor(AWTColor.WHITE);
            contentStream.setStrokingColor(AWTColor.BLACK);
            contentStream.setLineWidth(2f);
            contentStream.addRect(2 * 2.54f, 2 * 2.54f,
                    71f * 2.54f, 45.6f * 2.54f);
            contentStream.fillAndStroke();
            contentStream.closePath();

            float marginLeft = 7.5f;
            float marginTop = page.getMediaBox().getHeight() - 15f;

            for (String line : content) {
                contentStream.beginText();
                contentStream.setNonStrokingColor(AWTColor.BLACK);
                contentStream.setFont(PDType1Font.HELVETICA, 10f);
                contentStream.newLineAtOffset(marginLeft, marginTop);
                contentStream.showText(line);
                contentStream.endText();
                contentStream.closePath();
                marginTop -= 15f;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss",
                    Locale.getDefault());
            String dateTime = dateFormat.format(new Date());
            contentStream.beginText();
            contentStream.setNonStrokingColor(AWTColor.BLACK);
            contentStream.setFont(PDType1Font.HELVETICA, 10f);
            contentStream.newLineAtOffset(marginLeft, marginTop);
            contentStream.showText(dateTime);
            contentStream.endText();
            contentStream.closePath();
            contentStream.close();

            document.save(new File(filePath));
            document.close();
        } catch (IOException e) {
            Toast.makeText(context, "Error al crear la etiqueta", Toast.LENGTH_SHORT).show();
        }
    }
}