package com.demo.libgiftplayer2;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

public class GiftPlayerView extends FrameLayout {

    private final String vertexShader =
            "attribute vec2 aPosition;\n"
                    + "attribute vec2 aTexCoord;\n"
                    + "varying vec2 vTexCoord;\n"
                    + "void main(void) {\n"
                    + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
                    + "  vTexCoord = aTexCoord;\n"
                    + "}\n";

    //左黑白，右彩色
    private final String fragmentShader1 =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform float centerPos;\n"
                    + "uniform samplerExternalOES sTexture;\n"
                    + "varying vec2 vTexCoord;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = vec4(texture2D(sTexture, vTexCoord).rgb, " +
                    "texture2D(sTexture, vTexCoord-vec2(centerPos,0)).r);"
                    + "}\n";

    //左彩色，右黑白
    private final String fragmentShader2 =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform float centerPos;\n"
                    + "uniform samplerExternalOES sTexture;\n"
                    + "varying vec2 vTexCoord;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = vec4(texture2D(sTexture, vTexCoord).rgb, " +
                    "texture2D(sTexture, vTexCoord+vec2(centerPos,0)).r);"
                    + "}\n";

    private final String fragmentShader = fragmentShader2;

    //顶点坐标
    private final float[] vertexCoords = {
            -1.0f, 1.0f,    //左上角
            -1.0f, -1.0f,   //左下角
            1.0f, 1.0f,     //右上角
            1.0f, -1.0f     //右下角
    };

    //全部范围的纹理, 纹理坐标-正放图片，纹理坐标与顶点坐标出现的顺序完全相同，则可以呈现出正放的图片
    private final float[] textureCoord_1 = {
            0.0f, 0.0f, //左上、原点
            0.0f, 1.0f, //左下
            1.0f, 0.0f, //右上
            1.0f, 1.0f, //右下
    };

    //左侧一半部分的纹理
    private final float[] textureCoord_2 = {
            0.0f, 0.0f, //顶边中点
            0.0f, 1.0f, //底边中点
            0.5f, 0.0f, //右上
            0.5f, 1.0f, //右下
    };

    //右侧一半部分的纹理
    private final float[] textureCoord_3 = {
            0.5f, 0.0f, //顶边中点
            0.5f, 1.0f, //底边中点
            1.0f, 0.0f, //右上
            1.0f, 1.0f, //右下
    };

    private final float[] textureCoord = textureCoord_2;

    private GLTextureView glTextureView;
    private MediaPlayer mediaPlayer;

    public GiftPlayerView(@NonNull Context context) {
        super(context);
    }

    public GiftPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GiftPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(Context context, String videoAssetsPath) {
        init(context, videoAssetsPath, 0.5f);
    }

