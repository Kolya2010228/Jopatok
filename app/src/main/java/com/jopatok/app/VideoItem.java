package com.jopatok.app;

import android.net.Uri;

/**
 * Модель видеофайла
 */
public class VideoItem {
    private String path;
    private Uri uri;
    private String title;
    private String folderName;
    
    public VideoItem(String path, Uri uri, String title, String folderName) {
        this.path = path;
        this.uri = uri;
        this.title = title;
        this.folderName = folderName;
    }
    
    public String getPath() {
        return path;
    }
    
    public Uri getUri() {
        return uri;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getFolderName() {
        return folderName;
    }
}
