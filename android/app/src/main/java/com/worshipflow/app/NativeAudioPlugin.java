package com.worshipflow.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.activity.result.ActivityResult;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import android.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "NativeAudio")
public class NativeAudioPlugin extends Plugin {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HandlerThread audioThread = new HandlerThread("WorshipFlowAudio");
    private Handler audioHandler;
    private MediaPlayer player;
    private boolean prepared = false;
    private PluginCall pendingPlay;
    private final Map<String, OutputStream> exportStreams = new ConcurrentHashMap<>();

    @Override public void load() {
        audioThread.start();
        audioHandler = new Handler(audioThread.getLooper());
    }

    private void keepScreen(boolean enabled) {
        handler.post(() -> {
            if (getActivity() == null) return;
            if (enabled) getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
    }

    @Override protected void handleOnDestroy() {
        if (audioHandler != null) audioHandler.post(() -> {
            releasePlayer();
            audioThread.quitSafely();
        });
        for (OutputStream stream : exportStreams.values()) { try { stream.close(); } catch (Exception ignored) {} }
        exportStreams.clear();
        worker.shutdownNow();
        super.handleOnDestroy();
    }
    private boolean loopEnabled = false;
    private Runnable progressTask;

    @PluginMethod
    public void selectAudioFile(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(call, intent, "audioFileResult");
    }

    @ActivityCallback
    private void audioFileResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject out = new JSObject();
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            out.put("cancelled", true);
            call.resolve(out);
            return;
        }
        Uri uri = result.getData().getData();
        try {
            getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String name = "Audio";
            long size = 0;
            try (Cursor c = getContext().getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int n = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    int z = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (n >= 0 && !c.isNull(n)) name = c.getString(n);
                    if (z >= 0 && !c.isNull(z)) size = c.getLong(z);
                }
            }
            String mime = getContext().getContentResolver().getType(uri);
            if (!isAudio(name, mime)) { call.reject("Sélectionnez un fichier audio."); return; }
            out.put("uri", uri.toString());
            out.put("name", name);
            out.put("size", size);
            out.put("mime", mime == null ? "" : mime);
            call.resolve(out);
        } catch (Exception e) {
            call.reject("Impossible de conserver l’accès au fichier audio : " + e.getMessage());
        }
    }

    @PluginMethod
    public void chooseAudioFolder(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/root/primary"));
        }
        startActivityForResult(call, intent, "folderResult");
    }

    @PluginMethod
    public void chooseExportFolder(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/root/primary"));
        }
        startActivityForResult(call, intent, "exportFolderResult");
    }

    @ActivityCallback
    private void exportFolderResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            call.reject("Aucun dossier de destination sélectionné.");
            return;
        }
        Uri tree = result.getData().getData();
        try {
            int flags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if ((flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0) throw new SecurityException("Le fournisseur n’a pas accordé l’écriture dans ce dossier.");
            getContext().getContentResolver().takePersistableUriPermission(tree, flags);
            JSObject out = new JSObject();
            out.put("treeUri", tree.toString());
            call.resolve(out);
        } catch (Exception e) {
            call.reject("Impossible d’accéder au dossier choisi : " + e.getMessage());
        }
    }

    @PluginMethod
    public void exportServiceAudios(PluginCall call) {
        String rawTree = call.getString("treeUri");
        String folderName = call.getString("folderName", "WorshipFlow - Audio du culte");
        JSArray files = call.getArray("files");
        if (rawTree == null || files == null || files.length() == 0) {
            call.reject("Aucun audio à exporter ou dossier manquant.");
            return;
        }
        worker.execute(() -> {
            int count = 0;
            long totalBytes = 0;
            try {
                Uri tree = Uri.parse(rawTree);
                String rootId = DocumentsContract.getTreeDocumentId(tree);
                Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId);
                Uri folder = DocumentsContract.createDocument(getContext().getContentResolver(), root,
                        DocumentsContract.Document.MIME_TYPE_DIR, safeFileName(folderName));
                if (folder == null) throw new IllegalStateException("Impossible de créer le dossier d’export.");
                for (int i = 0; i < files.length(); i++) {
                    org.json.JSONObject item = files.getJSONObject(i);
                    if (item == null) continue;
                    String sourceRaw = item.getString("uri");
                    String name = safeFileName(item.optString("name", "audio-" + (i + 1) + ".mp3"));
                    String mime = item.optString("mime", "audio/mpeg");
                    if (sourceRaw == null || sourceRaw.isEmpty()) continue;
                    Uri target = DocumentsContract.createDocument(getContext().getContentResolver(), folder,
                            mime.isEmpty() ? "audio/mpeg" : mime, String.format(java.util.Locale.ROOT, "%02d - %s", i + 1, name));
                    if (target == null) throw new IllegalStateException("Impossible de créer le fichier " + name);
                    try (InputStream in = getContext().getContentResolver().openInputStream(Uri.parse(sourceRaw));
                         OutputStream out = getContext().getContentResolver().openOutputStream(target, "w")) {
                        if (in == null || out == null) throw new IllegalStateException("Lecture ou écriture impossible : " + name);
                        byte[] buffer = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buffer)) >= 0) { out.write(buffer, 0, n); totalBytes += n; }
                        out.flush();
                    }
                    count++;
                }
                JSObject out = new JSObject();
                out.put("count", count);
                out.put("bytes", totalBytes);
                out.put("folder", folder.toString());
                call.resolve(out);
            } catch (Exception e) {
                call.reject("L’export des audios a échoué après " + count + " fichier(s) : " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void createServiceExportFolder(PluginCall call) {
        String rawTree = call.getString("treeUri");
        String folderName = call.getString("folderName", "WorshipFlow - Audio du culte");
        if (rawTree == null) { call.reject("Dossier de destination manquant."); return; }
        worker.execute(() -> {
            try {
                Uri tree = Uri.parse(rawTree);
                String rootId = DocumentsContract.getTreeDocumentId(tree);
                Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId);
                Uri folder = DocumentsContract.createDocument(getContext().getContentResolver(), root,
                        DocumentsContract.Document.MIME_TYPE_DIR, safeFileName(folderName));
                if (folder == null) throw new IllegalStateException("Impossible de créer le dossier d’export.");
                JSObject out = new JSObject();
                out.put("folderUri", folder.toString());
                call.resolve(out);
            } catch (Exception e) { call.reject("Impossible de créer le dossier d’export : " + e.getMessage()); }
        });
    }

    @PluginMethod
    public void copyServiceAudio(PluginCall call) {
        String rawFolder = call.getString("folderUri"), rawSource = call.getString("sourceUri");
        if (rawFolder == null || rawSource == null) { call.reject("Audio ou dossier source manquant."); return; }
        String name = safeFileName(call.getString("name", "Audio.mp3"));
        String mime = call.getString("mime", "audio/mpeg");
        worker.execute(() -> {
            try {
                Uri folder = Uri.parse(rawFolder);
                Uri target = DocumentsContract.createDocument(getContext().getContentResolver(), folder,
                        mime.isEmpty() ? "audio/mpeg" : mime, name);
                if (target == null) throw new IllegalStateException("Impossible de créer " + name);
                long bytes = 0;
                try (InputStream in = getContext().getContentResolver().openInputStream(Uri.parse(rawSource));
                     OutputStream out = getContext().getContentResolver().openOutputStream(target, "w")) {
                    if (in == null || out == null) throw new IllegalStateException("Lecture ou écriture impossible : " + name);
                    byte[] buffer = new byte[64 * 1024]; int n;
                    while ((n = in.read(buffer)) >= 0) { out.write(buffer, 0, n); bytes += n; }
                    out.flush();
                }
                JSObject out = new JSObject(); out.put("bytes", bytes); call.resolve(out);
            } catch (Exception e) { call.reject("Copie impossible pour " + name + " : " + e.getMessage()); }
        });
    }

    @PluginMethod
    public void beginAudioBlobExport(PluginCall call) {
        String rawFolder = call.getString("folderUri");
        String name = safeFileName(call.getString("name", "Audio.mp3"));
        String mime = call.getString("mime", "audio/mpeg");
        if (rawFolder == null) { call.reject("Dossier de destination manquant."); return; }
        worker.execute(() -> {
            try {
                Uri target = DocumentsContract.createDocument(getContext().getContentResolver(), Uri.parse(rawFolder),
                        mime.isEmpty() ? "audio/mpeg" : mime, name);
                if (target == null) throw new IllegalStateException("Impossible de créer " + name);
                OutputStream stream = getContext().getContentResolver().openOutputStream(target, "w");
                if (stream == null) throw new IllegalStateException("Écriture impossible : " + name);
                String token = UUID.randomUUID().toString(); exportStreams.put(token, stream);
                JSObject out = new JSObject(); out.put("token", token); call.resolve(out);
            } catch (Exception e) { call.reject("Impossible de préparer " + name + " : " + e.getMessage()); }
        });
    }

    @PluginMethod
    public void appendAudioBlobExport(PluginCall call) {
        String token = call.getString("token"), encoded = call.getString("data");
        if (token == null || encoded == null) { call.reject("Fragment audio manquant."); return; }
        worker.execute(() -> {
            try {
                OutputStream stream = exportStreams.get(token);
                if (stream == null) throw new IllegalStateException("Export audio expiré.");
                stream.write(Base64.decode(encoded, Base64.DEFAULT)); call.resolve();
            } catch (Exception e) { call.reject("Écriture du fragment audio impossible : " + e.getMessage()); }
        });
    }

    @PluginMethod
    public void finishAudioBlobExport(PluginCall call) {
        String token = call.getString("token");
        if (token == null) { call.reject("Export audio manquant."); return; }
        worker.execute(() -> {
            OutputStream stream = exportStreams.remove(token);
            if (stream == null) { call.reject("Export audio expiré."); return; }
            try { stream.flush(); stream.close(); call.resolve(); }
            catch (Exception e) { call.reject("Fermeture du fichier audio impossible : " + e.getMessage()); }
        });
    }

    private String safeFileName(String name) {
        if (name == null) return "Audio";
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return cleaned.isEmpty() ? "Audio" : cleaned;
    }

    @ActivityCallback
    private void folderResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            call.reject("Aucun dossier sélectionné.");
            return;
        }
        Uri tree = result.getData().getData();
        try {
            int flags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContext().getContentResolver().takePersistableUriPermission(tree, flags | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            call.reject("Impossible de conserver l’accès au dossier : " + e.getMessage());
            return;
        }
        JSObject out = new JSObject();
        out.put("treeUri", tree.toString());
        out.put("displayName", tree.getLastPathSegment() == null ? "Dossier audio" : tree.getLastPathSegment());
        call.resolve(out);
    }

    @PluginMethod
    public void listAudioFiles(PluginCall call) {
        String raw = call.getString("treeUri");
        if (raw == null || raw.isEmpty()) {
            call.reject("Sélectionnez d’abord un dossier audio.");
            return;
        }
        worker.execute(() -> {
            try {
                Uri tree = Uri.parse(raw);
                String root = DocumentsContract.getTreeDocumentId(tree);
                JSArray files = new JSArray();
                Set<String> visited = new HashSet<>();
                scan(tree, root, files, visited, 0);
                JSObject out = new JSObject();
                out.put("files", files);
                call.resolve(out);
            } catch (Exception e) {
                call.reject("La recherche des fichiers audio a échoué : " + e.getMessage());
            }
        });
    }

    private void scan(Uri tree, String parentId, JSArray files, Set<String> visited, int depth) throws JSONException {
        if (depth > 12 || visited.contains(parentId) || files.length() >= 10000) return;
        visited.add(parentId);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        String[] projection = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };
        try (Cursor cursor = getContext().getContentResolver().query(children, projection, null, null, null)) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int typeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
            while (cursor.moveToNext() && files.length() < 10000) {
                String id = idCol >= 0 ? cursor.getString(idCol) : null;
                String name = nameCol >= 0 ? cursor.getString(nameCol) : "Audio";
                String mime = typeCol >= 0 ? cursor.getString(typeCol) : "";
                if (id == null) continue;
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    scan(tree, id, files, visited, depth + 1);
                } else if (isAudio(name, mime)) {
                    Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    JSObject item = new JSObject();
                    item.put("uri", doc.toString());
                    item.put("name", name == null ? "Audio" : name);
                    item.put("mime", mime == null ? "" : mime);
                    item.put("size", sizeCol >= 0 && !cursor.isNull(sizeCol) ? cursor.getLong(sizeCol) : 0);
                    files.put(item);
                }
            }
        }
    }

    private boolean isAudio(String name, String mime) {
        if (mime != null && mime.toLowerCase().startsWith("audio/")) return true;
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".flac") || n.endsWith(".opus");
    }

    @PluginMethod
    public void readAudioBase64(PluginCall call) {
        String raw = call.getString("uri");
        if (raw == null) { call.reject("Fichier audio introuvable."); return; }
        worker.execute(() -> {
            try (InputStream input = getContext().getContentResolver().openInputStream(Uri.parse(raw));
                 java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                if (input == null) throw new java.io.IOException("Fichier audio inaccessible.");
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                JSObject result = new JSObject();
                result.put("data", Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP));
                call.resolve(result);
            } catch (Exception e) {
                call.reject("Impossible d’inclure l’audio dans la sauvegarde : " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void getDuration(PluginCall call) {
        String raw = call.getString("uri");
        if (raw == null) { call.reject("Fichier audio introuvable."); return; }
        worker.execute(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(getContext(), Uri.parse(raw));
                String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                JSObject out = new JSObject();
                out.put("duration", value == null ? 0 : Long.parseLong(value) / 1000.0);
                call.resolve(out);
            } catch (Exception e) {
                call.reject("Durée audio indisponible : " + e.getMessage());
            } finally {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        });
    }

    @PluginMethod
    public void play(PluginCall call) {
        String raw = call.getString("uri");
        if (raw == null) { call.reject("Fichier audio introuvable."); return; }
        audioHandler.post(() -> {
            releasePlayer();
            pendingPlay = call;
            try {
                MediaPlayer next = new MediaPlayer();
                player = next;
                next.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                next.setWakeMode(getContext(), PowerManager.PARTIAL_WAKE_LOCK);
                next.setLooping(loopEnabled);
                next.setOnPreparedListener(mp -> {
                    if (mp != player) return;
                    try {
                        prepared = true;
                        Integer seek = call.getInt("startAt", 0);
                        if (seek != null && seek > 0) mp.seekTo(seek * 1000);
                        mp.start();
                        keepScreen(true);
                        startProgress();
                        pendingPlay = null;
                        call.resolve(new JSObject());
                    } catch (Exception e) { failPlayback(e.getMessage()); }
                });
                next.setOnCompletionListener(mp -> {
                    if (mp != player) return;
                    stopProgress();
                    keepScreen(false);
                    JSObject event = new JSObject(); event.put("uri", raw);
                    notifyListeners("ended", event);
                });
                next.setOnErrorListener((mp, what, extra) -> {
                    if (mp == player) failPlayback("Lecture audio impossible (" + what + "/" + extra + ").");
                    return true;
                });
                next.setDataSource(getContext(), Uri.parse(raw));
                next.prepareAsync();
            } catch (Exception e) { failPlayback("Impossible d’ouvrir ce fichier audio : " + e.getMessage()); }
        });
    }

    private void failPlayback(String message) {
        if (pendingPlay != null) { pendingPlay.reject(message); pendingPlay = null; }
        JSObject error = new JSObject(); error.put("message", message);
        notifyListeners("error", error);
        releasePlayer();
    }

    private interface PlayerAction { void run() throws Exception; }
    private void command(PluginCall call, PlayerAction action) {
        audioHandler.post(() -> {
            try {
                if (player == null || !prepared) { call.reject("Le lecteur audio n’est pas prêt."); return; }
                action.run();
                resolveState(call);
            } catch (Exception e) { call.reject("Commande audio impossible : " + e.getMessage()); }
        });
    }
    @PluginMethod public void pause(PluginCall call) {
        command(call, () -> { if (player.isPlaying()) player.pause(); stopProgress(); keepScreen(false); });
    }
    @PluginMethod public void resume(PluginCall call) {
        command(call, () -> { player.start(); keepScreen(true); startProgress(); });
    }
    @PluginMethod public void seekTo(PluginCall call) {
        command(call, () -> {
            Integer seconds = call.getInt("seconds", 0);
            player.seekTo(Math.min(player.getDuration(), Math.max(0, seconds == null ? 0 : seconds) * 1000));
        });
    }
    @PluginMethod public void setLoop(PluginCall call) {
        audioHandler.post(() -> {
            try {
                loopEnabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
                if (player != null && prepared) player.setLooping(loopEnabled);
                call.resolve();
            } catch (Exception e) { call.reject(e.getMessage()); }
        });
    }
    @PluginMethod public void stop(PluginCall call) {
        audioHandler.post(() -> { releasePlayer(); call.resolve(); });
    }
    @PluginMethod public void setImmersive(PluginCall call) {
        final boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        getActivity().runOnUiThread(() -> {
            try {
                Window window = getActivity().getWindow();
                View decor = window.getDecorView();
                int flags = enabled ? (View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE) : View.SYSTEM_UI_FLAG_VISIBLE;
                decor.setSystemUiVisibility(flags);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController controller = window.getInsetsController();
                    if (controller != null) {
                        if (enabled) {
                            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                            controller.hide(WindowInsets.Type.systemBars());
                        } else controller.show(WindowInsets.Type.systemBars());
                    }
                }
                JSObject result = new JSObject(); result.put("enabled", enabled); call.resolve(result);
            } catch (Exception e) { call.reject("Fullscreen: " + e.getMessage()); }
        });
    }

    private void resolveState(PluginCall call) {
        JSObject out = new JSObject();
        if (player != null && prepared) {
            out.put("time", player.getCurrentPosition() / 1000.0);
            out.put("duration", player.getDuration() / 1000.0);
            out.put("playing", player.isPlaying());
        } else {
            out.put("time", 0); out.put("duration", 0); out.put("playing", false);
        }
        call.resolve(out);
    }
    private void startProgress() {
        stopProgress();
        progressTask = new Runnable() {
            @Override public void run() {
                if (player == null || !prepared) return;
                try {
                    JSObject state = new JSObject();
                    state.put("time", player.getCurrentPosition() / 1000.0);
                    state.put("duration", player.getDuration() / 1000.0);
                    state.put("playing", player.isPlaying());
                    notifyListeners("progress", state);
                    if (player.isPlaying()) audioHandler.postDelayed(this, 500);
                } catch (Exception ignored) {}
            }
        };
        audioHandler.post(progressTask);
    }
    private void stopProgress() {
        if (progressTask != null) audioHandler.removeCallbacks(progressTask);
        progressTask = null;
    }
    private void releasePlayer() {
        stopProgress();
        prepared = false;
        keepScreen(false);
        if (pendingPlay != null) { pendingPlay.reject("Lecture remplacée ou arrêtée."); pendingPlay = null; }
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }
}
