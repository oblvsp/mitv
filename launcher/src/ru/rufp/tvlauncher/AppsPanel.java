package ru.rufp.tvlauncher;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

class AppsPanel extends ViewGroup {

    interface Listener {
        boolean onAppOpen(Apps.AppInfo app);
    }

    private List<Apps.AppInfo> apps;
    private final Listener listener;
    private final int tileW;
    private final int tileH;
    private int cols;
    private int rows;
    private int cursor;
    private boolean shown;
    private int anchorY;
    private int anchorGap;
    private boolean anchored;

    AppsPanel(Context c, List<Apps.AppInfo> apps, int tileW, Listener l) {
        super(c);
        this.listener = l;
        this.tileW = tileW;
        this.tileH = (int) (tileW / 1.67f);
        setClipChildren(false);
        repopulate(apps);
    }

    void anchorAbove(int y, int gap) {
        anchorY = y;
        anchorGap = gap;
        anchored = true;
    }

    void repopulate(List<Apps.AppInfo> apps) {
        this.apps = apps;
        removeAllViews();
        cols = Math.max(1, Math.min(apps.size(), 5));
        rows = (apps.size() + cols - 1) / cols;
        for (int i = 0; i < apps.size(); i++) {
            TileView t = new TileView(getContext());
            t.bind(apps.get(i));
            addView(t, new LayoutParams(tileW, tileH));
        }
        cursor = 0;
        refreshFocus();
        requestLayout();
        invalidate();
    }

    void open() {
        shown = true;
        cursor = 0;
        refreshFocus();
        setVisibility(VISIBLE);
        requestFocus();
        invalidate();
    }

    void close() {
        shown = false;
        setVisibility(GONE);
    }

    boolean isOpen() {
        return shown;
    }

    private void refreshFocus() {
        for (int i = 0; i < apps.size(); i++) {
            ((TileView) getChildAt(i)).setFocusedState(i == cursor);
        }
        invalidate();
    }

    boolean handleKey(int keyCode) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                if (cursor > 0) {
                    cursor--;
                    refreshFocus();
                }
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                if (cursor < apps.size() - 1) {
                    cursor++;
                    refreshFocus();
                }
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN: {
                int n = cursor + cols;
                if (n < apps.size()) {
                    cursor = n;
                    refreshFocus();
                }
                return true;
            }
            case android.view.KeyEvent.KEYCODE_DPAD_UP: {
                int n = cursor - cols;
                if (n >= 0) {
                    cursor = n;
                    refreshFocus();
                }
                return true;
            }
            case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
            case android.view.KeyEvent.KEYCODE_ENTER:
                if (listener != null && cursor < apps.size()) {
                    listener.onAppOpen(apps.get(cursor));
                }
                return true;
        }
        return false;
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
        float gap = tileW / 4f;
        float gridW = cols * tileW + (cols - 1) * gap;
        float gridH = rows * tileH + (rows - 1) * gap;
        float left = (w - gridW) / 2f;
        float top = anchored ? anchorY - anchorGap - gridH : (h - gridH) / 2f;
        for (int i = 0; i < apps.size(); i++) {
            View v = getChildAt(i);
            int col = i % cols;
            int row = i / cols;
            float vx = left + col * (tileW + gap);
            float vy = top + row * (tileH + gap);
            v.layout((int) vx, (int) vy, (int) (vx + tileW), (int) (vy + tileH));
        }
    }
}
