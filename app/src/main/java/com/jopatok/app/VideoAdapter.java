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
        Log.d(TAG, "Setting videos: " + (videos != null ? videos.size() : 0));
        this.videos = videos != null ? videos : new ArrayList<>();
        currentPlayingPosition = -1;
        // Полная перерисовка для применения shuffle
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
        holder.unbind();
        super.onViewRecycled(holder);
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
        currentPlayingPosition = position;
        
        // Находим видимые ViewHolder и обновляем их
        for (int i = 0; i < getItemCount(); i++) {
            notifyItemChanged(i);
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
            int adapterPosition = getAdapterPosition();
            if (videos == null || adapterPosition < 0 || adapterPosition >= videos.size()) {
                return;
            }

            VideoItem video = videos.get(adapterPosition);
            boolean isActive = (adapterPosition == currentPlayingPosition);

            Log.d(TAG, "Binding position " + adapterPosition + ", active=" + isActive + ", uri=" + (video.getUri() != null));

            // Устанавливаем информацию о видео
            if (videoTitle != null && video.getTitle() != null) {
                videoTitle.setText(video.getTitle());
            }
            if (videoFolder != null && video.getFolderName() != null) {
                videoFolder.setText(video.getFolderName());
            }

            if (isActive) {
                // Активное видео - показываем и запускаем
                playerView.setVisibility(View.VISIBLE);
                
                if (player != null && video.getUri() != null) {
                    playerView.setPlayer(player);
                    
                    // Всегда загружаем новое видео для активной позиции
                    MediaItem mediaItem = MediaItem.fromUri(video.getUri());
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                    
                    Log.d(TAG, "Started playing video at position " + adapterPosition);
                }
            } else {
                // Неактивное видео - скрываем
                playerView.setVisibility(View.GONE);
                playerView.setPlayer(null);
            }
        }
        
        public void unbind() {
            if (playerView != null) {
                playerView.setPlayer(null);
                playerView.setVisibility(View.GONE);
            }
        }
    }
}
