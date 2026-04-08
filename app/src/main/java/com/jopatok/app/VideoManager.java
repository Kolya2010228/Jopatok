package com.jopatok.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class VideoManager {

    private static final String TAG = "VideoManager";
    private Context context;

    public VideoManager(Context context) {
        this.context = context;
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

    public List<VideoItem> scanFolders(List<Uri> folderUris) {
        List<VideoItem> allVideos = new ArrayList<VideoItem>();
        if (folderUris == null || folderUris.isEmpty()) return allVideos;

        for (Uri folderUri : folderUris) {
            if (folderUri != null) {
                String folderName = getFolderNameFromUri(folderUri);
                allVideos.addAll(scanFolder(folderUri, folderName));
            }
        }
        return allVideos;
    }

    private String getDocumentId(Uri uri) {
        try {
            String docId = uri.getLastPathSegment();
            return docId != null ? docId : "";
        } catch (Exception e) {
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
