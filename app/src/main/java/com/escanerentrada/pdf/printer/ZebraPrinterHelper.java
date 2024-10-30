package com.escanerentrada.pdf.printer;

import android.util.Log;

import com.escanerentrada.pdf.PdfToImage;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.graphics.ZebraImageFactory;
import com.zebra.sdk.graphics.ZebraImageI;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.File;
import java.io.IOException;

/**
 * Clase que se encarga de imprimir un PDF a una impresora Zebra.
 */
public class ZebraPrinterHelper {

    private final String printerIpAddress;
    private final int printerPort;

    /**
     * Constructor de la clase.
     *
     * @param printerIpAddress Dirección IP de la impresora Zebra
     * @param printerPort Puerto de la impresora Zebra
     */
    public ZebraPrinterHelper(String printerIpAddress, int printerPort) {
        this.printerIpAddress = printerIpAddress;
        this.printerPort = printerPort;
    }

    /**
     * Método que se encarga de imprimir un PDF a una impresora Zebra.
     *
     * @param pdfFile Archivo PDF a imprimir
     * @throws ConnectionException Si hay un problema al conectar con la impresora
     * @throws ZebraPrinterLanguageUnknownException Si la impresora no soporta el idioma del PDF
     */
    public void print(File pdfFile)
            throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Connection connection = new TcpConnection(printerIpAddress, printerPort);
        try {
            try {
                connection.open();
            } catch(ConnectionException e) {
                Log.e("ZebraPrinterHelper", "Error al conectar con la impresora", e);
            }
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            printer.printImage(getPdfBitmap(pdfFile.getAbsolutePath()),
                    0, 0, 0, 0, false);
            printer.reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }
    }

    /**
     * Método que se encarga de convertir un PDF a una imagen Zebra.
     *
     * @param filePath Ruta del archivo PDF
     * @return Imagen Zebra del PDF
     * @throws IOException Si hay un problema al convertir el PDF a una imagen
     */
    private ZebraImageI getPdfBitmap(String filePath) throws IOException {
        PdfToImage pdfToImage = new PdfToImage(filePath);
        return ZebraImageFactory.getImage(pdfToImage.toImage());
    }
}