package ru.rufp.tvlauncher;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class WeatherSettingsActivity extends Activity {

    private EditText query;
    private Button search;
    private LinearLayout results;
    private TextView status;
    private TextView current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(0x1A, 0x1C, 0x22));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        int side = (int) (getResources().getDisplayMetrics().widthPixels * 0.14f);
        content.setPadding(side, dp(32), side, dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("Погода", 38, 0xFFFFFFFF);
        content.addView(title, params(WRAP, WRAP, 0, 0, 0, dp(8)));
        current = text("", 20, 0xFFB6BCC6);
        content.addView(current, params(WRAP, WRAP, 0, 0, 0, dp(30)));
        showCurrentLocation();

        TextView prompt = text("Введите город или населённый пункт", 24, 0xFFE5E8ED);
        content.addView(prompt, params(WRAP, WRAP, 0, 0, 0, dp(12)));

        query = new EditText(this);
        query.setSingleLine(true);
        query.setTextSize(24);
        query.setMinHeight(dp(64));
        query.setHintTextColor(0xFF9299A4);
        query.setHint("Например, Екатеринбург");
        query.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        content.addView(query, params(MATCH, WRAP, 0, 0, 0, dp(12)));

        search = button("Найти место");
        content.addView(search, params(WRAP, WRAP, 0, 0, 0, dp(8)));
        search.setOnClickListener(v -> searchLocations());
        query.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchLocations();
                return true;
            }
            return false;
        });

        status = text("", 19, 0xFFB6BCC6);
        content.addView(status, params(WRAP, WRAP, 0, dp(8), 0, dp(8)));
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results, params(MATCH, WRAP, 0, 0, 0, 0));

        TextView attribution = text("Поиск мест: GeoNames через Open-Meteo", 15, 0xFF8A909A);
        content.addView(attribution, params(WRAP, WRAP, 0, dp(16), 0, 0));

        setContentView(scroll);
        query.requestFocus();
    }

    private void searchLocations() {
        String value = query.getText().toString().trim();
        if (value.length() < 2) {
            status.setText("Введите не менее двух символов.");
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(query.getWindowToken(), 0);
        search.setEnabled(false);
        status.setText("Ищем место…");
        results.removeAllViews();
        Weather.searchLocations(value, (locations, error) -> {
            if (isFinishing() || isDestroyed()) return;
            search.setEnabled(true);
            results.removeAllViews();
            if (error != null) {
                status.setText(error);
                return;
            }
            if (locations.isEmpty()) {
                status.setText("Ничего не найдено. Уточните название и попробуйте ещё раз.");
                return;
            }
            status.setText("Выберите подходящее место:");
            for (Weather.Location location : locations) {
                Button option = button(location.label());
                option.setOnClickListener(v -> saveLocation(location));
                results.addView(option, params(MATCH, WRAP, 0, 0, 0, dp(8)));
            }
            results.getChildAt(0).requestFocus();
        });
    }

    private void saveLocation(Weather.Location location) {
        getSharedPreferences("weather", MODE_PRIVATE).edit()
                .putString("latitude", Double.toString(location.latitude))
                .putString("longitude", Double.toString(location.longitude))
                .putString("name", location.label())
                .apply();
        finish();
    }

    private void showCurrentLocation() {
        String name = getSharedPreferences("weather", MODE_PRIVATE).getString("name", null);
        current.setText(name == null || name.isEmpty()
                ? "Место не выбрано" : "Сейчас в погоде: " + name);
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(20);
        button.setAllCaps(false);
        button.setMinHeight(dp(56));
        button.setPadding(dp(20), dp(8), dp(20), dp(8));
        button.setFocusable(true);
        return button;
    }

    private static final int MATCH = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams params(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.setMargins(left, top, right, bottom);
        return p;
    }
}
