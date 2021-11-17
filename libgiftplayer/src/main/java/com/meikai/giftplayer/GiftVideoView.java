package com.meikai.giftplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;

import androidx.annotation.IntDef;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

public class GiftVideoView extends GLTextureView {

    private static final String TAG = "AlphaMovieView";

    private enum PlayerState {
        NOT_PREPARED, PREPARED, STARTED, PAUSED, STOPPED, RELEASE
    }

    public static final int CENTER_INSIDE = 0;
    public static final int CENTER_CROP = 1;
    public static final int FIX_XY = 2;

    @IntDef({CENTER_INSIDE, CENTER_CROP, FIX_XY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScaleType {
    }

    private static final int GL_CONTEXT_VERSION = 2;
    private static final int NOT_DEFINED = -1;
    private static final int NOT_DEFINED_COLOR = 0;
    private static final float VIEW_ASPECT_RATIO = 4f / 3f;

    private float videoAspectRatio = VIEW_ASPECT_RATIO;

    @ScaleType
    private int mScaleType = CENTER_INSIDE;

    private GiftRenderer renderer;
    private MediaPlayer mediaPlayer;

    private OnVideoStartedListener onVideoStartedListener;
    private OnVideoEndedListener onVideoEndedListener;

    private boolean isSurfaceCreated;
    private boolean isDataSourceSet;

    private PlayerState state = PlayerState.NOT_PREPARED;

    private int mWidth;
    private int mHeight;

    private int mVideoWidth;
    private int mVideoHeight;

    public GiftVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainRendererOptions(attrs);
        init();
    }

    private void obtainRendererOptions(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.GiftVideoView);
            String scaleTypeStr = arr.getString(R.styleable.GiftVideoView_scaleType);
            if (!TextUtils.isEmpty(scaleTypeStr)) {
                mScaleType = Integer.parseInt(scaleTypeStr);
            }
            arr.recycle();
        }
    }

    private void init() {
        setEGLContextClientVersion(GL_CONTEXT_VERSION);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        initMediaPlayer();

        renderer = new GiftRenderer();

        renderer.setOnSurfacePrepareListener(new GiftRenderer
                .OnSurfacePrepareListener() {
            @Override
            public void surfacePrepared(Surface surface) {
                isSurfaceCreated = true;
                mediaPlayer.setSurface(surface);
                surface.release();
                if (isDataSourceSet) {
                    prepareAndStartMediaPlayer();
                }
            }
        });

        setRenderer(renderer);

        setPreserveEGLContextOnPause(true);
        setOpaque(false);
    }

    public void setAlphaMode(AlphaMode alphaMode) {
        renderer.setAlphaMode(alphaMode);
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public void setScaleType(@ScaleType int mScaleType) {
        this.mScaleType = mScaleType;
    }


    public void setVideoFromAssets(String assetsFileName) {
        reset();

        try {
            AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd
                    (assetsFileName);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor
                    .getStartOffset(), assetFileDescriptor.getLength());

            onDataSourceSet(retriever);

        } catch (Exception e) {
            logE("" + e);
            postError("assetsFileName" + assetsFileName + e);
        }
    }

    public void setVideoFromSD(String fileName) {
        reset();

        try {
            File file = new File(fileName);
            if (file.exists()) {
                mediaPlayer.setDataSource(fileName);

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(fileName);
                onDataSourceSet(retriever);
            }
        } catch (Exception e) {
            logE("" + e);
            postError("fileName " + fileName + e);
        }
    }


    public void setVideoByUrl(String url) {
        reset();

        try {
            mediaPlayer.setDataSource(url);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(url, new HashMap<String, String>());

            onDataSourceSet(retriever);

        } catch (Exception e) {
            logE("" + e);
            postError("url " + url + e);
        }
    }


    public void setVideoFromFile(FileDescriptor fileDescriptor) {
        reset();

        try {
            mediaPlayer.setDataSource(fileDescriptor);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor);

            onDataSourceSet(retriever);

        } catch (Exception e) {
            logE("" + e);
            postError("" + e);
        }
    }


    public void setVideoFromFile(FileDescriptor fileDescriptor, int startOffset, int endOffset) {
        reset();

        try {
            mediaPlayer.setDataSource(fileDescriptor, startOffset, endOffset);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor, startOffset, endOffset);

            onDataSourceSet(retriever);

        } catch (Exception e) {
            logE("" + e);
            postError("" + e);
        }
    }


    @TargetApi(23)
    public void setVideoFromMediaDataSource(MediaDataSource mediaDataSource) {
        reset();

        mediaPlayer.setDataSource(mediaDataSource);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mediaDataSource);

        onDataSourceSet(retriever);
    }


    public void setVideoFromUri(Context context, Uri uri) {
        reset();

        try {
            mediaPlayer.setDataSource(context, uri);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);

            onDataSourceSet(retriever);
        } catch (Exception e) {
            logE(e.getMessage() + e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pause();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    public void start() {
        if (mediaPlayer != null) {
            switch (state) {
                case PREPARED: {
                    mediaPlayer.start();
                    state = PlayerState.STARTED;
                    if (onVideoStartedListener != null) {
                        onVideoStartedListener.onVideoStarted();
                    }

                    break;
                }
                case PAUSED: {
                    mediaPlayer.start();
                    state = PlayerState.STARTED;

                    break;
                }
                case STOPPED: {
                    prepareAsync(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            state = PlayerState.STARTED;
                            if (onVideoStartedListener != null) {
                                onVideoStartedListener.onVideoStarted();
                            }
                        }
                    });

                    break;
                }
                default: {
                    if (onVideoEndedListener != null) {
                        onVideoEndedListener.onVideoEnded();
                    }
                    break;
                }
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && state == PlayerState.STARTED) {
            mediaPlayer.pause();
            state = PlayerState.PAUSED;
        }
    }

    public void stop() {
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED)) {
            mediaPlayer.stop();
            state = PlayerState.STOPPED;
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public void reset() {
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED ||
                state == PlayerState.STOPPED)) {
            mediaPlayer.reset();
            state = PlayerState.NOT_PREPARED;
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            state = PlayerState.RELEASE;
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public PlayerState getState() {
        return state;
    }

    public boolean isPlaying() {
        return state == PlayerState.STARTED;
    }

    public boolean isPaused() {
        return state == PlayerState.PAUSED;
    }

    public boolean isStopped() {
        return state == PlayerState.STOPPED;
    }

    public boolean isReleased() {
        return state == PlayerState.RELEASE;
    }

    public void seekTo(int offsetMilliseconds) {
        mediaPlayer.seekTo(offsetMilliseconds);
    }

    public void setLooping(boolean looping) {
        mediaPlayer.setLooping(looping);
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        mediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        mediaPlayer.setOnErrorListener(onErrorListener);
    }

    public void setOnVideoStartedListener(OnVideoStartedListener onVideoStartedListener) {
        this.onVideoStartedListener = onVideoStartedListener;
    }

    public void setOnVideoEndedListener(OnVideoEndedListener onVideoEndedListener) {
        this.onVideoEndedListener = onVideoEndedListener;
    }


    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener
                                                  onSeekCompleteListener) {
        mediaPlayer.setOnSeekCompleteListener(onSeekCompleteListener);
    }


    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public interface OnVideoStartedListener {
        void onVideoStarted();
    }

    public interface OnVideoEndedListener {
        void onVideoEnded();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = mWidth > 0 ? mWidth : MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = mHeight > 0 ? mHeight : MeasureSpec.getSize(heightMeasureSpec);
        mWidth = widthSize;
        mHeight = heightSize;

        double currentAspectRatio = (double) widthSize / heightSize;

        if (mScaleType == CENTER_INSIDE) {
            if (currentAspectRatio > videoAspectRatio) {
                widthSize = (int) (heightSize * videoAspectRatio);
            } else if (currentAspectRatio < videoAspectRatio) {
                heightSize = (int) (widthSize / videoAspectRatio);
            }
        } else if (mScaleType == CENTER_CROP) {
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                renderer.updateRadio(mVideoWidth, mVideoHeight, mWidth, mHeight);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            if (currentAspectRatio > videoAspectRatio) {
                heightSize = (int) (widthSize / videoAspectRatio);
            } else if (currentAspectRatio < videoAspectRatio) {
                widthSize = (int) (heightSize * videoAspectRatio);
            }
        }

        logI("onMeasure: widthSize " + widthSize + " heightSize " + heightSize);
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        setScreenOnWhilePlaying(true);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                state = PlayerState.PAUSED;
                if (onVideoEndedListener != null) {
                    onVideoEndedListener.onVideoEnded();
                }
            }
        });
    }

    private void prepareAndStartMediaPlayer() {
        prepareAsync(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                start();
            }
        });
    }

    private void calculateVideoAspectRatio(int videoWidth, int videoHeight) {

        boolean isHor;
        if (renderer.getAlphaMode() == null
                || renderer.getAlphaMode() == AlphaMode.LeftAlphaRightColor
                || renderer.getAlphaMode() == AlphaMode.LeftColorRightAlpha) {
            isHor = true;
        } else {
            isHor = false;
        }

        if (isHor) {
            videoWidth /= 2; //左右分布的源视频，实际显示的宽度需要除以2
        } else {
            videoHeight /= 2; //上下分布的源视频，实际显示的高度需要除以2
        }

        if (videoWidth > 0 && videoHeight > 0) {
            videoAspectRatio = (float) videoWidth / videoHeight;
        }
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
        requestLayout();
        invalidate();
    }

    private void onDataSourceSet(MediaMetadataRetriever retriever) {
        int videoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever
                .METADATA_KEY_VIDEO_WIDTH));
        int videoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever
                .METADATA_KEY_VIDEO_HEIGHT));
        logI("onDataSourceSet w " + videoWidth + "  h " + videoHeight);
        calculateVideoAspectRatio(videoWidth, videoHeight);
        isDataSourceSet = true;

        if (isSurfaceCreated) {
            prepareAndStartMediaPlayer();
        }
    }

    private void prepareAsync(final MediaPlayer.OnPreparedListener onPreparedListener) {
        if (mediaPlayer != null && (state == PlayerState.NOT_PREPARED
                || state == PlayerState.STOPPED)) {
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    state = PlayerState.PREPARED;
                    onPreparedListener.onPrepared(mp);
                }
            });
            mediaPlayer.prepareAsync();
        }
    }
}


