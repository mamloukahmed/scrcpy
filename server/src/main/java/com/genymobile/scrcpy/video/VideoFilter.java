package com.genymobile.scrcpy.video;

import com.genymobile.scrcpy.util.Ln;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

public class VideoFilter {
    private EGLDisplay eglDisplay;
    private EGLContext eglContext;
    private EGLSurface eglSurface;
    private SurfaceTexture surfaceTexture;
    private Surface inputSurface;
    int textureId;

    public VideoFilter(Surface outputSurface) {
        // 1. Get EGL display
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("Unable to get EGL14 display");
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("Unable to initialize EGL14");
        }

        // 2. Choose EGL config
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);
        if (numConfigs[0] <= 0) {
            throw new RuntimeException("Unable to find RGB888+recordable ES2 EGL config");
        }
        EGLConfig eglConfig = configs[0];

        // 3. Create EGL context
        int[] contextAttribList = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribList, 0);
        if (eglContext == null) {
            throw new RuntimeException("Failed to create EGL context");
        }

        // 4. Create EGL surface
        int[] surfaceAttribList = {
                EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, outputSurface, surfaceAttribList, 0);
        if (eglSurface == null) {
            throw new RuntimeException("Failed to create EGL window surface");
        }

        // 5. Make EGL context current
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("Failed to make EGL context current");
        }

        // Create a SurfaceTexture for capturing VirtualDisplay content
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        // Create SurfaceTexture to capture the VirtualDisplay output
        surfaceTexture = new SurfaceTexture(textureId);
        inputSurface = new Surface(surfaceTexture);

        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // This will be called when the VirtualDisplay has rendered a new frame.
                Ln.i("==== render");
                render();
            }
        });
    }

    public Surface getInputSurface() {
        return inputSurface;
    }

    public void render() {
        // OpenGL rendering commands here
        // Bind the SurfaceTexture and update its contents
        //surfaceTexture.updateTexImage();

        // Get the transformation matrix from SurfaceTexture (optional if you need it)
        float[] transformMatrix = new float[16];
        surfaceTexture.getTransformMatrix(transformMatrix);

        // Now bind the texture and render using OpenGL
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        // For example, clear the screen with a color
        GLES20.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, 1920, 1080);

        // Swap buffers to display the rendered content
        EGL14.eglSwapBuffers(eglDisplay, eglSurface);
    }

    public void release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglTerminate(eglDisplay);
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglContext = EGL14.EGL_NO_CONTEXT;
        eglSurface = EGL14.EGL_NO_SURFACE;
        surfaceTexture.release();
        inputSurface.release();
    }
}
