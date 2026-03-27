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
        this.videos = videos;
        currentPlayingPosition = -1;
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
        return videos != null ? videos.size() : 0;
    }

    public void playVideoAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }
        
        int previousPosition = currentPlayingPosition;
        currentPlayingPosition = position;
        
        // Уведомляем предыдущий элемент
        if (previousPosition >= 0 && previousPosition < videos.size()) {
            notifyItemChanged(previousPosition);
        }
        // Уведомляем новый элемент
        notifyItemChanged(position);
        
        Log.d(TAG, "Switched to video at position " + position);
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

            // Устанавливаем информацию о видео
            if (videoTitle != null && video.getTitle() != null) {
                videoTitle.setText(video.getTitle());
            }
            if (videoFolder != null && video.getFolderName() != null) {
                videoFolder.setText(video.getFolderName());
            }

            // Показываем/скрываем PlayerView в зависимости от позиции
            if (position == currentPlayingPosition) {
                playerView.setVisibility(View.VISIBLE);
                playerView.setPlayer(player);
                
                // Загружаем и запускаем видео
                if (video.getUri() != null) {
                    MediaItem currentMediaItem = player.getCurrentMediaItem();
                    
                    // Проверяем, нужно ли загружать новое видео
                    boolean needNewMedia = currentMediaItem == null || 
                        currentMediaItem.localConfiguration == null ||
                        !currentMediaItem.localConfiguration.uri.equals(video.getUri());
                    
                    if (needNewMedia) {
                        Log.d(TAG, "Loading new video at position " + position);
                        MediaItem mediaItem = MediaItem.fromUri(video.getUri());
                        player.setMediaItem(mediaItem);
                        player.prepare();
                    }
                    
                    player.play();
                }
            } else {
                // Для неактивных видео - скрываем PlayerView и останавливаем плеер
                playerView.setVisibility(View.GONE);
                playerView.setPlayer(null);
            }
        }
        
        public void unbind() {
            if (playerView != null) {
                playerView.setPlayer(null);
            }
        }
    }
}
