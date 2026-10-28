package ru.rufp.tvlauncher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

class TileView extends View {

    static final float SCALE_FOCUSED = 1.13f;

    private static final int CHIP = 0xFF2E333C;
    private static final int CARD = 0xFFF6F8FA;
    private static final DecelerateInterpolator FOCUS_CURVE = new DecelerateInterpolator(1.5f);

    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Apps.AppInfo app;
    private boolean focused;
    private float focusShift;
    private ValueAnimator focusAnimator;

    TileView(Context c) {
        super(c);
        setFocusable(true);
        card.setColor(CHIP);
        setClipToOutline(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        setPivotX(w / 2f);
        setPivotY(h);
        placeIcon();
    }

    void bind(Apps.AppInfo app) {
        this.app = app;
        setContentDescription(app.label);
        placeIcon();
    }

    private void placeIcon() {
        if (app == null || app.icon == null) return;
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;
        float size = Math.min(w, h) * 0.55f;
        int iw = app.icon.getIntrinsicWidth();
        int ih = app.icon.getIntrinsicHeight();
        if (iw <= 0) iw = 1;
        if (ih <= 0) ih = 1;
        float s = Math.min(size / iw, size / ih);
        int dw = (int) (iw * s);
        int dh = (int) (ih * s);
        app.icon.setBounds((int) ((w - dw) / 2), (int) ((h - dh) / 2),
                (int) ((w + dw) / 2), (int) ((h + dh) / 2));
    }

    Apps.AppInfo app() {
        return app;
    }

    void setFocusedState(boolean f) {
        if (focused == f) return;
        focused = f;
        card.setColor(f ? CARD : CHIP);
        invalidate();
        animateFocus(f ? SCALE_FOCUSED : 1f);
    }

    void animateFocus(float target) {
        if (focusAnimator != null) focusAnimator.cancel();
        float targetShift = (target - 1f) / (SCALE_FOCUSED - 1f);
        focusAnimator = ValueAnimator.ofFloat(focusShift, targetShift);
        focusAnimator.addUpdateListener(an -> {
            float value = (float) an.getAnimatedValue();
            focusShift = Math.max(0f, Math.min(1f, value));
            float s = 1f + (SCALE_FOCUSED - 1f) * focusShift;
            // The tile is a hardware layer: keep its contents cached while only transforming it.
            setScaleX(s);
            setScaleY(s);
        });
        focusAnimator.setDuration(180);
        focusAnimator.setInterpolator(FOCUS_CURVE);
        focusAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, rect.width() * 0.11f, rect.width() * 0.11f, card);
        if (app != null && app.icon != null) {
            app.icon.draw(canvas);
        }
    }

}
