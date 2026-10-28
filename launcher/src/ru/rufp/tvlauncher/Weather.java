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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Weather {

    interface Listener {
        void onWeather(double tempC, int pressureMm, double windMs);
    }

    interface SearchListener {
        void onResults(List<Location> results, String error);
    }

    static class Location {
        final String name;
        final String region;
        final String country;
        final double latitude;
        final double longitude;

        Location(String name, String region, String country, double latitude, double longitude) {
            this.name = name;
            this.region = region;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        String label() {
            StringBuilder label = new StringBuilder(name);
            if (region != null && !region.isEmpty() && !region.equals(name)) label.append(", ").append(region);
            if (country != null && !country.isEmpty()) label.append(", ").append(country);
            return label.toString();
        }
    }

    private static ExecutorService executor;

    static String loadEndpoint(android.content.Context context) {
        android.content.SharedPreferences saved = context.getSharedPreferences("weather", android.content.Context.MODE_PRIVATE);
        String savedLatitude = saved.getString("latitude", null);
        String savedLongitude = saved.getString("longitude", null);
        if (savedLatitude != null && savedLongitude != null) {
            String endpoint = endpointFor(savedLatitude, savedLongitude);
            if (endpoint != null) return endpoint;
        }

        Properties location = new Properties();
        try (InputStream in = context.getAssets().open("weather.properties")) {
            location.load(in);
            String endpoint = endpointFor(location.getProperty("latitude", ""), location.getProperty("longitude", ""));
            if (endpoint == null) {
                Log.i("rufp", "weather location is not configured");
                return null;
            }
            return endpoint;
        } catch (java.io.FileNotFoundException e) {
            Log.i("rufp", "weather is disabled; choose a location from the Apps folder");
            return null;
        } catch (Exception e) {
            Log.w("rufp", "invalid weather.properties", e);
            return null;
        }
    }

    private static String endpointFor(String latitude, String longitude) {
        try {
            String latText = latitude.trim();
            String lonText = longitude.trim();
            if (latText.isEmpty() || lonText.isEmpty()) return null;
            double lat = Double.parseDouble(latText);
            double lon = Double.parseDouble(lonText);
            if (!Double.isFinite(lat) || lat < -90 || lat > 90
                    || !Double.isFinite(lon) || lon < -180 || lon > 180) return null;
            return String.format(Locale.US,
                    "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.5f&lon=%.5f",
                    lat, lon);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static void searchLocations(String query, SearchListener listener) {
        executor().submit(() -> {
            List<Location> locations = new ArrayList<>();
            String error = null;
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://geocoding-api.open-meteo.com/v1/search?name="
                        + encoded + "&count=6&language=ru&format=json");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "MiTV-Launcher/1.0 (+https://github.com/oblvsp/mitv)");
                try (InputStream in = conn.getInputStream()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                    JSONObject response = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
                    org.json.JSONArray results = response.optJSONArray("results");
                    if (results != null) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject item = results.getJSONObject(i);
                            locations.add(new Location(item.optString("name"), item.optString("admin1"),
                                    item.optString("country"), item.getDouble("latitude"),
                                    item.getDouble("longitude")));
                        }
                    }
                }
            } catch (Exception e) {
                Log.w("rufp", "location search failed", e);
                error = "Не удалось выполнить поиск. Проверьте подключение к интернету.";
            } finally {
                if (conn != null) conn.disconnect();
            }
            String resultError = error;
            new Handler(Looper.getMainLooper()).post(() -> listener.onResults(locations, resultError));
        });
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
