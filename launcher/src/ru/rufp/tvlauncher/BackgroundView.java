package ru.rufp.tvlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;
import android.view.View;

import java.io.InputStream;

class BackgroundView extends View {

    private final Paint bitmapPaint =
            new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint topShade = new Paint();
    private final Paint bottomShade = new Paint();
    private final Paint dim = new Paint();
    private Bitmap bg;
    private int wantedDay = -1;

    BackgroundView(Context c) {
        super(c);
        dim.setColor(0x14000000);
        setWillNotDraw(false);
    }

    void load(int day) {
        wantedDay = day;
        new Thread(() -> {
            Bitmap b = null;
            try (InputStream in = getContext().getAssets().open("bg/" + day + ".jpg")) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                o.inSampleSize = 2;
                b = BitmapFactory.decodeStream(in, null, o);
            } catch (Exception e) {
                Log.w("rufp", "bg load failed", e);
            }
            Bitmap nb = b;
            post(() -> {
                if (nb == null || wantedDay != day) {
                    if (nb != null) nb.recycle();
                    return;
                }
                if (bg != null) bg.recycle();
                bg = nb;
                invalidate();
            });
        }, "wallpaper").start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        topShade.setShader(null);
        bottomShade.setShader(null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(0x1A, 0x1C, 0x22));
        if (bg != null) {
            float s = Math.max(getWidth() / (float) bg.getWidth(), getHeight() / (float) bg.getHeight());
            float w = bg.getWidth() * s;
            float h = bg.getHeight() * s;
            canvas.drawBitmap(bg, (getWidth() - w) / 2f, (getHeight() - h) / 2f, bitmapPaint);
        }
        float scrH = getHeight();
        if (scrH > 0 && topShade.getShader() == null) {
            topShade.setShader(new LinearGradient(0, 0, 0, scrH * 0.34f,
                    new int[]{0x9E000000, 0x00000000}, null, Shader.TileMode.CLAMP));
            bottomShade.setShader(new LinearGradient(0, scrH * 0.58f, 0, scrH,
                    new int[]{0x00000000, 0x73000000}, null, Shader.TileMode.CLAMP));
        }
        canvas.drawRect(0, 0, getWidth(), scrH, dim);
        canvas.drawRect(0, 0, getWidth(), scrH * 0.34f, topShade);
        canvas.drawRect(0, scrH * 0.58f, getWidth(), scrH, bottomShade);
    }
}
