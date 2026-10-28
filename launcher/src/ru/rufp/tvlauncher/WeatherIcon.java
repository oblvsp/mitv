package ru.rufp.tvlauncher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

final class WeatherIcon extends Drawable {

    private final Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloud = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cloudShape = new Path();

    WeatherIcon() {
        sun.setColor(0xFFFFD166);
        cloud.setColor(0xFFF1F6FC);
        outline.setColor(0xFF111318);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(3f);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float w = b.width();
        float h = b.height();
        canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(w / 100f, h / 100f);
        canvas.drawCircle(68, 34, 17, sun);
        cloudShape.reset();
        cloudShape.moveTo(15, 69);
        cloudShape.cubicTo(15, 59, 23, 52, 33, 52);
        cloudShape.cubicTo(35, 40, 45, 33, 56, 35);
        cloudShape.cubicTo(66, 35, 74, 42, 77, 51);
        cloudShape.cubicTo(90, 50, 96, 59, 95, 68);
        cloudShape.cubicTo(95, 77, 88, 83, 78, 83);
        cloudShape.lineTo(33, 83);
        cloudShape.cubicTo(22, 83, 15, 78, 15, 69);
        cloudShape.close();
        canvas.drawPath(cloudShape, cloud);
        canvas.drawPath(cloudShape, outline);
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) { cloud.setAlpha(alpha); sun.setAlpha(alpha); outline.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { cloud.setColorFilter(filter); sun.setColorFilter(filter); outline.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 100; }
    @Override public int getIntrinsicHeight() { return 100; }
}
