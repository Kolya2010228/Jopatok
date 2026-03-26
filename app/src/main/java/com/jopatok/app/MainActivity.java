package com.jopatok.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Главное приложение Jopatok
 */
public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PICK_FOLDER = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;
    
    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout folderSelector;
    private TextView emptyText;
    
    private VideoManager videoManager;
    private List<Uri> selectedFolders = new ArrayList<>();
    private List<VideoItem> allVideos = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        videoManager = new VideoManager(this);
        
        initViews();
        checkPermissions();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        folderSelector = findViewById(R.id.folderSelector);
        emptyText = findViewById(R.id.emptyText);
        
        // Настройка RecyclerView для вертикальных свайпов
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        
        videoAdapter = new VideoAdapter(this);
        recyclerView.setAdapter(videoAdapter);
        
        // Кнопка выбора папки
        Button selectFolderBtn = findViewById(R.id.selectFolderBtn);
        selectFolderBtn.setOnClickListener(v -> pickFolder());
        
        // Обновление по свайпу
        swipeRefreshLayout.setOnRefreshListener(this::loadVideos);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_700
        );
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            } else {
                showFolderSelector();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.READ_MEDIA_VIDEO) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.READ_MEDIA_VIDEO
                }, REQUEST_CODE_PERMISSIONS);
            } else {
                showFolderSelector();
            }
        } else {
            // Android 6-12
            if (ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, REQUEST_CODE_PERMISSIONS);
            } else {
                showFolderSelector();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                showFolderSelector();
            } else {
                Toast.makeText(this, "Нужны разрешения для доступа к видео", 
                    Toast.LENGTH_LONG).show();
                showFolderSelector();
            }
        }
    }
    
    private void requestManageStoragePermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
    }
    
    private void showFolderSelector() {
        folderSelector.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
    }
    
    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                       Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Сохраняем разрешение на доступ
                getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                selectedFolders.add(uri);
                loadVideos();
            }
        }
    }
    
    @OptIn(markerClass = UnstableApi.class)
    private void loadVideos() {
        swipeRefreshLayout.setRefreshing(true);
        
        new Thread(() -> {
            List<VideoItem> videos = videoManager.scanFolders(selectedFolders);
            allVideos = videos;
            
            runOnUiThread(() -> {
                swipeRefreshLayout.setRefreshing(false);
                
                if (videos.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    videoAdapter.setVideos(videos);
                    folderSelector.setVisibility(View.GONE);
                }
            });
        }).start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (videoAdapter != null) {
            videoAdapter.releasePlayer();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoAdapter != null) {
            videoAdapter.releasePlayer();
        }
    }
}
