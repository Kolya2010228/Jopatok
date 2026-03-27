package com.jopatok.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Главное приложение Jopatok
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "JopatokPrefs";
    private static final String PREF_SELECTED_FOLDERS = "selected_folders";
    private static final String PREF_SHUFFLE_VIDEOS = "shuffle_videos";
    private static final int REQUEST_CODE_PICK_FOLDER = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View folderSelector;
    private TextView emptyText;
    private ImageButton settingsBtn;
    
    private Player player;
    private VideoManager videoManager;
    private List<Uri> selectedFolders = new ArrayList<>();
    private SharedPreferences prefs;
    private PagerSnapHelper snapHelper;
    private boolean shuffleVideos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            videoManager = new VideoManager(this);
            
            // Инициализация ExoPlayer
            player = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build();

            initViews();
            loadSavedFolders();
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
        settingsBtn = findViewById(R.id.settingsBtn);

        // Настройка RecyclerView для вертикальных свайпов
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // Создаем адаптер с общим плеером
        videoAdapter = new VideoAdapter(this, player);
        recyclerView.setAdapter(videoAdapter);

        // Отслеживание скролла для авто-переключения видео
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                    if (snapView != null) {
                        int position = recyclerView.getChildAdapterPosition(snapView);
                        if (position >= 0 && position != videoAdapter.getCurrentPlayingPosition()) {
                            videoAdapter.playVideoAt(position);
                        }
                    }
                }
            }
        });

        // Кнопка настроек
        settingsBtn.setOnClickListener(v -> showSettingsDialog());

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
    
    private void loadSavedFolders() {
        Set<String> savedUris = prefs.getStringSet(PREF_SELECTED_FOLDERS, new HashSet<>());
        for (String uriString : savedUris) {
            try {
                Uri uri = Uri.parse(uriString);
                if (uri != null) {
                    selectedFolders.add(uri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading saved URI: " + e.getMessage());
            }
        }
        
        // Загружаем настройку shuffle
        shuffleVideos = prefs.getBoolean(PREF_SHUFFLE_VIDEOS, false);
        
        if (!selectedFolders.isEmpty()) {
            loadVideos();
        }
    }
    
    private void saveFolders() {
        Set<String> uriStrings = new HashSet<>();
        for (Uri uri : selectedFolders) {
            if (uri != null) {
                uriStrings.add(uri.toString());
            }
        }
        prefs.edit().putStringSet(PREF_SELECTED_FOLDERS, uriStrings).apply();
    }
    
    private void showSettingsDialog() {
        String[] options = {"Добавить папку", "Удалить папку", "Перемешать видео: " + (shuffleVideos ? "ВКЛ" : "ВЫКЛ")};
        
        new AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        pickFolder();
                        break;
                    case 1:
                        showRemoveFolderDialog();
                        break;
                    case 2:
                        toggleShuffle();
                        break;
                }
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }
    
    private void showRemoveFolderDialog() {
        if (selectedFolders.isEmpty()) {
            Toast.makeText(this, "Нет выбранных папок", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] folderNames = new String[selectedFolders.size()];
        for (int i = 0; i < selectedFolders.size(); i++) {
            folderNames[i] = "Папка #" + (i + 1);
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Удалить папку")
            .setItems(folderNames, (dialog, which) -> {
                selectedFolders.remove(which);
                saveFolders();
                loadVideos();
                Toast.makeText(this, "Папка удалена", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void toggleShuffle() {
        shuffleVideos = !shuffleVideos;
        prefs.edit().putBoolean(PREF_SHUFFLE_VIDEOS, shuffleVideos).apply();
        Toast.makeText(this, "Перемешивание: " + (shuffleVideos ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
        loadVideos();
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
                    saveFolders();
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
                
                // Перемешиваем видео если включена опция
                if (shuffleVideos && videos != null && !videos.isEmpty()) {
                    Collections.shuffle(videos);
                    Log.d(TAG, "Videos shuffled: " + videos.size() + " items");
                }
                
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (videos == null || videos.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        folderSelector.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Видео не найдено", Toast.LENGTH_SHORT).show();
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        folderSelector.setVisibility(View.GONE);
                        videoAdapter.setVideos(videos);
                        
                        // Запускаем первое видео
                        if (!videos.isEmpty()) {
                            videoAdapter.playVideoAt(0);
                        }
                        
                        String mode = shuffleVideos ? " (перемешаны)" : "";
                        Toast.makeText(this, "Найдено видео: " + videos.size() + mode, Toast.LENGTH_SHORT).show();
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
