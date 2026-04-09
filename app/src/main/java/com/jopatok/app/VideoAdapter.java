package com.jopatok.app;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private static final String TAG = "VideoAdapter";
    private static final int ITEMS_PER_PAGE = 10; // Порция для подгрузки

    private List<VideoItem> allVideos = new ArrayList<VideoItem>();
    private List<VideoItem> displayedVideos = new ArrayList<VideoItem>();
    private Context context;
    private Player player;
    private MainActivity mainActivity;
    private int currentPlayingPosition = -1;
    private int resizeMode = 0;
    private int loadedCount = 0;
    private boolean hasMore = true;
    private OnLoadMoreListener loadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public VideoAdapter(Context context, Player player, MainActivity mainActivity) {
        this.context = context;
        this.player = player;
        this.mainActivity = mainActivity;
    }

    public void setVideos(List<VideoItem> videos) {
        this.allVideos = videos != null ? videos : new ArrayList<VideoItem>();
        this.displayedVideos = new ArrayList<VideoItem>();
        this.loadedCount = 0;
        this.hasMore = true;
        loadNextPage();
    }

    /**
     * Подгрузить следующую порцию видео
     */
    public void loadNextPage() {
        if (!hasMore) return;

        int start = loadedCount;
        int end = Math.min(start + ITEMS_PER_PAGE, allVideos.size());

        for (int i = start; i < end; i++) {
            displayedVideos.add(allVideos.get(i));
        }

        loadedCount = end;
        hasMore = loadedCount < allVideos.size();

        if (start == 0) {
            currentPlayingPosition = displayedVideos.isEmpty() ? -1 : 0;
            notifyDataSetChanged();
        } else {
            notifyItemRangeInserted(start, end - start);
        }

        if (hasMore && loadMoreListener != null) {
            // Автоматически подгружаем следующую порцию
            loadMoreListener.onLoadMore();
        }
    }

    /**
     * Получить общее количество видео (для адаптера)
     */
    @Override
    public int getItemCount() {
        return displayedVideos.size();
    }

    public void setResizeMode(int mode) {
        this.resizeMode = mode;
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }

    public void playVideoAt(int position) {
        if (position < 0 || position >= getItemCount()) return;
        int previousPosition = currentPlayingPosition;
        currentPlayingPosition = position;
        if (previousPosition >= 0 && previousPosition < getItemCount()) {
            notifyItemChanged(previousPosition);
        }
        notifyItemChanged(position);
    }

    public void releasePlayer() {
        if (player != null) player.release();
    }

    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
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

        // Подгрузка при прокрутке к концу
        if (position >= getItemCount() - 2 && hasMore) {
            loadNextPage();
        }
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private PlayerView playerView;
        private FrameLayout videoContainer;
        private TextView videoTitle;
        private TextView videoFolder;
        private ImageButton shareBtn;
        private long lastTapTime = 0;
        private static final long DOUBLE_TAP_TIMEOUT = 300;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            videoContainer = itemView.findViewById(R.id.videoContainer);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            videoFolder = itemView.findViewById(R.id.videoFolder);
            shareBtn = itemView.findViewById(R.id.shareBtn);
        }

        public void bind(final int position) {
            if (displayedVideos == null || position < 0 || position >= displayedVideos.size()) return;

            final VideoItem video = displayedVideos.get(position);
            final int playingPos = currentPlayingPosition;
            boolean isActive = (position == playingPos);

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
                if (videoContainer != null) {
                    videoContainer.setAlpha(0f);
                    videoContainer.animate().alpha(1f).setDuration(300).start();
                }

                if (player != null && video.getUri() != null) {
                    playerView.setPlayer(player);
                    if (currentPlayingPosition == playingPos) {
                        player.clearMediaItems();
                        player.setMediaItem(MediaItem.fromUri(video.getUri()));
                        player.prepare();
                        player.play();
                    }
                }

                if (videoContainer != null) {
                    videoContainer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            long now = System.currentTimeMillis();
                            if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                                doubleTapSeek();
                                lastTapTime = 0;
                                return;
                            }
                            lastTapTime = now;
                            videoContainer.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (System.currentTimeMillis() - lastTapTime >= DOUBLE_TAP_TIMEOUT) {
                                        togglePlayPause();
                                    }
                                }
                            }, DOUBLE_TAP_TIMEOUT);
                        }
                    });
                }

                // Кнопка share
                if (shareBtn != null) {
                    shareBtn.setVisibility(View.VISIBLE);
                    shareBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mainActivity != null) {
                                mainActivity.setCurrentVideoUri(video.getUri());
                                mainActivity.shareCurrentVideo();
                            }
                        }
                    });
                }
            } else {
                playerView.setVisibility(View.GONE);
                playerView.setPlayer(null);
                if (videoContainer != null) {
                    videoContainer.setOnClickListener(null);
                    videoContainer.setAlpha(1f);
                }
            }
        }

        private void togglePlayPause() {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
                Toast.makeText(context, "Пауза", Toast.LENGTH_SHORT).show();
            } else {
                player.play();
                Toast.makeText(context, "Воспроизведение", Toast.LENGTH_SHORT).show();
            }
        }

        private void doubleTapSeek() {
            if (player == null) return;
            long seekTo = Math.min(player.getCurrentPosition() + 10000, player.getDuration());
            player.seekTo(seekTo);
            Toast.makeText(context, "+10 сек", Toast.LENGTH_SHORT).show();
        }

        public void unbind() {
            if (playerView != null) {
                playerView.setPlayer(null);
                playerView.setVisibility(View.GONE);
            }
            if (videoContainer != null) {
                videoContainer.setOnClickListener(null);
                videoContainer.setAlpha(1f);
            }
        }
    }
}
