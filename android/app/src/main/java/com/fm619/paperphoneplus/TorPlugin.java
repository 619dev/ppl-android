package com.fm619.paperphoneplus;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
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

import java.util.concurrent.Executor;

/** Starts the bundled Tor daemon and routes all WebView traffic through it. */
@CapacitorPlugin(name = "TorPlugin")
public class TorPlugin extends Plugin {
    private static final String TAG = "TorPlugin";
    private static final int SOCKS_PORT = 9050;

    private String status = TorService.STATUS_OFF;
    private boolean receiverRegistered;
    private boolean serviceBound;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String servicePackage = intent.getStringExtra(TorService.EXTRA_SERVICE_PACKAGE_NAME);
            if (servicePackage != null && !context.getPackageName().equals(servicePackage)) return;

            String nextStatus = intent.getStringExtra(TorService.EXTRA_STATUS);
            if (nextStatus == null) return;
            status = nextStatus;
            if (TorService.STATUS_ON.equals(status)) applyTorProxy();

            JSObject event = new JSObject();
            event.put("status", status);
            event.put("host", "127.0.0.1");
            event.put("port", SOCKS_PORT);
            notifyListeners("statusChange", event);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            serviceBound = true;
            Log.i(TAG, "Embedded Tor service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            status = TorService.STATUS_OFF;
        }
    };

    @Override
    public void load() {
        super.load();
        startEmbeddedTor();
    }

    private void startEmbeddedTor() {
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
        if (!serviceBound) {
            Intent intent = new Intent(context, TorService.class);
            intent.setAction(TorService.ACTION_START);
            serviceBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
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
                () -> Log.i(TAG, "WebView is routed through embedded Tor")
        );
    }

    @PluginMethod
    public void start(PluginCall call) {
        startEmbeddedTor();
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
        super.handleOnDestroy();
    }
}
