package com.fm619.paperphonelite;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CapacitorPlugin(name = "AttachmentSaver")
public class AttachmentSaverPlugin extends Plugin {
    private static final class PendingFile {
        final File file;
        final FileOutputStream output;
        final String mimeType;

        PendingFile(File file, FileOutputStream output, String mimeType) {
            this.file = file;
            this.output = output;
            this.mimeType = mimeType;
        }
    }

    private final Map<String, PendingFile> pendingFiles = new ConcurrentHashMap<>();

    @PluginMethod
    public void begin(PluginCall call) {
        try {
            String fileName = sanitizeFileName(call.getString("fileName", "attachment"));
            String mimeType = call.getString("mimeType", "application/octet-stream");
            String id = UUID.randomUUID().toString();
            File directory = new File(getContext().getCacheDir(), "shared-attachments");
            if (!directory.exists() && !directory.mkdirs()) {
                call.reject("Unable to create the attachment cache");
                return;
            }
            File file = new File(directory, id + "-" + fileName);
            pendingFiles.put(id, new PendingFile(file, new FileOutputStream(file), mimeType));
            JSObject result = new JSObject();
            result.put("id", id);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Unable to start saving the attachment", error);
        }
    }

    @PluginMethod
    public void append(PluginCall call) {
        String id = call.getString("id");
        String encoded = call.getString("data");
        PendingFile pending = id == null ? null : pendingFiles.get(id);
        if (pending == null || encoded == null) {
            call.reject("Unknown attachment transfer");
            return;
        }
        try {
            pending.output.write(Base64.decode(encoded, Base64.DEFAULT));
            call.resolve();
        } catch (Exception error) {
            discard(id, pending);
            call.reject("Unable to write the attachment", error);
        }
    }

    @PluginMethod
    public void finish(PluginCall call) {
        String id = call.getString("id");
        PendingFile pending = id == null ? null : pendingFiles.remove(id);
        if (pending == null) {
            call.reject("Unknown attachment transfer");
            return;
        }
        try {
            pending.output.close();
            Uri uri = FileProvider.getUriForFile(
                    getContext(), getContext().getPackageName() + ".fileprovider", pending.file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(pending.mimeType);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(Intent.createChooser(share, pending.file.getName()));
            call.resolve();
        } catch (Exception error) {
            pending.file.delete();
            call.reject("Unable to open the Android save/share sheet", error);
        }
    }

    @PluginMethod
    public void abort(PluginCall call) {
        String id = call.getString("id");
        PendingFile pending = id == null ? null : pendingFiles.remove(id);
        if (pending != null) discard(id, pending);
        call.resolve();
    }

    private void discard(String id, PendingFile pending) {
        if (id != null) pendingFiles.remove(id);
        try { pending.output.close(); } catch (Exception ignored) { }
        pending.file.delete();
    }

    private String sanitizeFileName(String value) {
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return cleaned.isEmpty() ? "attachment" : cleaned;
    }
}
