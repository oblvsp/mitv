package ru.rufp.tvlauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

public class MainActivity extends Activity implements AppsPanel.Listener {

    private FrameLayout root;
    private BackgroundView background;
    private TextView clock;
    private TextView date;
    private TextView dow;
    private TextView temp;
    private TextView pressure;
    private TextView wind;
    private DockView dock;
    private AppsPanel folder;
    private boolean folderOpen;
    private int lastDock;
    private int W;
    private int H;
    private int tileW;
    private int loadedDow = -1;
    private boolean weatherOk;
    private String weatherEndpoint;
    private long lastWeatherTry;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private Future<?> weatherTask;
    private SimpleDateFormat fmtTime;
    private SimpleDateFormat fmtDate;
    private SimpleDateFormat fmtDow;
    private BroadcastReceiver appsReceiver;
    private Runnable appsRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        android.graphics.Point p = new android.graphics.Point();
        wm.getDefaultDisplay().getRealSize(p);
        W = p.x;
        H = p.y;
        tileW = (int) (W * 0.128f);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(0x1A, 0x1C, 0x22));
        setContentView(root);

        background = new BackgroundView(this);
        root.addView(background, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        buildTopBar();

        List<Apps.AppInfo> folderApps = Apps.loadFolder(this);
        List<Apps.AppInfo> items = dockItems(folderApps);

        int dockTile = (int) (tileW * 1.02f);
        int maxRowW = (int) (W * 0.96f);
        if (dockTile * items.size() + (items.size() - 1) * (dockTile / 4) > maxRowW) {
            dockTile = (int) ((4f * maxRowW) / (5f * items.size() - 1));
        }

        int dockTileH = Math.round(dockTile / 1.67f);
        int dockH = dockTileH * 2 + dockTile / 4;
        dock = new DockView(this, dockTile, dockTileH);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dockH);
        dlp.gravity = Gravity.BOTTOM;
        root.addView(dock, dlp);
        dock.populate(items);

        folder = new AppsPanel(this, folderApps, (int) (tileW * 0.98f), this);
        folder.anchorAbove(H - dockTileH - dockTile / 4, dockTile / 4);
        folder.setVisibility(View.GONE);
        root.addView(folder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        appsRefresh = this::refreshApps;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        appsReceiver = new AppsReceiver(handler, appsRefresh);
        registerReceiver(appsReceiver, filter);

        if (dock.count() > 0) dock.setCursor(0, true);

        updateTopBar();
        checkDay();
        weatherEndpoint = Weather.loadEndpoint(getApplicationContext());
        if (weatherEndpoint != null) fetchWeather();
        ticker = () -> {
            updateTopBar();
            checkDay();
            long refreshAt = weatherOk ? 1800000L : 30000L;
            if (weatherEndpoint != null
                    && SystemClock.elapsedRealtime() - lastWeatherTry >= refreshAt) fetchWeather();
            handler.postDelayed(ticker, 15000);
        };
        handler.postDelayed(ticker, 15000);
    }

    private void buildTopBar() {
        Locale ru = new Locale("ru");
        fmtTime = new SimpleDateFormat("HH:mm", ru);
        fmtDate = new SimpleDateFormat("d MMMM", ru);
        fmtDow = new SimpleDateFormat("EEEE", ru);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding((int) (W * 0.047f), (int) (H * 0.038f), (int) (W * 0.047f), 0);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);

        clock = mkText(0.052f, 0xFFFFFFFF, 300);
        date = mkText(0.020f, 0xFF8A8D93, 400);
        dow = mkText(0.020f, 0xFF8A8D93, 400);
        date.setAllCaps(true);
        dow.setAllCaps(true);

        left.addView(clock, wrap());
        left.addView(date, wrap());
        left.addView(dow, wrap());

        bar.addView(left, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL);

        temp = mkText(0.052f, 0xFFFFFFFF, 300);
        temp.setText("—");
        pressure = mkText(0.020f, 0xFF9AA0A6, 400);
        pressure.setText("—");
        wind = mkText(0.020f, 0xFF9AA0A6, 400);
        wind.setText("—");

        right.addView(temp, wrap());
        right.addView(pressure, wrap());
        right.addView(wind, wrap());

        bar.addView(right, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView mkText(float sizeF, int color, int weight) {
        TextView t = new TextView(this);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, H * sizeF);
        t.setTextColor(color);
        t.setTypeface(Fonts.get(weight));
        t.setIncludeFontPadding(false);
        return t;
    }

    private void updateTopBar() {
        long now = System.currentTimeMillis();
        clock.setText(fmtTime.format(now));
        date.setText(fmtDate.format(now));
        dow.setText(fmtDow.format(now));
    }

    private void fetchWeather() {
        if (weatherEndpoint == null) return;
        if (weatherTask != null && !weatherTask.isDone()) return;
        lastWeatherTry = SystemClock.elapsedRealtime();
        String requestedEndpoint = weatherEndpoint;
        weatherTask = Weather.fetch(requestedEndpoint, (t, press, windMs) -> {
            if (!requestedEndpoint.equals(weatherEndpoint)) return;
            weatherTask = null;
            if (isFinishing() || isDestroyed()) return;
            weatherOk = true;
            temp.setText(Math.round(t) + "°");
            pressure.setText(press + " мм рт. ст.");
            wind.setText(Math.round(windMs) + " м/с");
        });
    }

    private List<Apps.AppInfo> dockItems(List<Apps.AppInfo> folderApps) {
        List<Apps.AppInfo> items = Apps.loadDock(this, true);
        if (items.isEmpty()) items.add(Apps.folderItem(folderApps));
        else items.add(items.size() - 1, Apps.folderItem(folderApps));
        return items;
    }

    private void refreshApps() {
        List<Apps.AppInfo> folderApps = Apps.loadFolder(this);
        List<Apps.AppInfo> items = dockItems(folderApps);
        int prev = folderOpen ? lastDock : dock.cursorIndex();
        dock.populate(items);
        if (dock.count() > 0) {
            dock.setCursor(Math.min(Math.max(prev, 0), dock.count() - 1), true);
        }
        folder.repopulate(folderApps);
    }

    private void checkDay() {
        int dow = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        if (dow != loadedDow) {
            loadedDow = dow;
            background.load(dow);
        }
    }

    @Override
    public boolean onAppOpen(Apps.AppInfo app) {
        closeFolder();
        if (app != null && app.launch != null) startActivity(app.launch);
        return true;
    }

    private void openFolder() {
        folderOpen = true;
        lastDock = dock.cursorIndex();
        dock.dimFocus();
        folder.open();
    }

    private void closeFolder() {
        folderOpen = false;
        folder.close();
        if (dock.count() > 0) dock.setCursor(lastDock, true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        int kc = event.getKeyCode();

        if (folderOpen) {
            if (kc == KeyEvent.KEYCODE_BACK || kc == KeyEvent.KEYCODE_MENU) {
                closeFolder();
                return true;
            }
            folder.handleKey(kc);
            return true;
        }

        switch (kc) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                dock.handleKey(kc);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: {
                TileView t = dock.focusedTile();
                Apps.AppInfo a = t != null ? t.app() : null;
                if (a != null) {
                    if (a.folder) openFolder();
                    else if (a.launch != null) startActivity(a.launch);
                }
                return true;
            }
            case KeyEvent.KEYCODE_MENU:
                openFolder();
                return true;
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTopBar();
        String configuredEndpoint = Weather.loadEndpoint(getApplicationContext());
        if (weatherEndpoint == null ? configuredEndpoint != null : !weatherEndpoint.equals(configuredEndpoint)) {
            if (weatherTask != null) weatherTask.cancel(true);
            weatherTask = null;
            weatherEndpoint = configuredEndpoint;
            weatherOk = false;
            lastWeatherTry = 0;
            temp.setText("—");
            pressure.setText("—");
            wind.setText("—");
            if (weatherEndpoint != null) fetchWeather();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            root.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(appsRefresh);
        if (weatherTask != null) weatherTask.cancel(true);
        if (appsReceiver != null) unregisterReceiver(appsReceiver);
    }
}
