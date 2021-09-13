/*
 * Copyright 2017 Pavel Semak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meikai.giftplayer;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by meikai on 2020/09/21.
 */
public class GiftRenderer implements GLTextureView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "VideoRendererOverlay";

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 4 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 2;

    private final float[] triangleVerticesData = {
            // x , y, s , t
            -1.0f, 1.0f, 0.5f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.5f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
    };

    private FloatBuffer triangleVertices;

    private final String vertexShader = "attribute vec2 a_position;\n"
            + "attribute vec2 a_texCoord;\n"
            + "varying vec2 v_texcoord;\n"
            + "void main(void) {\n"
            + "  gl_Position = vec4(a_position, 0.0, 1.0);\n"
            + "  v_texcoord = a_texCoord;\n"
            + "}\n";

    /**
     * 对应源视频要求为：左彩色、右黑白
     */
    private final String alphaShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texcoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(texture2D(sTexture, v_texcoord + vec2(-0.4956, 0)).rgb, texture2D(sTexture, v_texcoord).r);\n"
            + "}\n";


    /**
     * 对应源视频要求为：左黑白、右彩色
     */
    private final String alphaShader2 = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texcoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(texture2D(sTexture, v_texcoord).rgb, texture2D(sTexture, v_texcoord + vec2(-0.5, 0)).r);\n"
            + "}\n";

    private int program;
    private int textureID;
    private int aPositionHandle;
    private int aTextureHandle;

    private SurfaceTexture surface;
    private boolean updateSurface = false;

    private OnSurfacePrepareListener onSurfacePrepareListener;

    public GiftRenderer() {
        triangleVertices = ByteBuffer.allocateDirect(triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);

        //Matrix.setIdentityM(sTMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        synchronized (this) {
            if (updateSurface) {
                surface.updateTexImage();
                //surface.getTransformMatrix(sTMatrix);
                updateSurface = false;
            }
        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glEnable(GLES10.GL_MULTISAMPLE);
        //2.指定混合因子
        //注意:如果你修改了混合方程式,当你使用混合抗锯齿功能时,请一定要改为默认混合方程式

        //3.开启对点\线\多边形的抗锯齿功能
//        GLES20.glEnable(GL_POINT_SMOOTH);
//        GLES20.glEnable(GL_LINE_SMOOTH);
//        GLES20.glEnable(GL_POLYGON_SMOOTH_HINT);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        GLES20.glUseProgram(program);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        checkGlError("glEnableVertexAttribArray aPositionHandle");

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        checkGlError("glVertexAttribPointer aTextureHandle");
        GLES20.glEnableVertexAttribArray(aTextureHandle);
        checkGlError("glEnableVertexAttribArray aTextureHandle");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");

        GLES20.glFinish();
    }

    @Override
    public void onSurfaceDestroyed(GL10 gl) {
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        logI("onSufaceChanged w " + width + " h " + height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        program = createProgram(vertexShader, alphaShader);
        if (program == 0) {
            return;
        }
        aPositionHandle = GLES20.glGetAttribLocation(program, "a_position");
        checkGlError("glGetAttribLocation aPosition");
        if (aPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        aTextureHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (aTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        prepareSurface();
    }

    private void prepareSurface() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        checkGlError("glBindTexture textureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        surface = new SurfaceTexture(textureID);
        surface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(this.surface);
        onSurfacePrepareListener.surfacePrepared(surface);

        synchronized (this) {
            updateSurface = false;
        }
    }

    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                logI("Could not compile shader " + shaderType
                        + ":\n" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                logI("Could not link program: " + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void checkGlError(String op) {
        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            postError(op + ": checkGlError glError " + error);
        }
    }

    void setAlphaColor(int color) {
        logI("unsupport set alpha color");
    }

    void setCustomShader(String customShader) {
        logI("unsupport set customShader");
    }

    void setAccuracy(double accuracy) {
        logI("unsupport setAccuracy");
    }

    void setOnSurfacePrepareListener(OnSurfacePrepareListener onSurfacePrepareListener) {
        this.onSurfacePrepareListener = onSurfacePrepareListener;
    }

    private int mCurrentVideoWidth;
    private int mCurrentVideoHeight;
    private int mCurrentViewWidth;
    private int mCurrentViewHeight;

    public void updateRadio(int videoWidth, int videoHeight, int viewWidth, int viewHeight) {
        if (mCurrentVideoWidth == videoWidth
                && mCurrentVideoHeight == videoHeight
                && mCurrentViewWidth == viewWidth
                && mCurrentViewHeight == viewHeight
        ) {
            return;
        }
        mCurrentVideoWidth = videoWidth;
        mCurrentVideoHeight = videoHeight;

        mCurrentViewWidth = viewWidth;
        mCurrentViewHeight = viewHeight;

        if (triangleVertices == null) {
            triangleVertices = ByteBuffer.allocateDirect(
                    triangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
        }
        triangleVertices.clear();
        triangleVertices.put(triangleVerticesData).position(0);
        float videoRadio = videoWidth * 1.0f / videoHeight;
        float viewRadio = viewWidth * 1.0f / viewHeight;
        if (videoRadio > viewRadio) {
            float offset = videoWidth - (1.0f * viewWidth * videoHeight) / viewHeight;
            float scale = offset / videoWidth;
            FloatBuffer fb = triangleVertices;

            //  s1
            int startIndex = 2;
            float s1 = fb.get(startIndex);
            s1 = s1 + scale / 2;
            fb.put(startIndex, s1);

            //  s2
            startIndex = startIndex + 4;
            float s2 = fb.get(startIndex);
            s2 = s2 - scale / 2;
            fb.put(startIndex, s2);

            //  s3
            startIndex = startIndex + 4;
            float s3 = fb.get(startIndex);
            s3 = s3 + scale / 2;
            fb.put(startIndex, s3);

            //  s4
            startIndex = startIndex + 4;
            float s4 = fb.get(startIndex);
            s4 = s4 - scale / 2;
            fb.put(startIndex, s4);
        } else {
            float offset = videoHeight - (1.0f * viewHeight * videoWidth) / viewWidth;
            float scale = offset / videoHeight;
            FloatBuffer fb = triangleVertices;

            //  s1
            int startIndex = 3;
            float s1 = fb.get(startIndex);
            s1 = s1 + scale / 2;
            fb.put(startIndex, s1);

            //  s2
            startIndex = startIndex + 4;
            float s2 = fb.get(startIndex);
            s2 = s2 + scale / 2;
            fb.put(startIndex, s2);

            //  s3
            startIndex = startIndex + 4;
            float s3 = fb.get(startIndex);
            s3 = s3 - scale / 2;
            fb.put(startIndex, s3);

            //  s4
            startIndex = startIndex + 4;
            float s4 = fb.get(startIndex);
            s4 = s4 - scale / 2;
            fb.put(startIndex, s4);
        }
    }

    interface OnSurfacePrepareListener {
        void surfacePrepared(Surface surface);
    }

    private void logI(String log) {

    }

    private void logE(String log) {

    }

    private void postError(String error) {
        GLTextureView.postError(error);
    }
}