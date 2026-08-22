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
import net.freehaven.tor.control.TorControlConnection;

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

/** Starts the bundled Tor daemon and routes all WebView traffic through it. */
@CapacitorPlugin(name = "TorPlugin")
public class TorPlugin extends Plugin {
    private static final String TAG = "TorPlugin";
    private static final long DIRECT_CONNECT_TIMEOUT_MS = 20_000;
    private static final long WEBTUNNEL_CONNECT_TIMEOUT_MS = 45_000;
    private static final String WEBTUNNEL_SETTINGS_URL =
            "https://bridges.torproject.org/moat/circumvention/settings";
    private static final String WEBTUNNEL_CACHE_KEY = "webtunnel_bridge";
    private static volatile TorService activeTorService;

    private String status = TorService.STATUS_OFF;
    private boolean proxyReady;
    private boolean usingWebTunnel;
    private boolean fallbackInProgress;
    private boolean receiverRegistered;
    private boolean serviceBound;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private Controller transportController;

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
                applyTorProxy();
            } else {
                notifyStatusChange();
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            serviceBound = true;
            if (binder instanceof TorService.LocalBinder) {
                activeTorService = ((TorService.LocalBinder) binder).getService();
            }
            Log.i(TAG, "Embedded Tor service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            activeTorService = null;
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
                    getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                            .edit().putString(WEBTUNNEL_CACHE_KEY, bridge).apply();
                    return bridge;
                }
            }
        }
        return null;
    }

    private String loadCachedWebTunnelBridge() {
        String bridge = getContext().getSharedPreferences("tor_transport", Context.MODE_PRIVATE)
                .getString(WEBTUNNEL_CACHE_KEY, null);
        return validateWebTunnelBridge(bridge);
    }

    private String validateWebTunnelBridge(String bridge) {
        if (bridge == null) return null;
        String value = bridge.trim();
        if (!value.startsWith("webtunnel ") || value.contains("\n") || value.contains("\r") ||
                !value.contains(" url=https://") || !value.matches(".*\\sver=[0-9.]+(?:\\s.*)?$")) {
            return null;
        }
        return value;
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
        usingWebTunnel = true;
        status = "STARTING_WEBTUNNEL";
        notifyStatusChange();
        mainHandler.postDelayed(this::startEmbeddedTor, 1_000);
        mainHandler.removeCallbacks(webTunnelConnectTimeout);
        mainHandler.postDelayed(webTunnelConnectTimeout, WEBTUNNEL_CONNECT_TIMEOUT_MS);
    }

    private void stopEmbeddedTor() {
        Context context = getContext();
        if (serviceBound) {
            context.unbindService(connection);
            serviceBound = false;
        }
        activeTorService = null;
        context.stopService(new Intent(context, TorService.class));
    }

    /** Refresh client circuits and the hidden-service descriptor after a routing failure. */
    static synchronized boolean refreshOnionRoute(String onionHost) {
        TorService service = activeTorService;
        if (service == null) return false;
        try {
            TorControlConnection control = service.getTorControlConnection();
            if (control == null) return false;
            control.signal("NEWNYM");
            if (onionHost != null && onionHost.endsWith(".onion")) {
                control.hsFetch(onionHost.substring(0, onionHost.length() - ".onion".length()));
            }
            Log.i(TAG, "Refreshed Tor route and onion descriptor after request failure");
            return true;
        } catch (Exception error) {
            Log.w(TAG, "Unable to request a fresh Tor identity", error);
            return false;
        }
    }

    private void applyTorProxy() {
        int socksPort = TorService.socksPort;
        if (socksPort < 1 || socksPort > 65535) {
            proxyReady = false;
            Log.e(TAG, "Tor reported ON without a valid SOCKS listener");
            notifyStatusChange();
            return;
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.e(TAG, "WebView proxy override is unavailable; refusing clearnet fallback");
            return;
        }
        ProxyConfig config = new ProxyConfig.Builder()
                .addProxyRule("socks5://127.0.0.1:" + socksPort)
                .build();
        Executor executor = runnable -> getActivity().runOnUiThread(runnable);
        ProxyController.getInstance().setProxyOverride(
                config,
                executor,
                () -> {
                    proxyReady = true;
                    Log.i(TAG, "WebView is routed through embedded Tor on port " + socksPort);
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
        int socksPort = TorService.socksPort;
        result.put("port", socksPort);
        result.put("ready", TorService.STATUS_ON.equals(status) && proxyReady && socksPort > 0);
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
        activeTorService = null;
        mainHandler.removeCallbacks(directConnectTimeout);
        mainHandler.removeCallbacks(webTunnelConnectTimeout);
        backgroundExecutor.shutdownNow();
        super.handleOnDestroy();
    }
}
