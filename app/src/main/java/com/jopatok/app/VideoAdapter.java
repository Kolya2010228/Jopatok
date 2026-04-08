package com.jopatok.app;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
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

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private static final String TAG = "VideoAdapter";
    private List<VideoItem> videos = new ArrayList<VideoItem>();
    private Context context;
    private Player player;
    private int currentPlayingPosition = -1;
    private int resizeMode = 0;

    public VideoAdapter(Context context, Player player) {
        this.context = context;
        this.player = player;
    }

    public void setVideos(List<VideoItem> videos) {
        this.videos = videos != null ? videos : new ArrayList<VideoItem>();
        currentPlayingPosition = this.videos.isEmpty() ? -1 : 0;
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

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private PlayerView playerView;
        private FrameLayout videoContainer;
        private TextView videoTitle;
        private TextView videoFolder;
        private long lastTapTime = 0;
        private static final long DOUBLE_TAP_TIMEOUT = 300;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            videoContainer = itemView.findViewById(R.id.videoContainer);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            videoFolder = itemView.findViewById(R.id.videoFolder);
        }

        public void bind(final int position) {
            if (videos == null || position < 0 || position >= videos.size()) return;

            final VideoItem video = videos.get(position);
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
