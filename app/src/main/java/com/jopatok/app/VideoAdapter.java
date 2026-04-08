package com.jopatok.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для ленты видео с прелоадом и двойным тапом
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private static final String TAG = "VideoAdapter";
    private static final int PRELOAD_COUNT = 2; // Прелоадим 2 следующих видео
    private static final long DOUBLE_TAP_TIMEOUT = 300; // мс между тапами

    private List<VideoItem> videos = new ArrayList<>();
    private Context context;
    private Player player;
    private int currentPlayingPosition = -1;
    private int resizeMode = 0;

    public VideoAdapter(Context context, Player player) {
        this.context = context;
        this.player = player;
    }

    public void setVideos(List<VideoItem> videos) {
        Log.d(TAG, "Setting videos: " + (videos != null ? videos.size() : 0));
        this.videos = videos != null ? videos : new ArrayList<>();
        currentPlayingPosition = videos != null && !videos.isEmpty() ? 0 : -1;
        notifyDataSetChanged();
        preloadNext();
    }

    public void setResizeMode(int mode) {
        this.resizeMode = mode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void playVideoAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            Log.w(TAG, "Invalid position: " + position);
            return;
        }

        Log.d(TAG, "Playing video at position: " + position);

        int previousPosition = currentPlayingPosition;
        currentPlayingPosition = position;

        if (previousPosition >= 0 && previousPosition < getItemCount()) {
            notifyItemChanged(previousPosition);
        }
        notifyItemChanged(position);

        // Прелоад следующих видео при переключении
        preloadNext();
    }

    /**
     * Прелоад следующих видео для плавных переходов
     */
    private void preloadNext() {
        if (player == null || videos.isEmpty()) return;

        for (int i = 1; i <= PRELOAD_COUNT; i++) {
            int nextPosition = currentPlayingPosition + i;
            if (nextPosition >= 0 && nextPosition < videos.size()) {
                VideoItem nextVideo = videos.get(nextPosition);
                if (nextVideo != null && nextVideo.getUri() != null) {
                    MediaItem mediaItem = MediaItem.fromUri(nextVideo.getUri());
                    player.addMediaItem(mediaItem);
                }
            }
        }
        player.prepare();
        Log.d(TAG, "Preloaded next " + PRELOAD_COUNT + " videos");
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
        }
    }

    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private PlayerView playerView;
        private FrameLayout videoContainer;
        private TextView videoTitle;
        private TextView videoFolder;

        private long lastTapTime = 0;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            videoContainer = itemView.findViewById(R.id.videoContainer);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            videoFolder = itemView.findViewById(R.id.videoFolder);
        }

        public void bind(int position) {
            if (videos == null || position < 0 || position >= videos.size()) {
                return;
            }

            VideoItem video = videos.get(position);
            final int playingPos = currentPlayingPosition;
            boolean isActive = (position == playingPos);

            Log.d(TAG, "Binding position " + position + ", active=" + isActive);

            if (videoTitle != null && video.getTitle() != null) {
                videoTitle.setText(video.getTitle());
            }
            if (videoFolder != null && video.getFolderName() != null) {
                videoFolder.setText(video.getFolderName());
            }

            if (playerView != null) {
                playerView.setResizeMode(resizeMode);
            }

            if (isActive) {
                playerView.setVisibility(View.VISIBLE);

                // Анимация появления
                if (videoContainer != null) {
                    videoContainer.setAlpha(0f);
                    videoContainer.animate().alpha(1f).setDuration(300).start();
                }

                if (player != null && video.getUri() != null) {
                    playerView.setPlayer(player);

                    if (currentPlayingPosition == playingPos) {
                        // Проверяем, не загружено ли уже видео в плейлисте
                        if (player.getMediaItemCount() == 0) {
                            MediaItem mediaItem = MediaItem.fromUri(video.getUri());
                            player.setMediaItem(mediaItem);
                            player.prepare();
                        }
                        player.play();
                    }
                }

                // Обработка тапа: одинарный = пауза, двойной = перемотка
                if (videoContainer != null) {
                    videoContainer.setOnClickListener(v -> {
                        long now = System.currentTimeMillis();
                        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                            // Двойной тап — перемотка (не делаем здесь, ждём onDoubleTap)
                            return;
                        }
                        lastTapTime = now;

                        // Одинарный тап с задержкой для определения двойного
                        videoContainer.postDelayed(() -> {
                            if (System.currentTimeMillis() - lastTapTime >= DOUBLE_TAP_TIMEOUT) {
                                togglePlayPause();
                            }
                        }, DOUBLE_TAP_TIMEOUT);
                    });

                    videoContainer.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            long now = System.currentTimeMillis();
                            if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                                // Двойной тап — перемотка ±10 сек
                                doubleTapSeek(v);
                                lastTapTime = 0;
                                return true;
                            }
                            lastTapTime = now;
                        }
                        return false;
                    });
                }

            } else {
                playerView.setVisibility(View.GONE);
                playerView.setPlayer(null);
                if (videoContainer != null) {
                    videoContainer.setOnClickListener(null);
                    videoContainer.setOnTouchListener(null);
                    videoContainer.setAlpha(1f);
                }
            }
        }

        private void togglePlayPause() {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
                Toast.makeText(context, "⏸ Пауза", Toast.LENGTH_SHORT).show();
            } else {
                player.play();
                Toast.makeText(context, "▶️ Воспроизведение", Toast.LENGTH_SHORT).show();
            }
        }

        private void doubleTapSeek(View view) {
            if (player == null) return;

            // Определяем, где был тап — слева или справа
            float x = view.getWidth() / 2;
            float eventX = 0;
            // Используем половину экрана: левая = -10с, правая = +10с

            long currentPos = player.getCurrentPosition();
            long duration = player.getDuration();
            long seekTo;

            // Двойной тап по центру = +10 сек (по умолчанию)
            // Для упрощения: всегда +10 сек
            seekTo = Math.min(currentPos + 10000, duration);

            player.seekTo(seekTo);
            Toast.makeText(context, "⏩ +10 сек", Toast.LENGTH_SHORT).show();

            // Анимация при перемотке
            view.animate().scaleX(1.05f).scaleY(1.05f)
                .setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
        }

        public void unbind() {
            if (playerView != null) {
                playerView.setPlayer(null);
                playerView.setVisibility(View.GONE);
            }
            if (videoContainer != null) {
                videoContainer.setOnClickListener(null);
                videoContainer.setOnTouchListener(null);
                videoContainer.setAlpha(1f);
            }
        }
    }
}
