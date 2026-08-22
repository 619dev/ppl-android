package com.fm619.paperphonelite;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.torproject.jni.TorService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.SSLSocketFactory;

/** Performs onion HTTP requests using a remote-DNS SOCKS5 handshake with Tor. */
@CapacitorPlugin(name = "TorHttp")
public class TorHttpPlugin extends Plugin {
    private static final String TAG = "TorHttp";
    private static final int ONION_IO_TIMEOUT_MS = 30_000;
    private static final long WEBTUNNEL_RECOVERY_TIMEOUT_MS = 75_000;
    private static final int MAX_ATTEMPTS = 2;
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
            if (!url.getHost().toLowerCase().matches("[a-z2-7]{56}\\.onion")) {
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
            try {
                NativeResponse response = execute(url, method, headers, body);
                JSObject responseHeaders = new JSObject();
                for (Map.Entry<String, String> entry : response.headers.entrySet()) {
                    responseHeaders.put(entry.getKey(), entry.getValue());
                }
                JSObject result = new JSObject();
                result.put("status", response.status);
                result.put("body", response.body);
                result.put("headers", responseHeaders);
                call.resolve(result);
            } catch (Exception error) {
                Log.e(TAG, "Onion request failed", error);
                call.reject("Tor request failed: " + error.getMessage(), error);
            }
        });
    }

    private static NativeResponse execute(URL url, String method, JSObject headers, String body) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return executeOnce(url, method, headers, body);
            } catch (SocksReplyException error) {
                lastError = error;
                // Host/network unreachable and TTL-expired replies can be
                // circuit-specific. Give Tor a chance to build a fresh stream.
                if (error.code != 0x04 && error.code != 0x06) throw error;
            } catch (SocketTimeoutException error) {
                lastError = error;
            }
            if (attempt < MAX_ATTEMPTS) {
                // A healthy generic Tor circuit does not prove that onion
                // streams work on the current network. Replace the bridge and
                // retry the original request through the fresh WebTunnel.
                boolean recovering = TorPlugin.requestWebTunnelRecovery();
                if (!recovering || !TorPlugin.awaitWebTunnelReady(WEBTUNNEL_RECOVERY_TIMEOUT_MS)) {
                    TorPlugin.refreshOnionRoute(url.getHost());
                    Thread.sleep(2_000L);
                }
            }
        }
        throw new IllegalStateException("Onion service unreachable after " + MAX_ATTEMPTS + " Tor attempts: " +
                (lastError == null ? "unknown error" : lastError.getMessage()), lastError);
    }

    private static NativeResponse executeOnce(URL url, String method, JSObject headers, String body) throws Exception {
        int socksPort = TorService.socksPort;
        if (socksPort < 1 || socksPort > 65535) throw new IllegalStateException("Tor SOCKS listener is not ready");
        int targetPort = url.getPort() > 0 ? url.getPort() : ("https".equals(url.getProtocol()) ? 443 : 80);
        String host = url.getHost();
        byte[] hostBytes = host.getBytes(StandardCharsets.US_ASCII);
        if (hostBytes.length > 255) throw new IllegalArgumentException("Onion hostname is too long");

        Socket socket = new Socket();
        String timeoutStage = "local Tor SOCKS listener";
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", socksPort), 5_000);
            socket.setSoTimeout(ONION_IO_TIMEOUT_MS);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            OutputStream output = socket.getOutputStream();

            output.write(new byte[] { 0x05, 0x01, 0x00 });
            output.flush();
            if (input.readUnsignedByte() != 0x05 || input.readUnsignedByte() != 0x00) {
                throw new IllegalStateException("Tor SOCKS authentication negotiation failed");
            }

            ByteArrayOutputStream connect = new ByteArrayOutputStream();
            connect.write(new byte[] { 0x05, 0x01, 0x00, 0x03, (byte) hostBytes.length });
            connect.write(hostBytes);
            connect.write((targetPort >>> 8) & 0xff);
            connect.write(targetPort & 0xff);
            output.write(connect.toByteArray());
            output.flush();

            timeoutStage = "Tor onion connection";
            int version = input.readUnsignedByte();
            int reply = input.readUnsignedByte();
            input.readUnsignedByte();
            int addressType = input.readUnsignedByte();
            if (version != 0x05) throw new IllegalStateException("Invalid Tor SOCKS response version");
            if (reply != 0x00) throw new SocksReplyException(reply);
            skipSocksAddress(input, addressType);

            if ("https".equals(url.getProtocol())) {
                socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, targetPort, true);
                socket.setSoTimeout(ONION_IO_TIMEOUT_MS);
                input = new DataInputStream(socket.getInputStream());
                output = socket.getOutputStream();
            }

            byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            String path = url.getFile();
            if (path == null || path.isEmpty()) path = "/";
            StringBuilder request = new StringBuilder();
            request.append(method).append(' ').append(path).append(" HTTP/1.1\r\nHost: ").append(host);
            if (url.getPort() > 0) request.append(':').append(url.getPort());
            request.append("\r\nAccept: application/json\r\nAccept-Encoding: gzip\r\nConnection: close\r\n");
            Iterator<String> names = headers.keys();
            while (names.hasNext()) {
                String name = names.next();
                if ("host".equalsIgnoreCase(name) || "connection".equalsIgnoreCase(name) || "content-length".equalsIgnoreCase(name)) continue;
                request.append(name).append(": ").append(headers.optString(name, "")).append("\r\n");
            }
            if (bodyBytes.length > 0) request.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            request.append("\r\n");
            output.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
            if (bodyBytes.length > 0) output.write(bodyBytes);
            output.flush();
            timeoutStage = "onion HTTP response";
            return readHttpResponse(input);
        } catch (SocketTimeoutException error) {
            throw new SocketTimeoutException(timeoutStage + " timed out");
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private static void skipSocksAddress(DataInputStream input, int type) throws Exception {
        int length;
        if (type == 0x01) length = 4;
        else if (type == 0x04) length = 16;
        else if (type == 0x03) length = input.readUnsignedByte();
        else throw new IllegalStateException("Invalid SOCKS address type");
        byte[] ignoredAddressAndPort = new byte[length + 2];
        input.readFully(ignoredAddressAndPort);
    }

    private static NativeResponse readHttpResponse(InputStream input) throws Exception {
        String statusLine = readAsciiLine(input);
        String[] statusParts = statusLine.split(" ", 3);
        if (statusParts.length < 2) throw new IllegalStateException("Invalid HTTP status line");
        int status = Integer.parseInt(statusParts[1]);
        Map<String, String> headers = new LinkedHashMap<>();
        while (true) {
            String line = readAsciiLine(input);
            if (line.isEmpty()) break;
            int colon = line.indexOf(':');
            if (colon > 0) headers.put(line.substring(0, colon).trim().toLowerCase(), line.substring(colon + 1).trim());
        }

        byte[] payload;
        if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) {
            payload = decodeChunked(input);
        } else if (headers.containsKey("content-length")) {
            int contentLength = Integer.parseInt(headers.get("content-length"));
            if (contentLength < 0 || contentLength > 32 * 1024 * 1024) throw new IllegalStateException("Invalid HTTP content length");
            payload = new byte[contentLength];
            new DataInputStream(input).readFully(payload);
        } else if (status == 204 || status == 304 || (status >= 100 && status < 200)) {
            payload = new byte[0];
        } else {
            payload = readAll(input);
        }
        if ("gzip".equalsIgnoreCase(headers.get("content-encoding"))) payload = readAll(new GZIPInputStream(new ByteArrayInputStream(payload)));
        return new NativeResponse(status, new String(payload, StandardCharsets.UTF_8), headers);
    }

    private static byte[] decodeChunked(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (true) {
            String line = readAsciiLine(input);
            int semicolon = line.indexOf(';');
            int size = Integer.parseInt((semicolon >= 0 ? line.substring(0, semicolon) : line).trim(), 16);
            if (size == 0) break;
            byte[] chunk = new byte[size];
            int offset = 0;
            while (offset < size) {
                int count = input.read(chunk, offset, size - offset);
                if (count < 0) throw new EOFException("Truncated chunked response");
                offset += count;
            }
            output.write(chunk);
            if (input.read() != 13 || input.read() != 10) throw new IllegalStateException("Invalid chunk terminator");
        }
        return output.toByteArray();
    }

    private static String readAsciiLine(InputStream input) throws Exception {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            if (previous == 13 && current == 10) break;
            if (previous >= 0) line.write(previous);
            previous = current;
        }
        return line.toString(StandardCharsets.US_ASCII.name());
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static class NativeResponse {
        final int status;
        final String body;
        final Map<String, String> headers;
        NativeResponse(int status, String body, Map<String, String> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }
    }

    private static class SocksReplyException extends Exception {
        final int code;
        SocksReplyException(int code) {
            super("Tor SOCKS connection failed (code " + code + ")");
            this.code = code;
        }
    }

    @Override
    protected void handleOnDestroy() {
        executor.shutdownNow();
        super.handleOnDestroy();
    }
}
