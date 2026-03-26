package com.jopatok.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
        return videos.size();
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
        
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            videoContainer = itemView.findViewById(R.id.videoContainer);
        }
        
        public void bind(int position) {
            VideoItem video = videos.get(position);
            
            // Инициализация плеера только для текущего видимого элемента
            if (player == null) {
                player = new ExoPlayer.Builder(context).build();
                player.setRepeatMode(Player.REPEAT_MODE_ALL);
            }
            
            playerView.setPlayer(player);
            
            // Устанавливаем медиа
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
    }
    
    public void playVideoAt(int position) {
        currentPlayingPosition = position;
        notifyDataSetChanged();
    }
    
    public ExoPlayer getPlayer() {
        return player;
    }
}
