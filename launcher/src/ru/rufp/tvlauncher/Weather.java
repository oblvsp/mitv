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
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Weather {

    interface Listener {
        void onWeather(double tempC, int pressureMm, double windMs);
    }

    private static ExecutorService executor;

    static String loadEndpoint(android.content.Context context) {
        Properties location = new Properties();
        try (InputStream in = context.getAssets().open("weather.properties")) {
            location.load(in);
            String latText = location.getProperty("latitude", "").trim();
            String lonText = location.getProperty("longitude", "").trim();
            if (latText.isEmpty() || lonText.isEmpty()) {
                Log.i("rufp", "weather location is not configured");
                return null;
            }
            double lat = Double.parseDouble(latText);
            double lon = Double.parseDouble(lonText);
            if (!Double.isFinite(lat) || lat < -90 || lat > 90
                    || !Double.isFinite(lon) || lon < -180 || lon > 180) {
                throw new IllegalArgumentException("coordinates are outside valid ranges");
            }
            return String.format(Locale.US,
                    "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.5f&lon=%.5f",
                    lat, lon);
        } catch (java.io.FileNotFoundException e) {
            Log.i("rufp", "weather is disabled; copy docs/weather.properties.example to launcher/assets/weather.properties");
            return null;
        } catch (Exception e) {
            Log.w("rufp", "invalid weather.properties", e);
            return null;
        }
    }

    static Future<?> fetch(String endpoint, Listener l) {
        return executor().submit(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "MiTV-Launcher/1.0 (+https://github.com/oblvsp/mitv)");
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
        });
    }

    private static synchronized ExecutorService executor() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "weather");
                t.setDaemon(true);
                return t;
            });
        }
        return executor;
    }

    private Weather() {}
}
