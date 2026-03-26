package com.jopatok.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер для работы с видеофайлами
 */
public class VideoManager {
    
    private static final String TAG = "VideoManager";
    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".mkv", ".avi", ".mov", ".webm", ".3gp"};
    
    private Context context;
    
    public VideoManager(Context context) {
        this.context = context;
    }
    
    /**
     * Сканировать папку и вернуть список видео
     */
    public List<VideoItem> scanFolder(Uri folderUri) {
        List<VideoItem> videos = new ArrayList<>();
        
        ContentResolver resolver = context.getContentResolver();
        
        try {
            String docId = getDocumentId(folderUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri, docId);
            
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
                        
                        if (mimeType != null && mimeType.startsWith("video/")) {
                            Uri videoUri = DocumentsContract.buildDocumentUriUsingTree(
                                folderUri, documentId);
                            
                            String folderName = getFolderNameFromUri(folderUri);
                            videos.add(new VideoItem(displayName, videoUri, displayName, folderName));
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning folder: " + e.getMessage());
            e.printStackTrace();
        }
        
        return videos;
    }
    
    /**
     * Сканировать несколько папок
     */
    public List<VideoItem> scanFolders(List<Uri> folderUris) {
        List<VideoItem> allVideos = new ArrayList<>();
        
        for (Uri folderUri : folderUris) {
            List<VideoItem> folderVideos = scanFolder(folderUri);
            allVideos.addAll(folderVideos);
        }
        
        return allVideos;
    }
    
    /**
     * Получить видео из медиахранилища (для старых версий Android)
     */
    public List<VideoItem> getVideosFromMediaStore() {
        List<VideoItem> videos = new ArrayList<>();
        
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE
        };
        
        String selection = null;
        String[] selectionArgs = null;
        
        Cursor cursor = context.getContentResolver().query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        );
        
        if (cursor != null) {
            try {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String path = cursor.getString(dataColumn);
                    String name = cursor.getString(nameColumn);
                    
                    Uri contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 
                        String.valueOf(id));
                    
                    String folderName = new File(path).getParentFile().getName();
                    videos.add(new VideoItem(path, contentUri, name, folderName));
                }
            } finally {
                cursor.close();
            }
        }
        
        return videos;
    }
    
    private String getDocumentId(Uri uri) {
        String docId = uri.getLastPathSegment();
        return docId != null ? docId : "";
    }
    
    private String getFolderNameFromUri(Uri uri) {
        try {
            String path = uri.getPath();
            if (path != null && path.contains("/tree/")) {
                String[] parts = path.split("/");
                if (parts.length > 0) {
                    return parts[parts.length - 1];
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting folder name: " + e.getMessage());
        }
        return "Videos";
    }
}
