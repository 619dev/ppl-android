package com.fm619.paperphoneplus;

import android.util.Log;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.Executor;

/**
 * Capacitor plugin that applies proxy settings to the Android WebView
 * using AndroidX WebKit's ProxyController.
 *
 * Supports HTTP, HTTPS, and SOCKS5 (best-effort) proxy types.
 */
@CapacitorPlugin(name = "ProxyPlugin")
public class ProxyPlugin extends Plugin {

    private static final String TAG = "ProxyPlugin";

    /**
     * Apply proxy configuration to the WebView.
     *
     * Expected JS call:
     *   ProxyPlugin.applyProxy({ type, host, port, username, password })
     */
    @PluginMethod()
    public void applyProxy(PluginCall call) {
        String type = call.getString("type", "http");
        String host = call.getString("host", "");
        String port = call.getString("port", "");
        String username = call.getString("username", "");
        String password = call.getString("password", "");

        if (!"socks5".equalsIgnoreCase(type)
                || !"127.0.0.1".equals(host)
                || !"9050".equals(port)) {
            call.reject("This build is locked to the embedded Tor SOCKS proxy");
            return;
        }

        // Set up proxy authentication if credentials are provided
        if (username != null && !username.isEmpty()) {
            final String user = username;
            final String pass = password != null ? password : "";
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass.toCharArray());
                }
            });
        } else {
            Authenticator.setDefault(null);
        }

        // Also set system properties for SOCKS5 and HTTP proxy (covers non-WebView traffic)
        if ("socks5".equalsIgnoreCase(type)) {
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", port);
            // Clear HTTP proxy properties
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            if (username != null && !username.isEmpty()) {
                System.setProperty("java.net.socks.username", username);
                System.setProperty("java.net.socks.password", password != null ? password : "");
            }
        } else {
            // HTTP / HTTPS proxy
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port);
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", port);
            // Clear SOCKS properties
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            if (username != null && !username.isEmpty()) {
                System.setProperty("http.proxyUser", username);
                System.setProperty("http.proxyPassword", password != null ? password : "");
            }
        }

        // Apply to WebView via ProxyController (AndroidX WebKit)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                String proxyUrl;
                switch (type != null ? type.toLowerCase() : "http") {
                    case "socks5":
                        proxyUrl = "socks5://" + host + ":" + port;
                        break;
                    case "https":
                        proxyUrl = "https://" + host + ":" + port;
                        break;
                    case "http":
                    default:
                        proxyUrl = host + ":" + port;
                        break;
                }

                ProxyConfig proxyConfig = new ProxyConfig.Builder()
                        .addProxyRule(proxyUrl)
                        .build();

                Executor executor = Runnable::run;

                ProxyController.getInstance().setProxyOverride(
                        proxyConfig,
                        executor,
                        () -> Log.i(TAG, "Proxy applied: " + type + "://" + host + ":" + port)
                );

                JSObject result = new JSObject();
                result.put("success", true);
                result.put("proxy", type + "://" + host + ":" + port);
                call.resolve(result);

            } catch (Exception e) {
                Log.e(TAG, "Failed to set proxy via ProxyController", e);
                // Still resolve since system properties are set
                JSObject result = new JSObject();
                result.put("success", true);
                result.put("fallback", true);
                result.put("message", "ProxyController failed, using system properties");
                call.resolve(result);
            }
        } else {
            Log.w(TAG, "ProxyController not supported, using system properties only");
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("fallback", true);
            result.put("message", "WebView proxy override not supported, using system properties");
            call.resolve(result);
        }
    }

    /**
     * Clear all proxy settings and revert to direct connections.
     */
    @PluginMethod()
    public void clearProxy(PluginCall call) {
        call.reject("Direct connections are disabled; embedded Tor cannot be cleared");
        /*
        // Clear system properties
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
        System.clearProperty("java.net.socks.username");
        System.clearProperty("java.net.socks.password");

        // Clear authenticator
        Authenticator.setDefault(null);

        // Clear WebView proxy
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                Executor executor = Runnable::run;
                ProxyController.getInstance().clearProxyOverride(
                        executor,
                        () -> Log.i(TAG, "Proxy cleared")
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear proxy via ProxyController", e);
            }
        }

        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
        */
    }
}
