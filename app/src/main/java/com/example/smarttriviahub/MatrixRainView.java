package com.example.smarttriviahub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

public class MatrixRainView extends View {

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZﾊﾐﾋｰｳｼﾅﾓﾆｻﾜﾂｵﾘｱﾎﾃﾏｹﾒｴｶｷﾑﾕﾗｾﾈｽﾀﾇﾍｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ";
    private final Random random = new Random();
    private Paint paint;
    private Paint headPaint;
    private float[] drops;
    private float[] speeds;
    private final int fontSize = 20; // Smaller font for high density columns
    private int columns;

    public MatrixRainView(Context context) {
        super(context);
        init();
    }

    public MatrixRainView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MatrixRainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#00FF41")); // Matrix green
        paint.setTextSize(fontSize);
        paint.setAntiAlias(true);

        headPaint = new Paint();
        headPaint.setColor(Color.parseColor("#E0FFF0")); // Bright white-green head
        headPaint.setTextSize(fontSize);
        headPaint.setAntiAlias(true);
        headPaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#00FF41"));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        columns = w / fontSize;
        if (columns <= 0) columns = 1;
        int rows = h / fontSize;
        drops = new float[columns];
        speeds = new float[columns];
        for (int i = 0; i < columns; i++) {
            drops[i] = random.nextInt(rows + 20) - 20;
            speeds[i] = 0.6f + random.nextFloat() * 1.8f; // Variable drop speeds
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Dark background with fading trail effect
        canvas.drawColor(Color.parseColor("#10050A05"));

        for (int i = 0; i < columns; i++) {
            float x = i * fontSize;
            float currentDrop = drops[i];

            // Draw a dense trail of characters for each column
            int trailLength = 18;
            for (int j = 0; j < trailLength; j++) {
                float y = (currentDrop - j) * fontSize;
                if (y >= -fontSize && y <= getHeight() + fontSize) {
                    char c = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));

                    if (j == 0) {
                        // Head of the stream is bright white-green with glow
                        canvas.drawText(String.valueOf(c), x, y, headPaint);
                    } else {
                        // Tail characters fade out gradually
                        int alpha = Math.max(15, 255 - (j * 14));
                        paint.setAlpha(alpha);
                        canvas.drawText(String.valueOf(c), x, y, paint);
                    }
                }
            }

            drops[i] += speeds[i];

            // Reset drop to top when it falls past screen height
            if ((currentDrop - trailLength) * fontSize > getHeight() && random.nextDouble() > 0.94) {
                drops[i] = 0;
                speeds[i] = 0.6f + random.nextFloat() * 1.8f;
            }
        }

        // Smooth animation frame rate (~30 fps)
        postInvalidateDelayed(30);
    }
}
