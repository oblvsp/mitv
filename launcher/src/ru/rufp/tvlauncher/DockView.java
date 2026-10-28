package ru.rufp.tvlauncher;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

class DockView extends ViewGroup {

    private final int tileW;
    private final int tileH;
    private int cursor = -1;
    private int count;

    DockView(Context c, int tileW, int tileH) {
        super(c);
        this.tileW = tileW;
        this.tileH = tileH;
        setClipChildren(false);
        setClipToPadding(false);
    }

    void populate(java.util.List<Apps.AppInfo> apps) {
        count = apps.size();
        if (getChildCount() == count) {
            for (int i = 0; i < count; i++) {
                ((TileView) getChildAt(i)).bind(apps.get(i));
            }
        } else {
            removeAllViews();
            for (int i = 0; i < count; i++) {
                TileView t = new TileView(getContext());
                t.bind(apps.get(i));
                addView(t, new LayoutParams(tileW, tileH));
            }
        }
        requestLayout();
        invalidate();
    }

    int count() {
        return count;
    }

    void setCursor(int i, boolean snap) {
        if (i < 0 || i >= count) return;
        TileView prev = i != cursor ? tileAt(cursor) : null;
        if (prev != null) prev.setFocusedState(false);
        cursor = i;
        tileAt(i).setFocusedState(true);
    }

    void handleKey(int keyCode) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                if (cursor > 0) setCursor(cursor - 1, false);
                break;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                if (cursor < count - 1) setCursor(cursor + 1, false);
                break;
        }
    }

    TileView tileAt(int i) {
        return (i >= 0 && i < count) ? (TileView) getChildAt(i) : null;
    }

    TileView focusedTile() {
        return tileAt(cursor);
    }

    int cursorIndex() {
        return cursor;
    }

    void dimFocus() {
        TileView t = tileAt(cursor);
        if (t != null) t.setFocusedState(false);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = MeasureSpec.getSize(hSpec);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(tileW, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(tileH, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        int y = h - tileH - tileW / 4;
        int total = count * tileW + Math.max(0, count - 1) * (tileW / 4);
        int x = (w - total) / 2;
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            v.layout(x, y, x + tileW, y + tileH);
            x += tileW + tileW / 4;
        }
    }
}
