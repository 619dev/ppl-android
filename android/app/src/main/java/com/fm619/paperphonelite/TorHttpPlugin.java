package com.fm619.paperphonelite;

import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Performs onion HTTP requests through the embedded Tor SOCKS listener. */
@CapacitorPlugin(name = "TorHttp")
public class TorHttpPlugin extends Plugin {
    private static final String TAG = "TorHttp";
    private static final int SOCKS_PORT = 9050;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PluginMethod
    public void request(PluginCall call) {
        String rawUrl = call.getString("url", "");
        String method = call.getString("method", "GET").toUpperCase();
        String body = call.getString("body");
        JSObject headers = call.getObject("headers", new JSObject());

        final URL url;
        try {
            url = new URL(rawUrl);
            String host = url.getHost().toLowerCase();
            if (!host.matches("[a-z2-7]{56}\\.onion")) {
                call.reject("TorHttp only accepts v3 onion URLs");
                return;
            }
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                call.reject("Unsupported onion URL protocol");
                return;
            }
        } catch (Exception error) {
            call.reject("Invalid onion URL", error);
            return;
        }

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                Proxy tor = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", SOCKS_PORT));
                connection = (HttpURLConnection) url.openConnection(tor);
                connection.setConnectTimeout(30_000);
                connection.setReadTimeout(45_000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");

                Iterator<String> names = headers.keys();
                while (names.hasNext()) {
                    String name = names.next();
                    connection.setRequestProperty(name, headers.optString(name, ""));
                }

                if (body != null && !"GET".equals(method) && !"HEAD".equals(method)) {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(bytes);
                    }
                }

                int status = connection.getResponseCode();
                InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                String responseBody = readBody(stream);
                JSObject responseHeaders = new JSObject();
                for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                        responseHeaders.put(entry.getKey().toLowerCase(), entry.getValue().get(0));
                    }
                }

                JSObject result = new JSObject();
                result.put("status", status);
                result.put("body", responseBody);
                result.put("headers", responseHeaders);
                call.resolve(result);
            } catch (Exception error) {
                Log.e(TAG, "Onion request failed", error);
                call.reject("Tor request failed: " + error.getMessage(), error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static String readBody(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) output.append(buffer, 0, read);
        }
        return output.toString();
    }

    @Override
    protected void handleOnDestroy() {
        executor.shutdownNow();
        super.handleOnDestroy();
    }
}
