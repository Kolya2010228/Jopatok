package com.jopatok.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Адаптер для ленты видео
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private static final String TAG = "VideoAdapter";
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

                if (player != null && video.getUri() != null) {
                    playerView.setPlayer(player);

                    if (currentPlayingPosition == playingPos) {
                        MediaItem mediaItem = MediaItem.fromUri(video.getUri());
                        player.setMediaItem(mediaItem);
                        player.prepare();
                        player.play();
                    }
                }

                // Тап по экрану = пауза/воспроизведение
                if (videoContainer != null) {
                    videoContainer.setOnClickListener(v -> {
                        if (player == null) return;
                        if (player.isPlaying()) {
                            player.pause();
                            Toast.makeText(context, "⏸ Пауза", Toast.LENGTH_SHORT).show();
                        } else {
                            player.play();
                            Toast.makeText(context, "▶️ Воспроизведение", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } else {
                playerView.setVisibility(View.GONE);
                playerView.setPlayer(null);
                if (videoContainer != null) {
                    videoContainer.setOnClickListener(null);
                }
            }
        }

        public void unbind() {
            if (playerView != null) {
                playerView.setPlayer(null);
                playerView.setVisibility(View.GONE);
            }
            if (videoContainer != null) {
                videoContainer.setOnClickListener(null);
            }
        }
    }
}
