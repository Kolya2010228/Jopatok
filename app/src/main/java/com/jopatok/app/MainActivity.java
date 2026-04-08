package com.jopatok.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "JopatokPrefs";
    private static final String PREF_SELECTED_FOLDERS = "selected_folders";
    private static final String PREF_SHUFFLE_VIDEOS = "shuffle_videos";
    private static final String PREF_LOOP_VIDEO = "loop_video";
    private static final String PREF_PLAYBACK_SPEED = "playback_speed";
    private static final String PREF_RESIZE_MODE = "resize_mode";
    private static final int REQUEST_CODE_PICK_FOLDER = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View folderSelector;
    private TextView emptyText;
    private ImageButton settingsBtn;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private Player player;
    private VideoManager videoManager;
    private List<Uri> selectedFolders = new ArrayList<>();
    private SharedPreferences prefs;
    private PagerSnapHelper snapHelper;
    private boolean shuffleVideos = false;
    private boolean loopVideo = false;
    private float playbackSpeed = 1.0f;
    private int resizeMode = 0;
    private String[] resizeModes = {"Fit", "Fill", "Zoom"};

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        videoManager = new VideoManager(this);

        player = new ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build();

        initWakeLock();
        initViews();
        loadSavedSettings();
        checkPermissions();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        folderSelector = findViewById(R.id.folderSelector);
        emptyText = findViewById(R.id.emptyText);
        settingsBtn = findViewById(R.id.settingsBtn);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        videoAdapter = new VideoAdapter(this, player);
        recyclerView.setAdapter(videoAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastIdlePosition = 0;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                    if (snapView != null) {
                        int position = recyclerView.getChildAdapterPosition(snapView);
                        if (position >= 0 && position != lastIdlePosition) {
                            videoAdapter.playVideoAt(position);
                            lastIdlePosition = position;
                        }
                    }
                }
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_speed) {
                    showSpeedDialog();
                } else if (id == R.id.nav_zoom) {
                    resizeMode = (resizeMode + 1) % 3;
                    item.setTitle("Масштаб: " + resizeModes[resizeMode]);
                    prefs.edit().putInt(PREF_RESIZE_MODE, resizeMode).apply();
                    videoAdapter.setResizeMode(resizeMode);
                    Toast.makeText(MainActivity.this, "Масштаб: " + resizeModes[resizeMode], Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_folder) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                    showSettingsDialog();
                }
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            }
        });

        // Настроить Switch для shuffle и loop
        setupMenuSwitch(R.id.nav_shuffle,
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                    shuffleVideos = isChecked;
                    prefs.edit().putBoolean(PREF_SHUFFLE_VIDEOS, shuffleVideos).apply();
                    Toast.makeText(MainActivity.this, "Перемешивание: " + (shuffleVideos ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
                    loadVideos();
                }
            });

        setupMenuSwitch(R.id.nav_loop,
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                    loopVideo = isChecked;
                    prefs.edit().putBoolean(PREF_LOOP_VIDEO, loopVideo).apply();
                    if (player != null) {
                        player.setRepeatMode(loopVideo ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                    }
                    Toast.makeText(MainActivity.this, "Повтор: " + (loopVideo ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
                }
            });

        Button selectFolderBtn = findViewById(R.id.selectFolderBtn);
        selectFolderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.END);
                pickFolder();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadVideos();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500, R.color.purple_700);
    }

    private void setupMenuSwitch(int menuItemId, CompoundButton.OnCheckedChangeListener listener) {
        MenuItem item = navigationView.getMenu().findItem(menuItemId);
        if (item == null) return;

        View actionView = item.getActionView();
        if (actionView == null) return;

        Switch switchView = actionView.findViewById(R.id.menu_switch);
        if (switchView != null) switchView.setOnCheckedChangeListener(listener);
    }

    private void updateMenuSwitch(int menuItemId, boolean checked) {
        MenuItem item = navigationView.getMenu().findItem(menuItemId);
        if (item == null) return;
        View actionView = item.getActionView();
        if (actionView == null) return;
        Switch switchView = actionView.findViewById(R.id.menu_switch);
        if (switchView != null) switchView.setChecked(checked);
    }

    private void showSpeedDialog() {
        final String[] speeds = {"0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};
        final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        int currentIndex = 2;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - playbackSpeed) < 0.01f) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
            .setTitle("Скорость воспроизведения")
            .setSingleChoiceItems(speeds, currentIndex, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    playbackSpeed = speedValues[which];
                    prefs.edit().putFloat(PREF_PLAYBACK_SPEED, playbackSpeed).apply();
                    if (player != null) {
                        player.setPlaybackSpeed(playbackSpeed);
                    }
                    MenuItem speedItem = navigationView.getMenu().findItem(R.id.nav_speed);
                    if (speedItem != null) {
                        speedItem.setTitle("Скорость: " + speeds[which]);
                    }
                    Toast.makeText(MainActivity.this, "Скорость: " + speeds[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void loadSavedSettings() {
        Set<String> savedUris = prefs.getStringSet(PREF_SELECTED_FOLDERS, new HashSet<String>());
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

        shuffleVideos = prefs.getBoolean(PREF_SHUFFLE_VIDEOS, false);
        loopVideo = prefs.getBoolean(PREF_LOOP_VIDEO, false);
        playbackSpeed = prefs.getFloat(PREF_PLAYBACK_SPEED, 1.0f);
        resizeMode = prefs.getInt(PREF_RESIZE_MODE, 0);

        updateMenuSwitch(R.id.nav_shuffle, shuffleVideos);
        updateMenuSwitch(R.id.nav_loop, loopVideo);

        MenuItem speedItem = navigationView.getMenu().findItem(R.id.nav_speed);
        if (speedItem != null) {
            speedItem.setTitle("Скорость: " + playbackSpeed + "x");
        }

        MenuItem zoomItem = navigationView.getMenu().findItem(R.id.nav_zoom);
        if (zoomItem != null) {
            zoomItem.setTitle("Масштаб: " + resizeModes[resizeMode]);
        }

        if (player != null) {
            player.setPlaybackSpeed(playbackSpeed);
            player.setRepeatMode(loopVideo ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        }

        if (!selectedFolders.isEmpty()) {
            loadVideos();
        }
    }

    private void saveFolders() {
        Set<String> uriStrings = new HashSet<String>();
        for (Uri uri : selectedFolders) {
            if (uri != null) {
                uriStrings.add(uri.toString());
            }
        }
        prefs.edit().putStringSet(PREF_SELECTED_FOLDERS, uriStrings).apply();
    }

    private void showSettingsDialog() {
        String[] options = {"Добавить папку", "Удалить папку"};
        new AlertDialog.Builder(this)
            .setTitle("Управление папками")
            .setItems(options, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) {
                        pickFolder();
                    } else {
                        showRemoveFolderDialog();
                    }
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
            .setItems(folderNames, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    selectedFolders.remove(which);
                    saveFolders();
                    loadVideos();
                    Toast.makeText(MainActivity.this, "Папка удалена", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            } else {
                showFolderSelector();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_CODE_PERMISSIONS);
            } else {
                showFolderSelector();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            } else {
                showFolderSelector();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                Toast.makeText(this, "Нужны разрешения для доступа к видео", Toast.LENGTH_LONG).show();
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
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<VideoItem> videos = videoManager.scanFolders(selectedFolders);
                    if (shuffleVideos && videos != null && !videos.isEmpty()) {
                        Collections.shuffle(videos, new Random(System.currentTimeMillis()));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                            if (videos == null || videos.isEmpty()) {
                                emptyText.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                                folderSelector.setVisibility(View.VISIBLE);
                                Toast.makeText(MainActivity.this, "Видео не найдено", Toast.LENGTH_SHORT).show();
                            } else {
                                emptyText.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                                folderSelector.setVisibility(View.GONE);
                                videoAdapter.setVideos(new ArrayList<VideoItem>(videos));
                                videoAdapter.setResizeMode(resizeMode);
                                Toast.makeText(MainActivity.this, "Найдено видео: " + videos.size(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    Log.e(TAG, "Error loading videos: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(MainActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void initWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "Jopatok::WakeLock");
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseWakeLock();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        acquireWakeLock();
        if (player != null && videoAdapter.getCurrentPlayingPosition() >= 0) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        if (videoAdapter != null) {
            videoAdapter.releasePlayer();
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
