package com.worshipflow.app;

import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@CapacitorPlugin(name = "NativeSecurity")
public class NativeSecurityPlugin extends Plugin {
    private static final String PREFS = "worshipflow_secure_v1";
    private static final String BIO_ALIAS = "worshipflow_biometric_wrap_v1";
    private static final int ITERATIONS = 210000;
    private byte[] dataKey;

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, 0);
    }
    private String b64(byte[] value) { return Base64.encodeToString(value, Base64.NO_WRAP); }
    private byte[] unb64(String value) { return Base64.decode(value, Base64.NO_WRAP); }
    private boolean configured() { return prefs().contains("pinSalt") && prefs().contains("pinWrapped"); }
    private boolean biometricAvailable() {
        return BiometricManager.from(getContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }
    private void requireUnlocked(PluginCall call) {
        if (dataKey == null) throw new IllegalStateException("Application verrouillée. Déverrouillez-la avec la biométrie ou le PIN.");
    }
    private byte[] random(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
    private SecretKey derivePinKey(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256);
        try {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), "AES");
        } finally { spec.clearPassword(); }
    }
    private byte[] aesEncrypt(SecretKey key, byte[] plain, byte[] ivOut) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        System.arraycopy(iv, 0, ivOut, 0, iv.length);
        return cipher.doFinal(plain);
    }
    private byte[] aesDecrypt(SecretKey key, byte[] iv, byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }
    private SecretKey biometricKey(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(BIO_ALIAS)) return (SecretKey) store.getKey(BIO_ALIAS, null);
        if (!create) return null;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(BIO_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true);
        if (Build.VERSION.SDK_INT >= 24) builder.setInvalidatedByBiometricEnrollment(false);
        if (Build.VERSION.SDK_INT >= 30) builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
        else builder.setUserAuthenticationValidityDurationSeconds(-1);
        generator.init(builder.build());
        return generator.generateKey();
    }
    private Cipher biometricCipher(int mode, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        if (mode == Cipher.ENCRYPT_MODE) cipher.init(mode, biometricKey(true));
        else cipher.init(mode, biometricKey(false), new GCMParameterSpec(128, iv));
        return cipher;
    }
    private void saveBiometricWrap(PluginCall call, byte[] keyBytes) {
        try {
            Cipher cipher = biometricCipher(Cipher.ENCRYPT_MODE, null);
            BiometricPrompt prompt = biometricPrompt(call, new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    try {
                        Cipher authenticated = result.getCryptoObject().getCipher();
                        byte[] wrapped = authenticated.doFinal(keyBytes);
                        prefs().edit().putString("bioIv", b64(authenticated.getIV()))
                                .putString("bioWrapped", b64(wrapped)).apply();
                        JSObject out = new JSObject(); out.put("configured", true); out.put("biometricEnabled", true);
                        call.resolve(out);
                    } catch (Exception e) { call.reject("Impossible d'enregistrer la biométrie : " + e.getMessage(), e); }
                }
                @Override public void onAuthenticationError(int code, @NonNull CharSequence message) {
                    prefs().edit().remove("bioWrapped").remove("bioIv").apply();
                    JSObject out = new JSObject(); out.put("configured", true); out.put("biometricEnabled", false);
                    out.put("message", "Le PIN reste actif. Biométrie non activée.");
                    call.resolve(out);
                }
                @Override public void onAuthenticationFailed() { }
            });
            prompt.authenticate(promptInfo("Confirmer la biométrie pour protéger WorshipFlow"), new BiometricPrompt.CryptoObject(cipher));
        } catch (Exception e) {
            JSObject out = new JSObject(); out.put("configured", true); out.put("biometricEnabled", false);
            out.put("message", "Le PIN reste actif. Biométrie indisponible.");
            call.resolve(out);
        }
    }
    private BiometricPrompt biometricPrompt(PluginCall call, BiometricPrompt.AuthenticationCallback callback) {
        Executor executor = ContextCompat.getMainExecutor(getContext());
        return new BiometricPrompt(getActivity(), executor, callback);
    }
    private BiometricPrompt.PromptInfo promptInfo(String title) {
        return new BiometricPrompt.PromptInfo.Builder().setTitle(title)
                .setSubtitle("Utilisez votre empreinte ou la biométrie de l'appareil")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Annuler").build();
    }
    @PluginMethod public void status(PluginCall call) {
        JSObject out = new JSObject();
        out.put("configured", configured());
        out.put("unlocked", dataKey != null);
        out.put("biometricAvailable", biometricAvailable());
        out.put("biometricEnabled", prefs().contains("bioWrapped"));
        call.resolve(out);
    }
    @PluginMethod public void configurePin(PluginCall call) {
        String pin = call.getString("pin", "");
        if (pin == null || !pin.matches("[0-9]{6,12}")) { call.reject("Le PIN doit contenir 6 à 12 chiffres."); return; }
        try {
            byte[] keyBytes = random(32), salt = random(16), iv = new byte[12];
            SecretKey wrapping = derivePinKey(pin, salt);
            byte[] wrapped = aesEncrypt(wrapping, keyBytes, iv);
            prefs().edit().putString("pinSalt", b64(salt)).putString("pinIv", b64(iv))
                    .putString("pinWrapped", b64(wrapped)).remove("bioIv").remove("bioWrapped").commit();
            dataKey = keyBytes;
            if (biometricAvailable()) saveBiometricWrap(call, keyBytes);
            else { JSObject out = new JSObject(); out.put("configured", true); out.put("biometricEnabled", false); call.resolve(out); }
        } catch (Exception e) { call.reject("Configuration de la sécurité impossible : " + e.getMessage(), e); }
    }
    @PluginMethod public void unlockWithPin(PluginCall call) {
        String pin = call.getString("pin", "");
        if (pin == null || pin.isEmpty() || !configured()) { call.reject("PIN ou configuration invalide."); return; }
        try {
            byte[] key = aesDecrypt(derivePinKey(pin, unb64(prefs().getString("pinSalt", ""))),
                    unb64(prefs().getString("pinIv", "")), unb64(prefs().getString("pinWrapped", "")));
            dataKey = key;
            JSObject out = new JSObject(); out.put("unlocked", true); call.resolve(out);
        } catch (Exception e) { call.reject("PIN incorrect ou données de sécurité endommagées."); }
    }
    @PluginMethod public void unlockWithBiometrics(PluginCall call) {
        if (!prefs().contains("bioWrapped")) { call.reject("Biométrie non configurée. Utilisez le PIN."); return; }
        try {
            Cipher cipher = biometricCipher(Cipher.DECRYPT_MODE, unb64(prefs().getString("bioIv", "")));
            BiometricPrompt prompt = biometricPrompt(call, new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    try {
                        dataKey = result.getCryptoObject().getCipher().doFinal(unb64(prefs().getString("bioWrapped", "")));
                        JSObject out = new JSObject(); out.put("unlocked", true); call.resolve(out);
                    } catch (Exception e) { call.reject("Déverrouillage biométrique impossible; utilisez le PIN.", e); }
                }
                @Override public void onAuthenticationError(int code, @NonNull CharSequence message) {
                    call.reject("Biométrie annulée ou indisponible; utilisez le PIN.");
                }
                @Override public void onAuthenticationFailed() { }
            });
            prompt.authenticate(promptInfo("Déverrouiller WorshipFlow"), new BiometricPrompt.CryptoObject(cipher));
        } catch (Exception e) { call.reject("Biométrie indisponible; utilisez le PIN.", e); }
    }
    @PluginMethod public void lock(PluginCall call) {
        if (dataKey != null) Arrays.fill(dataKey, (byte) 0);
        dataKey = null; call.resolve();
    }
    @PluginMethod public void encrypt(PluginCall call) {
        try {
            requireUnlocked(call);
            String plain = call.getString("value", "");
            byte[] iv = new byte[12];
            byte[] encrypted = aesEncrypt(new SecretKeySpec(dataKey, "AES"), plain.getBytes(StandardCharsets.UTF_8), iv);
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            JSObject out = new JSObject(); out.put("value", b64(packed)); call.resolve(out);
        } catch (Exception e) { call.reject("Chiffrement local impossible.", e); }
    }
    @PluginMethod public void decrypt(PluginCall call) {
        try {
            requireUnlocked(call);
            byte[] packed = unb64(call.getString("value", ""));
            if (packed.length < 29) throw new IllegalArgumentException("Donnée chiffrée invalide.");
            byte[] iv = Arrays.copyOfRange(packed, 0, 12), encrypted = Arrays.copyOfRange(packed, 12, packed.length);
            String plain = new String(aesDecrypt(new SecretKeySpec(dataKey, "AES"), iv, encrypted), StandardCharsets.UTF_8);
            JSObject out = new JSObject(); out.put("value", plain); call.resolve(out);
        } catch (Exception e) { call.reject("Déchiffrement local impossible; PIN requis ou données altérées.", e); }
    }
}