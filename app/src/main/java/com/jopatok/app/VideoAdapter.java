package com.jopatok.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
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
    private ExoPlayer player;
    private int currentPlayingPosition = -1;

    public VideoAdapter(Context context) {
        this.context = context;
    }

    public void setVideos(List<VideoItem> videos) {
        this.videos = videos;
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
    public int getItemCount() {
        return videos != null ? videos.size() : 0;
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
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
            if (videos == null || position >= videos.size()) {
                return;
            }

            VideoItem video = videos.get(position);

            // Устанавливаем информацию о видео
            if (videoTitle != null) {
                videoTitle.setText(video.getTitle());
            }
            if (videoFolder != null) {
                videoFolder.setText(video.getFolderName());
            }

            try {
                // Инициализация плеера
                if (player == null) {
                    player = new ExoPlayer.Builder(context).build();
                    player.setRepeatMode(Player.REPEAT_MODE_ALL);
                }

                playerView.setPlayer(player);

                // Устанавливаем медиа
                if (video.getUri() != null) {
                    MediaItem mediaItem = MediaItem.fromUri(video.getUri());
                    player.setMediaItem(mediaItem);
                    player.prepare();

                    // Автовоспроизведение только если это текущая позиция
                    if (position == currentPlayingPosition) {
                        player.play();
                    } else {
                        player.pause();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding video: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void playVideoAt(int position) {
        currentPlayingPosition = position;
        notifyDataSetChanged();
    }

    public ExoPlayer getPlayer() {
        return player;
    }
}
