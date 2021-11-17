package com.meikai.giftplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VideoViewActivity extends AppCompatActivity {

    private SurfaceView surfaceView;

    private MediaPlayer mediaPlayer;
    private String assetsFileName;

    public static void launch(Activity activity, String assetsFileName) {
        Intent intent = new Intent(activity, VideoViewActivity.class);
        intent.putExtra("assetsFileName", assetsFileName);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        surfaceView = findViewById(R.id.surface_view);

        String assetsFileName = getIntent().getStringExtra("assetsFileName");


        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {

                    AssetFileDescriptor assetFileDescriptor = getAssets().openFd(assetsFileName);
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
                    mediaPlayer.setDataSource(assetFileDescriptor);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (Exception e) {
                    Log.e("", e.getMessage(), e);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }
}