    public void init(Context context, String videoAssetsPath, float centerPos) {
        glTextureView = new GLTextureView(context);
        FrameLayout.LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(glTextureView, lp);

        glTextureView.setEGLContextClientVersion(2);
        glTextureView.setOpaque(false);
        glTextureView.setPreserveEGLContextOnPause(true);
        glTextureView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        VideoRender render = new VideoRender();
        {
            RenderObject renderObject = new RenderObject();
            renderObject.centerPos = centerPos;
            renderObject.init(vertexShader, fragmentShader, vertexCoords, textureCoord, true);
            renderObject.onSurfaceListener = new OnSurfaceListener() {
                @Override
                public void onSurfaceCreated(Surface surface) {
                    try {
                        playVideo(surface, videoAssetsPath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            render.renderObject = renderObject;
        }

        glTextureView.setRenderer(render);

        //必须在setRenderer之后才能调用
        //因为MediaPlayer会连续不断的输出最新的解码图像，所以必须持续不断的刷新opengl
        glTextureView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    }

    private void playVideo(Surface surface, String videoAssetsPath) throws Exception {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //将此MediaPlayer解码出的图像输出到此SurfaceHolder对应的Surface中，
        // 而这个Surface是由SurfaceView创建的，而SurfaceView在创建时已将此Surface挂载到系统屏幕抽象上，从而能显示到屏幕中
        mediaPlayer.setSurface(surface);
        surface.release();
        //设置播放的视频源
        AssetFileDescriptor afd = getContext().getAssets().openFd(videoAssetsPath);
        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });

        mediaPlayer.prepareAsync();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mediaPlayer.release();
    }

    /**
     * ////////////////////////////////////////////////////////////////////////////////////////////////////////
     * ////////////////////////////////////////////////////////////////////////////////////////////////////////
     * ////////////////////////////////////////////////////////////////////////////////////////////////////////
     */
    private interface OnSurfaceListener {
        void onSurfaceCreated(Surface surface);
    }

    private static class RenderObject {
        private static int BYTE_OF_FLOAT = 4; //float类型数据所占用的字节数
        private static int COORDS_PER_VERTEX = 2; //每个顶点的坐标有2个float组成

        private String vertexShaderCode;
        private String fragmentShaderCode;

        private float[] vertexCoords;
        private float[] textureCoord;

        public boolean isEffective;

        //顶点个数
        private int vertexCount;
        //每个顶点坐标字节Buffer的步长，即多少个字节表示一个顶点坐标
        private int vertexStride;

        private int mProgram;
        private FloatBuffer vertexBuffer;
        private FloatBuffer textureCoordBuffer;
        private int vertexShaderIns;
        private int fragmentShaderIns;

        private int glAPosition;
        private int glATexCoord;
        private int glCenterPos;

        private int textureId;
        private SurfaceTexture surfaceTexture;
        private Surface surface;

        private boolean newFrameAvailable;

        public float centerPos = 0.5f;

        public OnSurfaceListener onSurfaceListener;

        public void init(String vertexShaderCode, String fragmentShaderCode,
                         float[] vertexCoords, float[] textureCoord, boolean isEffective) {
            this.vertexShaderCode = vertexShaderCode;
            this.fragmentShaderCode = fragmentShaderCode;
            this.vertexCoords = vertexCoords;
            this.textureCoord = textureCoord;
            this.isEffective = isEffective;

            vertexCount = vertexCoords.length / COORDS_PER_VERTEX;
            vertexStride = COORDS_PER_VERTEX * BYTE_OF_FLOAT;

            //将内存中的顶点坐标数组，转换为字节缓冲区，因为opengl只能接受整块的字节缓冲区的数据
            ByteBuffer bb = ByteBuffer.allocateDirect(vertexCoords.length * BYTE_OF_FLOAT);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertexCoords);
            vertexBuffer.position(0);

            //将内存中的纹理坐标数组，转换为字节缓冲区，因为opengl只能接受整块的字节缓冲区的数据
            ByteBuffer cc = ByteBuffer.allocateDirect(textureCoord.length * BYTE_OF_FLOAT);
            cc.order(ByteOrder.nativeOrder());
            textureCoordBuffer = cc.asFloatBuffer();
            textureCoordBuffer.put(textureCoord);
            textureCoordBuffer.position(0);
        }

        public void createProgram() {
            vertexShaderIns = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            fragmentShaderIns = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            //创建一个空的OpenGLES程序
            mProgram = GLES20.glCreateProgram();
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShaderIns);
            //将片元着色器加入到程序中
            GLES20.glAttachShader(mProgram, fragmentShaderIns);
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram);

            glAPosition = GLES20.glGetAttribLocation(mProgram, "aPosition");
            glATexCoord = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
            glCenterPos = GLES20.glGetUniformLocation(mProgram, "centerPos");

            createTexture();

            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    Log.e("tag", "onFrameAvailable, " + Thread.currentThread().getName());
                    newFrameAvailable = true;
                }
            });

            surface = new Surface(surfaceTexture);

            if (onSurfaceListener != null) {
                onSurfaceListener.onSurfaceCreated(surface);
            }
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        public void onDrawFrame(GL10 gl) {
            if (!isEffective) {
                return;
            }
//            Log.e("tag", "onDrawFrame, " + Thread.currentThread().getName());
            if (newFrameAvailable) {
                //将图像数据流的最新图像更新到纹理中，那么后续opengl纹理渲染时就会显示出图像数据流的最新图像到屏幕上
                surfaceTexture.updateTexImage();
                newFrameAvailable = false;
            }

            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_BLEND);

            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            GLES20.glUseProgram(mProgram);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

            GLES20.glUniform1f(glCenterPos, centerPos);
            GLES20.glVertexAttribPointer(glAPosition, 2, GLES20.GL_FLOAT, false,
                    vertexStride, vertexBuffer);
            GLES20.glEnableVertexAttribArray(glAPosition);

            GLES20.glVertexAttribPointer(glATexCoord, 2, GLES20.GL_FLOAT, false,
                    vertexStride, textureCoordBuffer);
            GLES20.glEnableVertexAttribArray(glATexCoord);

            int drawVertexCount = 4; //矩形的绘制需要4个顶点以三角形带的方式绘制
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, drawVertexCount);
            GLES20.glFinish();
        }

        public int loadShader(int type, String shaderCode) {
            //根据type创建顶点着色器或者片元着色器
            int shader = GLES20.glCreateShader(type);
            //将资源加入到着色器中，并编译
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private int createTexture() {
            int[] texture = new int[1];
            //从offset=0号纹理单元开始生成n=1个纹理，并将纹理id保存到int[]=texture数组中
            GLES20.glGenTextures(1, texture, 0);
            textureId = texture[0];
            //将生成的纹理与gpu关联为外部纹理类型，传入纹理id作为参数，每次bind之后，后续操作的纹理都是该纹理
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            //返回生成的纹理的句柄
            return texture[0];
        }

    }

    /**
     * ////////////////////////////////////////////////////////////////////////////////////////////////////////
     * ////////////////////////////////////////////////////////////////////////////////////////////////////////
     * ////////////////////////////////////////////////////////////////////////////////////////////////////////
     */
    private static class VideoRender implements GLTextureView.Renderer {

        public RenderObject renderObject;


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            renderObject.createProgram();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            renderObject.onSurfaceChanged(gl, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            renderObject.onDrawFrame(gl);
        }
    }
}
