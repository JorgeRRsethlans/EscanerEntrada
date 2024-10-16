package com.escanerentrada.camera.barcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Clase que se encarga de dibujar el cuadro de la cámara.
 */
public class CameraOverlayView extends View {

    private Paint borderPaint;
    private Paint backgroundPaint;
    private RectF frameRect;

    /**
     * Constructor de la clase.
     *
     * @param context
     */
    public CameraOverlayView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor de la clase.
     *
     * @param context
     * @param attrs
     */
    public CameraOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor de la clase.
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public CameraOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Método que inicializa los elementos.
     */
    private void init() {
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(0f);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAlpha(175);
    }

    /**
     * Método que se ejecuta cuando cambia el tamaño de la vista.
     *
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float centerX = w / 2f;
        float centerY = h / 2f;
        float frameWidth = w * 0.8f;
        float frameHeight = h * 0.3f;
        frameRect = new RectF(centerX - frameWidth / 2f, centerY - frameHeight / 2f,
                centerX + frameWidth / 2f, centerY + frameHeight / 2f);
    }

    /**
     * Método que se ejecuta cuando se dibuja la vista.
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, getWidth(), frameRect.top, backgroundPaint);
        canvas.drawRect(0, frameRect.bottom, getWidth(), getHeight(), backgroundPaint);
        canvas.drawRect(0, frameRect.top, frameRect.left, frameRect.bottom, backgroundPaint);
        canvas.drawRect(frameRect.right, frameRect.top, getWidth(),
                frameRect.bottom, backgroundPaint);

        int sc = canvas.saveLayer(0, 0, getWidth(), getHeight(), null,
                Canvas.ALL_SAVE_FLAG);
        canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
        borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRect(frameRect, borderPaint);
        borderPaint.setXfermode(null);
        canvas.restoreToCount(sc);
    }
}
