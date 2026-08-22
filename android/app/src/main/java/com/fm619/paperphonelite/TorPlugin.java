package com.fm619.paperphonelite;

import IPtProxy.Controller;
import IPtProxy.IPtProxy;
import IPtProxy.OnTransportEvents;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.torproject.jni.TorService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Starts the bundled Tor daemon and routes all WebView traffic through it. */
@CapacitorPlugin(name = "TorPlugin")
public class TorPlugin extends Plugin {
    private static final String TAG = "TorPlugin";
    private static final int SOCKS_PORT = 9050;
    private static final long DIRECT_CONNECT_TIMEOUT_MS = 20_000;
    private static final long WEBTUNNEL_CONNECT_TIMEOUT_MS = 45_000;
    private static final String WEBTUNNEL_SETTINGS_URL =
            "https://bridges.torproject.org/moat/circumvention/settings";
    private static final String WEBTUNNEL_CACHE_KEY = "webtunnel_bridge";
    private static final String WEBTUNNEL_RECOVERY_BRIDGE =
            "webtunnel [2001:db8:ff6a:3189:e53b:c8d6:9668:9374]:443 " +
            "4AB7BC0386FF75EF7DB54C01F0F50C4F38169BEC " +
            "url=https://ame.neverfeltsogood.top/gYuE1shom2 ver=0.0.5";
    private static final Pattern BOOTSTRAP_PROGRESS_PATTERN = Pattern.compile("PROGRESS=(\\d{1,3})");

    private String status = TorService.STATUS_OFF;
    private boolean proxyReady;
    private boolean usingWebTunnel;
    private boolean fallbackInProgress;
    private boolean receiverRegistered;
    private boolean serviceBound;
    private int bootstrapProgress;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private Controller transportController;
    private TorService torService;

    private final Runnable bootstrapPoll = new Runnable() {
        @Override
        public void run() {
            TorService service = torService;
            if (service == null || !serviceBound) return;
            backgroundExecutor.execute(() -> {
                int progress = readBootstrapProgress(service);
                mainHandler.post(() -> {
                    if (service != torService || !serviceBound) return;
                    bootstrapProgress = progress;
                    if (progress >= 100) {
                        if (!proxyReady) applyTorProxy();
                    } else {
                        proxyReady = false;
                        notifyStatusChange();
                        mainHandler.postDelayed(this, 1_000);
                    }
                });
            });
        }
    };

    private final Runnable directConnectTimeout = () -> {
        if (!TorService.STATUS_ON.equals(status) && !fallbackInProgress && !usingWebTunnel) {
            startWebTunnelFallback();
        }
    };
    private final Runnable webTunnelConnectTimeout = () -> {
        if (!TorService.STATUS_ON.equals(status) && usingWebTunnel) {
            fallbackInProgress = false;
            status = "WEBTUNNEL_ERROR";
            notifyStatusChange();
        }
    };

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String servicePackage = intent.getStringExtra(TorService.EXTRA_SERVICE_PACKAGE_NAME);
            if (servicePackage != null && !context.getPackageName().equals(servicePackage)) return;

