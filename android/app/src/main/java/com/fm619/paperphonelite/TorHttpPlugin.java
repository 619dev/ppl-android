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
        int socksPort = TorService.socksPort;
        if (socksPort < 1 || socksPort > 65535) throw new IllegalStateException("Tor SOCKS listener is not ready");
        int targetPort = url.getPort() > 0 ? url.getPort() : ("https".equals(url.getProtocol()) ? 443 : 80);
        String host = url.getHost();
        byte[] hostBytes = host.getBytes(StandardCharsets.US_ASCII);
        if (hostBytes.length > 255) throw new IllegalArgumentException("Onion hostname is too long");

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", socksPort), 5_000);
            socket.setSoTimeout(60_000);
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

            int version = input.readUnsignedByte();
            int reply = input.readUnsignedByte();
            input.readUnsignedByte();
            int addressType = input.readUnsignedByte();
            if (version != 0x05 || reply != 0x00) throw new IllegalStateException("Tor SOCKS connection failed (code " + reply + ")");
            skipSocksAddress(input, addressType);

            if ("https".equals(url.getProtocol())) {
                socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, targetPort, true);
                socket.setSoTimeout(60_000);
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
            return readHttpResponse(input);
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
        byte[] response = readAll(input);
        int headerEnd = findHeaderEnd(response);
        if (headerEnd < 0) throw new EOFException("Invalid HTTP response from onion service");
        String[] lines = new String(response, 0, headerEnd, StandardCharsets.ISO_8859_1).split("\\r\\n");
        String[] statusParts = lines[0].split(" ", 3);
        if (statusParts.length < 2) throw new IllegalStateException("Invalid HTTP status line");
        int status = Integer.parseInt(statusParts[1]);
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) headers.put(lines[i].substring(0, colon).trim().toLowerCase(), lines[i].substring(colon + 1).trim());
        }
        byte[] payload = new byte[response.length - headerEnd - 4];
        System.arraycopy(response, headerEnd + 4, payload, 0, payload.length);
        if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) payload = decodeChunked(payload);
        if ("gzip".equalsIgnoreCase(headers.get("content-encoding"))) payload = readAll(new GZIPInputStream(new ByteArrayInputStream(payload)));
        return new NativeResponse(status, new String(payload, StandardCharsets.UTF_8), headers);
    }

    private static int findHeaderEnd(byte[] data) {
        for (int i = 0; i <= data.length - 4; i++) {
            if (data[i] == 13 && data[i + 1] == 10 && data[i + 2] == 13 && data[i + 3] == 10) return i;
        }
        return -1;
    }

    private static byte[] decodeChunked(byte[] data) throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
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

    @Override
    protected void handleOnDestroy() {
        executor.shutdownNow();
        super.handleOnDestroy();
    }
}
