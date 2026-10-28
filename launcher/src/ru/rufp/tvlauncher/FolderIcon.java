package ru.rufp.tvlauncher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import java.util.List;

class FolderIcon extends Drawable {

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final RectF cell = new RectF();
    private final List<Apps.AppInfo> apps;

    FolderIcon(List<Apps.AppInfo> apps) {
        this.apps = apps;
        bg.setColor(0x2EFFFFFF);
        slot.setColor(0x16FFFFFF);
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        if (b.isEmpty()) return;
        r.set(b);
        canvas.drawRoundRect(r, r.width() * 0.22f, r.width() * 0.22f, bg);
        float inset = r.width() * 0.12f;
        float cw = (r.width() - inset * 3) / 2f;
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            cell.set(r.left + inset + col * (cw + inset),
                    r.top + inset + row * (cw + inset),
                    r.left + inset + col * (cw + inset) + cw,
                    r.top + inset + row * (cw + inset) + cw);
            canvas.drawRoundRect(cell, cw * 0.24f, cw * 0.24f, slot);
            if (apps != null && i < apps.size() && apps.get(i).icon != null) {
                Drawable d = apps.get(i).icon;
                int iw = d.getIntrinsicWidth();
                int ih = d.getIntrinsicHeight();
                if (iw <= 0) iw = 1;
                if (ih <= 0) ih = 1;
                float pad = cw * 0.14f;
                float avail = cw - pad * 2;
                float s = Math.min(avail / iw, avail / ih);
                int dw = (int) (iw * s);
                int dh = (int) (ih * s);
                android.graphics.Rect keep = d.copyBounds();
                d.setBounds((int) (cell.centerX() - dw / 2f), (int) (cell.centerY() - dh / 2f),
                        (int) (cell.centerX() + dw / 2f), (int) (cell.centerY() + dh / 2f));
                d.draw(canvas);
                d.setBounds(keep);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 108;
    }

    @Override
    public int getIntrinsicHeight() {
        return 108;
    }
}
