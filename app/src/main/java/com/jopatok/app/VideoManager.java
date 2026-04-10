package com.jopatok.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class VideoManager {

    private static final String TAG = "VideoManager";
    private static final String PREFS_NAME = "JopatokCache";
    private static final String PREF_CACHED_VIDEOS = "cached_videos";
    private static final String PREF_CACHE_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 минут

    private Context context;
    private SharedPreferences cachePrefs;

    public VideoManager(Context context) {
        this.context = context;
        this.cachePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Сканирует папки и возвращает список видео.
     * Если кэш валиден — возвращает кэшированные данные.
     */
    public List<VideoItem> scanFolders(List<Uri> folderUris) {
        // Проверяем кэш
        List<VideoItem> cached = getCachedVideos();
        if (cached != null && !cached.isEmpty()) {
            Log.d(TAG, "Returning cached video list (" + cached.size() + " items)");
            return cached;
        }

        List<VideoItem> allVideos = new ArrayList<VideoItem>();
        if (folderUris == null || folderUris.isEmpty()) return allVideos;

        for (Uri folderUri : folderUris) {
            if (folderUri != null) {
                String folderName = getFolderNameFromUri(folderUri);
                allVideos.addAll(scanFolder(folderUri, folderName));
            }
        }

        // Кэшируем результат
        saveCachedVideos(allVideos);
        Log.d(TAG, "Scanned " + allVideos.size() + " videos and cached");
        return allVideos;
    }

    /**
     * Принудительно пересканировать (без кэша)
     */
    public List<VideoItem> scanFoldersForce(List<Uri> folderUris) {
        clearCache();
        return scanFolders(folderUris);
    }

    public void clearCache() {
        cachePrefs.edit()
            .remove(PREF_CACHED_VIDEOS)
            .remove(PREF_CACHE_TIMESTAMP)
            .apply();
        Log.d(TAG, "Cache cleared");
    }

    private List<VideoItem> getCachedVideos() {
        long timestamp = cachePrefs.getLong(PREF_CACHE_TIMESTAMP, 0);
        if (System.currentTimeMillis() - timestamp > CACHE_VALIDITY_MS) {
            return null; // Кэш устарел
        }

        String json = cachePrefs.getString(PREF_CACHED_VIDEOS, null);
        if (json == null) return null;

        try {
            JSONArray array = new JSONArray(json);
            List<VideoItem> videos = new ArrayList<VideoItem>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.getString("title");
                String uriStr = obj.getString("uri");
                String folderName = obj.getString("folderName");
                videos.add(new VideoItem(uriStr, Uri.parse(uriStr), title, folderName));
            }
            return videos;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing cached videos: " + e.getMessage());
            return null;
        }
    }

    private void saveCachedVideos(List<VideoItem> videos) {
        try {
            JSONArray array = new JSONArray();
            for (VideoItem video : videos) {
                JSONObject obj = new JSONObject();
                obj.put("title", video.getTitle());
                obj.put("uri", video.getUri().toString());
                obj.put("folderName", video.getFolderName());
                array.put(obj);
            }
            cachePrefs.edit()
                .putString(PREF_CACHED_VIDEOS, array.toString())
                .putLong(PREF_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving cached videos: " + e.getMessage());
        }
    }

    public List<VideoItem> scanFolder(Uri folderUri, String folderName) {
        List<VideoItem> videos = new ArrayList<VideoItem>();
        if (folderUri == null) return videos;

        ContentResolver resolver = context.getContentResolver();
        try {
            String docId = getDocumentId(folderUri);
            if (docId == null || docId.isEmpty()) return videos;

            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId);
            String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            };

            Cursor cursor = resolver.query(childrenUri, projection, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String documentId = cursor.getString(0);
                        String displayName = cursor.getString(1);
                        String mimeType = cursor.getString(2);

                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                            Uri subFolderUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId);
                            videos.addAll(scanFolder(subFolderUri, displayName));
                        } else if (mimeType != null && mimeType.startsWith("video/")) {
                            Uri videoUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId);
                            videos.add(new VideoItem(displayName, videoUri, displayName, folderName));
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning folder: " + e.getMessage());
        }
        return videos;
    }

    private String getDocumentId(Uri uri) {
        try {
            String docId = DocumentsContract.getTreeDocumentId(uri);
            return docId != null ? docId : "";
        } catch (Exception e) {
            Log.e(TAG, "Error getting document ID: " + e.getMessage());
            return "";
        }
    }

    private String getFolderNameFromUri(Uri uri) {
        try {
            String path = uri.getPath();
            if (path != null && path.contains("/tree/")) {
                String[] parts = path.split("/");
                if (parts.length > 0) return parts[parts.length - 1];
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting folder name: " + e.getMessage());
        }
        return "Videos";
    }
}
