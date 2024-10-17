package com.escanerentrada.pdf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.print.PrintAttributes;
import android.print.PrintAttributes.MediaSize;
import android.print.pdf.PrintedPdfDocument;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase que se encarga de crear el PDF.
 */
public class PdfCreator {

    /**
     * Método que crea el PDF.
     *
     * @param context Contexto de la aplicación
     * @param filePath Ruta del archivo
     * @param content Contenido del PDF
     */
    public static void createPdf(Context context, String filePath, String[] content) {
        @SuppressLint("SimpleDateFormat")
        String dateTime = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(new Date());

        int dpi = 2700;
        int pdfWidthPixels = (int) (76.2f / 25.4f) * dpi;
        int pdfHeightPixels = (int) (50.8f / 25.4f) * dpi;

        PrintAttributes printAttrs = new PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(new MediaSize("1", "etiquetaGrande",
                        pdfWidthPixels, pdfHeightPixels))
                .setResolution(new PrintAttributes
                        .Resolution("pdf", "pdf", dpi, dpi))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();

        PrintedPdfDocument document = new PrintedPdfDocument(context, printAttrs);
        PdfDocument.Page page = document.startPage(1);
        Canvas canvas = page.getCanvas();

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        float borderOffSet = 25f;
        canvas.drawRect(borderOffSet, borderOffSet, canvas.getWidth() - borderOffSet,
                canvas.getHeight() - borderOffSet, borderPaint);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        float textSize = Math.min(canvas.getWidth(), canvas.getHeight()) / 15f;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.LEFT);

        float marginLeft = borderOffSet + 10f;
        float marginTop = borderOffSet + 10f;
        float y = -paint.ascent() + marginTop;

        canvas.drawText(content[0], marginLeft, y, paint);
        for(int i = 1; i < content.length; i++) {
            canvas.drawText(content[i], marginLeft, y + (-paint.ascent() + paint.descent()) * i,
                    paint);
        }
        canvas.drawText(dateTime, marginLeft,
                y + (-paint.ascent() + paint.descent()) * content.length
                        + borderOffSet + paint.descent()
                , paint);

        document.finishPage(page);

        try {
            File file = new File(filePath);
            FileOutputStream outputStream = new FileOutputStream(file);
            document.writeTo(outputStream);
            document.close();
            outputStream.close();
        } catch(IOException e) {
            Toast.makeText(context, "Error al crear la etiqueta", Toast.LENGTH_SHORT).show();
        }
    }
}
