package org.telegram.ui.Stars;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class HourglassView extends View {

    private Paint strokePaint;
    private Paint fillPaint;
    private Path hourglassPath;
    private float thinness = 0.5f; // 0 = узкие, 1 = широкие
    private float bottomScale = 1f; // 0 = узкие, 1 = широкие

    public HourglassView(Context context) {
        super(context);
        init();
    }

    public HourglassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HourglassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4f);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.BLACK);
        fillPaint.setStyle(Paint.Style.FILL);

        hourglassPath = new Path();
    }

    public float getThinness() {
        return thinness;
    }

    public void setThinness(float value,float scale) {
        thinness = Math.max(0f, Math.min(1f, value));
        bottomScale = scale;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = thinness > 0.4 ? getHeight() : lerp(0, getHeight(), thinness * 2.5f);
        float h2 =getHeight();
        float centerX = width / 2;
        float centerY = height / 2;
        float fixedWidth = width * 0.45f; // Фиксированная ширина верхней/нижней границ


            hourglassPath.reset();
            // Верхняя левая точка (фиксированная ширина)
            hourglassPath.moveTo(centerX - fixedWidth * (1.5f - bottomScale), 0);

            // Левая кривая (сужается/расширяется в зависимости от thinness)
            hourglassPath.cubicTo(
                    centerX - fixedWidth * (1.5f - bottomScale), -2,
                    centerX, Math.min(getHeight() * 2,(1 -thinness) * centerY * 8),
                    (centerX + fixedWidth * (1.5f - bottomScale)), -2
            );
            hourglassPath.close();

            canvas.drawPath(hourglassPath, fillPaint);
            canvas.drawPath(hourglassPath, strokePaint);

    }
}