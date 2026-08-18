package com.fm619.paperphoneplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@CapacitorPlugin(name = "SecureStorage")
public class SecureStoragePlugin extends Plugin {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_PREFIX = "ppp.secure-storage.v1.";
    private static final String PREFS = "ppp_secure_secrets_v1";

    private String required(PluginCall call, String name) {
        String value = call.getString(name);
        if (value == null || value.isEmpty()) call.reject("Missing " + name);
        return value;
    }

    private String digest(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
    }

    private SecretKey masterKey(String account) throws Exception {
        String alias = KEY_PREFIX + digest(account);
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        if (store.containsAlias(alias)) return ((KeyStore.SecretKeyEntry) store.getEntry(alias, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private byte[] sealBytes(String account, String purpose, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, masterKey(account));
        cipher.updateAAD(("ppp:v1:" + account + ":" + purpose).getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(cipher.getIV().length + encrypted.length).put(cipher.getIV()).put(encrypted).array();
    }

    private byte[] openBytes(String account, String purpose, byte[] combined) throws Exception {
        if (combined.length < 29) throw new IllegalArgumentException("Invalid ciphertext");
        byte[] nonce = new byte[12];
        byte[] encrypted = new byte[combined.length - nonce.length];
        System.arraycopy(combined, 0, nonce, 0, nonce.length);
        System.arraycopy(combined, nonce.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, masterKey(account), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(("ppp:v1:" + account + ":" + purpose).getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(encrypted);
    }

    private SharedPreferences preferences() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @PluginMethod
    public void seal(PluginCall call) {
        String account = required(call, "account");
        String purpose = required(call, "purpose");
        String plaintext = call.getString("plaintext");
        if (account == null || purpose == null || plaintext == null) { if (plaintext == null) call.reject("Missing plaintext"); return; }
        try {
            JSObject result = new JSObject();
            result.put("ciphertext", Base64.encodeToString(sealBytes(account, purpose, plaintext.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP));
            call.resolve(result);
        } catch (Exception error) { call.reject("Encryption failed", error); }
    }

    @PluginMethod
    public void open(PluginCall call) {
        String account = required(call, "account");
        String purpose = required(call, "purpose");
        String encoded = required(call, "ciphertext");
        if (account == null || purpose == null || encoded == null) return;
        try {
            byte[] plaintext = openBytes(account, purpose, Base64.decode(encoded, Base64.DEFAULT));
            JSObject result = new JSObject();
            result.put("plaintext", new String(plaintext, StandardCharsets.UTF_8));
            call.resolve(result);
        } catch (Exception error) { call.reject("Decryption failed", error); }
    }

    @PluginMethod
    public void setSecret(PluginCall call) {
        String account = required(call, "account");
        String name = required(call, "name");
        String value = call.getString("value");
        if (account == null || name == null || value == null) { if (value == null) call.reject("Missing value"); return; }
        try {
            String key = digest(account + "\u0000" + name);
            String encrypted = Base64.encodeToString(sealBytes(account, "secret:" + name, value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            if (!preferences().edit().putString(key, encrypted).commit()) throw new IllegalStateException("Secret write failed");
            call.resolve();
        } catch (Exception error) { call.reject("Secure secret write failed", error); }
    }

    @PluginMethod
    public void getSecret(PluginCall call) {
        String account = required(call, "account");
        String name = required(call, "name");
        if (account == null || name == null) return;
        try {
            String encoded = preferences().getString(digest(account + "\u0000" + name), null);
            JSObject result = new JSObject();
            if (encoded == null) result.put("value", JSObject.NULL);
            else result.put("value", new String(openBytes(account, "secret:" + name, Base64.decode(encoded, Base64.DEFAULT)), StandardCharsets.UTF_8));
            call.resolve(result);
        } catch (Exception error) { call.reject("Secure secret read failed", error); }
    }

    @PluginMethod
    public void deleteSecret(PluginCall call) {
        String account = required(call, "account");
        String name = required(call, "name");
        if (account == null || name == null) return;
        try {
            preferences().edit().remove(digest(account + "\u0000" + name)).apply();
            call.resolve();
        } catch (Exception error) { call.reject("Secure secret deletion failed", error); }
    }
}
