package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class HourglassView extends View {

    private Paint strokePaint;
    private Paint fillPaint;
    private Path hourglassPath;
    private float progress = 1f;
    private float avatarTop = 0f;
    private float avatarScale = 1f;
    private float connectionHeight = -1;

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

    public boolean isConnected() {
        return connectionHeight != -1;
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

    Interpolator decelerateInterpolator = new DecelerateInterpolator();
    Interpolator decelerateInterpolator2 = new DecelerateInterpolator(0.7f);

    public void setProgress(float value, float scale, float avatarContainerTop) {
        progress = decelerateInterpolator.getInterpolation(Math.max(0f, Math.min(1f, value)));
        avatarScale = scale;
        avatarTop = avatarContainerTop;
        invalidate();
    }

    public void setExpandProgress(float value) {
        expandProgress = value;
    }

    public float getAdditionalScale() {
        return additionalScale;
    }

    float additionalScale = 0f;
    float expandProgress = 0f;
    float maxHeight = 0f;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2;
        if (1 - progress < 0.09) {
            connectionHeight = -1;
            maxHeight= -1;
            return;
        }
        float realProgress = Math.min(1f, decelerateInterpolator.getInterpolation(((1f - progress) - 0.09f) / 0.91f));
        float wideProgress = Math.min(1f, Math.max(0f, (realProgress - 0.2f) / 1.3f));

        float height = Math.min(avatarTop + getHeight() * avatarScale, getHeight() * avatarScale * decelerateInterpolator2.getInterpolation((realProgress)));

        height = Math.min(avatarTop + getHeight() * avatarScale / 2, height) - dp(1);
        maxHeight = Math.max(maxHeight,height);
        float topOffsetY = 0;
        float topOffsetX = Math.min(dp(26),Math.max(0,((1 - height / maxHeight) * 10) * dp(30)));
        if (height <= -(dp(1))) {
            return;
        }

        if (avatarTop <= height) {
            if (connectionHeight == -1) {
                connectionHeight = avatarTop;
            }

            float startWidth = dp(15) + Math.max(0, expandProgress * getWidth() - dp(10)) + wideProgress * dp(4);

            float centerWidth = (dp(15) + expandProgress * getWidth()) / 3 + (dp(8) + getWidth() * avatarScale / 2  -  ((dp(15) + expandProgress * getWidth()) / 3)) * wideProgress;
            float topWidth = dp(8) + Math.max(0, expandProgress * getWidth() - dp(22)) + (topOffsetX + centerWidth - dp(38) + Math.max(0, expandProgress * getWidth() - dp(15)) - dp(10)) * wideProgress;

            hourglassPath.reset(); // avatarTop < height
            hourglassPath.moveTo(centerX - startWidth, 0);

            float centerY = height / 3 - (height / 4);
            hourglassPath.cubicTo(
                    centerX - startWidth, - topOffsetY,
                    centerX - centerWidth, centerY,
                    centerX - topWidth, height - height / 15);
            hourglassPath.cubicTo(
                    centerX - topWidth, height - height / 15,
                    centerX, height,
                    centerX + topWidth, height - height / 15);

            hourglassPath.cubicTo(
                    centerX + topWidth, height - height / 15,
                    centerX + centerWidth, centerY,
                    centerX + startWidth,  -topOffsetY);

        } else {
            maxHeight = 0f;
            additionalScale = 0f;
            connectionHeight = -1;
            float startWidth = dp(15);
            float height1 = height / 1.2f;
            float centerWidth = startWidth - dp(2);
            float centerAdditionalHeight = height1 / 2 - height1 / 15;
            float topWidth = 0f;

            hourglassPath.reset(); // avatarTop < height
            hourglassPath.moveTo(centerX - startWidth, 0);

            hourglassPath.cubicTo(
                    centerX - startWidth, 0,
                    centerX - centerWidth, (height1 / 2) + centerAdditionalHeight,
                    centerX - topWidth, height1);
            hourglassPath.cubicTo(
                    centerX, height1,
                    centerX, height1,
                    centerX, height1);

            hourglassPath.cubicTo(
                    centerX + topWidth, height1,
                    centerX + centerWidth, (height1 / 2) + centerAdditionalHeight,
                    centerX + startWidth, 0);

        }
        hourglassPath.close();
        canvas.drawPath(hourglassPath, fillPaint);
        canvas.drawPath(hourglassPath, strokePaint);
    }
}