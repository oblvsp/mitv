package ru.rufp.tvlauncher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class Weather {

    interface Listener {
        void onWeather(double tempC, int pressureMm, double windMs);
    }

    private static final String ENDPOINT =
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=51.7727&lon=55.0988";

    static void fetch(Listener l) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "MiTV-Launcher/1.0 (personal home TV)");
                try (InputStream in = conn.getInputStream()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                    org.json.JSONArray ts = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8))
                            .getJSONObject("properties").getJSONArray("timeseries");
                    JSONObject d = ts.getJSONObject(0).getJSONObject("data")
                            .getJSONObject("instant").getJSONObject("details");
                    double t = d.getDouble("air_temperature");
                    double phPa = d.has("surface_air_pressure")
                            ? d.getDouble("surface_air_pressure")
                            : d.getDouble("air_pressure_at_sea_level");
                    int press = (int) Math.round(phPa * 0.750062);
                    double wind = d.getDouble("wind_speed");
                    new Handler(Looper.getMainLooper()).post(() -> l.onWeather(t, press, wind));
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.w("rufp", "weather failed", e);
            }
        }, "weather").start();
    }

    private Weather() {}
}