            String nextStatus = intent.getStringExtra(TorService.EXTRA_STATUS);
            if (nextStatus == null) return;
            if (fallbackInProgress && TorService.STATUS_OFF.equals(nextStatus)) return;
            status = nextStatus;
            proxyReady = false;
            if (TorService.STATUS_ON.equals(status)) {
                fallbackInProgress = false;
                mainHandler.removeCallbacks(directConnectTimeout);
                mainHandler.removeCallbacks(webTunnelConnectTimeout);
                startBootstrapPolling();
            } else {
                notifyStatusChange();
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            serviceBound = true;
            torService = ((TorService.LocalBinder) binder).getService();
            Log.i(TAG, "Embedded Tor service connected");
            startBootstrapPolling();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            torService = null;
            mainHandler.removeCallbacks(bootstrapPoll);
            status = TorService.STATUS_OFF;
        }
    };

    @Override
    public void load() {
        super.load();
        registerStatusReceiver();
    }

    private void registerStatusReceiver() {
        Context context = getContext();
        TorService.setBroadcastPackageName(context.getPackageName());
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                    context,
                    statusReceiver,
                    new IntentFilter(TorService.ACTION_STATUS),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
            receiverRegistered = true;
        }
    }

    private void startEmbeddedTor() {
        Context context = getContext();
        registerStatusReceiver();
        if (!serviceBound) {
            Intent intent = new Intent(context, TorService.class);
            intent.setAction(TorService.ACTION_START);
            // Starting the service requests its current status as well. This is
            // important when Tor survived an Activity/WebView recreation.
            context.startService(intent);
            serviceBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void startDirectTor() {
        boolean restartRequired = serviceBound;
        mainHandler.removeCallbacks(webTunnelConnectTimeout);
        if (transportController != null) {
            try {
                transportController.stop(IPtProxy.Webtunnel);
            } catch (Exception error) {
                Log.w(TAG, "Unable to stop previous WebTunnel transport", error);
            }
        }
        if (restartRequired) stopEmbeddedTor();
        usingWebTunnel = false;
        fallbackInProgress = false;
        proxyReady = false;
        bootstrapProgress = 0;
        status = TorService.STATUS_STARTING;
        TorService.getTorrc(getContext()).delete();
        notifyStatusChange();
        if (restartRequired) mainHandler.postDelayed(this::startEmbeddedTor, 1_000);
        else startEmbeddedTor();
        mainHandler.removeCallbacks(directConnectTimeout);
        mainHandler.postDelayed(directConnectTimeout, DIRECT_CONNECT_TIMEOUT_MS);
    }

    private void startWebTunnelFallback() {
        fallbackInProgress = true;
        status = "FETCHING_WEBTUNNEL";
        notifyStatusChange();

        backgroundExecutor.execute(() -> {
            try {
                String bridge = fetchLatestWebTunnelBridge();
                if (bridge == null) bridge = loadCachedWebTunnelBridge();
                if (bridge == null) throw new IllegalStateException("No WebTunnel bridge is available");

                long transportPort = startWebTunnelTransport();
                writeWebTunnelTorrc(bridge, transportPort);

                mainHandler.post(() -> restartTorWithWebTunnel());
            } catch (Exception error) {
                Log.e(TAG, "Unable to configure WebTunnel", error);
                mainHandler.post(() -> {
                    fallbackInProgress = false;
                    status = "WEBTUNNEL_ERROR";
                    notifyStatusChange();
                });
            }
        });
    }

    private String fetchLatestWebTunnelBridge() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(WEBTUNNEL_SETTINGS_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/vnd.api+json");
        byte[] request = "{\"country\":\"cn\",\"transports\":[\"webtunnel\"]}"
                .getBytes(StandardCharsets.UTF_8);
        connection.getOutputStream().write(request);

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            connection.disconnect();
            throw new IllegalStateException("Tor bridge service returned HTTP " + responseCode);
        }

        String response;
        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            response = body.toString();
        } finally {
            connection.disconnect();
        }

        String cached = getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                .getString(WEBTUNNEL_CACHE_KEY, null);
        String firstValid = null;
        JSONArray settings = new JSONObject(response).optJSONArray("settings");
        if (settings == null) return null;
        for (int i = 0; i < settings.length(); i++) {
            JSONObject bridges = settings.getJSONObject(i).optJSONObject("bridges");
            if (bridges == null || !"webtunnel".equals(bridges.optString("type"))) continue;
            JSONArray bridgeStrings = bridges.optJSONArray("bridge_strings");
            if (bridgeStrings == null) continue;
            for (int j = 0; j < bridgeStrings.length(); j++) {
                String bridge = validateWebTunnelBridge(bridgeStrings.optString(j));
                if (bridge != null) {
                    if (firstValid == null) firstValid = bridge;
                    // Rotate away from the last bridge when Moat offers another
                    // candidate. A bridge can connect while serving no usable
                    // consensus, which otherwise leaves bootstrap stuck at 30%.
                    if (bridge.equals(cached)) continue;
                    getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                            .edit().putString(WEBTUNNEL_CACHE_KEY, bridge).apply();
                    return bridge;
                }
            }
        }
        if (firstValid != null) {
            getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                    .edit().putString(WEBTUNNEL_CACHE_KEY, firstValid).apply();
        }
        return firstValid;
    }

    private String loadCachedWebTunnelBridge() {
        String bridge = getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                .getString(WEBTUNNEL_CACHE_KEY, null);
        String validated = validateWebTunnelBridge(bridge);
        // This bridge was observed repeatedly returning "Consensus is too old"
        // on 2026-08-22. Use the other bridge from the same official Moat
        // response when censorship prevents refreshing the list on-device.
        if (validated != null && validated.contains("2FE716635FDCAF4A70DFCEA70242014EECDFDF8B")) {
            validated = validateWebTunnelBridge(WEBTUNNEL_RECOVERY_BRIDGE);
            getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                    .edit().putString(WEBTUNNEL_CACHE_KEY, validated).apply();
        }
        return validated;
    }

    private String validateWebTunnelBridge(String bridge) {
        if (bridge == null) return null;
        String value = bridge.trim();
        if (!value.startsWith("webtunnel ") || value.contains("\n") || value.contains("\r") ||
                !value.contains(" url=https://") || !value.matches(".*\\sver=[0-9.]+(?:\\s.*)?$")) {
            return null;
        }
        // Keep the known-good mobile setting used by the iOS/macOS clients:
        // avoid randomized hybrid uTLS curves unsupported by IPtProxy's Go runtime.
        return value.matches(".*\\sutls=.*") ? value : value + " utls=none";
    }

    private long startWebTunnelTransport() throws Exception {
        File stateDir = new File(getContext().getCacheDir(), "webtunnel-pt");
        if (!stateDir.exists() && !stateDir.mkdirs()) {
            throw new IllegalStateException("Unable to create WebTunnel state directory");
        }
        if (transportController == null) {
            transportController = new Controller(
                    stateDir.getAbsolutePath(), true, false, "INFO",
                    new OnTransportEvents() {
                        @Override public void connected(String name) { Log.i(TAG, "WebTunnel transport connected"); }
                        @Override public void error(String name, Exception error) { Log.e(TAG, "WebTunnel transport error", error); }
                        @Override public void stopped(String name, Exception error) {
                            if (error != null) Log.e(TAG, "WebTunnel transport stopped", error);
                        }
                    }
            );
        }
        transportController.start(IPtProxy.Webtunnel, null);
        long port = transportController.port(IPtProxy.Webtunnel);
        if (port < 1 || port > 65535) throw new IllegalStateException("Invalid WebTunnel transport port");
        return port;
    }

    private void writeWebTunnelTorrc(String bridge, long transportPort) throws Exception {
        File torrc = TorService.getTorrc(getContext());
        try (FileWriter writer = new FileWriter(torrc, false)) {
            writer.write("UseBridges 1\n");
            writer.write("ClientTransportPlugin webtunnel socks5 127.0.0.1:" + transportPort + "\n");
            writer.write("Bridge " + bridge + "\n");
        }
    }

    private void restartTorWithWebTunnel() {
        mainHandler.removeCallbacks(directConnectTimeout);
        stopEmbeddedTor();
        clearTorDirectoryCache();
        usingWebTunnel = true;
        status = "STARTING_WEBTUNNEL";
        notifyStatusChange();
        mainHandler.postDelayed(this::startEmbeddedTor, 1_000);
        mainHandler.removeCallbacks(webTunnelConnectTimeout);
        mainHandler.postDelayed(webTunnelConnectTimeout, WEBTUNNEL_CONNECT_TIMEOUT_MS);
    }

    private void clearTorDirectoryCache() {
        File dataDir = new File(getContext().getDir("TorService", Context.MODE_PRIVATE), "data");
        String[] staleDirectoryFiles = {
                "cached-certs",
                "cached-consensus",
                "cached-microdesc-consensus",
                "cached-microdescs",
                "cached-microdescs.new"
        };
        for (String name : staleDirectoryFiles) {
            File file = new File(dataDir, name);
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Unable to remove stale Tor directory cache: " + name);
            }
        }
    }

    private void stopEmbeddedTor() {
        Context context = getContext();
        mainHandler.removeCallbacks(bootstrapPoll);
        torService = null;
        if (serviceBound) {
            context.unbindService(connection);
            serviceBound = false;
        }
        context.stopService(new Intent(context, TorService.class));
    }

    private void startBootstrapPolling() {
        if (!TorService.STATUS_ON.equals(status) || torService == null || !serviceBound) return;
        mainHandler.removeCallbacks(bootstrapPoll);
        mainHandler.post(bootstrapPoll);
    }

    private int readBootstrapProgress(TorService service) {
        try {
            String phase = service.getInfo("status/bootstrap-phase");
            Matcher matcher = BOOTSTRAP_PROGRESS_PATTERN.matcher(phase == null ? "" : phase);
            if (matcher.find()) return Math.min(100, Integer.parseInt(matcher.group(1)));
        } catch (Exception error) {
            Log.w(TAG, "Unable to read Tor bootstrap progress", error);
        }
        return 0;
    }

    private void applyTorProxy() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.e(TAG, "WebView proxy override is unavailable; refusing clearnet fallback");
            return;
        }
        ProxyConfig config = new ProxyConfig.Builder()
                .addProxyRule("socks5://127.0.0.1:" + SOCKS_PORT)
                .build();
        Executor executor = runnable -> getActivity().runOnUiThread(runnable);
        ProxyController.getInstance().setProxyOverride(
                config,
                executor,
                () -> {
                    proxyReady = true;
                    Log.i(TAG, "WebView is routed through embedded Tor");
                    notifyStatusChange();
                }
        );
    }

    private void notifyStatusChange() {
        JSObject event = statusResult();
        notifyListeners("statusChange", event);
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (!TorService.STATUS_ON.equals(status) && !fallbackInProgress) startDirectTor();
        call.resolve(statusResult());
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        call.resolve(statusResult());
    }

    private JSObject statusResult() {
        JSObject result = new JSObject();
        result.put("status", status);
        result.put("host", "127.0.0.1");
        result.put("port", SOCKS_PORT);
        result.put("ready", TorService.STATUS_ON.equals(status) && proxyReady);
        result.put("bootstrap", bootstrapProgress);
        result.put("transport", usingWebTunnel ? "webtunnel" : "direct");
        return result;
    }

    @Override
    protected void handleOnDestroy() {
        Context context = getContext();
        if (receiverRegistered) {
            context.unregisterReceiver(statusReceiver);
            receiverRegistered = false;
        }
        if (serviceBound) {
            context.unbindService(connection);
            serviceBound = false;
        }
        mainHandler.removeCallbacks(directConnectTimeout);
        mainHandler.removeCallbacks(webTunnelConnectTimeout);
        backgroundExecutor.shutdownNow();
        super.handleOnDestroy();
    }
}
