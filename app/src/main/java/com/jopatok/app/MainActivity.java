package com.jopatok.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
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

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PICK_FOLDER = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout folderSelector;
    private TextView emptyText;
    
    private Player player;
    private VideoManager videoManager;
    private List<Uri> selectedFolders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            videoManager = new VideoManager(this);
            
            // Инициализация ExoPlayer
            player = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build();

            initViews();
            checkPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Ошибка запуска: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
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

        // Создаем адаптер с общим плеером
        videoAdapter = new VideoAdapter(this, player);
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
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.READ_MEDIA_VIDEO
                }, REQUEST_CODE_PERMISSIONS);
            } else {
                showFolderSelector();
            }
        } else {
            // Android 6-12
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
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
                try {
                    // Сохраняем разрешение на доступ
                    getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    selectedFolders.add(uri);
                    loadVideos();
                } catch (Exception e) {
                    Log.e(TAG, "Error taking URI permission: " + e.getMessage());
                    Toast.makeText(this, "Ошибка доступа к папке", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadVideos() {
        swipeRefreshLayout.setRefreshing(true);

        new Thread(() -> {
            try {
                List<VideoItem> videos = videoManager.scanFolders(selectedFolders);
                
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (videos == null || videos.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        Toast.makeText(this, "Видео не найдено", Toast.LENGTH_SHORT).show();
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        videoAdapter.setVideos(videos);
                        folderSelector.setVisibility(View.GONE);
                        
                        // Запускаем первое видео
                        if (!videos.isEmpty()) {
                            videoAdapter.playVideoAt(0);
                        }
                        
                        Toast.makeText(this, "Найдено видео: " + videos.size(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading videos: " + e.getMessage());
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && recyclerView.getChildCount() > 0) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoAdapter != null) {
            videoAdapter.releasePlayer();
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
