package com.jopatok.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    public VideoAdapter(Context context, Player player) {
        this.context = context;
        this.player = player;
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

    public void playVideoAt(int position) {
        int previousPosition = currentPlayingPosition;
        currentPlayingPosition = position;
        
        if (previousPosition >= 0 && previousPosition < videos.size()) {
            notifyItemChanged(previousPosition);
        }
        if (position >= 0 && position < videos.size()) {
            notifyItemChanged(position);
        }
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
        private TextView videoTitle;
        private TextView videoFolder;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            videoFolder = itemView.findViewById(R.id.videoFolder);
        }

        public void bind(int position) {
            if (videos == null || position < 0 || position >= videos.size()) {
                return;
            }

            VideoItem video = videos.get(position);

            // Устанавливаем информацию о видео
            if (videoTitle != null && video.getTitle() != null) {
                videoTitle.setText(video.getTitle());
            }
            if (videoFolder != null && video.getFolderName() != null) {
                videoFolder.setText(video.getFolderName());
            }

            try {
                // Привязываем плеер к PlayerView
                if (playerView != null && player != null) {
                    playerView.setPlayer(player);

                    // Устанавливаем медиа только если это текущая позиция
                    if (position == currentPlayingPosition) {
                        if (video.getUri() != null) {
                            MediaItem mediaItem = MediaItem.fromUri(video.getUri());
                            player.setMediaItem(mediaItem);
                            player.prepare();
                            player.play();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding video at position " + position + ": " + e.getMessage());
            }
        }
    }
}
